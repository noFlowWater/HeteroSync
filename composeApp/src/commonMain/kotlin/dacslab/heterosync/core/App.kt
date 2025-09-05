package dacslab.heterosync.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        val calculator = remember { Calculator() }
        var firstNumber by remember { mutableStateOf("") }
        var secondNumber by remember { mutableStateOf("") }
        var result by remember { mutableStateOf("") }
        
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "간단한 덧셈 계산기",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            OutlinedTextField(
                value = firstNumber,
                onValueChange = { firstNumber = it },
                label = { Text("첫 번째 숫자") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Text(
                text = "+",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            OutlinedTextField(
                value = secondNumber,
                onValueChange = { secondNumber = it },
                label = { Text("두 번째 숫자") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Button(
                onClick = {
                    val num1 = firstNumber.toDoubleOrNull() ?: 0.0
                    val num2 = secondNumber.toDoubleOrNull() ?: 0.0
                    val calcResult = calculator.add(num1, num2)
                    result = calculator.formatResult(calcResult)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("계산하기", fontSize = 18.sp)
            }
            
            if (result.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = "결과: $result",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}