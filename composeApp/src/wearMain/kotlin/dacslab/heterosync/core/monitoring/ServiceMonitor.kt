package dacslab.heterosync.core.monitoring

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import dacslab.heterosync.core.data.ConnectionHealth
import dacslab.heterosync.core.service.ServiceBroadcastActions
import dacslab.heterosync.core.service.WebSocketForegroundService
import kotlinx.coroutines.*

/**
 * WebSocketForegroundService를 외부에서 독립적으로 모니터링하는 클래스
 *
 * Service는 ServiceMonitor의 존재를 전혀 모르며, 단지 브로드캐스트만 전송합니다.
 * ServiceMonitor는 브로드캐스트를 수신하여 Service의 상태를 추적하고,
 * 재시작이 필요한 조건이 만족되면 Service를 stop/start합니다.
 *
 * 재시작 조건:
 * 1. 시간 기반: Service가 일정 시간(기본 25분) 동안 실행되면 재시작
 * 2. 상태 기반: ConnectionHealth가 DEAD/UNHEALTHY/UNKNOWN 상태를 일정 시간(기본 1분) 이상 유지하면 재시작
 *
 * @param context Android Context (BroadcastReceiver 등록 및 Service 제어용)
 * @param serviceRestartIntervalMs Service 재시작 주기 (밀리초, 기본값: 25분)
 * @param healthCheckIntervalMs 연결 상태 체크 주기 (밀리초, 기본값: 30초)
 * @param disconnectedThresholdMs 비정상 상태 허용 시간 (밀리초, 기본값: 1분)
 */
class ServiceMonitor(
    private val context: Context,
    private val serviceRestartIntervalMs: Long = 25 * 60 * 1000L,  // 25분
    private val healthCheckIntervalMs: Long = 30_000L,             // 30초
    private val disconnectedThresholdMs: Long = 60_000L           // 1분
) {
    // Service 시작 시간
    private var serviceStartTime: Long = 0L

    // 마지막으로 연결이 정상이었던 시간
    private var lastHealthyTime: Long = 0L

    // 현재 ConnectionHealth 상태
    private var currentHealth: ConnectionHealth = ConnectionHealth.UNKNOWN

    // 모니터링 작업 Job
    private var monitoringJob: Job? = null

    // 저장된 연결 파라미터 (재시작용)
    private var lastServerIp: String? = null
    private var lastServerPort: Int = 0
    private var lastDeviceType: String? = null
    private var lastDeviceId: String? = null

    /**
     * Service로부터 브로드캐스트를 수신하는 BroadcastReceiver
     */
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ServiceBroadcastActions.ACTION_SERVICE_STARTED -> {
                    val startTime = intent.getLongExtra(
                        ServiceBroadcastActions.EXTRA_START_TIME,
                        System.currentTimeMillis()
                    )
                    onServiceStarted(startTime)
                }
                ServiceBroadcastActions.ACTION_HEALTH_CHANGED -> {
                    val healthName = intent.getStringExtra(ServiceBroadcastActions.EXTRA_HEALTH)
                    val health = try {
                        ConnectionHealth.valueOf(healthName ?: "UNKNOWN")
                    } catch (e: IllegalArgumentException) {
                        ConnectionHealth.UNKNOWN
                    }
                    onHealthChanged(health)
                }
            }
        }
    }

    /**
     * 모니터링 시작
     *
     * @param scope 모니터링 작업이 실행될 CoroutineScope
     * @param serverIp 서버 IP (재시작 시 사용)
     * @param serverPort 서버 포트 (재시작 시 사용)
     * @param deviceType 디바이스 타입 (재시작 시 사용)
     * @param deviceId 디바이스 ID (재시작 시 사용)
     */
    fun startMonitoring(
        scope: CoroutineScope,
        serverIp: String,
        serverPort: Int,
        deviceType: String,
        deviceId: String
    ) {
        // 연결 파라미터 저장 (재시작용)
        lastServerIp = serverIp
        lastServerPort = serverPort
        lastDeviceType = deviceType
        lastDeviceId = deviceId

        // BroadcastReceiver 등록
        val filter = IntentFilter().apply {
            addAction(ServiceBroadcastActions.ACTION_SERVICE_STARTED)
            addAction(ServiceBroadcastActions.ACTION_HEALTH_CHANGED)
        }

        try {
            // Android 13 (API 33) 이상에서는 RECEIVER_NOT_EXPORTED 플래그 필수
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    broadcastReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                ContextCompat.registerReceiver(
                    context,
                    broadcastReceiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
            }
            println("🔍 ServiceMonitor: BroadcastReceiver registered successfully")
        } catch (e: Exception) {
            println("⚠️ ServiceMonitor: Failed to register receiver: ${e.message}")
        }

        // 모니터링 Job 시작
        startMonitoringJob(scope)
    }

    /**
     * 주기적으로 조건을 체크하는 모니터링 Job 시작
     */
    private fun startMonitoringJob(scope: CoroutineScope) {
        monitoringJob?.cancel()
        monitoringJob = scope.launch {
            while (isActive) {
                delay(healthCheckIntervalMs)
                checkConditions()
            }
        }
        println("🔍 ServiceMonitor: Monitoring job started (interval: ${healthCheckIntervalMs}ms)")
    }

    /**
     * 재시작 조건을 체크
     */
    private fun checkConditions() {
        // 1. 시간 기반 체크: Service 실행 시간이 임계값을 초과했는지
        val elapsedTime = System.currentTimeMillis() - serviceStartTime
        if (serviceStartTime > 0 && elapsedTime >= serviceRestartIntervalMs) {
            println("⏰ ServiceMonitor: Service uptime limit reached " +
                    "(${elapsedTime}ms / ${serviceRestartIntervalMs}ms)")
            restartService()
            return
        }

        // 2. 상태 기반 체크: 연결이 비정상 상태를 유지하는지
        // HEALTHY가 아닌 모든 상태(DEAD, UNHEALTHY, UNKNOWN)를 비정상으로 간주
        if (currentHealth != ConnectionHealth.HEALTHY) {
            val unhealthyDuration = System.currentTimeMillis() - lastHealthyTime
            if (lastHealthyTime > 0 && unhealthyDuration >= disconnectedThresholdMs) {
                println("💀 ServiceMonitor: Connection unhealthy for too long " +
                        "(${unhealthyDuration}ms / ${disconnectedThresholdMs}ms) - Health: $currentHealth")
                restartService()
                return
            } else {
                println("⚠️ ServiceMonitor: Connection $currentHealth for ${unhealthyDuration}ms")
            }
        }
    }

    /**
     * Service 시작 브로드캐스트 수신 시 호출
     */
    private fun onServiceStarted(startTime: Long) {
        serviceStartTime = startTime
        lastHealthyTime = startTime
        currentHealth = ConnectionHealth.UNKNOWN
        println("🔍 ServiceMonitor: Service started at $startTime")
    }

    /**
     * ConnectionHealth 변경 브로드캐스트 수신 시 호출
     */
    private fun onHealthChanged(health: ConnectionHealth) {
        val previousHealth = currentHealth
        currentHealth = health

        if (health == ConnectionHealth.HEALTHY) {
            lastHealthyTime = System.currentTimeMillis()
        }

        println("📊 ServiceMonitor: Health changed from $previousHealth to $health")
    }

    /**
     * WebSocket 연결 재시작 수행
     * (Service 자체는 재시작하지 않고, WebSocket 연결만 재연결)
     */
    private fun restartService() {
        println("🔄 ServiceMonitor: Restarting WebSocket connection...")

        // 모니터링 일시 중지
        monitoringJob?.cancel()

        // Coroutine으로 재연결 처리
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // Service에 WebSocket 재연결 요청
                val restartIntent = Intent(WebSocketForegroundService.ACTION_RESTART_CONNECTION).apply {
                    setPackage(context.packageName)
                }
                context.sendBroadcast(restartIntent)
                println("📡 ServiceMonitor: WebSocket restart broadcast sent")

                // 시작 시간 리셋 (타이머 초기화)
                serviceStartTime = System.currentTimeMillis()
                lastHealthyTime = serviceStartTime

                println("✅ ServiceMonitor: WebSocket restart requested")

                // 모니터링 재시작
                startMonitoringJob(this)
            } catch (e: Exception) {
                println("❌ ServiceMonitor: WebSocket restart failed: ${e.message}")
            }
        }
    }

    /**
     * 모니터링 중지
     */
    fun stopMonitoring() {
        try {
            context.unregisterReceiver(broadcastReceiver)
            println("🛑 ServiceMonitor: BroadcastReceiver unregistered")
        } catch (e: IllegalArgumentException) {
            println("⚠️ ServiceMonitor: Receiver already unregistered: ${e.message}")
        }

        monitoringJob?.cancel()
        monitoringJob = null
        println("🛑 ServiceMonitor: Monitoring stopped")
    }
}
