package com.example.tysmp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.IOException

class MusicService : Service() {

    companion object {
        const val ACTION_PLAY = "PLAY"
        const val ACTION_PAUSE = "PAUSE"
        const val ACTION_STOP = "STOP"
        const val ACTION_REPEAT = "REPEAT"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var isRepeating = false // 🔁 現在のリピート状態

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            ACTION_PLAY -> {
                val path: String? = intent.getStringExtra("path")
                startMusic(path)
            }
            ACTION_STOP -> stopMusic()
            ACTION_PAUSE -> pauseMusic()
            ACTION_REPEAT -> {
                val repeat = intent.getBooleanExtra("repeat", false)
                mediaPlayer?.isLooping = repeat
            }
        }

        return START_STICKY
    }

    private fun startMusic(path: String?) {
        if (path == null) return;

        stopMusic() // 再生中ならリセット

        mediaPlayer = MediaPlayer()
        try {
            mediaPlayer?.setOnCompletionListener {
                // 再生終了時の処理
                Log.d("MediaPlayer", "再生が終了しました")
                isPlaying = true
            }
            mediaPlayer?.setDataSource(path)
            mediaPlayer?.prepare()
            mediaPlayer?.start()
            isPlaying = true
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // 🔔 これが「バックグラウンドで生き続ける」条件
        startForeground(1, createNotification())

        sendState(true, path)
    }

    private fun stopMusic() {
        mediaPlayer?.let {
            if (it.isPlaying || it.currentPosition > 0) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
        isPlaying = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        sendState(false, null)
    }

    private fun pauseMusic() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.start()
            }
        }
    }

    private fun sendState(isPlaying: Boolean, path: String?) {
        Log.d("MusicService", "sendState")
        val intent = Intent(MusicConstants.ACTION_STATE_CHANGED)
        intent.setPackage(packageName) // ★超重要
        intent.putExtra(MusicConstants.EXTRA_IS_PLAYING, isPlaying)
        intent.putExtra(MusicConstants.EXTRA_PATH, path)
        sendBroadcast(intent)
    }

    private fun createNotification(): Notification {
        val channelId = "music_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "音楽再生",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("音楽再生中")
            .setContentText("バックグラウンド再生")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
    }
}