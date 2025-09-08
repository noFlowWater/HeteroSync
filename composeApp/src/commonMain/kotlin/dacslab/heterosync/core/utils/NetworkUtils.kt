package dacslab.heterosync.core.utils

expect class NetworkUtils() {
    fun getHostExternalIpAddress(): String
    fun getRandomAvailablePort(): Int
    fun isPortAvailable(port: Int): Boolean
}