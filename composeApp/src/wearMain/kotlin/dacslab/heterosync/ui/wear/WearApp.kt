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

        // Cleanup on dispose
        DisposableEffect(Unit) {
            onDispose {
                viewModel.cleanup()
            }
        }

        // WearOS back gesture handling
        BackHandler {
            if (!viewModel.navigateBack()) {
                // First screen - allow app exit
            }
        }

        Scaffold(
            timeText = { TimeText() },
            vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
        ) {
            when (val currentState = state) {
                is AppState.Loading -> {
                    LoadingScreen()
                }
                is AppState.DeviceInput -> {
                    DeviceInputScreen(
                        onQuickConnect = { serverIp, serverPort, deviceType ->
                            viewModel.connectToServer(serverIp, serverPort, deviceType)
                        }
                    )
                }
                is AppState.Connected -> {
                    ConnectedScreen(
                        deviceInfo = currentState.deviceInfo,
                        serverIp = currentState.serverIp,
                        serverPort = currentState.serverPort,
                        isWebSocketConnected = currentState.isWebSocketConnected,
                        webSocketDeviceId = currentState.webSocketDeviceId,
                        connectionStatus = currentState.connectionStatus,
                        connectionHealth = currentState.connectionHealth,
                        lastError = currentState.lastError,
                        onDisconnect = {
                            viewModel.disconnectWebSocket()
                            viewModel.resetToInput()
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
