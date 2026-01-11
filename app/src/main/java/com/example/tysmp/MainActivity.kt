package com.example.tysmp

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val musicList = mutableListOf<Music>()
    private lateinit var adapter: MusicAdapter
    private var mediaPlayer: MediaPlayer? = null

    private lateinit var btnPause: Button
    private lateinit var btnStop: Button
    private lateinit var btnRepeat: Button

    private var isPlaying = false

    private var isRepeating = false // 🔁 現在のリピート状態

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != MusicConstants.ACTION_STATE_CHANGED) return

            isPlaying = intent.getBooleanExtra(
                MusicConstants.EXTRA_IS_PLAYING, false
            )
            val path = intent.getStringExtra(MusicConstants.EXTRA_PATH)

            // 🔘 停止ボタン
            btnStop.isEnabled = isPlaying
            btnStop.alpha = if (isPlaying) 1.0f else 0.5f

            // 🎨 RecyclerView の再生中表示
            path?.let {
                adapter.setPlaying(it)
            }
            if (!isPlaying) {
                adapter.setPlaying("") // 全解除
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnPause = findViewById(R.id.btnPause)
        btnStop = findViewById(R.id.btnStop)
        btnRepeat = findViewById(R.id.btnRepeat)

        val recyclerView = findViewById<RecyclerView>(R.id.musicRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = MusicAdapter(musicList) { path ->
            if (!isPlaying) {
                playMusic(path)
            } else {
                showPlayNextConfirmDialog(path)
            }
        }
        recyclerView.adapter = adapter

        checkPermissionsAndLoadMusic()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }

        // 🎵 一時停止ボタン
        btnPause.setOnClickListener {
            pauseMusic()
        }

        // 🛑 停止ボタン
        btnStop.setOnClickListener {
            if (!isPlaying) {
                stopMusic()
            } else {
                showStopConfirmDialog()
            }
        }

        // 🔁 リピートボタン
        btnRepeat.setOnClickListener {
            val intent = Intent(this, MusicService::class.java)
            intent.action = MusicService.ACTION_REPEAT
            intent.putExtra("repeat", true)
            startService(intent)
        }
    }

    private fun playMusic(path: String) {
        val intent = Intent(this, MusicService::class.java)
        intent.action = "PLAY"
        intent.putExtra("path", path)
        startService(intent)
    }

    private fun stopMusic() {
        val intent = Intent(this, MusicService::class.java)
        intent.action = "STOP"
        startService(intent)
    }

    private fun pauseMusic() {
        val intent = Intent(this, MusicService::class.java)
        intent.action = MusicService.ACTION_PAUSE
        startService(intent)
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

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (!granted) {
                Toast.makeText(
                    this,
                    "通知を許可しないとバックグラウンド再生できません",
                    Toast.LENGTH_SHORT
                ).show()
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
                    .removeSuffix(".mp3")
                    .replace("_", " ")
                val path = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                musicList.add(Music(name, path))
            }
        }

        adapter.notifyDataSetChanged()
    }

    private fun showStopConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("うた を とめますか？")
            .setMessage("うた を とめたい ひと は はい をおしてね♪")
            .setPositiveButton("はい") { _, _ ->
                stopMusic()
            }
            .setNegativeButton("いいえ", null)
            .show()
    }

    private fun showPlayNextConfirmDialog(path: String) {
        val value = (0..1).random()
        val value2 = (0..1).random()
        val answer = if (value == 1) "はい" else "うん"
        val anotherText = if (value == 0) "はい" else "うん"
        val answerPosition = if (value2 == 1) "right" else "left"
        val rightText = if (answerPosition == "right") answer else anotherText
        val leftText = if (answerPosition == "left") answer else anotherText
        AlertDialog.Builder(this)
            .setTitle("つぎ の うた を ながしますか？")
            .setMessage("つぎ の うた を ながしたい ひと は $answer をおしてね♪")
            .setPositiveButton(rightText) { _, _ ->
                if (answerPosition == "right") {
                    stopCurrentAndPlayNext(path)
                }
            }
            .setNegativeButton(leftText) { _, _ ->
                if (answerPosition == "left") {
                    stopCurrentAndPlayNext(path)
                }
            }
            .show()
    }

    private fun stopCurrentAndPlayNext(path: String) {
        // ⏹ 現在の再生を停止
        stopMusic()

        // ▶ 次の曲を再生
        playMusic(path)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onStart() {
        super.onStart()
        registerReceiver(
            stateReceiver,
            IntentFilter(MusicConstants.ACTION_STATE_CHANGED),
            RECEIVER_NOT_EXPORTED
        )
    }
    override fun onStop() {
        super.onStop()
        unregisterReceiver(stateReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMusic()
    }
}
