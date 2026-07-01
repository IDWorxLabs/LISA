package com.idworx.lisa

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
        private const val SEQUENCE_MAX_WINDOW_MS = 4500L
        private const val EYE_PROB_JUMP_THRESHOLD = 0.28f
        private const val COUNTDOWN_TICK_MS = 1000L
    }

    private var countdownDurationSec = 3
    private var sequenceIdleTimeoutMs = 2500L

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
    private val uiActivePanel = mutableStateOf(LisaPanel.None)
    private val uiSettingsState = mutableStateOf(LisaSettingsUiState())
    private val uiDevLeftStreak = mutableStateOf(0)
    private val uiDevRightStreak = mutableStateOf(0)
    private val uiProfiles = mutableStateListOf<LisaUserProfile>()
    private val uiActiveProfileId = mutableStateOf("")
    private val uiTextSizeScale = mutableStateOf(1.0f)

    private lateinit var profileStore: LisaProfileStore
    private lateinit var caregiverStore: LisaCaregiverStore
    private val uiCaregivers = mutableStateListOf<LisaCaregiver>()
    private val uiActiveLanguage = mutableStateOf(PreferredLanguage.English)
    private val uiEmergencyNotifyNames = mutableStateOf<List<String>>(emptyList())
    private lateinit var releaseStore: LisaReleaseStore
    private val uiOnboardingCompleted = mutableStateOf(false)
    private val uiCameraPermissionGranted = mutableStateOf(false)
    private val uiCameraPermissionPermanentlyDenied = mutableStateOf(false)
    private val uiTestingChecklist = mutableStateOf<Map<String, Boolean>>(emptyMap())
    private val uiFeedbackSavedCount = mutableStateOf(0)
    private val uiVoiceSettingsState = mutableStateOf(LisaVoiceSettingsState())

    // phrase mappings
    private val mappingsState = mutableStateListOf<WinkMapping>()

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            uiCameraPermissionGranted.value = granted
            if (!granted) {
                uiCameraPermissionPermanentlyDenied.value =
                    !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
            } else {
                uiCameraPermissionPermanentlyDenied.value = false
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

        profileStore = LisaProfileStore(this)
        caregiverStore = LisaCaregiverStore(this)
        val legacySensitivity = loadSensitivityLevel(this)
        val legacyDeveloperMode = loadDeveloperMode(this)
        val profileState = profileStore.load(legacySensitivity, legacyDeveloperMode)
        uiProfiles.clear()
        uiProfiles.addAll(profileState.profiles)
        uiActiveProfileId.value = profileState.activeProfileId
        profileState.activeProfile?.let { applyProfileSettings(it, persist = false) }
        refreshCaregiversForActiveProfile()

        releaseStore = LisaReleaseStore(this)
        uiOnboardingCompleted.value = releaseStore.isOnboardingCompleted()
        uiTestingChecklist.value = releaseStore.loadChecklist()
        uiFeedbackSavedCount.value = releaseStore.loadFeedback().size
        refreshCameraPermissionState()

        setContent {
            LISATheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                    val uiStrings = LisaUiStrings.forLanguage(uiActiveLanguage.value)
                    val userDisplay = uiCommunicationState.value.toUserDisplay(
                        strings = uiStrings,
                        pendingPhrase = uiPendingPhrase.value,
                        countdown = uiCountdown.value,
                        leftWinkDots = uiDiagLeftCount.value,
                        rightWinkDots = uiDiagRightCount.value
                    )
                    LisaRootUI(
                        uiStrings = uiStrings,
                        userDisplay = userDisplay,
                        emergencyActive = uiEmergencyActive.value,
                        emergencyNotifyNames = uiEmergencyNotifyNames.value,
                        developerMode = uiDeveloperMode.value,
                        activePanel = uiActivePanel.value,
                        lastSpoken = uiLastSpoken.value,
                        countdownActive = countdownActive,
                        sensitivityLevel = uiSensitivityLevel.value,
                        settingsState = uiSettingsState.value.copy(
                            sensitivityLevel = uiSensitivityLevel.value,
                            developerMode = uiDeveloperMode.value
                        ),
                        textSizeScale = uiTextSizeScale.value,
                        profiles = uiProfiles.toList(),
                        activeProfileId = uiActiveProfileId.value,
                        caregivers = uiCaregivers.toList(),
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
                        onMenuClick = { toggleMenuPanel() },
                        onSelectPanel = { panel -> openPanel(panel) },
                        onClosePanel = { closeAllPanels() },
                        onBackToMenu = { openPanel(LisaPanel.Menu) },
                        onDeveloperModeChange = { enabled ->
                            updateActiveProfile { it.copy(developerMode = enabled) }
                        },
                        onSensitivityDecrease = { changeSensitivity(-1) },
                        onSensitivityIncrease = { changeSensitivity(1) },
                        onSettingsPlaceholderChange = { updated ->
                            updateActiveProfile { it.withUpdatedSettings(updated) }
                        },
                        onSelectProfile = { profileId -> switchToProfile(profileId) },
                        onCreateProfile = { createNewProfile() },
                        onUpdateProfile = { profile -> updateProfile(profile) },
                        onDeleteProfile = { profileId -> deleteProfile(profileId) },
                        onAddCaregiver = { caregiver -> addCaregiver(caregiver) },
                        onUpdateCaregiver = { caregiver -> updateCaregiver(caregiver) },
                        onDeleteCaregiver = { caregiverId -> deleteCaregiver(caregiverId) },
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
                            val newMap = WinkMapping(
                                left = left,
                                right = right,
                                vocabularyId = cleaned,
                                isCustom = true,
                                customPhrase = cleaned
                            )
                            mappingsState.add(newMap)
                            saveCustomMappings(this@MainActivity, mappingsState.filter { it.isCustom })
                            Toast.makeText(this@MainActivity, "Saved: L$left R$right → $cleaned", Toast.LENGTH_SHORT).show()
                        },
                        onboardingCompleted = uiOnboardingCompleted.value,
                        cameraPermissionGranted = uiCameraPermissionGranted.value,
                        cameraPermissionPermanentlyDenied = uiCameraPermissionPermanentlyDenied.value,
                        primaryUserName = activeProfile()?.name ?: "Primary User",
                        testingChecklist = uiTestingChecklist.value,
                        feedbackSavedCount = uiFeedbackSavedCount.value,
                        onPrimaryUserNameChange = { name ->
                            activeProfile()?.let { profile ->
                                updateProfile(profile.copy(name = name))
                            }
                        },
                        onCompleteOnboarding = { completeOnboarding() },
                        onRequestCameraPermission = { requestCameraPermissionFromUser() },
                        onOpenAppSettings = { openAppSettings() },
                        onSaveFeedback = { worked, confusing, winks, speech ->
                            saveFeedbackEntry(worked, confusing, winks, speech)
                        },
                        onToggleChecklistItem = { key, checked ->
                            toggleChecklistItem(key, checked)
                        },
                        voiceSettingsState = uiVoiceSettingsState.value,
                        onSelectTtsVoice = { voiceName -> selectTtsVoice(voiceName) },
                        onTestTtsVoice = { testTtsVoice() },
                        onInstallTtsVoiceData = { installTtsVoiceData() },
                        onOpenTtsSettings = { openTtsSettings() },
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

    override fun onResume() {
        super.onResume()
        refreshCameraPermissionState()
        activeProfile()?.let { applyTtsForProfile(it) }
        refreshVoiceSettingsState()
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
            activeProfile()?.let { LisaTtsVoiceManager.applyForProfile(tts!!, it) }
                ?: applyTtsForLanguage(uiActiveLanguage.value)
            refreshVoiceSettingsState()
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
        uiCountdown.value = countdownDurationSec
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

    private fun openPanel(panel: LisaPanel) {
        uiActivePanel.value = panel
        if (panel == LisaPanel.Voice || panel == LisaPanel.VoiceDevice) {
            refreshVoiceSettingsState()
        }
    }

    private fun closeAllPanels() {
        uiActivePanel.value = LisaPanel.None
    }

    private fun toggleMenuPanel() {
        uiActivePanel.value = when (uiActivePanel.value) {
            LisaPanel.Menu -> LisaPanel.None
            else -> LisaPanel.Menu
        }
    }

    private fun performReset() {
        emergencyAlarmController.stop()
        emergencyActive = false
        uiEmergencyActive.value = false
        uiEmergencyNotifyNames.value = emptyList()
        tts?.stop()
        clearCountdown()
        savedSequenceLeft = 0
        savedSequenceRight = 0
        resetSequence()
        closeAllPanels()
        setCommunicationState(LisaCommunicationState.Reset)
        mainHandler.postDelayed({ updateReadyOrWaitingState() }, 500L)
    }

    private fun startEmergencyMode() {
        emergencyActive = true
        uiEmergencyActive.value = true
        uiLastSpoken.value = "Emergency"
        setCommunicationState(LisaCommunicationState.EmergencyAlarmActive)
        val profileId = uiActiveProfileId.value
        val profileCaregivers = caregiverStore.loadForProfile(profileId)
        uiEmergencyNotifyNames.value = EmergencyNotificationService.notifyCaregiverPlaceholder(
            profileId = profileId,
            caregivers = profileCaregivers,
            sequenceLeft = leftWinks,
            sequenceRight = rightWinks
        )
        emergencyAlarmController.start(
            leftWinks,
            rightWinks,
            activeProfile()?.emergencyVolume ?: 1.0f,
            speechPhrase = LisaUiStrings.forLanguage(uiActiveLanguage.value).emergencySpeechPhrase
        )
        resetSequence()
    }

    private fun refreshCameraPermissionState() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        uiCameraPermissionGranted.value = granted
        if (!granted && releaseStore.wasCameraPermissionRequested()) {
            uiCameraPermissionPermanentlyDenied.value =
                !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
        } else if (granted) {
            uiCameraPermissionPermanentlyDenied.value = false
        }
    }

    private fun requestCameraPermissionFromUser() {
        releaseStore.markCameraPermissionRequested()
        requestCameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun openAppSettings() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
        )
    }

    private fun completeOnboarding() {
        releaseStore.setOnboardingCompleted(true)
        uiOnboardingCompleted.value = true
        refreshCameraPermissionState()
    }

    private fun saveFeedbackEntry(
        whatWorkedWell: String,
        whatWasConfusing: String,
        winkDetectionFeedback: String,
        speechTimingFeedback: String
    ) {
        releaseStore.saveFeedback(
            LisaFeedbackEntry(
                whatWorkedWell = whatWorkedWell.trim(),
                whatWasConfusing = whatWasConfusing.trim(),
                winkDetectionFeedback = winkDetectionFeedback.trim(),
                speechTimingFeedback = speechTimingFeedback.trim()
            )
        )
        uiFeedbackSavedCount.value = releaseStore.loadFeedback().size
        Toast.makeText(this, "Feedback saved locally", Toast.LENGTH_SHORT).show()
    }

    private fun toggleChecklistItem(key: String, checked: Boolean) {
        releaseStore.saveChecklistItem(key, checked)
        uiTestingChecklist.value = releaseStore.loadChecklist()
    }

    private fun applyProfileSettings(profile: LisaUserProfile, persist: Boolean = true) {
        uiActiveLanguage.value = profile.preferredLanguage
        applyTtsForProfile(profile)
        refreshVoiceSettingsState()
        applySensitivityLevel(profile.sensitivityLevel)
        uiDeveloperMode.value = profile.developerMode
        saveDeveloperMode(this, profile.developerMode)
        countdownDurationSec = profile.confirmationCountdownSec
        sequenceIdleTimeoutMs = (profile.sequenceTimeoutSec * 1000f).toLong()
        uiTextSizeScale.value = profile.textSizeScale
        emergencyAlarmController.setAlarmVolume(profile.emergencyVolume)
        uiSettingsState.value = profile.toSettingsUiState()
        if (persist) {
            saveProfilesToStore()
        }
    }

    private fun activeProfile(): LisaUserProfile? =
        uiProfiles.find { it.id == uiActiveProfileId.value }

    private fun saveProfilesToStore() {
        profileStore.saveProfiles(uiProfiles.toList(), uiActiveProfileId.value)
    }

    private fun updateActiveProfile(transform: (LisaUserProfile) -> LisaUserProfile) {
        val current = activeProfile() ?: return
        val updated = transform(current).copy(updatedAt = System.currentTimeMillis())
        val index = uiProfiles.indexOfFirst { it.id == current.id }
        if (index >= 0) {
            uiProfiles[index] = updated
        }
        applyProfileSettings(updated)
    }

    private fun updateProfile(profile: LisaUserProfile) {
        val index = uiProfiles.indexOfFirst { it.id == profile.id }
        if (index < 0) return
        val updated = profile.copy(updatedAt = System.currentTimeMillis())
        uiProfiles[index] = updated
        if (profile.id == uiActiveProfileId.value) {
            applyProfileSettings(updated)
        } else {
            saveProfilesToStore()
        }
    }

    private fun createNewProfile() {
        val newName = "Profile ${uiProfiles.size + 1}"
        val newProfile = LisaUserProfile.createNew(newName, activeProfile())
        uiProfiles.add(newProfile)
        switchToProfile(newProfile.id)
        Toast.makeText(this, "Created $newName", Toast.LENGTH_SHORT).show()
    }

    private fun switchToProfile(profileId: String) {
        val profile = uiProfiles.find { it.id == profileId } ?: return
        uiActiveProfileId.value = profileId
        applyProfileSettings(profile)
        refreshCaregiversForActiveProfile()
    }

    private fun applyTtsForProfile(profile: LisaUserProfile) {
        tts?.let { LisaTtsVoiceManager.applyForProfile(it, profile) }
    }

    private fun applyTtsForLanguage(language: PreferredLanguage) {
        activeProfile()?.let { applyTtsForProfile(it) } ?: run {
            tts?.language = LisaUiStrings.ttsLocale(language)
        }
    }

    private fun refreshVoiceSettingsState() {
        val profile = activeProfile() ?: return
        uiVoiceSettingsState.value = LisaTtsVoiceManager.buildSettingsState(
            tts = tts,
            profile = profile,
            ttsEngineLabel = resolveTtsEngineLabel()
        )
    }

    private fun resolveTtsEngineLabel(): String {
        val engine = Settings.Secure.getString(contentResolver, Settings.Secure.TTS_DEFAULT_SYNTH)
        if (engine.isNullOrBlank()) return "Android system default"
        return try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(engine, 0)
            ).toString()
        } catch (_: Exception) {
            engine
        }
    }

    private fun selectTtsVoice(voiceName: String) {
        updateActiveProfile { it.copy(selectedTtsVoiceName = voiceName) }
        refreshVoiceSettingsState()
    }

    private fun testTtsVoice() {
        val engine = tts ?: run {
            Toast.makeText(this, "Speech engine not ready", Toast.LENGTH_SHORT).show()
            return
        }
        activeProfile()?.let { LisaTtsVoiceManager.applyForProfile(engine, it) }
        val phrase = LisaTtsVoiceManager.samplePhrase(uiActiveLanguage.value)
        engine.speak(phrase, TextToSpeech.QUEUE_FLUSH, Bundle(), "LISA_TTS_TEST")
    }

    private fun installTtsVoiceData() {
        try {
            startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))
        } catch (_: Exception) {
            Toast.makeText(this, "Unable to open voice installer", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openTtsSettings() {
        val intents = listOf(
            Intent("com.android.settings.TTS_SETTINGS"),
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        )
        for (intent in intents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(packageManager) != null) {
                try {
                    startActivity(intent)
                    return
                } catch (_: Exception) {
                    // try next fallback
                }
            }
        }
        Toast.makeText(this, "Unable to open speech settings", Toast.LENGTH_SHORT).show()
    }

    private fun activeLanguage(): PreferredLanguage = uiActiveLanguage.value

    private fun refreshCaregiversForActiveProfile() {
        uiCaregivers.clear()
        uiCaregivers.addAll(caregiverStore.loadForProfile(uiActiveProfileId.value))
    }

    private fun addCaregiver(caregiver: LisaCaregiver) {
        caregiverStore.upsert(caregiver)
        refreshCaregiversForActiveProfile()
        Toast.makeText(this, "Caregiver added", Toast.LENGTH_SHORT).show()
    }

    private fun updateCaregiver(caregiver: LisaCaregiver) {
        caregiverStore.upsert(caregiver.copy(updatedAt = System.currentTimeMillis()))
        refreshCaregiversForActiveProfile()
        Toast.makeText(this, "Caregiver saved", Toast.LENGTH_SHORT).show()
    }

    private fun deleteCaregiver(caregiverId: String) {
        caregiverStore.delete(caregiverId)
        refreshCaregiversForActiveProfile()
        Toast.makeText(this, "Caregiver removed", Toast.LENGTH_SHORT).show()
    }

    private fun deleteProfile(profileId: String) {
        if (uiProfiles.size <= 1) return
        val index = uiProfiles.indexOfFirst { it.id == profileId }
        if (index < 0) return
        uiProfiles.removeAt(index)
        if (uiActiveProfileId.value == profileId) {
            val next = uiProfiles.first()
            uiActiveProfileId.value = next.id
            applyProfileSettings(next)
        } else {
            saveProfilesToStore()
        }
        Toast.makeText(this, "Profile deleted", Toast.LENGTH_SHORT).show()
    }

    private fun applySensitivityLevel(level: Int) {
        val settings = sensitivityPresets.getValue(level.coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL))
        closedEyeThreshold = settings.closedEyeThreshold
        openEyeThreshold = settings.openEyeThreshold
        requiredWinkFrames = settings.requiredWinkFrames
        uiSensitivityLevel.value = level
        uiSettingsState.value = uiSettingsState.value.copy(sensitivityLevel = level)
    }

    private fun changeSensitivity(delta: Int) {
        val newLevel = (uiSensitivityLevel.value + delta).coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL)
        if (newLevel == uiSensitivityLevel.value) return
        updateActiveProfile { it.copy(sensitivityLevel = newLevel) }
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
                idleTimeoutMs = sequenceIdleTimeoutMs,
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
            idleTimeoutMs = sequenceIdleTimeoutMs,
            maxWindowMs = SEQUENCE_MAX_WINDOW_MS,
            mappings = mappingsState
        )
        if (finalize) return

        if (exact != null && continuation) {
            setCommunicationState(LisaCommunicationState.PossibleMatch(exact.localizedPhrase(activeLanguage())))
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
        findExactMapping(l, r, mappingsState)?.localizedPhrase(activeLanguage())

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
            WinkMapping(l, r, vocabularyId = phrase, isCustom = true, customPhrase = phrase)
        }
}
