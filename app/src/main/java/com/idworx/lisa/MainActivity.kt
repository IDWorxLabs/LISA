package com.idworx.lisa

import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
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
            // RC8.37 — emergency TTS uses alarm stream + full volume, not normal speech volume.
            speak = { text -> speakEmergencyPhrase(text) },
            stopSpeech = {
                tts?.stop()
                restoreDefaultTtsAudioAttributes()
            }
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
    private val uiSpeechVolumeLevel = mutableStateOf(SpeechVolumeAuthority.DEFAULT_LEVEL)
    private val uiSpeechRateLevel = mutableStateOf(SpeechSpeedAuthority.DEFAULT_LEVEL)
    private val uiPendingPhrase = mutableStateOf<String?>(null)
    private val uiCountdown = mutableStateOf<Int?>(null)
    private val uiDeveloperMode = mutableStateOf(false)
    private val uiActivePanel = mutableStateOf(LisaPanel.None)
    private val uiPanelReturnTarget = mutableStateOf<LisaPanel?>(null)
    /** RC7D.28 — blink selection / paging state while LisaPanel.Menu is open. */
    private val uiMainMenuState = mutableStateOf(MainMenuNavigationState())
    private val uiMainMenuViewportHeightPx = mutableStateOf(0)
    private val uiMainMenuMaxScrollPx = mutableStateOf(0)
    /** RC7D.31 — shared state for all full-screen Main Menu destinations. */
    private val uiMenuDestinationState = mutableStateOf(
        MenuDestinationNavigationState(MainMenuDestination.CommunicationProfile)
    )
    private val uiMenuDestinationViewportHeightPx = mutableStateOf(0)
    private val uiMenuDestinationMaxScrollPx = mutableStateOf(0)
    private val uiMenuFeedbackDraft = mutableStateOf(MenuFeedbackDraft())
    private val uiSettingsRecalibrationState = mutableStateOf(SettingsRecalibrationState())
    private lateinit var settingsRecalibrationController: SettingsRecalibrationController
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
    private lateinit var startupSession: com.idworx.lisa.features.intelligentstartup.StartupSessionController
    private var trainingNarration: OnboardingNarrationController? = null
    private val uiGuidedTrainingState = mutableStateOf(GuidedTrainingUiState())
    private val uiStartupState = mutableStateOf(
        com.idworx.lisa.features.intelligentstartup.model.StartupFlowState()
    )

    // phrase mappings
    private val mappingsState = mutableStateListOf<WinkMapping>()
    private val uiCustomMappingsRevision = mutableStateOf(0)
    private val uiPhraseManagementState = mutableStateOf(PhraseManagementUiState())
    private var composeOpenedFromCategoryMenu = false
    /** True when Phrase Management was opened from the Categories menu (Back returns there). */
    private var phraseManagementOpenedFromCategories = false
    /** RC7D.31 — Main Menu ownership changes only the List-level return destination. */
    private var phraseManagementOpenedFromMainMenu = false
    /** When set, guided Back from the viewed category restores this Success (or similar) composer state. */
    private var composerReturnAfterCategoryView: PhraseComposerState? = null
    /**
     * RC8.17 — one-shot workspace preparation per Medical-journey lesson id so recomposition /
     * wink refresh cannot reset the learner's category selection mid-lesson.
     */
    private var preparedMedicalJourneyLessonId: String? = null
    /** RC8.25 — Lesson 18 accepts phrase Speak only after this lesson's entry prep armed it. */
    private var medicalPhraseLessonArmed: Boolean = false
    /**
     * RC8.28 — snapshot of the user's Sensitivity before Lesson 23 practice.
     * Restored after the lesson completes so guided practice does not permanently overwrite it.
     */
    private var sensitivityLessonOriginalLevel: Int? = null
    /** Practice starting level recorded when Lesson 23 Change phase arms (after optional max clamp). */
    private var sensitivityLessonStartLevel: Int? = null
    /** Draft value after the taught Increase — must match the eventual save. */
    private var sensitivityLessonTargetLevel: Int? = null

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
        if (profileState.preferredLanguageResetToEnglish) {
            Toast.makeText(
                this,
                LisaLanguageAvailabilityAuthority.legacyLanguageResetMessage(
                    LisaUiStrings.forLanguage(PreferredLanguage.English)
                ),
                Toast.LENGTH_LONG
            ).show()
        }

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
            // RC8.14 / RC8.27 — Guided Learning Emergency advances only after Stop Emergency
            // (touch or L1 R1), never automatically when the alarm starts.
        }
        trainingSession.onRecalibrationConfirmed = {
            trainingSession.startRecalibrationFlow()
            refreshTrainingActiveState()
        }
        trainingSession.onNavigationPhaseAdvanced = { resetWorkspace ->
            if (resetWorkspace) {
                // RC8.34 — after Method 1 Well done, close Medical and restore Category Menu for
                // Method 2. Cleared navigation cause / selection come from communicationWorkspaceRoot.
                preparedMedicalJourneyLessonId = null
                closeWorkspacePanelsOnly()
                uiGuidedNavigationState.value =
                    GuidedNavigationController.communicationWorkspaceRoot(GuidedNavigationState())
                preparedMedicalJourneyLessonId =
                    com.idworx.lisa.features.guidedmedicalcategoryjourney
                        .GuidedMedicalCategoryJourneyAuthority.ID_MOVE_TO_MEDICAL
            }
            refreshTrainingActiveState()
        }
        startupSession = com.idworx.lisa.features.intelligentstartup.StartupSessionController(
            loadProfiles = { uiProfiles.toList() },
            loadProfileCalibration = { activeProfile()?.eyeCalibration },
            persistCalibration = { calibration ->
                updateActiveProfile { it.copy(eyeCalibration = calibration) }
                applyProfileEyeCalibration(calibration)
            },
            activateProfile = { profileId -> switchToProfile(profileId) },
            createPrimaryUser = { name, languageLabel, levelLabel ->
                createPrimaryUserFromStartup(name, languageLabel, levelLabel)
            },
            nowMs = { System.currentTimeMillis() },
            onStateChanged = { uiStartupState.value = it },
            onEyeControlActivated = {
                activeProfile()?.eyeCalibration?.let { applyProfileEyeCalibration(it) }
            },
            onStartupComplete = {
                uiStartupState.value = startupSession.state
                refreshTrainingActiveState()
            },
            scheduleReadyHandoff = { delayMs, action ->
                mainHandler.postDelayed({ action() }, delayMs)
            },
            scheduleAutoRetry = { delayMs, action ->
                mainHandler.postDelayed({ action() }, delayMs)
            }
        )
        startupSession.start()
        com.idworx.lisa.features.universalsequenceexecution.UniversalSequenceExecutionDebugValidator
            .runIfDebug(
                isDebugBuild = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            ) { message ->
                android.util.Log.w("LisaSequenceParity", message)
            }
        settingsRecalibrationController = SettingsRecalibrationController(
            persistCalibration = { calibration ->
                updateActiveProfile { it.copy(eyeCalibration = calibration) }
                applyProfileEyeCalibration(calibration)
            },
            nowMs = { System.currentTimeMillis() },
            onStateChanged = { uiSettingsRecalibrationState.value = it },
            onSucceeded = {
                openPanel(LisaPanel.Settings)
            },
            scheduleCompleteHandoff = { delayMs, action ->
                mainHandler.postDelayed({ action() }, delayMs)
            }
        )
        applyColdLaunchSessionState()
        // Prefer stored high-confidence calibration immediately so Welcome is eye-ready after skip/quick path.
        activeProfile()?.eyeCalibration?.let { existing ->
            if (com.idworx.lisa.features.intelligentstartup.authority.EyeCalibrationAuthority
                    .shouldSkipQuickCalibration(existing, System.currentTimeMillis())
            ) {
                applyProfileEyeCalibration(existing)
            }
        }
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
                    val destinationMode =
                        MenuDestinationScreenMode.fromPanel(uiActivePanel.value)
                    val destinationActions = if (destinationMode != null) {
                        menuDestinationActions(uiActivePanel.value, uiStrings)
                    } else {
                        emptyList()
                    }
                    val destinationBinding = destinationMode?.let { mode ->
                        MenuDestinationUiBinding(
                            state = MenuDestinationNavigationController.updateActions(
                                uiMenuDestinationState.value,
                                destinationActions
                            ),
                            actions = destinationActions,
                            capabilities =
                                MenuDestinationNavigationController.capabilities(mode),
                            onCommand = ::handleMenuDestinationCommand,
                            onActivate = ::activateMenuDestinationAction,
                            onKeyboardKey = { row, col ->
                                uiMenuDestinationState.value =
                                    MenuDestinationNavigationController.touchTextKey(
                                        uiMenuDestinationState.value,
                                        row,
                                        col
                                    )
                            },
                            onViewportMetrics = { viewportHeightPx, maxScrollPx, scrollPx ->
                                uiMenuDestinationViewportHeightPx.value = viewportHeightPx
                                uiMenuDestinationMaxScrollPx.value = maxScrollPx
                                uiMenuDestinationState.value =
                                    MenuDestinationNavigationController.syncViewportMetrics(
                                        uiMenuDestinationState.value,
                                        viewportHeightPx,
                                        maxScrollPx,
                                        scrollPx
                                    ).copy(revealSelection = false)
                            }
                        )
                    }
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
                        mainMenuState = uiMainMenuState.value,
                        onMainMenuMoveUp = {
                            applyMainMenuResult(
                                MainMenuSequenceResult.Navigate(
                                    MainMenuController.moveSelectionUp(uiMainMenuState.value)
                                )
                            )
                        },
                        onMainMenuMoveDown = {
                            applyMainMenuResult(
                                MainMenuSequenceResult.Navigate(
                                    MainMenuController.moveSelectionDown(uiMainMenuState.value)
                                )
                            )
                        },
                        onMainMenuPreviousPage = {
                            applyMainMenuResult(
                                MainMenuSequenceResult.Navigate(
                                    MainMenuController.previousPage(
                                        uiMainMenuState.value,
                                        uiMainMenuViewportHeightPx.value,
                                        uiMainMenuMaxScrollPx.value
                                    )
                                )
                            )
                        },
                        onMainMenuNextPage = {
                            applyMainMenuResult(
                                MainMenuSequenceResult.Navigate(
                                    MainMenuController.nextPage(
                                        uiMainMenuState.value,
                                        uiMainMenuViewportHeightPx.value,
                                        uiMainMenuMaxScrollPx.value
                                    )
                                )
                            )
                        },
                        onMainMenuSelect = {
                            val state = uiMainMenuState.value.normalized()
                            applyMainMenuResult(
                                MainMenuSequenceResult.OpenDestination(
                                    state.selectedDestination,
                                    MainMenuController.close(state)
                                )
                            )
                        },
                        onMainMenuSelectDestination = { destination ->
                            applyMainMenuResult(
                                MainMenuSequenceResult.OpenDestination(
                                    destination,
                                    MainMenuController.close(uiMainMenuState.value)
                                )
                            )
                        },
                        onMainMenuViewportMetrics = { viewportHeightPx, maxScrollPx, scrollPx ->
                            uiMainMenuViewportHeightPx.value = viewportHeightPx
                            uiMainMenuMaxScrollPx.value = maxScrollPx
                            uiMainMenuState.value = MainMenuController.syncViewportMetrics(
                                uiMainMenuState.value,
                                viewportHeightPx,
                                maxScrollPx,
                                scrollPx
                            ).copy(revealSelection = false, scrollRequestPx = null)
                        },
                        onMainMenuEmergency = { triggerGuidedEmergencyTouch() },
                        menuDestinationBinding = destinationBinding,
                        feedbackDraft = uiMenuFeedbackDraft.value,
                        onFeedbackDraftChange = { uiMenuFeedbackDraft.value = it },
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
                        speechVolumeLevel = uiGuidedNavigationState.value.displaySpeechVolumeLevel(uiSpeechVolumeLevel.value),
                        speechSpeedLevel = uiGuidedNavigationState.value.displaySpeechSpeedLevel(uiSpeechRateLevel.value),
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
                        onGuidedSelectEnter = {
                            applyGuidedTouchNavigation(
                                GuidedModeNavigation.SELECT_LEFT,
                                GuidedModeNavigation.SELECT_RIGHT
                            )
                        },
                        onGuidedCancelSaveConfirmation = {
                            // RC7D.27 — R1 L1 (right-then-left) cancels save confirmation only.
                            applyGuidedTouchNavigation(
                                GuidedModeNavigation.SELECT_LEFT,
                                GuidedModeNavigation.SELECT_RIGHT,
                                blinkOrder = listOf(false, true)
                            )
                        },
                        onGuidedBack = { applyGuidedTouchNavigation(GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT) },
                        onGuidedNavigateDown = { applyGuidedTouchNavigation(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT) },
                        onGuidedEmergency = { triggerGuidedEmergencyTouch() },
                        onGuidedCategories = { applyGuidedTouchNavigation(GuidedModeNavigation.CATEGORIES_LEFT, GuidedModeNavigation.CATEGORIES_RIGHT) },
                        onGuidedPreviousCategoryPage = {
                            applyGuidedTouchNavigation(
                                GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_LEFT,
                                GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_RIGHT
                            )
                        },
                        onGuidedNextCategoryPage = {
                            applyGuidedTouchNavigation(
                                GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT,
                                GuidedModeNavigation.NEXT_CATEGORY_PAGE_RIGHT
                            )
                        },
                        onGuidedDecreaseValue = { applyGuidedTouchNavigation(GuidedModeNavigation.DECREASE_VALUE_LEFT, GuidedModeNavigation.DECREASE_VALUE_RIGHT) },
                        onGuidedIncreaseValue = { applyGuidedTouchNavigation(GuidedModeNavigation.INCREASE_VALUE_LEFT, GuidedModeNavigation.INCREASE_VALUE_RIGHT) },
                        onGuidedSettingsControl = { kind ->
                            when (kind) {
                                SettingsControlKind.Sensitivity,
                                SettingsControlKind.ResponseTime,
                                SettingsControlKind.SpeechVolume,
                                SettingsControlKind.SpeechSpeed -> {
                                    // RC8.5 — hub card touch opens via the same authority as blink Select
                                    // (openHubSetting). Do not route Sensitivity/Response Time through
                                    // L2 R0 / L0 R2 scroll sequences — those only move selection.
                                    val catalogContext = GuidedCatalogContext(
                                        responseTimeSec = uiSequenceProcessingDelaySec.value,
                                        sensitivityLevel = uiSensitivityLevel.value,
                                        speechVolumeLevel = uiSpeechVolumeLevel.value,
                                        speechSpeedLevel = uiSpeechRateLevel.value,
                                        listeningPaused = uiListeningPaused.value
                                    )
                                    val hubIndex = SettingsAndControlsHubSequences.HUB_SETTING_KINDS
                                        .indexOf(kind)
                                        .coerceAtLeast(0)
                                    val focused = uiGuidedNavigationState.value.copy(
                                        settingsHubSelection = hubIndex
                                    )
                                    uiGuidedNavigationState.value =
                                        PreferenceAdjustmentController.openHubSetting(
                                            focused,
                                            kind,
                                            catalogContext
                                        )
                                    setCommunicationState(LisaCommunicationState.Listening)
                                }
                                else -> {
                                    val (left, right) = when (kind) {
                                        SettingsControlKind.Listening ->
                                            if (uiGuidedNavigationState.value.isListeningControlActive) {
                                                GuidedModeNavigation.SELECT_LEFT to
                                                    GuidedModeNavigation.SELECT_RIGHT
                                            } else {
                                                SettingsAndControlsHubSequences.LISTENING
                                            }
                                        SettingsControlKind.RepeatLastMessage ->
                                            SettingsAndControlsHubSequences.REPEAT_LAST
                                        SettingsControlKind.ResetSequence ->
                                            SettingsAndControlsHubSequences.RESET_SEQUENCE
                                        SettingsControlKind.ShowHelp ->
                                            SettingsAndControlsHubSequences.SHOW_HELP
                                        SettingsControlKind.Sensitivity,
                                        SettingsControlKind.ResponseTime,
                                        SettingsControlKind.SpeechVolume,
                                        SettingsControlKind.SpeechSpeed ->
                                            error("Hub settings must open via openHubSetting")
                                    }
                                    applyGuidedTouchNavigation(left, right)
                                }
                            }
                        },
                        onGuidedPhraseEntry = { entry -> applyGuidedTouchNavigation(entry.left, entry.right) },
                        onGuidedCategoryRow = { index -> openGuidedCategoryFromTouch(index) },
                        onGuidedCategoryViewportPageState = { pageCount, currentPage ->
                            updateGuidedCategoryViewportPageState(pageCount, currentPage)
                        },
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
                        onConfirmEmergency = { confirmArmedEmergencyFromTouch() },
                        hasSavedEyeCalibration = activeProfile()?.eyeCalibration != null,
                        settingsRecalibrationState = uiSettingsRecalibrationState.value,
                        onSettingsRecalibrationRetry = { settingsRecalibrationController.retry() },
                        onSettingsRecalibrationCancel = { cancelSettingsRecalibration() },
                        guidedTrainingActive = trainingSession.shouldShowTraining(),
                        guidedTrainingState = uiGuidedTrainingState.value,
                        guidedTrainingSetupStep = uiGuidedTrainingState.value.setupStep,
                        guidedTrainingReturningUser = trainingSession.isReturningUser(),
                        trainingEyeTracking = trainingEyeTrackingState(),
                        eyeTrackingStatus = eyeTrackingStatusUiState(),
                        trainingBlinkDiagnostics = uiBlinkDiagnostics.value,
                        showBlinkDiagnostics = uiDeveloperMode.value,
                        intelligentStartupActive = startupSession.isActive,
                        intelligentStartupState = uiStartupState.value,
                        onIntelligentStartupCalibrationTimeout = {
                            startupSession.notifyCalibrationTimeoutFailure()
                        },
                        onIntelligentStartupCreateDraftChange = { name, language, level ->
                            startupSession.updateCreatePrimaryDraft(name, language, level)
                        },
                        onIntelligentStartupConfirmCreatePrimary = {
                            startupSession.confirmCreatePrimaryUser()
                        },
                        onIntelligentStartupSelectProfileIndex = { index ->
                            startupSession.setProfileSelectionIndex(index)
                            startupSession.onProfileSelectGesture()
                        },
                        onIntelligentStartupConfirmSelectedProfile = {
                            startupSession.onProfileSelectGesture()
                        },
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
                        onExploreFinishGuidedLearning = {
                            verifyTrainingNavigation(NavigationAction.FinishGuidedLearning)
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
        params.putFloat(
            TextToSpeech.Engine.KEY_PARAM_VOLUME,
            SpeechVolumeAuthority.toTtsVolume(uiSpeechVolumeLevel.value)
        )
        tts?.setSpeechRate(SpeechSpeedAuthority.toSpeechRate(uiSpeechRateLevel.value))
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "LISA_SPEAK")
    }

    /**
     * RC8.37 — emergency announcement path. Uses STREAM_ALARM / USAGE_ALARM at full utterance
     * volume so speech tracks the alarm path instead of media/speech-volume settings.
     * Does not stop or recreate the emergency ExoPlayer.
     */
    private fun speakEmergencyPhrase(text: String) {
        if (!LisaSpeechPolicy.allowsPhraseTranslation()) return
        val engine = tts ?: return
        engine.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        val params = Bundle()
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
        engine.setSpeechRate(SpeechSpeedAuthority.toSpeechRate(uiSpeechRateLevel.value))
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, "LISA_SPEAK")
    }

    private fun restoreDefaultTtsAudioAttributes() {
        tts?.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
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
        val previousPanel = uiActivePanel.value
        if (returnTo != null) {
            uiPanelReturnTarget.value = returnTo
        }
        val enteringSuspendedScope =
            ModeScopedGestureAuthority.suspendsCommunicationPhraseProcessing(panel) &&
                !ModeScopedGestureAuthority.suspendsCommunicationPhraseProcessing(previousPanel)
        uiActivePanel.value = panel
        if (ModeScopedGestureAuthority.suspendsCommunicationPhraseProcessing(panel)) {
            suspendCommunicationPhraseProcessing(resetWinkBuffer = enteringSuspendedScope)
        }
        when (panel) {
            LisaPanel.Menu -> {
                // A fresh Menu layer must never inherit a nested Settings/Voice return target.
                uiPanelReturnTarget.value = null
                uiMainMenuState.value = MainMenuController.open(uiMainMenuState.value)
                verifyTrainingNavigation(NavigationAction.OpenMenu)
            }
            LisaPanel.MyCommunication -> verifyTrainingNavigation(NavigationAction.OpenCommunicationHistory)
            LisaPanel.Settings -> verifyTrainingNavigation(NavigationAction.OpenSettings)
            LisaPanel.Voice,
            LisaPanel.VoiceDevice,
            LisaPanel.VoicePremium,
            LisaPanel.VoiceMyVoice,
            LisaPanel.VoiceFamily -> {
                uiMainMenuState.value = MainMenuController.close()
                if (panel == LisaPanel.Voice) {
                    verifyTrainingNavigation(NavigationAction.OpenVoice)
                }
            }
            LisaPanel.VocabularyTraining -> {
                phraseManagementOpenedFromMainMenu = previousPanel == LisaPanel.Menu
                resetPhraseManagementState()
            }
            else -> {
                if (panel != LisaPanel.None) {
                    uiMainMenuState.value = MainMenuController.close()
                }
            }
        }
        if (panel == LisaPanel.Voice || panel == LisaPanel.VoiceDevice) {
            refreshVoiceSettingsState()
        }
        MenuDestinationProductionUiAuthority.destinationForPanel(panel)?.let { destination ->
            val actions = menuDestinationActions(panel, LisaUiStrings.forLanguage(uiActiveLanguage.value))
            val current = uiMenuDestinationState.value
            uiMenuDestinationState.value = if (
                current.isActive && current.destination == destination
            ) {
                MenuDestinationNavigationController.updateActions(
                    current.copy(
                        panel = panel,
                        interactionStage = if (current.panel != panel) {
                            MenuDestinationInteractionStage.Nested(
                                panel = panel,
                                parentPanel = current.panel
                            )
                        } else {
                            current.interactionStage
                        }
                    ),
                    actions
                )
            } else {
                MenuDestinationNavigationController.open(destination, panel, actions)
            }
        }
    }

    /**
     * Suspend Communication phrase selection / WAITING preview while a layered panel owns input.
     * Preserves category/page navigation state for return; clears only transient phrase feedback.
     */
    private fun suspendCommunicationPhraseProcessing(resetWinkBuffer: Boolean) {
        clearCountdown()
        uiGuidedConfirmedPhrase.value = null
        when (uiCommunicationState.value) {
            is LisaCommunicationState.PossibleMatch,
            LisaCommunicationState.WaitingForNextWink,
            LisaCommunicationState.ProcessingSequence,
            LisaCommunicationState.BuildingMessage,
            is LisaCommunicationState.CountdownConfirm,
            is LisaCommunicationState.Detected,
            LisaCommunicationState.WaitingForConfirmation ->
                setCommunicationState(LisaCommunicationState.Listening)
            else -> Unit
        }
        if (resetWinkBuffer) {
            resetSequence()
        }
    }

    /** Restore a clean Communication gesture buffer after leaving a layered panel. */
    private fun resumeCommunicationPhraseProcessing() {
        clearCountdown()
        uiGuidedConfirmedPhrase.value = null
        resetSequence()
        updateReadyOrWaitingState()
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
        uiMainMenuState.value = MainMenuController.close()
        uiPhraseComposerState.value = PhraseComposerController.initialState()
        uiMenuDestinationState.value =
            MenuDestinationNavigationState(MainMenuDestination.CommunicationProfile)
        // RC8.1 — Main Menu → Communication always lands on Category Selection (not last category).
        uiGuidedNavigationState.value = GuidedNavigationController.communicationWorkspaceRoot(
            uiGuidedNavigationState.value
        )
        resumeCommunicationPhraseProcessing()
    }

    /** RC7D.28 — canonical open for touch Menu button and blink L4 R6. */
    private fun openMainMenu() {
        if (uiActivePanel.value == LisaPanel.Menu) return
        openPanel(LisaPanel.Menu)
    }

    private fun applyMainMenuResult(result: MainMenuSequenceResult) {
        val expected = trainingSession.expectedNavigationAction()
        when (result) {
            is MainMenuSequenceResult.Navigate -> {
                uiMainMenuState.value = result.newState
                // RC8.13 — Move Down until the Explore target destination is selected.
                when (expected) {
                    NavigationAction.MenuSelectVoice -> {
                        if (result.newState.selectedDestination == MainMenuDestination.Voice) {
                            verifyTrainingNavigation(NavigationAction.MenuSelectVoice)
                        }
                    }
                    NavigationAction.MenuSelectSettings -> {
                        if (result.newState.selectedDestination == MainMenuDestination.Settings) {
                            verifyTrainingNavigation(NavigationAction.MenuSelectSettings)
                        }
                    }
                    else -> Unit
                }
            }
            is MainMenuSequenceResult.CloseMenu -> {
                uiMainMenuState.value = result.newState
                closeAllPanels()
            }
            is MainMenuSequenceResult.OpenDestination -> {
                when (expected) {
                    NavigationAction.MenuSelectVoice -> {
                        // No shortcuts during "move until Voice" — selection only.
                        uiMainMenuState.value = MainMenuController.selectDestination(
                            uiMainMenuState.value,
                            MainMenuCatalog.destinations.indexOf(result.destination)
                        )
                        if (result.destination == MainMenuDestination.Voice) {
                            verifyTrainingNavigation(NavigationAction.MenuSelectVoice)
                        }
                    }
                    NavigationAction.MenuSelectSettings -> {
                        uiMainMenuState.value = MainMenuController.selectDestination(
                            uiMainMenuState.value,
                            MainMenuCatalog.destinations.indexOf(result.destination)
                        )
                        if (result.destination == MainMenuDestination.Settings) {
                            verifyTrainingNavigation(NavigationAction.MenuSelectSettings)
                        }
                    }
                    NavigationAction.OpenVoice -> {
                        if (result.destination != MainMenuDestination.Voice) {
                            rejectNavigationTrainingGesture()
                            return
                        }
                        uiMainMenuState.value = result.newState
                        openPanel(result.destination.panel)
                    }
                    NavigationAction.OpenSettings -> {
                        if (result.destination != MainMenuDestination.Settings) {
                            rejectNavigationTrainingGesture()
                            return
                        }
                        uiMainMenuState.value = result.newState
                        openPanel(result.destination.panel)
                    }
                    else -> {
                        uiMainMenuState.value = result.newState
                        openPanel(result.destination.panel)
                    }
                }
            }
            MainMenuSequenceResult.Unmatched -> Unit
        }
    }

    private fun menuDestinationActions(
        panel: LisaPanel = uiActivePanel.value,
        uiStrings: LisaUiStrings = LisaUiStrings.forLanguage(uiActiveLanguage.value)
    ): List<MenuDestinationAction> = when (panel) {
        LisaPanel.MyCommunication -> CommunicationProfileDestinationActionAuthority.actions(
            profiles = uiProfiles,
            activeProfileId = uiActiveProfileId.value,
            uiStrings = uiStrings
        )
        LisaPanel.Voice -> listOf(
            MenuDestinationAction(
                MenuDestinationActionId.VoiceDevice,
                uiStrings.deviceVoiceTitle,
                MenuDestinationActionType.Navigation
            ),
            MenuDestinationAction(
                MenuDestinationActionId.VoicePremium,
                uiStrings.premiumVoicesTitle,
                MenuDestinationActionType.Navigation
            ),
            MenuDestinationAction(
                MenuDestinationActionId.VoiceMyVoice,
                uiStrings.myVoiceTitle,
                MenuDestinationActionType.Navigation,
                isEnabled = false
            ),
            MenuDestinationAction(
                MenuDestinationActionId.VoiceFamily,
                uiStrings.familyVoiceTitle,
                MenuDestinationActionType.Navigation,
                isEnabled = false
            )
        )
        LisaPanel.VoiceDevice -> DeviceVoiceDestinationActionAuthority.actions(
            uiVoiceSettingsState.value,
            uiStrings
        )
        LisaPanel.VoicePremium,
        LisaPanel.VoiceMyVoice,
        LisaPanel.VoiceFamily -> emptyList()
        LisaPanel.AboutLisa -> listOf(
            uiStrings.aboutWhatIsLisaTitle,
            uiStrings.aboutWhoIsLisaForTitle,
            uiStrings.aboutHowLisaWorksTitle,
            uiStrings.aboutPrivacySummaryTitle,
            uiStrings.aboutSafetyTitle,
            uiStrings.aboutVersionTitle,
            uiStrings.aboutCreatorTitle,
            uiStrings.aboutCopyrightTitle
        ).mapIndexed { index, label ->
            MenuDestinationAction(
                MenuDestinationActionId.section("about.$index"),
                label,
                MenuDestinationActionType.ScrollAnchor
            )
        }
        LisaPanel.PrivacyPolicy -> listOf(
            uiStrings.privacyIntroTitle,
            uiStrings.privacyCameraTitle,
            uiStrings.privacyOnDeviceTitle,
            uiStrings.privacyNoSellingTitle,
            uiStrings.privacyYourInfoTitle,
            uiStrings.privacyControlTitle,
            uiStrings.privacyQuestionsTitle
        ).mapIndexed { index, label ->
            MenuDestinationAction(
                MenuDestinationActionId.section("privacy.$index"),
                label,
                MenuDestinationActionType.ScrollAnchor
            )
        }
        LisaPanel.Feedback -> listOf(
            MenuDestinationAction(
                MenuDestinationActionId.FeedbackWorkedWell,
                "What worked well?",
                MenuDestinationActionType.TextField
            ),
            MenuDestinationAction(
                MenuDestinationActionId.FeedbackConfusing,
                "What was confusing?",
                MenuDestinationActionType.TextField
            ),
            MenuDestinationAction(
                MenuDestinationActionId.FeedbackWinks,
                "Did LISA detect your winks correctly?",
                MenuDestinationActionType.TextField
            ),
            MenuDestinationAction(
                MenuDestinationActionId.FeedbackSpeech,
                "Did speech happen at the right time?",
                MenuDestinationActionType.TextField
            ),
            MenuDestinationAction(
                MenuDestinationActionId.FeedbackSave,
                uiStrings.saveFeedback,
                MenuDestinationActionType.Save,
                isEnabled = uiMenuFeedbackDraft.value.hasContent
            )
        )
        LisaPanel.ReleaseNotes -> listOf(
            MenuDestinationAction(
                MenuDestinationActionId.section("release.current"),
                uiStrings.releaseNotesVersionTitle,
                MenuDestinationActionType.ScrollAnchor
            )
        )
        LisaPanel.Settings -> PrimarySettingsAuthority.menuDestinationActions(uiStrings)
        LisaPanel.Recalibration -> {
            val failed = uiSettingsRecalibrationState.value.outcome ==
                SettingsRecalibrationOutcome.Failed
            listOf(
                MenuDestinationAction(
                    MenuDestinationActionId.setting("calibration_retry"),
                    uiStrings.calibrationRetryLabel,
                    MenuDestinationActionType.Button,
                    isEnabled = failed
                )
            )
        }
        LisaPanel.DeveloperTools -> listOf(
            MenuDestinationAction(
                MenuDestinationActionId.setting("developer_mode"),
                uiStrings.developerModeTitle,
                MenuDestinationActionType.Toggle
            )
        )
        LisaPanel.TestingChecklist -> TestingChecklistItem.entries.map { item ->
            MenuDestinationAction(
                MenuDestinationActionId.setting("checklist.${item.key}"),
                item.key,
                MenuDestinationActionType.Toggle
            )
        }
        else -> emptyList()
    }

    private fun activateMenuDestinationAction(actionId: MenuDestinationActionId) {
        val panel = uiActivePanel.value
        val actions = menuDestinationActions(panel)
        val focusable = actions.filter { it.canReceiveFocus }
        val index = focusable.indexOfFirst { it.id == actionId }
        if (index >= 0) {
            uiMenuDestinationState.value = uiMenuDestinationState.value.copy(
                selectedActionId = actionId,
                selectedIndex = index,
                revealSelection = true
            )
        }
        when {
            actionId == MenuDestinationActionId.ProfileActive ->
                activeProfile()?.let { switchToProfile(it.id) }
            actionId == MenuDestinationActionId.ProfileName -> {
                uiMenuDestinationState.value =
                    MenuDestinationNavigationController.beginTextEditing(
                        uiMenuDestinationState.value,
                        actionId,
                        activeProfile()?.name.orEmpty()
                    )
            }
            actionId.value.startsWith("profile.language.") -> {
                val label = actionId.value.removePrefix("profile.language.")
                val language = PreferredLanguage.fromStored(label)
                if (!LisaLanguageAvailabilityAuthority.isSelectableInVersion1(language)) {
                    Toast.makeText(
                        this,
                        LisaLanguageAvailabilityAuthority.version2ActivationMessage(guidedUiStrings()),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    updateActiveProfile {
                        it.copy(preferredLanguage = language)
                    }
                }
            }
            actionId.value.startsWith("profile.level.") -> {
                val label = actionId.value.removePrefix("profile.level.")
                updateActiveProfile {
                    it.withCommunicationLevel(CommunicationLevel.fromStored(label))
                }
            }
            actionId.value.startsWith("profile.saved.") ->
                switchToProfile(actionId.value.removePrefix("profile.saved."))
            actionId == MenuDestinationActionId.ProfileNew -> createNewProfile()
            actionId == MenuDestinationActionId.ProfileDelete ->
                activeProfile()?.let { deleteProfile(it.id) }
            actionId == MenuDestinationActionId.VoiceDevice -> openPanel(LisaPanel.VoiceDevice)
            actionId == MenuDestinationActionId.VoicePremium -> openPanel(LisaPanel.VoicePremium)
            actionId == MenuDestinationActionId.VoiceMyVoice ||
                actionId == MenuDestinationActionId.VoiceFamily -> Unit
            actionId.value.startsWith("voice.installed.") ->
                selectTtsVoice(actionId.value.removePrefix("voice.installed."))
            actionId == MenuDestinationActionId.VoiceTest -> testTtsVoice()
            actionId == MenuDestinationActionId.VoiceInstallData -> installTtsVoiceData()
            actionId == MenuDestinationActionId.VoiceSystemSettings -> openTtsSettings()
            actionId.value.startsWith("feedback.") &&
                actionId != MenuDestinationActionId.FeedbackSave -> {
                uiMenuDestinationState.value =
                    MenuDestinationNavigationController.beginTextEditing(
                        uiMenuDestinationState.value,
                        actionId,
                        uiMenuFeedbackDraft.value.valueFor(actionId),
                        requiresReview = true
                    )
            }
            actionId == MenuDestinationActionId.FeedbackSave &&
                uiMenuFeedbackDraft.value.hasContent -> {
                val draft = uiMenuFeedbackDraft.value
                saveFeedbackEntry(
                    draft.workedWell,
                    draft.confusing,
                    draft.winkDetection,
                    draft.speechTiming
                )
                uiMenuFeedbackDraft.value = MenuFeedbackDraft()
            }
            actionId == MenuDestinationActionId.setting("calibration") ->
                openSettingsRecalibration()
            actionId == MenuDestinationActionId.setting("calibration_retry") ->
                settingsRecalibrationController.retry()
            actionId == MenuDestinationActionId.setting("speech_volume") ->
                openPrimarySettingAdjustment(PrimarySettingsAuthority.ItemId.SpeechVolume)
            actionId == MenuDestinationActionId.setting("speech_speed") ->
                openPrimarySettingAdjustment(PrimarySettingsAuthority.ItemId.SpeechSpeed)
            actionId == MenuDestinationActionId.setting("device_check") ->
                openPanel(LisaPanel.TestingChecklist, LisaPanel.Settings)
            actionId == MenuDestinationActionId.setting("developer_mode") ->
                updateActiveProfile { it.copy(developerMode = !uiDeveloperMode.value) }
            actionId.value.startsWith("settings.checklist.") -> {
                val key = actionId.value.removePrefix("settings.checklist.")
                toggleChecklistItem(key, uiTestingChecklist.value[key] != true)
            }
        }
        uiMenuDestinationState.value =
            MenuDestinationNavigationController.updateActions(
                uiMenuDestinationState.value,
                menuDestinationActions(uiActivePanel.value)
            )
    }

    private fun moveMenuDestinationHorizontal(direction: Int) {
        when (uiMenuDestinationState.value.selectedActionId) {
            MenuDestinationActionId.setting("text_size") ->
                updateActiveProfile {
                    it.withUpdatedSettings(
                        uiSettingsState.value.copy(
                            textSizeScale =
                                (uiSettingsState.value.textSizeScale + direction * 0.1f)
                                    .coerceIn(0.8f, 1.4f)
                        )
                    )
                }
            MenuDestinationActionId.setting("emergency_volume") ->
                updateActiveProfile {
                    it.withUpdatedSettings(
                        uiSettingsState.value.copy(
                            emergencyAlarmVolume =
                                (uiSettingsState.value.emergencyAlarmVolume + direction * 0.1f)
                                    .coerceIn(0.5f, 1f)
                        )
                    )
                }
            else -> Unit
        }
    }

    /** Opens the shared Settings & Controls adjustment screen for a primary Settings launcher. */
    private fun openPrimarySettingAdjustment(itemId: PrimarySettingsAuthority.ItemId) {
        val kind = when (itemId) {
            PrimarySettingsAuthority.ItemId.SpeechVolume -> SettingsControlKind.SpeechVolume
            PrimarySettingsAuthority.ItemId.SpeechSpeed -> SettingsControlKind.SpeechSpeed
            else -> return
        }
        closeAllPanels()
        val hub = PreferenceAdjustmentController.openSettingsMenu(uiGuidedNavigationState.value)
        uiGuidedNavigationState.value = PreferenceAdjustmentController.openHubSetting(
            hub,
            kind,
            GuidedCatalogContext(
                responseTimeSec = uiSequenceProcessingDelaySec.value,
                sensitivityLevel = uiSensitivityLevel.value,
                speechVolumeLevel = uiSpeechVolumeLevel.value,
                speechSpeedLevel = uiSpeechRateLevel.value,
                listeningPaused = uiListeningPaused.value
            )
        )
        setCommunicationState(LisaCommunicationState.Listening)
    }

    private fun openSettingsRecalibration() {
        settingsRecalibrationController.start()
        openPanel(LisaPanel.Recalibration, LisaPanel.Settings)
    }

    private fun cancelSettingsRecalibration() {
        settingsRecalibrationController.cancel()
        openPanel(LisaPanel.Settings)
    }

    private fun confirmMenuDestinationTextEditing() {
        val stage = uiMenuDestinationState.value.interactionStage as?
            MenuDestinationInteractionStage.TextEditing ?: return
        if (stage.actionId == MenuDestinationActionId.ProfileName) {
            if (stage.draftText.isBlank()) return
            updateActiveProfile { it.copy(name = stage.draftText.trim()) }
        } else {
            uiMenuFeedbackDraft.value =
                uiMenuFeedbackDraft.value.withValue(stage.actionId, stage.draftText)
        }
        uiMenuDestinationState.value =
            MenuDestinationNavigationController.confirmTextEditing(
                uiMenuDestinationState.value
            )
    }

    private fun finishMenuDestinationKeyboardEditing() {
        uiMenuDestinationState.value =
            MenuDestinationNavigationController.finishKeyboardEditing(
                uiMenuDestinationState.value
            )
    }

    private fun handleMenuDestinationCommand(command: MenuDestinationPanelCommand) {
        val state = uiMenuDestinationState.value
        val textStage =
            state.interactionStage as? MenuDestinationInteractionStage.TextEditing
        if (textStage != null) {
            val reviewing =
                textStage.fieldEditingStage == FeedbackFieldEditingStage.Review
            when (command) {
                MenuDestinationPanelCommand.MoveUp ->
                    uiMenuDestinationState.value =
                        MenuDestinationNavigationController.moveTextCursor(
                            state,
                            PhraseComposerActionId.MoveUp
                        )
                MenuDestinationPanelCommand.MoveDown ->
                    uiMenuDestinationState.value =
                        MenuDestinationNavigationController.moveTextCursor(
                            state,
                            PhraseComposerActionId.MoveDown
                        )
                MenuDestinationPanelCommand.MoveLeft ->
                    uiMenuDestinationState.value =
                        MenuDestinationNavigationController.moveTextCursor(
                            state,
                            PhraseComposerActionId.MoveLeft
                        )
                MenuDestinationPanelCommand.MoveRight ->
                    uiMenuDestinationState.value =
                        MenuDestinationNavigationController.moveTextCursor(
                            state,
                            PhraseComposerActionId.MoveRight
                        )
                MenuDestinationPanelCommand.Select ->
                    if (reviewing) {
                        confirmMenuDestinationTextEditing()
                    } else {
                        uiMenuDestinationState.value =
                            MenuDestinationNavigationController.selectTextKey(state)
                    }
                MenuDestinationPanelCommand.DoneEditing ->
                    finishMenuDestinationKeyboardEditing()
                MenuDestinationPanelCommand.ContinueEditing ->
                    uiMenuDestinationState.value =
                        MenuDestinationNavigationController.continueKeyboardEditing(state)
                MenuDestinationPanelCommand.Save -> confirmMenuDestinationTextEditing()
                MenuDestinationPanelCommand.Back,
                MenuDestinationPanelCommand.Cancel -> backFromMenuDestination()
                MenuDestinationPanelCommand.Emergency -> triggerGuidedEmergencyTouch()
                MenuDestinationPanelCommand.PreviousPage,
                MenuDestinationPanelCommand.NextPage -> Unit
            }
            return
        }
        val actions = menuDestinationActions()
        when (command) {
            MenuDestinationPanelCommand.MoveUp ->
                uiMenuDestinationState.value =
                    MenuDestinationNavigationController.move(state, actions, -1)
            MenuDestinationPanelCommand.MoveDown ->
                uiMenuDestinationState.value =
                    MenuDestinationNavigationController.move(state, actions, 1)
            MenuDestinationPanelCommand.PreviousPage ->
                uiMenuDestinationState.value =
                    MenuDestinationNavigationController.previousPage(
                        state,
                        uiMenuDestinationViewportHeightPx.value,
                        uiMenuDestinationMaxScrollPx.value
                    )
            MenuDestinationPanelCommand.NextPage ->
                uiMenuDestinationState.value =
                    MenuDestinationNavigationController.nextPage(
                        state,
                        uiMenuDestinationViewportHeightPx.value,
                        uiMenuDestinationMaxScrollPx.value
                    )
            MenuDestinationPanelCommand.MoveLeft -> moveMenuDestinationHorizontal(-1)
            MenuDestinationPanelCommand.MoveRight -> moveMenuDestinationHorizontal(1)
            MenuDestinationPanelCommand.Select ->
                state.selectedActionId?.let(::activateMenuDestinationAction)
            MenuDestinationPanelCommand.DoneEditing,
            MenuDestinationPanelCommand.ContinueEditing -> Unit
            MenuDestinationPanelCommand.Save -> confirmMenuDestinationTextEditing()
            MenuDestinationPanelCommand.Cancel,
            MenuDestinationPanelCommand.Back -> backFromMenuDestination()
            MenuDestinationPanelCommand.Emergency -> triggerGuidedEmergencyTouch()
        }
    }

    private fun backFromMenuDestination() {
        if (uiActivePanel.value == LisaPanel.Recalibration) {
            cancelSettingsRecalibration()
            return
        }
        val state = uiMenuDestinationState.value
        when (val stage = state.interactionStage) {
            is MenuDestinationInteractionStage.TextEditing,
            is MenuDestinationInteractionStage.Confirmation -> {
                uiMenuDestinationState.value =
                    MenuDestinationNavigationController.cancelCurrentStage(state)
            }
            is MenuDestinationInteractionStage.Nested -> {
                uiMenuDestinationState.value = state.copy(
                    panel = stage.parentPanel,
                    interactionStage = MenuDestinationInteractionStage.Browsing
                )
                openPanel(stage.parentPanel)
            }
            MenuDestinationInteractionStage.Browsing -> {
                uiMenuDestinationState.value = state.copy(isActive = false)
                openPanel(LisaPanel.Menu)
                // RC8.13 — Explore Back from Voice/Settings shares production Back (L2 R2).
                verifyTrainingNavigation(NavigationAction.BackFromDestination)
            }
        }
    }

    private fun handleMenuDestinationSequence(left: Int, right: Int) {
        val state = uiMenuDestinationState.value
        val textStage = state.interactionStage as?
            MenuDestinationInteractionStage.TextEditing
        if (textStage != null) {
            val composerSequences = ModeScopedGestureAuthority.phraseComposerCommandSequences
            val reviewing =
                textStage.fieldEditingStage == FeedbackFieldEditingStage.Review
            val direction = when (left to right) {
                composerSequences.getValue(PhraseComposerActionId.MoveUp) ->
                    PhraseComposerActionId.MoveUp
                composerSequences.getValue(PhraseComposerActionId.MoveDown) ->
                    PhraseComposerActionId.MoveDown
                composerSequences.getValue(PhraseComposerActionId.MoveLeft) ->
                    PhraseComposerActionId.MoveLeft
                composerSequences.getValue(PhraseComposerActionId.MoveRight) ->
                    PhraseComposerActionId.MoveRight
                else -> null
            }
            when {
                reviewing && GuidedModeNavigation.isSelectSequence(left, right) ->
                    confirmMenuDestinationTextEditing()
                reviewing && GuidedModeNavigation.isIncreaseValueSequence(left, right) ->
                    uiMenuDestinationState.value =
                        MenuDestinationNavigationController.continueKeyboardEditing(state)
                reviewing && GuidedModeNavigation.isBackSequence(left, right) ->
                    backFromMenuDestination()
                !reviewing && direction != null ->
                    uiMenuDestinationState.value =
                        MenuDestinationNavigationController.moveTextCursor(state, direction)
                !reviewing &&
                    left to right == composerSequences.getValue(PhraseComposerActionId.SelectKey) ->
                    uiMenuDestinationState.value =
                        MenuDestinationNavigationController.selectTextKey(state)
                !reviewing &&
                    left to right == composerSequences.getValue(PhraseComposerActionId.Backspace) ->
                    uiMenuDestinationState.value =
                        MenuDestinationNavigationController.updateTextDraft(
                            state,
                            KeyboardNavigator.backspace(textStage.draftText)
                        )
                !reviewing &&
                    left to right == composerSequences.getValue(PhraseComposerActionId.Save) ->
                    finishMenuDestinationKeyboardEditing()
                GuidedModeNavigation.isBackSequence(left, right) ->
                    backFromMenuDestination()
            }
            resetSequence()
            setCommunicationState(LisaCommunicationState.Listening)
            return
        }
        val mode = MenuDestinationScreenMode.fromPanel(uiActivePanel.value) ?: return
        when (
            val result = MenuDestinationNavigationController.processSequence(
                left = left,
                right = right,
                state = state,
                actions = menuDestinationActions(),
                capabilities = MenuDestinationNavigationController.capabilities(mode),
                viewportHeightPx = uiMenuDestinationViewportHeightPx.value,
                maxScrollPx = uiMenuDestinationMaxScrollPx.value
            )
        ) {
            is MenuDestinationSequenceResult.Navigate ->
                uiMenuDestinationState.value = result.state
            is MenuDestinationSequenceResult.Activate ->
                activateMenuDestinationAction(result.actionId)
            is MenuDestinationSequenceResult.MoveHorizontal ->
                moveMenuDestinationHorizontal(result.direction)
            is MenuDestinationSequenceResult.Back -> backFromMenuDestination()
            MenuDestinationSequenceResult.Unmatched -> Unit
        }
        resetSequence()
        setCommunicationState(LisaCommunicationState.Listening)
    }

    private fun handleMainMenuSequence(left: Int, right: Int) {
        if (GuidedModeNavigation.isOpenMainMenuSequence(left, right) &&
            uiActivePanel.value != LisaPanel.Menu
        ) {
            openMainMenu()
            resetSequence()
            setCommunicationState(LisaCommunicationState.Listening)
            return
        }
        if (uiActivePanel.value != LisaPanel.Menu) return
        val result = MainMenuController.processSequence(
            left = left,
            right = right,
            state = uiMainMenuState.value,
            viewportHeightPx = uiMainMenuViewportHeightPx.value,
            maxScrollPx = uiMainMenuMaxScrollPx.value
        )
        applyMainMenuResult(result)
        resetSequence()
        setCommunicationState(LisaCommunicationState.Listening)
    }

    private fun toggleMenuPanel() {
        if (uiActivePanel.value == LisaPanel.Menu) {
            closeAllPanels()
        } else {
            openMainMenu()
        }
    }

    private fun performReset() {
        verifyTrainingNavigation(NavigationAction.ResetSequence)
        emergencyAlarmController.stop()
        emergencyActive = false
        uiEmergencyActive.value = false
        closeQuickControls()
        closePracticeMode()
        // RC8.1 — Finish Training / Reset lands on Category Selection (Communication root).
        uiGuidedNavigationState.value =
            GuidedNavigationController.communicationWorkspaceRoot(GuidedNavigationState())
        uiGuidedConfirmedPhrase.value = null
        uiGuidedConfirmedLeft.value = null
        uiGuidedConfirmedRight.value = null
        tts?.stop()
        clearCountdown()
        savedSequenceLeft = 0
        savedSequenceRight = 0
        resetSequence()
        // closeAllPanels also re-asserts Category Selection when returning to Communication.
        closeAllPanels()
        setCommunicationState(LisaCommunicationState.Reset)
        mainHandler.postDelayed({ updateReadyOrWaitingState() }, 500L)
    }

    private fun startEmergencyMode() {
        emergencyActive = true
        uiEmergencyActive.value = true
        uiLastSpoken.value = "Emergency"
        setCommunicationState(LisaCommunicationState.EmergencyAlarmActive)
        // RC8.14 — fixed maximum emergency volume; no in-emergency adjustment.
        emergencyAlarmController.start(
            leftWinks,
            rightWinks,
            com.idworx.lisa.features.accessibilityconsistency.metadata.AccessibilityMetadata
                .MAX_EMERGENCY_VOLUME,
            speechPhrase = LisaUiStrings.forLanguage(uiActiveLanguage.value).emergencySpeechPhrase
        )
        resetSequence()
    }

    /**
     * RC8.28 — touch Confirm Emergency on the armed overlay. Same production outcome as blink
     * L1 R1 through Brain1 confirm → [startEmergencyMode].
     */
    private fun confirmArmedEmergencyFromTouch() {
        if (emergencyActive) return
        if (!trainingSession.hasActiveBrain1Decision() &&
            !emergencyAwaitingConfirm(trainingSession.state.brain1Decision)
        ) {
            return
        }
        trainingSession.clearBrain1Decision()
        refreshTrainingActiveState()
        startEmergencyMode()
    }

    private fun cancelOrStopEmergency() {
        val stoppingActiveAlarm = emergencyActive
        val advanceEmergencyLesson = com.idworx.lisa.features.guidedemergencylesson
            .GuidedEmergencyLessonAuthority.mayCompleteAfterStop(
                wasEmergencyActive = stoppingActiveAlarm,
                isEmergencyActiveNow = false
            ) &&
            trainingSession.isNavigationTrainingActive() &&
            trainingSession.expectedNavigationAction() == NavigationAction.TriggerEmergency
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
        // RC8.14 / RC8.26 — learner advances only after successfully stopping the active lesson alarm.
        if (advanceEmergencyLesson) {
            verifyTrainingNavigation(NavigationAction.TriggerEmergency)
        }
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
        val guidedCalibration = trainingSession.shouldShowTraining() &&
            uiGuidedTrainingState.value.phase == TrainingPhase.Calibration
        val startupCalibration = startupSession.isActive &&
            uiStartupState.value.phase ==
            com.idworx.lisa.features.intelligentstartup.model.StartupPhase.QuickCalibration
        return EyeTrackingBannerContext(
            calibrationActive = guidedCalibration || startupCalibration,
            trackingLost = uiTrackingLost.value,
            faceDetected = uiFacePresent.value,
            eyesDetected = uiEyesDetected.value
        )
    }

    private fun eyeTrackingStatusUiState(): com.idworx.lisa.features.eyetrackingstatus.EyeTrackingStatusUiState {
        val feedback = ComposerEyeFeedback(
            eyeTrackingBanner = eyeTrackingBannerContext(),
            leftWinkCount = uiDiagLeftCount.value,
            rightWinkCount = uiDiagRightCount.value,
            sensitivityLevel = uiGuidedNavigationState.value.displaySensitivityLevel(
                uiSensitivityLevel.value
            ),
            responseTimeSec = uiGuidedNavigationState.value.displayResponseTimeSec(
                uiSequenceProcessingDelaySec.value
            )
        )
        return com.idworx.lisa.features.eyetrackingstatus.EyeTrackingStatusUiMapper.fromComposerFeedback(
            uiStrings = LisaUiStrings.forLanguage(uiActiveLanguage.value),
            feedback = feedback,
            cameraActive = uiCameraPermissionGranted.value,
            calibrationInProgress = feedback.eyeTrackingBanner.calibrationActive
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
        uiGuidedNavigationState.value =
            GuidedNavigationController.communicationWorkspaceRoot(GuidedNavigationState())
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
        // RC8.1 — Welcome / Skip to Communication opens Category Selection, not General Conversation.
        uiGuidedNavigationState.value =
            GuidedNavigationController.communicationWorkspaceRoot(uiGuidedNavigationState.value)
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
        prepareMedicalJourneyLessonWorkspaceIfNeeded()
    }

    /**
     * RC8.17 / RC8.18 — establish the real Communication workspace state once when entering each
     * Medical-journey / workspace-Back lesson. Never re-runs for the same lesson id (so mid-lesson
     * progress is kept). Never pre-executes the action the current lesson teaches.
     */
    private fun prepareMedicalJourneyLessonWorkspaceIfNeeded() {
        if (!trainingSession.isNavigationTrainingActive()) {
            preparedMedicalJourneyLessonId = null
            return
        }
        val lessonId = trainingSession.navigationLessonId() ?: return
        if (lessonId == preparedMedicalJourneyLessonId) return
        val authority = com.idworx.lisa.features.guidedmedicalcategoryjourney
            .GuidedMedicalCategoryJourneyAuthority
        val execution = com.idworx.lisa.features.guidedlessonexecutionauthority
            .GuidedLessonExecutionAuthority
        when (lessonId) {
            authority.ID_MOVE_TO_MEDICAL -> {
                // Lesson 16 — Category Menu at Conversation (do not select/open Medical).
                closeWorkspacePanelsOnly()
                uiGuidedNavigationState.value =
                    GuidedNavigationController.communicationWorkspaceRoot(GuidedNavigationState())
                medicalPhraseLessonArmed = false
            }
            authority.ID_OPEN_MEDICAL -> {
                // RC8.25 Lesson 17 — clean Category Menu so L3 R1 is genuinely required.
                // Close Medical left open by Lesson 16; never pre-execute the direct open.
                closeWorkspacePanelsOnly()
                uiGuidedNavigationState.value =
                    GuidedNavigationController.communicationWorkspaceRoot(GuidedNavigationState())
                medicalPhraseLessonArmed = false
            }
            authority.ID_USE_MEDICAL_PHRASE -> {
                // Lesson 18 — Medical must stay open from Lesson 17; never speak the phrase.
                val current = uiGuidedNavigationState.value
                uiGuidedNavigationState.value = if (!authority.isMedicalPhraseWorkspaceOpen(current)) {
                    GuidedNavigationController.openCategoryDirectly(
                        current,
                        authority.medicalCategoryIndex
                    )
                } else {
                    current.copy(phrasePageIndex = 0)
                }
                medicalPhraseLessonArmed = true
            }
            execution.ID_WORKSPACE_BACK -> {
                // Lesson 19 — Medical phrase workspace must remain OPEN; never pre-execute Back.
                val current = uiGuidedNavigationState.value
                if (!execution.isWorkspaceBackStartState(current)) {
                    uiGuidedNavigationState.value = GuidedNavigationController.openCategoryDirectly(
                        current,
                        authority.medicalCategoryIndex
                    )
                }
                medicalPhraseLessonArmed = false
            }
            com.idworx.lisa.features.guidedcategorypagenavigation
                .CategoryPageNavigationAuthority.ID_NEXT_PAGE -> {
                // RC8.26 Lesson 20 — Category Menu on Page 1; never pre-execute Next Page.
                closeWorkspacePanelsOnly()
                val current = uiGuidedNavigationState.value
                val pageCount = current.categoryViewportPageCount.coerceAtLeast(2)
                uiGuidedNavigationState.value =
                    GuidedNavigationController.communicationWorkspaceRoot(current).copy(
                        categoryViewportPage = 0,
                        categoryViewportPageCount = pageCount,
                        categoryNavigationCause = CategoryNavigationCause.MENU_RESTORE
                    )
                medicalPhraseLessonArmed = false
            }
            com.idworx.lisa.features.guidedcategorypagenavigation
                .CategoryPageNavigationAuthority.ID_PREVIOUS_PAGE -> {
                // RC8.26 Lesson 21 — preserve Page 2 from Lesson 20; never pre-execute Previous.
                closeWorkspacePanelsOnly()
                val pageAuthority = com.idworx.lisa.features.guidedcategorypagenavigation
                    .CategoryPageNavigationAuthority
                val current = uiGuidedNavigationState.value
                if (!pageAuthority.isPreviousPageStartState(current)) {
                    val pageCount = current.categoryViewportPageCount.coerceAtLeast(2)
                    uiGuidedNavigationState.value = current.copy(
                        screenMode = GuidedOverlayScreenMode.CategoryMenu,
                        preferencesAdjustMode = GuidedPreferencesAdjustMode.None,
                        phrasePageIndex = 0,
                        categoryViewportPage = 1,
                        categoryViewportPageCount = pageCount,
                        categoryMenuSelection = (GuidedVocabularyCategory.PAGE_COUNT - 1)
                            .coerceAtLeast(0),
                        categoryNavigationCause = CategoryNavigationCause.MENU_RESTORE
                    )
                }
                medicalPhraseLessonArmed = false
            }
            com.idworx.lisa.features.guidedemergencylesson
                .GuidedEmergencyLessonAuthority.ID_EMERGENCY -> {
                // RC8.26 Lesson 22 — Category Menu on Page 1; never arm/confirm/stop Emergency.
                closeWorkspacePanelsOnly()
                val current = uiGuidedNavigationState.value
                uiGuidedNavigationState.value =
                    GuidedNavigationController.communicationWorkspaceRoot(current).copy(
                        categoryViewportPage = 0,
                        categoryViewportPageCount = current.categoryViewportPageCount.coerceAtLeast(1),
                        categoryNavigationCause = CategoryNavigationCause.MENU_RESTORE
                    )
                medicalPhraseLessonArmed = false
                sensitivityLessonOriginalLevel = null
                sensitivityLessonStartLevel = null
            }
            com.idworx.lisa.features.guidedsensitivitylesson
                .GuidedSensitivityLessonAuthority.ID_ADJUST_SENSITIVITY -> {
                // RC8.32 Lesson 23 — Category Menu Page 1; never pre-open Page 2 / Settings /
                // Sensitivity / adjust / Back / completion CTAs.
                closeWorkspacePanelsOnly()
                val authority = com.idworx.lisa.features.guidedsensitivitylesson
                    .GuidedSensitivityLessonAuthority
                val original = uiSensitivityLevel.value
                sensitivityLessonOriginalLevel = original
                val practiceStart = authority.practiceStartingSensitivity(original)
                sensitivityLessonStartLevel = practiceStart
                if (practiceStart != original) {
                    applySensitivityLevel(practiceStart, persist = false)
                }
                val current = uiGuidedNavigationState.value
                val pageCount = current.categoryViewportPageCount.coerceAtLeast(2)
                uiGuidedNavigationState.value =
                    GuidedNavigationController.communicationWorkspaceRoot(current).copy(
                        categoryViewportPage = 0,
                        categoryViewportPageCount = pageCount,
                        categoryNavigationCause = CategoryNavigationCause.MENU_RESTORE
                    )
                medicalPhraseLessonArmed = false
            }
            else -> Unit
        }
        preparedMedicalJourneyLessonId = lessonId
    }

    /**
     * Close Menu / Voice / Settings panels without resetting the Communication workspace
     * navigation state (RC8.18 — CloseMenu must not pre-execute workspace Back).
     */
    private fun closeWorkspacePanelsOnly() {
        uiPanelReturnTarget.value = null
        uiActivePanel.value = LisaPanel.None
        uiMainMenuState.value = MainMenuController.close()
        uiPhraseComposerState.value = PhraseComposerController.initialState()
        uiMenuDestinationState.value =
            MenuDestinationNavigationState(MainMenuDestination.CommunicationProfile)
        resumeCommunicationPhraseProcessing()
    }

    private fun isMedicalPhraseWorkspaceOpen(): Boolean =
        com.idworx.lisa.features.guidedmedicalcategoryjourney.GuidedMedicalCategoryJourneyAuthority
            .isMedicalPhraseWorkspaceOpen(uiGuidedNavigationState.value)

    private fun handleTrainingEvent(event: TrainingEvent) {
        if (event is TrainingEvent.StartUsingLisa || event is TrainingEvent.ReplayTutorial) {
            restoreSensitivityLessonPreferenceIfNeeded()
        }
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
        if (trainingSession.handleSetupReadinessInteraction(left, right, order)) {
            refreshTrainingActiveState()
            return
        }
        if (trainingSession.state.progress.currentPhase == TrainingPhase.Completion &&
            trainingSession.state.awaitingCompletionChoice
        ) {
            handleTrainingCompletionChoice(left, right)
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

    /** RC8.32 — Training Complete: Start Communicating (L0 R3) or Restart Guided Learning (L3 R0). */
    private fun handleTrainingCompletionChoice(left: Int, right: Int) {
        val authority = com.idworx.lisa.features.guidedsensitivitylesson.GuidedSensitivityLessonAuthority
        when {
            authority.matchesStartCommunicating(left, right) -> {
                handleTrainingEvent(TrainingEvent.StartUsingLisa)
            }
            authority.matchesRestartGuidedLearning(left, right) -> {
                closeAllPanels()
                handleTrainingEvent(TrainingEvent.ReplayTutorial)
            }
            else -> Unit
        }
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
        GuidedModeNavigation.isOpenMainMenuSequence(left, right) -> NavigationAction.OpenMenu
        GuidedModeNavigation.isCategoriesSequence(left, right) -> NavigationAction.OpenCategories
        GuidedModeNavigation.isBackSequence(left, right) -> NavigationAction.CloseMenu
        // RC8.26 — viewport page jumps classify as Next/Previous Page before item Move Down/Up.
        GuidedModeNavigation.isNextCategoryPageSequence(left, right) -> NavigationAction.NextPage
        GuidedModeNavigation.isPreviousCategoryPageSequence(left, right) -> NavigationAction.PreviousPage
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
        // RC8.26 — Lessons 20–21 accept only production viewport page jumps (L0 R4 / L4 R0),
        // never Move Down/Up (L0 R2 / L2 R0) which still classify as NextPage/PreviousPage names.
        when (expected) {
            NavigationAction.NextPage ->
                return com.idworx.lisa.features.guidedcategorypagenavigation
                    .CategoryPageNavigationAuthority.matchesNextPage(left, right)
            NavigationAction.PreviousPage ->
                return com.idworx.lisa.features.guidedcategorypagenavigation
                    .CategoryPageNavigationAuthority.matchesPreviousPage(left, right)
            NavigationAction.AdjustSensitivity ->
                return acceptsAdjustSensitivityPhaseGesture(left, right)
            else -> Unit
        }
        val classified = classifyNavigationGesture(left, right)
        if (classified == expected) return true
        // Select (category menu) and a phrase's own code (vocabulary) both surface as "Select" —
        // let both through so the real screenMode decides which one actually applies.
        if ((expected == NavigationAction.SelectCategory && classified == NavigationAction.SelectPhrase) ||
            (expected == NavigationAction.SelectPhrase && classified == NavigationAction.SelectCategory)
        ) {
            return true
        }
        // RC8.13 / RC8.14 Explore LISA — same production sequences, aliased to Explore actions.
        // RC8.15 / RC8.23 — Move to Medical phases use Next (scroll) or Medical shortcut (jump).
        return when (expected) {
            NavigationAction.MenuSelectVoice,
            NavigationAction.MenuSelectSettings ->
                GuidedModeNavigation.isNextSequence(left, right)
            NavigationAction.MoveToMedicalCategory ->
                acceptsMoveToMedicalPhaseGesture(left, right, classified)
            NavigationAction.OpenVoice ->
                com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.matchesVoiceDestination(left, right)
            NavigationAction.OpenSettings ->
                com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.matchesSettingsDestination(left, right)
            NavigationAction.FinishGuidedLearning -> classified == NavigationAction.SelectCategory
            NavigationAction.BackFromDestination -> classified == NavigationAction.CloseMenu
            else -> false
        }
    }

    /** RC8.23 / RC8.24 — Lesson 16 phase-aware gesture acceptance (production sequences only). */
    private fun acceptsMoveToMedicalPhaseGesture(
        left: Int,
        right: Int,
        classified: NavigationAction
    ): Boolean {
        val phase = trainingSession.activeTeachingPhase()
            ?: return classified == NavigationAction.NextPage
        val authority = com.idworx.lisa.features.guidedmedicalcategoryjourney
            .GuidedMedicalCategoryJourneyAuthority
        return when (phase.requiredAction) {
            com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
                .MoveDownUntilCategorySelected ->
                GuidedModeNavigation.isNextSequence(left, right)
            com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
                .OpenSelectedCategory ->
                authority.matchesOpenSelected(left, right)
            com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
                .CategoryShortcutJump ->
                // RC8.34 — Lesson 16 Method 2 (also Lesson 17's sole phase).
                authority.matchesOpenMedical(left, right)
            // RC8.28 / RC8.32 Sensitivity phases belong to AdjustSensitivity, not Lesson 16.
            com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
                .MoveToSettingsPage,
            com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
                .OpenSettingsAndControls,
            com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
                .OpenSensitivitySetting,
            com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
                .AdjustSensitivity,
            com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
                .ReturnToSettingsAndControls,
            com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
                .IncreaseSensitivityOnce,
            com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
                .SaveSensitivity ->
                false
        }
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
                    targetCategoryIndex == GuidedWorkspaceTrainingSpec.medicalCategoryIndex
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
        if (trainingSession.state.navigationPhasePendingFeedback ||
            trainingSession.state.completionPendingFeedback
        ) {
            return
        }
        if (!acceptedByCurrentNavigationLesson(left, right)) {
            rejectNavigationTrainingGesture()
            return
        }
        if (isNavigationLessonOffTargetAttempt(left, right)) {
            rejectNavigationTrainingGesture()
            return
        }

        val expected = trainingSession.expectedNavigationAction()

        // RC8.13 — Explore LISA uses the same production Menu / Voice / Settings pipeline.
        // RC8.15 — Move to Medical uses production Category Menu Next until Medical is selected.
        when (expected) {
            NavigationAction.OpenMenu -> {
                if (GuidedModeNavigation.isOpenMainMenuSequence(left, right)) {
                    openMainMenu()
                } else {
                    rejectNavigationTrainingGesture()
                }
                return
            }
            NavigationAction.MoveToMedicalCategory -> {
                handleMoveToMedicalLessonPhase(left, right)
                return
            }
            NavigationAction.AdjustSensitivity -> {
                handleAdjustSensitivityLessonPhase(left, right)
                return
            }
            NavigationAction.SelectCategory -> {
                // RC8.25 Lesson 17 — Open Medical only via production L3 R1 (DIRECT_SHORTCUT).
                val authority = com.idworx.lisa.features.guidedmedicalcategoryjourney
                    .GuidedMedicalCategoryJourneyAuthority
                if (!guidedOverlayActive() ||
                    uiGuidedNavigationState.value.screenMode != GuidedOverlayScreenMode.CategoryMenu ||
                    !authority.matchesOpenMedical(left, right)
                ) {
                    rejectNavigationTrainingGesture()
                    return
                }
                handleGuidedOverlaySequence(left, right)
                if (authority.isMedicalOpenedViaDirectShortcut(uiGuidedNavigationState.value)) {
                    verifyTrainingNavigation(NavigationAction.SelectCategory)
                }
                return
            }
            NavigationAction.MenuSelectVoice,
            NavigationAction.MenuSelectSettings -> {
                if (uiActivePanel.value != LisaPanel.Menu ||
                    !GuidedModeNavigation.isNextSequence(left, right)
                ) {
                    rejectNavigationTrainingGesture()
                    return
                }
                val result = MainMenuController.processSequence(
                    left = left,
                    right = right,
                    state = uiMainMenuState.value,
                    viewportHeightPx = uiMainMenuViewportHeightPx.value,
                    maxScrollPx = uiMainMenuMaxScrollPx.value
                )
                applyMainMenuResult(result)
                return
            }
            NavigationAction.OpenVoice -> {
                if (uiActivePanel.value != LisaPanel.Menu ||
                    !com.idworx.lisa.features.explorelisa.ExploreLisaAuthority
                        .matchesVoiceDestination(left, right)
                ) {
                    rejectNavigationTrainingGesture()
                    return
                }
                val result = MainMenuController.processSequence(
                    left = left,
                    right = right,
                    state = uiMainMenuState.value,
                    viewportHeightPx = uiMainMenuViewportHeightPx.value,
                    maxScrollPx = uiMainMenuMaxScrollPx.value
                )
                applyMainMenuResult(result)
                return
            }
            NavigationAction.OpenSettings -> {
                if (uiActivePanel.value != LisaPanel.Menu ||
                    !com.idworx.lisa.features.explorelisa.ExploreLisaAuthority
                        .matchesSettingsDestination(left, right)
                ) {
                    rejectNavigationTrainingGesture()
                    return
                }
                val result = MainMenuController.processSequence(
                    left = left,
                    right = right,
                    state = uiMainMenuState.value,
                    viewportHeightPx = uiMainMenuViewportHeightPx.value,
                    maxScrollPx = uiMainMenuMaxScrollPx.value
                )
                applyMainMenuResult(result)
                return
            }
            NavigationAction.BackFromDestination -> {
                if (!GuidedModeNavigation.isBackSequence(left, right)) {
                    rejectNavigationTrainingGesture()
                    return
                }
                backFromMenuDestination()
                return
            }
            NavigationAction.CloseMenu -> {
                // Workspace Back vs Explore Close Menu — both are L2 R2; route by active panel.
                if (uiActivePanel.value == LisaPanel.Menu &&
                    GuidedModeNavigation.isBackSequence(left, right)
                ) {
                    val result = MainMenuController.processSequence(
                        left = left,
                        right = right,
                        state = uiMainMenuState.value,
                        viewportHeightPx = uiMainMenuViewportHeightPx.value,
                        maxScrollPx = uiMainMenuMaxScrollPx.value
                    )
                    applyMainMenuResult(result)
                    return
                }
                val exploreClose = trainingSession.navigationLessonId() ==
                    com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.ID_CLOSE_MENU
                if (exploreClose) {
                    rejectNavigationTrainingGesture()
                    return
                }
                // RC8.18 — Lesson 19 (nav_back): wait for learner Back; complete only after
                // production Category Selection is open.
                if (!GuidedModeNavigation.isBackSequence(left, right)) {
                    rejectNavigationTrainingGesture()
                    return
                }
                val execution = com.idworx.lisa.features.guidedlessonexecutionauthority
                    .GuidedLessonExecutionAuthority
                val before = uiGuidedNavigationState.value
                if (!execution.isWorkspaceBackStartState(before) &&
                    trainingSession.navigationLessonId() == execution.ID_WORKSPACE_BACK
                ) {
                    rejectNavigationTrainingGesture()
                    return
                }
                handleGuidedOverlaySequence(left, right)
                val after = uiGuidedNavigationState.value
                if (trainingSession.navigationLessonId() == execution.ID_WORKSPACE_BACK) {
                    if (execution.isWorkspaceBackCompleted(before, after)) {
                        verifyTrainingNavigation(NavigationAction.CloseMenu)
                    }
                } else if (GuidedModeNavigation.isBackSequence(left, right)) {
                    verifyTrainingNavigation(NavigationAction.CloseMenu)
                }
                return
            }
            NavigationAction.FinishGuidedLearning -> {
                if (GuidedModeNavigation.isSelectSequence(left, right)) {
                    verifyTrainingNavigation(NavigationAction.FinishGuidedLearning)
                } else {
                    rejectNavigationTrainingGesture()
                }
                return
            }
            NavigationAction.NextPage -> {
                // RC8.26 — production Next Page (L0 R4) via nextCategoryPage; never Move Down.
                val pageAuthority = com.idworx.lisa.features.guidedcategorypagenavigation
                    .CategoryPageNavigationAuthority
                if (!guidedOverlayActive() || !pageAuthority.matchesNextPage(left, right)) {
                    rejectNavigationTrainingGesture()
                    return
                }
                val before = uiGuidedNavigationState.value
                if (!pageAuthority.isNextPageStartState(before)) {
                    rejectNavigationTrainingGesture()
                    return
                }
                handleGuidedOverlaySequence(left, right)
                val after = uiGuidedNavigationState.value
                if (pageAuthority.isNextPageCompleted(before, after, left, right)) {
                    verifyTrainingNavigation(NavigationAction.NextPage)
                }
                return
            }
            NavigationAction.PreviousPage -> {
                // RC8.26 — production Previous Page (L4 R0) via previousCategoryPage; never Move Up.
                val pageAuthority = com.idworx.lisa.features.guidedcategorypagenavigation
                    .CategoryPageNavigationAuthority
                if (!guidedOverlayActive() || !pageAuthority.matchesPreviousPage(left, right)) {
                    rejectNavigationTrainingGesture()
                    return
                }
                val before = uiGuidedNavigationState.value
                if (!pageAuthority.isPreviousPageStartState(before)) {
                    rejectNavigationTrainingGesture()
                    return
                }
                handleGuidedOverlaySequence(left, right)
                val after = uiGuidedNavigationState.value
                if (pageAuthority.isPreviousPageCompleted(before, after, left, right)) {
                    verifyTrainingNavigation(NavigationAction.PreviousPage)
                }
                return
            }
            else -> Unit
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
                    GuidedModeNavigation.isSelectSequence(left, right) -> {
                        // RC8.17 — do not complete Open Medical on generic L1 R1; require Medical workspace.
                        if (trainingSession.expectedNavigationAction() == NavigationAction.SelectCategory) {
                            if (isMedicalPhraseWorkspaceOpen()) {
                                verifyTrainingNavigation(NavigationAction.SelectCategory)
                            }
                        } else {
                            verifyTrainingNavigation(NavigationAction.SelectCategory)
                        }
                    }
                    GuidedModeNavigation.isBackSequence(left, right) ->
                        verifyTrainingNavigation(NavigationAction.CloseMenu)
                    // RC8.26 — Next/Previous Page complete only via dedicated production page gates above.
                    // Item Move Down/Up (L0 R2 / L2 R0) must never complete Lessons 20–21.
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
                if (isCategoryShortcutGesture &&
                    trainingSession.expectedNavigationAction() == NavigationAction.SelectCategory &&
                    isMedicalPhraseWorkspaceOpen()
                ) {
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

    /**
     * RC8.23 — complete the active practical phase for a multi-phase navigation lesson
     * (Lesson 16), or the whole lesson when it is single-step / on its final phase.
     */
    private fun verifyTrainingNavigationPhase(action: NavigationAction) {
        trainingSession.completeNavigationLessonPhase(action)
        refreshTrainingActiveState()
    }

    /**
     * RC8.23 / RC8.34 — Lesson 16 Method 1 (scroll + OPEN_SELECTED) then Method 2 (DIRECT_SHORTCUT).
     * Method 1 completes only when Medical is visibly open via OPEN_SELECTED; Well done shows
     * while Medical remains open, then [onNavigationPhaseAdvanced] resets for Method 2.
     * Lesson 17 is reached only after Method 2 success + final acknowledgement.
     */
    private fun handleMoveToMedicalLessonPhase(left: Int, right: Int) {
        val authority = com.idworx.lisa.features.guidedmedicalcategoryjourney
            .GuidedMedicalCategoryJourneyAuthority
        val phase = trainingSession.activeTeachingPhase()
        val before = uiGuidedNavigationState.value
        when (phase?.requiredAction) {
            com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
                .MoveDownUntilCategorySelected,
            null -> {
                if (!guidedOverlayActive() ||
                    before.screenMode != GuidedOverlayScreenMode.CategoryMenu ||
                    !GuidedModeNavigation.isNextSequence(left, right)
                ) {
                    rejectNavigationTrainingGesture()
                    return
                }
                handleGuidedOverlaySequence(left, right)
                // Medical selection alone never completes Method 1 — silent advance to open phase.
                if (authority.isMedicalSelectedInCategoryMenu(uiGuidedNavigationState.value)) {
                    verifyTrainingNavigationPhase(NavigationAction.MoveToMedicalCategory)
                }
            }
            com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
                .OpenSelectedCategory -> {
                if (!guidedOverlayActive() ||
                    before.screenMode != GuidedOverlayScreenMode.CategoryMenu ||
                    !authority.matchesOpenSelected(left, right)
                ) {
                    rejectNavigationTrainingGesture()
                    return
                }
                // Must still be on Medical before Select; otherwise opening another category fails.
                if (!authority.isMedicalSelectedInCategoryMenu(before)) {
                    rejectNavigationTrainingGesture()
                    return
                }
                handleGuidedOverlaySequence(left, right)
                // RC8.34 — gate requires fresh L1 R1 + OPEN_SELECTED + visible Medical workspace.
                if (authority.isMethod1OpenCompleted(
                        before,
                        uiGuidedNavigationState.value,
                        left,
                        right
                    )
                ) {
                    verifyTrainingNavigationPhase(NavigationAction.MoveToMedicalCategory)
                }
            }
            com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
                .CategoryShortcutJump -> {
                if (!guidedOverlayActive() ||
                    before.screenMode != GuidedOverlayScreenMode.CategoryMenu ||
                    !authority.matchesOpenMedical(left, right)
                ) {
                    rejectNavigationTrainingGesture()
                    return
                }
                handleGuidedOverlaySequence(left, right)
                // RC8.34 — Method 2 requires fresh L3 R1 + DIRECT_SHORTCUT + visible Medical.
                if (authority.isMethod2DirectCompleted(
                        before,
                        uiGuidedNavigationState.value,
                        left,
                        right
                    )
                ) {
                    verifyTrainingNavigationPhase(NavigationAction.MoveToMedicalCategory)
                }
            }
            else -> rejectNavigationTrainingGesture()
        }
    }

    /** RC8.32 — Lesson 23 five-phase Settings adjustment journey. */
    private fun acceptsAdjustSensitivityPhaseGesture(left: Int, right: Int): Boolean {
        val authority = com.idworx.lisa.features.guidedsensitivitylesson.GuidedSensitivityLessonAuthority
        val phase = trainingSession.activeTeachingPhase()
        return when (phase?.requiredAction) {
            com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
                .MoveToSettingsPage ->
                authority.matchesMoveToSettingsPage(left, right)
            com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
                .OpenSettingsAndControls ->
                authority.matchesOpenSettings(left, right)
            com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
                .OpenSensitivitySetting ->
                authority.matchesOpenSensitivity(left, right)
            com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
                .AdjustSensitivity,
            com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
                .IncreaseSensitivityOnce ->
                authority.matchesAdjust(left, right)
            com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
                .ReturnToSettingsAndControls ->
                authority.matchesReturnToSettings(left, right)
            com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
                .SaveSensitivity -> false
            else -> authority.matchesMoveToSettingsPage(left, right)
        }
    }

    private fun handleAdjustSensitivityLessonPhase(left: Int, right: Int) {
        val authority = com.idworx.lisa.features.guidedsensitivitylesson.GuidedSensitivityLessonAuthority
        val phase = trainingSession.activeTeachingPhase()
        val state = uiGuidedNavigationState.value
        when (phase?.requiredAction) {
            com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
                .MoveToSettingsPage,
            null -> {
                val pageAuthority = com.idworx.lisa.features.guidedcategorypagenavigation
                    .CategoryPageNavigationAuthority
                if (!guidedOverlayActive() || !authority.matchesMoveToSettingsPage(left, right)) {
                    rejectNavigationTrainingGesture()
                    return
                }
                if (!authority.isMoveToSettingsPageStartState(state)) {
                    rejectNavigationTrainingGesture()
                    return
                }
                handleGuidedOverlaySequence(left, right)
                val after = uiGuidedNavigationState.value
                if (authority.isMoveToSettingsPageCompleted(state, after, left, right) ||
                    pageAuthority.isNextPageCompleted(state, after, left, right)
                ) {
                    verifyTrainingNavigationPhase(NavigationAction.AdjustSensitivity)
                }
            }
            com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
                .OpenSettingsAndControls -> {
                if (!guidedOverlayActive() || !authority.matchesOpenSettings(left, right)) {
                    rejectNavigationTrainingGesture()
                    return
                }
                handleGuidedOverlaySequence(left, right)
                if (authority.isSettingsHubOpen(uiGuidedNavigationState.value)) {
                    verifyTrainingNavigationPhase(NavigationAction.AdjustSensitivity)
                }
            }
            com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
                .OpenSensitivitySetting -> {
                if (!guidedOverlayActive() ||
                    !authority.isSettingsHubOpen(state) ||
                    !authority.matchesOpenSensitivity(left, right)
                ) {
                    rejectNavigationTrainingGesture()
                    return
                }
                handleGuidedOverlaySequence(left, right)
                if (authority.isSensitivityAdjustmentOpen(uiGuidedNavigationState.value)) {
                    if (sensitivityLessonStartLevel == null) {
                        sensitivityLessonStartLevel =
                            uiGuidedNavigationState.value.draftSensitivityLevel
                    }
                    verifyTrainingNavigationPhase(NavigationAction.AdjustSensitivity)
                }
            }
            com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
                .AdjustSensitivity,
            com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
                .IncreaseSensitivityOnce -> {
                if (!guidedOverlayActive() ||
                    !authority.isSensitivityAdjustmentOpen(state) ||
                    !authority.matchesAdjust(left, right)
                ) {
                    rejectNavigationTrainingGesture()
                    return
                }
                val before = state.draftSensitivityLevel
                handleGuidedOverlaySequence(left, right)
                val afterState = uiGuidedNavigationState.value
                val after = afterState.draftSensitivityLevel
                val savedOk = authority.isAdjustCompleted(before, after) &&
                    uiSensitivityLevel.value == after &&
                    authority.isSensitivityAdjustmentOpen(afterState)
                if (savedOk) {
                    sensitivityLessonTargetLevel = after
                    verifyTrainingNavigationPhase(NavigationAction.AdjustSensitivity)
                    // RC8.32 — keep adjusted value through Back proof; restore after Well done /
                    // before Start Communicating or Restart Guided Learning.
                } else {
                    rejectNavigationTrainingGesture()
                }
            }
            com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
                .ReturnToSettingsAndControls -> {
                if (!guidedOverlayActive() ||
                    !authority.isSensitivityAdjustmentOpen(state) ||
                    !authority.matchesReturnToSettings(left, right)
                ) {
                    rejectNavigationTrainingGesture()
                    return
                }
                val adjusted = sensitivityLessonTargetLevel ?: state.draftSensitivityLevel
                handleGuidedOverlaySequence(left, right)
                val afterState = uiGuidedNavigationState.value
                val backOk = authority.isSettingsHubOpen(afterState) &&
                    !authority.isSensitivityAdjustmentOpen(afterState) &&
                    uiSensitivityLevel.value == adjusted
                if (backOk) {
                    verifyTrainingNavigationPhase(NavigationAction.AdjustSensitivity)
                    // RC8.32 — restore after Well done acknowledgement window, before Start Communicating.
                    restoreSensitivityLessonPreferenceDelayed()
                } else {
                    rejectNavigationTrainingGesture()
                }
            }
            else -> rejectNavigationTrainingGesture()
        }
    }

    /**
     * RC8.32 — restore the pre-lesson Sensitivity after Well done / before normal entry or restart.
     * Not called immediately after adjustment so Back can prove the persisted value.
     */
    private fun restoreSensitivityLessonPreferenceIfNeeded() {
        val original = sensitivityLessonOriginalLevel ?: return
        applySensitivityLevel(original, persist = true)
        sensitivityLessonOriginalLevel = null
        sensitivityLessonStartLevel = null
        sensitivityLessonTargetLevel = null
    }

    private fun restoreSensitivityLessonPreferenceDelayed() {
        val original = sensitivityLessonOriginalLevel ?: return
        mainHandler.postDelayed({
            if (sensitivityLessonOriginalLevel != original) return@postDelayed
            restoreSensitivityLessonPreferenceIfNeeded()
        }, 1_600L)
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
        val language = LisaLanguageAvailabilityAuthority.coerceForVersion1(profile.preferredLanguage)
        uiActiveLanguage.value = language
        applyTtsForProfile(profile.copy(preferredLanguage = language))
        refreshVoiceSettingsState()
        applySensitivityLevel(profile.sensitivityLevel, persist = false)
        uiDeveloperMode.value = profile.developerMode
        saveDeveloperMode(this, profile.developerMode)
        countdownDurationSec = profile.confirmationCountdownSec
        applySequenceProcessingDelay(profile.sequenceProcessingDelaySec, persist = false)
        uiSpeechVolumeLevel.value = SpeechVolumeAuthority.coerce(profile.speechVolumeLevel)
        uiSpeechRateLevel.value = SpeechSpeedAuthority.coerce(profile.speechRateLevel)
        tts?.setSpeechRate(SpeechSpeedAuthority.toSpeechRate(uiSpeechRateLevel.value))
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
            speechVolumeLevel = uiSpeechVolumeLevel.value,
            speechSpeedLevel = uiSpeechRateLevel.value,
            listeningPaused = uiListeningPaused.value,
            caregiverCustomPhrases = CustomPhraseEngine.toCatalogEntries(mappingsState.filter { it.isCustom })
        )

    private fun applySpeechVolumeLevel(level: Int, persist: Boolean = true) {
        val coerced = SpeechVolumeAuthority.coerce(level)
        uiSpeechVolumeLevel.value = coerced
        if (persist) {
            updateActiveProfile { it.copy(speechVolumeLevel = coerced) }
        }
    }

    private fun applySpeechRateLevel(level: Int, persist: Boolean = true) {
        val coerced = SpeechSpeedAuthority.coerce(level)
        uiSpeechRateLevel.value = coerced
        tts?.setSpeechRate(SpeechSpeedAuthority.toSpeechRate(coerced))
        if (persist) {
            updateActiveProfile { it.copy(speechRateLevel = coerced) }
        }
    }

    private fun previewSpeechVolumeDraft(level: Int) {
        // Preview only — persistence happens on confirmed Save.
        uiSpeechVolumeLevel.value = SpeechVolumeAuthority.coerce(level)
    }

    private fun previewSpeechSpeedDraft(level: Int) {
        val coerced = SpeechSpeedAuthority.coerce(level)
        uiSpeechRateLevel.value = coerced
        tts?.setSpeechRate(SpeechSpeedAuthority.toSpeechRate(coerced))
    }

    private fun restoreSpeechVolumeOriginal(level: Int) {
        uiSpeechVolumeLevel.value = SpeechVolumeAuthority.coerce(level)
    }

    private fun restoreSpeechSpeedOriginal(level: Int) {
        val coerced = SpeechSpeedAuthority.coerce(level)
        uiSpeechRateLevel.value = coerced
        tts?.setSpeechRate(SpeechSpeedAuthority.toSpeechRate(coerced))
    }

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
                    exitPhraseManagementToOwner()
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

    private fun mappingsForSequenceContinuation(): List<WinkMapping> {
        // While Menu / destinations / composer own input, never advertise Communication phrases
        // as continuation targets — that produced STOP / WAITING previews over Main Menu.
        if (!ModeScopedGestureAuthority.communicationPhraseFeedbackActive(buildGestureContext())) {
            return activeScopeContinuationMappings()
        }
        return if (guidedOverlayActive()) {
            var base = workspaceContinuationMappings()
            // RC7D.25 — explicit continuation protection for the L5 R5 Adjust Settings entry. While
            // the user is still winking toward the full 5×5 sequence (every shorter existing gesture
            // is a prefix of it), advertising L5 R5 as a longer continuation keeps the partial in a
            // "keep winking" hint instead of prematurely resolving a shorter prefix. Only advertised
            // when NOT already adjusting, since the entry gesture is only meaningful from the normal
            // (non-adjustment) workspace.
            if (!uiGuidedNavigationState.value.isPreferencesAdjustmentActive) {
                base = base + WinkMapping(
                    GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_LEFT,
                    GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_RIGHT,
                    "",
                    isCustom = true,
                    customPhrase = ""
                )
            }
            // RC7D.28 — L4 R6 Open Menu continuation protection (shorter L4 R* prefixes exist).
            if (uiActivePanel.value != LisaPanel.Menu) {
                base = base + WinkMapping(
                    GuidedModeNavigation.OPEN_MAIN_MENU_LEFT,
                    GuidedModeNavigation.OPEN_MAIN_MENU_RIGHT,
                    "",
                    isCustom = true,
                    customPhrase = ""
                )
            }
            base
        } else {
            val base = mappingsState.toList()
            if (uiActivePanel.value != LisaPanel.Menu) {
                base + WinkMapping(
                    GuidedModeNavigation.OPEN_MAIN_MENU_LEFT,
                    GuidedModeNavigation.OPEN_MAIN_MENU_RIGHT,
                    "",
                    isCustom = true,
                    customPhrase = ""
                )
            } else {
                base
            }
        }
    }

    /** Continuation mappings owned by the active non-Communication scope (no phrase slots). */
    private fun activeScopeContinuationMappings(): List<WinkMapping> {
        val mode = ModeScopedGestureAuthority.activeMode(buildGestureContext())
        return ModeScopedGestureAuthority.namespaceFor(mode).map { binding ->
            WinkMapping(
                binding.left,
                binding.right,
                "",
                isCustom = true,
                customPhrase = ""
            )
        }
    }

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

    private fun applyGuidedTouchNavigation(
        left: Int,
        right: Int,
        blinkOrder: List<Boolean> = emptyList()
    ) {
        if (trainingSession.isNavigationTrainingActive() &&
            (!acceptedByCurrentNavigationLesson(left, right) || isNavigationLessonOffTargetAttempt(left, right))
        ) {
            rejectNavigationTrainingGesture()
            refreshTrainingActiveState()
            return
        }
        // RC8.18 — touch Back during workspace Go Back: same production gate as blink L2 R2.
        if (trainingSession.isNavigationTrainingActive() &&
            trainingSession.expectedNavigationAction() == NavigationAction.CloseMenu &&
            trainingSession.navigationLessonId() ==
            com.idworx.lisa.features.guidedlessonexecutionauthority
                .GuidedLessonExecutionAuthority.ID_WORKSPACE_BACK &&
            GuidedModeNavigation.isBackSequence(left, right)
        ) {
            val execution = com.idworx.lisa.features.guidedlessonexecutionauthority
                .GuidedLessonExecutionAuthority
            val before = uiGuidedNavigationState.value
            if (!execution.isWorkspaceBackStartState(before)) {
                rejectNavigationTrainingGesture()
                refreshTrainingActiveState()
                return
            }
            handleGuidedOverlaySequence(left, right, blinkOrder)
            if (execution.isWorkspaceBackCompleted(before, uiGuidedNavigationState.value)) {
                verifyTrainingNavigation(NavigationAction.CloseMenu)
            }
            return
        }
        if (GuidedModeNavigation.isCategoriesSequence(left, right)) {
            verifyTrainingNavigation(NavigationAction.OpenCategories)
        }
        handleGuidedOverlaySequence(left, right, blinkOrder)
        // RC8.15 / RC8.24 — touch Next during Lesson 16 scroll stage advances when Medical is selected.
        if (trainingSession.isNavigationTrainingActive() &&
            trainingSession.expectedNavigationAction() == NavigationAction.MoveToMedicalCategory &&
            GuidedModeNavigation.isNextSequence(left, right) &&
            trainingSession.activeTeachingPhase()?.requiredAction ==
            com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
                .MoveDownUntilCategorySelected &&
            com.idworx.lisa.features.guidedmedicalcategoryjourney
                .GuidedMedicalCategoryJourneyAuthority
                .isMedicalSelectedInCategoryMenu(uiGuidedNavigationState.value)
        ) {
            verifyTrainingNavigationPhase(NavigationAction.MoveToMedicalCategory)
        }
    }

    /**
     * RC7D.22 — the Compose layer measures the real category-list viewport + content and reports the
     * canonical viewport-page count and current page. Fold it into navigation state (guarded, and
     * only while the Category Menu is showing) so the header, button-enabled state and the
     * controller's page-nav gating share one source of truth. Guarding on a real change keeps this
     * measurement → state → recompose path from looping.
     */
    private fun updateGuidedCategoryViewportPageState(pageCount: Int, currentPage: Int) {
        val current = uiGuidedNavigationState.value
        if (current.screenMode != GuidedOverlayScreenMode.CategoryMenu) return
        val safeCount = pageCount.coerceAtLeast(1)
        val safePage = currentPage.coerceIn(0, safeCount - 1)
        if (current.categoryViewportPageCount == safeCount && current.categoryViewportPage == safePage) return
        uiGuidedNavigationState.value = current.copy(
            categoryViewportPageCount = safeCount,
            categoryViewportPage = safePage
        )
    }

    private fun openGuidedCategoryFromTouch(categoryIndex: Int) {
        if (trainingSession.isNavigationTrainingActive()) {
            val expected = trainingSession.expectedNavigationAction()
            // RC8.15 / RC8.24 — Lesson 16 phase-aware Medical touch.
            if (expected == NavigationAction.MoveToMedicalCategory) {
                if (categoryIndex != GuidedWorkspaceTrainingSpec.medicalCategoryIndex) {
                    rejectNavigationTrainingGesture()
                    refreshTrainingActiveState()
                    return
                }
                val phaseAction = trainingSession.activeTeachingPhase()?.requiredAction
                when (phaseAction) {
                    com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
                        .CategoryShortcutJump -> {
                        rejectNavigationTrainingGesture()
                    }
                    com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
                        .OpenSelectedCategory -> {
                        // Part 1 open stage requires L1 R1 / Select — category touch alone is not enough.
                        rejectNavigationTrainingGesture()
                    }
                    else -> {
                        // Scroll stage — select only; silent advance when Medical is selected.
                        uiGuidedNavigationState.value = uiGuidedNavigationState.value.copy(
                            categoryMenuSelection = GuidedWorkspaceTrainingSpec.medicalCategoryIndex,
                            categoryNavigationCause = CategoryNavigationCause.ITEM_MOVEMENT
                        )
                        if (com.idworx.lisa.features.guidedmedicalcategoryjourney
                                .GuidedMedicalCategoryJourneyAuthority
                                .isMedicalSelectedInCategoryMenu(uiGuidedNavigationState.value)
                        ) {
                            verifyTrainingNavigationPhase(NavigationAction.MoveToMedicalCategory)
                        }
                    }
                }
                refreshTrainingActiveState()
                return
            }
            // RC8.17 — Open Medical: only Medical may open; never advance via other destinations.
            if (expected == NavigationAction.SelectCategory) {
                if (categoryIndex != GuidedWorkspaceTrainingSpec.medicalCategoryIndex) {
                    rejectNavigationTrainingGesture()
                    refreshTrainingActiveState()
                    return
                }
            } else {
                val isHighlightedCategory = categoryIndex == GuidedWorkspaceTrainingSpec.medicalCategoryIndex
                val allowed = expected != null &&
                    GuidedTrainingFocusPolicy.isTargetAllowed(
                        expected, NavigationAction.SelectCategory, isHighlightedCategory
                    )
                if (!allowed) {
                    rejectNavigationTrainingGesture()
                    refreshTrainingActiveState()
                    return
                }
            }
        }
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
            CategoryAreaDestination.AdjustSettings -> {
                openAdjustSettingsFromCategories()
                verifyTrainingNavigation(NavigationAction.SelectCategory)
                return
            }
            is CategoryAreaDestination.CommunicationCategory -> Unit
        }
        uiGuidedNavigationState.value = GuidedNavigationController.openCategoryDirectly(
            uiGuidedNavigationState.value,
            categoryIndex
        )
        // RC8.25 — Lesson 17 completes only via production DIRECT_SHORTCUT into Medical.
        if (trainingSession.expectedNavigationAction() == NavigationAction.SelectCategory) {
            if (com.idworx.lisa.features.guidedmedicalcategoryjourney
                    .GuidedMedicalCategoryJourneyAuthority
                    .isMedicalOpenedViaDirectShortcut(uiGuidedNavigationState.value)
            ) {
                verifyTrainingNavigation(NavigationAction.SelectCategory)
            }
        } else {
            verifyTrainingNavigation(NavigationAction.SelectCategory)
        }
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

    /** RC7D.26 — Category Menu destination 9 and L5 R5 share this canonical Adjust Settings entry. */
    private fun openAdjustSettingsFromCategories() {
        val current = uiGuidedNavigationState.value.copy(
            screenMode = GuidedOverlayScreenMode.CategoryMenu,
            categoryMenuSelection = GuidedVocabularyCategory.ADJUST_SETTINGS_INDEX
        )
        uiGuidedNavigationState.value = PreferenceAdjustmentController.openSettingsMenu(current)
        uiGuidedConfirmedPhrase.value = guidedUiStrings().guidedAdjustSettingsTitle
        uiGuidedConfirmedLeft.value = GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_LEFT
        uiGuidedConfirmedRight.value = GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_RIGHT
        setCommunicationState(LisaCommunicationState.Listening)
        mainHandler.removeCallbacks(guidedConfirmationClearRunnable)
        mainHandler.postDelayed(guidedConfirmationClearRunnable, 1500L)
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
                // RC8.1 — top-level Communication entry lands on Category Selection.
                uiGuidedNavigationState.value = GuidedNavigationController.communicationWorkspaceRoot(
                    uiGuidedNavigationState.value
                )
                setCommunicationState(LisaCommunicationState.Listening)
            }
        }
    }

    private fun exitPhraseManagementToOwner() {
        if (phraseManagementOpenedFromMainMenu) {
            phraseManagementOpenedFromMainMenu = false
            phraseManagementOpenedFromCategories = false
            resetPhraseManagementState()
            openPanel(LisaPanel.Menu)
        } else {
            exitPhraseManagement(
                PhraseManagementController.PhraseManagementExitDestination.CommunicationWorkspace
            )
        }
    }

    private fun backFromActivePanel() {
        if (uiActivePanel.value == LisaPanel.VocabularyTraining) {
            exitPhraseManagementToOwner()
            return
        }
        if (MenuDestinationProductionUiAuthority.occupiesMainContentSlot(uiActivePanel.value)) {
            backFromMenuDestination()
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

    private fun handleGuidedOverlaySequence(
        left: Int,
        right: Int,
        blinkOrderOverride: List<Boolean>? = null
    ) {
        if (returnToComposerFromCategoryViewIfNeeded(left, right)) {
            resetSequence()
            return
        }
        if (uiListeningPaused.value &&
            !GuidedModeNavigation.isGlobalNavigationSequence(left, right) &&
            !GuidedModeNavigation.isAdjustSettingsEntrySequence(left, right) &&
            !uiGuidedNavigationState.value.isPreferencesAdjustmentActive
        ) {
            resetSequence()
            updateReadyOrWaitingState()
            return
        }

        val uiStrings = guidedUiStrings()
        val catalogContext = guidedCatalogContext()
        // RC7D.25 — the adjustment level active BEFORE this gesture, so Cancel feedback can name the
        // setting whose changes were discarded even though the result state has already cleared it.
        val priorAdjustMode = uiGuidedNavigationState.value.preferencesAdjustMode
        val result = GuidedNavigationController.processSequence(
            left = left,
            right = right,
            state = uiGuidedNavigationState.value,
            language = activeLanguage(),
            uiStrings = uiStrings,
            visibleEntryCap = guidedVisibleEntryCap(),
            catalogContext = catalogContext,
            blinkOrder = blinkOrderOverride ?: currentBlinkOrder()
        )
        resetSequence()
        applyGuidedSequenceResult(result, left, right, uiStrings, priorAdjustMode)
    }

    private fun applyGuidedSequenceResult(
        result: GuidedSequenceResult,
        left: Int,
        right: Int,
        uiStrings: LisaUiStrings,
        priorAdjustMode: GuidedPreferencesAdjustMode = GuidedPreferencesAdjustMode.None
    ) {
        when (result) {
            is GuidedSequenceResult.Navigate -> {
                val destination = CategoryAreaDestination.forCategoryIndex(result.newState.categoryIndex)
                val openingManagementDestination =
                    result.newState.screenMode == GuidedOverlayScreenMode.Vocabulary &&
                        (destination is CategoryAreaDestination.CreateCustomPhrase ||
                            destination is CategoryAreaDestination.PhraseManagement ||
                            destination is CategoryAreaDestination.AdjustSettings)
                if (openingManagementDestination) {
                    when (destination) {
                        CategoryAreaDestination.CreateCustomPhrase -> openComposeModeFromCustom()
                        CategoryAreaDestination.PhraseManagement -> openPhraseManagementFromCategories()
                        CategoryAreaDestination.AdjustSettings -> openAdjustSettingsFromCategories()
                        is CategoryAreaDestination.CommunicationCategory -> Unit
                    }
                    uiGuidedConfirmedPhrase.value = null
                    uiGuidedConfirmedLeft.value = null
                    uiGuidedConfirmedRight.value = null
                } else {
                    val priorOriginalSpeechVolume =
                        uiGuidedNavigationState.value.adjustmentOriginalSpeechVolumeLevel
                    val priorOriginalSpeechSpeed =
                        uiGuidedNavigationState.value.adjustmentOriginalSpeechSpeedLevel
                    uiGuidedNavigationState.value = result.newState
                    when (result.newState.preferencesAdjustMode) {
                        GuidedPreferencesAdjustMode.SpeechVolume,
                        GuidedPreferencesAdjustMode.ConfirmSaveSpeechVolume ->
                            previewSpeechVolumeDraft(result.newState.draftSpeechVolumeLevel)
                        GuidedPreferencesAdjustMode.SpeechSpeed,
                        GuidedPreferencesAdjustMode.ConfirmSaveSpeechSpeed ->
                            previewSpeechSpeedDraft(result.newState.draftSpeechSpeedLevel)
                        else -> Unit
                    }
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
                        // RC7D.25 — Cancel (Back) out of a value-adjustment restores the original
                        // value (the draft is simply discarded) and shows a brief cancellation note.
                        val cancelMessage = when {
                            GuidedModeNavigation.isBackSequence(left, right) &&
                                priorAdjustMode == GuidedPreferencesAdjustMode.Sensitivity &&
                                result.newState.preferencesAdjustMode == GuidedPreferencesAdjustMode.SettingsMenu ->
                                uiStrings.guidedSensitivityChangesCancelled
                            GuidedModeNavigation.isBackSequence(left, right) &&
                                priorAdjustMode == GuidedPreferencesAdjustMode.ResponseTime &&
                                result.newState.preferencesAdjustMode == GuidedPreferencesAdjustMode.SettingsMenu ->
                                uiStrings.guidedResponseTimeChangesCancelled
                            GuidedModeNavigation.isBackSequence(left, right) &&
                                priorAdjustMode == GuidedPreferencesAdjustMode.SpeechVolume &&
                                result.newState.preferencesAdjustMode == GuidedPreferencesAdjustMode.SettingsMenu -> {
                                restoreSpeechVolumeOriginal(priorOriginalSpeechVolume)
                                uiStrings.guidedSpeechVolumeChangesCancelled
                            }
                            GuidedModeNavigation.isBackSequence(left, right) &&
                                priorAdjustMode == GuidedPreferencesAdjustMode.SpeechSpeed &&
                                result.newState.preferencesAdjustMode == GuidedPreferencesAdjustMode.SettingsMenu -> {
                                restoreSpeechSpeedOriginal(priorOriginalSpeechSpeed)
                                uiStrings.guidedSpeechSpeedChangesCancelled
                            }
                            else -> null
                        }
                        if (cancelMessage != null) {
                            uiGuidedConfirmedPhrase.value = cancelMessage
                            uiGuidedConfirmedLeft.value = GuidedModeNavigation.BACK_LEFT
                            uiGuidedConfirmedRight.value = GuidedModeNavigation.BACK_RIGHT
                            mainHandler.removeCallbacks(guidedConfirmationClearRunnable)
                            mainHandler.postDelayed(guidedConfirmationClearRunnable, 1500L)
                        } else {
                            uiGuidedConfirmedPhrase.value = null
                            uiGuidedConfirmedLeft.value = null
                            uiGuidedConfirmedRight.value = null
                        }
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
                // RC8.17 / RC8.25 — SelectPhrase advances only on a fresh Speak of the first Medical
                // phrase after Lesson 18 entry armed acceptance (rejects stale / wrong phrases).
                if (trainingSession.expectedNavigationAction() == NavigationAction.SelectPhrase &&
                    medicalPhraseLessonArmed &&
                    isMedicalPhraseWorkspaceOpen() &&
                    com.idworx.lisa.features.guidedmedicalcategoryjourney
                        .GuidedMedicalCategoryJourneyAuthority
                        .matchesFirstMedicalPhrase(
                            result.entry,
                            activeLanguage(),
                            guidedUiStrings()
                        )
                ) {
                    medicalPhraseLessonArmed = false
                    verifyTrainingNavigation(NavigationAction.SelectPhrase)
                }
                speak(phrase)
                mainHandler.removeCallbacks(guidedConfirmationClearRunnable)
                mainHandler.postDelayed(guidedConfirmationClearRunnable, 2500L)
            }
            is GuidedSequenceResult.SavePreferencesAdjustment -> {
                uiGuidedNavigationState.value = result.newState
                // Save routes through the SAME persistence + runtime-apply authority as the touch
                // −/+ controls (setSequenceProcessingDelay / applySensitivityLevel), so there is no
                // duplicate mutation path — blink and touch share one source of truth.
                result.responseTimeSec?.let { setSequenceProcessingDelay(it) }
                result.sensitivityLevel?.let { applySensitivityLevel(it) }
                result.speechVolumeLevel?.let { applySpeechVolumeLevel(it) }
                result.speechSpeedLevel?.let { applySpeechRateLevel(it) }
                uiGuidedConfirmedPhrase.value = when {
                    result.sensitivityLevel != null -> uiStrings.guidedSensitivitySaved(result.sensitivityLevel)
                    result.responseTimeSec != null -> uiStrings.guidedResponseTimeSaved(result.responseTimeSec)
                    result.speechVolumeLevel != null -> uiStrings.guidedSpeechVolumeSaved(result.speechVolumeLevel)
                    result.speechSpeedLevel != null -> uiStrings.guidedSpeechSpeedSaved(result.speechSpeedLevel)
                    else -> uiStrings.guidedActionConfirmed
                }
                uiGuidedConfirmedLeft.value = GuidedModeNavigation.SELECT_LEFT
                uiGuidedConfirmedRight.value = GuidedModeNavigation.SELECT_RIGHT
                setCommunicationState(LisaCommunicationState.Listening)
                mainHandler.removeCallbacks(guidedConfirmationClearRunnable)
                mainHandler.postDelayed(guidedConfirmationClearRunnable, 1500L)
            }
            is GuidedSequenceResult.SettingsControlAction -> {
                when (result.kind) {
                    SettingsControlKind.RepeatLastMessage ->
                        executeGuidedOverlayAction(GuidedOverlayAction.RepeatLastPhrase)
                    SettingsControlKind.ResetSequence ->
                        executeGuidedOverlayAction(GuidedOverlayAction.ResetSequence)
                    SettingsControlKind.ShowHelp ->
                        executeGuidedOverlayAction(GuidedOverlayAction.ShowHelp)
                    SettingsControlKind.Listening -> toggleListeningPaused()
                    else -> Unit
                }
                uiGuidedConfirmedPhrase.value = when (result.kind) {
                    SettingsControlKind.Listening ->
                        if (uiListeningPaused.value) uiStrings.guidedListeningPausedStatus
                        else uiStrings.guidedListeningActiveStatus
                    SettingsControlKind.RepeatLastMessage -> uiStrings.guidedRepeatLastMessageAction
                    SettingsControlKind.ResetSequence -> uiStrings.guidedResetSequenceAction
                    SettingsControlKind.ShowHelp -> uiStrings.guidedShowHelpAction
                    else -> uiStrings.guidedActionConfirmed
                }
                uiGuidedConfirmedLeft.value = left
                uiGuidedConfirmedRight.value = right
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

    private fun createPrimaryUserFromStartup(
        name: String,
        languageLabel: String,
        levelLabel: String
    ): String {
        val language = LisaLanguageAvailabilityAuthority.coerceForVersion1(
            PreferredLanguage.fromStored(languageLabel)
        )
        val level = CommunicationLevel.fromStored(levelLabel)
        val profile = LisaUserProfile.createNew(name, activeProfile()).copy(
            preferredLanguage = language,
            communicationLevel = level
        ).withCommunicationLevel(level)
        uiProfiles.clear()
        uiProfiles.add(profile)
        uiActiveProfileId.value = profile.id
        applyProfileSettings(profile, persist = true)
        saveProfilesToStore()
        return profile.id
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
        val safeLanguage = LisaLanguageAvailabilityAuthority.coerceForVersion1(language)
        activeProfile()?.let { applyTtsForProfile(it.copy(preferredLanguage = safeLanguage)) } ?: run {
            tts?.language = LisaUiStrings.ttsLocale(safeLanguage)
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
        val base = sensitivitySettingsForLevel(clamped)
        val calibration = activeProfile()?.eyeCalibration
        val tuning = if (calibration != null) {
            com.idworx.lisa.features.intelligentstartup.authority.EyeCalibrationAuthority
                .toBlinkTuning(calibration, base)
        } else {
            base
        }
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

    private fun applyProfileEyeCalibration(
        calibration: com.idworx.lisa.features.intelligentstartup.model.ProfileEyeCalibration
    ) {
        val base = sensitivitySettingsForLevel(uiSensitivityLevel.value)
        val tuning = com.idworx.lisa.features.intelligentstartup.authority.EyeCalibrationAuthority
            .toBlinkTuning(calibration, base)
        blinkProcessor.tuning = tuning
        closedEyeThreshold = tuning.closedEyeThreshold
        openEyeThreshold = tuning.openEyeThreshold
        requiredWinkFrames = tuning.requiredWinkFrames
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
                // RC8.27 — while the alarm is active, keep processing eyes so Stop Emergency
                // (L1 R1) can match touch. No other sequences are executed in that mode.
                if (emergencyActive) {
                    if (faces.isEmpty()) {
                        updateDiagnostics(null, null)
                        return@addOnSuccessListener
                    }
                    val face = faces[0]
                    val eyes = userEyeProbabilities(face)
                    if (eyes == null) {
                        updateDiagnostics(null, null)
                        return@addOnSuccessListener
                    }
                    processActiveEmergencyStopWinks(eyes.userLeft, eyes.userRight)
                    return@addOnSuccessListener
                }
                if (faces.isEmpty()) {
                    uiTrackingLost.value = false
                    uiFacePresent.value = false
                    uiEyesDetected.value = false
                    blinkProcessor.clearPreviousProbabilities()
                    updateDiagnostics(null, null)
                    if (startupSession.isActive) {
                        startupSession.onFacePresence(false)
                    }
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
                if (startupSession.isActive) {
                    startupSession.onFacePresence(true)
                }
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

        if (startupSession.isActive) {
            val bounds = face.boundingBox
            val faceWidthNormalized = if (bounds.width() > 0) {
                // Relative width proxy; absolute camera resolution varies by device.
                (bounds.width().toFloat() / 1000f).coerceIn(0.05f, 1f)
            } else {
                0.35f
            }
            startupSession.onFrameSample(
                leftOpenness = leftProb,
                rightOpenness = rightProb,
                faceWidthNormalized = faceWidthNormalized
            )
        }
        if (settingsRecalibrationController.isActive) {
            val bounds = face.boundingBox
            val faceWidthNormalized = if (bounds.width() > 0) {
                (bounds.width().toFloat() / 1000f).coerceIn(0.05f, 1f)
            } else {
                0.35f
            }
            settingsRecalibrationController.onFrameSample(
                leftOpenness = leftProb,
                rightOpenness = rightProb,
                faceWidthNormalized = faceWidthNormalized
            )
        }

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

    /**
     * RC8.27 — while the alarm is sounding, only L1 R1 (confirm / left-then-right) stops
     * Emergency. Touch Stop Emergency and this blink path share [cancelOrStopEmergency].
     * Armed confirmation still uses L1 R1 to confirm and R1 L1 to cancel — phases never overlap.
     */
    private fun processActiveEmergencyStopWinks(leftProb: Float, rightProb: Float) {
        val now = System.currentTimeMillis()
        val result = blinkProcessor.processFrame(
            BlinkEyeProbabilities(leftProb, rightProb),
            now,
            acceptedLeftCount = leftWinks,
            acceptedRightCount = rightWinks
        )
        if (result.acceptLeft) {
            flashAcceptedBlink(isLeft = true)
            leftWinks += 1
            if (sequenceStartMs == 0L) sequenceStartMs = now
            lastWinkTimeMs = now
            recordWinkSide(isLeft = true)
        }
        if (result.acceptRight) {
            flashAcceptedBlink(isLeft = false)
            rightWinks += 1
            if (sequenceStartMs == 0L) sequenceStartMs = now
            lastWinkTimeMs = now
            recordWinkSide(isLeft = false)
        }
        // Publish live counters after increments so Emergency Active Left/Right update immediately.
        updateDiagnostics(leftProb, rightProb, result)
        val hasCountedWinks = leftWinks > 0 || rightWinks > 0
        val activelyWinking = result.leftCandidate || result.rightCandidate
        if (!hasCountedWinks || lastWinkTimeMs == 0L) return
        val idleMs = now - lastWinkTimeMs
        val totalWindowMs = now - sequenceStartMs
        val finalize = !activelyWinking &&
            shouldFinalizeSequence(
                left = leftWinks,
                right = rightWinks,
                idleMs = idleMs,
                sequenceAgeMs = totalWindowMs,
                idleTimeoutMs = effectiveSequenceIdleTimeoutMs(),
                maxWindowMs = effectiveSequenceMaxWindowMs()
            )
        if (!finalize) return
        val order = currentBlinkOrder()
        val left = leftWinks
        val right = rightWinks
        resetSequence()
        if (com.idworx.lisa.features.brain1interactionstandard.model.UniversalInteractionGestures
                .isConfirm(left, right, order)
        ) {
            cancelOrStopEmergency()
        }
    }

    private fun processSequenceWinks(leftProb: Float, rightProb: Float) {
        if (countdownActive) return
        if (emergencyActive) {
            processActiveEmergencyStopWinks(leftProb, rightProb)
            return
        }

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
            if (startupSession.isActive && !startupSession.eyeControlEnabled) {
                startupSession.onLeftWinkAccepted(closePeak = leftProb)
                flashAcceptedBlink(isLeft = true)
            } else if (settingsRecalibrationController.isActive &&
                settingsRecalibrationController.state.outcome == SettingsRecalibrationOutcome.InProgress
            ) {
                settingsRecalibrationController.onLeftWinkAccepted(closePeak = leftProb)
                flashAcceptedBlink(isLeft = true)
            } else {
                if (rejectLessonWrongEyeBlink(isLeft = true)) return
                flashAcceptedBlink(isLeft = true)
                leftWinks += 1
                if (sequenceStartMs == 0L) sequenceStartMs = now
                onWinkCounted(isLeft = true)
            }
        }

        if (result.acceptRight) {
            if (startupSession.isActive && !startupSession.eyeControlEnabled) {
                startupSession.onRightWinkAccepted(closePeak = rightProb)
                flashAcceptedBlink(isLeft = false)
            } else if (settingsRecalibrationController.isActive &&
                settingsRecalibrationController.state.outcome == SettingsRecalibrationOutcome.InProgress
            ) {
                settingsRecalibrationController.onRightWinkAccepted(closePeak = rightProb)
                flashAcceptedBlink(isLeft = false)
            } else {
                if (rejectLessonWrongEyeBlink(isLeft = false)) return
                flashAcceptedBlink(isLeft = false)
                rightWinks += 1
                if (sequenceStartMs == 0L) sequenceStartMs = now
                onWinkCounted(isLeft = false)
            }
        }

        if (startupSession.isActive && !startupSession.eyeControlEnabled) {
            return
        }
        // RC8.12 — while recalibration is InProgress, winks feed the calibration engine only.
        // Failed Retry (L1 R1) must fall through so blink matches touch onRetry.
        if (settingsRecalibrationController.isActive &&
            settingsRecalibrationController.state.outcome == SettingsRecalibrationOutcome.InProgress
        ) {
            // During BlinkThreeTimes, both-eye blinks are observed via onFrameSample.
            // Left/right wink steps are handled above; do not route into menu navigation.
            return
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
            // Layered scopes (Main Menu, Settings, …) must never show Communication phrase
            // previews or WAITING phrase feedback while they own blink input.
            if (!ModeScopedGestureAuthority.communicationPhraseFeedbackActive(buildGestureContext())) {
                setCommunicationState(LisaCommunicationState.BuildingMessage)
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

        if (!ModeScopedGestureAuthority.communicationPhraseFeedbackActive(buildGestureContext())) {
            setCommunicationState(LisaCommunicationState.BuildingMessage)
            return
        }

        setCommunicationState(LisaCommunicationState.WaitingForNextWink)
        maybeSpeakLongPauseEncouragement(idleMs)
    }

    private fun finalizeSequence() {
        val capturedLeft = leftWinks
        val capturedRight = rightWinks
        val capturedOrder = currentBlinkOrder()

        if (startupSession.isActive &&
            uiStartupState.value.phase ==
            com.idworx.lisa.features.intelligentstartup.model.StartupPhase.ProfileSelection
        ) {
            if (startupSession.handleProfileSelectionSequence(capturedLeft, capturedRight)) {
                resetSequence()
                updateReadyOrWaitingState()
                return
            }
        }

        if (!isSequenceEligibleForSpeech(capturedLeft, capturedRight)) {
            resetSequence()
            updateReadyOrWaitingState()
            return
        }

        // RC8.12 — Settings recalibration Failed Retry shares touch onRetry (L1 R1).
        if (settingsRecalibrationController.isActive &&
            settingsRecalibrationController.state.outcome == SettingsRecalibrationOutcome.Failed &&
            com.idworx.lisa.features.universalsequenceexecution.SettingsRecalibrationRetrySequenceAuthority
                .matches(capturedLeft, capturedRight)
        ) {
            com.idworx.lisa.features.universalsequenceexecution.SettingsRecalibrationRetrySequenceAuthority
                .invokeRetry { settingsRecalibrationController.retry() }
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

        // RC8.12 — every training phase that shows sequence-labelled actions (including Setup
        // readiness) must share handleTrainingSequence with touch. Navigation already returned.
        if (trainingSession.shouldShowTraining()) {
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
            GestureRoutingTarget.MainMenu -> {
                handleMainMenuSequence(capturedLeft, capturedRight)
                return
            }
            GestureRoutingTarget.MainMenuDestination -> {
                handleMenuDestinationSequence(capturedLeft, capturedRight)
                return
            }
            GestureRoutingTarget.ScopeUnmatched -> {
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

        // Hard isolation: never evaluate Communication phrases while a layered panel owns input.
        if (!ModeScopedGestureAuthority.communicationPhraseFeedbackActive(buildGestureContext())) {
            resetSequence()
            setCommunicationState(LisaCommunicationState.Listening)
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
