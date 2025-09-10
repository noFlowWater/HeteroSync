package dacslab.heterosync.ui.mobile.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dacslab.heterosync.core.utils.NetworkUtils

@Composable
fun DeviceInputScreen(
    onDeviceCheck: (String, Int, String, Int) -> Unit
) {
    var serverIp by remember { mutableStateOf("155.230.34.145") }
    var serverPort by remember { mutableStateOf("8080") }
    var deviceIp by remember { mutableStateOf("") }
    var devicePort by remember { mutableStateOf("8081") }

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
            text = "서버 및 디바이스 정보를 입력하세요",
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )

        // 서버 정보 섹션
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
                    text = "🌐 연결할 서버 정보",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = serverIp,
                    onValueChange = { serverIp = it },
                    label = { Text("서버 IP 주소") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    placeholder = { Text("예: 192.168.1.100") }
                )

                OutlinedTextField(
                    value = serverPort,
                    onValueChange = { serverPort = it },
                    label = { Text("서버 포트") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("예: 8080") }
                )
            }
        }

        // 디바이스 정보 섹션
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "📱 현재 디바이스 정보",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = deviceIp,
                    onValueChange = { deviceIp = it },
                    label = { Text("디바이스 IP (선택사항)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    placeholder = { Text("비워두면 자동 감지") }
                )

                OutlinedTextField(
                    value = devicePort,
                    onValueChange = { devicePort = it },
                    label = { Text("클라이언트 서버 포트") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("예: 8081") }
                )
            }
        }

        Button(
            onClick = {
                val serverPortInt = serverPort.toIntOrNull() ?: 8080
                val devicePortInt = devicePort.toIntOrNull() ?: 8081
                val finalDeviceIp = deviceIp.ifBlank {
                    NetworkUtils().getHostExternalIpAddress()
                }
                onDeviceCheck(serverIp, serverPortInt, finalDeviceIp, devicePortInt)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = serverIp.isNotBlank()
        ) {
            Text("서버에 연결", fontSize = 18.sp)
        }
    }
}
