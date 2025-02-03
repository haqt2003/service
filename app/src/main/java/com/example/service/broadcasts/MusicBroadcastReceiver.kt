package com.example.service.broadcasts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.service.models.Track
import com.example.service.services.NEXT
import com.example.service.services.PLAY_PAUSE
import com.example.service.services.PREV
import com.example.service.services.UPDATE_TRACK

class MusicBroadcastReceiver(private val listener: MusicReceiverListener) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            PREV -> listener.onPrevClicked()
            NEXT -> listener.onNextClicked()
            PLAY_PAUSE -> listener.onPlayPauseClicked()
            UPDATE_TRACK -> {
                val track = intent.getParcelableExtra<Track>("track")
                val isPlaying = intent.getBooleanExtra("isPlaying", false)
                val currentDuration = intent.getLongExtra("currentDuration", 0)
                if (track != null) listener.onTrackChanged(track, isPlaying, currentDuration)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun register(context: Context) {
        val filter = IntentFilter().apply {
            addAction(PREV)
            addAction(NEXT)
            addAction(PLAY_PAUSE)
            addAction(UPDATE_TRACK)
        }
        context.registerReceiver(this, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    fun unregister(context: Context) {
        context.unregisterReceiver(this)
    }

    interface MusicReceiverListener {
        fun onTrackChanged(track: Track, isPlaying: Boolean, currentDuration: Long)
        fun onPlayPauseClicked()
        fun onPrevClicked()
        fun onNextClicked()
    }
}
