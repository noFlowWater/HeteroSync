package dacslab.heterosync.ui.wear

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import dacslab.heterosync.core.data.DeviceInfo
import dacslab.heterosync.ui.common.AppState
import dacslab.heterosync.ui.common.AppViewModel
import kotlinx.coroutines.launch

@Composable
fun WearApp() {
    MaterialTheme {
        val viewModel = remember { AppViewModel() }
        val state by viewModel.state.collectAsState()
        val listState = rememberScalingLazyListState()
        val scope = rememberCoroutineScope()
        
        // WearOS 뒤로가기 처리 (Edge swipe gesture 과 함께 작동)
        BackHandler {
            if (!viewModel.navigateBack()) {
                // 첫 화면에서 뒤로가기 시 앱 종료
            }
        }
        
        Scaffold(
            timeText = { TimeText() },
            vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
        ) {
            when (val currentState = state) {
                is AppState.Loading -> {
                    LoadingScreen(
                        listState = listState,
                        onBack = { viewModel.navigateBack() }
                    )
                }
                is AppState.DeviceInput -> {
                    DeviceInputScreen(
                        listState = listState,
                        onQuickConnect = { ip, port ->
                            scope.launch {
                                viewModel.checkDevice(ip, port)
                            }
                        }
                    )
                }
                is AppState.DeviceConfirmation -> {
                    DeviceConfirmationScreen(
                        listState = listState,
                        deviceInfo = currentState.deviceInfo,
                        onConfirm = { viewModel.confirmDevice(currentState.deviceInfo) },
                        onCancel = { viewModel.resetToInput() },
                        onBack = { viewModel.navigateBack() }
                    )
                }
                is AppState.Connected -> {
                    ConnectedScreen(
                        listState = listState,
                        deviceInfo = currentState.deviceInfo,
                        onBack = { viewModel.navigateBack() }
                    )
                }
                is AppState.DeviceNotFound, is AppState.Error -> {
                    ErrorScreen(
                        listState = listState,
                        onRetry = { viewModel.resetToInput() },
                        onBack = { viewModel.navigateBack() }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen(
    listState: ScalingLazyListState,
    onBack: () -> Unit
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "연결 중...",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.title2
            )
        }
        
        item {
            CircularProgressIndicator()
        }
        
        item {
            Chip(
                onClick = onBack,
                label = { Text("취소") },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
    }
}

@Composable
private fun DeviceInputScreen(
    listState: ScalingLazyListState,
    onQuickConnect: (String, Int) -> Unit
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "HeteroSync",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.title1
            )
        }
        
        item {
            Text(
                text = "디바이스 연결",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.body1
            )
        }
        
        item {
            Chip(
                onClick = { onQuickConnect("192.168.1.100", 8080) },
                label = { Text("테스트 디바이스 1") },
                colors = ChipDefaults.primaryChipColors()
            )
        }
        
        item {
            Chip(
                onClick = { onQuickConnect("192.168.1.101", 8081) },
                label = { Text("테스트 디바이스 2") },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
    }
}

@Composable
private fun DeviceConfirmationScreen(
    listState: ScalingLazyListState,
    deviceInfo: DeviceInfo,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "디바이스 확인",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.title2
            )
        }
        
        item {
            Card(
                onClick = { }
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = deviceInfo.device_name,
                        style = MaterialTheme.typography.body1
                    )
                    Text(
                        text = deviceInfo.device_os,
                        style = MaterialTheme.typography.body2
                    )
                }
            }
        }
        
        item {
            Chip(
                onClick = onConfirm,
                label = { Text("연결") },
                colors = ChipDefaults.primaryChipColors()
            )
        }
        
        item {
            Chip(
                onClick = onBack,
                label = { Text("뒤로") },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
    }
}

@Composable
private fun ConnectedScreen(
    listState: ScalingLazyListState,
    deviceInfo: DeviceInfo,
    onBack: () -> Unit
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "연결됨",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.title1
            )
        }
        
        item {
            Card(
                onClick = { }
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = deviceInfo.device_name,
                        style = MaterialTheme.typography.body1
                    )
                    Text(
                        text = "동기화 준비완료",
                        style = MaterialTheme.typography.body2
                    )
                }
            }
        }
        
        item {
            Chip(
                onClick = {
                    // TODO: 동기화 시작 로직 구현
                },
                label = { Text("동기화") },
                colors = ChipDefaults.primaryChipColors()
            )
        }
        
        item {
            Chip(
                onClick = onBack,
                label = { Text("뒤로") },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
    }
}

@Composable
private fun ErrorScreen(
    listState: ScalingLazyListState,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "연결 실패",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.title2
            )
        }
        
        item {
            Text(
                text = "디바이스를 찾을 수 없습니다",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.body1
            )
        }
        
        item {
            Chip(
                onClick = onRetry,
                label = { Text("다시 시도") },
                colors = ChipDefaults.primaryChipColors()
            )
        }
        
        item {
            Chip(
                onClick = onBack,
                label = { Text("뒤로") },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
    }
}