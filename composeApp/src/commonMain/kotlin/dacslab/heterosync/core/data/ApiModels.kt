package dacslab.heterosync.core.data

import kotlinx.serialization.Serializable


@Serializable
data class ConnectedMessage(
    val type: String,
    val deviceId: String,
    val serverTime: Long
)

@Serializable
data class TimeRequestMessage(
    val type: String,
    val requestId: String,
    val pairingId: String? = null
)

@Serializable
data class TimeResponseMessage(
    val type: String,
    val requestId: String,
    val timestamp: Long
)