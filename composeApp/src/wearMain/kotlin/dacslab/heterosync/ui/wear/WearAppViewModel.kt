package dacslab.heterosync.ui.wear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import dacslab.heterosync.core.data.ConnectionHealth
import dacslab.heterosync.core.data.DeviceInfo
import dacslab.heterosync.core.monitoring.ServiceMonitor
import dacslab.heterosync.core.service.ServiceBroadcastActions
import dacslab.heterosync.core.service.WebSocketForegroundService
import dacslab.heterosync.core.utils.DevicePreferences
import dacslab.heterosync.core.utils.getDeviceUniqueId
import dacslab.heterosync.ui.common.AppState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WearAppViewModel(private val context: Context) {
    private val devicePreferences = DevicePreferences(context)

    private val _state = MutableStateFlow<AppState>(AppState.DeviceInput)
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val _showDeviceIdSetting = MutableStateFlow(false)
    val showDeviceIdSetting: StateFlow<Boolean> = _showDeviceIdSetting.asStateFlow()

    // ViewModel의 CoroutineScope
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ServiceMonitor 인스턴스
    private var serviceMonitor: ServiceMonitor? = null

    // Service로부터 Health 변경을 수신하는 BroadcastReceiver
    private val healthReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ServiceBroadcastActions.ACTION_HEALTH_CHANGED) {
                val healthName = intent.getStringExtra(ServiceBroadcastActions.EXTRA_HEALTH)
                val health = try {
                    ConnectionHealth.valueOf(healthName ?: "UNKNOWN")
                } catch (e: IllegalArgumentException) {
                    ConnectionHealth.UNKNOWN
                }
                updateConnectionHealth(health)
            }
        }
    }

    // BroadcastReceiver 등록 여부 추적
    private var isHealthReceiverRegistered = false

    /**
     * ConnectionHealth 변경 시 AppState.Connected 업데이트
     */
    private fun updateConnectionHealth(health: ConnectionHealth) {
        val currentState = _state.value
        if (currentState is AppState.Connected) {
            _state.value = currentState.copy(connectionHealth = health)
            println("📊 WearAppViewModel: ConnectionHealth updated to $health")
        }
    }

    /**
     * BroadcastReceiver 해제
     */
    private fun unregisterHealthReceiver() {
        if (isHealthReceiverRegistered) {
            try {
                context.unregisterReceiver(healthReceiver)
                isHealthReceiverRegistered = false
                println("🛑 WearAppViewModel: Health receiver unregistered")
            } catch (e: IllegalArgumentException) {
                println("⚠️ WearAppViewModel: Receiver already unregistered: ${e.message}")
            }
        }
    }

    fun getSavedDeviceId(): String? {
        return devicePreferences.getDeviceId()
    }

    fun saveDeviceId(deviceId: String) {
        devicePreferences.saveDeviceId(deviceId)
    }

    fun showDeviceIdSetting() {
        _showDeviceIdSetting.value = true
    }

    fun hideDeviceIdSetting() {
        _showDeviceIdSetting.value = false
    }

    fun connectToServer(
        serverIp: String,
        serverPort: Int,
        deviceType: String
    ) {
        _state.value = AppState.Loading

        // Use saved device ID, or generate one
        val deviceId = devicePreferences.getDeviceId() ?: getDeviceUniqueId()

        val tempDeviceInfo = DeviceInfo(
            deviceId = deviceId,
            deviceType = deviceType
        )

        _state.value = AppState.Connected(tempDeviceInfo, serverIp, serverPort)

        // 기존 ServiceMonitor가 있으면 먼저 정리 (중복 방지)
        if (serviceMonitor != null) {
            println("⚠️ WearAppViewModel: Stopping existing ServiceMonitor before starting new one")
            serviceMonitor?.stopMonitoring()
            serviceMonitor = null
        }

        // Start foreground service
        val serviceIntent = Intent(context, WebSocketForegroundService::class.java).apply {
            action = WebSocketForegroundService.ACTION_START_SERVICE
            putExtra(WebSocketForegroundService.EXTRA_SERVER_IP, serverIp)
            putExtra(WebSocketForegroundService.EXTRA_SERVER_PORT, serverPort)
            putExtra(WebSocketForegroundService.EXTRA_DEVICE_TYPE, deviceType)
            putExtra(WebSocketForegroundService.EXTRA_DEVICE_ID, deviceId)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // ServiceMonitor 시작
        serviceMonitor = ServiceMonitor(context = context)
        serviceMonitor?.startMonitoring(
            scope = viewModelScope,
            serverIp = serverIp,
            serverPort = serverPort,
            deviceType = deviceType,
            deviceId = deviceId
        )

        // BroadcastReceiver 등록 (ConnectionHealth 변경 수신)
        if (!isHealthReceiverRegistered) {
            val filter = IntentFilter(ServiceBroadcastActions.ACTION_HEALTH_CHANGED)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(
                        healthReceiver,
                        filter,
                        Context.RECEIVER_NOT_EXPORTED
                    )
                } else {
                    ContextCompat.registerReceiver(
                        context,
                        healthReceiver,
                        filter,
                        ContextCompat.RECEIVER_NOT_EXPORTED
                    )
                }
                isHealthReceiverRegistered = true
                println("🔍 WearAppViewModel: Health receiver registered successfully")
            } catch (e: Exception) {
                println("⚠️ WearAppViewModel: Failed to register health receiver: ${e.message}")
            }
        }
    }

    fun disconnectWebSocket() {
        // ServiceMonitor 중지
        serviceMonitor?.stopMonitoring()
        serviceMonitor = null

        // BroadcastReceiver 해제
        unregisterHealthReceiver()

        // Stop service
        val serviceIntent = Intent(context, WebSocketForegroundService::class.java).apply {
            action = WebSocketForegroundService.ACTION_STOP_SERVICE
        }
        context.startService(serviceIntent)

        // UI 상태를 즉시 DeviceInput으로 변경
        _state.value = AppState.DeviceInput
        println("🔌 WearAppViewModel: Disconnected and returned to input screen")
    }

    fun resetToInput() {
        _state.value = AppState.DeviceInput
    }

    fun showError(message: String) {
        _state.value = AppState.Error(message)
    }

    fun navigateBack(): Boolean {
        return when (val currentState = _state.value) {
            is AppState.Loading -> {
                _state.value = AppState.DeviceInput
                true
            }
            is AppState.Connected -> {
                // 뒤로가기 시: 서비스는 유지하고 UI만 DeviceInput으로 변경
                // (연결 해제는 명시적인 "연결 해제" 버튼을 통해서만 수행)
                _state.value = AppState.DeviceInput
                println("⬅️ WearAppViewModel: Navigate back (service still running)")
                true
            }
            is AppState.Error -> {
                _state.value = AppState.DeviceInput
                true
            }
            is AppState.DeviceInput -> {
                false
            }
        }
    }

    fun cleanup() {
        // BroadcastReceiver 해제
        unregisterHealthReceiver()

        // ServiceMonitor 정리
        serviceMonitor?.stopMonitoring()
        serviceMonitor = null

        // ViewModel CoroutineScope 정리
        viewModelScope.cancel()
    }
}
