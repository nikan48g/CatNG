package com.hnn.catng.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hnn.catng.model.ConfigItem
import com.hnn.catng.model.SortType
import com.hnn.catng.model.SubscriptionItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConfigRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("catng_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _configs = MutableStateFlow<List<ConfigItem>>(emptyList())
    val configs: StateFlow<List<ConfigItem>> = _configs.asStateFlow()

    private val _subscriptions = MutableStateFlow<List<SubscriptionItem>>(emptyList())
    val subscriptions: StateFlow<List<SubscriptionItem>> = _subscriptions.asStateFlow()

    private val _selectedSubscriptionId = MutableStateFlow<String?>("ALL")
    val selectedSubscriptionId: StateFlow<String?> = _selectedSubscriptionId.asStateFlow()

    private val _selectedConfigId = MutableStateFlow<String?>(null)
    val selectedConfigId: StateFlow<String?> = _selectedConfigId.asStateFlow()

    private val _sortType = MutableStateFlow(SortType.NEWEST)
    val sortType: StateFlow<SortType> = _sortType.asStateFlow()

    private val _isFirstLaunch = MutableStateFlow(true)
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val isFirst = prefs.getBoolean("is_first_launch", true)
        _isFirstLaunch.value = isFirst

        val lastSelectedConfig = prefs.getString("selected_config_id", null)
        _selectedConfigId.value = lastSelectedConfig

        val configsJson = prefs.getString("saved_configs", "[]") ?: "[]"
        val subsJson = prefs.getString("saved_subscriptions", "[]") ?: "[]"

        val configType = object : TypeToken<List<ConfigItem>>() {}.type
        val subType = object : TypeToken<List<SubscriptionItem>>() {}.type

        val loadedConfigs: List<ConfigItem> = try {
            gson.fromJson(configsJson, configType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        val loadedSubs: List<SubscriptionItem> = try {
            gson.fromJson(subsJson, subType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        _configs.value = loadedConfigs
        _subscriptions.value = loadedSubs

        if (_selectedConfigId.value == null && loadedConfigs.isNotEmpty()) {
            _selectedConfigId.value = loadedConfigs.first().id
        }
    }

    fun markFirstLaunchComplete() {
        _isFirstLaunch.value = false
        prefs.edit().putBoolean("is_first_launch", false).apply()
    }

    fun selectConfig(id: String) {
        _selectedConfigId.value = id
        prefs.edit().putString("selected_config_id", id).apply()
    }

    fun selectSubscriptionFilter(subscriptionId: String?) {
        _selectedSubscriptionId.value = subscriptionId ?: "ALL"
    }

    fun setSortType(sortType: SortType) {
        _sortType.value = sortType
    }

    fun addConfigs(newConfigs: List<ConfigItem>) {
        val current = _configs.value.toMutableList()
        current.addAll(0, newConfigs) // اضافه شدن به ابتدای لیست
        saveConfigs(current)

        if (_selectedConfigId.value == null && current.isNotEmpty()) {
            selectConfig(current.first().id)
        }
    }

    fun addOrUpdateSubscription(subscription: SubscriptionItem, configsForSub: List<ConfigItem>) {
        val currentSubs = _subscriptions.value.toMutableList()
        val existingIndex = currentSubs.indexOfFirst { it.id == subscription.id }

        if (existingIndex >= 0) {
            currentSubs[existingIndex] = subscription
        } else {
            currentSubs.add(subscription)
        }
        saveSubscriptions(currentSubs)

        // حذف کانفیگ‌های قدیمی این ساب و افزودن کانفیگ‌های جدید
        val otherConfigs = _configs.value.filter { it.subscriptionId != subscription.id }
        val updatedConfigs = configsForSub + otherConfigs
        saveConfigs(updatedConfigs)

        if (_selectedConfigId.value == null && updatedConfigs.isNotEmpty()) {
            selectConfig(updatedConfigs.first().id)
        }
    }

    fun deleteConfig(configId: String) {
        val updated = _configs.value.filter { it.id != configId }
        saveConfigs(updated)
        if (_selectedConfigId.value == configId) {
            _selectedConfigId.value = updated.firstOrNull()?.id
            prefs.edit().putString("selected_config_id", _selectedConfigId.value).apply()
        }
    }

    fun deleteSubscription(subscriptionId: String) {
        val updatedSubs = _subscriptions.value.filter { it.id != subscriptionId }
        saveSubscriptions(updatedSubs)

        val updatedConfigs = _configs.value.filter { it.subscriptionId != subscriptionId }
        saveConfigs(updatedConfigs)

        if (_selectedSubscriptionId.value == subscriptionId) {
            _selectedSubscriptionId.value = "ALL"
        }
    }

    fun updateConfigPing(configId: String, pingMs: Long) {
        val updated = _configs.value.map {
            if (it.id == configId) it.copy(pingMs = pingMs) else it
        }
        _configs.value = updated
    }

    fun clearAllConfigs() {
        saveConfigs(emptyList())
        _selectedConfigId.value = null
    }

    private fun saveConfigs(list: List<ConfigItem>) {
        _configs.value = list
        prefs.edit().putString("saved_configs", gson.toJson(list)).apply()
    }

    private fun saveSubscriptions(list: List<SubscriptionItem>) {
        _subscriptions.value = list
        prefs.edit().putString("saved_subscriptions", gson.toJson(list)).apply()
    }
}
