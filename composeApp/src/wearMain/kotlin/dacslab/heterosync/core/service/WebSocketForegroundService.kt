package dacslab.heterosync.core.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import dacslab.heterosync.core.data.ConnectionHealth
import dacslab.heterosync.core.network.DeviceWebSocketService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class WebSocketForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "websocket_service_channel"
        const val NOTIFICATION_ID = 1

        const val ACTION_START_SERVICE = "ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"

        const val EXTRA_SERVER_IP = "EXTRA_SERVER_IP"
        const val EXTRA_SERVER_PORT = "EXTRA_SERVER_PORT"
        const val EXTRA_DEVICE_TYPE = "EXTRA_DEVICE_TYPE"
        const val EXTRA_DEVICE_ID = "EXTRA_DEVICE_ID"
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val webSocketService = DeviceWebSocketService()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _connectionHealth = MutableStateFlow(ConnectionHealth.UNKNOWN)
    val connectionHealth: StateFlow<ConnectionHealth> = _connectionHealth

    private var wakeLock: PowerManager.WakeLock? = null

    inner class LocalBinder : Binder() {
        fun getService(): WebSocketForegroundService = this@WebSocketForegroundService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupWebSocketCallbacks()
        acquireWakeLock()
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "HeteroSync::WebSocketWakeLock"
            ).apply {
                acquire()
                println("Wake Lock acquired - preventing system freeze")
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

                startForeground(NOTIFICATION_ID, createNotification("Connecting..."))
                connectToServer(serverIp, serverPort, deviceType, deviceId)
            }
            ACTION_STOP_SERVICE -> {
                stopService()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private fun setupWebSocketCallbacks() {
        webSocketService.onConnected = { deviceId, serverTime ->
            _connectionState.value = ConnectionState.Connected(deviceId, serverTime)
            updateNotification("Connected: $deviceId")
        }

        webSocketService.onDisconnected = {
            _connectionState.value = ConnectionState.Disconnected
            updateNotification("Disconnected")
        }

        webSocketService.onError = { error ->
            _connectionState.value = ConnectionState.Error(error)
            updateNotification("Error: $error")
        }

        webSocketService.onReconnecting = { attempt ->
            _connectionState.value = ConnectionState.Reconnecting(attempt)
            updateNotification("Reconnecting... ($attempt)")
        }

        webSocketService.onHealthChanged = { health ->
            _connectionHealth.value = health
        }

        // Monitor connection health
        serviceScope.launch {
            webSocketService.connectionHealth.collect { health ->
                _connectionHealth.value = health
                val healthStatus = when (health) {
                    ConnectionHealth.HEALTHY -> "✓"
                    ConnectionHealth.UNHEALTHY -> "⚠"
                    ConnectionHealth.DEAD -> "✗"
                    ConnectionHealth.UNKNOWN -> "?"
                }

                when (val state = _connectionState.value) {
                    is ConnectionState.Connected -> {
                        updateNotification("Connected $healthStatus")
                    }
                    else -> {}
                }
            }
        }
    }

    private fun connectToServer(serverIp: String, serverPort: Int, deviceType: String, deviceId: String) {
        serviceScope.launch {
            _connectionState.value = ConnectionState.Connecting
            updateNotification("Connecting to $serverIp:$serverPort...")

            val result = webSocketService.connectToServer(serverIp, serverPort, deviceType, deviceId)
            result.onFailure { error ->
                _connectionState.value = ConnectionState.Error(error.message ?: "Connection failed")
                updateNotification("Connection failed")
            }
        }
    }

    fun disconnectFromServer() {
        serviceScope.launch {
            webSocketService.disconnect()
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    private fun stopService() {
        serviceScope.launch {
            webSocketService.disconnect()
            releaseWakeLock()
            serviceScope.cancel()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
    }

    private fun createNotification(contentText: String): Notification {
        val stopIntent = Intent(this, WebSocketForegroundService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
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
                "Disconnect",
                stopPendingIntent
            )
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        data class Connected(val deviceId: String, val serverTime: Long) : ConnectionState()
        data class Reconnecting(val attempt: Int) : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }
}