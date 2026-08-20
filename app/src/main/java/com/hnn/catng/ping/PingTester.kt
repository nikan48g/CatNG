package com.hnn.catng.ping

import com.hnn.catng.model.ConfigItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

object PingTester {
    /**
     * تست پینگ سریع یک کانفیگ از طریق اتصال سوکت TCP
     */
    suspend fun testPing(item: ConfigItem, timeoutMs: Int = 3000): Long = withContext(Dispatchers.IO) {
        if (item.server.isBlank()) return@withContext -1L

        val startTime = System.currentTimeMillis()
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.connect(InetSocketAddress(item.server, item.port), timeoutMs)
            val duration = System.currentTimeMillis() - startTime
            return@withContext duration.coerceAtLeast(1L)
        } catch (e: Exception) {
            return@withContext -1L // خطا در اتصال یا Timeout
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * تست پینگ همزمان برای تمامی کانفیگ‌های یک لیست
     */
    suspend fun testAll(
        configs: List<ConfigItem>,
        onProgress: (configId: String, pingMs: Long) -> Unit
    ) = coroutineScope {
        configs.map { config ->
            async(Dispatchers.IO) {
                val ping = testPing(config)
                withContext(Dispatchers.Main) {
                    onProgress(config.id, ping)
                }
            }
        }.awaitAll()
    }
}
