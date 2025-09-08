package dacslab.heterosync.core

import dacslab.heterosync.core.data.DeviceInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class HeteroSyncCoreTest {

    @Test
    fun deviceInfoCreation() {
        val deviceInfo = DeviceInfo(
            device_name = "Test Device",
            device_os = "Android",
            device_ip = "192.168.1.100",
            device_port = 8080
        )
        
        assertEquals("Test Device", deviceInfo.device_name)
        assertEquals("Android", deviceInfo.device_os)
        assertEquals("192.168.1.100", deviceInfo.device_ip)
        assertEquals(8080, deviceInfo.device_port)
    }

    @Test
    fun deviceApiServiceExists() {
        val service = dacslab.heterosync.core.network.DeviceApiService()
        assertNotNull(service)
    }
}