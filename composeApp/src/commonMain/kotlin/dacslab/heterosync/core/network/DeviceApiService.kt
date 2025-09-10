package dacslab.heterosync.core.network

import dacslab.heterosync.core.data.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

expect fun createHttpClient(): HttpClient

@Serializable
private data class GetDeviceRequestBody(
    val device_ip: String,
    val device_port: Int
)

class DeviceApiService {
    
    private val httpClient = createHttpClient()

    suspend fun createDevice(request: CreateDeviceRequest): Result<String> {
        return try {
            val requestBody = CreateDeviceRequest(
                device_name = request.device_name,
                device_os = request.device_name,
                device_ip = request.device_ip,
                device_port = request.device_port
            )

            val response = httpClient.post("http://${request.server_ip}:${request.server_port}/api/v1/device/create") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            
            val result: String = response.body()
            println("✅ createDevice API 성공: $result")
            Result.success(result)
        } catch (e: Exception) {
            println("❌ createDevice API 실패: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getDevice(request: GetDeviceRequest): Result<DeviceInfo> {
        return try {
            val requestBody = GetDeviceRequest(
                device_ip = request.device_ip,
                device_port = request.device_port
            )
            println("📤 getDevice 요청 body: $requestBody")
            
            val response = httpClient.post("http://${request.server_ip}:${request.server_port}/api/v1/device") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            
            val responseText = response.body<String>()
            println("📝 getDevice 응답: $responseText")

            val deviceInfo: DeviceInfo = Json.decodeFromString(responseText)
            println("✅ getDevice API 성공: ${deviceInfo.device_name}")
            Result.success(deviceInfo)

        } catch (e: Exception) {
            println("❌ getDevice API 실패: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateDevice(request: UpdateDeviceRequest): Result<String> {
        return try {

            val requestBody = UpdateDeviceRequest(
                current_device_ip = request.current_device_ip,
                current_device_port = request.current_device_port,
                after_device_name = request.after_device_name,
                after_device_os = request.after_device_os,
                after_device_ip = request.after_device_ip,
                after_device_port = request.after_device_port
            )

            val response = httpClient.put("http://${request.server_ip}:${request.server_port}/api/v1/device/update") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            val result: String = response.body()
            println("✅ updateDevice API 성공: $result")
            Result.success(result)
        } catch (e: Exception) {
            println("❌ updateDevice API 실패: ${e.message}")
            Result.failure(e)
        }
    }
}