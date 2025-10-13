package dacslab.heterosync.core.network

import dacslab.heterosync.core.data.*
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class DeviceWebSocketService {

    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient {
        install(WebSockets)
    }

    private var webSocketSession: DefaultClientWebSocketSession? = null
    private var isConnected: Boolean = false
    private var connectionJob: Job? = null

    // 연결 상태 콜백
    var onConnected: ((String, Long) -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

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

            connectionJob = CoroutineScope(Dispatchers.Default).launch {
                try {
                    client.webSocket(
                        host = serverIp,
                        port = serverPort,
                        path = "/ws?deviceType=$deviceType&deviceId=$deviceId"
                    ) {
                        webSocketSession = this
                        isConnected = true
                        println("WebSocket connected: ws://$serverIp:$serverPort/ws")

                        // Wait for incoming messages
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val messageText = frame.readText()
                                handleIncomingMessage(messageText)
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("WebSocket connection failed: ${e.message}")
                    isConnected = false
                    onDisconnected?.invoke()
                    onError?.invoke("Connection error: ${e.message}")
                } finally {
                    isConnected = false
                    webSocketSession = null
                    onDisconnected?.invoke()
                    println("WebSocket connection closed")
                }
            }

            Result.success("WebSocket connection started")
        } catch (e: Exception) {
            println("WebSocket connection failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun disconnect(): Result<String> {
        return try {
            connectionJob?.cancel()
            webSocketSession?.close()
            webSocketSession = null
            isConnected = false
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

            println("Unknown message type")
        } catch (e: Exception) {
            println("Message processing failed: ${e.message}")
            onError?.invoke("Message processing failed: ${e.message}")
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