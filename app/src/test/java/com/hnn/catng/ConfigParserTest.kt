package com.hnn.catng

import com.hnn.catng.parser.ConfigParser
import com.hnn.catng.parser.EmojiCleaner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigParserTest {

    @Test
    fun testEmojiCleaner() {
        val rawName = "💦 1. VLESS - Domain : 443"
        val cleaned = EmojiCleaner.clean(rawName)
        assertEquals("1. VLESS - Domain : 443", cleaned)

        val multipleEmojis = "🚀🔥 [US] Premium Server ⚡⚡"
        val cleanedMultiple = EmojiCleaner.clean(multipleEmojis)
        assertEquals("[US] Premium Server", cleanedMultiple)
    }

    @Test
    fun testParseUserJsonArray() {
        val sampleJsonArray = """
        [
            {
                "remarks": "💦 1. VLESS - Domain : 443",
                "version": {
                    "min": "26.2.6"
                },
                "log": {
                    "loglevel": "warning"
                },
                "dns": {
                    "hosts": {
                        "dns.google": [
                            "8.8.4.4",
                            "8.8.8.8"
                        ]
                    },
                    "servers": [
                        {
                            "address": "tls://dns.google",
                            "tag": "remote-dns"
                        }
                    ],
                    "queryStrategy": "UseIP",
                    "tag": "dns"
                },
                "inbounds": [
                    {
                        "listen": "127.0.0.1",
                        "port": 10808,
                        "protocol": "mixed",
                        "settings": {
                            "auth": "noauth",
                            "udp": true
                        },
                        "tag": "mixed-in"
                    }
                ],
                "outbounds": [
                    {
                        "protocol": "vless",
                        "settings": {
                            "vnext": [
                                {
                                    "address": "lerio.dpdns.org",
                                    "port": 443,
                                    "users": [
                                        {
                                            "id": "011a3882-4bda-4c14-b977-8248b1b7ddab",
                                            "encryption": "none"
                                        }
                                    ]
                                }
                            ]
                        },
                        "streamSettings": {
                            "network": "ws",
                            "wsSettings": {
                                "host": "lerio.dpdns.org",
                                "path": "/vl/Hd0RHY0c6tv2u2KoXfbKm?ed=2560"
                            },
                            "security": "tls",
                            "tlsSettings": {
                                "serverName": "lERiO.dpDnS.OrG",
                                "fingerprint": "chrome",
                                "alpn": [
                                    "http/1.1"
                                ]
                            },
                            "sockopt": {
                                "domainStrategy": "UseIP",
                                "happyEyeballs": {
                                    "tryDelayMs": 250,
                                    "prioritizeIPv6": false,
                                    "interleave": 2,
                                    "maxConcurrentTry": 4
                                }
                            }
                        },
                        "tag": "proxy"
                    }
                ]
            },
            {
                "remarks": "💦 2. VLESS - IPv4 : 443",
                "outbounds": [
                    {
                        "protocol": "vless",
                        "settings": {
                            "vnext": [
                                {
                                    "address": "104.21.63.29",
                                    "port": 443,
                                    "users": [
                                        {
                                            "id": "011a3882-4bda-4c14-b977-8248b1b7ddab"
                                        }
                                    ]
                                }
                            ]
                        },
                        "tag": "proxy"
                    }
                ]
            }
        ]
        """.trimIndent()

        val configs = ConfigParser.parseJson(sampleJsonArray)

        assertEquals(2, configs.size)

        val first = configs[0]
        assertEquals("1. VLESS - Domain : 443", first.name)
        assertEquals("VLESS", first.protocol)
        assertEquals("lerio.dpdns.org", first.server)
        assertEquals(443, first.port)
        assertEquals("ws", first.network)
        assertEquals("tls", first.security)
        assertNotNull(first.rawJson)
        assertTrue(first.rawJson!!.contains("happyEyeballs"))

        val second = configs[1]
        assertEquals("2. VLESS - IPv4 : 443", second.name)
        assertEquals("104.21.63.29", second.server)
    }

    @Test
    fun testParseVlessUri() {
        val uri = "vless://011a3882-4bda-4c14-b977-8248b1b7ddab@myhost.com:443?type=ws&security=tls&path=%2Fws#Fast%20VLESS"
        val config = ConfigParser.parseUri(uri)

        assertNotNull(config)
        assertEquals("Fast VLESS", config?.name)
        assertEquals("VLESS", config?.protocol)
        assertEquals("myhost.com", config?.server)
        assertEquals(443, config?.port)
        assertEquals("ws", config?.network)
    }
}
