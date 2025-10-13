package dacslab.heterosync.ui.wear.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.*

@Composable
fun ErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "오류",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.title2
            )
        }

        item {
            Text(
                text = message,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.body1
            )
        }

        item {
            Chip(
                onClick = onRetry,
                label = { Text("다시 시도") },
                colors = ChipDefaults.primaryChipColors()
            )
        }
    }
}