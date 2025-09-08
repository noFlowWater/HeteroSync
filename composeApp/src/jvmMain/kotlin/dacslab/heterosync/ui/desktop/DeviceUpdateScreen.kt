package dacslab.heterosync.ui.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        Text(
            text = if (isPortConflict) "포트 충돌 - 디바이스 정보 수정" else "디바이스 정보 수정",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = if (isPortConflict) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (isPortConflict) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "현재 설정된 포트가 이미 사용 중입니다. 사용 가능한 포트로 변경해주세요. 추천 포트가 자동으로 설정되었습니다.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Text(
            text = if (isPortConflict) "포트를 변경하여 디바이스 정보를 업데이트하세요" else "디바이스 정보를 수정하세요",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Server info display
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
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
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("서버: $serverIp:$serverPort")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Input fields
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            OutlinedTextField(
                value = deviceName,
                onValueChange = { deviceName = it },
                label = { Text("디바이스 이름") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isUpdating,
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = deviceOs,
                onValueChange = { deviceOs = it },
                label = { Text("운영체제 (예: Windows, Android, iOS)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isUpdating,
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = deviceIp,
                onValueChange = { deviceIp = it },
                label = { Text("디바이스 IP 주소") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isUpdating,
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = devicePortText,
                onValueChange = { devicePortText = it },
                label = { Text("디바이스 포트") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isUpdating,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
            ) {
                Text("뒤로")
            }

            OutlinedButton(
                onClick = onCancel,
                enabled = !isUpdating
            ) {
                Text("취소")
            }
            
            Button(
                onClick = {
                    val port = devicePortText.toIntOrNull()
                    if (deviceName.isNotBlank() && deviceOs.isNotBlank() && 
                        deviceIp.isNotBlank() && port != null && port > 0) {
                        isUpdating = true
                        onUpdateDevice(deviceName, deviceOs, deviceIp, port)
                    }
                },
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
        }
    }
}