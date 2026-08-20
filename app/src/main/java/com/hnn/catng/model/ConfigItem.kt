package com.hnn.catng.model

import java.util.UUID

/**
 * آیتم کانفیگ ذخیره شده در برنامه
 */
data class ConfigItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String, // Remarks پاکسازی شده از هرگونه ایموجی
    val subscriptionId: String? = null,
    val protocol: String = "VLESS", // VLESS, VMESS, TROJAN, SHADOWSOCKS, SOCKS
    val server: String = "",
    val port: Int = 443,
    val network: String = "ws", // ws, grpc, tcp
    val security: String = "tls", // tls, reality, none
    val pingMs: Long? = null, // -1 = Error / Timeout, null = Not Tested, > 0 = Latency ms
    val rawJson: String? = null, // ساختار کامل JSON برای کانفیگ‌های پیشرفته Xray
    val rawUri: String? = null, // لینک استاندارد مثل vless://
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * مدل سابسکریپشن / گروه‌بندی کانفیگ‌ها
 */
data class SubscriptionItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "import_sub",
    val url: String = "",
    val autoUpdate: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis(),
    val totalConfigCount: Int = 0
)

/**
 * وضعیت ارتباط و ترافیک زنده
 */
enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING
}

data class VpnState(
    val status: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val activeConfigId: String? = null,
    val uploadSpeedBps: Long = 0,
    val downloadSpeedBps: Long = 0,
    val totalUploadedBytes: Long = 0,
    val totalDownloadedBytes: Long = 0,
    val connectedDurationSeconds: Long = 0
)

enum class SortType {
    BEST_PING,
    NAME_ASC,
    NAME_DESC,
    NEWEST,
    OLDEST
}
