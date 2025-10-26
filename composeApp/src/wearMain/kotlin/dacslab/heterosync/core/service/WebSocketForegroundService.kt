package dacslab.heterosync.core.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import dacslab.heterosync.core.network.DeviceWebSocketService
import kotlinx.coroutines.*

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

    private val webSocketService = DeviceWebSocketService()
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Starting..."))

        // Acquire wake lock once when service is created
        acquireWakeLock()

        // Simple callbacks - just update notification
        webSocketService.onConnected = { deviceId, _ ->
            updateNotification("Connected: $deviceId")
        }

        webSocketService.onDisconnected = {
            updateNotification("Disconnected")
            stopSelf()
        }

        webSocketService.onError = { error ->
            updateNotification("Error")
        }

        webSocketService.onReconnecting = { attempt ->
            updateNotification("Reconnecting ($attempt/5)")
        }
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

                // Connect in background (wake lock already acquired in onCreate)
                CoroutineScope(Dispatchers.IO).launch {
                    updateNotification("Connecting...")
                    webSocketService.connectToServer(serverIp, serverPort, deviceType, deviceId)
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
        CoroutineScope(Dispatchers.IO).launch {
            webSocketService.disconnect()
            releaseWakeLock()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
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
        super.onDestroy()
    }
}