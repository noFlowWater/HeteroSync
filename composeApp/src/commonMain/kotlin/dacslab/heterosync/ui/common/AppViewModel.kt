package dacslab.heterosync.ui.common

import dacslab.heterosync.core.data.ConnectionHealth
import dacslab.heterosync.core.data.DeviceInfo
import dacslab.heterosync.core.network.DeviceWebSocketService
import dacslab.heterosync.core.utils.getDeviceUniqueId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel {
    private val webSocketService = DeviceWebSocketService()
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _state = MutableStateFlow<AppState>(AppState.DeviceInput)
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        // WebSocket connection state callbacks
        webSocketService.onConnected = { deviceId, serverTime ->
            updateWebSocketConnectedState(true, deviceId, null)
        }

        webSocketService.onDisconnected = {
            updateWebSocketConnectedState(false, null, null)
        }

        webSocketService.onError = { error ->
            println("WebSocket error: $error")
            updateErrorMessage(error)
        }

        webSocketService.onReconnecting = { attempt ->
            println("Reconnection attempt: $attempt")
            updateWebSocketConnectedState(false, null, "재연결 중... ($attempt/5)")
        }

        webSocketService.onHealthChanged = { health ->
            println("Connection health changed: $health")
            updateConnectionHealth(health)
        }

        // Observe connection health flow
        viewModelScope.launch {
            webSocketService.connectionHealth.collect { health ->
                updateConnectionHealth(health)
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
    suspend fun navigateBack(): Boolean {
        return when (val currentState = _state.value) {
            is AppState.Loading -> {
                // 로딩 중일 때는 DeviceInput으로 돌아감
                _state.value = AppState.DeviceInput
                true
            }
            is AppState.Connected -> {
                // 연결된 상태에서는 입력 화면으로
                webSocketService.disconnect()
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
