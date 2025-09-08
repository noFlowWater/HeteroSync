package dacslab.heterosync.ui.common

import dacslab.heterosync.core.data.DeviceInfo

sealed class AppState {
    object Loading : AppState()
    object DeviceInput : AppState()
    data class DeviceConfirmation(val deviceInfo: DeviceInfo, val serverIp: String, val serverPort: Int) : AppState()
    data class Connected(val deviceInfo: DeviceInfo, val serverIp: String, val serverPort: Int) : AppState()
    data class DeviceNotFound(val serverIp: String, val serverPort: Int, val deviceIp: String, val devicePort: Int) : AppState()
    data class DeviceRegistration(val serverIp: String, val serverPort: Int, val deviceIp: String, val devicePort: Int) : AppState()
    data class DeviceUpdate(val serverIp: String, val serverPort: Int, val deviceInfo: DeviceInfo, val isPortConflict: Boolean = false, val suggestedPort: Int? = null) : AppState()
    data class ClientServerRunning(val deviceInfo: DeviceInfo, val serverIp: String, val serverPort: Int, val clientServerPort: Int) : AppState()
    data class Error(val message: String, val serverIp: String? = null, val serverPort: Int? = null, val deviceIp: String? = null, val devicePort: Int? = null) : AppState()
}