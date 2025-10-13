package dacslab.heterosync.core.data

import kotlinx.serialization.Serializable


@Serializable
data class DeviceInfo(
    val deviceId: String = "",
    val deviceType: String = ""
)