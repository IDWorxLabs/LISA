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
import com.idworx.lisa.features.onboardingguide.navigation.GuidedTrainingFocusPolicy
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceTrainingSpec
import com.idworx.lisa.features.onboardingguide.navigation.NavigationTrainingGestureHandler
import com.idworx.lisa.features.onboardingguide.services.TrainingProgressStore
import com.idworx.lisa.features.onboardingguide.services.TrainingSessionController
import com.idworx.lisa.features.experiencepolish.communicationworkspace.CommunicationWorkspaceEntryHandler
import com.idworx.lisa.features.experiencepolish.emotionalpresence.EmotionalPresenceEngine
import com.idworx.lisa.features.experiencepolish.emotionalpresence.model.PresenceSessionTracker
import com.idworx.lisa.features.silentwelcome.LisaSpeechPolicy
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
    private var sequenceMaxWindowMs = SequenceProcessingDelay.maxWindowMs(SequenceProcessingDelay.DEFAULT_SECONDS)

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
    private val uiTrackingLost = mutableStateOf(false)
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
    private val uiPanelReturnTarget = mutableStateOf<LisaPanel?>(null)
    private val uiSettingsState = mutableStateOf(LisaSettingsUiState())
    private val uiDevLeftStreak = mutableStateOf(0)
    private val uiDevRightStreak = mutableStateOf(0)
    private val uiAcceptedBlinkFlash = mutableStateOf<String?>(null)
    private val uiProfiles = mutableStateListOf<LisaUserProfile>()
    private val uiActiveProfileId = mutableStateOf("")
    private val uiTextSizeScale = mutableStateOf(1.0f)

    private lateinit var profileStore: LisaProfileStore
    private val uiActiveLanguage = mutableStateOf(PreferredLanguage.English)
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
    private val uiPhraseComposerState = mutableStateOf(PhraseComposerController.initialState())

    private lateinit var trainingProgressStore: TrainingProgressStore
    private lateinit var trainingSession: TrainingSessionController
    private var trainingNarration: OnboardingNarrationController? = null
    private val uiGuidedTrainingState = mutableStateOf(GuidedTrainingUiState())

    // phrase mappings
    private val mappingsState = mutableStateListOf<WinkMapping>()
    private val uiCustomMappingsRevision = mutableStateOf(0)
    private val uiPhraseManagementState = mutableStateOf(PhraseManagementUiState())
    private var composeOpenedFromCategoryMenu = false
    /** True when Phrase Management was opened from the Categories menu (Back returns there). */
    private var phraseManagementOpenedFromCategories = false
    /** When set, guided Back from the viewed category restores this Success (or similar) composer state. */
    private var composerReturnAfterCategoryView: PhraseComposerState? = null

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
        mappingsState.addAll(CustomPhraseRepository.loadCustomMappings(applicationContext))
        applyCustomCategoryMigrationIfNeeded()

        profileStore = LisaProfileStore(this)
        val legacySensitivity = loadSensitivityLevel(this)
        val legacyDeveloperMode = loadDeveloperMode(this)
        val profileState = profileStore.load(legacySensitivity, legacyDeveloperMode)
        uiProfiles.clear()
        uiProfiles.addAll(profileState.profiles)
        uiActiveProfileId.value = profileState.activeProfileId
        profileState.activeProfile?.let { applyProfileSettings(it, persist = false) }

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
        trainingSession.onEmergencyConfirmed = {
            startEmergencyMode()
            // No-ops outside the Emergency lesson (verifyNavigation checks the active phase/target
            // itself) — only advances Guided Training once the REAL alarm has actually fired.
            verifyTrainingNavigation(NavigationAction.TriggerEmergency)
        }
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
                    val customMappingsRevision = uiCustomMappingsRevision.value
                    val customPhrases = remember(customMappingsRevision) { customPhrasesForManagement() }
                    val phraseManagementState = uiPhraseManagementState.value
                    val guidedNavState = uiGuidedNavigationState.value
                    val guidedCategoryPage = remember(customMappingsRevision, guidedNavState) {
                        guidedCurrentCategoryPage()
                    }
                    val appVersionInfo = remember { LisaAppVersionInfo.from(this@MainActivity) }
                    val userDisplay = uiCommunicationState.value.toUserDisplay(
                        strings = uiStrings,
                        pendingPhrase = uiPendingPhrase.value,
                        countdown = uiCountdown.value,
                        leftWinkDots = uiDiagLeftCount.value,
                        rightWinkDots = uiDiagRightCount.value,
                        eyeTrackingBanner = eyeTrackingBannerContext()
                    )
                    LisaRootUI(
                        uiStrings = uiStrings,
                        appVersionInfo = appVersionInfo,
                        userDisplay = userDisplay,
                        emergencyActive = uiEmergencyActive.value,
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
                        onMenuClick = { toggleMenuPanel() },
                        onSelectPanel = { panel -> openPanel(panel) },
                        onClosePanel = { closeAllPanels() },
                        onBackToMenu = { backFromActivePanel() },
                        customPhrases = customPhrases,
                        phraseManagementState = phraseManagementState,
                        onSelectCustomPhrase = { identity -> openPhraseManagementDetails(identity) },
                        onPhraseManagementBackToList = {
                            uiPhraseManagementState.value = uiPhraseManagementState.value.copy(
                                screen = PhraseManagementScreen.List,
                                selectedIdentity = null,
                                errorMessage = null,
                                successMessage = null
                            )
                        },
                        onPhraseManagementOpenEdit = {
                            val identity = uiPhraseManagementState.value.selectedIdentity
                            val mapping = customPhrases.firstOrNull {
                                identity != null && CustomPhraseIdentity.from(it) == identity
                            }
                            if (mapping != null) {
                                openComposerForEdit(mapping)
                            }
                        },
                        onPhraseManagementOpenMove = {
                            uiPhraseManagementState.value = uiPhraseManagementState.value.copy(
                                screen = PhraseManagementScreen.Move,
                                moveTargetCategory = null,
                                errorMessage = null,
                                successMessage = null
                            )
                        },
                        onPhraseManagementOpenDelete = {
                            val identity = uiPhraseManagementState.value.selectedIdentity
                            val mapping = customPhrases.firstOrNull {
                                identity != null && CustomPhraseIdentity.from(it) == identity
                            }
                            if (mapping != null) {
                                openComposerForDelete(mapping)
                            }
                        },
                        onPhraseManagementEditTextChange = { text ->
                            uiPhraseManagementState.value = uiPhraseManagementState.value.copy(editText = text)
                        },
                        onPhraseManagementSaveEdit = { savePhraseManagementEdit() },
                        onPhraseManagementSelectMoveCategory = { category ->
                            uiPhraseManagementState.value = uiPhraseManagementState.value.copy(
                                moveTargetCategory = category
                            )
                        },
                        onPhraseManagementConfirmMove = { confirmPhraseManagementMove() },
                        onPhraseManagementConfirmDelete = { confirmPhraseManagementDelete() },
                        onPhraseManagementCancelSubScreen = {
                            uiPhraseManagementState.value = uiPhraseManagementState.value.copy(
                                screen = PhraseManagementScreen.Details,
                                editText = "",
                                moveTargetCategory = null,
                                errorMessage = null
                            )
                        },
                        onPhraseManagementScrollUp = { scrollPhraseManagementList(up = true) },
                        onPhraseManagementScrollDown = { scrollPhraseManagementList(up = false) },
                        onOpenCreatePhrase = { },
                        onOpenPhraseEditor = { openComposeModeFromCustom() },
                        onPreviewCaregiverPhrase = { phrase -> previewCaregiverPhrase(phrase) },
                        onReturnToCommunication = { returnToCommunicationWorkspace() },
                        onOpenDeviceCheck = { openPanel(LisaPanel.TestingChecklist, LisaPanel.Settings) },
                        onOpenDeveloperTools = { openPanel(LisaPanel.DeveloperTools, LisaPanel.Settings) },
                        onDeveloperModeChange = { enabled ->
                            updateActiveProfile { it.copy(developerMode = enabled) }
                        },
                        onSensitivityDecrease = { changeSensitivity(-1) },
                        onSensitivityIncrease = { changeSensitivity(1) },
                        onResponseTimeDecrease = { changeResponseTime(-1) },
                        onResponseTimeIncrease = { changeResponseTime(1) },
                        onSettingsPlaceholderChange = { updated ->
                            updateActiveProfile { it.withUpdatedSettings(updated) }
                        },
                        onSelectProfile = { profileId -> switchToProfile(profileId) },
                        onCreateProfile = { createNewProfile() },
                        onUpdateProfile = { profile -> updateProfile(profile) },
                        onDeleteProfile = { profileId -> deleteProfile(profileId) },
                        onRepeat = {
                            val phrase = uiLastSpoken.value
                            if (phrase.isNotBlank()) speak(phrase)
                        },
                        onReset = { performReset() },
                        onEditCountdown = { editCountdownAndRetry() },
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
                        guidedNavigationState = guidedNavState,
                        guidedCategoryPage = guidedCategoryPage,
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
                        onGuidedPhraseEntry = { entry -> applyGuidedTouchNavigation(entry.left, entry.right) },
                        onGuidedCategoryRow = { index -> openGuidedCategoryFromTouch(index) },
                        phraseComposerState = uiPhraseComposerState.value,
                        phraseComposerActive = uiActivePanel.value == LisaPanel.PhraseEditor,
                        composerEyeFeedback = ComposerEyeFeedback(
                            eyeTrackingBanner = eyeTrackingBannerContext(),
                            leftWinkCount = uiDiagLeftCount.value,
                            rightWinkCount = uiDiagRightCount.value,
                            sensitivityLevel = uiGuidedNavigationState.value.displaySensitivityLevel(
                                uiSensitivityLevel.value
                            ),
                            responseTimeSec = uiGuidedNavigationState.value.displayResponseTimeSec(
                                uiSequenceProcessingDelaySec.value
                            )
                        ),
                        onPhraseComposerEntry = { entry ->
                            applyPhraseComposerTouchNavigation(entry.left, entry.right)
                        },
                        onPhraseComposerCommand = { entry ->
                            applyPhraseComposerTouchNavigation(entry.left, entry.right)
                        },
                        onPhraseComposerKeyTouched = { row, col ->
                            applyPhraseComposerTouchKey(row, col)
                        },
                        onPhraseComposerEmergency = { triggerGuidedEmergencyTouch() },
                        onCancelOrStopEmergency = { cancelOrStopEmergency() },
                        guidedTrainingActive = trainingSession.shouldShowTraining(),
                        guidedTrainingState = uiGuidedTrainingState.value,
                        guidedTrainingSetupStep = uiGuidedTrainingState.value.setupStep,
                        guidedTrainingReturningUser = trainingSession.isReturningUser(),
                        trainingEyeTracking = trainingEyeTrackingState(),
                        trainingBlinkDiagnostics = uiBlinkDiagnostics.value,
                        showBlinkDiagnostics = uiDeveloperMode.value,
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
                        onTrainingDecreaseResponseTime = { changeGuidedResponseTime(-1) },
                        onTrainingIncreaseResponseTime = { changeGuidedResponseTime(1) },
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
                                onFrame = { imageProxy -> processFrame(imageProxy) },
                                cameraErrorMessage = uiStrings.cameraStartupFailed
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
            trainingSession.onPhraseSpeechFinished {
                refreshTrainingActiveState()
                setCommunicationState(LisaCommunicationState.Listening)
            }
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
        // Uses the SAME authoritative settle time as every other sequence-finalization path (see
        // effectiveSequenceIdleTimeoutMs()) instead of a separate hardcoded constant, so raising the
        // Guided Training response-time setting (up to 8s) also gives a partial lesson attempt that
        // much longer before it resets — no duplicated/conflicting timing constant here anymore.
        mainHandler.postDelayed(lessonPartialSequenceTimeoutRunnable, effectiveSequenceIdleTimeoutMs())
    }

    private fun cancelLessonPartialSequenceTimeout() {
        mainHandler.removeCallbacks(lessonPartialSequenceTimeoutRunnable)
    }

    private fun shouldDeferLessonFinalize(): Boolean =
        trainingSession.isPartialSequenceInProgress(leftWinks, rightWinks, currentBlinkOrder())

    /**
     * The idle-time "settle" window used to decide a blink sequence is finished. Guided Mode /
     * Guided Training (any active lesson phase, including real-workspace navigation lessons) uses
     * its own, slower, user-adjustable settle time from [com.idworx.lisa.features.onboardingguide.model.TrainingPreferences]
     * so multi-step lesson gestures like Categories are not cut off mid-sequence. Everyday workspace use
     * outside Guided Mode is unaffected and keeps the general response-speed setting. Every new
     * blink updates [lastWinkTimeMs], so this window naturally restarts on each new input — it is a
     * completion timer, not a fixed timeout.
     */
    private fun effectiveSequenceIdleTimeoutMs(): Long =
        if (trainingSession.shouldShowTraining()) {
            SequenceProcessingDelay.toMillis(trainingSession.state.progress.preferences.guidedResponseTimeSec)
        } else {
            sequenceIdleTimeoutMs
        }

    private fun effectiveSequenceMaxWindowMs(): Long =
        if (trainingSession.shouldShowTraining()) {
            SequenceProcessingDelay.maxWindowMs(trainingSession.state.progress.preferences.guidedResponseTimeSec)
        } else {
            sequenceMaxWindowMs
        }

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

    private fun openPhraseComposer(returnTo: LisaPanel? = null) {
        openComposeModeFromCustom()
        if (returnTo != null) {
            uiPanelReturnTarget.value = returnTo
        }
    }

    /** RC7D.1 — canonical compose entry: keyboard mode immediately, no vocabulary page. */
    private fun openComposeModeFromCustom() {
        composeOpenedFromCategoryMenu = uiGuidedNavigationState.value.screenMode ==
            GuidedOverlayScreenMode.CategoryMenu
        val preferredCategory =
            if (uiGuidedNavigationState.value.screenMode == GuidedOverlayScreenMode.Vocabulary) {
                val guided = GuidedVocabularyCatalog.categoryAt(
                    pageIndex = uiGuidedNavigationState.value.categoryIndex,
                    language = activeLanguage(),
                    uiStrings = guidedUiStrings(),
                    catalogContext = guidedCatalogContext()
                )?.category
                CustomPhraseEngine.selectableCategories.firstOrNull {
                    it.toGuidedCategory() == guided
                }
            } else {
                null
            }
        uiPhraseComposerState.value = PhraseComposerController.keyboardEntryState().let { base ->
            if (preferredCategory == null) base else base.copy(selectedCategory = preferredCategory)
        }
        uiActivePanel.value = LisaPanel.PhraseEditor
        if (uiGuidedNavigationState.value.screenMode == GuidedOverlayScreenMode.CategoryMenu) {
            uiGuidedNavigationState.value = GuidedNavigationController.closeCategoryMenu(
                uiGuidedNavigationState.value
            )
        }
    }

    private fun openComposerForEdit(mapping: WinkMapping) {
        composeOpenedFromCategoryMenu = false
        uiPhraseComposerState.value = PhraseComposerController.editEntryState(mapping)
        uiActivePanel.value = LisaPanel.PhraseEditor
        uiPanelReturnTarget.value = LisaPanel.VocabularyTraining
    }

    private fun openComposerForDelete(mapping: WinkMapping) {
        composeOpenedFromCategoryMenu = false
        uiPhraseComposerState.value = PhraseComposerController.deleteConfirmState(mapping)
        uiActivePanel.value = LisaPanel.PhraseEditor
        uiPanelReturnTarget.value = LisaPanel.VocabularyTraining
    }

    private fun exitComposeMode(
        openDestinationCategory: CustomPhraseEngine.CaregiverPhraseCategory? = null,
        returnToCategoryMenu: Boolean = false,
        destinationPhrasePageIndex: Int = 0,
        returnToPhraseManagement: Boolean = false
    ) {
        val returnTarget = uiPanelReturnTarget.value
        uiPhraseComposerState.value = PhraseComposerController.keyboardEntryState()
        when {
            openDestinationCategory != null -> {
                uiActivePanel.value = LisaPanel.None
                uiPanelReturnTarget.value = null
                uiGuidedNavigationState.value = GuidedNavigationController.openCategoryAtPage(
                    uiGuidedNavigationState.value,
                    openDestinationCategory.toGuidedCategory().ordinal,
                    destinationPhrasePageIndex
                )
            }
            returnToPhraseManagement || returnTarget == LisaPanel.VocabularyTraining -> {
                uiActivePanel.value = LisaPanel.VocabularyTraining
                uiPanelReturnTarget.value = null
                if (uiPhraseManagementState.value.selectedIdentity == null) {
                    uiPhraseManagementState.value = PhraseManagementUiState()
                } else {
                    uiPhraseManagementState.value = uiPhraseManagementState.value.copy(
                        screen = PhraseManagementScreen.Details,
                        editText = "",
                        errorMessage = null
                    )
                }
            }
            returnToCategoryMenu || composeOpenedFromCategoryMenu -> {
                uiActivePanel.value = LisaPanel.None
                uiPanelReturnTarget.value = null
                uiGuidedNavigationState.value = GuidedNavigationController.openCategoryMenu(
                    uiGuidedNavigationState.value
                )
            }
            else -> {
                uiActivePanel.value = LisaPanel.None
                uiPanelReturnTarget.value = null
            }
        }
        composeOpenedFromCategoryMenu = false
    }

    private fun refreshRuntimeCustomMappings() {
        val builtIn = mappingsState.filter { !it.isCustom }
        val storedCustom = CustomPhraseRepository.loadCustomMappings(applicationContext)
        mappingsState.clear()
        mappingsState.addAll(builtIn)
        mappingsState.addAll(storedCustom)
        uiCustomMappingsRevision.value++
    }

    private fun customPhrasesForManagement(): List<WinkMapping> =
        CustomPhraseRepository.listCustomPhrases(mappingsState.toList())
            .sortedWith(compareBy({ it.caregiverCategory?.ordinal ?: 0 }, { it.customPhrase.orEmpty() }))

    private fun applyCustomCategoryMigrationIfNeeded() {
        val migration = CustomPhraseEngine.migrateCustomCategoryMappings(mappingsState.toList())
        if (migration.migratedCount == 0) return
        mappingsState.clear()
        mappingsState.addAll(migration.mappings)
        CustomPhraseRepository.writeCustomMappings(migration.mappings.filter { it.isCustom }, applicationContext)
        uiCustomMappingsRevision.value++
    }

    private fun openPanel(panel: LisaPanel, returnTo: LisaPanel? = null) {
        if (returnTo != null) {
            uiPanelReturnTarget.value = returnTo
        }
        uiActivePanel.value = panel
        when (panel) {
            LisaPanel.Menu -> verifyTrainingNavigation(NavigationAction.OpenMenu)
            LisaPanel.MyCommunication -> verifyTrainingNavigation(NavigationAction.OpenCommunicationHistory)
            LisaPanel.Settings -> verifyTrainingNavigation(NavigationAction.OpenSettings)
            LisaPanel.VocabularyTraining -> resetPhraseManagementState()
            else -> Unit
        }
        if (panel == LisaPanel.Voice || panel == LisaPanel.VoiceDevice) {
            refreshVoiceSettingsState()
        }
    }

    private fun navigateBackFromPanel() {
        val target = uiPanelReturnTarget.value ?: LisaPanel.Menu
        uiPanelReturnTarget.value = null
        openPanel(target)
    }

    private fun closeAllPanels() {
        if (uiActivePanel.value != LisaPanel.None) {
            verifyTrainingNavigation(NavigationAction.CloseMenu)
        }
        uiPanelReturnTarget.value = null
        uiActivePanel.value = LisaPanel.None
        uiPhraseComposerState.value = PhraseComposerController.initialState()
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
        emergencyAlarmController.start(
            leftWinks,
            rightWinks,
            activeProfile()?.emergencyVolume ?: 1.0f,
            speechPhrase = LisaUiStrings.forLanguage(uiActiveLanguage.value).emergencySpeechPhrase
        )
        resetSequence()
    }

    private fun cancelOrStopEmergency() {
        emergencyAlarmController.stop()
        tts?.stop()
        emergencyActive = false
        uiEmergencyActive.value = false
        if (trainingSession.hasActiveBrain1Decision()) {
            trainingSession.clearBrain1Decision()
            refreshTrainingActiveState()
        }
        resetSequence()
        updateReadyOrWaitingState()
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

    private fun eyeTrackingBannerContext(): EyeTrackingBannerContext {
        val calibrationActive = trainingSession.shouldShowTraining() &&
            uiGuidedTrainingState.value.phase == TrainingPhase.Calibration
        return EyeTrackingBannerContext(
            calibrationActive = calibrationActive,
            trackingLost = uiTrackingLost.value,
            faceDetected = uiFacePresent.value,
            eyesDetected = uiEyesDetected.value
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

    /**
     * Best-effort classification of what a gesture would do in the real workspace, used only to
     * decide whether Guided Training should accept it. [NavigationAction.SelectPhrase] is the
     * fallback for any gesture that isn't a recognised global-navigation sequence — in Vocabulary
     * mode that is exactly how a specific phrase entry is picked (each phrase blinks its own code).
     */
    private fun classifyNavigationGesture(left: Int, right: Int): NavigationAction = when {
        isEmergencySequence(left, right) -> NavigationAction.TriggerEmergency
        GuidedModeNavigation.isFinishTrainingSequence(left, right) -> NavigationAction.ResetSequence
        GuidedModeNavigation.isCategoriesSequence(left, right) -> NavigationAction.OpenCategories
        GuidedModeNavigation.isBackSequence(left, right) -> NavigationAction.CloseMenu
        GuidedModeNavigation.isNextSequence(left, right) -> NavigationAction.NextPage
        GuidedModeNavigation.isPreviousSequence(left, right) -> NavigationAction.PreviousPage
        GuidedModeNavigation.isSelectSequence(left, right) -> NavigationAction.SelectCategory
        else -> NavigationAction.SelectPhrase
    }

    /**
     * Guided Training Mode gate — only the current navigation lesson's target gesture is accepted
     * inside the real workspace; every other gesture is ignored so it can't open unrelated
     * categories, speak hidden phrases, or otherwise leak into a lesson it doesn't belong to.
     */
    private fun acceptedByCurrentNavigationLesson(left: Int, right: Int): Boolean {
        val expected = trainingSession.expectedNavigationAction() ?: return true
        val classified = classifyNavigationGesture(left, right)
        if (classified == expected) return true
        // Select (category menu) and a phrase's own code (vocabulary) both surface as "Select" —
        // let both through so the real screenMode decides which one actually applies.
        return (expected == NavigationAction.SelectCategory && classified == NavigationAction.SelectPhrase) ||
            (expected == NavigationAction.SelectPhrase && classified == NavigationAction.SelectCategory)
    }

    /**
     * Second, finer-grained Guided Training gate layered on top of
     * [acceptedByCurrentNavigationLesson]. That coarse gate only guarantees the gesture is the
     * right KIND of action for the active lesson; lessons whose real workspace screen shows more
     * than one candidate row at once (Select Category, Select Phrase) also need this row-level
     * check so the learner can only ever act on the ONE highlighted row — every other lesson has
     * exactly one on-screen target, so this always returns false (never blocks) for them. Never
     * hardcoded to a specific lesson, category, or phrase — driven by
     * [com.idworx.lisa.features.onboardingguide.navigation.GuidedTrainingFocusPolicy] plus
     * whatever the real workspace currently highlights.
     */
    private fun isNavigationLessonOffTargetAttempt(left: Int, right: Int): Boolean {
        val expected = trainingSession.expectedNavigationAction() ?: return false
        val state = uiGuidedNavigationState.value
        return when {
            expected == NavigationAction.SelectCategory &&
                state.screenMode == GuidedOverlayScreenMode.CategoryMenu -> {
                // The real category row displays and accepts its own direct shortcut gesture
                // (GuidedCategoryShortcuts — the exact same lookup the row UI and
                // GuidedNavigationController.processCategoryMenuGesture use), never a separate
                // hardcoded "Select" gesture, so the lesson can never teach one code while the
                // row shows another.
                val targetCategoryIndex = GuidedCategoryShortcuts.categoryIndexForGesture(left, right)
                val isHighlightedCategory =
                    targetCategoryIndex == GuidedWorkspaceTrainingSpec.conversationCategoryIndex
                !GuidedTrainingFocusPolicy.isTargetAllowed(
                    expected, NavigationAction.SelectCategory, isHighlightedCategory
                )
            }
            expected == NavigationAction.SelectPhrase &&
                state.screenMode == GuidedOverlayScreenMode.Vocabulary &&
                !GuidedModeNavigation.isGlobalNavigationSequence(left, right) -> {
                val highlightedEntry = WorkspacePhraseResolver.visibleEntriesForState(
                    state, activeLanguage(), guidedUiStrings(), guidedCatalogContext(), guidedVisibleEntryCap()
                ).firstOrNull()
                val matchesHighlighted = highlightedEntry != null &&
                    highlightedEntry.left == left && highlightedEntry.right == right
                !GuidedTrainingFocusPolicy.isTargetAllowed(
                    expected, NavigationAction.SelectPhrase, matchesHighlighted
                )
            }
            else -> false
        }
    }

    /**
     * Blocks the unrelated action, shows a brief red "wrong sequence" acknowledgement on the
     * floating lesson card, and resets the active blink sequence so the learner can immediately
     * try the highlighted action again — the lesson stays exactly where it was, nothing speaks,
     * and no progress advances.
     */
    private fun rejectNavigationTrainingGesture() {
        trainingSession.applyNavigationWrongGestureFeedback()
        resetSequence()
    }

    private fun handleNavigationTrainingSequence(left: Int, right: Int) {
        if (!acceptedByCurrentNavigationLesson(left, right)) {
            rejectNavigationTrainingGesture()
            return
        }
        if (isNavigationLessonOffTargetAttempt(left, right)) {
            rejectNavigationTrainingGesture()
            return
        }

        when {
            isEmergencySequence(left, right) -> {
                // finalizeSequence() now routes the real Emergency lesson target straight to the
                // real confirm flow before this function is ever reached, so in practice this
                // branch only exists as a defensive fallback for that same real path — never a
                // fake/simulated one, per the "teach the real interface" rule the lesson-focus
                // gate above (acceptedByCurrentNavigationLesson) already enforces for every other
                // lesson's off-target attempts.
                trainingSession.beginEmergencyConfirm()
            }
            GuidedModeNavigation.isFinishTrainingSequence(left, right) -> {
                // The final lesson's real action — identical to tapping Reset, but reachable by
                // gesture alone. performReset() verifies+completes the lesson internally, so no
                // caregiver touch is ever required to leave Guided Training.
                performReset()
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
                    GuidedModeNavigation.isNextSequence(left, right) ->
                        verifyTrainingNavigation(NavigationAction.NextPage)
                    GuidedModeNavigation.isPreviousSequence(left, right) ->
                        verifyTrainingNavigation(NavigationAction.PreviousPage)
                }
            }
            guidedOverlayActive() -> {
                // Any other gesture while the real workspace is visible — e.g. blinking a specific
                // phrase's own code to select and speak it, or a category's own direct shortcut
                // gesture while the Category Menu is open. SelectPhrase is verified from
                // applyGuidedSequenceResult's Speak branch once the phrase is actually spoken.
                val screenModeBeforeHandling = uiGuidedNavigationState.value.screenMode
                val isCategoryShortcutGesture = screenModeBeforeHandling == GuidedOverlayScreenMode.CategoryMenu &&
                    GuidedCategoryShortcuts.categoryIndexForGesture(left, right) != null
                handleGuidedOverlaySequence(left, right)
                if (isCategoryShortcutGesture) {
                    verifyTrainingNavigation(NavigationAction.SelectCategory)
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
        Toast.makeText(this, guidedUiStrings().feedbackSavedConfirmation, Toast.LENGTH_SHORT).show()
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
        sequenceMaxWindowMs = SequenceProcessingDelay.maxWindowMs(sec)
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
            sensitivityLevel = uiSensitivityLevel.value,
            caregiverCustomPhrases = CustomPhraseEngine.toCatalogEntries(mappingsState.filter { it.isCustom })
        )

    private fun saveCaregiverPhrase(
        category: CustomPhraseEngine.CaregiverPhraseCategory,
        rawPhrase: String,
        allocatedSequence: Pair<Int, Int>? = null
    ): PhraseSaveTransactionResult {
        val uiStrings = guidedUiStrings()
        if (allocatedSequence == null) {
            return PhraseSaveTransactionResult.Failed(PhraseSaveFailureReason.NoSequenceAvailable)
        }
        val result = CustomPhraseRepository.createPhrase(
            rawPhrase = rawPhrase,
            category = category,
            allocatedSequence = allocatedSequence,
            existingMappings = mappingsState.toList(),
            language = activeLanguage(),
            uiStrings = uiStrings,
            visibleEntryCap = guidedVisibleEntryCap(),
            context = applicationContext
        )
        if (result is PhraseSaveTransactionResult.Success) {
            refreshRuntimeCustomMappings()
        }
        return result
    }

    private fun updateCaregiverPhrase(
        identity: CustomPhraseIdentity,
        category: CustomPhraseEngine.CaregiverPhraseCategory,
        rawPhrase: String
    ): PhraseManagementResult {
        val textResult = CustomPhraseRepository.updatePhraseText(
            identity = identity,
            rawPhrase = rawPhrase,
            existingMappings = mappingsState.toList(),
            language = activeLanguage(),
            uiStrings = guidedUiStrings(),
            visibleEntryCap = guidedVisibleEntryCap(),
            context = applicationContext
        )
        if (textResult !is PhraseManagementResult.Success) return textResult
        refreshRuntimeCustomMappings()
        val updatedIdentity = CustomPhraseIdentity.from(textResult.mapping)
        if (textResult.mapping.caregiverCategory == category) {
            return textResult
        }
        val moveResult = CustomPhraseRepository.movePhrase(
            identity = updatedIdentity,
            targetCategory = category,
            existingMappings = mappingsState.toList(),
            language = activeLanguage(),
            uiStrings = guidedUiStrings(),
            visibleEntryCap = guidedVisibleEntryCap(),
            context = applicationContext
        )
        if (moveResult is PhraseManagementResult.Success) {
            refreshRuntimeCustomMappings()
        }
        return moveResult
    }

    private fun resetPhraseManagementState() {
        uiPhraseManagementState.value = PhraseManagementUiState()
    }

    private fun openPhraseManagementDetails(identity: CustomPhraseIdentity) {
        uiPhraseManagementState.value = uiPhraseManagementState.value.copy(
            screen = PhraseManagementScreen.Details,
            selectedIdentity = identity,
            errorMessage = null,
            successMessage = null
        )
    }

    private fun scrollPhraseManagementList(up: Boolean) {
        val phrases = customPhrasesForManagement()
        val state = uiPhraseManagementState.value
        if (state.screen != PhraseManagementScreen.List) return
        uiPhraseManagementState.value = if (up) {
            if (!PhraseManagementController.canScrollUp(state.listPageIndex)) return
            PhraseManagementController.scrollUp(state)
        } else {
            if (!PhraseManagementController.canScrollDown(state.listPageIndex, phrases.size)) return
            PhraseManagementController.scrollDown(state, phrases.size)
        }
        setCommunicationState(LisaCommunicationState.Listening)
    }

    private fun handlePhraseManagementSequence(left: Int, right: Int) {
        val state = uiPhraseManagementState.value
        val phrases = customPhrasesForManagement()
        when (state.screen) {
            PhraseManagementScreen.List -> {
                if (GuidedModeNavigation.isBackSequence(left, right)) {
                    exitPhraseManagement(
                        PhraseManagementController.PhraseManagementExitDestination.CommunicationWorkspace
                    )
                    resetSequence()
                    setCommunicationState(LisaCommunicationState.Listening)
                    return
                }
                if (GuidedModeNavigation.isPreviousSequence(left, right)) {
                    scrollPhraseManagementList(up = true)
                    resetSequence()
                    return
                }
                if (GuidedModeNavigation.isNextSequence(left, right)) {
                    scrollPhraseManagementList(up = false)
                    resetSequence()
                    return
                }
                PhraseManagementController.visiblePhraseSelectionSlots(phrases, state.listPageIndex)
                    .firstOrNull { (_, sequence) -> sequence.first == left && sequence.second == right }
                    ?.let { (mapping, _) ->
                        openPhraseManagementDetails(CustomPhraseIdentity.from(mapping))
                        resetSequence()
                        setCommunicationState(LisaCommunicationState.Listening)
                        return
                    }
            }
            PhraseManagementScreen.Details -> {
                if (GuidedModeNavigation.isBackSequence(left, right)) {
                    uiPhraseManagementState.value = state.copy(
                        screen = PhraseManagementScreen.List,
                        selectedIdentity = null,
                        errorMessage = null,
                        successMessage = null
                    )
                    resetSequence()
                    setCommunicationState(LisaCommunicationState.Listening)
                    return
                }
                val detailsAction = PhraseManagementController.detailsActionEntries(guidedUiStrings())
                    .firstOrNull { it.left == left && it.right == right }
                when (detailsAction?.action) {
                    PhraseManagementController.PhraseDetailsAction.Edit -> {
                        val identity = state.selectedIdentity
                        val mapping = phrases.firstOrNull {
                            identity != null && CustomPhraseIdentity.from(it) == identity
                        }
                        if (mapping != null) openComposerForEdit(mapping)
                        resetSequence()
                        setCommunicationState(LisaCommunicationState.Listening)
                        return
                    }
                    PhraseManagementController.PhraseDetailsAction.Move -> {
                        uiPhraseManagementState.value = state.copy(
                            screen = PhraseManagementScreen.Move,
                            moveTargetCategory = null,
                            errorMessage = null,
                            successMessage = null
                        )
                        resetSequence()
                        setCommunicationState(LisaCommunicationState.Listening)
                        return
                    }
                    PhraseManagementController.PhraseDetailsAction.Delete -> {
                        val identity = state.selectedIdentity
                        val mapping = phrases.firstOrNull {
                            identity != null && CustomPhraseIdentity.from(it) == identity
                        }
                        if (mapping != null) openComposerForDelete(mapping)
                        resetSequence()
                        setCommunicationState(LisaCommunicationState.Listening)
                        return
                    }
                    null -> Unit
                }
            }
            PhraseManagementScreen.DeleteConfirm,
            PhraseManagementScreen.Edit,
            PhraseManagementScreen.Move -> {
                if (GuidedModeNavigation.isBackSequence(left, right)) {
                    uiPhraseManagementState.value = state.copy(
                        screen = PhraseManagementScreen.Details,
                        errorMessage = null,
                        successMessage = null
                    )
                    resetSequence()
                    setCommunicationState(LisaCommunicationState.Listening)
                    return
                }
            }
        }
        resetSequence()
        setCommunicationState(LisaCommunicationState.Listening)
    }

    private fun savePhraseManagementEdit() {
        val state = uiPhraseManagementState.value
        val identity = state.selectedIdentity ?: return
        when (
            val result = CustomPhraseRepository.updatePhraseText(
                identity = identity,
                rawPhrase = state.editText,
                existingMappings = mappingsState.toList(),
                language = activeLanguage(),
                uiStrings = guidedUiStrings(),
                visibleEntryCap = guidedVisibleEntryCap(),
                context = applicationContext
            )
        ) {
            is PhraseManagementResult.Success -> {
                refreshRuntimeCustomMappings()
                uiPhraseManagementState.value = state.copy(
                    screen = PhraseManagementScreen.Details,
                    selectedIdentity = CustomPhraseIdentity.from(result.mapping),
                    editText = "",
                    errorMessage = null,
                    successMessage = guidedUiStrings().phraseUpdatedSuccess
                )
            }
            is PhraseManagementResult.Failed -> {
                val message = when (result.reason) {
                    PhraseSaveFailureReason.Duplicate ->
                        result.duplicateMatch?.let { guidedUiStrings().phraseDuplicateExistsMessage(it) }
                            ?: guidedUiStrings().phraseValidationDuplicate
                    PhraseSaveFailureReason.Empty -> guidedUiStrings().phraseValidationEmpty
                    PhraseSaveFailureReason.TooLong -> guidedUiStrings().phraseValidationTooLong
                    else -> guidedUiStrings().phraseUpdateFailed
                }
                uiPhraseManagementState.value = state.copy(errorMessage = message)
            }
        }
    }

    private fun confirmPhraseManagementMove() {
        val state = uiPhraseManagementState.value
        val identity = state.selectedIdentity ?: return
        val target = state.moveTargetCategory ?: return
        when (
            val result = CustomPhraseRepository.movePhrase(
                identity = identity,
                targetCategory = target,
                existingMappings = mappingsState.toList(),
                language = activeLanguage(),
                uiStrings = guidedUiStrings(),
                visibleEntryCap = guidedVisibleEntryCap(),
                context = applicationContext
            )
        ) {
            is PhraseManagementResult.Success -> {
                refreshRuntimeCustomMappings()
                val sequenceLabel = formatWinkSequenceShort(result.mapping.left, result.mapping.right)
                uiPhraseManagementState.value = state.copy(
                    screen = PhraseManagementScreen.Details,
                    selectedIdentity = CustomPhraseIdentity.from(result.mapping),
                    moveTargetCategory = null,
                    errorMessage = null,
                    successMessage = guidedUiStrings().phraseManagementMovedSequence(sequenceLabel)
                )
            }
            is PhraseManagementResult.Failed -> {
                uiPhraseManagementState.value = state.copy(
                    errorMessage = guidedUiStrings().phraseMoveFailed
                )
            }
        }
    }

    private fun confirmPhraseManagementDelete() {
        val state = uiPhraseManagementState.value
        val identity = state.selectedIdentity ?: return
        when (CustomPhraseRepository.deletePhrase(identity, mappingsState.toList(), applicationContext)) {
            is PhraseManagementResult.Success -> {
                refreshRuntimeCustomMappings()
                val remaining = customPhrasesForManagement().size
                uiPhraseManagementState.value = PhraseManagementController.afterPhraseListChanged(
                    state = state.copy(
                        screen = PhraseManagementScreen.List,
                        selectedIdentity = null,
                        errorMessage = null,
                        successMessage = guidedUiStrings().phraseDeletedSuccess
                    ),
                    remainingCount = remaining
                )
            }
            is PhraseManagementResult.Failed -> {
                uiPhraseManagementState.value = state.copy(
                    errorMessage = guidedUiStrings().phraseDeleteFailed
                )
            }
        }
    }

    private fun phraseComposerRuntimeContext(): PhraseComposerRuntimeContext =
        PhraseComposerRuntimeContext(
            customMappings = mappingsState.toList(),
            language = activeLanguage()
        )

    private fun openExistingDuplicatePhrase(match: DuplicatePhraseMatch) {
        composerReturnAfterCategoryView = uiPhraseComposerState.value.copy(
            // Keep duplicate screen restorable if caregiver presses Back from the category.
            mode = PhraseComposerMode.DuplicateWarning,
            duplicateMatch = match
        )
        uiPhraseComposerState.value = PhraseComposerController.keyboardEntryState()
        uiActivePanel.value = LisaPanel.None
        uiPanelReturnTarget.value = null
        composeOpenedFromCategoryMenu = false
        uiGuidedNavigationState.value = GuidedNavigationController.openCategoryAtPage(
            uiGuidedNavigationState.value,
            match.category.toGuidedCategory().ordinal,
            phrasePageIndex = 0
        )
    }

    private fun returnToComposerFromCategoryViewIfNeeded(left: Int, right: Int): Boolean {
        if (!GuidedModeNavigation.isBackSequence(left, right)) return false
        val pending = composerReturnAfterCategoryView ?: return false
        composerReturnAfterCategoryView = null
        uiPhraseComposerState.value = pending
        uiActivePanel.value = LisaPanel.PhraseEditor
        uiPanelReturnTarget.value = null
        setCommunicationState(LisaCommunicationState.Listening)
        return true
    }

    private fun previewCaregiverPhrase(rawPhrase: String) {
        val normalized = CustomPhraseEngine.normalizePhrase(rawPhrase)
        if (normalized.isNotBlank()) speak(normalized)
    }

    private fun returnToCommunicationWorkspace() {
        // Prefer explicit View-in-category when a saved mapping is present; otherwise exit cleanly.
        val composerState = uiPhraseComposerState.value
        val category = composerState.savedMapping?.caregiverCategory
        if (category != null && composerState.mode == PhraseComposerMode.Success) {
            composerReturnAfterCategoryView = composerState
            exitComposeMode(
                openDestinationCategory = category,
                destinationPhrasePageIndex = composerState.savedPhrasePageIndex
            )
        } else {
            exitComposeMode(returnToCategoryMenu = composeOpenedFromCategoryMenu)
        }
    }

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
            quickControlsOpen = uiQuickControlsOpen.value,
            guidedWorkspaceTrainingActive = trainingSession.isNavigationTrainingActive()
        )

    private fun buildGestureContext(): LisaGestureContext {
        val guided = uiGuidedNavigationState.value
        val brain1 = trainingSession.state.brain1Decision
        return LisaGestureContext(
            activePanel = uiActivePanel.value,
            guidedOverlayActive = guidedOverlayActive(),
            guidedScreenMode = guided.screenMode,
            isAdjustingPreference = guided.isPreferencesAdjustmentActive,
            phraseComposerMode = if (uiActivePanel.value == LisaPanel.PhraseEditor) {
                uiPhraseComposerState.value.mode
            } else {
                null
            },
            emergencyModalActive = emergencyAwaitingConfirm(brain1)
        )
    }

    private fun applyGuidedTouchNavigation(left: Int, right: Int) {
        if (trainingSession.isNavigationTrainingActive() &&
            (!acceptedByCurrentNavigationLesson(left, right) || isNavigationLessonOffTargetAttempt(left, right))
        ) {
            rejectNavigationTrainingGesture()
            refreshTrainingActiveState()
            return
        }
        if (GuidedModeNavigation.isCategoriesSequence(left, right)) {
            verifyTrainingNavigation(NavigationAction.OpenCategories)
        }
        handleGuidedOverlaySequence(left, right)
    }

    private fun openGuidedCategoryFromTouch(categoryIndex: Int) {
        when (CategoryAreaDestination.forCategoryIndex(categoryIndex)) {
            CategoryAreaDestination.CreateCustomPhrase -> {
                openComposeModeFromCustom()
                verifyTrainingNavigation(NavigationAction.SelectCategory)
                return
            }
            CategoryAreaDestination.PhraseManagement -> {
                openPhraseManagementFromCategories()
                verifyTrainingNavigation(NavigationAction.SelectCategory)
                return
            }
            is CategoryAreaDestination.CommunicationCategory -> Unit
        }
        if (trainingSession.isNavigationTrainingActive()) {
            val expected = trainingSession.expectedNavigationAction()
            val isHighlightedCategory = categoryIndex == GuidedWorkspaceTrainingSpec.conversationCategoryIndex
            val allowed = expected != null &&
                GuidedTrainingFocusPolicy.isTargetAllowed(expected, NavigationAction.SelectCategory, isHighlightedCategory)
            if (!allowed) {
                rejectNavigationTrainingGesture()
                refreshTrainingActiveState()
                return
            }
        }
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

    private fun openPhraseManagementFromCategories() {
        phraseManagementOpenedFromCategories = true
        // Keep Categories as entry context; List Back exits to Communication Workspace (RC7D.17).
        uiGuidedNavigationState.value = uiGuidedNavigationState.value.copy(
            screenMode = GuidedOverlayScreenMode.CategoryMenu,
            categoryMenuSelection = GuidedVocabularyCategory.PHRASE_MANAGEMENT_INDEX
        )
        uiPanelReturnTarget.value = null
        openPanel(LisaPanel.VocabularyTraining)
    }

    /**
     * RC7D.17 — Phrase Management List Back destination is explicit, not history-pop.
     * Touch and blink both call this handler.
     */
    private fun exitPhraseManagement(
        destination: PhraseManagementController.PhraseManagementExitDestination
    ) {
        when (destination) {
            PhraseManagementController.PhraseManagementExitDestination.CommunicationWorkspace -> {
                phraseManagementOpenedFromCategories = false
                uiPanelReturnTarget.value = null
                uiActivePanel.value = LisaPanel.None
                resetPhraseManagementState()
                // Leave Categories if it was underneath — do not reopen Categories or Phrase Details.
                if (uiGuidedNavigationState.value.screenMode == GuidedOverlayScreenMode.CategoryMenu) {
                    uiGuidedNavigationState.value = GuidedNavigationController.closeCategoryMenu(
                        uiGuidedNavigationState.value
                    )
                }
                setCommunicationState(LisaCommunicationState.Listening)
            }
        }
    }

    private fun backFromActivePanel() {
        if (uiActivePanel.value == LisaPanel.VocabularyTraining) {
            exitPhraseManagement(
                PhraseManagementController.PhraseManagementExitDestination.CommunicationWorkspace
            )
            return
        }
        navigateBackFromPanel()
    }

    /**
     * Emergency is just another Guided Training lesson target, governed by the same
     * [GuidedTrainingFocusPolicy] as Open Categories / Select Category / Select Phrase / Back /
     * Next / Previous — never a separate validator. Defense in depth: this check never relies on
     * the button's dimmed/enabled UI state, so even if a future UI change accidentally leaves the
     * Emergency button tappable outside its lesson, the policy still rejects it here.
     */
    private fun triggerGuidedEmergencyTouch() {
        if (trainingSession.isNavigationTrainingActive()) {
            val expected = trainingSession.expectedNavigationAction()
            val allowed = expected != null &&
                GuidedTrainingFocusPolicy.isTargetAllowed(expected, NavigationAction.TriggerEmergency)
            if (!allowed) {
                rejectNavigationTrainingGesture()
                refreshTrainingActiveState()
                return
            }
            // Emergency lesson — falls through to the exact same real Brain1 confirm/alarm/flash
            // flow used below for the normal workspace, never a simulated one.
        }
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
            GuidedOverlayAction.OpenAdjustSensitivity,
            GuidedOverlayAction.OpenPhraseComposer -> openComposeModeFromCustom()
        }
    }

    private fun applyPhraseComposerTouchNavigation(left: Int, right: Int) {
        handlePhraseComposerSequence(left, right)
    }

    private fun applyPhraseComposerTouchKey(row: Int, col: Int) {
        if (emergencyActive || emergencyAwaitingConfirm(trainingSession.state.brain1Decision)) {
            return
        }
        val uiStrings = guidedUiStrings()
        when (
            val result = PhraseComposerController.processTouchKey(
                row = row,
                col = col,
                state = uiPhraseComposerState.value,
                uiStrings = uiStrings
            )
        ) {
            is PhraseComposerSequenceResult.Navigate -> {
                uiPhraseComposerState.value = result.newState
                setCommunicationState(LisaCommunicationState.Listening)
            }
            else -> Unit
        }
    }

    private fun handlePhraseComposerSequence(left: Int, right: Int) {
        if (emergencyActive || emergencyAwaitingConfirm(trainingSession.state.brain1Decision)) {
            resetSequence()
            return
        }
        val uiStrings = guidedUiStrings()
        when (
            val result = PhraseComposerController.processSequence(
                left = left,
                right = right,
                state = uiPhraseComposerState.value,
                uiStrings = uiStrings,
                runtimeContext = phraseComposerRuntimeContext()
            )
        ) {
            is PhraseComposerSequenceResult.Navigate -> {
                var newState = result.newState
                val category = newState.selectedCategory
                if (newState.mode == PhraseComposerMode.SaveConfirmation &&
                    newState.pendingAllocatedSequence == null &&
                    category != null
                ) {
                    val allocated = CustomPhraseEngine.allocateSequence(
                        category,
                        mappingsState.toList()
                    )
                    newState = newState.copy(pendingAllocatedSequence = allocated)
                }
                uiPhraseComposerState.value = newState
                setCommunicationState(LisaCommunicationState.Listening)
            }
            is PhraseComposerSequenceResult.Preview -> {
                previewCaregiverPhrase(result.phrase)
                uiPhraseComposerState.value = uiPhraseComposerState.value.copy(
                    confirmedLeft = left,
                    confirmedRight = right,
                    errorMessage = null
                )
                setCommunicationState(LisaCommunicationState.Listening)
            }
            is PhraseComposerSequenceResult.Save -> {
                val composerState = uiPhraseComposerState.value
                val saveResult = saveCaregiverPhrase(
                    category = result.category,
                    rawPhrase = result.phrase,
                    allocatedSequence = composerState.pendingAllocatedSequence
                )
                uiPhraseComposerState.value = PhraseComposerController.applyTransactionSaveResult(
                    composerState,
                    saveResult,
                    uiStrings
                )
                setCommunicationState(LisaCommunicationState.Listening)
            }
            is PhraseComposerSequenceResult.Update -> {
                val composerState = uiPhraseComposerState.value
                val updateResult = updateCaregiverPhrase(
                    identity = result.identity,
                    category = result.category,
                    rawPhrase = result.phrase
                )
                uiPhraseComposerState.value = when (updateResult) {
                    is PhraseManagementResult.Success -> composerState.copy(
                        mode = PhraseComposerMode.Success,
                        savedMapping = updateResult.mapping,
                        selectedCategory = updateResult.mapping.caregiverCategory,
                        savedPhrasePageIndex = CustomPhraseRepository.catalogLocationForMapping(
                            mapping = updateResult.mapping,
                            allMappings = mappingsState.toList(),
                            language = activeLanguage(),
                            uiStrings = uiStrings,
                            visibleEntryCap = guidedVisibleEntryCap()
                        ).second,
                        errorMessage = null,
                        pendingAllocatedSequence = null,
                        confirmedLeft = null,
                        confirmedRight = null,
                        wasEdit = true,
                        editingIdentity = CustomPhraseIdentity.from(updateResult.mapping),
                        navigationHistory = if (composerState.mode == PhraseComposerMode.Success) {
                            composerState.navigationHistory
                        } else {
                            composerState.navigationHistory + composerState.mode
                        }
                    )
                    is PhraseManagementResult.Failed -> {
                        val message = when (updateResult.reason) {
                            PhraseSaveFailureReason.Duplicate ->
                                updateResult.duplicateMatch?.let { uiStrings.phraseDuplicateExistsMessage(it) }
                                    ?: uiStrings.phraseAlreadySaved
                            PhraseSaveFailureReason.Empty -> uiStrings.phraseValidationEmpty
                            PhraseSaveFailureReason.TooLong -> uiStrings.phraseValidationTooLong
                            PhraseSaveFailureReason.StorageVerificationFailed ->
                                uiStrings.phraseStorageVerificationFailed
                            else -> uiStrings.phraseUpdateFailed
                        }
                        if (updateResult.reason == PhraseSaveFailureReason.Duplicate &&
                            updateResult.duplicateMatch != null
                        ) {
                            composerState.copy(
                                mode = PhraseComposerMode.DuplicateWarning,
                                duplicateMatch = updateResult.duplicateMatch,
                                errorMessage = null,
                                navigationHistory = if (composerState.mode == PhraseComposerMode.DuplicateWarning) {
                                    composerState.navigationHistory
                                } else {
                                    composerState.navigationHistory + composerState.mode
                                }
                            )
                        } else {
                            composerState.copy(
                                mode = PhraseComposerMode.SaveConfirmation,
                                errorMessage = message
                            )
                        }
                    }
                }
                setCommunicationState(LisaCommunicationState.Listening)
            }
            is PhraseComposerSequenceResult.Delete -> {
                when (
                    CustomPhraseRepository.deletePhrase(
                        result.identity,
                        mappingsState.toList(),
                        applicationContext
                    )
                ) {
                    is PhraseManagementResult.Success -> {
                        refreshRuntimeCustomMappings()
                        val remaining = customPhrasesForManagement().size
                        uiPhraseManagementState.value = PhraseManagementController.afterPhraseListChanged(
                            state = uiPhraseManagementState.value.copy(
                                screen = PhraseManagementScreen.List,
                                selectedIdentity = null,
                                errorMessage = null,
                                successMessage = uiStrings.phraseDeletedSuccess
                            ),
                            remainingCount = remaining
                        )
                        exitComposeMode(returnToPhraseManagement = true)
                    }
                    is PhraseManagementResult.Failed -> {
                        uiPhraseComposerState.value = uiPhraseComposerState.value.copy(
                            errorMessage = uiStrings.phraseDeleteFailed
                        )
                    }
                }
                setCommunicationState(LisaCommunicationState.Listening)
            }
            is PhraseComposerSequenceResult.OpenExistingPhrase -> {
                openExistingDuplicatePhrase(result.match)
                setCommunicationState(LisaCommunicationState.Listening)
            }
            is PhraseComposerSequenceResult.ViewSavedCategory -> {
                composerReturnAfterCategoryView = result.returnComposerState
                exitComposeMode(
                    openDestinationCategory = result.category,
                    destinationPhrasePageIndex = result.phrasePageIndex
                )
                setCommunicationState(LisaCommunicationState.Listening)
            }
            PhraseComposerSequenceResult.ReturnToCommunication -> {
                // Legacy alias: treat as explicit View in category when a saved mapping exists.
                val composerState = uiPhraseComposerState.value
                val category = composerState.savedMapping?.caregiverCategory
                if (category != null) {
                    composerReturnAfterCategoryView = composerState
                    exitComposeMode(
                        openDestinationCategory = category,
                        destinationPhrasePageIndex = composerState.savedPhrasePageIndex
                    )
                } else {
                    exitComposeMode(returnToCategoryMenu = true)
                }
                setCommunicationState(LisaCommunicationState.Listening)
            }
            PhraseComposerSequenceResult.ExitToPreviousPanel -> {
                if (uiPanelReturnTarget.value == LisaPanel.VocabularyTraining) {
                    exitComposeMode(returnToPhraseManagement = true)
                } else {
                    exitComposeMode(returnToCategoryMenu = true)
                }
            }
            PhraseComposerSequenceResult.Unmatched -> {
                if (GuidedModeNavigation.isCategoriesSequence(left, right)) {
                    returnToCommunicationWorkspace()
                    uiGuidedNavigationState.value = GuidedNavigationController.openCategoryMenu(
                        uiGuidedNavigationState.value
                    )
                }
                setCommunicationState(LisaCommunicationState.Listening)
            }
        }
        resetSequence()
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
        if (returnToComposerFromCategoryViewIfNeeded(left, right)) {
            resetSequence()
            return
        }
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
                val destination = CategoryAreaDestination.forCategoryIndex(result.newState.categoryIndex)
                val openingManagementDestination =
                    result.newState.screenMode == GuidedOverlayScreenMode.Vocabulary &&
                        (destination is CategoryAreaDestination.CreateCustomPhrase ||
                            destination is CategoryAreaDestination.PhraseManagement)
                if (openingManagementDestination) {
                    when (destination) {
                        CategoryAreaDestination.CreateCustomPhrase -> openComposeModeFromCustom()
                        CategoryAreaDestination.PhraseManagement -> openPhraseManagementFromCategories()
                        is CategoryAreaDestination.CommunicationCategory -> Unit
                    }
                    uiGuidedConfirmedPhrase.value = null
                    uiGuidedConfirmedLeft.value = null
                    uiGuidedConfirmedRight.value = null
                } else {
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
                verifyTrainingNavigation(NavigationAction.SelectPhrase)
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
            Toast.makeText(this, guidedUiStrings().speechEngineNotReady, Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, guidedUiStrings().voiceInstallerUnavailable, Toast.LENGTH_SHORT).show()
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
        Toast.makeText(this, guidedUiStrings().speechSettingsUnavailable, Toast.LENGTH_SHORT).show()
    }

    private fun activeLanguage(): PreferredLanguage = uiActiveLanguage.value

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

    /**
     * Everyday Communication Workspace response time — the +/- controls next to Sensitivity at
     * the top of the screen. Persists via [applySequenceProcessingDelay] (same path the Guided
     * Vocabulary "Adjust response time" flow already uses), so runtime gesture detection
     * ([effectiveSequenceIdleTimeoutMs]) always reflects whichever value the user last picked.
     */
    private fun changeResponseTime(deltaSeconds: Int) {
        val newSeconds = SequenceProcessingDelay.coerce(uiSequenceProcessingDelaySec.value + deltaSeconds)
        if (newSeconds == uiSequenceProcessingDelaySec.value) return
        applySequenceProcessingDelay(newSeconds)
    }

    /**
     * Adjusts Guided Mode/Training's own response (settle) time — independent of the everyday
     * Communication Workspace's response speed control. Persisted in [com.idworx.lisa.features.onboardingguide.model.TrainingPreferences]
     * so it applies generally to every guided lesson, never a single hardcoded lesson.
     */
    private fun changeGuidedResponseTime(deltaSeconds: Int) {
        trainingSession.updatePreferences {
            it.copy(guidedResponseTimeSec = SequenceProcessingDelay.coerce(it.guidedResponseTimeSec + deltaSeconds))
        }
        refreshTrainingActiveState()
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
                    uiTrackingLost.value = false
                    uiFacePresent.value = false
                    uiEyesDetected.value = false
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
                uiTrackingLost.value = false
                uiFacePresent.value = true
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
                uiTrackingLost.value = true
                uiFacePresent.value = false
                uiEyesDetected.value = false
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
        }

        if (result.acceptRight) {
            if (rejectLessonWrongEyeBlink(isLeft = false)) return
            flashAcceptedBlink(isLeft = false)
            rightWinks += 1
            if (sequenceStartMs == 0L) sequenceStartMs = now
            onWinkCounted(isLeft = false)
        }

        val hasCountedWinks = leftWinks > 0 || rightWinks > 0
        val activelyWinking = result.leftCandidate || result.rightCandidate
        updateSequencePauseState(leftProb, rightProb)

        if (lastWinkTimeMs == 0L) return

        // No early/quick-resolve fast path: every sequence — phrase, category, navigation,
        // confirm, cancel, or Emergency — must wait for the user to stop blinking/winking for the
        // full configured response-time idle window before it is ever processed. This is the sole
        // finalize gate in the app; there is no way to execute a gesture before this fires.
        val idleMs = now - lastWinkTimeMs
        val totalWindowMs = now - sequenceStartMs
        val finalize = hasCountedWinks && !activelyWinking &&
            shouldFinalizeSequence(
                left = leftWinks,
                right = rightWinks,
                idleMs = idleMs,
                sequenceAgeMs = totalWindowMs,
                idleTimeoutMs = effectiveSequenceIdleTimeoutMs(),
                maxWindowMs = effectiveSequenceMaxWindowMs()
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
            idleTimeoutMs = effectiveSequenceIdleTimeoutMs(),
            maxWindowMs = effectiveSequenceMaxWindowMs()
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
            // The Emergency lesson is the one navigation lesson that must trigger the REAL
            // confirm/alarm/flash flow — identical to the normal Communication Workspace — so
            // Guided Learning teaches genuine muscle memory rather than a simulated path. Every
            // OTHER lesson still routes an off-target emergency-shaped gesture through the normal
            // training gate below, which rejects it (acceptedByCurrentNavigationLesson) without
            // ever reaching the real alarm.
            val isEmergencyLessonTarget = trainingSession.isNavigationTrainingActive() &&
                trainingSession.expectedNavigationAction() == NavigationAction.TriggerEmergency
            if (trainingSession.isNavigationTrainingActive() && !isEmergencyLessonTarget) {
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

        // Finish Training / workspace Reset gesture — reachable from anywhere in the real
        // workspace, exactly like the bottom-bar Reset button it mirrors. Checked after Quick
        // Controls and Practice Mode (which already returned above) so it never shadows either
        // overlay's own gestures, and before normal category/phrase dispatch so it always wins.
        if (GuidedModeNavigation.isFinishTrainingSequence(capturedLeft, capturedRight)) {
            performReset()
            return
        }

        when (ModeScopedGestureAuthority.routingTarget(buildGestureContext(), capturedLeft, capturedRight)) {
            GestureRoutingTarget.PhraseComposer -> {
                handlePhraseComposerSequence(capturedLeft, capturedRight)
                return
            }
            GestureRoutingTarget.PhraseManagement -> {
                handlePhraseManagementSequence(capturedLeft, capturedRight)
                return
            }
            GestureRoutingTarget.SettingsPanelBack -> {
                backFromActivePanel()
                resetSequence()
                setCommunicationState(LisaCommunicationState.Listening)
                return
            }
            GestureRoutingTarget.GuidedOverlay -> {
                handleGuidedOverlaySequence(capturedLeft, capturedRight)
                return
            }
            GestureRoutingTarget.SystemCommand -> {
                LisaSystemLanguage.resolveGlobalCommand(capturedLeft, capturedRight)?.let { action ->
                    resetSequence()
                    executeGlobalSystemAction(action)
                    return
                }
            }
            GestureRoutingTarget.Emergency,
            GestureRoutingTarget.FinishTraining,
            GestureRoutingTarget.CommunicationPhrasePath -> Unit
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
    onFrame: (ImageProxy) -> Unit,
    cameraErrorMessage: String
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
                    Toast.makeText(ctx, cameraErrorMessage, Toast.LENGTH_LONG).show()
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

// Format per line: "L,R|phrase|category"
private fun saveCustomMappings(context: Context, custom: List<WinkMapping>) {
    CustomPhraseRepository.writeCustomMappings(custom, context)
}

private fun loadCustomMappings(context: Context): List<WinkMapping> =
    CustomPhraseRepository.loadCustomMappings(context)
