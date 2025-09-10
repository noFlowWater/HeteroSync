package dacslab.heterosync.ui.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import dacslab.heterosync.ui.common.AppState
import dacslab.heterosync.ui.common.AppViewModel
import dacslab.heterosync.ui.mobile.screens.DeviceRegistrationScreen
import dacslab.heterosync.ui.mobile.screens.DeviceUpdateScreen
import dacslab.heterosync.ui.mobile.screens.ClientServerStartScreen
import dacslab.heterosync.ui.mobile.screens.ClientServerRunningScreen
import dacslab.heterosync.ui.mobile.screens.DeviceConfirmationScreen
import dacslab.heterosync.ui.mobile.screens.DeviceInputScreen
import dacslab.heterosync.ui.mobile.screens.ErrorScreen
import dacslab.heterosync.ui.mobile.screens.LoadingScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileApp() {
    MaterialTheme {
        val viewModel = remember { AppViewModel() }
        val state by viewModel.state.collectAsState()
        val scope = rememberCoroutineScope()
        
        // Android 뒤로가기 버튼 핸들링
        BackHandler {
            if (!viewModel.navigateBack()) {
                // 첫 화면에서 뒤로가기 시 앱 종료 (기본 동작)
            }
        }
        
        when (val currentState = state) {
            is AppState.Loading -> {
                LoadingScreen(
                    onBack = { viewModel.navigateBack() }
                )
            }
            is AppState.DeviceInput -> {
                DeviceInputScreen(
                    onDeviceCheck = { serverIp, serverPort, deviceIp, devicePort ->
                        scope.launch {
                            viewModel.checkDevice(
                                serverIp,
                                serverPort,
                                deviceIp,
                                devicePort
                            )
                        }
                    }
                )
            }
            is AppState.DeviceConfirmation -> {
                DeviceConfirmationScreen(
                    deviceInfo = currentState.deviceInfo,
                    onConfirm = { viewModel.confirmDevice(currentState.deviceInfo, currentState.serverIp, currentState.serverPort) },
                    onCancel = { viewModel.resetToInput() },
                    onBack = { viewModel.navigateBack() },
                    onUpdateDevice = {
                        viewModel.navigateToDeviceUpdate(
                            currentState.serverIp,
                            currentState.serverPort,
                            currentState.deviceInfo
                        )
                    }
                )
            }
            is AppState.Connected -> {
                ClientServerStartScreen(
                    deviceInfo = currentState.deviceInfo,
                    serverIp = currentState.serverIp,
                    serverPort = currentState.serverPort,
                    onBack = { viewModel.navigateBack() },
                    onStartServer = {
                        viewModel.startClientServer(
                            currentState.deviceInfo,
                            currentState.serverIp,
                            currentState.serverPort
                        )
                    }
                )
            }
            is AppState.DeviceNotFound -> {
                ErrorScreen(
                    message = "디바이스를 찾을 수 없습니다",
                    onRetry = { viewModel.resetToInput() },
                    onRegisterNewDevice = {
                        viewModel.navigateToDeviceRegistration(
                            currentState.serverIp,
                            currentState.serverPort,
                            currentState.deviceIp,
                            currentState.devicePort
                        )
                    }
                )
            }
            is AppState.Error -> {
                ErrorScreen(
                    message = currentState.message,
                    onRetry = { viewModel.resetToInput() },
                    onRegisterNewDevice = if (currentState.serverIp != null && currentState.serverPort != null &&
                                             currentState.deviceIp != null && currentState.devicePort != null) {
                        {
                            viewModel.navigateToDeviceRegistration(
                                currentState.serverIp,
                                currentState.serverPort,
                                currentState.deviceIp,
                                currentState.devicePort
                            )
                        }
                    } else null
                )
            }

            is AppState.DeviceRegistration -> {
                DeviceRegistrationScreen(
                    serverIp = currentState.serverIp,
                    serverPort = currentState.serverPort,
                    deviceIp = currentState.deviceIp,
                    devicePort = currentState.devicePort,
                    onRegisterDevice = { deviceName, deviceOs ->
                        scope.launch {
                            viewModel.registerDevice(
                                currentState.serverIp,
                                currentState.serverPort,
                                currentState.deviceIp,
                                currentState.devicePort,
                                deviceName,
                                deviceOs
                            )
                        }
                    },
                    onCancel = { viewModel.resetToInput() },
                    onBack = { viewModel.navigateBack() }
                )
            }
            is AppState.DeviceUpdate -> {
                DeviceUpdateScreen(
                    serverIp = currentState.serverIp,
                    serverPort = currentState.serverPort,
                    deviceInfo = currentState.deviceInfo,
                    onUpdateDevice = { deviceName, deviceOs, deviceIp, devicePort ->
                        scope.launch {
                            viewModel.updateDevice(
                                currentState.serverIp,
                                currentState.serverPort,
                                currentState.deviceInfo.device_ip,
                                currentState.deviceInfo.device_port,
                                deviceName,
                                deviceOs,
                                deviceIp,
                                devicePort
                            )
                        }
                    },
                    onCancel = { viewModel.navigateBack() },
                    onBack = { viewModel.navigateBack() },
                    isPortConflict = currentState.isPortConflict,
                    suggestedPort = currentState.suggestedPort
                )
            }
            is AppState.ClientServerRunning -> {
                ClientServerRunningScreen(
                    deviceInfo = currentState.deviceInfo,
                    serverIp = currentState.serverIp,
                    serverPort = currentState.serverPort,
                    clientServerPort = currentState.clientServerPort,
                    onStop = { viewModel.stopClientServer() },
                    onBack = { viewModel.navigateBack() }
                )
            }
        }
    }
}


