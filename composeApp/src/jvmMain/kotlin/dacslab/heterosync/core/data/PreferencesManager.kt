package dacslab.heterosync.core.data

import java.io.File
import java.util.Properties

/**
 * JVM 플랫폼용 설정 저장 관리자
 * 디바이스 ID 등의 설정을 디스크에 저장하고 로드합니다.
 */
object PreferencesManager {
    private const val PREFS_FILE_NAME = "heterosync.properties"
    private const val KEY_DEVICE_ID = "device_id"

    private val prefsFile: File by lazy {
        val userHome = System.getProperty("user.home")
        val appDir = File(userHome, ".heterosync")
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        File(appDir, PREFS_FILE_NAME)
    }

    private val properties = Properties()

    init {
        loadProperties()
    }

    private fun loadProperties() {
        try {
            if (prefsFile.exists()) {
                prefsFile.inputStream().use { input ->
                    properties.load(input)
                }
            }
        } catch (e: Exception) {
            println("Failed to load preferences: ${e.message}")
        }
    }

    private fun saveProperties() {
        try {
            prefsFile.outputStream().use { output ->
                properties.store(output, "HeteroSync Preferences")
            }
        } catch (e: Exception) {
            println("Failed to save preferences: ${e.message}")
        }
    }

    /**
     * 저장된 디바이스 ID를 가져옵니다.
     * 저장된 값이 없으면 null을 반환합니다.
     */
    fun getDeviceId(): String? {
        return properties.getProperty(KEY_DEVICE_ID)
    }

    /**
     * 디바이스 ID를 저장합니다.
     */
    fun saveDeviceId(deviceId: String) {
        properties.setProperty(KEY_DEVICE_ID, deviceId)
        saveProperties()
    }

    /**
     * 저장된 디바이스 ID를 삭제합니다.
     */
    fun clearDeviceId() {
        properties.remove(KEY_DEVICE_ID)
        saveProperties()
    }
}