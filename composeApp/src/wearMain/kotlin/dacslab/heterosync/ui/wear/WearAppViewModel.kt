package dacslab.heterosync.ui.wear

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import dacslab.heterosync.core.data.ConnectionHealth
import dacslab.heterosync.core.data.DeviceInfo
import dacslab.heterosync.core.service.WebSocketForegroundService
import dacslab.heterosync.core.utils.getDeviceUniqueId
import dacslab.heterosync.ui.common.AppState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WearAppViewModel(private val context: Context) {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _state = MutableStateFlow<AppState>(AppState.DeviceInput)
    val state: StateFlow<AppState> = _state.asStateFlow()

    private var webSocketService: WebSocketForegroundService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as WebSocketForegroundService.LocalBinder
            webSocketService = binder.getService()
            isBound = true

            // Observe service state
            viewModelScope.launch {
                webSocketService?.connectionState?.collect { connectionState ->
                    handleServiceConnectionState(connectionState)
                }
            }

            // Observe connection health
            viewModelScope.launch {
                webSocketService?.connectionHealth?.collect { health ->
                    updateConnectionHealth(health)
                }
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            webSocketService = null
        }
    }

    private fun handleServiceConnectionState(connectionState: WebSocketForegroundService.ConnectionState) {
        when (connectionState) {
            is WebSocketForegroundService.ConnectionState.Connected -> {
                updateWebSocketConnectedState(true, connectionState.deviceId, "연결됨")
            }
            is WebSocketForegroundService.ConnectionState.Connecting -> {
                updateWebSocketConnectedState(false, null, "연결 중...")
            }
            is WebSocketForegroundService.ConnectionState.Reconnecting -> {
                updateWebSocketConnectedState(false, null, "재연결 중... (${connectionState.attempt}/5)")
            }
            is WebSocketForegroundService.ConnectionState.Disconnected -> {
                updateWebSocketConnectedState(false, null, "연결 끊김")
            }
            is WebSocketForegroundService.ConnectionState.Error -> {
                updateWebSocketConnectedState(false, null, "에러: ${connectionState.message}")
                updateErrorMessage(connectionState.message)
            }
        }
    }

    private fun updateWebSocketConnectedState(isConnected: Boolean, deviceId: String?, statusMessage: String?) {
        when (val currentState = _state.value) {
            is AppState.Connected -> {
                _state.value = currentState.copy(
                    isWebSocketConnected = isConnected,
                    webSocketDeviceId = deviceId,
                    connectionStatus = statusMessage ?: if (isConnected) "연결됨" else "연결 끊김"
                )
            }
            else -> {}
        }
    }

    private fun updateErrorMessage(error: String) {
        when (val currentState = _state.value) {
            is AppState.Connected -> {
                _state.value = currentState.copy(
                    lastError = error
                )
            }
            else -> {}
        }
    }

    private fun updateConnectionHealth(health: ConnectionHealth) {
        when (val currentState = _state.value) {
            is AppState.Connected -> {
                _state.value = currentState.copy(
                    connectionHealth = health
                )
            }
            else -> {}
        }
    }

    fun connectToServer(
        serverIp: String,
        serverPort: Int,
        deviceType: String
    ) {
        _state.value = AppState.Loading

        // Get unique device ID
        val deviceId = getDeviceUniqueId()

        val tempDeviceInfo = DeviceInfo(
            deviceId = deviceId,
            deviceType = deviceType
        )

        _state.value = AppState.Connected(tempDeviceInfo, serverIp, serverPort)

        // Start foreground service
        val serviceIntent = Intent(context, WebSocketForegroundService::class.java).apply {
            action = WebSocketForegroundService.ACTION_START_SERVICE
            putExtra(WebSocketForegroundService.EXTRA_SERVER_IP, serverIp)
            putExtra(WebSocketForegroundService.EXTRA_SERVER_PORT, serverPort)
            putExtra(WebSocketForegroundService.EXTRA_DEVICE_TYPE, deviceType)
            putExtra(WebSocketForegroundService.EXTRA_DEVICE_ID, deviceId)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // Bind to service
        val bindIntent = Intent(context, WebSocketForegroundService::class.java)
        context.bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun disconnectWebSocket() {
        webSocketService?.disconnectFromServer()

        // Stop service
        val serviceIntent = Intent(context, WebSocketForegroundService::class.java).apply {
            action = WebSocketForegroundService.ACTION_STOP_SERVICE
        }
        context.startService(serviceIntent)

        // Unbind
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
        }
    }

    fun resetToInput() {
        _state.value = AppState.DeviceInput
    }

    fun showError(message: String) {
        _state.value = AppState.Error(message)
    }

    fun navigateBack(): Boolean {
        return when (val currentState = _state.value) {
            is AppState.Loading -> {
                _state.value = AppState.DeviceInput
                true
            }
            is AppState.Connected -> {
                disconnectWebSocket()
                _state.value = AppState.DeviceInput
                true
            }
            is AppState.Error -> {
                _state.value = AppState.DeviceInput
                true
            }
            is AppState.DeviceInput -> {
                false
            }
        }
    }

    fun cleanup() {
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
        }
    }
}
