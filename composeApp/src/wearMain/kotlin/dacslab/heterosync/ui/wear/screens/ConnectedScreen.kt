package dacslab.heterosync.ui.wear.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
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
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Card(
                onClick = { },
                backgroundPainter = CardDefaults.cardBackgroundPainter(
                    startBackgroundColor = when (connectionHealth) {
                        ConnectionHealth.HEALTHY -> Color(0xFF1B5E20) // 진한 초록색
                        ConnectionHealth.UNHEALTHY -> Color(0xFFE65100) // 진한 주황색
                        ConnectionHealth.DEAD -> Color(0xFFB71C1C) // 진한 빨간색
                        ConnectionHealth.UNKNOWN -> Color(0xFF424242) // 회색
                    },
                    endBackgroundColor = when (connectionHealth) {
                        ConnectionHealth.HEALTHY -> Color(0xFF2E7D32) // 초록색
                        ConnectionHealth.UNHEALTHY -> Color(0xFFF57C00) // 주황색
                        ConnectionHealth.DEAD -> Color(0xFFC62828) // 빨간색
                        ConnectionHealth.UNKNOWN -> Color(0xFF616161) // 밝은 회색
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = when (connectionHealth) {
                            ConnectionHealth.HEALTHY -> "🟢"
                            ConnectionHealth.UNHEALTHY -> "🟡"
                            ConnectionHealth.DEAD -> "🔴"
                            ConnectionHealth.UNKNOWN -> "❓"
                        },
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.display1,
                        color = Color.White
                    )

                    Text(
                        text = connectionStatus,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.body1,
                        color = Color.White
                    )

                    if (webSocketDeviceId != null) {
                        Text(
                            text = "ID: ${webSocketDeviceId.take(15)}...",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.caption2,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        item {
            Card(onClick = { }) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$serverIp:$serverPort",
                        style = MaterialTheme.typography.body2
                    )
                }
            }
        }

        item {
            Card(onClick = { }) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = deviceInfo.deviceType,
                        style = MaterialTheme.typography.body1
                    )
                    Text(
                        text = deviceInfo.deviceId.take(20) + "...",
                        style = MaterialTheme.typography.caption3
                    )
                }
            }
        }

        item {
            Chip(
                onClick = onDisconnect,
                label = { Text("연결 해제") },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
    }
}
