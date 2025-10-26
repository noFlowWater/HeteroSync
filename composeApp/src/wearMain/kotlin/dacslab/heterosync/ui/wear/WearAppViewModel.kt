package dacslab.heterosync.ui.wear

import android.content.Context
import android.content.Intent
import android.os.Build
import dacslab.heterosync.core.data.DeviceInfo
import dacslab.heterosync.core.service.WebSocketForegroundService
import dacslab.heterosync.core.utils.DevicePreferences
import dacslab.heterosync.core.utils.getDeviceUniqueId
import dacslab.heterosync.ui.common.AppState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WearAppViewModel(private val context: Context) {
    private val devicePreferences = DevicePreferences(context)

    private val _state = MutableStateFlow<AppState>(AppState.DeviceInput)
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val _showDeviceIdSetting = MutableStateFlow(false)
    val showDeviceIdSetting: StateFlow<Boolean> = _showDeviceIdSetting.asStateFlow()

    fun getSavedDeviceId(): String? {
        return devicePreferences.getDeviceId()
    }

    fun saveDeviceId(deviceId: String) {
        devicePreferences.saveDeviceId(deviceId)
    }

    fun showDeviceIdSetting() {
        _showDeviceIdSetting.value = true
    }

    fun hideDeviceIdSetting() {
        _showDeviceIdSetting.value = false
    }

    fun connectToServer(
        serverIp: String,
        serverPort: Int,
        deviceType: String
    ) {
        _state.value = AppState.Loading

        // Use saved device ID, or generate one
        val deviceId = devicePreferences.getDeviceId() ?: getDeviceUniqueId()

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
    }

    fun disconnectWebSocket() {
        // Stop service
        val serviceIntent = Intent(context, WebSocketForegroundService::class.java).apply {
            action = WebSocketForegroundService.ACTION_STOP_SERVICE
        }
        context.startService(serviceIntent)
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
        // No longer needed - no service binding
    }
}
