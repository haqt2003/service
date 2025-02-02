package com.example.service.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.service.R
import com.example.service.broadcasts.MusicBroadcastReceiver
import com.example.service.composables.formatDuration
import com.example.service.databinding.ActivityPlayBinding
import com.example.service.models.Track
import com.example.service.services.MusicService
import com.example.service.services.NEXT
import com.example.service.services.PLAY_PAUSE
import com.example.service.services.PREV
import com.example.service.services.STOP

class PlayActivity : AppCompatActivity(), MusicBroadcastReceiver.MusicReceiverListener {
    private val binding: ActivityPlayBinding by lazy {
        ActivityPlayBinding.inflate(layoutInflater)
    }

    private lateinit var musicReceiver: MusicBroadcastReceiver
    private var trackList = listOf<Track>()
    private lateinit var track: Track
    private var isPlaying = false

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cl_play)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        trackList = intent.getParcelableArrayListExtra("tracks") ?: emptyList()
        track = intent.getParcelableExtra("track") ?: throw IllegalArgumentException("Track is null")

        musicReceiver = MusicBroadcastReceiver(this)
        musicReceiver.register(this)

        startService(Intent(this, MusicService::class.java).apply {
            action = "PLAY"
            putParcelableArrayListExtra("tracks", ArrayList(trackList))
            putExtra("track", track)
        })

        binding.tvDuration.text = formatDuration(track.duration)
        binding.tvTitle.text = track.name

        binding.ivPlay.setOnClickListener {
            startService(Intent(this, MusicService::class.java).apply {
                action = PLAY_PAUSE
            })
        }

        binding.ivPause.setOnClickListener {
            startService(Intent(this, MusicService::class.java).apply {
                action = PLAY_PAUSE
            })
        }

        binding.ivNext.setOnClickListener {
            startService(Intent(this, MusicService::class.java).apply {
                action = NEXT
            })
        }

        binding.ivPre.setOnClickListener {
            startService(Intent(this, MusicService::class.java).apply {
                action = PREV
            })
        }
    }

    override fun onTrackChanged(track: Track) {
        binding.tvTitle.text = track.name
        binding.tvDuration.text = formatDuration(track.duration)
    }

    override fun onPlayPauseClicked() {
        Log.d("OPP", "swap")
        isPlaying = !isPlaying
        if (isPlaying) {
            binding.ivPlay.visibility = android.view.View.GONE
            binding.ivPause.visibility = android.view.View.VISIBLE
        } else {
            binding.ivPlay.visibility = android.view.View.VISIBLE
            binding.ivPause.visibility = android.view.View.GONE
        }
    }

    override fun onPrevClicked() {
        Log.d("PlayActivity", "onPrevClicked: $isPlaying")
    }

    override fun onNextClicked() {
        Log.d("PlayActivity", "onNextClicked: $isPlaying")
    }

    override fun onDestroy() {
        super.onDestroy()
        musicReceiver.unregister(this)
        startService(Intent(this, MusicService::class.java).apply {
            action = STOP
        })
    }
}
