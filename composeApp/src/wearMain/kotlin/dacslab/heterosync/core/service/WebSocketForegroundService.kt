package dacslab.heterosync.core.service

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import dacslab.heterosync.core.data.ConnectionHealth
import dacslab.heterosync.core.network.DeviceWebSocketService
import kotlinx.coroutines.*

class WebSocketForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "websocket_service_channel"
        const val NOTIFICATION_ID = 1

        const val ACTION_START_SERVICE = "ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
        const val ACTION_RESTART_CONNECTION = "ACTION_RESTART_CONNECTION"

        const val EXTRA_SERVER_IP = "EXTRA_SERVER_IP"
        const val EXTRA_SERVER_PORT = "EXTRA_SERVER_PORT"
        const val EXTRA_DEVICE_TYPE = "EXTRA_DEVICE_TYPE"
        const val EXTRA_DEVICE_ID = "EXTRA_DEVICE_ID"
    }

    // 서비스 생명주기와 연결된 CoroutineScope
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var webSocketService: DeviceWebSocketService
    private var wakeLock: PowerManager.WakeLock? = null

    // 연결 파라미터 저장 (재연결용)
    private var currentServerIp: String? = null
    private var currentServerPort: Int = 0
    private var currentDeviceType: String? = null
    private var currentDeviceId: String? = null

    // WebSocket 재연결 요청을 받는 BroadcastReceiver
    private val restartReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_RESTART_CONNECTION) {
                println("📥 Received restart connection request")
                restartWebSocketConnection()
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()

        // DeviceWebSocketService 초기화
        webSocketService = DeviceWebSocketService()

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Starting..."))

        // Acquire wake lock once when service is created
        acquireWakeLock()

        // WebSocket 콜백 설정
        setupWebSocketCallbacks()

        // WebSocket 재연결 요청 BroadcastReceiver 등록
        val filter = IntentFilter(ACTION_RESTART_CONNECTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(restartReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(restartReceiver, filter)
        }
        println("🔍 Restart receiver registered")
    }

    /**
     * WebSocket 콜백 설정
     * 재초기화 시 재사용 가능하도록 별도 메서드로 분리
     */
    private fun setupWebSocketCallbacks() {
        webSocketService.onConnected = { deviceId, _ ->
            updateNotification("Connected: $deviceId")
            // 연결 성공 시 현재 Health 상태 브로드캐스트
            // (ServiceMonitor가 초기 상태 변경을 놓치지 않도록)
            sendHealthChangedBroadcast(webSocketService.connectionHealth.value)
        }

        webSocketService.onDisconnected = {
            updateNotification("Disconnected")
            // stopSelf() 제거 - 명시적 종료만 stopService()를 통해 처리
        }

        webSocketService.onError = { error ->
            val shortMsg = if (error.length > 40) {
                "${error.take(37)}..."
            } else {
                error
            }
            println("❌ WebSocket Error: $error")
            updateNotification("Error: $shortMsg")
        }

        webSocketService.onReconnecting = { attempt ->
            updateNotification("Reconnecting (#$attempt)")
        }

        webSocketService.onHealthChanged = { health ->
            println("Connection health changed: $health")
            sendHealthChangedBroadcast(health)
        }

        println("🔧 WebSocket callbacks configured")
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "HeteroSync::WebSocketWakeLock"
            ).apply {
                setReferenceCounted(false)  // Prevent automatic release
                acquire()  // Acquire indefinitely (will be released in onDestroy)
                println("Wake Lock acquired (indefinite) - preventing system freeze")
            }
        } catch (e: Exception) {
            println("Failed to acquire Wake Lock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    println("Wake Lock released")
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            println("Failed to release Wake Lock: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVICE -> {
                val serverIp = intent.getStringExtra(EXTRA_SERVER_IP) ?: return START_NOT_STICKY
                val serverPort = intent.getIntExtra(EXTRA_SERVER_PORT, 0)
                val deviceType = intent.getStringExtra(EXTRA_DEVICE_TYPE) ?: return START_NOT_STICKY
                val deviceId = intent.getStringExtra(EXTRA_DEVICE_ID) ?: return START_NOT_STICKY

                // 연결 파라미터 저장 (재연결용)
                currentServerIp = serverIp
                currentServerPort = serverPort
                currentDeviceType = deviceType
                currentDeviceId = deviceId

                // 이미 연결되어 있는지 확인
                if (webSocketService.isConnectedToServer()) {
                    println("✅ Service already connected - updating notification only")
                    // 이미 연결되어 있으면 현재 상태로 알림만 업데이트
                    updateNotification("Connected: $deviceId")

                    // Service 시작 브로드캐스트 전송 (ServiceMonitor 타이머 리셋용)
                    sendServiceStartedBroadcast()

                    // 현재 Health 상태 브로드캐스트
                    sendHealthChangedBroadcast(webSocketService.connectionHealth.value)
                } else {
                    println("🔌 Starting new connection...")

                    // Service 시작 브로드캐스트 전송
                    sendServiceStartedBroadcast()

                    // 현재 connectionHealth 상태 즉시 브로드캐스트
                    // (ServiceMonitor가 나중에 시작되어도 현재 상태를 알 수 있도록)
                    val currentHealth = webSocketService.connectionHealth.value
                    println("📊 Current connectionHealth at service start: $currentHealth")
                    sendHealthChangedBroadcast(currentHealth)

                    // Connect in background (wake lock already acquired in onCreate)
                    CoroutineScope(Dispatchers.IO).launch {
                        updateNotification("Connecting...")
                        webSocketService.connectToServer(serverIp, serverPort, deviceType, deviceId)
                    }
                }
            }
            ACTION_STOP_SERVICE -> {
                stopService()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun stopService() {
        // 실제 정리 작업은 onDestroy()에서 동기적으로 처리됨
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * 리소스 정리 (WebSocket 연결 종료, Wake Lock 해제)
     */
    private suspend fun cleanupResources() {
        println("🧹 Cleaning up resources...")

        try {
            // 1. WebSocket 연결 종료
            webSocketService.disconnect()
            println("  ✓ WebSocket disconnected")

            // 2. Wake Lock 해제
            releaseWakeLock()
            println("  ✓ Wake Lock released")

            // 리소스 정리 완료 대기
            delay(500)

            println("🧹 Cleanup completed")
        } catch (e: Exception) {
            println("⚠️ Cleanup error: ${e.message}")
        }
    }

    /**
     * 리소스 재초기화 (DeviceWebSocketService 새 인스턴스, 콜백 재설정, Wake Lock 재획득)
     */
    private fun reinitializeResources() {
        println("🔄 Reinitializing resources...")

        try {
            // 1. DeviceWebSocketService 새 인스턴스 생성
            webSocketService = DeviceWebSocketService()
            println("  ✓ New DeviceWebSocketService instance created")

            // 2. 콜백 재설정
            setupWebSocketCallbacks()
            println("  ✓ Callbacks reconfigured")

            // 3. Wake Lock 재획득
            acquireWakeLock()
            println("  ✓ Wake Lock reacquired")

            // 4. Notification 업데이트
            updateNotification("Reinitialized")

            println("🔄 Reinitialization completed")
        } catch (e: Exception) {
            println("⚠️ Reinitialization error: ${e.message}")
        }
    }

    /**
     * WebSocket 연결 재시작 (Service는 계속 실행)
     * 완전한 재시작: 리소스 정리 → 재초기화 → 재연결 → 타이머 리셋
     */
    private fun restartWebSocketConnection() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                println("🔄 Full restart initiated...")
                updateNotification("Restarting...")

                // === Phase 1: 리소스 정리 ===
                cleanupResources()

                // === Phase 2: 리소스 재초기화 ===
                reinitializeResources()

                // === Phase 3: 연결 재시작 ===
                val ip = currentServerIp ?: return@launch
                val port = currentServerPort
                val type = currentDeviceType ?: return@launch
                val id = currentDeviceId ?: return@launch

                updateNotification("Reconnecting...")
                webSocketService.connectToServer(ip, port, type, id)

                // === Phase 4: 타이머 리셋 브로드캐스트 ===
                sendServiceStartedBroadcast()

                println("✅ Full restart completed")
            } catch (e: Exception) {
                println("❌ Full restart failed: ${e.message}")
                updateNotification("Restart failed")
            }
        }
    }

    /**
     * Service 시작 브로드캐스트 전송
     */
    private fun sendServiceStartedBroadcast() {
        val intent = Intent(ServiceBroadcastActions.ACTION_SERVICE_STARTED).apply {
            // 명시적 브로드캐스트: 같은 패키지 내에서만 전달되도록 설정
            setPackage(packageName)
            putExtra(ServiceBroadcastActions.EXTRA_START_TIME, System.currentTimeMillis())
        }
        sendBroadcast(intent)
        println("📡 Broadcast sent: SERVICE_STARTED")
    }

    /**
     * ConnectionHealth 변경 브로드캐스트 전송
     */
    private fun sendHealthChangedBroadcast(health: ConnectionHealth) {
        val intent = Intent(ServiceBroadcastActions.ACTION_HEALTH_CHANGED).apply {
            // 명시적 브로드캐스트: 같은 패키지 내에서만 전달되도록 설정
            setPackage(packageName)
            putExtra(ServiceBroadcastActions.EXTRA_HEALTH, health.name)
        }
        sendBroadcast(intent)
        println("📡 Broadcast sent: HEALTH_CHANGED -> $health")
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "WebSocket Connection",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Maintains WebSocket connection in background"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(contentText: String): Notification {
        // 앱 열기 Intent
        val openAppIntent = Intent(this, dacslab.heterosync.core.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 연결 해제 Intent
        val stopIntent = Intent(this, WebSocketForegroundService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HeteroSync")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "연결 해제",
                stopPendingIntent
            )
            .setContentIntent(openAppPendingIntent)  // 알림 클릭 시 앱 열기
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        // BroadcastReceiver 등록 해제
        try {
            unregisterReceiver(restartReceiver)
            println("🛑 Restart receiver unregistered")
        } catch (e: IllegalArgumentException) {
            println("⚠️ Restart receiver already unregistered")
        }

        // 동기적으로 서비스 종료 처리 (ANR 방지를 위해 타임아웃 설정)
        runBlocking {
            try {
                withTimeout(5000L) {  // 최대 5초 대기
                    webSocketService.disconnect()
                }
            } catch (e: Exception) {
                println("Service cleanup error: ${e.message}")
            }
        }

        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }
}