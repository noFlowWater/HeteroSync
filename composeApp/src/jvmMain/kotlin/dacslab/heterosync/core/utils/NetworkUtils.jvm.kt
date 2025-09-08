package dacslab.heterosync.core.utils

import java.net.*
import java.util.*

actual class NetworkUtils {
    actual fun getHostExternalIpAddress(): String {
        return try {
            // 1. 기본 게이트웨이를 통한 외부 연결 시뮬레이션으로 실제 외부 노출 IP 찾기 (타임아웃 설정)
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress("8.8.8.8", 80), 2000) // 3초 타임아웃
                    val localAddress = socket.localAddress.hostAddress
                    if (localAddress != null && isValidExternalIp(localAddress)) {
                        return localAddress
                    }
                }
            } catch (e: Exception) {
                println("외부 서버 연결 실패 (${e.message}), 로컬 인터페이스 스캔으로 전환")
            }
            
            // 2. 네트워크 인터페이스에서 외부 접근 가능한 IP 찾기
            val interfaces = NetworkInterface.getNetworkInterfaces()
            val candidateIps = mutableListOf<String>()
            val wiredIps = mutableListOf<String>()
            val wirelessIps = mutableListOf<String>()
            
            for (networkInterface in interfaces) {
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                
                val interfaceName = networkInterface.name.lowercase()
                val displayName = networkInterface.displayName?.lowercase() ?: ""
                
                // 이더넷/유선 연결 우선 (더 안정적)
                val isWired = interfaceName.contains("eth") || 
                             displayName.contains("ethernet") ||
                             displayName.contains("이더넷")
                
                // WiFi/무선 연결
                val isWiFi = interfaceName.contains("wlan") || 
                            interfaceName.contains("wifi") ||
                            displayName.contains("wi-fi") ||
                            displayName.contains("wireless")
                
                val addresses = networkInterface.inetAddresses
                for (address in addresses) {
                    val ip = address.hostAddress
                    if (ip != null && isValidExternalIp(ip)) {
                        when {
                            isWired -> wiredIps.add(ip)
                            isWiFi -> wirelessIps.add(ip)
                            else -> candidateIps.add(ip)
                        }
                    }
                }
            }
            
            // 3. 우선순위: 유선 > 무선 > 기타, 각 그룹 내에서는 공인 IP > 사설 IP
            val allIps = (wiredIps + wirelessIps + candidateIps)
            
            // 공인 IP 우선 검색
            wiredIps.firstOrNull { !isPrivateIp(it) }
                ?: wirelessIps.firstOrNull { !isPrivateIp(it) }
                ?: candidateIps.firstOrNull { !isPrivateIp(it) }
                // 사설 IP 검색
                ?: wiredIps.firstOrNull()
                ?: wirelessIps.firstOrNull() 
                ?: candidateIps.firstOrNull()
                ?: "192.168.1.100" // 기본값
                
        } catch (e: Exception) {
            println("IP 주소 조회 실패: ${e.message}")
            "192.168.1.100" // 기본값
        }
    }
    
    private fun isValidExternalIp(ip: String): Boolean {
        return try {
            val address = InetAddress.getByName(ip)
            !address.isLoopbackAddress && 
            !address.isLinkLocalAddress && 
            !address.isAnyLocalAddress &&
            ip.indexOf(':') == -1 // IPv4만 사용
        } catch (e: Exception) {
            false
        }
    }
    
    private fun isPrivateIp(ip: String): Boolean {
        return ip.startsWith("192.168.") || 
               ip.startsWith("10.") || 
               (ip.startsWith("172.") && 
                ip.split(".").getOrNull(1)?.toIntOrNull()?.let { it in 16..31 } == true)
    }
    
    actual fun getRandomAvailablePort(): Int {
        return ServerSocket(0).use { it.localPort }
    }
    
    actual fun isPortAvailable(port: Int): Boolean {
        return try {
            ServerSocket(port).use { true }
        } catch (e: Exception) {
            false
        }
    }
}