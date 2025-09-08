package dacslab.heterosync.core.data

import kotlinx.serialization.Serializable

@Serializable
data class DeviceInfo(
    val device_name: String,
    val device_os: String,
    val device_ip: String,
    val device_port: Int
)