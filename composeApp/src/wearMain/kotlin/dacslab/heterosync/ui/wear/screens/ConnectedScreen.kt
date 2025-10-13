package dacslab.heterosync.ui.wear.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
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
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "연결됨",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.title1,
                color = if (isWebSocketConnected)
                    MaterialTheme.colors.primary
                else
                    MaterialTheme.colors.error
            )
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
            Text(
                text = if (isWebSocketConnected) "WebSocket 연결됨" else "연결 중...",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.caption1
            )
        }

        if (webSocketDeviceId != null) {
            item {
                Text(
                    text = "ID: ${webSocketDeviceId.take(15)}...",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.caption2
                )
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