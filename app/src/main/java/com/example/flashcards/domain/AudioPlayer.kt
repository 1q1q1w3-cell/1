package com.example.flashcards.domain

import android.media.MediaPlayer
import android.media.PlaybackParams
import com.example.flashcards.data.MediaLocator

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

    fun stop() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
