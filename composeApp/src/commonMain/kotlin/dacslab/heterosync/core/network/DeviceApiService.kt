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

@Serializable
private data class GetDeviceRequestBody(
    val device_ip: String,
    val device_port: Int
)

class DeviceApiService {
    
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

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
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDevice(request: GetDeviceRequest): Result<DeviceInfo> {
        return try {
            val requestBody = GetDeviceRequestBody(
                device_ip = request.device_ip,
                device_port = request.device_port
            )
            
            val response = httpClient.get("http://${request.server_ip}:${request.server_port}/api/v1/device") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            
            val deviceInfo: DeviceInfo = response.body()
            Result.success(deviceInfo)
        } catch (e: Exception) {
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
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}