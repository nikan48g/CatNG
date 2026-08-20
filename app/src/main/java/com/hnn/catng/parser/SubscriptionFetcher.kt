package com.hnn.catng.parser

import android.util.Base64
import com.hnn.catng.model.ConfigItem
import com.hnn.catng.model.SubscriptionItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

object SubscriptionFetcher {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * واکشی و پردازش کامل یک سابسکریپشن از اینترنت
     */
    suspend fun fetchSubscription(
        url: String,
        customName: String? = null,
        existingSubId: String? = null
    ): Pair<SubscriptionItem, List<ConfigItem>> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "CatNG/1.0.0 v2rayNG/1.9.1 ClashforWindows/0.20.39")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to fetch subscription: HTTP ${response.code}")
        }

        val bodyString = response.body?.string() ?: throw Exception("Empty response body")

        // 1. تشخیص نام سابسکریپشن
        val detectedName = customName?.takeIf { it.isNotBlank() }
            ?: extractNameFromHeaders(response)
            ?: extractNameFromUrl(url)
            ?: "import_sub"

        val cleanSubName = EmojiCleaner.clean(detectedName).ifBlank { "import_sub" }

        // 2. دیکود محتوا (Base64 یا Plain)
        val decodedContent = decodeSubscriptionContent(bodyString)

        val subItem = SubscriptionItem(
            id = existingSubId ?: java.util.UUID.randomUUID().toString(),
            name = cleanSubName,
            url = url,
            lastUpdated = System.currentTimeMillis()
        )

        // 3. پارس کانفیگ‌ها و انتساب به این سابسکریپشن
        val configs = ConfigParser.parseInput(decodedContent, subItem.id)

        return@withContext Pair(
            subItem.copy(totalConfigCount = configs.size),
            configs
        )
    }

    private fun extractNameFromHeaders(response: okhttp3.Response): String? {
        // بررسی هدر Content-Disposition (filename=...)
        val disposition = response.header("Content-Disposition")
        if (!disposition.isNullOrBlank() && disposition.contains("filename=")) {
            val filename = disposition.substringAfter("filename=").replace("\"", "").trim()
            if (filename.isNotBlank()) {
                return try {
                    URLDecoder.decode(filename, StandardCharsets.UTF_8.name())
                } catch (e: Exception) {
                    filename
                }
            }
        }

        // بررسی هدر Profile-Title یا Subscription-Userinfo
        val title = response.header("Profile-Title")
        if (!title.isNullOrBlank()) {
            return try {
                URLDecoder.decode(title, StandardCharsets.UTF_8.name())
            } catch (e: Exception) {
                title
            }
        }

        return null
    }

    private fun extractNameFromUrl(url: String): String? {
        return try {
            if (url.contains("#")) {
                val fragment = url.substringAfter("#")
                if (fragment.isNotBlank()) {
                    return URLDecoder.decode(fragment, StandardCharsets.UTF_8.name())
                }
            }
            val uri = android.net.Uri.parse(url)
            val nameParam = uri.getQueryParameter("name") ?: uri.getQueryParameter("title")
            if (!nameParam.isNullOrBlank()) {
                return nameParam
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeSubscriptionContent(content: String): String {
        val trimmed = content.trim()
        // اگر قبلاً JSON است، تغییری لازم نیست
        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            return trimmed
        }

        // اگر لینک‌ها به صورت معمولی خط به خط هستند
        if (trimmed.startsWith("vless://") || trimmed.startsWith("vmess://") || trimmed.startsWith("ss://")) {
            return trimmed
        }

        // تلاش برای دیکود Base64
        return try {
            val decodedBytes = Base64.decode(trimmed, Base64.DEFAULT)
            val decoded = String(decodedBytes, StandardCharsets.UTF_8).trim()
            if (decoded.isNotEmpty()) decoded else trimmed
        } catch (e: Exception) {
            trimmed
        }
    }
}
