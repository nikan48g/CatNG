package com.hnn.catng.vpn

import android.content.Context
import android.net.TrafficStats
import android.os.Process
import com.hnn.catng.model.ConfigItem
import com.hnn.catng.parser.ConfigParser
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object XrayCoreManager {
    const val CONFIG_FILE_NAME = "current_config.json"

    fun extractAssetsIfNeeded(context: Context) {
        val filesDir = context.filesDir
        listOf("geoip.dat", "geosite.dat").forEach { fileName ->
            val destFile = File(filesDir, fileName)
            if (!destFile.exists() || destFile.length() == 0L) {
                try {
                    context.assets.open(fileName).use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * آماده‌سازی و استانداردسازی JSON برای هسته Xray.
     * اگر JSON خام معتبر نباشد (مثلا لینک باشد)، آن را می‌سازد.
     * همچنین اینباندهای مورد نیاز برای TUN و SOCKS را تضمین می‌کند.
     */
    fun prepareConfigFile(context: Context, config: ConfigItem): File {
        extractAssetsIfNeeded(context)

        var jsonString = config.rawJson ?: ""
        
        // اگر رشته JSON نیست (خالی است یا با { شروع نمی‌شود)، آن را از روی URI پارس کن
        if (jsonString.isBlank() || !jsonString.trim().startsWith("{")) {
            val parsedConfig = ConfigParser.parseUri(config.rawUri ?: "")
            if (parsedConfig?.rawJson != null && parsedConfig.rawJson.trim().startsWith("{")) {
                jsonString = parsedConfig.rawJson
            } else {
                jsonString = ConfigParser.gson.toJson(
                    ConfigParser.buildFullXrayConfig(
                        com.hnn.catng.model.XrayOutbound(
                            protocol = config.protocol.lowercase(),
                            tag = "proxy"
                        ),
                        config.name
                    )
                )
            }
        }

        // حالا JSON را می‌خوانیم و تضمین می‌کنیم که inbounds استاندارد داشته باشد
        try {
            val root = JSONObject(jsonString)
            
            // اگر inbounds خالی است یا وجود ندارد
            if (!root.has("inbounds") || root.getJSONArray("inbounds").length() == 0) {
                val inbounds = JSONArray()
                
                // SOCKS5 Inbound
                val socksInbound = JSONObject().apply {
                    put("tag", "socks-in")
                    put("port", 10808)
                    put("listen", "127.0.0.1")
                    put("protocol", "socks")
                    put("settings", JSONObject().apply {
                        put("auth", "noauth")
                        put("udp", true)
                        put("sniffing", JSONObject().apply {
                            put("enabled", true)
                            put("destOverride", JSONArray(arrayOf("http", "tls")))
                        })
                    })
                }
                inbounds.put(socksInbound)
                root.put("inbounds", inbounds)
            }
            
            // اطمینان از تنظیمات DNS استاندارد
            if (!root.has("dns")) {
                root.put("dns", JSONObject().apply {
                    put("servers", JSONArray(arrayOf("1.1.1.1", "8.8.8.8", "localhost")))
                })
            }
            
            jsonString = root.toString(2)
        } catch (e: Exception) {
            e.printStackTrace()
            // در صورت خطای پارس، با همان رشته قبلی ادامه می‌دهیم
        }

        val configFile = File(context.filesDir, CONFIG_FILE_NAME)
        configFile.writeText(jsonString)
        return configFile
    }
}

class RealTrafficMonitor {
    private var lastRxBytes = 0L
    private var lastTxBytes = 0L
    private var baseRxBytes = 0L
    private var baseTxBytes = 0L
    private var initialized = false

    fun start() {
        val uid = Process.myUid()
        val currentRx = TrafficStats.getUidRxBytes(uid).let { if (it >= 0) it else TrafficStats.getTotalRxBytes() }
        val currentTx = TrafficStats.getUidTxBytes(uid).let { if (it >= 0) it else TrafficStats.getTotalTxBytes() }

        lastRxBytes = currentRx
        lastTxBytes = currentTx
        baseRxBytes = currentRx
        baseTxBytes = currentTx
        initialized = true
    }

    fun sample(): TrafficSample {
        if (!initialized) start()

        val uid = Process.myUid()
        val currentRx = TrafficStats.getUidRxBytes(uid).let { if (it >= 0) it else TrafficStats.getTotalRxBytes() }
        val currentTx = TrafficStats.getUidTxBytes(uid).let { if (it >= 0) it else TrafficStats.getTotalTxBytes() }

        val speedDown = (currentRx - lastRxBytes).coerceAtLeast(0L)
        val speedUp = (currentTx - lastTxBytes).coerceAtLeast(0L)

        lastRxBytes = currentRx
        lastTxBytes = currentTx

        val totalDown = (currentRx - baseRxBytes).coerceAtLeast(0L)
        val totalUp = (currentTx - baseTxBytes).coerceAtLeast(0L)

        return TrafficSample(
            downloadSpeedBps = speedDown,
            uploadSpeedBps = speedUp,
            totalDownloaded = totalDown,
            totalUploaded = totalUp
        )
    }
}

data class TrafficSample(
    val downloadSpeedBps: Long,
    val uploadSpeedBps: Long,
    val totalDownloaded: Long,
    val totalUploaded: Long
)
