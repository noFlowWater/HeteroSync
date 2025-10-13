package dacslab.heterosync.core.utils

import android.os.Build
import java.net.InetAddress

/**
 * Get unique device identifier based on hostname and username
 * Format: hostname-username
 *
 * On Wear OS, we use device model with "watch" identifier
 */
actual fun getDeviceUniqueId(): String {
    return try {
        val hostname = InetAddress.getLocalHost().hostName
        val username = System.getProperty("user.name") ?: "wear"

        "$hostname-$username"
    } catch (e: Exception) {
        // Fallback to device model for Wear OS
        try {
            val deviceModel = Build.MODEL.replace(" ", "-")
            val username = System.getProperty("user.name") ?: "wear"
            "$deviceModel-$username"
        } catch (ex: Exception) {
            // Final fallback with watch identifier
            "watch-${Build.MODEL.replace(" ", "-")}"
        }
    }
}