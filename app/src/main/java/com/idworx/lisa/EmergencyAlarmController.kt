package com.idworx.lisa

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Local emergency alarm: loud looping alarm sound + repeating TTS phrase.
 *
 * RC8.40 — emergency tone uses one Media3 ExoPlayer with [Player.REPEAT_MODE_ONE] for the
 * whole emergency session (seamless loop vs MediaPlayer EOS seek). TTS remains independent
 * and must never recreate, pause, or restart the player.
 */
class EmergencyAlarmController(
    private val context: Context,
    private val speak: (String) -> Unit,
    private val stopSpeech: () -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var exoPlayer: ExoPlayer? = null
    private var running = false
    private var alarmVolume: Float = 1.0f
    private var emergencySpeechPhrase = "Emergency. I need help."
    private var audioFocusRequest: AudioFocusRequest? = null

    private val platformAlarmAudioAttributes: android.media.AudioAttributes =
        android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

    private val media3AlarmAudioAttributes: AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(C.USAGE_ALARM)
            .setContentType(C.AUDIO_CONTENT_TYPE_SONIFICATION)
            .build()

    /**
     * Keep the alarm audible if TTS briefly takes focus. Do not pause, duck-handle, or
     * recreate the player — emergency tone must stay continuous underneath speech.
     */
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { /* no-op */ }

    private val playerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Emergency ExoPlayer error: ${error.errorCodeName}", error)
        }
    }

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
        try {
            stopSpeech()
        } catch (t: Throwable) {
            Log.e(TAG, "Emergency stopSpeech failed", t)
        }
        releasePlayer()
        abandonAlarmAudioFocus()
    }

    fun isRunning(): Boolean = running

    fun setAlarmVolume(volume: Float) {
        // RC8.14 / RC8.37 — once the emergency alarm is active, keep the confirmed max level.
        if (running) return
        alarmVolume = volume.coerceIn(0.5f, 1f)
        exoPlayer?.volume = alarmVolume
    }

    private fun startAlarmSound(volume: Float) {
        this.alarmVolume = volume
        requestAlarmAudioFocus()
        // RC8.40 — single ExoPlayer for the whole emergency; prepare once, REPEAT_MODE_ONE.
        try {
            val player = ExoPlayer.Builder(context).build()
            player.addListener(playerListener)
            player.setAudioAttributes(
                media3AlarmAudioAttributes,
                /* handleAudioFocus= */ false // RC8.37 focus ownership preserved manually
            )
            player.volume = volume
            player.repeatMode = Player.REPEAT_MODE_ONE
            player.setMediaItem(
                MediaItem.fromUri(
                    Uri.parse("android.resource://${context.packageName}/$EMERGENCY_TONE_RAW_RES")
                )
            )
            player.prepare()
            player.play()
            exoPlayer = player
        } catch (t: Throwable) {
            Log.e(TAG, "Emergency ExoPlayer failed to initialise or prepare", t)
            releasePlayer()
            // Emergency UI / TTS remain active; tone may be silent after failure.
        }
    }

    private fun releasePlayer() {
        val player = exoPlayer ?: return
        exoPlayer = null
        try {
            player.removeListener(playerListener)
        } catch (_: Throwable) {
            // ignore
        }
        try {
            player.stop()
        } catch (_: Throwable) {
            // ignore — may already be idle/released
        }
        try {
            player.release()
        } catch (t: Throwable) {
            Log.e(TAG, "Emergency ExoPlayer release failed", t)
        }
    }

    private fun requestAlarmAudioFocus() {
        abandonAlarmAudioFocus()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(platformAlarmAudioAttributes)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .setAcceptsDelayedFocusGain(false)
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    private fun abandonAlarmAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    companion object {
        private const val TAG: String = "EmergencyAlarm"
        private const val TTS_REPEAT_MS = 3500L

        /** Production raw resource for the emergency tone (v2 bytes under this name). */
        val EMERGENCY_TONE_RAW_RES: Int = R.raw.lisa_emergency_tone
    }
}
