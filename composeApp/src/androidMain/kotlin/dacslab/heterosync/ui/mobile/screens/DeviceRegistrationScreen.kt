package dacslab.heterosync.ui.mobile.screens

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceRegistrationScreen(
    serverIp: String,
    serverPort: Int,
    deviceIp: String,
    devicePort: Int,
    onRegisterDevice: (deviceName: String, deviceOs: String) -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit
) {
    var deviceName by remember { mutableStateOf("") }
    var deviceOs by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("디바이스 등록") },
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
                text = "새 디바이스 등록",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "디바이스를 찾을 수 없습니다.\n새 디바이스를 등록하시겠습니까?",
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "디바이스 정보",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("서버: $serverIp:$serverPort", fontSize = 14.sp)
                    Text("디바이스: $deviceIp:$devicePort", fontSize = 14.sp)
                }
            }

            OutlinedTextField(
                value = deviceName,
                onValueChange = { deviceName = it },
                label = { Text("디바이스 이름") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRegistering,
                singleLine = true
            )
            
            OutlinedTextField(
                value = deviceOs,
                onValueChange = { deviceOs = it },
                label = { Text("운영체제 (예: Android, iOS)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRegistering,
                singleLine = true
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (deviceName.isNotBlank() && deviceOs.isNotBlank()) {
                            isRegistering = true
                            onRegisterDevice(deviceName, deviceOs)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isRegistering && deviceName.isNotBlank() && deviceOs.isNotBlank()
                ) {
                    if (isRegistering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("등록 중...")
                    } else {
                        Text("디바이스 등록")
                    }
                }
                
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isRegistering
                ) {
                    Text("취소")
                }
            }
        }
    }
}