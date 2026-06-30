package com.example.macoswallpaper

import android.animation.ValueAnimator
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class VideoWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine {
        return VideoEngine()
    }

    inner class VideoEngine : Engine() {
        private var exoPlayer: ExoPlayer? = null
        private var speedAnimator: ValueAnimator? = null
        private var keyguardManager: KeyguardManager? = null

        private val screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        exoPlayer?.pause()
                        speedAnimator?.cancel()
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        if (keyguardManager?.isKeyguardLocked == true) {
                            playNormalSpeed()
                        }
                    }
                    Intent.ACTION_USER_PRESENT -> {
                        startSlowDownAnimation()
                    }
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            registerReceiver(screenReceiver, filter)
        }

        override fun onDestroy() {
            super.onDestroy()
            unregisterReceiver(screenReceiver)
            speedAnimator?.cancel()
            exoPlayer?.release()
            exoPlayer = null
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                if (keyguardManager?.isKeyguardLocked == true) {
                    playNormalSpeed()
                } else {
                    // Already unlocked, keep it paused
                    exoPlayer?.pause()
                }
            } else {
                exoPlayer?.pause()
                speedAnimator?.cancel()
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            initializePlayer()
            exoPlayer?.setVideoSurfaceHolder(holder)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            exoPlayer?.pause()
            exoPlayer?.clearVideoSurfaceHolder(holder)
        }

        private fun initializePlayer() {
            if (exoPlayer == null) {
                exoPlayer = ExoPlayer.Builder(this@VideoWallpaperService).build().apply {
                    repeatMode = Player.REPEAT_MODE_ALL
                    volume = 0f // Mute wallpaper
                }
            }

            val sharedPrefs = getSharedPreferences("WallpaperPrefs", Context.MODE_PRIVATE)
            val savedUriString = sharedPrefs.getString("video_uri", null)
            
            if (savedUriString != null) {
                try {
                    val mediaItem = MediaItem.fromUri(Uri.parse(savedUriString))
                    exoPlayer?.setMediaItem(mediaItem)
                    exoPlayer?.prepare()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        private fun playNormalSpeed() {
            speedAnimator?.cancel()
            exoPlayer?.playbackParameters = PlaybackParameters(1.0f)
            exoPlayer?.play()
        }

        private fun startSlowDownAnimation() {
            speedAnimator?.cancel()
            val currentSpeed = exoPlayer?.playbackParameters?.speed ?: 1.0f
            if (currentSpeed <= 0.1f) {
                exoPlayer?.pause()
                return
            }

            speedAnimator = ValueAnimator.ofFloat(currentSpeed, 0.1f).apply {
                duration = 2500L // 2.5 seconds slow down
                addUpdateListener { animator ->
                    val speed = animator.animatedValue as Float
                    if (speed <= 0.1f) {
                        exoPlayer?.pause()
                    } else {
                        exoPlayer?.playbackParameters = PlaybackParameters(speed)
                    }
                }
                start()
            }
        }
    }
}
