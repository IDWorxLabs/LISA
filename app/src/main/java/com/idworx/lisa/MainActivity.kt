package com.idworx.lisa

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.idworx.lisa.ui.theme.LISATheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    companion object {
        private const val WINK_COOLDOWN_MS = 900L
        private const val SEQUENCE_IDLE_TIMEOUT_MS = 2500L
        private const val SEQUENCE_MAX_WINDOW_MS = 4500L
        private const val EYE_PROB_JUMP_THRESHOLD = 0.28f
        private const val COUNTDOWN_SECONDS = 3
        private const val COUNTDOWN_TICK_MS = 1000L
    }

    private data class SensitivitySettings(
        val closedEyeThreshold: Float,
        val openEyeThreshold: Float,
        val requiredWinkFrames: Int
    )

    private val sensitivityPresets = mapOf(
        1 to SensitivitySettings(0.15f, 0.85f, 5),
        2 to SensitivitySettings(0.20f, 0.80f, 4),
        3 to SensitivitySettings(0.25f, 0.75f, 3),
        4 to SensitivitySettings(0.32f, 0.68f, 2),
        5 to SensitivitySettings(0.40f, 0.60f, 1)
    )

    private var closedEyeThreshold = sensitivityPresets.getValue(DEFAULT_SENSITIVITY_LEVEL).closedEyeThreshold
    private var openEyeThreshold = sensitivityPresets.getValue(DEFAULT_SENSITIVITY_LEVEL).openEyeThreshold
    private var requiredWinkFrames = sensitivityPresets.getValue(DEFAULT_SENSITIVITY_LEVEL).requiredWinkFrames

    private var tts: TextToSpeech? = null
    private lateinit var cameraExecutor: ExecutorService
    private val mainHandler = Handler(Looper.getMainLooper())
    private var emergencyActive = false

    private val emergencyAlarmController by lazy {
        EmergencyAlarmController(
            context = this,
            speak = { text -> speak(text) },
            stopSpeech = { tts?.stop() }
        )
    }

    // --- Wink detection state ---
    private var leftWinks = 0
    private var rightWinks = 0
    private var lastWinkTimeMs = 0L
    private var sequenceStartMs = 0L

    private var leftWinkFrameStreak = 0
    private var rightWinkFrameStreak = 0
    private var leftWinkGestureCounted = false
    private var rightWinkGestureCounted = false
    private var lastLeftWinkCountedMs = 0L
    private var lastRightWinkCountedMs = 0L

    private var prevLeftProb: Float? = null
    private var prevRightProb: Float? = null
    private var wasLeftWinkCandidate = false
    private var wasRightWinkCandidate = false

    private var pendingPhrase: String? = null
    private var countdownActive = false
    private var countdownLeftHandled = false
    private var countdownRightHandled = false
    private var savedSequenceLeft = 0
    private var savedSequenceRight = 0

    // Face detector (FAST + eye open probabilities)
    private val detector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // needed for eye open prob
            .enableTracking()
            .build()
        FaceDetection.getClient(options)
    }

    // UI states (Compose)
    private val uiCommunicationState = mutableStateOf<LisaCommunicationState>(LisaCommunicationState.WaitingForFace)
    private val uiFacePresent = mutableStateOf(false)
    private val uiEmergencyActive = mutableStateOf(false)
    private val uiLastSpoken = mutableStateOf("")
    private val uiDiagLeftEye = mutableStateOf("--")
    private val uiDiagRightEye = mutableStateOf("--")
    private val uiDiagLeftCount = mutableStateOf(0)
    private val uiDiagRightCount = mutableStateOf(0)
    private val uiSensitivityLevel = mutableStateOf(DEFAULT_SENSITIVITY_LEVEL)
    private val uiPendingPhrase = mutableStateOf<String?>(null)
    private val uiCountdown = mutableStateOf<Int?>(null)
    private val uiDeveloperMode = mutableStateOf(false)
    private val uiShowSettings = mutableStateOf(false)
    private val uiDevLeftStreak = mutableStateOf(0)
    private val uiDevRightStreak = mutableStateOf(0)

    // training mode toggle
    private val uiShowTraining = mutableStateOf(false)

    // phrase mappings
    private val mappingsState = mutableStateListOf<WinkMapping>()

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

// TTS
        tts = TextToSpeech(this, this)

// camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()

// load mappings (defaults + saved)
        mappingsState.clear()
        mappingsState.addAll(defaultLanguageMappings())
        mappingsState.addAll(loadCustomMappings(this))

        applySensitivityLevel(loadSensitivityLevel(this))
        uiDeveloperMode.value = loadDeveloperMode(this)

// camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }

        setContent {
            LISATheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                    val userDisplay = uiCommunicationState.value.toUserDisplay(
                        pendingPhrase = uiPendingPhrase.value,
                        countdown = uiCountdown.value,
                        leftWinkDots = uiDiagLeftCount.value,
                        rightWinkDots = uiDiagRightCount.value
                    )
                    LisaRootUI(
                        userDisplay = userDisplay,
                        emergencyActive = uiEmergencyActive.value,
                        developerMode = uiDeveloperMode.value,
                        showSettings = uiShowSettings.value,
                        showTraining = uiShowTraining.value,
                        countdownActive = countdownActive,
                        sensitivityLevel = uiSensitivityLevel.value,
                        developerInfo = DeveloperPanelInfo(
                            leftEye = uiDiagLeftEye.value,
                            rightEye = uiDiagRightEye.value,
                            leftCount = uiDiagLeftCount.value,
                            rightCount = uiDiagRightCount.value,
                            leftFrameStreak = uiDevLeftStreak.value,
                            rightFrameStreak = uiDevRightStreak.value,
                            closedThreshold = closedEyeThreshold,
                            openThreshold = openEyeThreshold,
                            requiredFrames = requiredWinkFrames,
                            sensitivityLevel = uiSensitivityLevel.value,
                            detectionState = uiCommunicationState.value.displayText
                        ),
                        mappings = mappingsState,
                        onToggleSettings = { uiShowSettings.value = !uiShowSettings.value },
                        onDeveloperModeChange = { enabled ->
                            uiDeveloperMode.value = enabled
                            saveDeveloperMode(this@MainActivity, enabled)
                        },
                        onSensitivityDecrease = { changeSensitivity(-1) },
                        onSensitivityIncrease = { changeSensitivity(1) },
                        onToggleTraining = { uiShowTraining.value = !uiShowTraining.value },
                        onRepeat = {
                            val phrase = uiLastSpoken.value
                            if (phrase.isNotBlank()) speak(phrase)
                        },
                        onReset = { performReset() },
                        onEditCountdown = { editCountdownAndRetry() },
                        onAddMapping = { left, right, phrase ->
                            val cleaned = phrase.trim()
                            if (cleaned.isBlank()) return@LisaRootUI
                            if (!isSequenceEligibleForSpeech(left, right)) return@LisaRootUI
                            val newMap = WinkMapping(left, right, cleaned, isCustom = true)
                            mappingsState.add(newMap)
                            saveCustomMappings(this@MainActivity, mappingsState.filter { it.isCustom })
                            Toast.makeText(this@MainActivity, "Saved: L$left R$right → $cleaned", Toast.LENGTH_SHORT).show()
                        },
                        cameraView = {
                            CameraPreview(
                                onFrame = { imageProxy -> processFrame(imageProxy) }
                            )
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacksAndMessages(null)
        emergencyAlarmController.stop()
        cameraExecutor.shutdown()
        tts?.stop()
        tts?.shutdown()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) {
                    if (utteranceId == "LISA_SPEAK") {
                        runOnUiThread { onSpeechFinished() }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (utteranceId == "LISA_SPEAK") {
                        runOnUiThread { onSpeechFinished() }
                    }
                }
            })
        }
    }

    private fun speak(text: String) {
        val params = Bundle()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "LISA_SPEAK")
    }

    private fun onSpeechFinished() {
        if (emergencyActive) return
        setCommunicationState(LisaCommunicationState.MessageDelivered)
        mainHandler.postDelayed({ updateReadyOrWaitingState() }, 1800L)
    }

    private fun setCommunicationState(state: LisaCommunicationState) {
        uiCommunicationState.value = state
    }

    private fun updateReadyOrWaitingState() {
        if (emergencyActive || countdownActive) return
        setCommunicationState(
            if (uiFacePresent.value) LisaCommunicationState.Listening
            else LisaCommunicationState.WaitingForFace
        )
    }

    /**
     * Front-camera mirror correction.
     *
     * ML Kit reports leftEye/rightEye from the camera sensor's perspective. The front-camera
     * preview is mirrored for the user, so the user's physical left eye corresponds to the
     * sensor's right-eye probability and vice versa. Swap here so wink counts match the
     * user's actual left and right winks.
     */
    private data class UserEyeProbabilities(val userLeft: Float, val userRight: Float)

    private fun userEyeProbabilities(face: Face): UserEyeProbabilities? {
        val sensorLeft = face.leftEyeOpenProbability ?: return null
        val sensorRight = face.rightEyeOpenProbability ?: return null
        return UserEyeProbabilities(userLeft = sensorRight, userRight = sensorLeft)
    }

    private fun clearCountdown() {
        countdownActive = false
        pendingPhrase = null
        uiPendingPhrase.value = null
        uiCountdown.value = null
        countdownLeftHandled = false
        countdownRightHandled = false
        mainHandler.removeCallbacks(countdownTickRunnable)
    }

    private val countdownTickRunnable = object : Runnable {
        override fun run() {
            if (!countdownActive) return
            val current = uiCountdown.value ?: return
            if (current <= 1) {
                speakPendingPhraseAndFinish()
            } else {
                uiCountdown.value = current - 1
                mainHandler.postDelayed(this, COUNTDOWN_TICK_MS)
            }
        }
    }

    private fun startCountdown(phrase: String, sequenceLeft: Int, sequenceRight: Int) {
        savedSequenceLeft = sequenceLeft
        savedSequenceRight = sequenceRight
        resetSequence()
        pendingPhrase = phrase
        countdownActive = true
        countdownLeftHandled = false
        countdownRightHandled = false
        uiPendingPhrase.value = phrase
        uiLastSpoken.value = phrase
        uiCountdown.value = COUNTDOWN_SECONDS
        setCommunicationState(LisaCommunicationState.CountdownConfirm(phrase))
        mainHandler.removeCallbacks(countdownTickRunnable)
        mainHandler.postDelayed(countdownTickRunnable, COUNTDOWN_TICK_MS)
    }

    private fun speakPendingPhraseAndFinish() {
        val phrase = pendingPhrase ?: return
        clearCountdown()
        resetSequence()
        uiLastSpoken.value = phrase
        setCommunicationState(LisaCommunicationState.Speaking(phrase))
        speak(phrase)
    }

    private fun cancelCountdown() {
        clearCountdown()
        resetSequence()
        savedSequenceLeft = 0
        savedSequenceRight = 0
        setCommunicationState(LisaCommunicationState.Cancelled)
        mainHandler.postDelayed({ updateReadyOrWaitingState() }, 600L)
    }

    /**
     * Edit during countdown: restores the preserved wink sequence so the user can
     * continue adjusting without starting completely over. Does not speak.
     * If the saved sequence is invalid, falls back to cancel-and-retry (Listening).
     */
    private fun editCountdownAndRetry() {
        val restoreLeft = savedSequenceLeft
        val restoreRight = savedSequenceRight
        clearCountdown()
        uiPendingPhrase.value = null

        if (isSequenceEligibleForSpeech(restoreLeft, restoreRight)) {
            leftWinks = restoreLeft
            rightWinks = restoreRight
            uiDiagLeftCount.value = restoreLeft
            uiDiagRightCount.value = restoreRight
            lastWinkTimeMs = System.currentTimeMillis()
            sequenceStartMs = System.currentTimeMillis()
            leftWinkFrameStreak = 0
            rightWinkFrameStreak = 0
            leftWinkGestureCounted = false
            rightWinkGestureCounted = false
            setCommunicationState(LisaCommunicationState.WaitingForNextWink)
        } else {
            savedSequenceLeft = 0
            savedSequenceRight = 0
            resetSequence()
            setCommunicationState(LisaCommunicationState.Listening)
        }
    }

    private val sequenceStateRunnable = Runnable {
        if (emergencyActive) return@Runnable
        if (leftWinks > 0 || rightWinks > 0) {
            setCommunicationState(LisaCommunicationState.Sequence(leftWinks, rightWinks))
        }
    }

    private fun scheduleSequenceStateUpdate() {
        mainHandler.removeCallbacks(sequenceStateRunnable)
        mainHandler.postDelayed(sequenceStateRunnable, 400L)
    }

    private fun onWinkCounted(isLeft: Boolean) {
        val totalBefore = leftWinks + rightWinks - 1
        if (totalBefore == 0) {
            setCommunicationState(LisaCommunicationState.Listening)
        }
        setCommunicationState(
            if (isLeft) LisaCommunicationState.LeftWinkDetected
            else LisaCommunicationState.RightWinkDetected
        )
        scheduleSequenceStateUpdate()
    }

    private fun performReset() {
        emergencyAlarmController.stop()
        emergencyActive = false
        uiEmergencyActive.value = false
        tts?.stop()
        clearCountdown()
        savedSequenceLeft = 0
        savedSequenceRight = 0
        resetSequence()
        setCommunicationState(LisaCommunicationState.Reset)
        mainHandler.postDelayed({ updateReadyOrWaitingState() }, 500L)
    }

    private fun startEmergencyMode() {
        emergencyActive = true
        uiEmergencyActive.value = true
        uiLastSpoken.value = "Emergency"
        setCommunicationState(LisaCommunicationState.EmergencyAlarmActive)
        emergencyAlarmController.start(leftWinks, rightWinks)
        resetSequence()
    }

    private fun applySensitivityLevel(level: Int) {
        val settings = sensitivityPresets.getValue(level.coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL))
        closedEyeThreshold = settings.closedEyeThreshold
        openEyeThreshold = settings.openEyeThreshold
        requiredWinkFrames = settings.requiredWinkFrames
        uiSensitivityLevel.value = level
    }

    private fun changeSensitivity(delta: Int) {
        val newLevel = (uiSensitivityLevel.value + delta).coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL)
        if (newLevel == uiSensitivityLevel.value) return
        applySensitivityLevel(newLevel)
        saveSensitivityLevel(this, newLevel)
        leftWinkFrameStreak = 0
        rightWinkFrameStreak = 0
        leftWinkGestureCounted = false
        rightWinkGestureCounted = false
    }

    private fun updateDiagnostics(leftProb: Float?, rightProb: Float?) {
        uiDiagLeftEye.value = leftProb?.let { "%.2f".format(it) } ?: "--"
        uiDiagRightEye.value = rightProb?.let { "%.2f".format(it) } ?: "--"
        uiDiagLeftCount.value = leftWinks
        uiDiagRightCount.value = rightWinks
    }

    // --------- Camera + ML processing ----------
    private fun processFrame(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        detector.process(image)
            .addOnSuccessListener { faces ->
                if (emergencyActive) {
                    updateDiagnostics(null, null)
                    return@addOnSuccessListener
                }
                if (faces.isEmpty()) {
                    uiFacePresent.value = false
                    prevLeftProb = null
                    prevRightProb = null
                    updateDiagnostics(null, null)
                    if (leftWinks == 0 && rightWinks == 0) {
                        setCommunicationState(LisaCommunicationState.WaitingForFace)
                    }
                    return@addOnSuccessListener
                }
                uiFacePresent.value = true
                val face = faces[0]
                if (leftWinks == 0 && rightWinks == 0 && lastWinkTimeMs == 0L && !countdownActive) {
                    val current = uiCommunicationState.value
                    if (current is LisaCommunicationState.WaitingForFace ||
                        current is LisaCommunicationState.Ready ||
                        current is LisaCommunicationState.Reset ||
                        current is LisaCommunicationState.Cancelled ||
                        current is LisaCommunicationState.NoPhraseMatched
                    ) {
                        setCommunicationState(LisaCommunicationState.Listening)
                    }
                }
                handleWinks(face)
            }
            .addOnFailureListener {
                uiFacePresent.value = false
                prevLeftProb = null
                prevRightProb = null
                updateDiagnostics(null, null)
                if (!emergencyActive && leftWinks == 0 && rightWinks == 0) {
                    setCommunicationState(LisaCommunicationState.WaitingForFace)
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun handleWinks(face: Face) {
        if (emergencyActive) return

        val eyes = userEyeProbabilities(face)
        if (eyes == null) {
            updateDiagnostics(null, null)
            return
        }
        val leftProb = eyes.userLeft
        val rightProb = eyes.userRight

        if (countdownActive) {
            handleCountdownWinks(leftProb, rightProb)
            return
        }

        processSequenceWinks(leftProb, rightProb)
    }

    private fun handleCountdownWinks(leftProb: Float, rightProb: Float) {
        val leftUncertain = leftProb in closedEyeThreshold..openEyeThreshold
        val rightUncertain = rightProb in closedEyeThreshold..openEyeThreshold
        if (leftUncertain && rightUncertain) {
            updateDiagnostics(leftProb, rightProb)
            return
        }

        val prevLeft = prevLeftProb
        val prevRight = prevRightProb
        if (prevLeft != null && prevRight != null) {
            val unstable = abs(leftProb - prevLeft) > EYE_PROB_JUMP_THRESHOLD ||
                abs(rightProb - prevRight) > EYE_PROB_JUMP_THRESHOLD
            if (unstable) {
                prevLeftProb = leftProb
                prevRightProb = rightProb
                updateDiagnostics(leftProb, rightProb)
                return
            }
        }
        prevLeftProb = leftProb
        prevRightProb = rightProb

        val leftWinkCandidate = leftProb < closedEyeThreshold && rightProb > openEyeThreshold
        val rightWinkCandidate = rightProb < closedEyeThreshold && leftProb > openEyeThreshold
        val now = System.currentTimeMillis()

        if (leftWinkCandidate) {
            leftWinkFrameStreak += 1
        } else {
            leftWinkFrameStreak = 0
            leftWinkGestureCounted = false
        }

        if (rightWinkCandidate) {
            rightWinkFrameStreak += 1
        } else {
            rightWinkFrameStreak = 0
            rightWinkGestureCounted = false
        }

        uiDevLeftStreak.value = leftWinkFrameStreak
        uiDevRightStreak.value = rightWinkFrameStreak
        updateDiagnostics(leftProb, rightProb)

        if (leftWinkFrameStreak >= requiredWinkFrames &&
            !countdownLeftHandled &&
            !leftWinkGestureCounted &&
            now - lastLeftWinkCountedMs >= WINK_COOLDOWN_MS
        ) {
            leftWinkGestureCounted = true
            countdownLeftHandled = true
            lastLeftWinkCountedMs = now
            cancelCountdown()
            return
        }

        if (rightWinkFrameStreak >= requiredWinkFrames &&
            !countdownRightHandled &&
            !rightWinkGestureCounted &&
            now - lastRightWinkCountedMs >= WINK_COOLDOWN_MS
        ) {
            rightWinkGestureCounted = true
            countdownRightHandled = true
            lastRightWinkCountedMs = now
            speakPendingPhraseAndFinish()
        }
    }

    private fun processSequenceWinks(leftProb: Float, rightProb: Float) {
        val leftUncertain = leftProb in closedEyeThreshold..openEyeThreshold
        val rightUncertain = rightProb in closedEyeThreshold..openEyeThreshold
        if (leftUncertain && rightUncertain) {
            updateDiagnostics(leftProb, rightProb)
            updateSequencePauseState(leftProb, rightProb)
            return
        }

        val prevLeft = prevLeftProb
        val prevRight = prevRightProb
        if (prevLeft != null && prevRight != null) {
            val unstable = abs(leftProb - prevLeft) > EYE_PROB_JUMP_THRESHOLD ||
                abs(rightProb - prevRight) > EYE_PROB_JUMP_THRESHOLD
            if (unstable) {
                prevLeftProb = leftProb
                prevRightProb = rightProb
                updateDiagnostics(leftProb, rightProb)
                updateSequencePauseState(leftProb, rightProb)
                return
            }
        }
        prevLeftProb = leftProb
        prevRightProb = rightProb

        val leftWinkCandidate = leftProb < closedEyeThreshold && rightProb > openEyeThreshold
        val rightWinkCandidate = rightProb < closedEyeThreshold && leftProb > openEyeThreshold

        val now = System.currentTimeMillis()

        if (leftWinkCandidate) {
            leftWinkFrameStreak += 1
        } else {
            leftWinkFrameStreak = 0
            leftWinkGestureCounted = false
        }

        if (rightWinkCandidate) {
            rightWinkFrameStreak += 1
        } else {
            rightWinkFrameStreak = 0
            rightWinkGestureCounted = false
        }

        if (leftWinkFrameStreak >= requiredWinkFrames &&
            !leftWinkGestureCounted &&
            now - lastLeftWinkCountedMs >= WINK_COOLDOWN_MS
        ) {
            leftWinkGestureCounted = true
            lastLeftWinkCountedMs = now
            leftWinks += 1
            if (sequenceStartMs == 0L) sequenceStartMs = now
            onWinkCounted(isLeft = true)
            if (isEmergencySequence(leftWinks, rightWinks)) {
                finalizeSequence()
                return
            }
        }

        if (rightWinkFrameStreak >= requiredWinkFrames &&
            !rightWinkGestureCounted &&
            now - lastRightWinkCountedMs >= WINK_COOLDOWN_MS
        ) {
            rightWinkGestureCounted = true
            lastRightWinkCountedMs = now
            rightWinks += 1
            if (sequenceStartMs == 0L) sequenceStartMs = now
            onWinkCounted(isLeft = false)
            if (isEmergencySequence(leftWinks, rightWinks)) {
                finalizeSequence()
                return
            }
        }

        val hasCountedWinks = leftWinks > 0 || rightWinks > 0
        if (hasCountedWinks) {
            if (wasLeftWinkCandidate && !leftWinkCandidate) {
                lastWinkTimeMs = now
            }
            if (wasRightWinkCandidate && !rightWinkCandidate) {
                lastWinkTimeMs = now
            }
        }
        wasLeftWinkCandidate = leftWinkCandidate
        wasRightWinkCandidate = rightWinkCandidate

        uiDevLeftStreak.value = leftWinkFrameStreak
        uiDevRightStreak.value = rightWinkFrameStreak
        updateDiagnostics(leftProb, rightProb)
        updateSequencePauseState(leftProb, rightProb)

        if (lastWinkTimeMs == 0L) return

        val activelyWinking = leftWinkCandidate || rightWinkCandidate
        val idleMs = now - lastWinkTimeMs
        val totalWindowMs = now - sequenceStartMs
        val finalize = hasCountedWinks && !activelyWinking &&
            shouldFinalizeSequence(
                left = leftWinks,
                right = rightWinks,
                idleMs = idleMs,
                sequenceAgeMs = totalWindowMs,
                idleTimeoutMs = SEQUENCE_IDLE_TIMEOUT_MS,
                maxWindowMs = SEQUENCE_MAX_WINDOW_MS,
                mappings = mappingsState
            )

        if (finalize) {
            finalizeSequence()
        }
    }

    private fun updateSequencePauseState(leftProb: Float, rightProb: Float) {
        if (emergencyActive) return
        if (leftWinks == 0 && rightWinks == 0) return

        when (uiCommunicationState.value) {
            is LisaCommunicationState.LeftWinkDetected,
            is LisaCommunicationState.RightWinkDetected,
            is LisaCommunicationState.Listening,
            is LisaCommunicationState.ProcessingSequence,
            is LisaCommunicationState.Speaking,
            is LisaCommunicationState.MessageDelivered,
            is LisaCommunicationState.NoPhraseMatched,
            is LisaCommunicationState.Reset,
            is LisaCommunicationState.Detected,
            is LisaCommunicationState.CountdownConfirm,
            LisaCommunicationState.EmergencyAlarmActive -> return
            else -> Unit
        }

        val leftWinkCandidate = leftProb < closedEyeThreshold && rightProb > openEyeThreshold
        val rightWinkCandidate = rightProb < closedEyeThreshold && leftProb > openEyeThreshold
        val activelyWinking = leftWinkCandidate || rightWinkCandidate
        if (activelyWinking) return

        val now = System.currentTimeMillis()
        if (lastWinkTimeMs == 0L) return
        val idleMs = now - lastWinkTimeMs
        val totalWindowMs = now - sequenceStartMs
        val (seqLeft, seqRight) = currentSequence(leftWinks, rightWinks)
        val exact = findExactMapping(seqLeft, seqRight, mappingsState)
        val continuation = hasLongerContinuation(seqLeft, seqRight, mappingsState)
        val finalize = shouldFinalizeSequence(
            left = seqLeft,
            right = seqRight,
            idleMs = idleMs,
            sequenceAgeMs = totalWindowMs,
            idleTimeoutMs = SEQUENCE_IDLE_TIMEOUT_MS,
            maxWindowMs = SEQUENCE_MAX_WINDOW_MS,
            mappings = mappingsState
        )
        if (finalize) return

        if (exact != null && continuation) {
            setCommunicationState(LisaCommunicationState.PossibleMatch(exact.phrase))
        } else {
            setCommunicationState(LisaCommunicationState.WaitingForNextWink)
        }
    }

    private fun finalizeSequence() {
        val capturedLeft = leftWinks
        val capturedRight = rightWinks

        if (!isSequenceEligibleForSpeech(capturedLeft, capturedRight)) {
            resetSequence()
            updateReadyOrWaitingState()
            return
        }

        setCommunicationState(LisaCommunicationState.ProcessingSequence)

        if (isEmergencySequence(capturedLeft, capturedRight)) {
            startEmergencyMode()
            return
        }

        val phrase = findPhraseFor(capturedLeft, capturedRight)
        resetSequence()

        if (phrase != null) {
            startCountdown(phrase, capturedLeft, capturedRight)
        } else {
            setCommunicationState(LisaCommunicationState.NoPhraseMatched)
            mainHandler.postDelayed({ updateReadyOrWaitingState() }, 1500L)
        }
    }

    private fun findPhraseFor(l: Int, r: Int): String? =
        findExactMapping(l, r, mappingsState)?.phrase

    private fun resetSequence() {
        leftWinks = 0
        rightWinks = 0
        lastWinkTimeMs = 0L
        sequenceStartMs = 0L
        leftWinkFrameStreak = 0
        rightWinkFrameStreak = 0
        leftWinkGestureCounted = false
        rightWinkGestureCounted = false
        wasLeftWinkCandidate = false
        wasRightWinkCandidate = false
        uiDiagLeftCount.value = 0
        uiDiagRightCount.value = 0
        mainHandler.removeCallbacks(sequenceStateRunnable)
    }
}

@Composable
private fun CameraPreview(
    onFrame: (ImageProxy) -> Unit
) {
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                    onFrame(imageProxy)
                }

                val selector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT) // FRONT camera default ✅
                    .build()

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        (ctx as ComponentActivity),
                        selector,
                        preview,
                        analysis
                    )
                } catch (e: Exception) {
                    Toast.makeText(ctx, "Camera failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

// ---------------- Preferences ----------------
private const val PREFS_NAME = "lisa_prefs"
private const val KEY_CUSTOM_MAPS = "custom_maps"
private const val KEY_SENSITIVITY_LEVEL = "sensitivity_level"
private const val KEY_DEVELOPER_MODE = "developer_mode"

private fun loadSensitivityLevel(context: Context): Int {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getInt(KEY_SENSITIVITY_LEVEL, DEFAULT_SENSITIVITY_LEVEL)
        .coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL)
}

private fun saveSensitivityLevel(context: Context, level: Int) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putInt(KEY_SENSITIVITY_LEVEL, level.coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL))
        .apply()
}

private fun loadDeveloperMode(context: Context): Boolean {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_DEVELOPER_MODE, false)
}

private fun saveDeveloperMode(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_DEVELOPER_MODE, enabled)
        .apply()
}

// Format per line: "L,R|phrase"
private fun saveCustomMappings(context: Context, custom: List<WinkMapping>) {
    val text = buildString {
        custom.forEach { m ->
            append("${m.left},${m.right}|${m.phrase.replace("\n", " ")}\n")
        }
    }
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_CUSTOM_MAPS, text)
        .apply()
}

private fun loadCustomMappings(context: Context): List<WinkMapping> {
    val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_CUSTOM_MAPS, "") ?: ""

    if (raw.isBlank()) return emptyList()

    return raw.lines()
        .mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isBlank()) return@mapNotNull null
            val parts = trimmed.split("|", limit = 2)
            if (parts.size != 2) return@mapNotNull null
            val lr = parts[0].split(",", limit = 2)
            if (lr.size != 2) return@mapNotNull null
            val l = lr[0].toIntOrNull() ?: return@mapNotNull null
            val r = lr[1].toIntOrNull() ?: return@mapNotNull null
            val phrase = parts[1].trim()
            if (phrase.isBlank()) return@mapNotNull null
            WinkMapping(l, r, phrase, isCustom = true)
        }
}
