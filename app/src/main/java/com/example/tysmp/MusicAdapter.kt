package com.example.tysmp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Music(val name: String, val path: String)

class MusicAdapter(
    private val musicList: List<Music>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<MusicAdapter.MusicViewHolder>() {

    private var playingPath: String? = null

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

        // 🎨 再生中の曲だけ色を変える
        if (music.path == playingPath) {
            holder.itemView.setBackgroundColor(
                holder.itemView.context.getColor(android.R.color.holo_blue_light)
            )
        } else {
            holder.itemView.setBackgroundColor(
                holder.itemView.context.getColor(android.R.color.transparent)
            )
        }

        holder.itemView.setOnClickListener {
            onItemClick(music.path)
        }
    }

    // 🔔 再生中の曲を更新
    fun setPlaying(path: String) {
        val oldIndex = musicList.indexOfFirst { it.path == playingPath }
        val newIndex = musicList.indexOfFirst { it.path == path }

        playingPath = path

        if (oldIndex >= 0) notifyItemChanged(oldIndex)
        if (newIndex >= 0) notifyItemChanged(newIndex)
    }
}
