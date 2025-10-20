package dacslab.heterosync.ui.wear.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.*

@Composable
fun DeviceInputScreen(
    savedDeviceId: String?,
    onQuickConnect: (String, Int, String) -> Unit,
    onDeviceIdSettingClick: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "HeteroSync",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.title1
            )
        }

        item {
            Text(
                text = "서버 연결",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.body1
            )
        }

        // Show current device ID if set
        item {
            Text(
                text = if (savedDeviceId != null) "ID: $savedDeviceId" else "ID: 자동 생성",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.caption1
            )
        }

        item {
            Chip(
                onClick = onDeviceIdSettingClick,
                label = { Text("Device ID 설정") },
                colors = ChipDefaults.secondaryChipColors()
            )
        }

        item {
            Chip(
                onClick = { onQuickConnect("155.230.34.145", 8081, "WATCH") },
                label = { Text("기본 서버 연결") },
                colors = ChipDefaults.primaryChipColors()
            )
        }
    }
}

@Composable
fun DeviceIdSettingScreen(
    currentDeviceId: String?,
    onBack: () -> Unit,
    onDeviceIdSelected: (String) -> Unit
) {
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "Device ID 선택",
                style = MaterialTheme.typography.title2,
                textAlign = TextAlign.Center
            )
        }

        item {
            Text(
                text = "현재: ${currentDeviceId ?: "자동 생성"}",
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Center
            )
        }

        // Preset options
        item {
            Chip(
                onClick = {
                    onDeviceIdSelected("watch-1")
                    onBack()
                },
                label = { Text("watch-1") },
                colors = if (currentDeviceId == "watch-1") {
                    ChipDefaults.primaryChipColors()
                } else {
                    ChipDefaults.secondaryChipColors()
                }
            )
        }

        item {
            Chip(
                onClick = {
                    onDeviceIdSelected("watch-2")
                    onBack()
                },
                label = { Text("watch-2") },
                colors = if (currentDeviceId == "watch-2") {
                    ChipDefaults.primaryChipColors()
                } else {
                    ChipDefaults.secondaryChipColors()
                }
            )
        }

        item {
            Chip(
                onClick = {
                    onDeviceIdSelected("watch-test")
                    onBack()
                },
                label = { Text("watch-test") },
                colors = if (currentDeviceId == "watch-test") {
                    ChipDefaults.primaryChipColors()
                } else {
                    ChipDefaults.secondaryChipColors()
                }
            )
        }

        item {
            Chip(
                onClick = onBack,
                label = { Text("뒤로") },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
    }
}