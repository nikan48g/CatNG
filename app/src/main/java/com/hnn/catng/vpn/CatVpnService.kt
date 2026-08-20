package com.hnn.catng.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.hnn.catng.MainActivity
import com.hnn.catng.model.ConfigItem
import com.hnn.catng.model.ConnectionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray
import java.io.File

class CatVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var coreController: CoreController? = null
    private var statsJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val trafficMonitor = RealTrafficMonitor()

    companion object {
        const val ACTION_CONNECT = "com.hnn.catng.CONNECT"
        const val ACTION_DISCONNECT = "com.hnn.catng.DISCONNECT"
        const val EXTRA_CONFIG_ID = "extra_config_id"
        const val EXTRA_CONFIG_NAME = "extra_config_name"
        const val EXTRA_CONFIG_JSON = "extra_config_json"
        const val EXTRA_SERVER_HOST = "extra_server_host"
        const val EXTRA_SERVER_PORT = "extra_server_port"
        const val CHANNEL_ID = "catng_vpn_channel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        try {
            Libv2ray.initCoreEnv(filesDir.absolutePath, filesDir.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val configName = intent.getStringExtra(EXTRA_CONFIG_NAME) ?: "CatNG Server"
                val configJson = intent.getStringExtra(EXTRA_CONFIG_JSON) ?: ""
                val serverHost = intent.getStringExtra(EXTRA_SERVER_HOST) ?: ""
                val serverPort = intent.getIntExtra(EXTRA_SERVER_PORT, 443)
                startVpn(configName, configJson, serverHost, serverPort)
            }
            ACTION_DISCONNECT -> {
                stopVpn()
            }
        }
        return START_NOT_STICKY
    }

    private fun startVpn(configName: String, configJson: String, serverHost: String, serverPort: Int) {
        try {
            VpnManager.updateStatus(ConnectionStatus.CONNECTING)

            // ۱. آماده‌سازی فایل‌های هسته و کانفیگ
            val dummyConfig = ConfigItem(
                name = configName,
                server = serverHost,
                port = serverPort,
                rawJson = configJson
            )
            val configFile = XrayCoreManager.prepareConfigFile(this, dummyConfig)
            val finalJson = configFile.readText()

            // ۲. ساخت اینترفیس TUN
            val builder = Builder()
                .setSession("CatNG ($configName)")
                .setMtu(1500)
                .addAddress("172.19.0.1", 30)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
                .addRoute("0.0.0.0", 0)

            if (serverHost.isNotBlank()) {
                try {
                    builder.addDisallowedApplication(packageName)
                } catch (_: Exception) {
                }
            }

            vpnInterface = builder.establish()

            if (vpnInterface != null) {
                val fd = vpnInterface!!.fd

                // ۳. راه‌اندازی واقعی هسته Xray با Libv2ray CoreController
                val callback = object : CoreCallbackHandler {
                    override fun onEmitStatus(status: Long, msg: String?): Long {
                        return 0
                    }

                    override fun startup(): Long {
                        return 0
                    }

                    override fun shutdown(): Long {
                        return 0
                    }
                }

                coreController = Libv2ray.newCoreController(callback)
                serviceScope.launch(Dispatchers.IO) {
                    try {
                        coreController?.startLoop(finalJson, fd)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                startForeground(NOTIFICATION_ID, buildNotification(configName, "Connected"))
                VpnManager.updateStatus(ConnectionStatus.CONNECTED)
                startRealTrafficMonitor()
            } else {
                throw Exception("Failed to establish TUN interface")
            }
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
            coreController?.stopLoop()
            coreController = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

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

    private fun startRealTrafficMonitor() {
        statsJob?.cancel()
        trafficMonitor.start()
        statsJob = serviceScope.launch {
            var seconds = 0L

            while (isActive && VpnManager.vpnState.value.status == ConnectionStatus.CONNECTED) {
                delay(1000)
                seconds++

                val sample = trafficMonitor.sample()

                VpnManager.updateLiveStats(
                    upSpeed = sample.uploadSpeedBps,
                    downSpeed = sample.downloadSpeedBps,
                    totalUp = sample.totalUploaded,
                    totalDown = sample.totalDownloaded,
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
