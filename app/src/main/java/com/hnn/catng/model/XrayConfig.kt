package com.hnn.catng.model

import com.google.gson.annotations.SerializedName

/**
 * مدل کامل ساختار JSON Xray
 */
data class XrayConfig(
    @SerializedName("remarks") val remarks: String? = null,
    @SerializedName("version") val version: XrayVersion? = null,
    @SerializedName("log") val log: XrayLog? = null,
    @SerializedName("dns") val dns: XrayDns? = null,
    @SerializedName("inbounds") val inbounds: List<XrayInbound>? = null,
    @SerializedName("outbounds") val outbounds: List<XrayOutbound>? = null,
    @SerializedName("routing") val routing: XrayRouting? = null,
    @SerializedName("policy") val policy: XrayPolicy? = null,
    @SerializedName("stats") val stats: Map<String, Any>? = null
)

data class XrayVersion(
    @SerializedName("min") val min: String? = "26.2.6"
)

data class XrayLog(
    @SerializedName("loglevel") val loglevel: String? = "warning"
)

data class XrayDns(
    @SerializedName("hosts") val hosts: Map<String, List<String>>? = null,
    @SerializedName("servers") val servers: List<Any>? = null,
    @SerializedName("queryStrategy") val queryStrategy: String? = "UseIP",
    @SerializedName("tag") val tag: String? = "dns"
)

data class XrayInbound(
    @SerializedName("listen") val listen: String? = "127.0.0.1",
    @SerializedName("port") val port: Int? = 10808,
    @SerializedName("protocol") val protocol: String? = "mixed",
    @SerializedName("settings") val settings: Map<String, Any>? = null,
    @SerializedName("sniffing") val sniffing: XraySniffing? = null,
    @SerializedName("tag") val tag: String? = "mixed-in"
)

data class XraySniffing(
    @SerializedName("destOverride") val destOverride: List<String>? = listOf("http", "tls"),
    @SerializedName("enabled") val enabled: Boolean = true,
    @SerializedName("routeOnly") val routeOnly: Boolean = true
)

data class XrayOutbound(
    @SerializedName("protocol") val protocol: String = "vless",
    @SerializedName("settings") val settings: XrayOutboundSettings? = null,
    @SerializedName("streamSettings") val streamSettings: XrayStreamSettings? = null,
    @SerializedName("tag") val tag: String = "proxy"
)

data class XrayOutboundSettings(
    @SerializedName("vnext") val vnext: List<XrayVnext>? = null,
    @SerializedName("servers") val servers: List<XrayServerSetting>? = null,
    @SerializedName("rules") val rules: List<Map<String, Any>>? = null,
    @SerializedName("domainStrategy") val domainStrategy: String? = null,
    @SerializedName("response") val response: Map<String, Any>? = null
)

data class XrayVnext(
    @SerializedName("address") val address: String = "",
    @SerializedName("port") val port: Int = 443,
    @SerializedName("users") val users: List<XrayUser> = emptyList()
)

data class XrayUser(
    @SerializedName("id") val id: String = "",
    @SerializedName("encryption") val encryption: String = "none",
    @SerializedName("flow") val flow: String? = null,
    @SerializedName("security") val security: String? = null,
    @SerializedName("level") val level: Int? = 0
)

data class XrayServerSetting(
    @SerializedName("address") val address: String = "",
    @SerializedName("port") val port: Int = 443,
    @SerializedName("password") val password: String? = null,
    @SerializedName("method") val method: String? = null,
    @SerializedName("users") val users: List<Map<String, Any>>? = null
)

data class XrayStreamSettings(
    @SerializedName("network") val network: String? = "ws",
    @SerializedName("security") val security: String? = "tls",
    @SerializedName("wsSettings") val wsSettings: XrayWsSettings? = null,
    @SerializedName("grpcSettings") val grpcSettings: XrayGrpcSettings? = null,
    @SerializedName("tlsSettings") val tlsSettings: XrayTlsSettings? = null,
    @SerializedName("realitySettings") val realitySettings: XrayRealitySettings? = null,
    @SerializedName("sockopt") val sockopt: XraySockOpt? = null
)

data class XrayWsSettings(
    @SerializedName("host") val host: String? = null,
    @SerializedName("path") val path: String? = null
)

data class XrayGrpcSettings(
    @SerializedName("serviceName") val serviceName: String? = null,
    @SerializedName("multiMode") val multiMode: Boolean? = false
)

data class XrayTlsSettings(
    @SerializedName("serverName") val serverName: String? = null,
    @SerializedName("fingerprint") val fingerprint: String? = "chrome",
    @SerializedName("alpn") val alpn: List<String>? = listOf("http/1.1"),
    @SerializedName("allowInsecure") val allowInsecure: Boolean? = false
)

data class XrayRealitySettings(
    @SerializedName("show") val show: Boolean? = false,
    @SerializedName("fingerprint") val fingerprint: String? = "chrome",
    @SerializedName("serverName") val serverName: String? = null,
    @SerializedName("publicKey") val publicKey: String? = null,
    @SerializedName("shortId") val shortId: String? = null,
    @SerializedName("spiderX") val spiderX: String? = null
)

data class XraySockOpt(
    @SerializedName("domainStrategy") val domainStrategy: String? = "UseIP",
    @SerializedName("happyEyeballs") val happyEyeballs: XrayHappyEyeballs? = null,
    @SerializedName("mark") val mark: Int? = null,
    @SerializedName("tproxy") val tproxy: String? = null
)

data class XrayHappyEyeballs(
    @SerializedName("tryDelayMs") val tryDelayMs: Int = 250,
    @SerializedName("prioritizeIPv6") val prioritizeIPv6: Boolean = false,
    @SerializedName("interleave") val interleave: Int = 2,
    @SerializedName("maxConcurrentTry") val maxConcurrentTry: Int = 4
)

data class XrayRouting(
    @SerializedName("domainStrategy") val domainStrategy: String? = "IPIfNonMatch",
    @SerializedName("rules") val rules: List<XrayRoutingRule>? = null
)

data class XrayRoutingRule(
    @SerializedName("type") val type: String = "field",
    @SerializedName("inboundTag") val inboundTag: List<String>? = null,
    @SerializedName("outboundTag") val outboundTag: String? = null,
    @SerializedName("port") val port: Any? = null,
    @SerializedName("network") val network: String? = null,
    @SerializedName("domain") val domain: List<String>? = null,
    @SerializedName("ip") val ip: List<String>? = null
)

data class XrayPolicy(
    @SerializedName("levels") val levels: Map<String, Any>? = null,
    @SerializedName("system") val system: Map<String, Any>? = null
)
