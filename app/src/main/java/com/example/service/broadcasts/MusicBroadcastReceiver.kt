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

    interface MusicReceiverListener {
        fun onTrackChanged(track: Track)
        fun onPlayPauseClicked()
        fun onPrevClicked()
        fun onNextClicked()
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            PREV -> listener.onPrevClicked()
            NEXT -> listener.onNextClicked()
            PLAY_PAUSE -> listener.onPlayPauseClicked()
            UPDATE_TRACK -> {
                val track = intent.getParcelableExtra<Track>("track")
                if (track != null) listener.onTrackChanged(track)
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
}
