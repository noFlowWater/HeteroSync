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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        Text(
            text = "디바이스 등록",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "디바이스를 찾을 수 없습니다.\n새 디바이스를 등록하시겠습니까?",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Device info display
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
                    text = "디바이스 정보",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("서버: $serverIp:$serverPort")
                Text("디바이스: $deviceIp:$devicePort")
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
                enabled = !isRegistering,
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = deviceOs,
                onValueChange = { deviceOs = it },
                label = { Text("운영체제 (예: Windows, Android, iOS)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRegistering,
                singleLine = true
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
                enabled = !isRegistering
            ) {
                Text("취소")
            }
            
            Button(
                onClick = {
                    if (deviceName.isNotBlank() && deviceOs.isNotBlank()) {
                        isRegistering = true
                        onRegisterDevice(deviceName, deviceOs)
                    }
                },
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
        }
    }
}