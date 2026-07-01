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

    private val emergencyPhrase = "Emergency. I need help."

    private val ttsLoopRunnable = object : Runnable {
        override fun run() {
            if (!running) return
            speak(emergencyPhrase)
            mainHandler.postDelayed(this, TTS_REPEAT_MS)
        }
    }

    fun start(sequenceLeft: Int, sequenceRight: Int) {
        if (running) return
        running = true
        startAlarmSound()
        mainHandler.post(ttsLoopRunnable)
        EmergencyNotificationService.notifyCaregiverPlaceholder(
            sequenceLeft = sequenceLeft,
            sequenceRight = sequenceRight
        )
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

    private fun startAlarmSound() {
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: return

        mediaPlayer = MediaPlayer().apply {
            setDataSource(context, alarmUri)
            isLooping = true
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setVolume(1.0f, 1.0f)
            prepare()
            start()
        }
    }

    companion object {
        private const val TTS_REPEAT_MS = 3500L
    }
}
