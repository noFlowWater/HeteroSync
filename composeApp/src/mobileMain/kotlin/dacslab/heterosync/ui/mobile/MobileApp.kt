package dacslab.heterosync.ui.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import dacslab.heterosync.ui.common.AppState
import dacslab.heterosync.ui.common.AppViewModel
import dacslab.heterosync.ui.mobile.screens.ConnectedScreen
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

        // Android back button handling
        BackHandler {
            if (!viewModel.navigateBack()) {
                // First screen - allow app exit (default behavior)
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (val currentState = state) {
                is AppState.Loading -> {
                    LoadingScreen()
                }
                is AppState.DeviceInput -> {
                    DeviceInputScreen(
                        onDeviceCheck = { serverIp, serverPort, deviceType ->
                            scope.launch {
                                viewModel.connectToServer(
                                    serverIp,
                                    serverPort,
                                    deviceType
                                )
                            }
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
                        onDisconnect = {
                            scope.launch {
                                viewModel.disconnectWebSocket()
                                viewModel.resetToInput()
                            }
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