package dacslab.heterosync.ui.mobile.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dacslab.heterosync.core.data.DeviceInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceUpdateScreen(
    serverIp: String,
    serverPort: Int,
    deviceInfo: DeviceInfo,
    onUpdateDevice: (deviceName: String, deviceOs: String, deviceIp: String, devicePort: Int) -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit,
    isPortConflict: Boolean = false,
    suggestedPort: Int? = null
) {
    var deviceName by remember { mutableStateOf(deviceInfo.device_name) }
    var deviceOs by remember { mutableStateOf(deviceInfo.device_os) }
    var deviceIp by remember { mutableStateOf(deviceInfo.device_ip) }
    var devicePortText by remember { mutableStateOf(suggestedPort?.toString() ?: deviceInfo.device_port.toString()) }
    var isUpdating by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isPortConflict) "포트 충돌 - 수정" else "디바이스 수정") },
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
                text = if (isPortConflict) "포트 충돌" else "디바이스 정보 수정",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPortConflict) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            
            if (isPortConflict) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "포트 충돌 알림",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "설정된 포트가 이미 사용 중입니다. 사용 가능한 포트로 변경해주세요.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

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
                        text = "서버 정보",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("서버: $serverIp:$serverPort", fontSize = 14.sp)
                }
            }

            OutlinedTextField(
                value = deviceName,
                onValueChange = { deviceName = it },
                label = { Text("디바이스 이름") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isUpdating,
                singleLine = true
            )
            
            OutlinedTextField(
                value = deviceOs,
                onValueChange = { deviceOs = it },
                label = { Text("운영체제 (예: Android, iOS)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isUpdating,
                singleLine = true
            )
            
            OutlinedTextField(
                value = deviceIp,
                onValueChange = { deviceIp = it },
                label = { Text("디바이스 IP 주소") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isUpdating,
                singleLine = true
            )
            
            OutlinedTextField(
                value = devicePortText,
                onValueChange = { devicePortText = it },
                label = { Text("디바이스 포트") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isUpdating,
                singleLine = true
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val port = devicePortText.toIntOrNull()
                        if (deviceName.isNotBlank() && deviceOs.isNotBlank() && 
                            deviceIp.isNotBlank() && port != null && port > 0) {
                            isUpdating = true
                            onUpdateDevice(deviceName, deviceOs, deviceIp, port)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUpdating && deviceName.isNotBlank() && deviceOs.isNotBlank() && 
                             deviceIp.isNotBlank() && devicePortText.toIntOrNull() != null && 
                             devicePortText.toIntOrNull()!! > 0
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("수정 중...")
                    } else {
                        Text("디바이스 정보 수정")
                    }
                }
                
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUpdating
                ) {
                    Text("취소")
                }
            }
        }
    }
}