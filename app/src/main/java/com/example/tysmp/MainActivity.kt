package com.example.tysmp

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val musicList = mutableListOf<Music>()
    private lateinit var adapter: MusicAdapter
    private var mediaPlayer: MediaPlayer? = null

    private lateinit var btnPause: Button
    private lateinit var btnStop: Button
    private lateinit var btnRepeat: Button

    private var isRepeating = false // 🔁 現在のリピート状態

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnPause = findViewById(R.id.btnPause)
        btnStop = findViewById(R.id.btnStop)
        btnRepeat = findViewById(R.id.btnRepeat)

        val recyclerView = findViewById<RecyclerView>(R.id.musicRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = MusicAdapter(musicList) { path ->
            playMusic(path)
        }
        recyclerView.adapter = adapter

        checkPermissionsAndLoadMusic()

        // 🎵 一時停止ボタン
        btnPause.setOnClickListener {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    btnPause.text = "▶ 再開"
                } else {
                    it.start()
                    btnPause.text = "⏸ 一時停止"
                }
            }
        }

        // 🛑 停止ボタン
        btnStop.setOnClickListener {
            stopMusic()
        }

        // 🔁 リピートボタン
        btnRepeat.setOnClickListener {
            isRepeating = !isRepeating
            mediaPlayer?.isLooping = isRepeating
            btnRepeat.text = if (isRepeating) "🔁 リピートON" else "🔁 リピートOFF"
        }
    }

    private fun playMusic(path: String) {
        stopMusic() // 再生中ならリセット

        mediaPlayer = MediaPlayer()
        try {
            mediaPlayer?.setDataSource(path)
            mediaPlayer?.prepare()
            mediaPlayer?.isLooping = isRepeating // 🔁 現在の設定を反映
            mediaPlayer?.start()
            btnPause.text = "⏸ 一時停止"
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun stopMusic() {
        mediaPlayer?.let {
            if (it.isPlaying || it.currentPosition > 0) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
        btnPause.text = "⏸ 一時停止"
    }

    private fun checkPermissionsAndLoadMusic() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 100)
        } else {
            loadMusic()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadMusic()
        }
    }

    private fun loadMusic() {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA
        )

        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            MediaStore.Audio.Media.DATE_ADDED + " DESC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val name = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
                val path = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                musicList.add(Music(name, path))
            }
        }

        adapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMusic()
    }
}
