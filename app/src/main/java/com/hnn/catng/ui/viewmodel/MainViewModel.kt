package com.hnn.catng.ui.viewmodel

import android.app.Application
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hnn.catng.data.ConfigRepository
import com.hnn.catng.model.ConfigItem
import com.hnn.catng.model.ConnectionStatus
import com.hnn.catng.model.SortType
import com.hnn.catng.model.SubscriptionItem
import com.hnn.catng.model.VpnState
import com.hnn.catng.model.XrayOutbound
import com.hnn.catng.model.XrayOutboundSettings
import com.hnn.catng.model.XrayStreamSettings
import com.hnn.catng.model.XrayTlsSettings
import com.hnn.catng.model.XrayUser
import com.hnn.catng.model.XrayVnext
import com.hnn.catng.model.XrayWsSettings
import com.hnn.catng.parser.ConfigParser
import com.hnn.catng.parser.SubscriptionFetcher
import com.hnn.catng.ping.PingTester
import com.hnn.catng.vpn.VpnManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = ConfigRepository(application)
    val vpnState: StateFlow<VpnState> = VpnManager.vpnState

    private val _isTestingPings = MutableStateFlow(false)
    val isTestingPings: StateFlow<Boolean> = _isTestingPings.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    val isFirstLaunch: StateFlow<Boolean> = repository.isFirstLaunch
    val subscriptions: StateFlow<List<SubscriptionItem>> = repository.subscriptions
    val selectedSubscriptionId: StateFlow<String?> = repository.selectedSubscriptionId
    val selectedConfigId: StateFlow<String?> = repository.selectedConfigId
    val sortType: StateFlow<SortType> = repository.sortType

    // لیست فیلتر شده و مرتب شده کانفیگ‌ها
    val displayConfigs: StateFlow<List<ConfigItem>> = combine(
        repository.configs,
        repository.selectedSubscriptionId,
        repository.sortType
    ) { configs, selectedSubId, sort ->
        val filtered = if (selectedSubId == null || selectedSubId == "ALL") {
            configs
        } else {
            configs.filter { it.subscriptionId == selectedSubId }
        }

        when (sort) {
            SortType.BEST_PING -> filtered.sortedWith(compareBy(
                { it.pingMs == null || it.pingMs <= 0 },
                { it.pingMs ?: Long.MAX_VALUE }
            ))
            SortType.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            SortType.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
            SortType.NEWEST -> filtered.sortedByDescending { it.createdAt }
            SortType.OLDEST -> filtered.sortedBy { it.createdAt }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun completeFirstLaunch() {
        repository.markFirstLaunchComplete()
    }

    fun selectConfig(configId: String) {
        repository.selectConfig(configId)
    }

    fun selectSubscriptionFilter(subId: String?) {
        repository.selectSubscriptionFilter(subId)
    }

    fun setSortType(sortType: SortType) {
        repository.setSortType(sortType)
    }

    fun deleteConfig(configId: String) {
        repository.deleteConfig(configId)
    }

    fun deleteSubscription(subscriptionId: String) {
        repository.deleteSubscription(subscriptionId)
    }

    /**
     * چسباندن خودکار از کلیپ‌بورد با تشخیص هوشمند
     */
    fun pasteFromClipboard() {
        viewModelScope.launch(Dispatchers.IO) {
            val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (!clipboard.hasPrimaryClip() || clipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == false) {
                _userMessage.emit("Clipboard is empty")
                return@launch
            }

            val clipItem = clipboard.primaryClip?.getItemAt(0)
            val text = clipItem?.text?.toString()?.trim()

            if (text.isNullOrBlank()) {
                _userMessage.emit("Clipboard is empty")
                return@launch
            }

            // بررسی اگر URL سابسکریپشن است
            if ((text.startsWith("http://") || text.startsWith("https://")) && !text.contains("\n")) {
                fetchAndAddSubscription(text, null)
                return@launch
            }

            // بررسی اگر کانفیگ JSON یا لینک است
            val parsedConfigs = ConfigParser.parseInput(text)
            if (parsedConfigs.isNotEmpty()) {
                repository.addConfigs(parsedConfigs)
                _userMessage.emit("Added ${parsedConfigs.size} configs successfully")
            } else {
                _userMessage.emit("No valid config found in clipboard")
            }
        }
    }

    /**
     * افزودن دستی سابسکریپشن
     */
    fun fetchAndAddSubscription(url: String, customName: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _userMessage.emit("Fetching subscription...")
                val (subItem, configs) = SubscriptionFetcher.fetchSubscription(url, customName)
                repository.addOrUpdateSubscription(subItem, configs)
                _userMessage.emit("Subscription \"${subItem.name}\" added with ${configs.size} configs")
            } catch (e: Exception) {
                e.printStackTrace()
                _userMessage.emit("Failed to fetch subscription: ${e.message}")
            }
        }
    }

    /**
     * افزودن دستی کانفیگ با پارامترهای مختلف پروتکل
     */
    fun addManualConfig(
        protocol: String,
        remarks: String,
        server: String,
        port: Int,
        userId: String,
        network: String,
        path: String,
        sni: String,
        security: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val outbound = XrayOutbound(
                protocol = protocol.lowercase(),
                tag = "proxy",
                settings = XrayOutboundSettings(
                    vnext = listOf(
                        XrayVnext(
                            address = server,
                            port = port,
                            users = listOf(XrayUser(id = userId, encryption = "none"))
                        )
                    )
                ),
                streamSettings = XrayStreamSettings(
                    network = network,
                    security = security,
                    wsSettings = if (network == "ws") XrayWsSettings(host = sni, path = path) else null,
                    tlsSettings = if (security == "tls") XrayTlsSettings(serverName = sni) else null
                )
            )

            val fullConfig = ConfigParser.buildFullXrayConfig(outbound, remarks)
            val configItem = ConfigItem(
                name = remarks.ifBlank { "$protocol $server:$port" },
                protocol = protocol.uppercase(),
                server = server,
                port = port,
                network = network,
                security = security,
                rawJson = ConfigParser.gson.toJson(fullConfig)
            )

            repository.addConfigs(listOf(configItem))
            _userMessage.emit("Config added")
        }
    }

    /**
     * تست پینگ تمامی کانفیگ‌های لیست در حال نمایش
     */
    fun testAllCurrentConfigs() {
        if (_isTestingPings.value) return
        val currentList = displayConfigs.value
        if (currentList.isEmpty()) {
            viewModelScope.launch { _userMessage.emit("No configs to test") }
            return
        }

        viewModelScope.launch {
            _isTestingPings.value = true
            PingTester.testAll(getApplication<Application>(), currentList) { configId, pingMs ->
                repository.updateConfigPing(configId, pingMs)
            }
            _isTestingPings.value = false
            _userMessage.emit("Ping test completed")
        }
    }

    /**
     * تست تکی پینگ یک کانفیگ
     */
    fun testSinglePing(config: ConfigItem) {
        viewModelScope.launch {
            val ping = PingTester.testPing(getApplication<Application>(), config)
            repository.updateConfigPing(config.id, ping)
        }
    }

    /**
     * سوییچ اتصال/قطع VPN
     */
    fun toggleConnection(context: Context) {
        val currentState = vpnState.value.status
        if (currentState == ConnectionStatus.CONNECTED || currentState == ConnectionStatus.CONNECTING) {
            VpnManager.stopVpn(context)
        } else {
            val targetConfig = displayConfigs.value.firstOrNull { it.id == selectedConfigId.value }
                ?: repository.configs.value.firstOrNull()

            if (targetConfig == null) {
                viewModelScope.launch { _userMessage.emit("Please add and select a config first") }
                return
            }

            repository.selectConfig(targetConfig.id)
            VpnManager.startVpn(context, targetConfig)
        }
    }
}
