package dacslab.heterosync.ui.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dacslab.heterosync.core.data.DeviceInfo

@Composable
fun ConnectedScreen(
    deviceInfo: DeviceInfo,
    serverIp: String,
    serverPort: Int,
    isWebSocketConnected: Boolean,
    webSocketDeviceId: String?,
    onDisconnect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "연결됨",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "서버 정보",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                InfoRow("서버 주소", "$serverIp:$serverPort")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "디바이스 정보",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                InfoRow("Device ID", deviceInfo.deviceId)
                InfoRow("Device Type", deviceInfo.deviceType)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isWebSocketConnected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "WebSocket 상태",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                InfoRow(
                    "연결 상태",
                    if (isWebSocketConnected) "연결됨" else "연결 중..."
                )

                if (webSocketDeviceId != null) {
                    InfoRow("WebSocket ID", webSocketDeviceId)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onDisconnect,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("연결 해제", fontSize = 16.sp)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}