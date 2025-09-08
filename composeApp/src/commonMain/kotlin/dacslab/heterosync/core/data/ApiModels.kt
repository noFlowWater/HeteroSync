package dacslab.heterosync.core.data

import kotlinx.serialization.Serializable

@Serializable
data class CreateDeviceRequest(
    val server_ip: String? = null,
    val server_port: Int? = null,
    val device_name: String,
    val device_os: String,
    val device_ip: String,
    val device_port: Int
)

@Serializable
data class UpdateDeviceRequest(
    val server_ip: String? = null,
    val server_port: Int? = null,
    val current_device_ip: String,
    val current_device_port: Int,
    val after_device_name: String? = null,
    val after_device_os: String? = null,
    val after_device_ip: String? = null,
    val after_device_port: Int? = null
)

@Serializable
data class GetDeviceRequest(
    val server_ip: String,
    val server_port: Int,
    val device_ip: String,
    val device_port: Int
)

@Serializable
data class DeleteDeviceRequest(
    val device_ip: String,
    val device_port: Int
)

@Serializable
data class PingRequest(
    val client_timestamp: Long,
    val client_ip: String,
    val client_port: Int
)

@Serializable
data class PongResponse(
    val server_timestamp: Long,
    val client_timestamp: Long,
    val round_trip_time: Long
)

@Serializable
data class SyncStatusRequest(
    val client_ip: String,
    val client_port: Int
)

@Serializable
data class SyncStatusResponse(
    val is_active: Boolean,
    val connected_clients: Int,
    val last_sync_time: Long?
)