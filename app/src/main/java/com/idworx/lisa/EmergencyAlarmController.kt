package com.idworx.lisa

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper

/**
 * Local emergency alarm: loud looping alarm sound + repeating TTS phrase.
 */
class EmergencyAlarmController(
    private val context: Context,
    private val speak: (String) -> Unit,
    private val stopSpeech: () -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var running = false
    private var alarmVolume: Float = 1.0f
    private var emergencySpeechPhrase = "Emergency. I need help."

    private val ttsLoopRunnable = object : Runnable {
        override fun run() {
            if (!running) return
            speak(emergencySpeechPhrase)
            mainHandler.postDelayed(this, TTS_REPEAT_MS)
        }
    }

    fun start(sequenceLeft: Int, sequenceRight: Int, alarmVolume: Float = 1.0f, speechPhrase: String? = null) {
        if (running) return
        running = true
        if (speechPhrase != null) emergencySpeechPhrase = speechPhrase
        startAlarmSound(alarmVolume.coerceIn(0.5f, 1f))
        mainHandler.post(ttsLoopRunnable)
    }

    fun stop() {
        running = false
        mainHandler.removeCallbacks(ttsLoopRunnable)
        stopSpeech()
        mediaPlayer?.let { player ->
            if (player.isPlaying) player.stop()
            player.release()
        }
        mediaPlayer = null
    }

    fun isRunning(): Boolean = running

    fun setAlarmVolume(volume: Float) {
        alarmVolume = volume.coerceIn(0.5f, 1f)
        mediaPlayer?.setVolume(alarmVolume, alarmVolume)
    }

    private fun startAlarmSound(volume: Float) {
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: return

        this.alarmVolume = volume
        mediaPlayer = MediaPlayer().apply {
            setDataSource(context, alarmUri)
            isLooping = true
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setVolume(volume, volume)
            prepare()
            start()
        }
    }

    companion object {
        private const val TTS_REPEAT_MS = 3500L
    }
}
