package dacslab.heterosync.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.material.rememberScalingLazyListState
import androidx.wear.compose.material.ScalingLazyColumn

@Composable
fun WearApp() {
    MaterialTheme {
        val listState = rememberScalingLazyListState()
        Scaffold(
            timeText = { TimeText() },
            vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        ) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
            ) {
                item { Text(text = "HeteroSync") }
                items(5) { index ->
                    Chip(onClick = { /* TODO */ }, label = { Text("Item #$index") })
                }
            }
        }
    }
}

@Preview
@Composable
private fun WearAppPreview() {
    WearApp()
}

