package com.hnn.catng.vpn

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.hnn.catng.model.ConfigItem
import com.hnn.catng.model.ConnectionStatus
import com.hnn.catng.model.VpnState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object VpnManager {
    private val _vpnState = MutableStateFlow(VpnState())
    val vpnState: StateFlow<VpnState> = _vpnState.asStateFlow()

    fun updateStatus(status: ConnectionStatus) {
        _vpnState.value = _vpnState.value.copy(status = status)
    }

    fun updateLiveStats(
        upSpeed: Long,
        downSpeed: Long,
        totalUp: Long,
        totalDown: Long,
        durationSeconds: Long
    ) {
        _vpnState.value = _vpnState.value.copy(
            uploadSpeedBps = upSpeed,
            downloadSpeedBps = downSpeed,
            totalUploadedBytes = totalUp,
            totalDownloadedBytes = totalDown,
            connectedDurationSeconds = durationSeconds
        )
    }

    fun startVpn(context: Context, config: ConfigItem) {
        val intent = VpnService.prepare(context)
        if (intent != null && context is Activity) {
            // نیاز به تأیید کاربر برای اتصال VPN
            context.startActivityForResult(intent, 100)
            return
        }

        _vpnState.value = _vpnState.value.copy(
            status = ConnectionStatus.CONNECTING,
            activeConfigId = config.id
        )

        val serviceIntent = Intent(context, CatVpnService::class.java).apply {
            action = CatVpnService.ACTION_CONNECT
            putExtra(CatVpnService.EXTRA_CONFIG_NAME, config.name)
            putExtra(CatVpnService.EXTRA_CONFIG_JSON, config.rawJson ?: "")
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    fun stopVpn(context: Context) {
        val serviceIntent = Intent(context, CatVpnService::class.java).apply {
            action = CatVpnService.ACTION_DISCONNECT
        }
        context.startService(serviceIntent)
    }
}
