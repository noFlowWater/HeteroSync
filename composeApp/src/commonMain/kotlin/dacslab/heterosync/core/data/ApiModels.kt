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
    val receiveTime: Long,  // T2: 클라이언트가 TIME_REQUEST를 받은 시간
    val sendTime: Long      // T3: 클라이언트가 TIME_RESPONSE를 보내는 시간
)

@Serializable
data class PingMessage(
    val type: String,
    val timestamp: Long
)

@Serializable
data class PongMessage(
    val type: String,
    val timestamp: Long
)

enum class ConnectionHealth {
    UNKNOWN,    // 연결되지 않음
    HEALTHY,    // 정상 (Last PING < 90초)
    UNHEALTHY,  // 불안정 (Last PING > 90초)
    DEAD        // 연결 끊김 (Last PING > 120초)
}
