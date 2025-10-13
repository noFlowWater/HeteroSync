package dacslab.heterosync.ui.wear.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.*

@Composable
fun DeviceInputScreen(
    onQuickConnect: (String, Int, String) -> Unit
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

        item {
            Chip(
                onClick = { onQuickConnect("155.230.34.145", 8080, "WATCH") },
                label = { Text("기본 서버 연결") },
                colors = ChipDefaults.primaryChipColors()
            )
        }
    }
}