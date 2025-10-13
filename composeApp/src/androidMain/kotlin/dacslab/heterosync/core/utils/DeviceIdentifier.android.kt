package dacslab.heterosync.core.utils

import android.os.Build
import java.net.InetAddress

/**
 * Get unique device identifier based on hostname and username
 * Format: hostname-username
 *
 * On Android, we use device model and Android ID as fallback
 */
actual fun getDeviceUniqueId(): String {
    return try {
        val hostname = InetAddress.getLocalHost().hostName
        val username = System.getProperty("user.name") ?: "android"

        "$hostname-$username"
    } catch (e: Exception) {
        // Fallback to device model and user
        try {
            val deviceModel = Build.MODEL.replace(" ", "-")
            val username = System.getProperty("user.name") ?: "android"
            "$deviceModel-$username"
        } catch (ex: Exception) {
            // Final fallback
            "android-${Build.MODEL.replace(" ", "-")}"
        }
    }
}