package com.example.service.activities

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
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
import com.example.service.services.REPEAT
import com.example.service.services.SEEK
import com.example.service.services.SHUFFLE
import com.example.service.services.STOP

class PlayActivity : AppCompatActivity(), MusicBroadcastReceiver.MusicReceiverListener {
    private val binding: ActivityPlayBinding by lazy {
        ActivityPlayBinding.inflate(layoutInflater)
    }

    private lateinit var musicReceiver: MusicBroadcastReceiver
    private var trackList = listOf<Track>()
    private lateinit var track: Track

    private lateinit var rotateAnimator: ObjectAnimator
    private var isRotating = false
    private var isPlaying = false
    private var isShuffling = false
    private  var isRepeatOne = false

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

        rotateAnimator = ObjectAnimator.ofFloat(binding.ivThumbnail, "rotation", 0f, 360f).apply {
            duration = 12000
            interpolator = LinearInterpolator()
            repeatCount = ObjectAnimator.INFINITE
        }

        trackList = intent.getParcelableArrayListExtra("tracks") ?: emptyList()
        track =
            intent.getParcelableExtra("track") ?: throw IllegalArgumentException("Track is null")

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

        binding.ivShuffle.setOnClickListener {
            isShuffling = !isShuffling
            binding.ivShuffle.setImageResource(if (isShuffling) R.drawable.ic_shuffle_active else R.drawable.ic_shuffle)
            startService(Intent(this, MusicService::class.java).apply {
                action = SHUFFLE
                putParcelableArrayListExtra("tracks", ArrayList(trackList))
            })
        }

        binding.ivRepeat.setOnClickListener {
            isRepeatOne = !isRepeatOne
            binding.ivRepeat.setImageResource(if (isRepeatOne) R.drawable.ic_repeat_one else R.drawable.ic_repeat)
            startService(Intent(this, MusicService::class.java).apply {
                action = REPEAT
            })
        }

        binding.ivClose.setOnClickListener {
            finish()
        }

        binding.sbTimeline.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.tvProgress.text = formatDuration(progress.toLong())
                    startService(Intent(this@PlayActivity, MusicService::class.java).apply {
                        action = SEEK
                        putExtra("progress", seekBar?.progress)
                    })
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
    }

    override fun onTrackChanged(track: Track, isPlaying: Boolean, currentDuration: Long) {
        binding.tvTitle.text = track.name
        binding.tvDuration.text = formatDuration(track.duration)
        binding.tvProgress.text = formatDuration(currentDuration)
        binding.sbTimeline.progress = currentDuration.toInt()
        binding.sbTimeline.max = track.duration.toInt()
        if (isPlaying) {
            binding.ivPlay.visibility = android.view.View.GONE
            binding.ivPause.visibility = android.view.View.VISIBLE
            if (!isRotating) {
                rotateAnimator.start()
                isRotating = true
            } else {
                rotateAnimator.resume()
            }
        } else {
            binding.ivPlay.visibility = android.view.View.VISIBLE
            binding.ivPause.visibility = android.view.View.GONE
            rotateAnimator.pause()
        }
    }

    override fun onPlayPauseClicked() {
        Log.d("OPP", "swap")
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
