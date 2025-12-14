package com.example.tysmp

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class MusicService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var isRepeating = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            "PLAY" -> {
                val path = intent.getStringExtra("path")
                val repeat = intent.getBooleanExtra("repeat", false)
                startMusic(path, repeat)
            }
            "STOP" -> stopMusic()
        }

        return START_STICKY
    }

    private fun startMusic(path: String?, repeat: Boolean) {
        if (path == null) return

        stopMusic()

        mediaPlayer = MediaPlayer().apply {
            setDataSource(path)
            prepare()
            isLooping = repeat
            start()
        }

        startForeground(1, createNotification("再生中: ${path.substringAfterLast('/')}"))
    }

    private fun stopMusic() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
        stopForeground(true)
        stopSelf()
    }

    private fun createNotification(title: String): Notification {
        val channelId = "music_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "音楽再生",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText("バックグラウンド再生中")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMusic()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
