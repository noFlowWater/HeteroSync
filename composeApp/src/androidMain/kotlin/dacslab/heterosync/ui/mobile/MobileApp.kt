package dacslab.heterosync.ui.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dacslab.heterosync.core.data.DeviceInfo
import dacslab.heterosync.ui.common.AppState
import dacslab.heterosync.ui.common.AppViewModel
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
                    onDeviceCheck = { ip, port ->
                        scope.launch {
                            viewModel.checkDevice(ip, port)
                        }
                    }
                )
            }
            is AppState.DeviceConfirmation -> {
                DeviceConfirmationScreen(
                    deviceInfo = currentState.deviceInfo,
                    onConfirm = { viewModel.confirmDevice(currentState.deviceInfo) },
                    onCancel = { viewModel.resetToInput() },
                    onBack = { viewModel.navigateBack() }
                )
            }
            is AppState.Connected -> {
                ConnectedScreen(
                    deviceInfo = currentState.deviceInfo,
                    onBack = { viewModel.navigateBack() }
                )
            }
            is AppState.DeviceNotFound -> {
                ErrorScreen(
                    message = "디바이스를 찾을 수 없습니다",
                    onRetry = { viewModel.resetToInput() }
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

@Composable
private fun LoadingScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("연결 중...") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "뒤로가기")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text("디바이스를 찾는 중...")
            }
        }
    }
}

@Composable
private fun DeviceInputScreen(
    onDeviceCheck: (String, Int) -> Unit
) {
    var ip by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = "HeteroSync",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "연결할 디바이스 정보를 입력하세요",
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        
        OutlinedTextField(
            value = ip,
            onValueChange = { ip = it },
            label = { Text("IP 주소") },
            modifier = Modifier.fillMaxWidth()
        )
        
        OutlinedTextField(
            value = port,
            onValueChange = { port = it },
            label = { Text("포트 번호") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Button(
            onClick = {
                val portInt = port.toIntOrNull() ?: 8080
                onDeviceCheck(ip, portInt)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = ip.isNotBlank()
        ) {
            Text("디바이스 찾기", fontSize = 18.sp)
        }
    }
}

@Composable
private fun DeviceConfirmationScreen(
    deviceInfo: DeviceInfo,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("디바이스 확인") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "뒤로가기")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
        Text(
            text = "디바이스를 찾았습니다",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "디바이스명: ${deviceInfo.device_name}",
                    fontSize = 16.sp
                )
                Text(
                    text = "운영체제: ${deviceInfo.device_os}",
                    fontSize = 16.sp
                )
                Text(
                    text = "주소: ${deviceInfo.device_ip}:${deviceInfo.device_port}",
                    fontSize = 16.sp
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("취소")
            }
            
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f)
            ) {
                Text("연결")
            }
        }
    }
}

@Composable
private fun ConnectedScreen(
    deviceInfo: DeviceInfo,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("연결 완료") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "뒤로가기")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Text(
                text = "연결 완료",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "연결된 디바이스",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${deviceInfo.device_name} (${deviceInfo.device_os})",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${deviceInfo.device_ip}:${deviceInfo.device_port}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Button(
                onClick = {
                    // TODO: 동기화 시작 로직 구현
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("동기화 시작", fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun ErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = "오류",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        
        Text(
            text = message,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("다시 시도")
        }
    }
}