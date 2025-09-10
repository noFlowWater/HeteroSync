package dacslab.heterosync.core.server

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import io.ktor.server.application.*

@Serializable
data class ApiEndpoint(
    val method: String,
    val path: String,
    val description: String
)

data class ApiEndpointWithHandler(
    val method: String,
    val path: String,
    val description: String,
    val handler: suspend (call: ApplicationCall) -> Unit
)

class ClientServerService {
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private var isRunning = false
    
    private val endpointsWithHandlers = listOf(
        ApiEndpointWithHandler(
            method = "GET",
            path = "/api/v1/health",
            description = "서버 상태 확인"
        ) { call ->
            println("📥 [GET] /api/v1/health - Health check requested")
            call.respond(mapOf("healthy" to true))
            println("📤 Health check response sent")
        },
        ApiEndpointWithHandler(
            method = "GET", 
            path = "/api/v1/time",
            description = "현재 시간 조회"
        ) { call ->
            println("📥 [GET] /api/v1/time - Time requested")
            val currentTime = System.currentTimeMillis()
            call.respond(mapOf("local_time" to currentTime))
            println("📤 Time response sent: $currentTime")
        }
    )

    fun startServer(port: Int, onStarted: (Int) -> Unit, onError: (Exception) -> Unit) {
        if (isRunning) {
            onError(Exception("서버가 이미 실행 중입니다"))
            return
        }

        try {
            server = embeddedServer(Netty, port = port) {
                install(ContentNegotiation) {
                    json()
                }

                routing {
                    endpointsWithHandlers.forEach { endpoint ->
                        when (endpoint.method.uppercase()) {
                            "GET" -> get(endpoint.path) { 
                                println("📥 [GET] ${endpoint.path} - ${endpoint.description}")
                                endpoint.handler(call)
                            }
                            "POST" -> post(endpoint.path) { 
                                println("📥 [POST] ${endpoint.path} - ${endpoint.description}")
                                endpoint.handler(call)
                            }
                            "PUT" -> put(endpoint.path) { 
                                println("📥 [PUT] ${endpoint.path} - ${endpoint.description}")
                                endpoint.handler(call)
                            }
                            "DELETE" -> delete(endpoint.path) { 
                                println("📥 [DELETE] ${endpoint.path} - ${endpoint.description}")
                                endpoint.handler(call)
                            }
                        }
                    }
                }
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    server?.start(wait = false)
                    isRunning = true
                    onStarted(port)
                } catch (e: Exception) {
                    onError(e)
                }
            }
        } catch (e: Exception) {
            onError(e)
        }
    }

    fun stopServer() {
        server?.stop(1000, 2000)
        server = null
        isRunning = false
    }

    fun isServerRunning(): Boolean = isRunning
    
    fun getAvailableEndpoints(): List<ApiEndpoint> {
        return endpointsWithHandlers.map { endpoint ->
            ApiEndpoint(
                method = endpoint.method,
                path = endpoint.path,
                description = endpoint.description
            )
        }
    }
}