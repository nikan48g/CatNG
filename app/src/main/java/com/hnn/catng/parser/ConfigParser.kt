package com.hnn.catng.parser

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.hnn.catng.model.ConfigItem
import com.hnn.catng.model.XrayConfig
import com.hnn.catng.model.XrayOutbound
import com.hnn.catng.model.XrayOutboundSettings
import com.hnn.catng.model.XrayStreamSettings
import com.hnn.catng.model.XrayTlsSettings
import com.hnn.catng.model.XrayUser
import com.hnn.catng.model.XrayVnext
import com.hnn.catng.model.XrayWsSettings
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object ConfigParser {
    val gson: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    /**
     * تشخیص و پارس خودکار متن ورودی (آرایه JSON، آبجکت JSON، لینک‌های چندخطی یا سابسکریپشن)
     */
    fun parseInput(text: String, subscriptionId: String? = null): List<ConfigItem> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()

        // 1. اگر JSON باشد
        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            val jsonConfigs = parseJson(trimmed, subscriptionId)
            if (jsonConfigs.isNotEmpty()) return jsonConfigs
        }

        // 2. در غیر این صورت، خط به خط بررسی لینک‌ها
        val results = mutableListOf<ConfigItem>()
        val lines = trimmed.split("\n", "\r\n")

        for (line in lines) {
            val cleanLine = line.trim()
            if (cleanLine.isEmpty()) continue

            val item = parseUri(cleanLine, subscriptionId)
            if (item != null) {
                results.add(item)
            }
        }

        return results
    }

    /**
     * پارس کانفیگ کامل JSON Xray (تکی یا آرایه)
     */
    fun parseJson(jsonString: String, subscriptionId: String? = null): List<ConfigItem> {
        val items = mutableListOf<ConfigItem>()
        try {
            val element = JsonParser.parseString(jsonString)
            if (element.isJsonArray) {
                val array = element.asJsonArray
                for (i in 0 until array.size()) {
                    val obj = array.get(i)
                    if (obj.isJsonObject) {
                        val configItem = parseSingleJsonObject(obj.asJsonObject, subscriptionId, index = i + 1)
                        if (configItem != null) {
                            items.add(configItem)
                        }
                    }
                }
            } else if (element.isJsonObject) {
                val configItem = parseSingleJsonObject(element.asJsonObject, subscriptionId, index = 1)
                if (configItem != null) {
                    items.add(configItem)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return items
    }

    private fun parseSingleJsonObject(jsonObj: JsonObject, subscriptionId: String?, index: Int): ConfigItem? {
        try {
            val rawJson = gson.toJson(jsonObj)
            val xrayConfig = gson.fromJson(jsonObj, XrayConfig::class.java)

            // پیدا کردن outbound اصلی پروکسی
            val proxyOutbound = xrayConfig.outbounds?.firstOrNull {
                it.tag == "proxy" || it.protocol.lowercase() in listOf("vless", "vmess", "trojan", "shadowsocks", "socks")
            } ?: xrayConfig.outbounds?.firstOrNull()

            val protocol = proxyOutbound?.protocol?.uppercase() ?: "XRAY"
            var server = ""
            var port = 443
            var network = "ws"
            var security = "tls"

            // استخراج آدرس سرور و پورت
            proxyOutbound?.settings?.vnext?.firstOrNull()?.let { vnext ->
                server = vnext.address
                port = vnext.port
            } ?: proxyOutbound?.settings?.servers?.firstOrNull()?.let { srv ->
                server = srv.address
                port = srv.port
            }

            proxyOutbound?.streamSettings?.let { stream ->
                network = stream.network ?: "ws"
                security = stream.security ?: "tls"
            }

            val remarksRaw = xrayConfig.remarks
                ?: jsonObj.get("remarks")?.asString
                ?: "Config $index ($protocol)"

            val cleanName = EmojiCleaner.clean(remarksRaw).ifBlank { "Config $index - $protocol" }

            return ConfigItem(
                name = cleanName,
                subscriptionId = subscriptionId,
                protocol = protocol,
                server = server,
                port = port,
                network = network,
                security = security,
                rawJson = rawJson
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * پارس لینک‌های استانداردی مثل vless://, vmess://, trojan://, ss://, socks:// با پارسر مستقل از اندروید
     */
    fun parseUri(uriString: String, subscriptionId: String? = null): ConfigItem? {
        val trimmed = uriString.trim()
        return when {
            trimmed.startsWith("vless://", ignoreCase = true) -> parseVlessUri(trimmed, subscriptionId)
            trimmed.startsWith("vmess://", ignoreCase = true) -> parseVmessUri(trimmed, subscriptionId)
            trimmed.startsWith("trojan://", ignoreCase = true) -> parseTrojanUri(trimmed, subscriptionId)
            trimmed.startsWith("ss://", ignoreCase = true) -> parseShadowsocksUri(trimmed, subscriptionId)
            trimmed.startsWith("socks://", ignoreCase = true) || trimmed.startsWith("socks5://", ignoreCase = true) -> parseSocksUri(trimmed, subscriptionId)
            else -> null
        }
    }

    private fun parseVlessUri(uriString: String, subscriptionId: String?): ConfigItem? {
        try {
            // ساختار: vless://uuid@host:port?query#fragment
            val noScheme = uriString.substringAfter("://")
            val fragment = if (noScheme.contains("#")) noScheme.substringAfter("#") else null
            val mainPart = noScheme.substringBefore("#")

            val queryParams = mutableMapOf<String, String>()
            val hostPart = if (mainPart.contains("?")) {
                val q = mainPart.substringAfter("?")
                q.split("&").forEach { pair ->
                    val kv = pair.split("=")
                    if (kv.size == 2) {
                        queryParams[kv[0]] = decodeSafe(kv[1])
                    }
                }
                mainPart.substringBefore("?")
            } else {
                mainPart
            }

            val userInfo = hostPart.substringBefore("@")
            val serverAndPort = hostPart.substringAfter("@")
            val host = serverAndPort.substringBefore(":")
            val port = serverAndPort.substringAfter(":", "443").toIntOrNull() ?: 443

            val remark = decodeRemark(fragment) ?: "VLESS - $host"
            val network = queryParams["type"] ?: "tcp"
            val security = queryParams["security"] ?: "none"
            val sni = queryParams["sni"] ?: queryParams["serverName"] ?: host
            val path = queryParams["path"] ?: "/"
            val wsHost = queryParams["host"] ?: sni
            val fp = queryParams["fp"] ?: "chrome"

            val outbound = XrayOutbound(
                protocol = "vless",
                tag = "proxy",
                settings = XrayOutboundSettings(
                    vnext = listOf(
                        XrayVnext(
                            address = host,
                            port = port,
                            users = listOf(XrayUser(id = userInfo, encryption = "none"))
                        )
                    )
                ),
                streamSettings = XrayStreamSettings(
                    network = network,
                    security = security,
                    wsSettings = if (network == "ws") XrayWsSettings(host = wsHost, path = path) else null,
                    tlsSettings = if (security == "tls") XrayTlsSettings(serverName = sni, fingerprint = fp) else null
                )
            )

            val fullConfig = buildFullXrayConfig(outbound, remark)

            return ConfigItem(
                name = EmojiCleaner.clean(remark).ifBlank { "VLESS $host:$port" },
                subscriptionId = subscriptionId,
                protocol = "VLESS",
                server = host,
                port = port,
                network = network,
                security = security,
                rawUri = uriString,
                rawJson = gson.toJson(fullConfig)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun parseVmessUri(uriString: String, subscriptionId: String?): ConfigItem? {
        try {
            val b64 = uriString.substringAfter("vmess://")
            val decoded = try {
                String(java.util.Base64.getDecoder().decode(b64.trim()), StandardCharsets.UTF_8)
            } catch (_: Exception) {
                String(Base64.decode(b64.trim(), Base64.DEFAULT), StandardCharsets.UTF_8)
            }
            val json = JsonParser.parseString(decoded).asJsonObject

            val host = json.get("add")?.asString ?: ""
            val port = json.get("port")?.asInt ?: 443
            val uuid = json.get("id")?.asString ?: ""
            val aid = json.get("aid")?.asInt ?: 0
            val net = json.get("net")?.asString ?: "ws"
            val hostHeader = json.get("host")?.asString ?: ""
            val path = json.get("path")?.asString ?: ""
            val tls = json.get("tls")?.asString ?: "none"
            val sni = json.get("sni")?.asString ?: hostHeader
            val ps = json.get("ps")?.asString ?: "VMess - $host"

            val outbound = XrayOutbound(
                protocol = "vmess",
                tag = "proxy",
                settings = XrayOutboundSettings(
                    vnext = listOf(
                        XrayVnext(
                            address = host,
                            port = port,
                            users = listOf(XrayUser(id = uuid, level = aid, security = "auto"))
                        )
                    )
                ),
                streamSettings = XrayStreamSettings(
                    network = net,
                    security = if (tls == "tls") "tls" else "none",
                    wsSettings = if (net == "ws") XrayWsSettings(host = hostHeader, path = path) else null,
                    tlsSettings = if (tls == "tls") XrayTlsSettings(serverName = sni) else null
                )
            )

            val fullConfig = buildFullXrayConfig(outbound, ps)

            return ConfigItem(
                name = EmojiCleaner.clean(ps).ifBlank { "VMess $host:$port" },
                subscriptionId = subscriptionId,
                protocol = "VMESS",
                server = host,
                port = port,
                network = net,
                security = tls,
                rawUri = uriString,
                rawJson = gson.toJson(fullConfig)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun parseTrojanUri(uriString: String, subscriptionId: String?): ConfigItem? {
        try {
            val noScheme = uriString.substringAfter("://")
            val fragment = if (noScheme.contains("#")) noScheme.substringAfter("#") else null
            val mainPart = noScheme.substringBefore("#")

            val hostPart = mainPart.substringBefore("?")
            val serverAndPort = hostPart.substringAfter("@")
            val host = serverAndPort.substringBefore(":")
            val port = serverAndPort.substringAfter(":", "443").toIntOrNull() ?: 443
            val remark = decodeRemark(fragment) ?: "Trojan - $host"

            return ConfigItem(
                name = EmojiCleaner.clean(remark).ifBlank { "Trojan $host:$port" },
                subscriptionId = subscriptionId,
                protocol = "TROJAN",
                server = host,
                port = port,
                network = "tcp",
                security = "tls",
                rawUri = uriString
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseShadowsocksUri(uriString: String, subscriptionId: String?): ConfigItem? {
        try {
            val noScheme = uriString.substringAfter("://")
            val fragment = if (noScheme.contains("#")) noScheme.substringAfter("#") else null
            val mainPart = noScheme.substringBefore("#")
            val remark = decodeRemark(fragment) ?: "Shadowsocks"

            var host = ""
            var port = 8388

            if (mainPart.contains("@")) {
                val serverPart = mainPart.substringAfter("@").split(":")
                host = serverPart.getOrNull(0) ?: ""
                port = serverPart.getOrNull(1)?.toIntOrNull() ?: 8388
            }

            return ConfigItem(
                name = EmojiCleaner.clean(remark).ifBlank { "Shadowsocks $host" },
                subscriptionId = subscriptionId,
                protocol = "SHADOWSOCKS",
                server = host,
                port = port,
                network = "tcp",
                security = "none",
                rawUri = uriString
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseSocksUri(uriString: String, subscriptionId: String?): ConfigItem? {
        try {
            val noScheme = uriString.substringAfter("://")
            val fragment = if (noScheme.contains("#")) noScheme.substringAfter("#") else null
            val mainPart = noScheme.substringBefore("#")
            val hostPart = if (mainPart.contains("@")) mainPart.substringAfter("@") else mainPart
            val host = hostPart.substringBefore(":")
            val port = hostPart.substringAfter(":", "1080").toIntOrNull() ?: 1080
            val remark = decodeRemark(fragment) ?: "Socks5 - $host"

            return ConfigItem(
                name = EmojiCleaner.clean(remark).ifBlank { "Socks5 $host:$port" },
                subscriptionId = subscriptionId,
                protocol = "SOCKS",
                server = host,
                port = port,
                network = "tcp",
                security = "none",
                rawUri = uriString
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun decodeRemark(fragment: String?): String? {
        if (fragment.isNullOrBlank()) return null
        return decodeSafe(fragment)
    }

    private fun decodeSafe(text: String): String {
        return try {
            URLDecoder.decode(text, StandardCharsets.UTF_8.name())
        } catch (e: Exception) {
            text
        }
    }

    /**
     * تولید ساختار نهایی و استاندارد Xray Config برای اجرا
     */
    fun buildFullXrayConfig(proxyOutbound: XrayOutbound, remarks: String): XrayConfig {
        return XrayConfig(
            remarks = remarks,
            inbounds = listOf(
                com.hnn.catng.model.XrayInbound(
                    listen = "127.0.0.1",
                    port = 10808,
                    protocol = "mixed",
                    sniffing = com.hnn.catng.model.XraySniffing(enabled = true, routeOnly = true)
                )
            ),
            outbounds = listOf(
                proxyOutbound,
                XrayOutbound(protocol = "freedom", tag = "direct"),
                XrayOutbound(protocol = "blackhole", tag = "block")
            ),
            routing = com.hnn.catng.model.XrayRouting(
                domainStrategy = "IPIfNonMatch",
                rules = listOf(
                    com.hnn.catng.model.XrayRoutingRule(
                        type = "field",
                        ip = listOf("geoip:private", "geoip:ir"),
                        outboundTag = "direct"
                    ),
                    com.hnn.catng.model.XrayRoutingRule(
                        type = "field",
                        domain = listOf("geosite:private", "geosite:category-ir"),
                        outboundTag = "direct"
                    )
                )
            )
        )
    }
}
