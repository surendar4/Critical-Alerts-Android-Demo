package com.surendar4.criticalnotifications.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.surendar4.criticalnotifications.BaseApplication
import com.surendar4.criticalnotifications.R
import kotlin.math.ceil

object NotificationNotifier : ContextWrapper(BaseApplication.getInstance()) {

    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }

    fun postCriticalNotification(ringerVolume: Float) {
        val channelId = createDefaultChannel(true)
        val notification = buildNotification(channelId, "Critical Notification", "It's a critical notification")

        overrideSilentModeAndConfigureCustomVolume(ringerVolume)
        notificationManager.notify(getNotificationId(), notification)
    }

    fun postNormalNotification() {
        val channelId = createDefaultChannel(false)
        val notification = buildNotification(channelId, "Normal Notification", "It's a normal notification")

        notificationManager.notify(getNotificationId(), notification)
    }

    private fun createDefaultChannel(isCriticalNotification: Boolean = false): String {
        val channelId = "default"
        val notificationChannel = NotificationChannel(channelId, "Default", NotificationManager.IMPORTANCE_DEFAULT)
        notificationChannel.setBypassDnd(isCriticalNotification)
        notificationManager.createNotificationChannel(notificationChannel)
        return channelId
    }

    private fun buildNotification(channel: String, title: String, message: String): Notification {
        return NotificationCompat.Builder(this, channel)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun getNotificationId(): Int {
        val prefs: SharedPreferences = getSharedPreferences("notification_ids", MODE_PRIVATE)
        val notificationId = prefs.getInt("notification_id", 0)
        prefs.edit().putInt("notification_id", notificationId + 1).apply()
        return notificationId
    }

    private fun overrideSilentModeAndConfigureCustomVolume(ringToneVolume: Float?) {
        try {
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            var originalRingMode = audioManager.ringerMode
            val originalNotificationVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
            val maxNotificationVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)

            //When DND mode is enabled, we get ringerMode as silent even though actual ringer mode is Normal
            val isDndModeEnabled = notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
            if (isDndModeEnabled && originalRingMode == AudioManager.RINGER_MODE_SILENT && originalNotificationVolume != 0) {
                originalRingMode = AudioManager.RINGER_MODE_NORMAL
            }

            val newVolume = if (ringToneVolume != null) {
                ceil(maxNotificationVolume * ringToneVolume).toInt()
            } else {
                originalNotificationVolume
            }

            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, newVolume, 0)

            //Resetting the original ring mode, volume and dnd mode
            Handler(Looper.getMainLooper()).postDelayed({
                audioManager.ringerMode = originalRingMode
                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, originalNotificationVolume, 0)
            }, getSoundFileDuration(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)))
        } catch (ex: Exception) {
        }
    }

    private fun getSoundFileDuration(uri: Uri): Long {
        return try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(this, uri)
            val durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toLong() ?: 0
        } catch (ex: Exception) {
            5000
        }
    }
}