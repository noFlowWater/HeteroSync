package dacslab.heterosync.ui.wear.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.*

@Composable
fun LoadingScreen() {
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "연결 중...",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.title2
            )
        }

        item {
            CircularProgressIndicator()
        }
    }
}
