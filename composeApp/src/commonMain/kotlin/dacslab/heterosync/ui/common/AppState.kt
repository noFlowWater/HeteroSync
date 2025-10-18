package dacslab.heterosync.ui.common

import dacslab.heterosync.core.data.ConnectionHealth
import dacslab.heterosync.core.data.DeviceInfo

sealed class AppState {

    object Loading : AppState()

    object DeviceInput : AppState()

    data class Connected(
        val deviceInfo: DeviceInfo,
        val serverIp: String,
        val serverPort: Int,
        val isWebSocketConnected: Boolean = false,
        val webSocketDeviceId: String? = null,
        val connectionStatus: String = "연결 중...",
        val connectionHealth: ConnectionHealth = ConnectionHealth.UNKNOWN,
        val lastError: String? = null
    ) : AppState()

    data class Error(
        val message: String
    ) : AppState()
}
