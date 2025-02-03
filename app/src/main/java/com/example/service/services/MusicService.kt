package com.example.service.services

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.PendingIntent
import android.app.Service
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.service.CHANNEL_ID
import com.example.service.R
import com.example.service.composables.formatDuration
import com.example.service.models.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val PREV = "prev"
const val PLAY_PAUSE = "play_pause"
const val NEXT = "next"
const val SEEK = "seek"
const val STOP = "stop"
const val UPDATE_TRACK = "update_track"

class MusicService : Service() {

    private var mediaPlayer = MediaPlayer()
    private lateinit var currentTrack: Track
    private var maxDuration = 0L
    private var currentDuration = 0L
    private var progress = 0

    private var musicList = mutableListOf<Track>()
    private var isPlaying = false

    private val scope = CoroutineScope(Dispatchers.Main)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        progress = intent?.getIntExtra("progress", 0) ?: 0
        musicList = intent?.getParcelableArrayListExtra("tracks") ?: musicList
        val track = intent?.getParcelableExtra("track") ?: currentTrack

        track.let {
            currentTrack = it
        }

        intent?.let {
            when (intent.action) {
                PREV -> {
                    prev()
                }

                NEXT -> {
                    next()
                }

                PLAY_PAUSE -> {
                    playPause()
                }

                SEEK -> {
                    seek()
                }

                STOP -> {
                    stop()
                }

                else -> {
                    play()
                }
            }
        }
        return START_STICKY
    }

    private fun prev() {
        job?.cancel()
        mediaPlayer.reset()
        mediaPlayer = MediaPlayer()
        val index = musicList.indexOf(currentTrack)
        val prevIndex = if (index == 0) musicList.size.minus(1) else index.minus(1)
        val prevItem = musicList[prevIndex]
        currentTrack = prevItem
        mediaPlayer.setDataSource(this, getUri(currentTrack.id))
        mediaPlayer.prepareAsync()
        mediaPlayer.setOnPreparedListener {
            mediaPlayer.start()
            sendNotification(currentTrack)
            updateDurations()
        }
    }

    private fun next() {
        job?.cancel()
        mediaPlayer.reset()
        mediaPlayer = MediaPlayer()
        val index = musicList.indexOf(currentTrack)
        val nextIndex = index.plus(1).mod(musicList.size)
        val nextItem = musicList[nextIndex]
        currentTrack = nextItem
        mediaPlayer.setDataSource(this, getUri(nextItem.id))
        mediaPlayer.prepareAsync()
        mediaPlayer.setOnPreparedListener {
            mediaPlayer.start()
            sendNotification(currentTrack)
            updateDurations()
        }
    }

    private fun playPause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        } else {
            mediaPlayer.start()
        }
        sendNotification(currentTrack)
    }

    private fun seek() {
        mediaPlayer.seekTo(progress)
    }

    private fun play() {
        mediaPlayer.reset()
        mediaPlayer = MediaPlayer()
        mediaPlayer.setDataSource(this, getUri(currentTrack.id))
        mediaPlayer.prepareAsync()
        mediaPlayer.setOnPreparedListener {
            mediaPlayer.start()
            sendNotification(currentTrack)
            updateDurations()
        }
    }

    private fun stop() {
        job?.cancel()
        mediaPlayer.stop()
        mediaPlayer.release()
        stopForeground(true)
        stopSelf()
    }

    private fun updateDurations() {
        job = scope.launch {
            if (mediaPlayer.isPlaying.not()) return@launch

            maxDuration = mediaPlayer.duration.toLong()

            while (true) {
                currentDuration = mediaPlayer.currentPosition.toLong()
                sendTrackUpdate()
                sendNotification(currentTrack)
                delay(1000)

                mediaPlayer.setOnCompletionListener {
                    next()
                }
            }
        }
    }

    private fun sendTrackUpdate() {
        val intent = Intent(UPDATE_TRACK).apply {
            putExtra("track", currentTrack)
            putExtra("isPlaying", mediaPlayer.isPlaying)
            putExtra("currentDuration", currentDuration)
        }
        sendBroadcast(intent)
    }

    private fun getUri(id: Long) =
        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

    private fun sendNotification(track: Track) {

        val session = MediaSessionCompat(this, "music")

        isPlaying = mediaPlayer.isPlaying
        val style = androidx.media.app.NotificationCompat.MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)
            .setMediaSession(session.sessionToken)

        val remainingTime =
            formatDuration((mediaPlayer.duration - mediaPlayer.currentPosition).toLong())

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setStyle(style)
            .setContentTitle(track.name)
            .setContentText("- $remainingTime")
            .addAction(R.drawable.ic_pre, "prev", createPrevPendingIntent())
            .addAction(
                if (mediaPlayer.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                "play_pause",
                createPlayPausePendingIntent()
            )
            .addAction(R.drawable.ic_next, "next", createNextPendingIntent())
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    resources,
                    R.drawable.banner
                )
            )
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startForeground(1, notification)
            }
        } else {
            startForeground(1, notification)
        }
    }


    private fun createPrevPendingIntent(): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply {
            action = PREV
        }
        sendBroadcast(Intent(PREV))
        return PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createPlayPausePendingIntent(): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply {
            action = PLAY_PAUSE
        }
        sendBroadcast(Intent(PLAY_PAUSE))
        return PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createNextPendingIntent(): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply {
            action = NEXT
        }
        sendBroadcast(Intent(NEXT))
        return PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
