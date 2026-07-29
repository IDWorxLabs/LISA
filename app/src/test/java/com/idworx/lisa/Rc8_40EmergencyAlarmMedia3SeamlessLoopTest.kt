package com.idworx.lisa

import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * RC8.40 — emergency tone uses Media3 ExoPlayer + REPEAT_MODE_ONE (not MediaPlayer looping).
 */
class Rc8_40EmergencyAlarmMedia3SeamlessLoopTest {

    private fun readController(): String {
        val path = "app/src/main/java/com/idworx/lisa/EmergencyAlarmController.kt"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source: $path")
    }

    private fun readMain(): String {
        val path = "app/src/main/java/com/idworx/lisa/MainActivity.kt"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source: $path")
    }

    private fun readAppGradle(): String {
        val path = "app/build.gradle.kts"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: File(path).takeIf { it.isFile }?.readText()
            ?: error("Missing: $path")
    }

    @Test
    fun dependencyAddsMedia3ExoPlayerOnly() {
        val gradle = readAppGradle()
        assertTrue(gradle.contains("androidx.media3:media3-exoplayer:1.4.1"))
        assertFalse(gradle.contains("media3-ui"))
        assertFalse(gradle.contains("media3-session"))
    }

    @Test
    fun emergencyAlarmUsesExoPlayerNotMediaPlayer() {
        val src = readController()
        assertTrue(src.contains("ExoPlayer"))
        assertTrue(src.contains("androidx.media3.exoplayer.ExoPlayer"))
        assertFalse(src.contains("android.media.MediaPlayer"))
        assertFalse(src.contains("MediaPlayer()"))
        assertFalse(src.contains("isLooping"))
    }

    @Test
    fun repeatModeOneConfiguredOncePerSession() {
        val src = readController()
        assertTrue(src.contains("Player.REPEAT_MODE_ONE"))
        assertTrue(src.contains("player.repeatMode = Player.REPEAT_MODE_ONE"))
        assertTrue(src.contains("player.prepare()"))
        assertTrue(src.contains("player.play()"))
        assertFalse(src.contains("setOnCompletionListener"))
        assertFalse(src.contains("OnCompletionListener"))
        assertFalse(src.contains("seekTo(0)"))
        assertFalse(src.contains("seekTo(0L)"))
    }

    @Test
    fun usesRawLisaEmergencyToneResource() {
        val src = readController()
        assertTrue(
            src.contains("R.raw.lisa_emergency_tone") ||
                src.contains("EMERGENCY_TONE_RAW_RES")
        )
        assertTrue(src.contains("android.resource://"))
        val wav = listOf(
            File("app/src/main/res/raw/lisa_emergency_tone.wav"),
            File("src/main/res/raw/lisa_emergency_tone.wav")
        ).firstOrNull { it.isFile }
            ?: error("Missing lisa_emergency_tone.wav under app/src/main/res/raw")
        assertTrue(wav.length() > 1_000_000L)
    }

    @Test
    fun runningGuardPreventsDuplicatePlayersAndStopReleases() {
        val src = readController()
        assertTrue(src.contains("if (running) return"))
        assertTrue(src.contains("releasePlayer()"))
        assertTrue(src.contains("player.release()"))
        assertTrue(src.contains("exoPlayer = null"))
        // Single field — one player ownership
        assertTrue(src.contains("private var exoPlayer: ExoPlayer? = null"))
        assertFalse(src.contains("private var mediaPlayer"))
    }

    @Test
    fun alarmAttributesAndManualAudioFocusPreserved() {
        val src = readController()
        assertTrue(src.contains("C.USAGE_ALARM"))
        assertTrue(src.contains("C.AUDIO_CONTENT_TYPE_SONIFICATION"))
        assertTrue(src.contains("handleAudioFocus= */ false"))
        assertTrue(src.contains("requestAlarmAudioFocus"))
        assertTrue(src.contains("abandonAlarmAudioFocus"))
    }

    @Test
    fun ttsLoopIntactAndDoesNotTouchExoPlayer() {
        val src = readController()
        assertTrue(src.contains("ttsLoopRunnable"))
        assertTrue(src.contains("TTS_REPEAT_MS = 3500L"))
        assertTrue(src.contains("speak(emergencySpeechPhrase)"))
        val runnable = src.substringAfter("private val ttsLoopRunnable")
            .substringBefore("fun start(")
        assertFalse(runnable.contains("exoPlayer"))
        assertFalse(runnable.contains("ExoPlayer"))
        assertFalse(runnable.contains("releasePlayer"))
        assertFalse(runnable.contains("prepare()"))

        val main = readMain()
        assertTrue(main.contains("speakEmergencyPhrase"))
        assertTrue(main.contains("STREAM_ALARM"))
    }

    @Test
    fun failureHandlingDoesNotCrashAndDoesNotFallbackToMediaPlayer() {
        val src = readController()
        assertTrue(src.contains("Emergency ExoPlayer failed to initialise or prepare"))
        assertTrue(src.contains("Log.e"))
        assertFalse(src.contains("MediaPlayer()"))
        assertFalse(src.contains("RingtoneManager"))
    }

    @Test
    fun emergencySequencesAndStartWiringUnchanged() {
        val main = readMain()
        assertTrue(main.contains("private fun startEmergencyMode()"))
        assertTrue(main.contains("emergencyAlarmController.start("))
        assertTrue(main.contains("emergencyAlarmController.stop()"))
        assertTrue(main.contains("MAX_EMERGENCY_VOLUME"))
        assertTrue(main.contains("emergencySpeechPhrase"))
    }
}
