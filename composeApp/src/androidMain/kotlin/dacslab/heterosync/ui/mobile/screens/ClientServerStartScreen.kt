package dacslab.heterosync.ui.mobile.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dacslab.heterosync.core.data.DeviceInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientServerStartScreen(
    deviceInfo: DeviceInfo,
    serverIp: String,
    serverPort: Int,
    onBack: () -> Unit,
    onStartServer: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("연결 완료") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "뒤로가기")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Text(
                text = "연결 완료",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "연결된 디바이스",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${deviceInfo.device_name} (${deviceInfo.device_os})",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${deviceInfo.device_ip}:${deviceInfo.device_port}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Text(
                text = "클라이언트 서버 시작하기",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            
            Button(
                onClick = onStartServer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("서버 시작", fontSize = 18.sp)
            }
            
            Text(
                text = "동기화 클라이언트 서버를 시작할 준비되었습니다. 시작 버튼을 클릭하여 시간 동기화 서비스 네트워크에 참여하세요.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}