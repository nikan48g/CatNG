package com.hnn.catng.vpn

import android.content.Context
import android.net.TrafficStats
import android.os.Process
import com.hnn.catng.model.ConfigItem
import com.hnn.catng.parser.ConfigParser
import java.io.File
import java.io.FileOutputStream

object XrayCoreManager {
    private const val CONFIG_FILE_NAME = "current_config.json"

    /**
     * استخراج فایل‌های geoip.dat و geosite.dat از Assets به دایرکتوری فایل‌های برنامه
     */
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
     * تولید و ذخیره فایل نهایی JSON کانفیگ برای اجرا توسط هسته
     */
    fun prepareConfigFile(context: Context, config: ConfigItem): File {
        extractAssetsIfNeeded(context)

        val jsonString = config.rawJson ?: run {
            // ساخت یک JSON استاندارد اگر فقط لینک موجود بود
            val outbound = ConfigParser.parseUri(config.rawUri ?: "")?.rawJson
            outbound ?: ConfigParser.gson.toJson(
                ConfigParser.buildFullXrayConfig(
                    com.hnn.catng.model.XrayOutbound(
                        protocol = config.protocol.lowercase(),
                        tag = "proxy"
                    ),
                    config.name
                )
            )
        }

        val configFile = File(context.filesDir, CONFIG_FILE_NAME)
        configFile.writeText(jsonString)
        return configFile
    }
}

/**
 * مانیتورینگ ۱۰۰٪ واقعی ترافیک مصرفی شبکه از سیستم‌عامل اندروید
 */
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
