package com.example.tysmp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

    private var isPlaying = false

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
//            mediaPlayer?.let {
//                if (it.isPlaying) {
//                    it.pause()
//                    btnPause.text = "▶ 再開"
//                } else {
//                    it.start()
//                    btnPause.text = "⏸ 一時停止"
//                }
//            }
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
            isRepeating = !isRepeating
            mediaPlayer?.isLooping = isRepeating
            btnRepeat.text = if (isRepeating) "🔁 リピートON" else "🔁 リピートOFF"
        }
    }

    private fun playMusic(path: String) {
        val intent = Intent(this, MusicService::class.java)
        intent.action = "PLAY"
        intent.putExtra("path", path)
        startService(intent)

//        stopMusic() // 再生中ならリセット
//
//        mediaPlayer = MediaPlayer()
//        try {
//            mediaPlayer?.setOnCompletionListener {
//                // 再生終了時の処理
//                Log.d("MediaPlayer", "再生が終了しました")
//                isPlaying = false
//                adapter.setPlaying("")
//            }
//            mediaPlayer?.setDataSource(path)
//            mediaPlayer?.prepare()
//            mediaPlayer?.isLooping = isRepeating // 🔁 現在の設定を反映
//            mediaPlayer?.start()
//            btnPause.text = "⏸ 一時停止"
//            isPlaying = true
//            // 🎨 RecyclerView に再生中を通知
//            adapter.setPlaying(path)
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
    }

    private fun stopMusic() {
        val intent = Intent(this, MusicService::class.java)
        intent.action = "STOP"
        startService(intent)

//        mediaPlayer?.let {
//            if (it.isPlaying || it.currentPosition > 0) {
//                it.stop()
//            }
//            it.release()
//        }
//        mediaPlayer = null
//        adapter.setPlaying("")
//        btnPause.text = "⏸ 一時停止"
//        isPlaying = false
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

    override fun onDestroy() {
        super.onDestroy()
        stopMusic()
    }
}
