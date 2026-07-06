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
import com.idworx.lisa.features.onboardingguide.audio.OnboardingNarrationController
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.navigation.NavigationTrainingGestureHandler
import com.idworx.lisa.features.onboardingguide.services.TrainingProgressStore
import com.idworx.lisa.features.onboardingguide.services.TrainingSessionController
import com.idworx.lisa.features.experiencepolish.communicationworkspace.CommunicationWorkspaceEntryHandler
import com.idworx.lisa.features.experiencepolish.emotionalpresence.EmotionalPresenceEngine
import com.idworx.lisa.features.experiencepolish.emotionalpresence.model.PresenceSessionTracker
import com.idworx.lisa.features.experiencepolish.caregiverconfidence.CaregiverConfidenceEngine
import com.idworx.lisa.features.silentwelcome.LisaSpeechPolicy
import com.idworx.lisa.features.experiencepolish.caregiverconfidence.model.CaregiverSupportUiState
import com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionDiagnostics
import com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionProcessor
import com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionTuning
import com.idworx.lisa.features.blinkdetectionreliability.BlinkEyeProbabilities
import com.idworx.lisa.features.calibrationreliability.model.CalibrationHealthState
import com.idworx.lisa.features.companionmemory.engine.CompanionMemoryEngines
import com.idworx.lisa.features.companionmemory.integration.PersonalityMemoryAdapter
import com.idworx.lisa.features.companionmemory.integration.PracticeMemoryAdapter
import com.idworx.lisa.features.personality.model.AppFeature
import com.idworx.lisa.features.personality.model.DialogueContext
import com.idworx.lisa.features.personality.model.PresenceMoment
import com.idworx.lisa.features.corecommunicationreliability.engine.CommunicationReliabilityContext
import com.idworx.lisa.features.corecommunicationreliability.engine.CoreCommunicationReliabilityEngines
import com.idworx.lisa.features.calibrationreliability.engine.CalibrationReliabilityEngines
import com.idworx.lisa.features.communicationanalytics.integration.CommunicationAnalyticsBridge
import com.idworx.lisa.features.corecommunicationreliability.model.CommunicationMode
import com.idworx.lisa.features.corecommunicationreliability.model.CommunicationReliabilityOutcome
import com.idworx.lisa.features.corecommunicationreliability.model.PhraseReliabilityAction
import com.idworx.lisa.features.onboardingguide.state.GuidedTrainingUiState
import com.idworx.lisa.features.onboardingguide.state.TrainingEvent
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
        private const val COUNTDOWN_TICK_MS = 1000L
        private const val NO_PHRASE_MATCHED_DISPLAY_MS = 1800L
    }

    private var countdownDurationSec = 3
    private var sequenceIdleTimeoutMs = SEQUENCE_IDLE_TIMEOUT_MS
    private var sequenceMaxWindowMs = ResponseSpeed.default.maxSequenceWindowMs()

    private val sensitivityPresets = (MIN_SENSITIVITY_LEVEL..MAX_SENSITIVITY_LEVEL).associateWith { level ->
        BlinkDetectionTuning.forSensitivityLevel(level)
    }

    private fun sensitivitySettingsForLevel(level: Int): BlinkDetectionTuning =
        sensitivityPresets.getValue(level.coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL))

    private val blinkProcessor = BlinkDetectionProcessor(BlinkDetectionTuning.default)

    private var closedEyeThreshold = BlinkDetectionTuning.default.closedEyeThreshold
    private var openEyeThreshold = BlinkDetectionTuning.default.openEyeThreshold
    private var requiredWinkFrames = BlinkDetectionTuning.default.requiredWinkFrames

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

    private val uiBlinkDiagnostics = mutableStateOf(BlinkDetectionDiagnostics())

    private var pendingPhrase: String? = null
    private var countdownActive = false
    private var countdownLeftHandled = false
    private var countdownRightHandled = false
    private var savedSequenceLeft = 0
    private var savedSequenceRight = 0
    private val winkSideOrder = mutableListOf<Boolean>()
    private var workspaceIntroLines: List<String> = emptyList()
    private var workspaceIntroIndex: Int = 0
    private var presenceTracker = PresenceSessionTracker()
    private var pausedAtMs = 0L
    private val uiCaregiverSupport = mutableStateOf<CaregiverSupportUiState?>(null)
    private var lastReliabilityAttemptId: String? = null
    private var lastReliabilityPhraseId: String? = null

    private val communicationReliability = CoreCommunicationReliabilityEngines.default
    private val calibrationReliability = CalibrationReliabilityEngines.default

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
    private val uiEyesDetected = mutableStateOf(false)
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
    private val uiAcceptedBlinkFlash = mutableStateOf<String?>(null)
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
    private val uiQuickControlsOpen = mutableStateOf(false)
    private val uiPracticeModeOpen = mutableStateOf(false)
    private val uiPracticeItemIndex = mutableStateOf(0)
    private val uiPracticeFeedback = mutableStateOf<PracticeFeedback?>(null)
    private val uiGuidedNavigationState = mutableStateOf(GuidedNavigationState())
    private val uiSequenceProcessingDelaySec = mutableStateOf(SequenceProcessingDelay.DEFAULT_SECONDS)
    private val uiGuidedConfirmedPhrase = mutableStateOf<String?>(null)
    private val uiGuidedConfirmedLeft = mutableStateOf<Int?>(null)
    private val uiGuidedConfirmedRight = mutableStateOf<Int?>(null)
    private val uiListeningPaused = mutableStateOf(false)

    private lateinit var trainingProgressStore: TrainingProgressStore
    private lateinit var trainingSession: TrainingSessionController
    private var trainingNarration: OnboardingNarrationController? = null
    private val uiGuidedTrainingState = mutableStateOf(GuidedTrainingUiState())

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
        trainingProgressStore = TrainingProgressStore(this)
        CompanionMemoryEngines.init(this)
        CommunicationAnalyticsBridge.attach()
        CompanionMemoryEngines.default.startSession()
        trainingNarration = OnboardingNarrationController(
            ttsProvider = { tts },
            preferencesProvider = { uiGuidedTrainingState.value.progress.preferences },
            onSpeakingChanged = { speaking ->
                uiGuidedTrainingState.value = uiGuidedTrainingState.value.copy(narrationSpeaking = speaking)
            }
        )
        trainingSession = TrainingSessionController(
            store = trainingProgressStore,
            narration = trainingNarration!!,
            speakPhrase = { text -> speakTranslatedPhrase(text) },
            onPersist = { state -> uiGuidedTrainingState.value = state },
            onTrainingFinished = { refreshTrainingActiveState() },
            onCompleteSetupOnboarding = { completeOnboarding() }
        )
        trainingNarration!!.onSequenceComplete = {
            runOnUiThread { trainingSession.onNarrationSequenceComplete() }
        }
        trainingSession.attachDelayedHandler { delayMs, block ->
            mainHandler.postDelayed({ block() }, delayMs)
        }
        trainingSession.onEmergencyConfirmed = { startEmergencyMode() }
        trainingSession.onRecalibrationConfirmed = {
            trainingSession.startRecalibrationFlow()
            refreshTrainingActiveState()
        }
        applyColdLaunchSessionState()
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
                        sensitivityLevel = uiGuidedNavigationState.value.displaySensitivityLevel(uiSensitivityLevel.value),
                        responseTimeSec = uiGuidedNavigationState.value.displayResponseTimeSec(uiSequenceProcessingDelaySec.value),
                        settingsState = uiSettingsState.value.copy(
                            sensitivityLevel = uiSensitivityLevel.value,
                            sequenceProcessingDelaySec = uiSequenceProcessingDelaySec.value,
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
                        quickControlsOpen = uiQuickControlsOpen.value,
                        practiceModeOpen = uiPracticeModeOpen.value,
                        practiceItemIndex = uiPracticeItemIndex.value,
                        practiceFeedback = uiPracticeFeedback.value,
                        listeningPaused = uiListeningPaused.value,
                        onResponseSpeedChange = { speed -> setResponseSpeed(speed) },
                        onQuickControlsClose = { closeQuickControls() },
                        onQuickControlsDecreaseSensitivity = { changeSensitivity(-1) },
                        onQuickControlsIncreaseSensitivity = { changeSensitivity(1) },
                        onQuickControlsRepeat = {
                            val phrase = uiLastSpoken.value
                            if (phrase.isNotBlank()) speak(phrase)
                        },
                        onQuickControlsTogglePause = { toggleListeningPaused() },
                        onQuickControlsOpenPractice = { openPracticeMode() },
                        onPracticeClose = { closePracticeMode() },
                        guidedNavigationState = uiGuidedNavigationState.value,
                        guidedCategoryPage = guidedCurrentCategoryPage(),
                        guidedCategoryMenuTitles = GuidedVocabularyCatalog.categoryMenuTitles(guidedUiStrings()),
                        guidedConfirmedPhrase = uiGuidedConfirmedPhrase.value,
                        guidedConfirmedLeft = uiGuidedConfirmedLeft.value,
                        guidedConfirmedRight = uiGuidedConfirmedRight.value,
                        onGuidedNavigateUp = { applyGuidedTouchNavigation(GuidedModeNavigation.PREVIOUS_LEFT, GuidedModeNavigation.PREVIOUS_RIGHT) },
                        onGuidedSelectEnter = { applyGuidedTouchNavigation(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT) },
                        onGuidedBack = { applyGuidedTouchNavigation(GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT) },
                        onGuidedNavigateDown = { applyGuidedTouchNavigation(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT) },
                        onGuidedEmergency = { triggerGuidedEmergencyTouch() },
                        onGuidedCategories = { applyGuidedTouchNavigation(GuidedModeNavigation.CATEGORIES_LEFT, GuidedModeNavigation.CATEGORIES_RIGHT) },
                        onGuidedDecreaseValue = { applyGuidedTouchNavigation(GuidedModeNavigation.DECREASE_VALUE_LEFT, GuidedModeNavigation.DECREASE_VALUE_RIGHT) },
                        onGuidedIncreaseValue = { applyGuidedTouchNavigation(GuidedModeNavigation.INCREASE_VALUE_LEFT, GuidedModeNavigation.INCREASE_VALUE_RIGHT) },
                        onGuidedChooseCategory = { applyGuidedTouchNavigation(GuidedModeNavigation.CATEGORIES_LEFT, GuidedModeNavigation.CATEGORIES_RIGHT) },
                        onGuidedPhraseEntry = { entry -> applyGuidedTouchNavigation(entry.left, entry.right) },
                        onGuidedCategoryRow = { index -> openGuidedCategoryFromTouch(index) },
                        guidedTrainingActive = trainingSession.shouldShowTraining(),
                        guidedTrainingState = uiGuidedTrainingState.value,
                        guidedTrainingSetupStep = uiGuidedTrainingState.value.setupStep,
                        guidedTrainingReturningUser = trainingSession.isReturningUser(),
                        trainingEyeTracking = trainingEyeTrackingState(),
                        trainingBlinkDiagnostics = uiBlinkDiagnostics.value,
                        showBlinkDiagnostics = uiDeveloperMode.value,
                        caregiverSupport = uiCaregiverSupport.value,
                        onTrainingEvent = { event -> handleTrainingEvent(event) },
                        onTrainingWelcomeNarration = { trainingSession.welcomeNarration() },
                        onTrainingFirstLaunchNarration = { trainingSession.firstLaunchChoiceNarration() },
                        onTrainingSkipConfirmNarration = { trainingSession.skipConfirmNarration() },
                        onTrainingCompletionNarration = { trainingSession.completionNarration() },
                        onTrainingLessonNarration = { phrase, instruction ->
                            trainingSession.coachBeginLesson(phrase, instruction)
                        },
                        onTrainingNavigationNarration = { title, instruction ->
                            trainingSession.navigationNarration(title, instruction)
                        },
                        onTrainingSetupStepChange = { step ->
                            trainingSession.setSetupStep(step)
                            refreshTrainingActiveState()
                        },
                        onTrainingCalibrationStarted = { trainingSession.startCalibrationIfNeeded() },
                        onTrainingAdvanceCalibrationDot = { trainingSession.advanceCalibrationDot() },
                        onTrainingTouchLeftWink = { simulateTrainingWink(isLeft = true) },
                        onTrainingTouchRightWink = { simulateTrainingWink(isLeft = false) },
                        onTrainingReduceSensitivity = { changeSensitivity(-1) },
                        onTrainingIncreaseSensitivity = { changeSensitivity(1) },
                        guidedTrainingSensitivityLevel = uiSensitivityLevel.value,
                        onTrainingReplayTutorial = {
                            closeAllPanels()
                            trainingSession.beginAwaitingBrain1Decision(
                                com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionKind.ReplayLearning
                            )
                            refreshTrainingActiveState()
                        },
                        onTrainingPracticeCommunication = {
                            closeAllPanels()
                            handleTrainingEvent(TrainingEvent.PracticeCommunication)
                        },
                        onTrainingPracticeNavigation = {
                            closeAllPanels()
                            handleTrainingEvent(TrainingEvent.PracticeNavigation)
                        },
                        onTrainingResetProgress = {
                            trainingSession.beginAwaitingBrain1Decision(
                                com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionKind.ResetLearningProgress
                            )
                            refreshTrainingActiveState()
                        },
                        onTrainingPreferencesChange = { prefs ->
                            trainingSession.updatePreferences { prefs }
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

    override fun onPause() {
        super.onPause()
        pausedAtMs = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()
        refreshCameraPermissionState()
        activeProfile()?.let { applyTtsForProfile(it) }
        refreshVoiceSettingsState()
        maybeSpeakWarmReturnAfterBackground()
    }

    override fun onDestroy() {
        super.onDestroy()
        CompanionMemoryEngines.default.endSession()
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
                override fun onStart(utteranceId: String?) {
                    trainingNarration?.handleUtteranceStart(utteranceId)
                }

                override fun onDone(utteranceId: String?) {
                    if (OnboardingNarrationController.isNarrationUtterance(utteranceId)) {
                        trainingNarration?.handleUtteranceDone(utteranceId)
                        return
                    }
                    if (utteranceId == "LISA_SPEAK") {
                        runOnUiThread { onSpeechFinished() }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (OnboardingNarrationController.isNarrationUtterance(utteranceId)) {
                        trainingNarration?.handleUtteranceDone(utteranceId)
                        return
                    }
                    if (utteranceId == "LISA_SPEAK") {
                        runOnUiThread { onSpeechFinished() }
                    }
                }
            })
        }
    }

    private fun speakTranslatedPhrase(text: String) {
        if (!LisaSpeechPolicy.allowsPhraseTranslation()) return
        val params = Bundle()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "LISA_SPEAK")
    }

    private fun speakNarration(text: String) {
        if (!LisaSpeechPolicy.allowsNarration()) return
        val params = Bundle()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "LISA_NARRATION")
    }

    private fun speak(text: String) = speakTranslatedPhrase(text)

    private fun onSpeechFinished() {
        if (trainingSession.hasPendingInteractiveLessonSuccess()) {
            trainingSession.completePendingInteractiveLessonSuccess()
            refreshTrainingActiveState()
            setCommunicationState(LisaCommunicationState.Listening)
            return
        }
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
        val seqLeft = savedSequenceLeft
        val seqRight = savedSequenceRight
        clearCountdown()
        resetSequence()
        uiLastSpoken.value = phrase
        setCommunicationState(LisaCommunicationState.Speaking(phrase))
        communicationReliability.speechAdapter().onSpeechRequested(
            phraseText = phrase,
            phraseId = lastReliabilityPhraseId,
            ttsAvailable = tts != null
        )
        speak(phrase)
        communicationReliability.recordSpeechDelivery(
            attemptId = lastReliabilityAttemptId ?: "unknown",
            phraseText = phrase,
            phraseId = lastReliabilityPhraseId,
            sequenceLeft = seqLeft,
            sequenceRight = seqRight,
            mode = CommunicationMode.MAIN,
            emergency = false,
            success = true
        )
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
            blinkProcessor.resetGestureFlags()
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
            setCommunicationState(LisaCommunicationState.BuildingMessage)
        }
    }

    private fun scheduleSequenceStateUpdate() {
        mainHandler.removeCallbacks(sequenceStateRunnable)
        mainHandler.postDelayed(sequenceStateRunnable, 400L)
    }

    private fun currentBlinkOrder(): List<Boolean> = winkSideOrder.toList()

    private fun recordWinkSide(isLeft: Boolean) {
        winkSideOrder.add(isLeft)
    }

    private fun onWinkCounted(isLeft: Boolean) {
        recordWinkSide(isLeft)
        lastWinkTimeMs = System.currentTimeMillis()
        val totalBefore = leftWinks + rightWinks - 1
        if (totalBefore == 0) {
            setCommunicationState(LisaCommunicationState.BuildingMessage)
        } else {
            setCommunicationState(LisaCommunicationState.BuildingMessage)
        }
        setCommunicationState(
            if (isLeft) LisaCommunicationState.LeftWinkDetected
            else LisaCommunicationState.RightWinkDetected
        )
        if (trainingSession.isCommunicationLessonPhase()) {
            if (trainingSession.onLessonWink(isLeft, leftWinks, rightWinks, currentBlinkOrder())) {
                resetSequence()
                refreshTrainingActiveState()
                return
            }
            refreshTrainingActiveState()
            syncLessonPartialSequenceTimeout()
        } else if (trainingSession.shouldShowTraining()) {
            trainingSession.updateWinkDots(leftWinks, rightWinks)
        }
        scheduleSequenceStateUpdate()
    }

    private val lessonPartialSequenceTimeoutRunnable = Runnable {
        if (!trainingSession.isCommunicationLessonPhase()) return@Runnable
        if (leftWinks == 0 && rightWinks == 0) return@Runnable
        if (!trainingSession.isPartialSequenceInProgress(leftWinks, rightWinks, currentBlinkOrder())) {
            return@Runnable
        }
        resetSequence()
        trainingSession.applyPartialSequenceTimeout()
        refreshTrainingActiveState()
    }

    private fun syncLessonPartialSequenceTimeout() {
        if (!trainingSession.isCommunicationLessonPhase()) {
            cancelLessonPartialSequenceTimeout()
            return
        }
        if (leftWinks == 0 && rightWinks == 0) {
            cancelLessonPartialSequenceTimeout()
            return
        }
        if (!trainingSession.isPartialSequenceInProgress(leftWinks, rightWinks, currentBlinkOrder())) {
            cancelLessonPartialSequenceTimeout()
            return
        }
        mainHandler.removeCallbacks(lessonPartialSequenceTimeoutRunnable)
        mainHandler.postDelayed(
            lessonPartialSequenceTimeoutRunnable,
            com.idworx.lisa.features.onboardingguide.lessoninteraction.LessonInteractionEngine.PARTIAL_SEQUENCE_IDLE_MS
        )
    }

    private fun cancelLessonPartialSequenceTimeout() {
        mainHandler.removeCallbacks(lessonPartialSequenceTimeoutRunnable)
    }

    private fun shouldDeferLessonFinalize(): Boolean =
        trainingSession.isPartialSequenceInProgress(leftWinks, rightWinks, currentBlinkOrder())

    private fun rejectLessonWrongEyeBlink(isLeft: Boolean): Boolean {
        if (!trainingSession.isCommunicationLessonPhase()) return false
        if (!trainingSession.isWrongEyeBlink(isLeft, leftWinks, rightWinks, currentBlinkOrder())) {
            return false
        }
        resetSequence()
        cancelLessonPartialSequenceTimeout()
        trainingSession.applyWrongEyeFeedback()
        refreshTrainingActiveState()
        return true
    }

    private fun openPanel(panel: LisaPanel) {
        uiActivePanel.value = panel
        when (panel) {
            LisaPanel.Menu -> verifyTrainingNavigation(NavigationAction.OpenMenu)
            LisaPanel.MyCommunication -> verifyTrainingNavigation(NavigationAction.OpenCommunicationHistory)
            LisaPanel.Settings -> verifyTrainingNavigation(NavigationAction.OpenSettings)
            LisaPanel.CaregiverLinking -> verifyTrainingNavigation(NavigationAction.OpenCaregiver)
            else -> Unit
        }
        if (panel == LisaPanel.Voice || panel == LisaPanel.VoiceDevice) {
            refreshVoiceSettingsState()
        }
    }

    private fun closeAllPanels() {
        if (uiActivePanel.value != LisaPanel.None) {
            verifyTrainingNavigation(NavigationAction.CloseMenu)
        }
        uiActivePanel.value = LisaPanel.None
    }

    private fun toggleMenuPanel() {
        val wasMenu = uiActivePanel.value == LisaPanel.Menu
        uiActivePanel.value = when (uiActivePanel.value) {
            LisaPanel.Menu -> LisaPanel.None
            else -> LisaPanel.Menu
        }
        if (wasMenu) {
            verifyTrainingNavigation(NavigationAction.CloseMenu)
        } else {
            verifyTrainingNavigation(NavigationAction.OpenMenu)
        }
    }

    private fun performReset() {
        verifyTrainingNavigation(NavigationAction.ResetSequence)
        emergencyAlarmController.stop()
        emergencyActive = false
        uiEmergencyActive.value = false
        uiEmergencyNotifyNames.value = emptyList()
        closeQuickControls()
        closePracticeMode()
        uiGuidedNavigationState.value = GuidedNavigationState()
        uiGuidedConfirmedPhrase.value = null
        uiGuidedConfirmedLeft.value = null
        uiGuidedConfirmedRight.value = null
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
            maybePlayWorkspaceEntryIntro()
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

    private fun trainingEyeTrackingState(): com.idworx.lisa.features.onboardingguide.ui.TrainingEyeTrackingState {
        val interaction = uiGuidedTrainingState.value.lessonInteraction
        return com.idworx.lisa.features.onboardingguide.ui.TrainingEyeTrackingState(
            cameraActive = uiCameraPermissionGranted.value,
            faceDetected = uiFacePresent.value,
            eyesDetected = uiEyesDetected.value,
            leftBlinkCount = maxOf(leftWinks, interaction.liveLeftBlinks),
            rightBlinkCount = maxOf(rightWinks, interaction.liveRightBlinks),
            acceptedBlinkLabel = uiAcceptedBlinkFlash.value
        )
    }

    private val clearAcceptedBlinkRunnable = Runnable {
        uiAcceptedBlinkFlash.value = null
    }

    private fun flashAcceptedBlink(isLeft: Boolean) {
        uiAcceptedBlinkFlash.value = if (isLeft) "Left blink accepted" else "Right blink accepted"
        mainHandler.removeCallbacks(clearAcceptedBlinkRunnable)
        mainHandler.postDelayed(clearAcceptedBlinkRunnable, 900L)
    }

    private fun applyColdLaunchSessionState() {
        uiOnboardingCompleted.value = false
        uiGuidedNavigationState.value = GuidedNavigationState()
        uiQuickControlsOpen.value = false
        uiPracticeModeOpen.value = false
        uiGuidedConfirmedPhrase.value = null
        uiGuidedConfirmedLeft.value = null
        uiGuidedConfirmedRight.value = null
        uiListeningPaused.value = false
        resetSequence()
        closeAllPanels()
        uiGuidedTrainingState.value = trainingSession.state
    }

    private fun completeOnboarding() {
        releaseStore.setOnboardingCompleted(true)
        uiOnboardingCompleted.value = true
        refreshCameraPermissionState()
        maybePlayWorkspaceEntryIntro()
    }

    private fun refreshCaregiverSupport() {
        if (trainingSession.shouldShowTraining() || emergencyActive) {
            uiCaregiverSupport.value = null
            return
        }
        val health = calibrationReliability.currentHealth()
        val facePresent = uiFacePresent.value
        if (facePresent && health == CalibrationHealthState.Healthy) {
            uiCaregiverSupport.value = null
            return
        }
        uiCaregiverSupport.value = CaregiverConfidenceEngine.communicationSupport(
            facePresent = facePresent,
            calibrationHealth = health
        )
    }

    private fun maybePlayWorkspaceEntryIntro() {
        if (!LisaSpeechPolicy.allowsNarration()) {
            if (CommunicationWorkspaceEntryHandler.shouldPlayEntryIntro(
                    releaseStore,
                    uiOnboardingCompleted.value,
                    uiCameraPermissionGranted.value
                )
            ) {
                CommunicationWorkspaceEntryHandler.markEntryIntroComplete(releaseStore)
            }
            return
        }
        if (!CommunicationWorkspaceEntryHandler.shouldPlayEntryIntro(
                releaseStore,
                uiOnboardingCompleted.value,
                uiCameraPermissionGranted.value
            )
        ) {
            return
        }
        CommunicationWorkspaceEntryHandler.markEntryIntroComplete(releaseStore)
        val presenceLines = sessionStartPresenceLines()
        workspaceIntroLines = presenceLines + CommunicationWorkspaceEntryHandler.entryDialogues()
        workspaceIntroIndex = 0
        speakNextWorkspaceIntroLine()
    }

    private fun sessionStartPresenceLines(): List<String> {
        val ctx = presenceDialogueContext()
        val moment = if (ctx.returningUser || ctx.daysSinceLastSession > 0) {
            PresenceMoment.WarmReturnGreeting
        } else {
            PresenceMoment.SessionOpening
        }
        if (!EmotionalPresenceEngine.shouldSpeak(moment, ctx, presenceTracker)) return emptyList()
        val lines = EmotionalPresenceEngine.dialogueTexts(ctx, moment)
        if (lines.isEmpty()) return emptyList()
        presenceTracker = EmotionalPresenceEngine.recordSpoken(presenceTracker, moment)
        return lines
    }

    private fun presenceDialogueContext(idleDurationMs: Long = 0L): DialogueContext =
        PersonalityMemoryAdapter.enrichDialogueContext(
            DialogueContext(
                feature = AppFeature.Communication,
                locale = "en",
                idleDurationMs = idleDurationMs
            ),
            CompanionMemoryEngines.default.getGreetingContext()
        )

    private fun maybeSpeakWarmReturnAfterBackground() {
        if (pausedAtMs == 0L) return
        val awayMs = System.currentTimeMillis() - pausedAtMs
        pausedAtMs = 0L
        if (awayMs < 60_000L) return
        if (trainingSession.shouldShowTraining()) return
        if (countdownActive || emergencyActive) return
        val ctx = presenceDialogueContext()
        if (!EmotionalPresenceEngine.shouldSpeak(PresenceMoment.WarmReturnGreeting, ctx, presenceTracker)) return
        val lines = EmotionalPresenceEngine.dialogueTexts(ctx, PresenceMoment.WarmReturnGreeting)
        if (lines.isEmpty()) return
        speakNarration(lines.first())
        presenceTracker = EmotionalPresenceEngine.recordSpoken(presenceTracker, PresenceMoment.WarmReturnGreeting)
    }

    private fun maybeSpeakLongPauseEncouragement(idleMs: Long) {
        if (uiCommunicationState.value != LisaCommunicationState.WaitingForNextWink) return
        if (leftWinks == 0 && rightWinks == 0) return
        if (trainingSession.shouldShowTraining() || countdownActive || emergencyActive) return
        val ctx = presenceDialogueContext(idleDurationMs = idleMs)
        if (!EmotionalPresenceEngine.shouldSpeak(PresenceMoment.LongPauseEncouragement, ctx, presenceTracker)) return
        val lines = EmotionalPresenceEngine.dialogueTexts(ctx, PresenceMoment.LongPauseEncouragement)
        if (lines.isEmpty()) return
        speakNarration(lines.first())
        presenceTracker = EmotionalPresenceEngine.recordSpoken(presenceTracker, PresenceMoment.LongPauseEncouragement)
    }

    private fun speakNextWorkspaceIntroLine() {
        if (workspaceIntroIndex >= workspaceIntroLines.size) return
        speakNarration(workspaceIntroLines[workspaceIntroIndex++])
        if (workspaceIntroIndex < workspaceIntroLines.size) {
            mainHandler.postDelayed({ speakNextWorkspaceIntroLine() }, 3200L)
        }
    }

    private fun refreshTrainingActiveState() {
        uiGuidedTrainingState.value = trainingSession.state
    }

    private fun handleTrainingEvent(event: TrainingEvent) {
        trainingSession.dispatch(event)
        refreshTrainingActiveState()
    }

    private fun simulateTrainingWink(isLeft: Boolean) {
        if (rejectLessonWrongEyeBlink(isLeft)) return
        if (isLeft) {
            leftWinks = (leftWinks + 1).coerceAtMost(7)
        } else {
            rightWinks = (rightWinks + 1).coerceAtMost(7)
        }
        uiDiagLeftCount.value = leftWinks
        uiDiagRightCount.value = rightWinks
        if (trainingSession.isCommunicationLessonPhase()) {
            onWinkCounted(isLeft)
            if (trainingSession.isPartialSequenceInProgress(leftWinks, rightWinks, currentBlinkOrder())) {
                return
            }
        } else {
            trainingSession.updateWinkDots(leftWinks, rightWinks)
        }
        if (isSequenceEligibleForSpeech(leftWinks, rightWinks)) {
            handleTrainingSequence(leftWinks, rightWinks)
            resetSequence()
        }
    }

    private fun handleTrainingSequence(left: Int, right: Int) {
        val order = currentBlinkOrder()
        if (trainingSession.handleBrain1Interaction(left, right, order)) {
            refreshTrainingActiveState()
            return
        }
        if (trainingSession.isNavigationTrainingActive()) {
            handleNavigationTrainingSequence(left, right)
            refreshTrainingActiveState()
            return
        }
        trainingSession.handleSequence(left, right, activeLanguage(), order)
        refreshTrainingActiveState()
    }

    private fun handleNavigationTrainingSequence(left: Int, right: Int) {
        when {
            isEmergencySequence(left, right) -> {
                trainingSession.beginEmergencyConfirm()
            }
            NavigationTrainingGestureHandler.opensCategories(left, right) -> {
                if (guidedOverlayActive()) {
                    applyGuidedTouchNavigation(left, right)
                } else {
                    uiGuidedNavigationState.value = GuidedNavigationController.openCategoryMenu(
                        uiGuidedNavigationState.value
                    )
                    verifyTrainingNavigation(NavigationAction.OpenCategories)
                }
            }
            guidedOverlayActive() && GuidedModeNavigation.isGlobalNavigationSequence(left, right) -> {
                handleGuidedOverlaySequence(left, right)
                when {
                    GuidedModeNavigation.isSelectSequence(left, right) ->
                        verifyTrainingNavigation(NavigationAction.SelectCategory)
                    GuidedModeNavigation.isBackSequence(left, right) ->
                        verifyTrainingNavigation(NavigationAction.CloseMenu)
                }
            }
            GuidedModeNavigation.isSelectSequence(left, right) ->
                verifyTrainingNavigation(NavigationAction.SelectCategory)
            GuidedModeNavigation.isBackSequence(left, right) ->
                verifyTrainingNavigation(NavigationAction.CloseMenu)
            LisaSystemLanguage.resolveQuickControlCommand(left, right) == SystemCommandAction.RepeatLastPhrase ->
                executeQuickControlAction(SystemCommandAction.RepeatLastPhrase)
            LisaSystemLanguage.resolveGlobalCommand(left, right) == SystemCommandAction.OpenQuickControls -> {
                openQuickControls()
                verifyTrainingNavigation(NavigationAction.OpenQuickControls)
            }
            else -> Unit
        }
    }

    private fun verifyTrainingNavigation(action: NavigationAction) {
        trainingSession.verifyNavigation(action)
        refreshTrainingActiveState()
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
        applySensitivityLevel(profile.sensitivityLevel, persist = false)
        uiDeveloperMode.value = profile.developerMode
        saveDeveloperMode(this, profile.developerMode)
        countdownDurationSec = profile.confirmationCountdownSec
        applySequenceProcessingDelay(profile.sequenceProcessingDelaySec, persist = false)
        uiTextSizeScale.value = profile.textSizeScale
        emergencyAlarmController.setAlarmVolume(profile.emergencyVolume)
        uiSettingsState.value = profile.toSettingsUiState()
        if (persist) {
            saveProfilesToStore()
        }
    }

    private fun applyResponseSpeed(speed: ResponseSpeed) {
        applySequenceProcessingDelay(speed.toProcessingDelaySeconds(), persist = false)
    }

    private fun setResponseSpeed(speed: ResponseSpeed) {
        applySequenceProcessingDelay(speed.toProcessingDelaySeconds())
    }

    private fun applySequenceProcessingDelay(seconds: Int, persist: Boolean = true) {
        val sec = SequenceProcessingDelay.coerce(seconds)
        sequenceIdleTimeoutMs = SequenceProcessingDelay.toMillis(sec)
        sequenceMaxWindowMs = ResponseSpeed.fromProcessingDelaySeconds(sec).maxSequenceWindowMs()
        uiSequenceProcessingDelaySec.value = sec
        uiSettingsState.value = uiSettingsState.value.copy(
            sequenceProcessingDelaySec = sec,
            sequenceIdleTimeoutSec = sec.toFloat(),
            responseSpeed = ResponseSpeed.fromProcessingDelaySeconds(sec)
        )
        if (persist) {
            updateActiveProfile {
                it.copy(
                    sequenceProcessingDelaySec = sec,
                    responseSpeed = ResponseSpeed.fromProcessingDelaySeconds(sec),
                    sequenceTimeoutSec = sec.toFloat()
                )
            }
        }
    }

    private fun setSequenceProcessingDelay(seconds: Int) {
        applySequenceProcessingDelay(seconds)
    }

    private fun openQuickControls() {
        uiQuickControlsOpen.value = true
        verifyTrainingNavigation(NavigationAction.OpenQuickControls)
    }

    private fun closeQuickControls() {
        uiQuickControlsOpen.value = false
    }

    private fun openPracticeMode() {
        closeQuickControls()
        uiPracticeModeOpen.value = true
        uiPracticeItemIndex.value = 0
        uiPracticeFeedback.value = null
        PracticeMemoryAdapter.onPracticeSessionStarted(CompanionMemoryEngines.default)
    }

    private fun closePracticeMode() {
        if (uiPracticeModeOpen.value) {
            PracticeMemoryAdapter.onPracticeSessionEnded(CompanionMemoryEngines.default)
        }
        uiPracticeModeOpen.value = false
        uiPracticeFeedback.value = null
    }

    private fun guidedUiStrings(): LisaUiStrings =
        LisaUiStrings.forLanguage(activeLanguage())

    private fun guidedCatalogContext(): GuidedCatalogContext =
        GuidedCatalogContext(
            responseTimeSec = uiSequenceProcessingDelaySec.value,
            sensitivityLevel = uiSensitivityLevel.value
        )

    private fun guidedVisibleEntryCap(): Int =
        GuidedVocabularyCatalog.visibleEntryCount(
            screenWidthDp = resources.configuration.screenWidthDp,
            screenHeightDp = resources.configuration.screenHeightDp
        )

    private fun workspaceContinuationMappings(): List<WinkMapping> =
        WorkspacePhraseResolver.continuationMappings(
            state = uiGuidedNavigationState.value,
            language = activeLanguage(),
            uiStrings = guidedUiStrings(),
            catalogContext = guidedCatalogContext(),
            visibleEntryCap = guidedVisibleEntryCap()
        )

    private fun mappingsForSequenceContinuation(): List<WinkMapping> =
        if (guidedOverlayActive()) workspaceContinuationMappings() else mappingsState.toList()

    private fun guidedCurrentCategoryPage(): GuidedCategoryPage? =
        GuidedVocabularyCatalog.categoryAt(
            pageIndex = uiGuidedNavigationState.value.categoryIndex,
            language = activeLanguage(),
            uiStrings = guidedUiStrings(),
            catalogContext = guidedCatalogContext()
        )

    private fun guidedOverlayActive(): Boolean =
        GuidedVocabularyOverlayVisibility.shouldShowOverlay(
            onboardingCompleted = uiOnboardingCompleted.value,
            cameraPermissionGranted = uiCameraPermissionGranted.value,
            emergencyActive = emergencyActive,
            practiceModeOpen = uiPracticeModeOpen.value,
            quickControlsOpen = uiQuickControlsOpen.value
        )

    private fun applyGuidedTouchNavigation(left: Int, right: Int) {
        if (GuidedModeNavigation.isCategoriesSequence(left, right)) {
            verifyTrainingNavigation(NavigationAction.OpenCategories)
        }
        handleGuidedOverlaySequence(left, right)
    }

    private fun openGuidedCategoryFromTouch(categoryIndex: Int) {
        uiGuidedNavigationState.value = GuidedNavigationController.openCategoryDirectly(
            uiGuidedNavigationState.value,
            categoryIndex
        )
        verifyTrainingNavigation(NavigationAction.SelectCategory)
        val uiStrings = guidedUiStrings()
        uiGuidedConfirmedPhrase.value =
            GuidedVocabularyCatalog.categoryAt(
                categoryIndex,
                activeLanguage(),
                uiStrings,
                guidedCatalogContext()
            )?.title
        uiGuidedConfirmedLeft.value = GuidedModeNavigation.SELECT_LEFT
        uiGuidedConfirmedRight.value = GuidedModeNavigation.SELECT_RIGHT
        setCommunicationState(LisaCommunicationState.Listening)
        mainHandler.removeCallbacks(guidedConfirmationClearRunnable)
        mainHandler.postDelayed(guidedConfirmationClearRunnable, 1500L)
    }

    private fun triggerGuidedEmergencyTouch() {
        leftWinks = EMERGENCY_LEFT_WINKS
        rightWinks = EMERGENCY_RIGHT_WINKS
        resetSequence()
        trainingSession.beginEmergencyConfirm()
        refreshTrainingActiveState()
    }

    private fun executeGuidedOverlayAction(action: GuidedOverlayAction) {
        when (action) {
            GuidedOverlayAction.RepeatLastPhrase -> {
                val phrase = uiLastSpoken.value
                if (phrase.isNotBlank()) speak(phrase)
            }
            GuidedOverlayAction.DecreaseSensitivity -> changeSensitivity(-1)
            GuidedOverlayAction.IncreaseSensitivity -> changeSensitivity(1)
            GuidedOverlayAction.SetSpeedFast -> setResponseSpeed(ResponseSpeed.Fast)
            GuidedOverlayAction.SetSpeedSlow -> setResponseSpeed(ResponseSpeed.Slow)
            GuidedOverlayAction.TogglePauseListening -> toggleListeningPaused()
            GuidedOverlayAction.OpenMenu -> toggleMenuPanel()
            GuidedOverlayAction.ResetSequence -> {
                resetSequence()
                updateReadyOrWaitingState()
            }
            GuidedOverlayAction.ShowHelp -> speakNarration(guidedUiStrings().guidedHelpSpoken)
            GuidedOverlayAction.ShowCurrentResponseTime -> speakNarration(
                guidedUiStrings().guidedCurrentResponseTime(uiSequenceProcessingDelaySec.value)
            )
            GuidedOverlayAction.ShowCurrentSensitivity -> speakNarration(
                guidedUiStrings().guidedCurrentSensitivity(uiSensitivityLevel.value)
            )
            GuidedOverlayAction.OpenAdjustResponseTime,
            GuidedOverlayAction.OpenAdjustSensitivity -> Unit
        }
    }

    private fun executeGuidedPreferenceAction(entry: GuidedVocabularyEntry) {
        when (entry.guidedAction) {
            GuidedOverlayAction.ShowCurrentResponseTime -> speakNarration(
                guidedUiStrings().guidedCurrentResponseTime(uiSequenceProcessingDelaySec.value)
            )
            GuidedOverlayAction.ShowCurrentSensitivity -> speakNarration(
                guidedUiStrings().guidedCurrentSensitivity(uiSensitivityLevel.value)
            )
            else -> entry.guidedAction?.let { executeGuidedOverlayAction(it) }
        }
    }

    private fun handleGuidedOverlaySequence(left: Int, right: Int) {
        if (uiListeningPaused.value && !GuidedModeNavigation.isGlobalNavigationSequence(left, right)) {
            resetSequence()
            updateReadyOrWaitingState()
            return
        }

        val uiStrings = guidedUiStrings()
        val catalogContext = guidedCatalogContext()
        val result = GuidedNavigationController.processSequence(
            left = left,
            right = right,
            state = uiGuidedNavigationState.value,
            language = activeLanguage(),
            uiStrings = uiStrings,
            visibleEntryCap = guidedVisibleEntryCap(),
            catalogContext = catalogContext
        )
        resetSequence()
        applyGuidedSequenceResult(result, left, right, uiStrings)
    }

    private fun applyGuidedSequenceResult(
        result: GuidedSequenceResult,
        left: Int,
        right: Int,
        uiStrings: LisaUiStrings
    ) {
        when (result) {
            is GuidedSequenceResult.Navigate -> {
                uiGuidedNavigationState.value = result.newState
                when {
                    GuidedModeNavigation.isSelectSequence(left, right) &&
                        result.newState.screenMode == GuidedOverlayScreenMode.CategoryMenu -> {
                        uiGuidedConfirmedPhrase.value = uiStrings.guidedChooseCategoryAction
                        uiGuidedConfirmedLeft.value = left
                        uiGuidedConfirmedRight.value = right
                        mainHandler.removeCallbacks(guidedConfirmationClearRunnable)
                        mainHandler.postDelayed(guidedConfirmationClearRunnable, 1500L)
                    }
                    GuidedModeNavigation.isSelectSequence(left, right) &&
                        result.newState.screenMode == GuidedOverlayScreenMode.Vocabulary -> {
                        uiGuidedConfirmedPhrase.value =
                            GuidedVocabularyCatalog.categoryAt(
                                result.newState.categoryIndex,
                                activeLanguage(),
                                uiStrings
                            )?.title
                        uiGuidedConfirmedLeft.value = left
                        uiGuidedConfirmedRight.value = right
                        mainHandler.removeCallbacks(guidedConfirmationClearRunnable)
                        mainHandler.postDelayed(guidedConfirmationClearRunnable, 1500L)
                    }
                    else -> {
                        uiGuidedConfirmedPhrase.value = null
                        uiGuidedConfirmedLeft.value = null
                        uiGuidedConfirmedRight.value = null
                    }
                }
                setCommunicationState(LisaCommunicationState.Listening)
            }
            is GuidedSequenceResult.SystemAction -> {
                result.entry.systemAction?.let { executeQuickControlAction(it) }
                if (result.entry.guidedAction != null) {
                    executeGuidedPreferenceAction(result.entry)
                }
                uiGuidedConfirmedPhrase.value = result.entry.phrase
                uiGuidedConfirmedLeft.value = result.entry.left
                uiGuidedConfirmedRight.value = result.entry.right
                setCommunicationState(LisaCommunicationState.Listening)
                mainHandler.removeCallbacks(guidedConfirmationClearRunnable)
                mainHandler.postDelayed(guidedConfirmationClearRunnable, 2000L)
            }
            is GuidedSequenceResult.Speak -> {
                val phrase = result.entry.phrase
                uiLastSpoken.value = phrase
                uiGuidedConfirmedPhrase.value = phrase
                uiGuidedConfirmedLeft.value = result.entry.left
                uiGuidedConfirmedRight.value = result.entry.right
                setCommunicationState(LisaCommunicationState.Speaking(phrase))
                speak(phrase)
                mainHandler.removeCallbacks(guidedConfirmationClearRunnable)
                mainHandler.postDelayed(guidedConfirmationClearRunnable, 2500L)
            }
            is GuidedSequenceResult.SavePreferencesAdjustment -> {
                uiGuidedNavigationState.value = result.newState
                result.responseTimeSec?.let { setSequenceProcessingDelay(it) }
                result.sensitivityLevel?.let { applySensitivityLevel(it) }
                uiGuidedConfirmedPhrase.value = uiStrings.guidedActionConfirmed
                uiGuidedConfirmedLeft.value = GuidedModeNavigation.SELECT_LEFT
                uiGuidedConfirmedRight.value = GuidedModeNavigation.SELECT_RIGHT
                setCommunicationState(LisaCommunicationState.Listening)
                mainHandler.removeCallbacks(guidedConfirmationClearRunnable)
                mainHandler.postDelayed(guidedConfirmationClearRunnable, 1500L)
            }
            GuidedSequenceResult.Unmatched -> {
                setCommunicationState(LisaCommunicationState.Listening)
            }
        }
    }

    private val guidedConfirmationClearRunnable = Runnable {
        uiGuidedConfirmedPhrase.value = null
        uiGuidedConfirmedLeft.value = null
        uiGuidedConfirmedRight.value = null
        updateReadyOrWaitingState()
    }

    private fun toggleListeningPaused() {
        uiListeningPaused.value = !uiListeningPaused.value
        if (!uiListeningPaused.value) {
            updateReadyOrWaitingState()
        }
    }

    private fun executeQuickControlAction(action: SystemCommandAction) {
        when (action) {
            SystemCommandAction.SetSpeedFast -> setResponseSpeed(ResponseSpeed.Fast)
            SystemCommandAction.SetSpeedNormal -> setResponseSpeed(ResponseSpeed.Normal)
            SystemCommandAction.SetSpeedSlow -> setResponseSpeed(ResponseSpeed.Slow)
            SystemCommandAction.DecreaseSensitivity -> changeSensitivity(-1)
            SystemCommandAction.IncreaseSensitivity -> changeSensitivity(1)
            SystemCommandAction.RepeatLastPhrase -> {
                verifyTrainingNavigation(NavigationAction.RepeatLastPhrase)
                val phrase = uiLastSpoken.value
                if (phrase.isNotBlank()) speak(phrase)
            }
            SystemCommandAction.TogglePauseListening -> toggleListeningPaused()
            SystemCommandAction.OpenPracticeMode -> openPracticeMode()
            SystemCommandAction.CloseQuickControls -> closeQuickControls()
            else -> Unit
        }
    }

    private fun executeGlobalSystemAction(action: SystemCommandAction) {
        when (action) {
            SystemCommandAction.OpenQuickControls -> openQuickControls()
            SystemCommandAction.CloseOverlay -> {
                closeQuickControls()
                closePracticeMode()
            }
            else -> Unit
        }
        setCommunicationState(LisaCommunicationState.Listening)
    }

    private fun handlePracticeSequence(left: Int, right: Int) {
        if (isEmergencySequence(left, right)) {
            closePracticeMode()
            trainingSession.beginEmergencyConfirm()
            refreshTrainingActiveState()
            return
        }
        if (isCloseHelpSequence(left, right)) {
            resetSequence()
            closePracticeMode()
            setCommunicationState(LisaCommunicationState.Listening)
            return
        }
        val item = PracticeModeCatalog.items[uiPracticeItemIndex.value]
        resetSequence()
        when {
            left == item.left && right == item.right -> {
                uiPracticeFeedback.value = PracticeFeedback.Correct
                PracticeMemoryAdapter.onPracticeExerciseCompleted(
                    CompanionMemoryEngines.default,
                    exerciseId = "practice_${uiPracticeItemIndex.value}",
                    successful = true
                )
                mainHandler.postDelayed({
                    if (uiPracticeItemIndex.value < PracticeModeCatalog.items.lastIndex) {
                        uiPracticeItemIndex.value += 1
                        uiPracticeFeedback.value = null
                    }
                }, 1500L)
            }
            kotlin.math.abs(left - item.left) + kotlin.math.abs(right - item.right) <= 2 ->
                uiPracticeFeedback.value = PracticeFeedback.Almost
            else -> uiPracticeFeedback.value = PracticeFeedback.TryAgain
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

    private fun applySensitivityLevel(level: Int, persist: Boolean = true) {
        val clamped = level.coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL)
        val tuning = sensitivitySettingsForLevel(clamped)
        blinkProcessor.tuning = tuning
        closedEyeThreshold = tuning.closedEyeThreshold
        openEyeThreshold = tuning.openEyeThreshold
        requiredWinkFrames = tuning.requiredWinkFrames
        uiSensitivityLevel.value = clamped
        uiSettingsState.value = uiSettingsState.value.copy(sensitivityLevel = clamped)
        if (persist) {
            updateActiveProfile { it.copy(sensitivityLevel = clamped) }
        }
        calibrationReliability.notifySensitivityAdjusted(clamped)
    }

    private fun changeSensitivity(delta: Int) {
        val newLevel = (uiSensitivityLevel.value + delta).coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL)
        if (newLevel == uiSensitivityLevel.value) return
        applySensitivityLevel(newLevel)
        blinkProcessor.resetGestureFlags()
    }

    private fun publishBlinkDiagnostics(
        leftProb: Float?,
        rightProb: Float?,
        result: com.idworx.lisa.features.blinkdetectionreliability.BlinkProcessResult? = null
    ) {
        uiBlinkDiagnostics.value = BlinkDetectionDiagnostics(
            cameraActive = uiCameraPermissionGranted.value,
            eyesDetected = uiEyesDetected.value,
            leftEyeSignal = leftProb?.let { "%.2f".format(it) } ?: "--",
            rightEyeSignal = rightProb?.let { "%.2f".format(it) } ?: "--",
            leftCandidate = result?.leftCandidate ?: blinkProcessor.lastLeftCandidate,
            rightCandidate = result?.rightCandidate ?: blinkProcessor.lastRightCandidate,
            leftStreak = result?.leftStreak ?: uiDevLeftStreak.value,
            rightStreak = result?.rightStreak ?: uiDevRightStreak.value,
            acceptedLeftCount = leftWinks,
            acceptedRightCount = rightWinks,
            skippedForJitter = result?.skippedForJitter == true
        )
    }

    private fun updateDiagnostics(
        leftProb: Float?,
        rightProb: Float?,
        result: com.idworx.lisa.features.blinkdetectionreliability.BlinkProcessResult? = null
    ) {
        uiDiagLeftEye.value = leftProb?.let { "%.2f".format(it) } ?: "--"
        uiDiagRightEye.value = rightProb?.let { "%.2f".format(it) } ?: "--"
        uiDiagLeftCount.value = leftWinks
        uiDiagRightCount.value = rightWinks
        publishBlinkDiagnostics(leftProb, rightProb, result)
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
                    uiEyesDetected.value = false
                    refreshCaregiverSupport()
                    blinkProcessor.clearPreviousProbabilities()
                    updateDiagnostics(null, null)
                    if (trainingSession.state.phase == TrainingPhase.Setup) {
                        trainingSession.onFaceLostDuringSetup()
                    }
                    if (leftWinks == 0 && rightWinks == 0) {
                        setCommunicationState(LisaCommunicationState.WaitingForFace)
                    }
                    return@addOnSuccessListener
                }
                uiFacePresent.value = true
                refreshCaregiverSupport()
                if (trainingSession.state.phase == TrainingPhase.Setup) {
                    trainingSession.onFaceDetectedDuringSetup()
                }
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
                uiEyesDetected.value = false
                refreshCaregiverSupport()
                blinkProcessor.clearPreviousProbabilities()
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
            uiEyesDetected.value = false
            updateDiagnostics(null, null)
            return
        }
        uiEyesDetected.value = true
        val leftProb = eyes.userLeft
        val rightProb = eyes.userRight

        if (countdownActive) {
            handleCountdownWinks(leftProb, rightProb)
            return
        }

        processSequenceWinks(leftProb, rightProb)
    }

    private fun handleCountdownWinks(leftProb: Float, rightProb: Float) {
        val now = System.currentTimeMillis()
        val result = blinkProcessor.processFrame(
            BlinkEyeProbabilities(leftProb, rightProb),
            now,
            acceptedLeftCount = if (countdownLeftHandled) 1 else 0,
            acceptedRightCount = if (countdownRightHandled) 1 else 0
        )
        uiDevLeftStreak.value = result.leftStreak
        uiDevRightStreak.value = result.rightStreak
        updateDiagnostics(leftProb, rightProb, result)

        if (result.acceptLeft && !countdownLeftHandled) {
            countdownLeftHandled = true
            cancelCountdown()
            return
        }

        if (result.acceptRight && !countdownRightHandled) {
            countdownRightHandled = true
            speakPendingPhraseAndFinish()
        }
    }

    private fun processSequenceWinks(leftProb: Float, rightProb: Float) {
        if (countdownActive) return
        if (emergencyActive) return

        val now = System.currentTimeMillis()
        val result = blinkProcessor.processFrame(
            BlinkEyeProbabilities(leftProb, rightProb),
            now,
            acceptedLeftCount = leftWinks,
            acceptedRightCount = rightWinks
        )

        uiDevLeftStreak.value = result.leftStreak
        uiDevRightStreak.value = result.rightStreak
        updateDiagnostics(leftProb, rightProb, result)

        if (result.acceptLeft) {
            if (rejectLessonWrongEyeBlink(isLeft = true)) return
            flashAcceptedBlink(isLeft = true)
            leftWinks += 1
            if (sequenceStartMs == 0L) sequenceStartMs = now
            onWinkCounted(isLeft = true)
            if (isEmergencySequence(leftWinks, rightWinks)) {
                finalizeSequence()
                return
            }
        }

        if (result.acceptRight) {
            if (rejectLessonWrongEyeBlink(isLeft = false)) return
            flashAcceptedBlink(isLeft = false)
            rightWinks += 1
            if (sequenceStartMs == 0L) sequenceStartMs = now
            onWinkCounted(isLeft = false)
            if (isEmergencySequence(leftWinks, rightWinks)) {
                finalizeSequence()
                return
            }
        }

        val hasCountedWinks = leftWinks > 0 || rightWinks > 0
        val activelyWinking = result.leftCandidate || result.rightCandidate
        updateSequencePauseState(leftProb, rightProb)

        if (lastWinkTimeMs == 0L) return

        val idleMs = now - lastWinkTimeMs
        val totalWindowMs = now - sequenceStartMs
        val finalize = hasCountedWinks && !activelyWinking &&
            shouldFinalizeSequence(
                left = leftWinks,
                right = rightWinks,
                idleMs = idleMs,
                sequenceAgeMs = totalWindowMs,
                idleTimeoutMs = sequenceIdleTimeoutMs,
                maxWindowMs = sequenceMaxWindowMs
            )

        if (finalize) {
            if (shouldDeferLessonFinalize()) {
                syncLessonPartialSequenceTimeout()
            } else {
                finalizeSequence()
            }
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
        val finalize = shouldFinalizeSequence(
            left = leftWinks,
            right = rightWinks,
            idleMs = idleMs,
            sequenceAgeMs = totalWindowMs,
            idleTimeoutMs = sequenceIdleTimeoutMs,
            maxWindowMs = sequenceMaxWindowMs
        )
        if (finalize) return

        if (communicationReliability.shouldBlockFinalizationForContinuation(
                leftWinks, rightWinks, mappingsForSequenceContinuation()
            )
        ) {
            if (trainingSession.isNavigationTrainingActive()) {
                setCommunicationState(LisaCommunicationState.WaitingForNextWink)
                return
            }
            val partial = if (guidedOverlayActive()) {
                WorkspacePhraseResolver.visibleEntriesForState(
                    state = uiGuidedNavigationState.value,
                    language = activeLanguage(),
                    uiStrings = guidedUiStrings(),
                    catalogContext = guidedCatalogContext(),
                    visibleEntryCap = guidedVisibleEntryCap()
                ).firstOrNull { it.left == leftWinks && it.right == rightWinks }?.phrase
            } else {
                findPhraseFor(leftWinks, rightWinks)
            }
            if (partial != null) {
                setCommunicationState(LisaCommunicationState.PossibleMatch(partial))
            } else {
                setCommunicationState(LisaCommunicationState.WaitingForNextWink)
            }
            return
        }

        setCommunicationState(LisaCommunicationState.WaitingForNextWink)
        maybeSpeakLongPauseEncouragement(idleMs)
    }

    private fun finalizeSequence() {
        val capturedLeft = leftWinks
        val capturedRight = rightWinks
        val capturedOrder = currentBlinkOrder()

        if (!isSequenceEligibleForSpeech(capturedLeft, capturedRight)) {
            resetSequence()
            updateReadyOrWaitingState()
            return
        }

        setCommunicationState(LisaCommunicationState.ProcessingSequence)

        if (isEmergencySequence(capturedLeft, capturedRight)) {
            val emergencyCtx = reliabilityContext(CommunicationMode.EMERGENCY)
            CommunicationAnalyticsBridge.setObservationContext(emergencyCtx)
            val emergencyReport = communicationReliability.evaluateEmergency(
                emergencyCtx,
                capturedLeft,
                capturedRight
            )
            if (trainingSession.isNavigationTrainingActive()) {
                handleTrainingSequence(capturedLeft, capturedRight)
                setCommunicationState(LisaCommunicationState.Listening)
                return
            }
            if (emergencyReport.finalOutcome == CommunicationReliabilityOutcome.BLOCKED) {
                resetSequence()
                updateReadyOrWaitingState()
                return
            }
            closeQuickControls()
            closePracticeMode()
            if (trainingSession.handleBrain1Interaction(capturedLeft, capturedRight, capturedOrder)) {
                refreshTrainingActiveState()
                resetSequence()
                setCommunicationState(LisaCommunicationState.Listening)
                return
            }
            trainingSession.beginEmergencyConfirm()
            refreshTrainingActiveState()
            resetSequence()
            setCommunicationState(LisaCommunicationState.Listening)
            return
        }

        if (trainingSession.hasActiveBrain1Decision()) {
            handleTrainingSequence(capturedLeft, capturedRight)
            resetSequence()
            setCommunicationState(LisaCommunicationState.Listening)
            return
        }

        if (trainingSession.isNavigationTrainingActive()) {
            handleNavigationTrainingSequence(capturedLeft, capturedRight)
            resetSequence()
            setCommunicationState(LisaCommunicationState.Listening)
            return
        }

        if (trainingSession.shouldShowTraining() &&
            (trainingSession.state.progress.currentPhase == TrainingPhase.CommunicationLesson ||
                trainingSession.state.progress.currentPhase == TrainingPhase.CommunicationMastery)
        ) {
            handleTrainingSequence(capturedLeft, capturedRight)
            resetSequence()
            setCommunicationState(LisaCommunicationState.Listening)
            return
        }

        if (uiPracticeModeOpen.value) {
            handlePracticeSequence(capturedLeft, capturedRight)
            return
        }

        if (uiQuickControlsOpen.value) {
            LisaSystemLanguage.resolveQuickControlCommand(capturedLeft, capturedRight)?.let { action ->
                resetSequence()
                executeQuickControlAction(action)
                setCommunicationState(LisaCommunicationState.Listening)
                return
            }
        }

        if (guidedOverlayActive()) {
            handleGuidedOverlaySequence(capturedLeft, capturedRight)
            return
        }

        LisaSystemLanguage.resolveGlobalCommand(capturedLeft, capturedRight)?.let { action ->
            resetSequence()
            executeGlobalSystemAction(action)
            return
        }

        if (uiListeningPaused.value) {
            resetSequence()
            updateReadyOrWaitingState()
            return
        }

        val ctx = reliabilityContext()
        CommunicationAnalyticsBridge.setObservationContext(ctx)
        val reliabilityReport = communicationReliability.evaluatePhrasePath(
            ctx,
            capturedLeft,
            capturedRight
        )
        resetSequence()

        when (reliabilityReport.attemptResult.action) {
            PhraseReliabilityAction.PROCEED_TO_CONFIRMATION,
            PhraseReliabilityAction.PROCEED_IMMEDIATE -> {
                val phrase = reliabilityReport.matchedPhraseText
                if (phrase != null) {
                    lastReliabilityAttemptId = reliabilityReport.attemptId
                    lastReliabilityPhraseId = reliabilityReport.matchedPhraseId
                    startCountdown(phrase, capturedLeft, capturedRight)
                } else {
                    setCommunicationState(LisaCommunicationState.NoPhraseMatched)
                    mainHandler.removeCallbacks(noPhraseMatchedRunnable)
                    mainHandler.postDelayed(noPhraseMatchedRunnable, NO_PHRASE_MATCHED_DISPLAY_MS)
                }
            }
            PhraseReliabilityAction.NO_PHRASE -> {
                setCommunicationState(LisaCommunicationState.NoPhraseMatched)
                mainHandler.removeCallbacks(noPhraseMatchedRunnable)
                mainHandler.postDelayed(noPhraseMatchedRunnable, NO_PHRASE_MATCHED_DISPLAY_MS)
            }
            PhraseReliabilityAction.BLOCK -> {
                setCommunicationState(LisaCommunicationState.NoPhraseMatched)
                mainHandler.removeCallbacks(noPhraseMatchedRunnable)
                mainHandler.postDelayed(noPhraseMatchedRunnable, NO_PHRASE_MATCHED_DISPLAY_MS)
            }
            else -> updateReadyOrWaitingState()
        }
    }

    private val noPhraseMatchedRunnable = Runnable {
        updateReadyOrWaitingState()
    }

    private fun reliabilityContext(mode: CommunicationMode = CommunicationMode.MAIN): CommunicationReliabilityContext =
        CommunicationReliabilityContext(
            mode = mode,
            mappings = mappingsState.toList(),
            language = activeLanguage(),
            listeningPaused = uiListeningPaused.value,
            navigationTrainingActive = trainingSession.isNavigationTrainingActive(),
            communicationTrainingActive = trainingSession.shouldShowTraining() &&
                trainingSession.state.progress.currentPhase == TrainingPhase.CommunicationLesson,
            practiceMode = uiPracticeModeOpen.value,
            ttsAvailable = tts != null,
            calibrationHealthState = calibrationReliability.currentHealth(),
            calibrationAllowsCommunication = calibrationReliability.allowsCommunication()
        )

    private fun findPhraseFor(l: Int, r: Int): String? =
        findExactMapping(l, r, mappingsState)?.localizedPhrase(activeLanguage())

    private fun resetSequence() {
        cancelLessonPartialSequenceTimeout()
        leftWinks = 0
        rightWinks = 0
        lastWinkTimeMs = 0L
        sequenceStartMs = 0L
        blinkProcessor.resetSequence()
        uiDiagLeftCount.value = 0
        uiDiagRightCount.value = 0
        winkSideOrder.clear()
        presenceTracker = EmotionalPresenceEngine.resetSequencePause(presenceTracker)
        mainHandler.removeCallbacks(sequenceStateRunnable)
        publishBlinkDiagnostics(null, null)
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
