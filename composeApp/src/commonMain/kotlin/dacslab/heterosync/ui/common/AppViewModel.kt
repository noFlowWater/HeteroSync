package dacslab.heterosync.ui.common

import dacslab.heterosync.core.data.DeviceInfo
import dacslab.heterosync.core.network.DeviceApiService
import dacslab.heterosync.core.data.GetDeviceRequest
import dacslab.heterosync.core.data.CreateDeviceRequest
import dacslab.heterosync.core.data.UpdateDeviceRequest
import dacslab.heterosync.core.server.ClientServerService
import dacslab.heterosync.core.utils.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import io.ktor.client.plugins.*
import io.ktor.http.*

class AppViewModel {
    private val deviceApiService = DeviceApiService()
    private val clientServerService = ClientServerService()
    private val networkUtils = NetworkUtils()
    
    private val _state = MutableStateFlow<AppState>(AppState.DeviceInput)
    val state: StateFlow<AppState> = _state.asStateFlow()
    
    suspend fun checkDevice(
        server_ip: String,
        server_port: Int,
        device_ip: String,
        device_port: Int,
    ) {
        _state.value = AppState.Loading
        
        val request = GetDeviceRequest(
            server_ip = server_ip,
            server_port = server_port,
            device_ip = device_ip,
            device_port = device_port
        )
        deviceApiService.getDevice(request)
            .onSuccess { deviceInfo ->
                _state.value = AppState.DeviceConfirmation(deviceInfo, server_ip, server_port)
            }
            .onFailure { exception ->
                // 404 에러인지 확인하여 디바이스 등록 화면으로 이동
                if (exception is ClientRequestException && exception.response.status == HttpStatusCode.NotFound) {
                    _state.value = AppState.DeviceRegistration(server_ip, server_port, device_ip, device_port)
                } else {
                    _state.value = AppState.DeviceNotFound(server_ip, server_port, device_ip, device_port)
                }
            }
    }
    
    fun confirmDevice(deviceInfo: DeviceInfo, serverIp: String, serverPort: Int) {
        _state.value = AppState.Connected(deviceInfo, serverIp, serverPort)
    }
    
    suspend fun registerDevice(
        serverIp: String,
        serverPort: Int,
        deviceIp: String,
        devicePort: Int,
        deviceName: String,
        deviceOs: String
    ) {
        _state.value = AppState.Loading
        
        val request = CreateDeviceRequest(
            server_ip = serverIp,
            server_port = serverPort,
            device_name = deviceName,
            device_os = deviceOs,
            device_ip = deviceIp,
            device_port = devicePort
        )
        
        deviceApiService.createDevice(request)
            .onSuccess { 
                // 등록 성공 후 디바이스 정보를 다시 조회
                checkDevice(serverIp, serverPort, deviceIp, devicePort)
            }
            .onFailure { exception ->
                _state.value = AppState.Error(
                    message = "디바이스 등록에 실패했습니다: ${exception.message}",
                    serverIp = serverIp,
                    serverPort = serverPort,
                    deviceIp = deviceIp,
                    devicePort = devicePort
                )
            }
    }
    
    suspend fun updateDevice(
        serverIp: String,
        serverPort: Int,
        currentDeviceIp: String,
        currentDevicePort: Int,
        deviceName: String,
        deviceOs: String,
        deviceIp: String,
        devicePort: Int
    ) {
        _state.value = AppState.Loading
        
        val request = UpdateDeviceRequest(
            server_ip = serverIp,
            server_port = serverPort,
            current_device_ip = currentDeviceIp,
            current_device_port = currentDevicePort,
            after_device_name = deviceName,
            after_device_os = deviceOs,
            after_device_ip = deviceIp,
            after_device_port = devicePort
        )
        
        deviceApiService.updateDevice(request)
            .onSuccess { 
                // 수정 성공 후 디바이스 정보를 다시 조회
                checkDevice(serverIp, serverPort, deviceIp, devicePort)
            }
            .onFailure { exception ->
                _state.value = AppState.Error(
                    message = "디바이스 수정에 실패했습니다: ${exception.message}",
                    serverIp = serverIp,
                    serverPort = serverPort,
                    deviceIp = deviceIp,
                    devicePort = devicePort
                )
            }
    }
    
    fun resetToInput() {
        _state.value = AppState.DeviceInput
    }
    
    fun navigateToDeviceRegistration(serverIp: String, serverPort: Int, deviceIp: String, devicePort: Int) {
        _state.value = AppState.DeviceRegistration(serverIp, serverPort, deviceIp, devicePort)
    }
    
    fun navigateToDeviceUpdate(serverIp: String, serverPort: Int, deviceInfo: DeviceInfo) {
        _state.value = AppState.DeviceUpdate(serverIp, serverPort, deviceInfo)
    }
    
    fun startClientServer(deviceInfo: DeviceInfo, serverIp: String, serverPort: Int) {
        _state.value = AppState.Loading
        
        // 현재 디바이스 포트가 사용 가능한지 확인
        if (!networkUtils.isPortAvailable(deviceInfo.device_port)) {
            // 포트가 사용 중이면 DeviceUpdate 화면으로 이동하여 포트 변경 요청
            val suggestedPort = networkUtils.getRandomAvailablePort()
//            val updatedDeviceInfo = deviceInfo.copy(device_port = suggestedPort)
            _state.value = AppState.DeviceUpdate(serverIp, serverPort, deviceInfo, isPortConflict = true, suggestedPort)
            return
        }
        
        clientServerService.startServer(
            port = deviceInfo.device_port,
            onStarted = { port ->
                _state.value = AppState.ClientServerRunning(
                    deviceInfo = deviceInfo,
                    serverIp = serverIp,
                    serverPort = serverPort,
                    clientServerPort = port
                )
            },
            onError = { exception ->
                _state.value = AppState.Error(
                    message = "클라이언트 서버 시작 실패: ${exception.message}",
                    serverIp = serverIp,
                    serverPort = serverPort,
                    deviceIp = deviceInfo.device_ip,
                    devicePort = deviceInfo.device_port
                )
            }
        )
    }
    
    fun stopClientServer() {
        clientServerService.stopServer()
        when (val currentState = _state.value) {
            is AppState.ClientServerRunning -> {
                _state.value = AppState.Connected(
                    currentState.deviceInfo,
                    currentState.serverIp,
                    currentState.serverPort
                )
            }
            else -> {
                _state.value = AppState.DeviceInput
            }
        }
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
            is AppState.DeviceConfirmation -> {
                // 디바이스 확인 화면에서는 입력 화면으로
                _state.value = AppState.DeviceInput
                true
            }
            is AppState.Connected -> {
                // 연결된 상태에서는 확인 화면으로 (선택사항)
                _state.value = AppState.DeviceConfirmation(currentState.deviceInfo, currentState.serverIp, currentState.serverPort)
                true
            }
            is AppState.DeviceNotFound, is AppState.Error, is AppState.DeviceRegistration -> {
                // 에러 화면이나 등록 화면에서는 입력 화면으로
                _state.value = AppState.DeviceInput
                true
            }
            is AppState.DeviceUpdate -> {
                // 디바이스 수정 화면에서는 확인 화면으로
                if (currentState.isPortConflict) {
                    // 포트 충돌로 인한 수정인 경우 Connected 화면으로
                    _state.value = AppState.Connected(currentState.deviceInfo, currentState.serverIp, currentState.serverPort)
                } else {
                    // 일반 수정인 경우 확인 화면으로
                    _state.value = AppState.DeviceConfirmation(currentState.deviceInfo, currentState.serverIp, currentState.serverPort)
                }
                true
            }
            is AppState.ClientServerRunning -> {
                // 클라이언트 서버 실행 중에서는 연결 화면으로
                stopClientServer()
                true
            }
            is AppState.DeviceInput -> {
                // 첫 화면에서는 뒤로가기 불가 (앱 종료)
                false
            }
        }
    }
}