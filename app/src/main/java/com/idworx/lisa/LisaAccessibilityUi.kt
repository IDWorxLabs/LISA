package com.idworx.lisa

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.features.onboardingguide.model.TrainingPreferences
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.features.onboardingguide.state.GuidedTrainingUiState
import com.idworx.lisa.features.onboardingguide.state.TrainingEvent
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceMode
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceLessonCardDock
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceTrainingSpec
import com.idworx.lisa.features.onboardingguide.ui.GuidedTrainingFlow
import com.idworx.lisa.features.onboardingguide.ui.TrainingEyeTrackingState
import com.idworx.lisa.features.onboardingguide.ui.GuidedWorkspaceLessonCard
import com.idworx.lisa.features.onboardingguide.ui.trainingBlocksMainUi
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.eyetrackingstatus.UniversalEyeTrackingHeader
import com.idworx.lisa.ui.theme.LisaBlue
import com.idworx.lisa.ui.theme.LisaBlueDark
import com.idworx.lisa.ui.theme.LisaBlueLight
import com.idworx.lisa.ui.theme.LisaEmergencyRed
import com.idworx.lisa.ui.theme.LisaGray
import com.idworx.lisa.ui.theme.LisaSoftGray
import com.idworx.lisa.ui.theme.LisaStatusGreen
import com.idworx.lisa.ui.theme.LisaWhite
import com.idworx.lisa.ui.theme.LisaWorkspaceVisualStyle
import com.idworx.lisa.ui.theme.lisaFocusEmphasis
import java.util.Locale

@Composable
fun LisaRootUI(
    uiStrings: LisaUiStrings,
    appVersionInfo: LisaAppVersionInfo,
    userDisplay: LisaUserDisplay,
    emergencyActive: Boolean,
    developerMode: Boolean,
    activePanel: LisaPanel,
    lastSpoken: String,
    countdownActive: Boolean,
    sensitivityLevel: Int,
    responseTimeSec: Int = SequenceProcessingDelay.DEFAULT_SECONDS,
    settingsState: LisaSettingsUiState,
    textSizeScale: Float = 1.0f,
    profiles: List<LisaUserProfile> = emptyList(),
    activeProfileId: String = "",
    developerInfo: DeveloperPanelInfo,
    onMenuClick: () -> Unit,
    onSelectPanel: (LisaPanel) -> Unit,
    onClosePanel: () -> Unit,
    onBackToMenu: () -> Unit,
    mainMenuState: MainMenuNavigationState = MainMenuNavigationState(),
    onMainMenuMoveUp: () -> Unit = {},
    onMainMenuMoveDown: () -> Unit = {},
    onMainMenuPreviousPage: () -> Unit = {},
    onMainMenuNextPage: () -> Unit = {},
    onMainMenuSelect: () -> Unit = {},
    onMainMenuSelectDestination: (MainMenuDestination) -> Unit = {},
    onMainMenuViewportMetrics: (viewportHeightPx: Int, maxScrollPx: Int, scrollPx: Int) -> Unit = { _, _, _ -> },
    onMainMenuEmergency: () -> Unit = {},
    menuDestinationBinding: MenuDestinationUiBinding? = null,
    feedbackDraft: MenuFeedbackDraft = MenuFeedbackDraft(),
    onFeedbackDraftChange: (MenuFeedbackDraft) -> Unit = {},
    onOpenCreatePhrase: () -> Unit = {},
    onOpenPhraseEditor: () -> Unit = {},
    onPreviewCaregiverPhrase: (String) -> Unit = {},
    onReturnToCommunication: () -> Unit = {},
    customPhrases: List<WinkMapping> = emptyList(),
    phraseManagementState: PhraseManagementUiState = PhraseManagementUiState(),
    onSelectCustomPhrase: (CustomPhraseIdentity) -> Unit = {},
    onPhraseManagementBackToList: () -> Unit = {},
    onPhraseManagementOpenEdit: () -> Unit = {},
    onPhraseManagementOpenMove: () -> Unit = {},
    onPhraseManagementOpenDelete: () -> Unit = {},
    onPhraseManagementEditTextChange: (String) -> Unit = {},
    onPhraseManagementSaveEdit: () -> Unit = {},
    onPhraseManagementSelectMoveCategory: (CustomPhraseEngine.CaregiverPhraseCategory) -> Unit = {},
    onPhraseManagementConfirmMove: () -> Unit = {},
    onPhraseManagementConfirmDelete: () -> Unit = {},
    onPhraseManagementCancelSubScreen: () -> Unit = {},
    onPhraseManagementScrollUp: () -> Unit = {},
    onPhraseManagementScrollDown: () -> Unit = {},
    onOpenDeviceCheck: () -> Unit = {},
    onOpenDeveloperTools: () -> Unit = {},
    onDeveloperModeChange: (Boolean) -> Unit,
    onSensitivityDecrease: () -> Unit,
    onSensitivityIncrease: () -> Unit,
    onResponseTimeDecrease: () -> Unit = {},
    onResponseTimeIncrease: () -> Unit = {},
    onSettingsPlaceholderChange: (LisaSettingsUiState) -> Unit,
    onCreateProfile: () -> Unit = {},
    onSelectProfile: (String) -> Unit = {},
    onUpdateProfile: (LisaUserProfile) -> Unit = {},
    onDeleteProfile: (String) -> Unit = {},
    onRepeat: () -> Unit,
    onReset: () -> Unit,
    onEditCountdown: () -> Unit,
    onboardingCompleted: Boolean = true,
    cameraPermissionGranted: Boolean = true,
    cameraPermissionPermanentlyDenied: Boolean = false,
    primaryUserName: String = "Primary User",
    testingChecklist: Map<String, Boolean> = emptyMap(),
    feedbackSavedCount: Int = 0,
    onPrimaryUserNameChange: (String) -> Unit = {},
    onCompleteOnboarding: () -> Unit = {},
    onRequestCameraPermission: () -> Unit = {},
    onOpenAppSettings: () -> Unit = {},
    onSaveFeedback: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onToggleChecklistItem: (String, Boolean) -> Unit = { _, _ -> },
    voiceSettingsState: LisaVoiceSettingsState = LisaVoiceSettingsState(),
    onSelectTtsVoice: (String) -> Unit = {},
    onTestTtsVoice: () -> Unit = {},
    onInstallTtsVoiceData: () -> Unit = {},
    onOpenTtsSettings: () -> Unit = {},
    quickControlsOpen: Boolean = false,
    practiceModeOpen: Boolean = false,
    practiceItemIndex: Int = 0,
    practiceFeedback: PracticeFeedback? = null,
    listeningPaused: Boolean = false,
    onResponseSpeedChange: (ResponseSpeed) -> Unit = {},
    onQuickControlsClose: () -> Unit = {},
    onQuickControlsDecreaseSensitivity: () -> Unit = {},
    onQuickControlsIncreaseSensitivity: () -> Unit = {},
    onQuickControlsRepeat: () -> Unit = {},
    onQuickControlsTogglePause: () -> Unit = {},
    onQuickControlsOpenPractice: () -> Unit = {},
    onPracticeClose: () -> Unit = {},
    guidedNavigationState: GuidedNavigationState = GuidedNavigationState(),
    guidedCategoryPage: GuidedCategoryPage? = null,
    guidedCategoryMenuTitles: List<String> = emptyList(),
    guidedConfirmedPhrase: String? = null,
    guidedConfirmedLeft: Int? = null,
    guidedConfirmedRight: Int? = null,
    onGuidedNavigateUp: () -> Unit = {},
    onGuidedSelectEnter: () -> Unit = {},
    onGuidedCancelSaveConfirmation: () -> Unit = {},
    onGuidedBack: () -> Unit = {},
    onGuidedNavigateDown: () -> Unit = {},
    onGuidedEmergency: () -> Unit = {},
    onGuidedCategories: () -> Unit = {},
    onGuidedPreviousCategoryPage: () -> Unit = {},
    onGuidedNextCategoryPage: () -> Unit = {},
    onGuidedDecreaseValue: () -> Unit = {},
    onGuidedIncreaseValue: () -> Unit = {},
    onGuidedSettingsControl: (SettingsControlKind) -> Unit = {},
    onGuidedPhraseEntry: (GuidedVocabularyEntry) -> Unit = {},
    speechVolumeLevel: Int = SpeechVolumeAuthority.DEFAULT_LEVEL,
    speechSpeedLevel: Int = SpeechSpeedAuthority.DEFAULT_LEVEL,
    onGuidedCategoryRow: (Int) -> Unit = {},
    onGuidedCategoryViewportPageState: (pageCount: Int, currentPage: Int) -> Unit = { _, _ -> },
    phraseComposerState: PhraseComposerState = PhraseComposerController.initialState(),
    phraseComposerActive: Boolean = false,
    composerEyeFeedback: ComposerEyeFeedback = ComposerEyeFeedback(
        eyeTrackingBanner = EyeTrackingBannerContext(),
        leftWinkCount = 0,
        rightWinkCount = 0,
        sensitivityLevel = DEFAULT_SENSITIVITY_LEVEL,
        responseTimeSec = SequenceProcessingDelay.DEFAULT_SECONDS
    ),
    onPhraseComposerEntry: (PhraseComposerEntry) -> Unit = {},
    onPhraseComposerCommand: (PhraseComposerEntry) -> Unit = {},
    onPhraseComposerKeyTouched: (row: Int, col: Int) -> Unit = { _, _ -> },
    onPhraseComposerEmergency: () -> Unit = {},
    onCancelOrStopEmergency: () -> Unit = {},
    onDecreaseEmergencyAlarmVolume: () -> Unit = {},
    onIncreaseEmergencyAlarmVolume: () -> Unit = {},
    hasSavedEyeCalibration: Boolean = false,
    settingsRecalibrationState: SettingsRecalibrationState = SettingsRecalibrationState(),
    onSettingsRecalibrationRetry: () -> Unit = {},
    onSettingsRecalibrationCancel: () -> Unit = {},
    guidedTrainingActive: Boolean = false,
    guidedTrainingState: GuidedTrainingUiState = GuidedTrainingUiState(),
    guidedTrainingSetupStep: Int = 0,
    guidedTrainingReturningUser: Boolean = false,
    trainingEyeTracking: TrainingEyeTrackingState = TrainingEyeTrackingState(),
    eyeTrackingStatus: com.idworx.lisa.features.eyetrackingstatus.EyeTrackingStatusUiState =
        com.idworx.lisa.features.eyetrackingstatus.EyeTrackingStatusUiState(),
    trainingBlinkDiagnostics: com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionDiagnostics =
        com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionDiagnostics(),
    showBlinkDiagnostics: Boolean = false,
    onTrainingEvent: (TrainingEvent) -> Unit = {},
    onTrainingWelcomeNarration: () -> Unit = {},
    onTrainingFirstLaunchNarration: () -> Unit = {},
    onTrainingSkipConfirmNarration: () -> Unit = {},
    onTrainingCompletionNarration: () -> Unit = {},
    onTrainingLessonNarration: (String, String) -> Unit = { _, _ -> },
    onTrainingNavigationNarration: (String, String) -> Unit = { _, _ -> },
    onTrainingSetupStepChange: (Int) -> Unit = {},
    onTrainingCalibrationStarted: () -> Unit = {},
    onTrainingAdvanceCalibrationDot: () -> Unit = {},
    onTrainingTouchLeftWink: () -> Unit = {},
    onTrainingTouchRightWink: () -> Unit = {},
    onTrainingReduceSensitivity: () -> Unit = {},
    onTrainingIncreaseSensitivity: () -> Unit = {},
    guidedTrainingSensitivityLevel: Int = DEFAULT_SENSITIVITY_LEVEL,
    onTrainingDecreaseResponseTime: () -> Unit = {},
    onTrainingIncreaseResponseTime: () -> Unit = {},
    onTrainingReplayTutorial: () -> Unit = {},
    onTrainingPracticeCommunication: () -> Unit = {},
    onTrainingPracticeNavigation: () -> Unit = {},
    onTrainingResetProgress: () -> Unit = {},
    onTrainingPreferencesChange: (TrainingPreferences) -> Unit = {},
    intelligentStartupActive: Boolean = false,
    intelligentStartupState: com.idworx.lisa.features.intelligentstartup.model.StartupFlowState =
        com.idworx.lisa.features.intelligentstartup.model.StartupFlowState(isActive = false),
    onIntelligentStartupCalibrationTimeout: () -> Unit = {},
    onIntelligentStartupCreateDraftChange: (String?, String?, String?) -> Unit = { _, _, _ -> },
    onIntelligentStartupConfirmCreatePrimary: () -> Unit = {},
    onIntelligentStartupSelectProfileIndex: (Int) -> Unit = {},
    onIntelligentStartupConfirmSelectedProfile: () -> Unit = {},
    cameraView: @Composable () -> Unit
) {
    if (intelligentStartupActive) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (cameraPermissionGranted) {
                Box(modifier = Modifier.matchParentSize().alpha(0f)) {
                    cameraView()
                }
            }
            com.idworx.lisa.features.intelligentstartup.ui.IntelligentStartupFlow(
                state = intelligentStartupState,
                uiStrings = uiStrings,
                cameraPermissionGranted = cameraPermissionGranted,
                cameraView = {},
                eyeTrackingStatus = eyeTrackingStatus,
                onCalibrationTimeout = onIntelligentStartupCalibrationTimeout,
                onRequestCameraPermission = onRequestCameraPermission,
                onCreateDraftChange = onIntelligentStartupCreateDraftChange,
                onConfirmCreatePrimaryUser = onIntelligentStartupConfirmCreatePrimary,
                onSelectProfileIndex = onIntelligentStartupSelectProfileIndex,
                onConfirmSelectedProfile = onIntelligentStartupConfirmSelectedProfile,
                onDecreaseSensitivity = onSensitivityDecrease,
                onIncreaseSensitivity = onSensitivityIncrease,
                onDecreaseResponseTime = onResponseTimeDecrease,
                onIncreaseResponseTime = onResponseTimeIncrease
            )
        }
        return
    }
    if (guidedTrainingActive && trainingBlocksMainUi(guidedTrainingState.phase)) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (cameraPermissionGranted) {
                Box(modifier = Modifier.matchParentSize().alpha(0f)) {
                    cameraView()
                }
            }
            GuidedTrainingFlow(
                state = guidedTrainingState,
                uiStrings = uiStrings,
                language = uiStrings.language,
                primaryUserName = primaryUserName,
                isReturningUser = guidedTrainingReturningUser,
                onEvent = onTrainingEvent,
                onWelcomeNarration = onTrainingWelcomeNarration,
                onFirstLaunchNarration = onTrainingFirstLaunchNarration,
                onSkipConfirmNarration = onTrainingSkipConfirmNarration,
                onCompletionNarration = onTrainingCompletionNarration,
                onLessonNarration = onTrainingLessonNarration,
                onNavigationNarration = onTrainingNavigationNarration,
                onPrimaryUserNameChange = onPrimaryUserNameChange,
                onRequestCameraPermission = onRequestCameraPermission,
                onTouchLeftWink = onTrainingTouchLeftWink,
                onTouchRightWink = onTrainingTouchRightWink,
                onReduceSensitivity = onTrainingReduceSensitivity,
                onIncreaseSensitivity = onTrainingIncreaseSensitivity,
                sensitivityLevel = guidedTrainingSensitivityLevel,
                onDecreaseResponseTime = onTrainingDecreaseResponseTime,
                onIncreaseResponseTime = onTrainingIncreaseResponseTime,
                setupStep = guidedTrainingSetupStep,
                onSetupStepChange = onTrainingSetupStepChange,
                eyeTracking = trainingEyeTracking,
                eyeTrackingStatus = eyeTrackingStatus,
                blinkDiagnostics = trainingBlinkDiagnostics,
                showBlinkDiagnostics = showBlinkDiagnostics,
                onCalibrationStarted = onTrainingCalibrationStarted,
                onAdvanceCalibrationDot = onTrainingAdvanceCalibrationDot
            )
        }
        return
    }

    if (!onboardingCompleted && !guidedTrainingActive) {
        OnboardingFlow(
            uiStrings = uiStrings,
            primaryUserName = primaryUserName,
            onPrimaryUserNameChange = onPrimaryUserNameChange,
            onRequestCameraPermission = onRequestCameraPermission,
            onComplete = onCompleteOnboarding
        )
        return
    }

    val density = LocalDensity.current
    // Guided Training Mode — navigation lessons teach the real Communication Workspace, so the
    // workspace overlay must be visible even though onboarding has not finished yet.
    val guidedWorkspaceTrainingActive =
        guidedTrainingActive && guidedTrainingState.phase == TrainingPhase.NavigationLesson
    // RC7D.17A — Phrase Management must occupy the main workspace slot like the composer.
    // Hosting it under Menu/Reset (after GuidedVocabularyOverlay weight(1f)) clipped the
    // fixed Scroll/Back/Emergency controls off the physical-device screen.
    val phraseManagementActive = PhraseManagementController.occupiesMainContentSlot(activePanel)
    // RC7D.29 — Main Menu occupies the same central content slot; never leave the guided
    // workspace painted behind a partial bottom sheet.
    val mainMenuActive = MainMenuProductionUiAuthority.occupiesMainContentSlot(activePanel)
    val menuDestinationActive =
        MenuDestinationProductionUiAuthority.occupiesMainContentSlot(activePanel)
    val showSharedBlinkStatusHeader =
        PhraseManagementController.showSharedBlinkStatusHeader(phraseComposerActive)
    val showGuidedVocabularyOverlay = GuidedVocabularyOverlayVisibility.shouldShowOverlay(
        onboardingCompleted = onboardingCompleted,
        cameraPermissionGranted = cameraPermissionGranted,
        emergencyActive = emergencyActive,
        practiceModeOpen = practiceModeOpen,
        quickControlsOpen = quickControlsOpen,
        guidedWorkspaceTrainingActive = guidedWorkspaceTrainingActive
    ) && !menuDestinationActive && MainMenuProductionUiAuthority.showGuidedVocabularyOverlay(
        activePanel = activePanel,
        phraseComposerActive = phraseComposerActive,
        phraseManagementActive = phraseManagementActive
    )
    val emergencyAwaitingConfirm = emergencyAwaitingConfirm(guidedTrainingState.brain1Decision)
    val composerInputSuspended = emergencyActive || emergencyAwaitingConfirm
    val activeNavigationLesson = if (guidedWorkspaceTrainingActive) {
        TrainingLessonCatalog.navigationLessonAt(guidedTrainingState.progress.navigationLessonIndex)
    } else {
        null
    }
    val guidedWorkspaceHighlight = activeNavigationLesson?.let {
        GuidedWorkspaceTrainingSpec.highlightTargetFor(it.action)
    }
    // The floating card's "Select a phrase" gesture hint must be the *actual* highlighted
    // phrase entry's own code — the exact same entry (first visible row) that
    // GuidedVocabularyOverlay renders and MainActivity's lesson-focus gate validates against —
    // so the card can never show a gesture that differs from the real workspace row.
    val guidedHighlightedPhraseGesture = if (activeNavigationLesson?.action == NavigationAction.SelectPhrase) {
        val configuration = LocalConfiguration.current
        val visibleEntryCap = GuidedVocabularyCatalog.visibleEntryCount(
            configuration.screenWidthDp,
            configuration.screenHeightDp
        )
        GuidedNavigationController.visiblePhraseEntries(
            entries = guidedCategoryPage?.entries.orEmpty(),
            phrasePageIndex = guidedNavigationState.phrasePageIndex,
            visibleCap = visibleEntryCap
        ).firstOrNull()?.sequenceLabel
    } else {
        null
    }
    Box(modifier = Modifier.fillMaxSize()) {
        if (!emergencyActive) {
        if (cameraPermissionGranted) {
            cameraView()
        } else {
            CameraPermissionScreen(
                uiStrings = uiStrings,
                permanentlyDenied = cameraPermissionPermanentlyDenied,
                onRequestPermission = onRequestCameraPermission,
                onOpenSettings = onOpenAppSettings
            )
        }

        if (practiceModeOpen) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                PracticeModeOverlay(
                    uiStrings = uiStrings,
                    language = uiStrings.language,
                    itemIndex = practiceItemIndex,
                    feedback = practiceFeedback,
                    visible = true,
                    onClose = onPracticeClose
                )
            }
        }

        if (quickControlsOpen) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                QuickControlsOverlay(
                    uiStrings = uiStrings,
                    responseSpeed = settingsState.responseSpeed,
                    listeningPaused = listeningPaused,
                    onSelectSpeed = onResponseSpeedChange,
                    onDecreaseSensitivity = onQuickControlsDecreaseSensitivity,
                    onIncreaseSensitivity = onQuickControlsIncreaseSensitivity,
                    onRepeatLastPhrase = onQuickControlsRepeat,
                    onTogglePause = onQuickControlsTogglePause,
                    onOpenPractice = onQuickControlsOpenPractice,
                    onClose = onQuickControlsClose,
                    visible = true
                )
            }
        }

        if (listeningPaused && !quickControlsOpen && !practiceModeOpen) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(LisaBlueLight)
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiStrings.listeningPaused,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = LisaBlueDark,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = uiStrings.tapOrWinkToResume,
                        fontSize = 12.sp,
                        color = LisaGray
                    )
                }
            }
        }

        CompositionLocalProvider(
            LocalDensity provides Density(density.density, density.fontScale * textSizeScale)
        ) {
        Column(modifier = Modifier.fillMaxSize()) {
        // RC7D.18 — keep the canonical blink-status header while Phrase Management / Details
        // occupy the central slot. Only the eye-keyboard composer hides this shell chrome.
        if (showSharedBlinkStatusHeader) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = if (phraseManagementActive) 4.dp else 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (userDisplay.showIntentPreview &&
                userDisplay.phrase != null &&
                !ModeScopedGestureAuthority.suspendsCommunicationPhraseProcessing(activePanel)
            ) {
                IntentPreviewCard(
                    phrase = userDisplay.phrase,
                    compact = !countdownActive,
                    speaking = userDisplay.timelineStage == CommunicationTimelineStage.Speaking,
                    uiStrings = uiStrings
                )
                Spacer(Modifier.height(4.dp))
            }

            // RC8.11 — UniversalEyeTrackingHeader owns passive eye-tracking banners
            // ("Watching your eyes", calibrating, no face, etc.). EverydayCommunicationPanel
            // only renders active communication timeline content (phrase / countdown / progress).
            val passiveEyeTrackingOwnedByUniversalHeader =
                !countdownActive &&
                    userDisplay.phrase == null &&
                    userDisplay.countdown == null &&
                    !userDisplay.showCountdownHints &&
                    userDisplay.subtitle.isBlank() &&
                    isPassiveEyeTrackingHeadline(userDisplay.headline, uiStrings)
            if (!passiveEyeTrackingOwnedByUniversalHeader) {
                EverydayCommunicationPanel(
                    uiStrings = uiStrings,
                    userDisplay = userDisplay,
                    countdownActive = countdownActive,
                    onEditCountdown = onEditCountdown
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Universal Communication-style header: blue status + dark panel with live
            // Left/Right counts, Sensitivity, and Response time (shared with Welcome / Guided Learning).
            UniversalEyeTrackingHeader(
                uiStrings = uiStrings,
                statusText = when {
                    passiveEyeTrackingOwnedByUniversalHeader ->
                        userDisplay.headline.ifBlank { uiStrings.eyeTrackingStatusWatching }
                    else -> uiStrings.eyeTrackingStatusWatching
                },
                leftBlinkCount = userDisplay.leftWinkDots,
                rightBlinkCount = userDisplay.rightWinkDots,
                sensitivityLevel = sensitivityLevel,
                responseTimeSec = responseTimeSec,
                onDecreaseSensitivity = onSensitivityDecrease,
                onIncreaseSensitivity = onSensitivityIncrease,
                onDecreaseResponseTime = onResponseTimeDecrease,
                onIncreaseResponseTime = onResponseTimeIncrease,
                guidedResponseTimeControlsVisible = guidedWorkspaceTrainingActive,
                guidedResponseTimeSec = guidedTrainingState.progress.preferences.guidedResponseTimeSec,
                onDecreaseGuidedResponseTime = onTrainingDecreaseResponseTime,
                onIncreaseGuidedResponseTime = onTrainingIncreaseResponseTime,
                compact = phraseManagementActive
            )

            if (developerMode) {
                Spacer(Modifier.height(4.dp))
                DeveloperPanel(info = developerInfo)
            }
        }
        }

        if (showGuidedVocabularyOverlay) {
        GuidedVocabularyOverlay(
            uiStrings = uiStrings,
            navigationState = guidedNavigationState,
            categoryPage = guidedCategoryPage,
            categoryMenuTitles = guidedCategoryMenuTitles,
            confirmedPhrase = guidedConfirmedPhrase,
            confirmedLeft = guidedConfirmedLeft,
            confirmedRight = guidedConfirmedRight,
            visible = showGuidedVocabularyOverlay,
            emergencyAwaitingConfirm = emergencyAwaitingConfirm,
            sensitivityLevel = sensitivityLevel,
            responseTimeSec = responseTimeSec,
            speechVolumeLevel = speechVolumeLevel,
            speechSpeedLevel = speechSpeedLevel,
            listeningPaused = listeningPaused,
            onNavigateUp = onGuidedNavigateUp,
            onSelectEnter = onGuidedSelectEnter,
            onCancelSaveConfirmation = onGuidedCancelSaveConfirmation,
            onBack = onGuidedBack,
            onNavigateDown = onGuidedNavigateDown,
            onEmergency = onGuidedEmergency,
            onCategories = onGuidedCategories,
            onPreviousCategoryPage = onGuidedPreviousCategoryPage,
            onNextCategoryPage = onGuidedNextCategoryPage,
            onDecreaseValue = onGuidedDecreaseValue,
            onIncreaseValue = onGuidedIncreaseValue,
            onSettingsControl = onGuidedSettingsControl,
            onPhraseEntry = onGuidedPhraseEntry,
            onCategoryRow = onGuidedCategoryRow,
            onCategoryViewportPageState = onGuidedCategoryViewportPageState,
            workspaceMode = if (guidedWorkspaceTrainingActive) {
                GuidedWorkspaceMode.GUIDED_TRAINING
            } else {
                GuidedWorkspaceMode.NORMAL
            },
            trainingHighlight = guidedWorkspaceHighlight,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        }

        // RC7D.29 — Main Menu replaces the workspace content region (full-screen below header).
        if (mainMenuActive) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(
                        horizontal = LisaWorkspaceVisualStyle.FullWidthChromeHorizontalPadding,
                        vertical = 6.dp
                    )
                    .clip(RoundedCornerShape(LisaWorkspaceVisualStyle.PanelCornerRadius))
                    .background(MainMenuProductionUiAuthority.solidWorkspaceBackground())
            ) {
                MenuPanel(
                    uiStrings = uiStrings,
                    mainMenuState = mainMenuState,
                    onMoveUp = onMainMenuMoveUp,
                    onMoveDown = onMainMenuMoveDown,
                    onPreviousPage = onMainMenuPreviousPage,
                    onNextPage = onMainMenuNextPage,
                    onSelect = onMainMenuSelect,
                    onSelectDestination = onMainMenuSelectDestination,
                    onViewportMetrics = onMainMenuViewportMetrics,
                    onEmergency = onMainMenuEmergency,
                    onClose = onClosePanel,
                    fillWorkspace = true
                )
            }
        }

        if (menuDestinationActive && menuDestinationBinding != null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(
                        horizontal = LisaWorkspaceVisualStyle.FullWidthChromeHorizontalPadding,
                        vertical = 6.dp
                    )
            ) {
                MenuDestinationWorkspace(
                    uiStrings = uiStrings,
                    binding = menuDestinationBinding
                ) {
                    when (activePanel) {
                        LisaPanel.MyCommunication -> MyCommunicationPanel(
                            uiStrings = uiStrings,
                            profiles = profiles,
                            activeProfileId = activeProfileId,
                            onCreateProfile = onCreateProfile,
                            onSelectProfile = onSelectProfile,
                            onUpdateProfile = onUpdateProfile,
                            onDeleteProfile = onDeleteProfile,
                            onBack = onBackToMenu
                        )
                        LisaPanel.Voice -> VoiceHomePanel(
                            uiStrings = uiStrings,
                            onOpenDeviceVoice = { onSelectPanel(LisaPanel.VoiceDevice) },
                            onOpenPremiumVoices = { onSelectPanel(LisaPanel.VoicePremium) },
                            onOpenMyVoice = { onSelectPanel(LisaPanel.VoiceMyVoice) },
                            onOpenFamilyVoice = { onSelectPanel(LisaPanel.VoiceFamily) },
                            onBack = onBackToMenu
                        )
                        LisaPanel.VoiceDevice -> DeviceVoicePanel(
                            uiStrings = uiStrings,
                            state = voiceSettingsState,
                            onSelectVoice = onSelectTtsVoice,
                            onTestVoice = onTestTtsVoice,
                            onInstallVoiceData = onInstallTtsVoiceData,
                            onOpenTtsSettings = onOpenTtsSettings,
                            onBack = onBackToMenu
                        )
                        LisaPanel.VoicePremium -> PremiumVoicesPanel(
                            uiStrings = uiStrings,
                            onBack = onBackToMenu
                        )
                        LisaPanel.VoiceMyVoice -> MyVoicePanel(
                            uiStrings = uiStrings,
                            onBack = onBackToMenu
                        )
                        LisaPanel.VoiceFamily -> FamilyVoicePanel(
                            uiStrings = uiStrings,
                            onBack = onBackToMenu
                        )
                        LisaPanel.Settings -> SettingsPanel(
                            uiStrings = uiStrings,
                            settingsState = settingsState,
                            speechVolumeLevel = speechVolumeLevel,
                            speechSpeedLevel = speechSpeedLevel,
                            hasSavedCalibration = hasSavedEyeCalibration,
                            onBack = onBackToMenu
                        )
                        LisaPanel.Recalibration -> SettingsRecalibrationPanel(
                            uiStrings = uiStrings,
                            state = settingsRecalibrationState,
                            eyeTrackingStatus = eyeTrackingStatus,
                            onDecreaseSensitivity = onSensitivityDecrease,
                            onIncreaseSensitivity = onSensitivityIncrease,
                            onDecreaseResponseTime = onResponseTimeDecrease,
                            onIncreaseResponseTime = onResponseTimeIncrease,
                            onRetry = onSettingsRecalibrationRetry,
                            onCancel = onSettingsRecalibrationCancel
                        )
                        LisaPanel.DeveloperTools -> DeveloperToolsPanel(
                            uiStrings = uiStrings,
                            developerMode = developerMode,
                            onDeveloperModeChange = onDeveloperModeChange,
                            onBack = onBackToMenu
                        )
                        LisaPanel.AboutLisa -> AboutLisaPanel(
                            uiStrings = uiStrings,
                            appVersionInfo = appVersionInfo,
                            onBack = onBackToMenu
                        )
                        LisaPanel.PrivacyPolicy -> PrivacyPolicyPanel(
                            uiStrings = uiStrings,
                            onBack = onBackToMenu
                        )
                        LisaPanel.Feedback -> FeedbackPanel(
                            uiStrings = uiStrings,
                            savedCount = feedbackSavedCount,
                            draft = feedbackDraft,
                            onDraftChange = onFeedbackDraftChange,
                            onSaveFeedback = onSaveFeedback,
                            onBack = onBackToMenu
                        )
                        LisaPanel.TestingChecklist -> TestingChecklistPanel(
                            uiStrings = uiStrings,
                            checklist = testingChecklist,
                            onToggleItem = onToggleChecklistItem,
                            onBack = onBackToMenu
                        )
                        LisaPanel.ReleaseNotes -> ReleaseNotesPanel(
                            uiStrings = uiStrings,
                            appVersionInfo = appVersionInfo,
                            onBack = onBackToMenu
                        )
                        else -> Unit
                    }
                }
            }
        }

        if (phraseManagementActive) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                VocabularyManagementPanel(
                    uiStrings = uiStrings,
                    customPhrases = customPhrases,
                    managementState = phraseManagementState,
                    onSelectPhrase = onSelectCustomPhrase,
                    onBackToList = onPhraseManagementBackToList,
                    onBackToMenu = onBackToMenu,
                    onOpenEdit = onPhraseManagementOpenEdit,
                    onOpenMove = onPhraseManagementOpenMove,
                    onOpenDeleteConfirm = onPhraseManagementOpenDelete,
                    onEditTextChange = onPhraseManagementEditTextChange,
                    onSaveEdit = onPhraseManagementSaveEdit,
                    onSelectMoveCategory = onPhraseManagementSelectMoveCategory,
                    onConfirmMove = onPhraseManagementConfirmMove,
                    onConfirmDelete = onPhraseManagementConfirmDelete,
                    onCancelSubScreen = onPhraseManagementCancelSubScreen,
                    onScrollUp = onPhraseManagementScrollUp,
                    onScrollDown = onPhraseManagementScrollDown,
                    onEmergency = onPhraseComposerEmergency
                )
            }
        }

        EyeControlledPhraseComposerOverlay(
            uiStrings = uiStrings,
            state = phraseComposerState,
            visible = phraseComposerActive,
            composerEyeFeedback = composerEyeFeedback,
            inputSuspended = composerInputSuspended,
            onSensitivityDecrease = onSensitivityDecrease,
            onSensitivityIncrease = onSensitivityIncrease,
            onResponseTimeDecrease = onResponseTimeDecrease,
            onResponseTimeIncrease = onResponseTimeIncrease,
            onEmergency = onPhraseComposerEmergency,
            onEntrySelected = onPhraseComposerEntry,
            onCommandSelected = onPhraseComposerCommand,
            onKeyTouched = onPhraseComposerKeyTouched,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        if (MainMenuProductionUiAuthority.showWorkspaceBottomChrome(phraseComposerActive) &&
            !menuDestinationActive
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = LisaWorkspaceVisualStyle.FullWidthChromeHorizontalPadding,
                    vertical = 10.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (mainMenuActive) {
                WorkspaceFullWidthActionButton(
                    label = uiStrings.close,
                    sequenceLabel = MainMenuProductionUiAuthority.closeMenuSequenceLabel(),
                    onClick = onClosePanel
                )
            } else {
                WorkspaceFullWidthActionButton(
                    label = uiStrings.menu,
                    sequenceLabel = MainMenuProductionUiAuthority.openMenuSequenceLabel(),
                    onClick = onMenuClick
                )
            }

            if (activePanel != LisaPanel.None && !phraseManagementActive && !mainMenuActive) {
                Spacer(Modifier.height(10.dp))
                when (activePanel) {
                    LisaPanel.Menu -> Unit
                    LisaPanel.MyCommunication -> Unit
                    LisaPanel.VocabularyTraining -> Unit
                    LisaPanel.CreatePhrase -> CreatePhrasePanel(
                        uiStrings = uiStrings,
                        onBegin = onOpenPhraseEditor,
                        onBack = onBackToMenu
                    )
                    LisaPanel.PhraseEditor -> Unit
                    LisaPanel.Voice,
                    LisaPanel.VoiceDevice,
                    LisaPanel.VoicePremium,
                    LisaPanel.VoiceMyVoice,
                    LisaPanel.VoiceFamily,
                    LisaPanel.Settings,
                    LisaPanel.Recalibration,
                    LisaPanel.DeveloperTools,
                    LisaPanel.AboutLisa,
                    LisaPanel.PrivacyPolicy,
                    LisaPanel.Feedback,
                    LisaPanel.TestingChecklist,
                    LisaPanel.ReleaseNotes -> Unit
                    LisaPanel.None -> Unit
                }
            }
        }
        }
        }
        }

        // Floating lesson card renders last so it is always drawn above the workspace and the
        // Listening/Watching-your-eyes banner at the top — never behind it. It docks above the
        // bottom Menu/Reset row, on whichever side keeps the highlighted control uncovered.
        if (guidedWorkspaceTrainingActive && activeNavigationLesson != null) {
            val lessonProgress = TrainingLessonCatalog.guidedLessonProgress(guidedTrainingState.progress)
            val cardDock = GuidedWorkspaceTrainingSpec.cardDockFor(guidedWorkspaceHighlight)
            val cardAlignment = if (cardDock == GuidedWorkspaceLessonCardDock.BottomStart) {
                Alignment.BottomStart
            } else {
                Alignment.BottomEnd
            }
            GuidedWorkspaceLessonCard(
                lessonNumber = lessonProgress?.first,
                totalLessons = lessonProgress?.second,
                title = GuidedWorkspaceTrainingSpec.lessonCardTitle(activeNavigationLesson.action, uiStrings),
                gestureLabel = GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(
                    activeNavigationLesson.action,
                    guidedHighlightedPhraseGesture
                ),
                feedbackMessage = guidedTrainingState.navigationFeedbackMessage,
                wrongGestureMessage = guidedTrainingState.navigationWrongGestureMessage,
                modifier = Modifier
                    .align(cardAlignment)
                    .padding(horizontal = 10.dp, vertical = 84.dp)
            )
        }
        }

        GlobalEmergencyOverlayLayer(
            uiStrings = uiStrings,
            emergencyActive = emergencyActive,
            emergencyAwaitingConfirm = emergencyAwaitingConfirm,
            blinkFeedback = composerEyeFeedback,
            emergencyAlarmVolume = settingsState.emergencyAlarmVolume,
            onDecreaseAlarmVolume = onDecreaseEmergencyAlarmVolume,
            onIncreaseAlarmVolume = onIncreaseEmergencyAlarmVolume,
            onCancelOrStopEmergency = onCancelOrStopEmergency,
            modifier = Modifier.fillMaxSize()
        )
    }
}

data class DeveloperPanelInfo(
    val leftEye: String,
    val rightEye: String,
    val leftCount: Int,
    val rightCount: Int,
    val leftFrameStreak: Int,
    val rightFrameStreak: Int,
    val closedThreshold: Float,
    val openThreshold: Float,
    val requiredFrames: Int,
    val sensitivityLevel: Int,
    val detectionState: String
)

@Composable
private fun IntentPreviewCard(
    phrase: String,
    compact: Boolean = false,
    speaking: Boolean = false,
    uiStrings: LisaUiStrings? = null
) {
    // RC8.0 — when speaking, show brief ✓ confirmation on the phrase (fade in/out, non-blocking).
    var speechConfirmVisible by remember(phrase, speaking) { mutableStateOf(speaking) }
    LaunchedEffect(phrase, speaking) {
        if (speaking) {
            speechConfirmVisible = true
            kotlinx.coroutines.delay(1000L)
            speechConfirmVisible = false
        } else {
            speechConfirmVisible = false
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (compact) 12.dp else 16.dp))
            .background(LisaWhite.copy(alpha = 0.94f))
            .padding(
                horizontal = if (compact) 14.dp else 18.dp,
                vertical = if (compact) 10.dp else 14.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = speechConfirmVisible,
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(220))
        ) {
            Text(
                text = uiStrings?.speechSpokenConfirmation(phrase) ?: "✓ $phrase",
                color = LisaStatusGreen,
                fontSize = if (compact) 18.sp else 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = if (compact) 22.sp else 28.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (!speechConfirmVisible) {
            Text(text = if (speaking) "🔊" else "💬", fontSize = if (compact) 22.sp else 28.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = phrase.uppercase(Locale.getDefault()),
                color = LisaBlueDark,
                fontSize = if (compact) 18.sp else 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = if (compact) 22.sp else 28.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * RC8.11 — Passive eye-tracking banner headlines owned exclusively by
 * [UniversalEyeTrackingHeader]. When true, EverydayCommunicationPanel must not
 * emit a second “Watching your eyes” (or related) status strip.
 */
private fun isPassiveEyeTrackingHeadline(headline: String, uiStrings: LisaUiStrings): Boolean {
    if (headline.isBlank()) return true
    return headline.equals(uiStrings.eyeTrackingStatusWatching, ignoreCase = true) ||
        headline.equals(uiStrings.eyeTrackingStatusCalibrating, ignoreCase = true) ||
        headline.equals(uiStrings.eyeTrackingStatusTrackingLost, ignoreCase = true) ||
        headline.equals(uiStrings.eyeTrackingStatusNoFace, ignoreCase = true) ||
        headline.equals(uiStrings.eyeTrackingStatusLookAtCamera, ignoreCase = true)
}

@Composable
private fun EverydayCommunicationPanel(
    uiStrings: LisaUiStrings,
    userDisplay: LisaUserDisplay,
    countdownActive: Boolean,
    onEditCountdown: () -> Unit
) {
    val expanded = countdownActive || userDisplay.countdown != null
    // RC8.0 — idle eye-tracking status is a calm horizontal strip so communication stays the focus.
    val calmEyeStatus = !expanded &&
        userDisplay.phrase == null &&
        userDisplay.timelineStage == CommunicationTimelineStage.Watching
    val statusReady = calmEyeStatus &&
        userDisplay.headline.equals(uiStrings.eyeTrackingStatusWatching, ignoreCase = true)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (calmEyeStatus) 8.dp else 10.dp))
            .background(
                when {
                    calmEyeStatus && statusReady -> LisaStatusGreen.copy(alpha = 0.14f)
                    calmEyeStatus -> LisaBlue.copy(alpha = 0.28f)
                    expanded -> LisaBlue.copy(alpha = 0.78f)
                    else -> LisaBlue.copy(alpha = 0.55f)
                }
            )
            .padding(
                horizontal = if (expanded) 14.dp else 10.dp,
                vertical = when {
                    calmEyeStatus -> 4.dp
                    expanded -> 12.dp
                    else -> 5.dp
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (calmEyeStatus) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (statusReady) LisaStatusGreen else LisaWhite.copy(alpha = 0.85f))
                )
                AnimatedContent(
                    targetState = userDisplay.headline,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                    },
                    label = "calm_headline",
                    modifier = Modifier.weight(1f)
                ) { headline ->
                    Text(
                        text = headline,
                        color = if (statusReady) LisaBlueDark else LisaWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Start,
                        lineHeight = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
        AnimatedContent(
            targetState = userDisplay.headline,
            transitionSpec = {
                fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(200))
            },
            label = "headline"
        ) { headline ->
            Text(
                text = headline,
                color = LisaWhite,
                fontSize = if (expanded) 22.sp else 15.sp,
                fontWeight = if (expanded) FontWeight.Bold else FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = if (expanded) 26.sp else 19.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (userDisplay.subtitle.isNotBlank()) {
            Spacer(Modifier.height(if (expanded) 6.dp else 2.dp))
            Text(
                text = userDisplay.subtitle,
                color = LisaWhite.copy(alpha = 0.95f),
                fontSize = if (expanded) 15.sp else 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = if (expanded) 18.sp else 15.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (userDisplay.phrase != null && !userDisplay.showIntentPreview) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "\"${userDisplay.phrase}\"",
                color = LisaWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (userDisplay.countdown != null) {
            Spacer(Modifier.height(16.dp))
            AnimatedContent(
                targetState = userDisplay.countdown,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150))
                },
                label = "countdown"
            ) { count ->
                Text(
                    text = count.toString(),
                    color = LisaWhite,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (userDisplay.showCountdownHints) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = uiStrings.leftWinkCancel,
                    color = LisaWhite.copy(alpha = 0.95f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = uiStrings.rightWinkSpeak,
                    color = LisaWhite.copy(alpha = 0.95f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (countdownActive) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onEditCountdown,
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = LisaWhite,
                        containerColor = Color.White.copy(alpha = 0.15f)
                    )
                ) {
                    Text(uiStrings.editSequence, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        }
    }
}

@Composable
private fun DeveloperPanel(info: DeveloperPanelInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(12.dp)
    ) {
        Text("Developer Mode", color = LisaWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        Text("LeftEye=${info.leftEye}", color = LisaWhite, fontSize = 12.sp, lineHeight = 16.sp)
        Text("RightEye=${info.rightEye}", color = LisaWhite, fontSize = 12.sp, lineHeight = 16.sp)
        Text("L=${info.leftCount}  R=${info.rightCount}", color = LisaWhite, fontSize = 12.sp, lineHeight = 16.sp)
        Text(
            "Frames: L-streak=${info.leftFrameStreak} R-streak=${info.rightFrameStreak}",
            color = LisaWhite,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
        Text(
            "Thresholds: closed=${"%.2f".format(info.closedThreshold)} open=${"%.2f".format(info.openThreshold)}",
            color = LisaWhite,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
        Text(
            "Required frames: ${info.requiredFrames}",
            color = LisaWhite,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
        Text(
            "Sensitivity: ${info.sensitivityLevel}",
            color = LisaWhite,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
        Text(
            "State: ${info.detectionState}",
            color = LisaWhite,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
internal fun LisaPanelShell(
    title: String,
    onBack: (() -> Unit)?,
    backLabel: String = "Back",
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val fillDestinationPane = LocalMenuDestinationScrollState.current != null
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (fillDestinationPane) Modifier.fillMaxHeight() else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LisaBlueLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (fillDestinationPane) Modifier.fillMaxHeight() else Modifier)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (title.isBlank()) Arrangement.End else Arrangement.SpaceBetween
            ) {
                if (title.isNotBlank()) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = LisaBlueDark,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (onBack != null) {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.widthIn(min = 76.dp)
                    ) {
                        Text(
                            backLabel,
                            color = LisaBlueDark,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (fillDestinationPane) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    content = content
                )
            } else {
                content()
            }
        }
    }
}

@Composable
internal fun PanelPurposeLine(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        fontSize = 13.sp,
        color = LisaBlueDark.copy(alpha = 0.75f),
        lineHeight = 18.sp
    )
}


@Composable
private fun MenuPanel(
    uiStrings: LisaUiStrings,
    mainMenuState: MainMenuNavigationState,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onSelect: () -> Unit,
    onSelectDestination: (MainMenuDestination) -> Unit,
    onViewportMetrics: (viewportHeightPx: Int, maxScrollPx: Int, scrollPx: Int) -> Unit,
    onEmergency: () -> Unit,
    onClose: () -> Unit,
    fillWorkspace: Boolean = false
) {
    val normalized = mainMenuState.normalized()
    val destinationCount = MainMenuCatalog.destinationCount
    val entries = remember { MainMenuCatalog.listEntries() }
    val menuScrollState = rememberScrollState()
    var viewportHeightPx by remember { mutableIntStateOf(0) }
    var selectedItemTopPx by remember { mutableIntStateOf(0) }
    var selectedItemHeightPx by remember { mutableIntStateOf(0) }
    val maxScrollPx = menuScrollState.maxValue

    LaunchedEffect(
        normalized.selectionIndex,
        normalized.revealSelection,
        normalized.scrollRequestPx,
        selectedItemTopPx,
        selectedItemHeightPx,
        viewportHeightPx,
        maxScrollPx
    ) {
        if (viewportHeightPx <= 0) return@LaunchedEffect
        val target: Int? = when {
            normalized.scrollRequestPx != null && maxScrollPx > 0 ->
                normalized.scrollRequestPx.coerceIn(0, maxScrollPx)
            normalized.revealSelection && selectedItemHeightPx > 0 && maxScrollPx > 0 ->
                GuidedCategoryMenuScroll.centeredScrollOffsetPx(
                    selectedItemTopPx = selectedItemTopPx,
                    selectedItemHeightPx = selectedItemHeightPx,
                    viewportHeightPx = viewportHeightPx,
                    maxScrollPx = maxScrollPx
                )
            else -> null
        }
        if (target != null &&
            GuidedCategoryMenuScroll.shouldAnimateTo(
                current = menuScrollState.value,
                target = target
            )
        ) {
            menuScrollState.animateScrollTo(target)
        }
    }

    LaunchedEffect(viewportHeightPx, maxScrollPx) {
        if (viewportHeightPx <= 0) return@LaunchedEffect
        snapshotFlow {
            menuScrollState.isScrollInProgress to menuScrollState.value
        }.collect { (inProgress, scrollPx) ->
            if (!inProgress) {
                onViewportMetrics(viewportHeightPx, maxScrollPx, scrollPx)
            }
        }
    }

    val canMoveUp = normalized.selectionIndex > 0
    val canMoveDown = normalized.selectionIndex < destinationCount - 1
    val canGoPreviousPage = CategoryViewportPaging.canGoToPreviousPage(normalized.viewportPage)
    val canGoNextPage = CategoryViewportPaging.canGoToNextPage(
        normalized.viewportPage,
        normalized.viewportPageCount
    )

    val menuBody: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .then(
                    if (fillWorkspace) {
                        Modifier
                            .fillMaxSize()
                            .background(MainMenuProductionUiAuthority.solidWorkspaceBackground())
                            .padding(LisaWorkspaceVisualStyle.PanelContentPadding)
                    } else {
                        Modifier.fillMaxWidth()
                    }
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiStrings.menu,
                    fontWeight = FontWeight.Bold,
                    fontSize = LisaWorkspaceVisualStyle.MenuTitleSize,
                    color = if (fillWorkspace) LisaWhite else LisaBlueDark
                )
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (fillWorkspace) Color.White.copy(alpha = 0.14f)
                            else LisaSoftGray
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = uiStrings.mainMenuItemIndicator(
                            normalized.selectionIndex + 1,
                            destinationCount
                        ),
                        fontSize = LisaWorkspaceVisualStyle.IndicatorSize,
                        fontWeight = FontWeight.SemiBold,
                        color = if (fillWorkspace) LisaWhite.copy(alpha = 0.9f) else LisaBlueDark.copy(alpha = 0.75f)
                    )
                    Text(
                        text = uiStrings.mainMenuPageIndicator(
                            normalized.viewportPage + 1,
                            normalized.viewportPageCount
                        ),
                        fontSize = LisaWorkspaceVisualStyle.IndicatorSize,
                        fontWeight = FontWeight.SemiBold,
                        color = if (fillWorkspace) LisaWhite.copy(alpha = 0.9f) else LisaBlueDark.copy(alpha = 0.75f)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = if (fillWorkspace) {
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                } else {
                    Modifier.fillMaxWidth()
                },
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (fillWorkspace) {
                                Modifier.fillMaxHeight()
                            } else {
                                Modifier.heightIn(max = 280.dp)
                            }
                        )
                        // Match MainMenuNavigationControls outer padding(6.dp) so the first
                        // destination card top edge aligns with the Move Up button top edge.
                        .padding(top = 6.dp)
                        .onGloballyPositioned { viewportHeightPx = it.size.height }
                        .verticalScroll(menuScrollState),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    entries.forEach { entry ->
                        when (entry) {
                            is MainMenuListEntry.SectionHeader -> {
                                // Skip the leading Communication header so the first destination
                                // card (Communication Profile) shares Move Up's vertical origin.
                                if (entry.section == MainMenuSection.Communication) return@forEach
                                MenuSectionHeader(
                                    title = when (entry.section) {
                                        MainMenuSection.Communication -> uiStrings.menuSectionCommunication
                                        MainMenuSection.Application -> uiStrings.menuSectionApplication
                                        MainMenuSection.Support -> uiStrings.menuSectionSupport
                                    },
                                    onDarkWorkspace = fillWorkspace
                                )
                            }
                            is MainMenuListEntry.Destination -> {
                                val isSelected =
                                    entry.destination == normalized.selectedDestination
                                MainMenuDestinationRow(
                                    label = MainMenuCatalog.title(entry.destination, uiStrings),
                                    number = entry.selectionIndex,
                                    sequenceLabel = MainMenuDestinationShortcuts.sequenceLabelForDestination(entry.destination),
                                    selected = isSelected,
                                    onClick = { onSelectDestination(entry.destination) },
                                    modifier = if (isSelected) {
                                        Modifier.onGloballyPositioned { coords ->
                                            selectedItemTopPx = coords.positionInParent().y.roundToInt()
                                            selectedItemHeightPx = coords.size.height
                                        }
                                    } else {
                                        Modifier
                                    }
                                )
                            }
                        }
                    }
                }
                MainMenuNavigationControls(
                    uiStrings = uiStrings,
                    canMoveUp = canMoveUp,
                    canMoveDown = canMoveDown,
                    canGoPreviousPage = canGoPreviousPage,
                    canGoNextPage = canGoNextPage,
                    showPageControls = normalized.viewportPageCount > 1,
                    onMoveUp = onMoveUp,
                    onMoveDown = onMoveDown,
                    onPreviousPage = onPreviousPage,
                    onNextPage = onNextPage,
                    onSelect = onSelect,
                    onClose = onClose,
                    onEmergency = onEmergency,
                    navPanelWidth = LisaWorkspaceVisualStyle.NavPanelWidth
                )
            }
            if (!fillWorkspace) {
                Spacer(Modifier.height(8.dp))
                MainMenuCloseRow(
                    uiStrings = uiStrings,
                    onClose = onClose
                )
            }
        }
    }

    if (fillWorkspace) {
        menuBody()
    } else {
        LisaPanelShell(title = "", onBack = null) {
            menuBody()
        }
    }
}

@Composable
private fun MainMenuNavigationControls(
    uiStrings: LisaUiStrings,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canGoPreviousPage: Boolean,
    canGoNextPage: Boolean,
    showPageControls: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    onEmergency: () -> Unit,
    navPanelWidth: Dp = LisaWorkspaceVisualStyle.NavPanelWidth
) {
    Column(
        modifier = Modifier
            .width(navPanelWidth)
            .fillMaxHeight()
            .clip(RoundedCornerShape(LisaWorkspaceVisualStyle.NavPanelCornerRadius))
            .background(LisaWorkspaceVisualStyle.NavPanelBackground)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // Reuse Category Menu guided control component for identical typography / sizing (RC7D.30).
        GuidedNavigationActionButton(
            symbol = "↑↑",
            title = uiStrings.mainMenuMoveUp,
            gestureHint = uiStrings.guidedScrollUpHint,
            sequenceLabel = formatWinkSequenceShort(
                GuidedModeNavigation.PREVIOUS_LEFT,
                GuidedModeNavigation.PREVIOUS_RIGHT
            ),
            enabled = canMoveUp,
            compact = true,
            onClick = onMoveUp
        )
        GuidedNavigationActionButton(
            symbol = "↓↓",
            title = uiStrings.mainMenuMoveDown,
            gestureHint = uiStrings.guidedScrollDownHint,
            sequenceLabel = formatWinkSequenceShort(
                GuidedModeNavigation.NEXT_LEFT,
                GuidedModeNavigation.NEXT_RIGHT
            ),
            enabled = canMoveDown,
            compact = true,
            onClick = onMoveDown
        )
        if (showPageControls) {
            GuidedNavigationActionButton(
                symbol = "⏮",
                title = uiStrings.mainMenuPreviousPage,
                gestureHint = uiStrings.guidedPreviousCategoryPageHint,
                sequenceLabel = formatWinkSequenceShort(
                    GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_LEFT,
                    GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_RIGHT
                ),
                enabled = canGoPreviousPage,
                compact = true,
                onClick = onPreviousPage
            )
            GuidedNavigationActionButton(
                symbol = "⏭",
                title = uiStrings.mainMenuNextPage,
                gestureHint = uiStrings.guidedNextCategoryPageHint,
                sequenceLabel = formatWinkSequenceShort(
                    GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT,
                    GuidedModeNavigation.NEXT_CATEGORY_PAGE_RIGHT
                ),
                enabled = canGoNextPage,
                compact = true,
                onClick = onNextPage
            )
        }
        GuidedNavigationActionButton(
            symbol = "✅",
            title = uiStrings.mainMenuOpenSelected,
            gestureHint = uiStrings.guidedSelectEnterHint,
            sequenceLabel = formatWinkSequenceShort(
                GuidedModeNavigation.SELECT_LEFT,
                GuidedModeNavigation.SELECT_RIGHT
            ),
            enabled = true,
            compact = true,
            onClick = onSelect
        )
        GuidedNavigationActionButton(
            symbol = "↩",
            title = uiStrings.mainMenuClose,
            gestureHint = uiStrings.guidedBackHint,
            sequenceLabel = formatWinkSequenceShort(
                GuidedModeNavigation.BACK_LEFT,
                GuidedModeNavigation.BACK_RIGHT
            ),
            enabled = true,
            compact = true,
            onClick = onClose
        )
        GuidedEmergencyNavButton(
            symbol = "🚨",
            title = uiStrings.emergency,
            gestureHint = uiStrings.guidedEmergencyNavHint,
            sequenceLabel = formatWinkSequenceShort(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS),
            compact = true,
            onClick = onEmergency
        )
    }
}

@Composable
private fun MainMenuCloseRow(
    uiStrings: LisaUiStrings,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LisaBlue)
            .clickable(onClick = onClose)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = uiStrings.close,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = LisaWhite
        )
        Text(
            text = formatWinkSequenceShort(
                GuidedModeNavigation.BACK_LEFT,
                GuidedModeNavigation.BACK_RIGHT
            ),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = LisaWhite
        )
    }
}

@Composable
private fun MenuSectionHeader(title: String, onDarkWorkspace: Boolean = false) {
    Text(
        text = title.uppercase(Locale.getDefault()),
        fontSize = LisaWorkspaceVisualStyle.SectionHeadingSize,
        fontWeight = FontWeight.Bold,
        color = if (onDarkWorkspace) {
            LisaWhite.copy(alpha = 0.75f)
        } else {
            LisaBlueDark.copy(alpha = 0.70f)
        },
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(start = 2.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun MainMenuDestinationRow(
    label: String,
    number: Int,
    sequenceLabel: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(LisaWorkspaceVisualStyle.CardCornerRadius)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .lisaFocusEmphasis(selected, LisaWorkspaceVisualStyle.CardCornerRadius)
            .background(
                color = if (selected) {
                    LisaWorkspaceVisualStyle.CardSelectedBackground
                } else {
                    LisaWorkspaceVisualStyle.CardBackground
                },
                shape = shape
            )
            .then(
                if (selected) {
                    Modifier.border(
                        LisaWorkspaceVisualStyle.CardSelectedBorderWidth,
                        LisaBlue,
                        shape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .semantics { this.selected = selected }
            .padding(
                horizontal = LisaWorkspaceVisualStyle.CardHorizontalPadding,
                vertical = LisaWorkspaceVisualStyle.CardVerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "$number.",
            fontWeight = FontWeight.Bold,
            fontSize = LisaWorkspaceVisualStyle.CardNumberSize,
            color = LisaBlueDark
        )
        Text(
            text = label,
            fontSize = LisaWorkspaceVisualStyle.CardTitleSize,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = LisaBlueDark,
            lineHeight = LisaWorkspaceVisualStyle.CardTitleLineHeight,
            modifier = Modifier.weight(1f)
        )
        WinkSequenceBadge(
            sequenceLabel = sequenceLabel,
            selected = selected
        )
    }
}

/**
 * RC7D.30 — full-width blue action matching the EverydayCommunicationPanel banner width/margins.
 * Label and sequence sit on one horizontal line (Menu L4 R6 / Close L2 R2).
 */
@Composable
private fun WorkspaceFullWidthActionButton(
    label: String,
    sequenceLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = LisaWorkspaceVisualStyle.FullWidthActionMinHeight)
            .clip(RoundedCornerShape(LisaWorkspaceVisualStyle.FullWidthActionCornerRadius))
            .background(LisaBlue)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = LisaWhite,
            maxLines = 1
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = sequenceLabel,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = LisaWhite,
            maxLines = 1
        )
    }
}

@Composable
private fun MyCommunicationPanel(
    uiStrings: LisaUiStrings,
    profiles: List<LisaUserProfile>,
    activeProfileId: String,
    onCreateProfile: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onUpdateProfile: (LisaUserProfile) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onBack: () -> Unit
) {
    val activeProfile = profiles.find { it.id == activeProfileId } ?: profiles.firstOrNull()

    LisaPanelShell(title = uiStrings.myCommunication, onBack = onBack, backLabel = uiStrings.back) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberDestinationScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PanelPurposeLine(uiStrings.communicationProfilePurpose)
            if (activeProfile != null) {
                SettingsSectionLabel(uiStrings.activeProfileSection)
                MenuDestinationSelectableSurface(
                    actionId = MenuDestinationActionId.ProfileActive,
                    active = true,
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = activeProfile.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = LisaBlueDark,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${LisaLanguageAvailabilityAuthority.coerceForVersion1(activeProfile.preferredLanguage).label} · ${activeProfile.communicationLevel.label}",
                            fontSize = 12.sp,
                            color = LisaBlueDark.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                SettingsSectionLabel(uiStrings.profileNameSection)
                MenuDestinationSelectableSurface(
                    actionId = MenuDestinationActionId.ProfileName,
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Column {
                        Text(
                            uiStrings.nameLabel,
                            fontSize = 12.sp,
                            color = LisaBlueDark.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            activeProfile.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LisaBlueDark,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                SettingsSectionLabel(uiStrings.preferredLanguageSection)
                ProfileLanguageOptionGroup(
                    activeLanguage = LisaLanguageAvailabilityAuthority.coerceForVersion1(
                        activeProfile.preferredLanguage
                    ),
                    uiStrings = uiStrings
                )

                SettingsSectionLabel(uiStrings.communicationLevelSection)
                ProfileOptionGroup(
                    options = CommunicationLevel.entries.map { it.label },
                    selected = activeProfile.communicationLevel.label,
                    idFor = { MenuDestinationActionId.communicationLevel(it) }
                )
            }

            SettingsSectionLabel(uiStrings.savedProfilesSection)
            profiles.forEach { profile ->
                val isActive = profile.id == activeProfileId
                val actionId = MenuDestinationActionId.savedProfile(profile.id)
                MenuDestinationSelectableSurface(
                    actionId = actionId,
                    active = isActive
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.name,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp,
                                color = LisaBlueDark,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = profile.communicationLevel.label,
                                fontSize = 11.sp,
                                color = LisaBlueDark.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (isActive) {
                            Text(
                                text = uiStrings.activeLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LisaBlue,
                                maxLines = 1,
                                modifier = Modifier.widthIn(min = 48.dp)
                            )
                        } else {
                            Text(text = "›", fontSize = 18.sp, color = LisaGray)
                        }
                    }
                }
            }

            MenuDestinationSelectableSurface(
                actionId = MenuDestinationActionId.ProfileNew
            ) {
                Text(
                    uiStrings.createNewProfile,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            if (profiles.size > 1 && activeProfile != null) {
                MenuDestinationSelectableSurface(
                    actionId = MenuDestinationActionId.ProfileDelete
                ) {
                    Text(
                        uiStrings.deleteActiveProfile,
                        color = LisaEmergencyRed,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileLanguageOptionGroup(
    activeLanguage: PreferredLanguage,
    uiStrings: LisaUiStrings
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LisaLanguageAvailabilityAuthority.displayedLanguages.forEach { language ->
            val canSelect = LisaLanguageAvailabilityAuthority.isSelectableInVersion1(language)
            val isActive = canSelect && activeLanguage == language
            val actionId = MenuDestinationActionId.language(language.label)
            MenuDestinationSelectableSurface(
                actionId = actionId,
                active = isActive
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = language.label,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 14.sp,
                            color = LisaBlueDark.copy(alpha = if (canSelect) 1f else 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!canSelect) {
                            Text(
                                text = LisaLanguageAvailabilityAuthority.version2StatusLine(uiStrings),
                                fontSize = 11.sp,
                                color = LisaBlueDark.copy(alpha = 0.55f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (isActive) {
                        Text(text = "✓", fontSize = 14.sp, color = LisaBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileOptionGroup(
    options: List<String>,
    selected: String,
    idFor: (String) -> MenuDestinationActionId
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { option ->
            val actionId = idFor(option)
            MenuDestinationSelectableSurface(
                actionId = actionId,
                active = option == selected
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = option,
                        fontWeight = if (option == selected) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 14.sp,
                        color = LisaBlueDark,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (option == selected) {
                        Text(text = "✓", fontSize = 14.sp, color = LisaBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceholderPanel(
    title: String,
    purpose: String,
    description: String,
    onBack: () -> Unit,
    backLabel: String = "Back"
) {
    LisaPanelShell(title = title, onBack = onBack, backLabel = backLabel) {
        PanelPurposeLine(purpose)
        if (description.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = LisaBlueDark.copy(alpha = 0.85f),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun DeveloperToolsPanel(
    uiStrings: LisaUiStrings,
    developerMode: Boolean,
    onDeveloperModeChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    LisaPanelShell(title = uiStrings.developerTools, onBack = onBack, backLabel = uiStrings.back) {
        PanelPurposeLine(uiStrings.developerToolsPurpose)
        Spacer(Modifier.height(12.dp))
        SettingsToggleRow(
            title = uiStrings.developerModeTitle,
            subtitle = uiStrings.developerModeSubtitle,
            checked = developerMode,
            onCheckedChange = onDeveloperModeChange
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LisaWhite)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = LisaBlueDark)
            Text(subtitle, fontSize = 12.sp, color = LisaBlueDark.copy(alpha = 0.7f))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun SettingsPanel(
    uiStrings: LisaUiStrings,
    settingsState: LisaSettingsUiState,
    speechVolumeLevel: Int,
    speechSpeedLevel: Int,
    hasSavedCalibration: Boolean,
    onBack: () -> Unit
) {
    LisaPanelShell(title = uiStrings.settings, onBack = onBack, backLabel = uiStrings.back) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberDestinationScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PanelPurposeLine(uiStrings.settingsPurpose)
            PrimarySettingsAuthority.Section.entries.forEach { section ->
                val sectionItems = PrimarySettingsAuthority.items.filter { it.section == section }
                if (sectionItems.isEmpty()) return@forEach
                SettingsSectionLabel(PrimarySettingsAuthority.sectionTitle(section, uiStrings))
                sectionItems.forEach { item ->
                    val status = when (item.id) {
                        PrimarySettingsAuthority.ItemId.Calibration ->
                            PrimarySettingsAuthority.calibrationStatusLabel(hasSavedCalibration, uiStrings)
                        PrimarySettingsAuthority.ItemId.SpeechVolume ->
                            SpeechVolumeAuthority.percentLabel(speechVolumeLevel)
                        PrimarySettingsAuthority.ItemId.SpeechSpeed ->
                            SpeechSpeedAuthority.displayLabel(speechSpeedLevel, uiStrings)
                        PrimarySettingsAuthority.ItemId.TextSize ->
                            "${(settingsState.textSizeScale * 100).toInt()}%"
                        PrimarySettingsAuthority.ItemId.DeviceCheck -> uiStrings.runDeviceCheckSubtitle
                        PrimarySettingsAuthority.ItemId.DeveloperMode ->
                            if (settingsState.developerMode) "On" else "Off"
                    }
                    val hint = when (item.actionType) {
                        MenuDestinationActionType.Choice -> uiStrings.settingsAdjustChoiceHint
                        MenuDestinationActionType.Toggle -> uiStrings.settingsToggleHint
                        else -> uiStrings.settingsAdjustWithSelectHint
                    }
                    PrimarySettingsLauncherCard(
                        actionId = item.actionId,
                        title = PrimarySettingsAuthority.title(item.id, uiStrings),
                        status = status,
                        hint = hint
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimarySettingsLauncherCard(
    actionId: MenuDestinationActionId,
    title: String,
    status: String,
    hint: String
) {
    MenuDestinationSelectableSurface(actionId = actionId) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = LisaBlueDark)
            Text(text = status, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = LisaBlueDark.copy(alpha = 0.85f), maxLines = 2)
            Text(text = hint, fontSize = 11.sp, color = LisaBlueDark.copy(alpha = 0.6f), maxLines = 2)
        }
    }
}

@Composable
private fun SettingsLinkRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LisaWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = LisaBlueDark)
            Text(subtitle, fontSize = 12.sp, color = LisaBlueDark.copy(alpha = 0.7f))
        }
        Text(text = "›", fontSize = 20.sp, color = LisaGray)
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text.uppercase(Locale.getDefault()),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = LisaBlueDark.copy(alpha = 0.55f),
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun SettingsSliderRow(
    title: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LisaWhite)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = LisaBlueDark)
            Text(valueLabel, fontSize = 12.sp, color = LisaBlueDark.copy(alpha = 0.7f))
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = LisaBlue,
                activeTrackColor = LisaBlue,
                inactiveTrackColor = LisaSoftGray
            )
        )
    }
}

@Composable
private fun VocabularyCreatePhraseCard(
    uiStrings: LisaUiStrings,
    onCreatePhrase: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LisaWhite)
            .padding(14.dp)
    ) {
        Text(
            text = uiStrings.createPhraseCardTitle,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = LisaBlueDark
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = uiStrings.createPhraseCardDescription,
            fontSize = 13.sp,
            color = LisaBlueDark.copy(alpha = 0.75f),
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onCreatePhrase,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LisaBlue)
        ) {
            Text(uiStrings.createPhraseButton, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CreatePhrasePanel(
    uiStrings: LisaUiStrings,
    onBegin: () -> Unit,
    onBack: () -> Unit
) {
    LisaPanelShell(title = uiStrings.createPhraseTitle, onBack = onBack, backLabel = uiStrings.back) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PanelPurposeLine(uiStrings.createPhrasePurpose)
            Text(
                text = uiStrings.createPhraseIntroLead,
                fontSize = 14.sp,
                color = LisaBlueDark.copy(alpha = 0.85f),
                lineHeight = 20.sp
            )
            Text(
                text = uiStrings.createPhraseIntroDetail,
                fontSize = 14.sp,
                color = LisaBlueDark.copy(alpha = 0.85f),
                lineHeight = 20.sp
            )
            Text(
                text = uiStrings.createPhraseAudienceNote,
                fontSize = 13.sp,
                color = LisaBlueDark.copy(alpha = 0.7f),
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = uiStrings.createPhraseHowItWorksTitle,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = LisaBlueDark
            )
            PhraseCreationStepCard(
                stepLabel = uiStrings.createPhraseStep1Label,
                body = uiStrings.createPhraseStep1Body,
                detail = uiStrings.createPhraseStep1Examples
            )
            PhraseCreationStepCard(
                stepLabel = uiStrings.createPhraseStep2Label,
                body = uiStrings.createPhraseStep2Body
            )
            PhraseCreationStepCard(
                stepLabel = uiStrings.createPhraseStep3Label,
                body = uiStrings.createPhraseStep3Body
            )
            Button(
                onClick = onBegin,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LisaBlue)
            ) {
                Text(uiStrings.createPhraseBeginButton, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PhraseCreationStepCard(
    stepLabel: String,
    body: String,
    detail: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LisaWhite)
            .padding(12.dp)
    ) {
        Text(
            text = stepLabel,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = LisaBlueDark.copy(alpha = 0.55f),
            letterSpacing = 0.4.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = body,
            fontSize = 14.sp,
            color = LisaBlueDark,
            lineHeight = 19.sp
        )
        if (detail != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = detail,
                fontSize = 12.sp,
                color = LisaBlueDark.copy(alpha = 0.65f),
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun LisaActionButton(
    text: String,
    modifier: Modifier = Modifier,
    filled: Boolean,
    danger: Boolean = false,
    subtitle: String? = null,
    multiline: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    val twoLine = multiline || subtitle != null || text.contains('\n')
    val buttonHeight = if (twoLine) 52.dp else 44.dp
    val label: @Composable () -> Unit = {
        if (subtitle != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        } else {
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = if (filled) FontWeight.SemiBold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = if (twoLine) 2 else 1,
                lineHeight = if (twoLine) 15.sp else 13.sp
            )
        }
    }
    if (filled) {
        Button(
            onClick = onClick,
            modifier = modifier.height(buttonHeight),
            shape = shape,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LisaBlue)
        ) {
            label()
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(buttonHeight),
            shape = shape,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (danger) LisaEmergencyRed else LisaBlueDark,
                containerColor = LisaWhite.copy(alpha = 0.92f)
            )
        ) {
            label()
        }
    }
}
