package com.example.tysmp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val musicList = mutableListOf<Music>()
    private lateinit var adapter: MusicAdapter
    private var isRepeating = false

    private lateinit var btnRepeat: Button
    private lateinit var btnStop: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnRepeat = findViewById(R.id.btnRepeat)
        btnStop = findViewById(R.id.btnStop)

        val recyclerView = findViewById<RecyclerView>(R.id.musicRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = MusicAdapter(musicList) { path ->
            val intent = Intent(this, MusicService::class.java)
            intent.action = "PLAY"
            intent.putExtra("path", path)
            intent.putExtra("repeat", isRepeating)
            startService(intent)
        }
        recyclerView.adapter = adapter

        btnRepeat.setOnClickListener {
            isRepeating = !isRepeating
            btnRepeat.text = if (isRepeating) "🔁 リピートON" else "🔁 リピートOFF"
        }

        btnStop.setOnClickListener {
            val intent = Intent(this, MusicService::class.java)
            intent.action = "STOP"
            startService(intent)
        }

        checkPermissionsAndLoadMusic()
    }

    private fun checkPermissionsAndLoadMusic() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ActivityCompat.checkSelfPermission(this, permission)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 100)
        } else {
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
}
