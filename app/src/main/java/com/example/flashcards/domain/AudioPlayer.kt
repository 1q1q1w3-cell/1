package com.example.flashcards.domain

import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Handler
import android.os.Looper
import com.example.flashcards.data.MediaLocator

class AudioPlayer(private val mediaLocator: MediaLocator) {
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var stopRunnable: Runnable? = null

    fun play(assetPath: String, speed: Float, startRatio: Float? = null, endRatio: Float? = null) {
        stop()
        val file = mediaLocator.copyAssetToCache(assetPath)
        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                playbackParams = PlaybackParams().setSpeed(speed)
            }
            val durationMs = duration.takeIf { it > 0 } ?: 0
            val safeStart = (((startRatio ?: 0f).coerceIn(0f, 1f)) * durationMs).toInt()
            val safeEnd = (((endRatio ?: 1f).coerceIn(0f, 1f)) * durationMs).toInt().coerceAtLeast(safeStart + 1)
            if (safeStart > 0) seekTo(safeStart)
            start()

            if (durationMs > 0 && safeEnd > safeStart) {
                val delayMs = ((safeEnd - safeStart) / speed.coerceAtLeast(0.1f)).toLong().coerceAtLeast(50L)
                stopRunnable = Runnable { stop() }
                handler.postDelayed(stopRunnable!!, delayMs)
            }
        }
    }

    fun stop() {
        stopRunnable?.let { handler.removeCallbacks(it) }
        stopRunnable = null
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
