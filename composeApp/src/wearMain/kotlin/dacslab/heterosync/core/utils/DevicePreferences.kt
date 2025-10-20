package dacslab.heterosync.core.utils

import android.content.Context
import android.content.SharedPreferences

class DevicePreferences(context: Context) {

    companion object {
        private const val PREF_NAME = "heterosync_device_prefs"
        private const val KEY_DEVICE_ID = "device_id"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveDeviceId(deviceId: String) {
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        println("Device ID saved: $deviceId")
    }

    fun getDeviceId(): String? {
        return prefs.getString(KEY_DEVICE_ID, null)
    }

    fun hasDeviceId(): Boolean {
        return prefs.contains(KEY_DEVICE_ID)
    }

    fun clearDeviceId() {
        prefs.edit().remove(KEY_DEVICE_ID).apply()
    }
}
