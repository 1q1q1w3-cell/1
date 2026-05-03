package com.example.flashcards.domain

import android.media.MediaPlayer
import android.media.PlaybackParams
import com.example.flashcards.data.MediaLocator
import kotlin.concurrent.thread

class AudioPlayer(private val mediaLocator: MediaLocator) {
    private var mediaPlayer: MediaPlayer? = null

    fun play(assetPath: String, speed: Float) {
        stop()
        val file = mediaLocator.copyAssetToCache(assetPath)
        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                playbackParams = PlaybackParams().setSpeed(speed)
            }
            start()
        }
    }

    fun getDurationMs(assetPath: String): Int {
        val file = mediaLocator.copyAssetToCache(assetPath)
        val probe = MediaPlayer()
        return try {
            probe.setDataSource(file.absolutePath)
            probe.prepare()
            probe.duration
        } finally {
            probe.release()
        }
    }

    fun playSegment(assetPath: String, speed: Float, startMs: Int, endMs: Int) {
        stop()
        val file = mediaLocator.copyAssetToCache(assetPath)
        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                playbackParams = PlaybackParams().setSpeed(speed)
            }
            seekTo(startMs)
            start()
            val safeEnd = endMs.coerceAtLeast(startMs + 50)
            thread {
                val safeSpeed = speed.coerceAtLeast(0.1f)
                val waitMs = ((safeEnd - startMs) / safeSpeed).toLong()
                Thread.sleep(waitMs)
                stop()
            }
        }
    }

    fun stop() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
