package com.example.core.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity

/**
 * Phase 6: Next-Gen Processing, Voice & Audio
 * 
 * Foreground Service for Lock-Screen Playback Controls.
 * Allows the Shadowing Coach or AI Podcast to gracefully continue out-of-app.
 */
class AudioPlaybackService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // Mock Play/Pause Intent
        val playPauseIntent = PendingIntent.getBroadcast(
            this, 1, Intent("ACTION_PLAY_PAUSE"), PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, "AUDIO_CHANNEL")
            .setContentTitle("Clinical Shadowing Coach")
            .setContentText("Playing: Cardiology Consult (1.0x Speed)")
            .setSmallIcon(android.R.drawable.ic_media_play) // Use actual bespoke sound design icon in prod
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Pause", playPauseIntent)
            // Simplified for compilation without adding androidx.media
            .build()

        startForeground(1, notification)

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            "AUDIO_CHANNEL",
            "Audio Playback Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
