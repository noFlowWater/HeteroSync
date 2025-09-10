package dacslab.heterosync.core.utils

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import java.net.Inet4Address
import java.net.InetAddress
import java.net.ServerSocket
import java.net.UnknownHostException
import kotlin.random.Random

actual class NetworkUtils {

    actual fun getHostExternalIpAddress(): String {
        // 동기(Blocking) 반환
        val ip = runBlocking {
            for (u in ENDPOINTS) {
                val t = fetchIpv4(u)
                if (t != null) return@runBlocking t
            }
            null
        }
        // 모두 실패 시 기존 기본값 유지
        return ip ?: FALLBACK_IP
    }

    private suspend fun fetchIpv4(url: String): String? = try {
        val resp: HttpResponse = CLIENT.get(url)
        if (resp.status.value in 200..299) {
            val body = resp.bodyAsText().trim()
            val line = body.lineSequence().firstOrNull()?.trim()
            if (line != null && IPV4_REGEX.matches(line)) line else null
        } else null
    } catch (_: Exception) {
        null
    }

    companion object {
        private const val FALLBACK_IP = "192.168.1.100"
        private val IPV4_REGEX = Regex(
            "^((25[0-5]|2[0-4]\\d|[01]?\\d?\\d)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d?\\d)$"
        )

        private val CLIENT = HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = 3000
                connectTimeoutMillis = 20003
                socketTimeoutMillis = 2000
            }
            defaultRequest {
                headers.append("User-Agent", "PublicIpv4Fetcher/1.0")
            }
        }

        // IPv4 only 또는 IPv4 응답 보장/우선 엔드포인트
        private val ENDPOINTS = listOf(
            "https://v4.ident.me",        // IPv4 only
            "https://ipv4.icanhazip.com", // IPv4 only
            "https://api.ipify.org",
            "https://checkip.amazonaws.com",
            "https://ifconfig.me/ip"
        )
    }

    actual fun getRandomAvailablePort(): Int {
        repeat(100) { // Try up to 100 times to find an available port
            val port = Random.nextInt(1024, 65536) // Use ephemeral port range
            if (isPortAvailable(port)) {
                return port
            }
        }
        // Fallback: let the system choose a random available port
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