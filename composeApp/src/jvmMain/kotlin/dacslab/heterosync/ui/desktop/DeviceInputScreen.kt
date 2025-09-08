package dacslab.heterosync.ui.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dacslab.heterosync.core.utils.NetworkUtils

@Composable
fun DeviceInputScreen(
    onDeviceCheck: (String, Int, String, Int) -> Unit
) {
    // 연결하고자 하는 서버의 BaseURL 정보를 위한 입력.
    var server_ip by remember { mutableStateOf("155.230.34.145") }
    var server_port by remember { mutableStateOf("8080") }

    // 현재 디바이스의 IP, 그리고 현재 디바이스에 띄울 '클라이언트 서버'의 Port를 지정.
    var device_ip by remember { mutableStateOf("") } // Optional, 지정 안되면 getHostExternalIpAddress() 써서 해당 데이터 채우면 됨.
    var device_port by remember { mutableStateOf("8081") } // 현재 디바이스에 클라이언트 서버의 Port를 입력.
    
    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        // 좌측 패널
        Card(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "HeteroSync",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "다중 디바이스 동기화 도구",
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // 우측 입력 패널
        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "서버 및 디바이스 설정",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // 서버 연결 정보 섹션
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "🌐 연결할 서버 정보",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    OutlinedTextField(
                        value = server_ip,
                        onValueChange = { server_ip = it },
                        label = { Text("서버 IP 주소") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        placeholder = { Text("예: 192.168.1.100") }
                    )
                    
                    OutlinedTextField(
                        value = server_port,
                        onValueChange = { server_port = it },
                        label = { Text("서버 포트") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("예: 8080") }
                    )
                }
            }
            
            // 현재 디바이스 정보 섹션
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "📱 현재 디바이스 정보",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = device_ip,
                        onValueChange = { device_ip = it },
                        label = { Text("디바이스 IP (선택사항)") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        placeholder = { Text("비워두면 자동 감지") }
                    )

                    OutlinedTextField(
                        value = device_port,
                        onValueChange = { device_port = it },
                        label = { Text("클라이언트 서버 포트") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("예: 8081") }
                    )
                }
            }
            
            Button(
                onClick = {
                    val serverPortInt = server_port.toIntOrNull() ?: 8080
                    val devicePortInt = device_port.toIntOrNull() ?: 8081
                    val finalDeviceIp = device_ip.ifBlank { 
                        NetworkUtils().getHostExternalIpAddress()
                    }
                    
                    onDeviceCheck(
                        server_ip,
                        serverPortInt,
                        finalDeviceIp,
                        devicePortInt
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = server_ip.isNotBlank()
            ) {
                Text("서버에 연결", fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "서버 정보와 현재 디바이스 정보를 입력하여 동기화 네트워크에 참여하세요",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}