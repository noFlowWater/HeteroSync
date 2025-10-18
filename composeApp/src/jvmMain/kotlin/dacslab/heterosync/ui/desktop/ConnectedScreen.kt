package dacslab.heterosync.ui.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dacslab.heterosync.core.data.ConnectionHealth
import dacslab.heterosync.core.data.DeviceInfo

@Composable
fun ConnectedScreen(
    deviceInfo: DeviceInfo,
    serverIp: String,
    serverPort: Int,
    isWebSocketConnected: Boolean,
    webSocketDeviceId: String?,
    connectionStatus: String = if (isWebSocketConnected) "연결됨" else "연결 중...",
    connectionHealth: ConnectionHealth = ConnectionHealth.UNKNOWN,
    lastError: String? = null,
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
                containerColor = when (connectionHealth) {
                    ConnectionHealth.HEALTHY -> MaterialTheme.colorScheme.primaryContainer
                    ConnectionHealth.UNHEALTHY -> MaterialTheme.colorScheme.tertiaryContainer
                    ConnectionHealth.DEAD -> MaterialTheme.colorScheme.errorContainer
                    ConnectionHealth.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant
                }
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
                    connectionStatus
                )

                InfoRow(
                    "연결 건강도",
                    when (connectionHealth) {
                        ConnectionHealth.HEALTHY -> "🟢 정상"
                        ConnectionHealth.UNHEALTHY -> "🟡 불안정"
                        ConnectionHealth.DEAD -> "🔴 연결 끊김"
                        ConnectionHealth.UNKNOWN -> "❓ 알 수 없음"
                    }
                )

                if (webSocketDeviceId != null) {
                    InfoRow("WebSocket ID", webSocketDeviceId)
                }

                if (lastError != null) {
                    Text(
                        text = "⚠️ $lastError",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
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
