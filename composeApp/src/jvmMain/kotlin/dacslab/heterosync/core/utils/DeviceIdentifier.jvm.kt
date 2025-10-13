package dacslab.heterosync.core.utils

import java.net.InetAddress

/**
 * Get unique device identifier based on hostname and username
 * Format: hostname-username
 */
actual fun getDeviceUniqueId(): String {
    return try {
        val hostname = InetAddress.getLocalHost().hostName
        val username = System.getProperty("user.name")

        "$hostname-$username"
    } catch (e: Exception) {
        // Fallback to username only if hostname fails
        try {
            val username = System.getProperty("user.name")
            "unknown-$username"
        } catch (ex: Exception) {
            "unknown-unknown"
        }
    }
}