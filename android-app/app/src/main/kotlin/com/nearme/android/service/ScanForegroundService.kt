package com.nearme.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.nearme.android.R
import com.nearme.android.data.IdentityStore

/**
 * Keeps BLE scanning alive while the app isn't in the foreground. A foreground
 * service is required for any BLE scan meant to run longer than a few minutes
 * on modern Android — background scans without one are heavily throttled or
 * killed outright.
 */
class ScanForegroundService : Service() {
    private lateinit var identityStore: IdentityStore

    override fun onCreate() {
        super.onCreate()
        identityStore = IdentityStore.get(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification(), foregroundServiceType())
        identityStore.startScanning()
        return START_STICKY
    }

    override fun onDestroy() {
        identityStore.stopScanning()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun foregroundServiceType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        } else {
            0
        }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.scan_notification_title),
                NotificationManager.IMPORTANCE_LOW,
            )
            manager.createNotificationChannel(channel)
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.scan_notification_title))
            .setContentText(getString(R.string.scan_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "nearme_scan"
        private const val NOTIFICATION_ID = 1
    }
}
