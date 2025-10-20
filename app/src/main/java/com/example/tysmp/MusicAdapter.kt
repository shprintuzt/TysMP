package com.example.tysmp

import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.IOException

data class Music(val name: String, val path: String)

class MusicAdapter(private val musicList: List<Music>) :
    RecyclerView.Adapter<MusicAdapter.MusicViewHolder>() {

    private var mediaPlayer: MediaPlayer? = null

    class MusicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val musicName: TextView = itemView.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return MusicViewHolder(view)
    }

    override fun getItemCount(): Int = musicList.size

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        val music = musicList[position]
        holder.musicName.text = music.name

        holder.itemView.setOnClickListener {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer()
            try {
                mediaPlayer?.setDataSource(music.path)
                mediaPlayer?.prepare()
                mediaPlayer?.start()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
