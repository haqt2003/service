package com.example.service.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.service.composables.formatDuration
import com.example.service.databinding.LayoutItemBinding
import com.example.service.models.Track

class TrackAdapter(
    private val tracks: MutableList<Track>,
    private val listener: OnAdapterListener
) :
    RecyclerView.Adapter<TrackAdapter.TrackViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TrackViewHolder {
        val binding = LayoutItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TrackViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = tracks[position]
        holder.bind(track)

        holder.itemView.setOnClickListener {
            listener.onClick(track)
        }
    }

    override fun getItemCount(): Int {
        return tracks.size
    }

    interface OnAdapterListener {
        fun onClick(track: Track)
    }

    class TrackViewHolder(private val binding: LayoutItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(track: Track) {
            binding.tvName.text = track.name
            binding.tvDuration.text = formatDuration(track.duration)
        }
    }
}