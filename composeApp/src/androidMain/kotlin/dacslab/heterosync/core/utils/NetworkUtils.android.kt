package dacslab.heterosync.core.utils

import java.net.*

actual class NetworkUtils {
    actual fun getHostExternalIpAddress(): String {
        return try {
            // 1. 외부 서버에 연결을 시도하여 실제 사용되는 로컬 IP 확인 (타임아웃 설정)
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress("8.8.8.8", 80), 3000) // 3초 타임아웃
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
            
            for (networkInterface in interfaces) {
                val interfaceName = networkInterface.name.lowercase()
                
                // WiFi, 이더넷, 모바일 데이터 인터페이스 우선 고려
                val isRelevantInterface = interfaceName.contains("wlan") || 
                                        interfaceName.contains("eth") || 
                                        interfaceName.contains("rmnet") ||
                                        interfaceName.contains("ccmni") ||
                                        interfaceName.contains("pdp")
                
                if (!networkInterface.isLoopback && networkInterface.isUp && isRelevantInterface) {
                    val addresses = networkInterface.inetAddresses
                    for (address in addresses) {
                        val ip = address.hostAddress
                        if (ip != null && isValidExternalIp(ip)) {
                            candidateIps.add(ip)
                        }
                    }
                }
            }
            
            // 3. WiFi 인터페이스 IP 우선, 그 다음 모바일 데이터
            candidateIps.firstOrNull { !isPrivateIp(it) }
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
}