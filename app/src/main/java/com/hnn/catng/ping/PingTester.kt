package com.hnn.catng.ping

import android.content.Context
import com.hnn.catng.model.ConfigItem
import com.hnn.catng.vpn.XrayCoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import libv2ray.Libv2ray
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket

object PingTester {
    private const val TEST_URL = "https://www.google.com/generate_204"

    /**
     * تست پینگ واقعی با استفاده از موتور LibXray یا سوکت TCP واقعی
     */
    suspend fun testPing(context: Context, item: ConfigItem, timeoutMs: Int = 3500): Long = withContext(Dispatchers.IO) {
        // ۱. تلاش اول: تست تأخیر واقعی با هسته Xray (Measure Outbound Delay)
        try {
            // ساخت JSON کاملاً ولید برای این کانفیگ خاص
            val tempFile = XrayCoreManager.prepareConfigFile(context, item)
            val validJson = tempFile.readText()
            
            val delay = Libv2ray.measureOutboundDelay(validJson, TEST_URL)
            if (delay > 0) {
                return@withContext delay
            }
        } catch (_: Exception) {
            // اگر از طریق هسته خطا داد، فال‌بک به سوکت مستقیم
        }

        // ۲. تلاش دوم: تست اتصال سوکت TCP مستقیم به سرور و پورت (Real TCP Handshake)
        if (item.server.isBlank()) return@withContext -1L

        val startTime = System.currentTimeMillis()
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.tcpNoDelay = true
            socket.soTimeout = timeoutMs
            socket.connect(InetSocketAddress(item.server, item.port), timeoutMs)
            val duration = System.currentTimeMillis() - startTime
            return@withContext duration.coerceAtLeast(1L)
        } catch (e: Exception) {
            return@withContext -1L
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * تست پینگ همزمان برای تمامی کانفیگ‌ها با استفاده از Coroutines
     */
    suspend fun testAll(
        context: Context,
        configs: List<ConfigItem>,
        onProgress: (configId: String, pingMs: Long) -> Unit
    ) = coroutineScope {
        configs.map { config ->
            async(Dispatchers.IO) {
                val ping = testPing(context, config)
                withContext(Dispatchers.Main) {
                    onProgress(config.id, ping)
                }
            }
        }.awaitAll()
    }
}
