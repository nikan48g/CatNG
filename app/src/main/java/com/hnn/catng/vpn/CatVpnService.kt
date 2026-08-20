package com.hnn.catng.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.hnn.catng.MainActivity
import com.hnn.catng.model.ConnectionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CatVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var statsJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default)

    companion object {
        const val ACTION_CONNECT = "com.hnn.catng.CONNECT"
        const val ACTION_DISCONNECT = "com.hnn.catng.DISCONNECT"
        const val EXTRA_CONFIG_NAME = "extra_config_name"
        const val EXTRA_CONFIG_JSON = "extra_config_json"
        const val CHANNEL_ID = "catng_vpn_channel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val configName = intent.getStringExtra(EXTRA_CONFIG_NAME) ?: "CatNG Server"
                val configJson = intent.getStringExtra(EXTRA_CONFIG_JSON) ?: ""
                startVpn(configName, configJson)
            }
            ACTION_DISCONNECT -> {
                stopVpn()
            }
        }
        return START_NOT_STICKY
    }

    private fun startVpn(configName: String, configJson: String) {
        try {
            VpnManager.updateStatus(ConnectionStatus.CONNECTING)

            val builder = Builder()
                .setSession("CatNG VPN")
                .setMtu(1500)
                .addAddress("172.19.0.1", 30)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
                .addRoute("0.0.0.0", 0)

            vpnInterface = builder.establish()

            startForeground(NOTIFICATION_ID, buildNotification(configName, "Connected"))
            VpnManager.updateStatus(ConnectionStatus.CONNECTED)

            // شروع شبیه‌ساز آمار ترافیک زنده و تست زنده
            startStatsUpdater()
        } catch (e: Exception) {
            e.printStackTrace()
            VpnManager.updateStatus(ConnectionStatus.DISCONNECTED)
            stopSelf()
        }
    }

    private fun stopVpn() {
        VpnManager.updateStatus(ConnectionStatus.DISCONNECTING)
        statsJob?.cancel()
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        VpnManager.updateStatus(ConnectionStatus.DISCONNECTED)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startStatsUpdater() {
        statsJob?.cancel()
        statsJob = serviceScope.launch {
            var totalDown = 0L
            var totalUp = 0L
            var seconds = 0L

            while (isActive && VpnManager.vpnState.value.status == ConnectionStatus.CONNECTED) {
                delay(1000)
                seconds++

                // شبیه‌سازی فعالیت ترافیک زنده
                val downSpeed = (150_000L..1_800_000L).random()
                val upSpeed = (30_000L..450_000L).random()
                totalDown += downSpeed
                totalUp += upSpeed

                VpnManager.updateLiveStats(
                    upSpeed = upSpeed,
                    downSpeed = downSpeed,
                    totalUp = totalUp,
                    totalDown = totalDown,
                    durationSeconds = seconds
                )
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "CatNG VPN Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "CatNG active VPN tunnel status"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(configName: String, statusText: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle("CatNG: $configName")
            .setContentText(statusText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
    }
}
