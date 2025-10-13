package dacslab.heterosync.ui.common

import dacslab.heterosync.core.data.DeviceInfo
import dacslab.heterosync.core.network.DeviceWebSocketService
import dacslab.heterosync.core.utils.getDeviceUniqueId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppViewModel {
    private val webSocketService = DeviceWebSocketService()

    private val _state = MutableStateFlow<AppState>(AppState.DeviceInput)
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        // WebSocket connection state callbacks
        webSocketService.onConnected = { deviceId, serverTime ->
            updateWebSocketConnectedState(true, deviceId)
        }

        webSocketService.onDisconnected = {
            updateWebSocketConnectedState(false, null)
        }

        webSocketService.onError = { error ->
            println("WebSocket error: $error")
        }
    }

    private fun updateWebSocketConnectedState(isConnected: Boolean, deviceId: String?) {
        when (val currentState = _state.value) {
            is AppState.Connected -> {
                _state.value = currentState.copy(
                    isWebSocketConnected = isConnected,
                    webSocketDeviceId = deviceId
                )
            }
            else -> {}
        }
    }
    
    suspend fun connectToServer(
        serverIp: String,
        serverPort: Int,
        deviceType: String
    ) {
        _state.value = AppState.Loading

        // Get unique device ID based on hostname-username
        val deviceId = getDeviceUniqueId()

        val tempDeviceInfo = DeviceInfo(
            deviceId = deviceId,
            deviceType = deviceType
        )

        _state.value = AppState.Connected(tempDeviceInfo, serverIp, serverPort)

        // WebSocket connection attempt
        webSocketService.connectToServer(
            serverIp = serverIp,
            serverPort = serverPort,
            deviceType = deviceType,
            deviceId = deviceId
        )
    }

    suspend fun disconnectWebSocket() {
        webSocketService.disconnect()
    }

    fun resetToInput() {
        _state.value = AppState.DeviceInput
    }

    fun showError(message: String) {
        _state.value = AppState.Error(message)
    }
    
    // 뒤로가기 네비게이션 처리
    fun navigateBack(): Boolean {
        return when (val currentState = _state.value) {
            is AppState.Loading -> {
                // 로딩 중일 때는 DeviceInput으로 돌아감
                _state.value = AppState.DeviceInput
                true
            }
            is AppState.Connected -> {
                // 연결된 상태에서는 입력 화면으로
                _state.value = AppState.DeviceInput
                true
            }
            is AppState.Error -> {
                // 에러 화면에서는 입력 화면으로
                _state.value = AppState.DeviceInput
                true
            }
            is AppState.DeviceInput -> {
                // 첫 화면에서는 뒤로가기 불가 (앱 종료)
                false
            }
        }
    }
}