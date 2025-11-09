package dacslab.heterosync.core.service

/**
 * WebSocketForegroundService가 외부로 전송하는 브로드캐스트 액션 및 Extra 키 정의
 *
 * Service는 자신의 상태를 브로드캐스트로만 알리고, ServiceMonitor는 이를 수신하여 독립적으로 모니터링합니다.
 */
object ServiceBroadcastActions {
    /**
     * Service가 시작되었을 때 전송되는 브로드캐스트
     * Extra: EXTRA_START_TIME (Long) - 서비스 시작 시간 (System.currentTimeMillis())
     */
    const val ACTION_SERVICE_STARTED = "dacslab.heterosync.SERVICE_STARTED"

    /**
     * ConnectionHealth 상태가 변경되었을 때 전송되는 브로드캐스트
     * Extra: EXTRA_HEALTH (String) - ConnectionHealth.name 값
     */
    const val ACTION_HEALTH_CHANGED = "dacslab.heterosync.HEALTH_CHANGED"

    /**
     * 서비스 시작 시간 (밀리초)
     */
    const val EXTRA_START_TIME = "start_time"

    /**
     * ConnectionHealth 상태 (String: "HEALTHY", "UNHEALTHY", "DEAD", "UNKNOWN")
     */
    const val EXTRA_HEALTH = "health"
}
