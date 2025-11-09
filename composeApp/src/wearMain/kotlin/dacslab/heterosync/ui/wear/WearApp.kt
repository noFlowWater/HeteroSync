package dacslab.heterosync.ui.wear

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.*
import dacslab.heterosync.ui.common.AppState
import dacslab.heterosync.ui.wear.screens.ConnectedScreen
import dacslab.heterosync.ui.wear.screens.DeviceIdSettingScreen
import dacslab.heterosync.ui.wear.screens.DeviceInputScreen
import dacslab.heterosync.ui.wear.screens.ErrorScreen
import dacslab.heterosync.ui.wear.screens.LoadingScreen
import kotlinx.coroutines.launch

@Composable
fun WearApp() {
    MaterialTheme {
        val context = LocalContext.current
        val viewModel = remember { WearAppViewModel(context) }
        val state by viewModel.state.collectAsState()
        val showDeviceIdSetting by viewModel.showDeviceIdSetting.collectAsState()

        // Cleanup on dispose
        DisposableEffect(Unit) {
            onDispose {
                viewModel.cleanup()
            }
        }

        // WearOS back gesture handling
        BackHandler {
            if (showDeviceIdSetting) {
                viewModel.hideDeviceIdSetting()
            } else if (!viewModel.navigateBack()) {
                // First screen - allow app exit
            }
        }

        Scaffold(
            timeText = { TimeText() },
            vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
        ) {
            if (showDeviceIdSetting) {
                DeviceIdSettingScreen(
                    currentDeviceId = viewModel.getSavedDeviceId(),
                    onBack = { viewModel.hideDeviceIdSetting() },
                    onDeviceIdSelected = { deviceId ->
                        viewModel.saveDeviceId(deviceId)
                    }
                )
            } else {
                when (val currentState = state) {
                    is AppState.Loading -> {
                        LoadingScreen()
                    }
                    is AppState.DeviceInput -> {
                        DeviceInputScreen(
                            savedDeviceId = viewModel.getSavedDeviceId(),
                            onQuickConnect = { serverIp, serverPort, deviceType ->
                                viewModel.connectToServer(serverIp, serverPort, deviceType)
                            },
                            onDeviceIdSettingClick = {
                                viewModel.showDeviceIdSetting()
                            }
                        )
                    }
                    is AppState.Connected -> {
                        ConnectedScreen(
                            deviceInfo = currentState.deviceInfo,
                            serverIp = currentState.serverIp,
                            serverPort = currentState.serverPort,
                            isWebSocketConnected = true,  // Service is running
                            webSocketDeviceId = currentState.deviceInfo.deviceId,
                            connectionStatus = "연결됨",
                            connectionHealth = currentState.connectionHealth,
                            lastError = currentState.lastError,
                            onDisconnect = {
                                viewModel.disconnectWebSocket()
                            }
                        )
                    }
                    is AppState.Error -> {
                        ErrorScreen(
                            message = currentState.message,
                            onRetry = { viewModel.resetToInput() }
                        )
                    }
                }
            }
        }
    }
}
