package dacslab.heterosync.core.network

import dacslab.heterosync.core.data.*
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.time.Duration.Companion.seconds

class DeviceWebSocketService {

    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient {
        install(WebSockets) {
            pingInterval = 20.seconds  // 20초마다 ping 전송
            maxFrameSize = Long.MAX_VALUE
        }
    }

    private var webSocketSession: DefaultClientWebSocketSession? = null
    private var isConnected: Boolean = false
    private var connectionJob: Job? = null
    private var pingJob: Job? = null
    private var reconnectJob: Job? = null
    private var healthCheckJob: Job? = null

    // 재연결 설정
    private var shouldReconnect = false
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private val reconnectDelay = 3_000L  // 3초

    // 연결 파라미터 저장 (재연결용)
    private var lastServerIp: String? = null
    private var lastServerPort: Int? = null
    private var lastDeviceType: String? = null
    private var lastDeviceId: String? = null

    // 연결 건강도 모니터링
    private var lastPingReceived: Long = 0L
    private var lastPongSent: Long = 0L
    private val _connectionHealth = MutableStateFlow(ConnectionHealth.UNKNOWN)
    val connectionHealth: StateFlow<ConnectionHealth> = _connectionHealth

    // 연결 상태 콜백
    var onConnected: ((String, Long) -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onReconnecting: ((Int) -> Unit)? = null  // 재연결 시도 횟수 전달
    var onHealthChanged: ((ConnectionHealth) -> Unit)? = null

    suspend fun connectToServer(
        serverIp: String,
        serverPort: Int,
        deviceType: String,
        deviceId: String
    ): Result<String> {
        return try {
            if (isConnected) {
                return Result.success("Already connected to WebSocket server")
            }

            // 연결 파라미터 저장
            lastServerIp = serverIp
            lastServerPort = serverPort
            lastDeviceType = deviceType
            lastDeviceId = deviceId
            shouldReconnect = true
            reconnectAttempts = 0

            startConnection()
            Result.success("WebSocket connection started")
        } catch (e: Exception) {
            println("WebSocket connection failed: ${e.message}")
            Result.failure(e)
        }
    }

    private fun startConnection() {
        val serverIp = lastServerIp ?: return
        val serverPort = lastServerPort ?: return
        val deviceType = lastDeviceType ?: return
        val deviceId = lastDeviceId ?: return

        connectionJob?.cancel()
        connectionJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                client.webSocket(
                    host = serverIp,
                    port = serverPort,
                    path = "/ws?deviceType=$deviceType&deviceId=$deviceId"
                ) {
                    webSocketSession = this
                    isConnected = true
                    reconnectAttempts = 0  // 연결 성공시 재연결 카운터 초기화
                    lastPingReceived = System.currentTimeMillis()  // 연결 시작 시간 기록
                    _connectionHealth.value = ConnectionHealth.HEALTHY
                    println("WebSocket connected: ws://$serverIp:$serverPort/ws")

                    // 건강도 모니터링 시작
                    startHealthCheckJob()

                    // 커스텀 Ping 작업 시작
                    startPingJob()

                    // Wait for incoming messages
                    try {
                        for (frame in incoming) {
                            when (frame) {
                                is Frame.Text -> {
                                    val messageText = frame.readText()
                                    handleIncomingMessage(messageText)
                                }
                                is Frame.Ping -> {
                                    println("Received Ping from server")
                                    send(Frame.Pong(frame.data))
                                }
                                is Frame.Pong -> {
                                    println("Received Pong from server")
                                }
                                is Frame.Close -> {
                                    println("Received Close frame from server")
                                }
                                else -> {
                                    println("Received other frame type: ${frame.frameType}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("Error reading frames: ${e.message}")
                        throw e
                    }
                }
            } catch (e: Exception) {
                println("WebSocket connection failed: ${e.message}")
                isConnected = false
                _connectionHealth.value = ConnectionHealth.DEAD
                stopPingJob()
                stopHealthCheckJob()

                // 재연결 시도
                if (shouldReconnect && reconnectAttempts < maxReconnectAttempts) {
                    scheduleReconnect()
                } else {
                    onDisconnected?.invoke()
                    if (reconnectAttempts >= maxReconnectAttempts) {
                        onError?.invoke("Connection lost after $maxReconnectAttempts attempts")
                    } else {
                        onError?.invoke("Connection error: ${e.message}")
                    }
                }
            } finally {
                isConnected = false
                webSocketSession = null
                _connectionHealth.value = ConnectionHealth.UNKNOWN
                stopPingJob()
                stopHealthCheckJob()
                println("WebSocket connection closed")
            }
        }
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = CoroutineScope(Dispatchers.Default).launch {
            reconnectAttempts++
            println("Reconnecting... Attempt $reconnectAttempts/$maxReconnectAttempts")
            onReconnecting?.invoke(reconnectAttempts)
            
            delay(reconnectDelay)
            
            if (shouldReconnect) {
                startConnection()
            }
        }
    }

    private fun startPingJob() {
        stopPingJob()
        pingJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive && isConnected) {
                delay(15_000)  // 15초마다 ping 전송
                try {
                    val pingMessage = PingMessage(
                        type = "PING",
                        timestamp = System.currentTimeMillis()
                    )
                    val pingJson = json.encodeToString(pingMessage)
                    webSocketSession?.send(Frame.Text(pingJson))
                    println("Ping sent to keep connection alive: $pingJson")
                } catch (e: Exception) {
                    println("Failed to send ping: ${e.message}")
                    break
                }
            }
        }
    }

    private fun stopPingJob() {
        pingJob?.cancel()
        pingJob = null
    }

    private fun startHealthCheckJob() {
        stopHealthCheckJob()
        healthCheckJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive && isConnected) {
                delay(30_000)  // 30초마다 건강도 체크
                checkConnectionHealth()
            }
        }
    }

    private fun stopHealthCheckJob() {
        healthCheckJob?.cancel()
        healthCheckJob = null
    }

    private fun checkConnectionHealth() {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastPing = currentTime - lastPingReceived

        val newHealth = when {
            !isConnected -> ConnectionHealth.UNKNOWN
            timeSinceLastPing > 120_000 -> ConnectionHealth.DEAD  // 120초 초과
            timeSinceLastPing > 90_000 -> ConnectionHealth.UNHEALTHY  // 90초 초과
            else -> ConnectionHealth.HEALTHY
        }

        if (_connectionHealth.value != newHealth) {
            _connectionHealth.value = newHealth
            onHealthChanged?.invoke(newHealth)

            when (newHealth) {
                ConnectionHealth.HEALTHY ->
                    println("✅ Connection healthy (Last PING: ${timeSinceLastPing}ms ago)")
                ConnectionHealth.UNHEALTHY ->
                    println("⚠️ Connection unhealthy (Last PING: ${timeSinceLastPing}ms ago)")
                ConnectionHealth.DEAD -> {
                    println("🔴 Connection dead (Last PING: ${timeSinceLastPing}ms ago)")
                    // 연결이 죽었으면 재연결 시도
                    CoroutineScope(Dispatchers.Default).launch {
                        disconnect()
                        if (shouldReconnect) {
                            scheduleReconnect()
                        }
                    }
                }
                ConnectionHealth.UNKNOWN ->
                    println("❓ Connection status unknown")
            }
        }
    }

    suspend fun disconnect(): Result<String> {
        return try {
            shouldReconnect = false  // 재연결 중지
            reconnectJob?.cancel()
            stopPingJob()
            stopHealthCheckJob()
            connectionJob?.cancel()
            webSocketSession?.close()
            webSocketSession = null
            isConnected = false
            reconnectAttempts = 0
            _connectionHealth.value = ConnectionHealth.UNKNOWN
            println("WebSocket disconnected")
            Result.success("WebSocket disconnection completed")
        } catch (e: Exception) {
            println("WebSocket disconnection failed: ${e.message}")
            Result.failure(e)
        }
    }

    fun isConnectedToServer(): Boolean {
        return isConnected
    }

    private suspend fun handleIncomingMessage(messageText: String) {
        try {
            println("Received message: $messageText")

            // Handle CONNECTED message
            if (messageText.contains("\"type\":\"CONNECTED\"")) {
                val message = json.decodeFromString<ConnectedMessage>(messageText)
                println("Connection confirmed: deviceId=${message.deviceId}, serverTime=${message.serverTime}")
                onConnected?.invoke(message.deviceId, message.serverTime)
                return
            }

            // Handle TIME_REQUEST message
            if (messageText.contains("\"type\":\"TIME_REQUEST\"")) {
                val message = json.decodeFromString<TimeRequestMessage>(messageText)
                println("Time request received: requestId=${message.requestId}")
                handleTimeRequest(message.requestId)
                return
            }

            // Handle PING message
            if (messageText.contains("\"type\":\"PING\"")) {
                val pingMessage = json.decodeFromString<PingMessage>(messageText)
                lastPingReceived = System.currentTimeMillis()  // PING 수신 시간 기록
                println("Ping received from server at ${pingMessage.timestamp}")
                handlePing()
                return
            }

            // Handle PONG message
            if (messageText.contains("\"type\":\"PONG\"")) {
                val pongMessage = json.decodeFromString<PongMessage>(messageText)
                println("Pong received from server at ${pongMessage.timestamp}")
                return
            }

            println("Unknown message type")
        } catch (e: Exception) {
            println("Message processing failed: ${e.message}")
            onError?.invoke("Message processing failed: ${e.message}")
        }
    }

    private suspend fun handlePing() {
        try {
            val currentTime = System.currentTimeMillis()
            val pongMessage = PongMessage(
                type = "PONG",
                timestamp = currentTime
            )
            val pongJson = json.encodeToString(pongMessage)
            webSocketSession?.send(Frame.Text(pongJson))
            lastPongSent = currentTime  // PONG 전송 시간 기록
            println("Pong sent in response to server ping: $pongJson")
        } catch (e: Exception) {
            println("Failed to send pong: ${e.message}")
            onError?.invoke("Failed to send pong: ${e.message}")
        }
    }

    private suspend fun handleTimeRequest(requestId: String) {
        try {
            val currentTime = System.currentTimeMillis()
            val response = TimeResponseMessage(
                type = "TIME_RESPONSE",
                requestId = requestId,
                timestamp = currentTime
            )

            val responseJson = json.encodeToString(response)
            webSocketSession?.send(Frame.Text(responseJson))
            println("Time response sent: requestId=$requestId, timestamp=$currentTime")
        } catch (e: Exception) {
            println("Time response send failed: ${e.message}")
            onError?.invoke("Time response send failed: ${e.message}")
        }
    }
}