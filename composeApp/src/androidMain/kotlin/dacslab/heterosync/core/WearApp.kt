package dacslab.heterosync.core

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*

@Composable
fun WearApp() {
    MaterialTheme {
        val calculator = remember { Calculator() }
        var firstNumber by remember { mutableStateOf(0) }
        var secondNumber by remember { mutableStateOf(0) }
        var result by remember { mutableStateOf("") }
        var showResult by remember { mutableStateOf(false) }
        
        val listState = rememberScalingLazyListState()
        
        Scaffold(
            timeText = { TimeText() },
            vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        ) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item { 
                    Text(
                        text = "덧셈 계산기",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.title2
                    ) 
                }
                
                item { 
                    Text(
                        text = "첫 번째: $firstNumber",
                        textAlign = TextAlign.Center
                    ) 
                }
                
                item {
                    Row {
                        Chip(
                            onClick = { firstNumber++ },
                            label = { Text("+") },
                            modifier = Modifier.padding(2.dp)
                        )
                        Chip(
                            onClick = { if (firstNumber > 0) firstNumber-- },
                            label = { Text("-") },
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                }
                
                item { 
                    Text(
                        text = "두 번째: $secondNumber",
                        textAlign = TextAlign.Center
                    ) 
                }
                
                item {
                    Row {
                        Chip(
                            onClick = { secondNumber++ },
                            label = { Text("+") },
                            modifier = Modifier.padding(2.dp)
                        )
                        Chip(
                            onClick = { if (secondNumber > 0) secondNumber-- },
                            label = { Text("-") },
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                }
                
                item {
                    Chip(
                        onClick = { 
                            val calcResult = calculator.add(firstNumber, secondNumber)
                            result = calculator.formatResult(calcResult)
                            showResult = true
                        },
                        label = { Text("계산") },
                        colors = ChipDefaults.primaryChipColors()
                    )
                }
                
                if (showResult) {
                    item {
                        Card(
                            onClick = { }
                        ) {
                            Text(
                                text = "결과: $result",
                                modifier = Modifier.padding(8.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.title3
                            )
                        }
                    }
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

