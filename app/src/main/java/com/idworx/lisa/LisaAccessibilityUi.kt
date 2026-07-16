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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.idworx.lisa.features.onboardingguide.ui.TrainingSettingsSection
import com.idworx.lisa.features.onboardingguide.ui.trainingBlocksMainUi
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.ui.theme.LisaBlue
import com.idworx.lisa.ui.theme.LisaBlueDark
import com.idworx.lisa.ui.theme.LisaBlueLight
import com.idworx.lisa.ui.theme.LisaEmergencyRed
import com.idworx.lisa.ui.theme.LisaGray
import com.idworx.lisa.ui.theme.LisaSoftGray
import com.idworx.lisa.ui.theme.LisaWhite
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
    onGuidedPhraseEntry: (GuidedVocabularyEntry) -> Unit = {},
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
    guidedTrainingActive: Boolean = false,
    guidedTrainingState: GuidedTrainingUiState = GuidedTrainingUiState(),
    guidedTrainingSetupStep: Int = 0,
    guidedTrainingReturningUser: Boolean = false,
    trainingEyeTracking: TrainingEyeTrackingState = TrainingEyeTrackingState(),
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
    cameraView: @Composable () -> Unit
) {
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

    val canRepeat = lastSpoken.isNotBlank()
    val density = LocalDensity.current
    // Guided Training Mode — navigation lessons teach the real Communication Workspace, so the
    // workspace overlay must be visible even though onboarding has not finished yet.
    val guidedWorkspaceTrainingActive =
        guidedTrainingActive && guidedTrainingState.phase == TrainingPhase.NavigationLesson
    // RC7D.17A — Phrase Management must occupy the main workspace slot like the composer.
    // Hosting it under Menu/Reset (after GuidedVocabularyOverlay weight(1f)) clipped the
    // fixed Scroll/Back/Emergency controls off the physical-device screen.
    val phraseManagementActive = PhraseManagementController.occupiesMainContentSlot(activePanel)
    val showSharedBlinkStatusHeader =
        PhraseManagementController.showSharedBlinkStatusHeader(phraseComposerActive)
    val showGuidedVocabularyOverlay = GuidedVocabularyOverlayVisibility.shouldShowOverlay(
        onboardingCompleted = onboardingCompleted,
        cameraPermissionGranted = cameraPermissionGranted,
        emergencyActive = emergencyActive,
        practiceModeOpen = practiceModeOpen,
        quickControlsOpen = quickControlsOpen,
        guidedWorkspaceTrainingActive = guidedWorkspaceTrainingActive
    ) && PhraseManagementController.showGuidedVocabularyOverlayAlongsideManagement(
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
                        color = LisaBlueDark
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
            if (userDisplay.showIntentPreview && userDisplay.phrase != null) {
                IntentPreviewCard(phrase = userDisplay.phrase, compact = !countdownActive)
                Spacer(Modifier.height(4.dp))
            }

            EverydayCommunicationPanel(
                uiStrings = uiStrings,
                userDisplay = userDisplay,
                countdownActive = countdownActive,
                onEditCountdown = onEditCountdown
            )

            Spacer(Modifier.height(4.dp))
            CompactSensitivityControls(
                uiStrings = uiStrings,
                sensitivityLevel = sensitivityLevel,
                responseTimeSec = responseTimeSec,
                onDecrease = onSensitivityDecrease,
                onIncrease = onSensitivityIncrease,
                onDecreaseResponseTime = onResponseTimeDecrease,
                onIncreaseResponseTime = onResponseTimeIncrease,
                guidedResponseTimeControlsVisible = guidedWorkspaceTrainingActive,
                guidedResponseTimeSec = guidedTrainingState.progress.preferences.guidedResponseTimeSec,
                onDecreaseGuidedResponseTime = onTrainingDecreaseResponseTime,
                onIncreaseGuidedResponseTime = onTrainingIncreaseResponseTime
            )

            // During Phrase Management / Details, always show Left/Right counters so blink
            // progress stays visible even at zero (RC7D.18). Workspace idle behaviour unchanged.
            if (!developerMode && (
                    phraseManagementActive ||
                        userDisplay.leftWinkDots > 0 ||
                        userDisplay.rightWinkDots > 0
                    )
            ) {
                Spacer(Modifier.height(4.dp))
                SequenceProgressDots(
                    uiStrings = uiStrings,
                    leftCount = userDisplay.leftWinkDots,
                    rightCount = userDisplay.rightWinkDots
                )
            }

            if (developerMode) {
                Spacer(Modifier.height(4.dp))
                DeveloperPanel(info = developerInfo)
            }
        }
        }

        if (PhraseManagementController.showGuidedVocabularyOverlayAlongsideManagement(
                phraseComposerActive = phraseComposerActive,
                phraseManagementActive = phraseManagementActive
            )
        ) {
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

        if (!phraseComposerActive) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LisaActionButton(
                    text = if (activePanel.isOpen()) uiStrings.close else uiStrings.menu,
                    modifier = Modifier.weight(1f),
                    filled = !activePanel.isOpen(),
                    onClick = onMenuClick
                )
                LisaActionButton(
                    text = uiStrings.reset,
                    modifier = Modifier.weight(1f),
                    filled = false,
                    danger = emergencyActive,
                    onClick = onReset
                )
                if (canRepeat) {
                    LisaActionButton(
                        text = uiStrings.repeat,
                        modifier = Modifier.weight(1f),
                        filled = false,
                        onClick = onRepeat
                    )
                }
            }

            if (activePanel != LisaPanel.None && !phraseManagementActive) {
                Spacer(Modifier.height(10.dp))
                when (activePanel) {
                    LisaPanel.Menu -> MenuPanel(
                        uiStrings = uiStrings,
                        onSelectPanel = onSelectPanel,
                        onClose = onClosePanel
                    )
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
                    LisaPanel.VocabularyTraining -> Unit
                    LisaPanel.CreatePhrase -> CreatePhrasePanel(
                        uiStrings = uiStrings,
                        onBegin = onOpenPhraseEditor,
                        onBack = onBackToMenu
                    )
                    LisaPanel.PhraseEditor -> Unit
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
                        onBack = { onSelectPanel(LisaPanel.Voice) }
                    )
                    LisaPanel.VoicePremium -> PremiumVoicesPanel(
                        uiStrings = uiStrings,
                        onBack = { onSelectPanel(LisaPanel.Voice) }
                    )
                    LisaPanel.VoiceMyVoice -> MyVoicePanel(
                        uiStrings = uiStrings,
                        onBack = { onSelectPanel(LisaPanel.Voice) }
                    )
                    LisaPanel.VoiceFamily -> FamilyVoicePanel(
                        uiStrings = uiStrings,
                        onBack = { onSelectPanel(LisaPanel.Voice) }
                    )
                    LisaPanel.Settings -> SettingsPanel(
                        uiStrings = uiStrings,
                        settingsState = settingsState,
                        trainingPreferences = guidedTrainingState.progress.preferences,
                        learningProgress = guidedTrainingState.progress,
                        onDeveloperModeChange = onDeveloperModeChange,
                        onSensitivityDecrease = onSensitivityDecrease,
                        onSensitivityIncrease = onSensitivityIncrease,
                        onPlaceholderChange = onSettingsPlaceholderChange,
                        onTrainingReplayTutorial = onTrainingReplayTutorial,
                        onTrainingPracticeCommunication = onTrainingPracticeCommunication,
                        onTrainingPracticeNavigation = onTrainingPracticeNavigation,
                        onTrainingResetProgress = onTrainingResetProgress,
                        onTrainingPreferencesChange = onTrainingPreferencesChange,
                        onOpenDeviceCheck = onOpenDeviceCheck,
                        onOpenDeveloperTools = onOpenDeveloperTools,
                        onBack = onBackToMenu
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
                    LisaPanel.PrivacyPolicy -> PrivacyPolicyPanel(uiStrings = uiStrings, onBack = onBackToMenu)
                    LisaPanel.Feedback -> FeedbackPanel(
                        uiStrings = uiStrings,
                        savedCount = feedbackSavedCount,
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
private fun CompactSensitivityControls(
    uiStrings: LisaUiStrings,
    sensitivityLevel: Int,
    responseTimeSec: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    /** Everyday Communication Workspace response time — same visual style as Sensitivity's row. */
    onDecreaseResponseTime: () -> Unit = {},
    onIncreaseResponseTime: () -> Unit = {},
    /**
     * During Guided Training's Navigation Lesson, this replaces the everyday workspace row above
     * (rather than adding a second one) so its own, separately adjustable settle time is what's
     * shown — the two rows share a backing value that never applies at the same time
     * ([MainActivity.effectiveSequenceIdleTimeoutMs] only reads the guided one while training is
     * active), so showing both together was a genuine duplicate, not just a visual one.
     */
    guidedResponseTimeControlsVisible: Boolean = false,
    guidedResponseTimeSec: Int = SequenceProcessingDelay.GUIDED_DEFAULT_SECONDS,
    onDecreaseGuidedResponseTime: () -> Unit = {},
    onIncreaseGuidedResponseTime: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = onDecrease,
                enabled = sensitivityLevel > MIN_SENSITIVITY_LEVEL,
                modifier = Modifier.height(30.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LisaWhite)
            ) { Text(uiStrings.sensitivityDecrease, fontSize = 10.sp) }
            Text(
                text = "${uiStrings.sensitivity}: $sensitivityLevel",
                color = LisaWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            OutlinedButton(
                onClick = onIncrease,
                enabled = sensitivityLevel < MAX_SENSITIVITY_LEVEL,
                modifier = Modifier.height(30.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LisaWhite)
            ) { Text(uiStrings.sensitivityIncrease, fontSize = 10.sp) }
        }
        // Exactly one response-time row is ever shown — the Guided Training row while a Navigation
        // Lesson is active (it's the value actually used for gesture timing then), otherwise the
        // everyday workspace row. Both use the identical labelled +/- style so the control is always
        // clearly named, never a second, bare-symbol duplicate underneath.
        if (guidedResponseTimeControlsVisible) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = onDecreaseGuidedResponseTime,
                    enabled = guidedResponseTimeSec > SequenceProcessingDelay.MIN_SECONDS,
                    modifier = Modifier.height(30.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LisaWhite)
                ) { Text(uiStrings.responseTimeDecrease, fontSize = 10.sp) }
                Text(
                    text = "${uiStrings.responseTime}: ${guidedResponseTimeSec}s",
                    color = LisaWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                OutlinedButton(
                    onClick = onIncreaseGuidedResponseTime,
                    enabled = guidedResponseTimeSec < SequenceProcessingDelay.MAX_SECONDS,
                    modifier = Modifier.height(30.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LisaWhite)
                ) { Text(uiStrings.responseTimeIncrease, fontSize = 10.sp) }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = onDecreaseResponseTime,
                    enabled = responseTimeSec > SequenceProcessingDelay.MIN_SECONDS,
                    modifier = Modifier.height(30.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LisaWhite)
                ) { Text(uiStrings.responseTimeDecrease, fontSize = 10.sp) }
                Text(
                    text = "${uiStrings.responseTime}: ${responseTimeSec}s",
                    color = LisaWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                OutlinedButton(
                    onClick = onIncreaseResponseTime,
                    enabled = responseTimeSec < SequenceProcessingDelay.MAX_SECONDS,
                    modifier = Modifier.height(30.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LisaWhite)
                ) { Text(uiStrings.responseTimeIncrease, fontSize = 10.sp) }
            }
        }
    }
}

@Composable
private fun SequenceProgressDots(uiStrings: LisaUiStrings, leftCount: Int, rightCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.32f))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = uiStrings.leftDots(leftCount),
            color = LisaWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = uiStrings.rightDots(rightCount),
            color = LisaWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun IntentPreviewCard(phrase: String, compact: Boolean = false) {
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
        Text(text = "💬", fontSize = if (compact) 22.sp else 28.sp)
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

@Composable
private fun EverydayCommunicationPanel(
    uiStrings: LisaUiStrings,
    userDisplay: LisaUserDisplay,
    countdownActive: Boolean,
    onEditCountdown: () -> Unit
) {
    val expanded = countdownActive || userDisplay.countdown != null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(LisaBlue.copy(alpha = if (expanded) 0.78f else 0.62f))
            .padding(
                horizontal = if (expanded) 14.dp else 10.dp,
                vertical = if (expanded) 12.dp else 6.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                fontSize = if (expanded) 22.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = if (expanded) 26.sp else 20.sp,
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
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LisaBlueLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                        color = LisaBlueDark
                    )
                }
                if (onBack != null) {
                    TextButton(onClick = onBack) {
                        Text(backLabel, color = LisaBlueDark, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            content()
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

private data class MenuSection(val title: String, val panels: List<LisaPanel>)

@Composable
private fun MenuPanel(
    uiStrings: LisaUiStrings,
    onSelectPanel: (LisaPanel) -> Unit,
    onClose: () -> Unit
) {
    val sections = listOf(
        MenuSection(uiStrings.menuSectionCommunication, listOf(
            LisaPanel.MyCommunication,
            LisaPanel.VocabularyTraining,
            LisaPanel.Voice
        )),
        MenuSection(uiStrings.menuSectionApplication, listOf(
            LisaPanel.Settings,
            LisaPanel.AboutLisa,
            LisaPanel.PrivacyPolicy
        )),
        MenuSection(uiStrings.menuSectionSupport, listOf(
            LisaPanel.Feedback,
            LisaPanel.ReleaseNotes
        ))
    )

    LisaPanelShell(title = uiStrings.menu, onBack = onClose, backLabel = uiStrings.close) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            sections.forEach { section ->
                MenuSectionHeader(title = section.title)
                section.panels.forEach { panel ->
                    MenuRow(label = uiStrings.menuLabel(panel), onClick = { onSelectPanel(panel) })
                }
            }
        }
    }
}

@Composable
private fun MenuSectionHeader(title: String) {
    Text(
        text = title.uppercase(Locale.getDefault()),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = LisaBlueDark.copy(alpha = 0.55f),
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(start = 2.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun MenuRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LisaWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = LisaBlueDark
        )
        Text(
            text = "›",
            fontSize = 20.sp,
            color = LisaGray
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
    var editingName by remember(activeProfileId) { mutableStateOf(activeProfile?.name ?: "") }

    LaunchedEffect(activeProfile?.name) {
        editingName = activeProfile?.name ?: ""
    }

    LisaPanelShell(title = uiStrings.myCommunication, onBack = onBack, backLabel = uiStrings.back) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 380.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PanelPurposeLine(uiStrings.communicationProfilePurpose)
            if (activeProfile != null) {
                SettingsSectionLabel(uiStrings.activeProfileSection)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LisaWhite)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = activeProfile.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = LisaBlueDark
                    )
                    Text(
                        text = "${activeProfile.preferredLanguage.label} · ${activeProfile.communicationLevel.label}",
                        fontSize = 12.sp,
                        color = LisaBlueDark.copy(alpha = 0.7f)
                    )
                }

                SettingsSectionLabel(uiStrings.profileNameSection)
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = editingName,
                    onValueChange = { editingName = it },
                    label = { Text(uiStrings.nameLabel) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (editingName != activeProfile.name && editingName.isNotBlank()) {
                            TextButton(onClick = {
                                onUpdateProfile(activeProfile.copy(name = editingName.trim()))
                            }) {
                                Text(uiStrings.saveLabel, fontSize = 12.sp)
                            }
                        }
                    }
                )

                SettingsSectionLabel(uiStrings.preferredLanguageSection)
                ProfileOptionGroup(
                    options = PreferredLanguage.selectable.map { it.label },
                    selected = activeProfile.preferredLanguage.label,
                    onSelect = { label ->
                        val language = PreferredLanguage.fromStored(label)
                        onUpdateProfile(activeProfile.copy(preferredLanguage = language))
                    }
                )

                SettingsSectionLabel(uiStrings.communicationLevelSection)
                ProfileOptionGroup(
                    options = CommunicationLevel.entries.map { it.label },
                    selected = activeProfile.communicationLevel.label,
                    onSelect = { label ->
                        val level = CommunicationLevel.fromStored(label)
                        onUpdateProfile(activeProfile.withCommunicationLevel(level))
                    }
                )
            }

            SettingsSectionLabel(uiStrings.savedProfilesSection)
            profiles.forEach { profile ->
                val isActive = profile.id == activeProfileId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isActive) LisaBlueLight.copy(alpha = 0.35f) else LisaWhite)
                        .clickable { if (!isActive) onSelectProfile(profile.id) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.name,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp,
                            color = LisaBlueDark
                        )
                        Text(
                            text = profile.communicationLevel.label,
                            fontSize = 11.sp,
                            color = LisaBlueDark.copy(alpha = 0.65f)
                        )
                    }
                    if (isActive) {
                        Text(
                            text = uiStrings.activeLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LisaBlue
                        )
                    } else {
                        Text(text = "›", fontSize = 18.sp, color = LisaGray)
                    }
                }
            }

            OutlinedButton(
                onClick = onCreateProfile,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(uiStrings.createNewProfile)
            }

            if (profiles.size > 1 && activeProfile != null) {
                OutlinedButton(
                    onClick = { onDeleteProfile(activeProfile.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LisaEmergencyRed)
                ) {
                    Text(uiStrings.deleteActiveProfile)
                }
            }
        }
    }
}

@Composable
private fun ProfileOptionGroup(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (option == selected) LisaBlueLight.copy(alpha = 0.35f) else LisaWhite)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = option,
                    fontWeight = if (option == selected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 14.sp,
                    color = LisaBlueDark
                )
                if (option == selected) {
                    Text(text = "✓", fontSize = 14.sp, color = LisaBlue, fontWeight = FontWeight.Bold)
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
    trainingPreferences: TrainingPreferences = TrainingPreferences(),
    learningProgress: TrainingProgress? = null,
    onDeveloperModeChange: (Boolean) -> Unit,
    onSensitivityDecrease: () -> Unit,
    onSensitivityIncrease: () -> Unit,
    onPlaceholderChange: (LisaSettingsUiState) -> Unit,
    onTrainingReplayTutorial: () -> Unit = {},
    onTrainingPracticeCommunication: () -> Unit = {},
    onTrainingPracticeNavigation: () -> Unit = {},
    onTrainingResetProgress: () -> Unit = {},
    onTrainingPreferencesChange: (TrainingPreferences) -> Unit = {},
    onOpenDeviceCheck: () -> Unit = {},
    onOpenDeveloperTools: () -> Unit = {},
    onBack: () -> Unit
) {
    LisaPanelShell(title = uiStrings.settings, onBack = onBack, backLabel = uiStrings.back) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PanelPurposeLine(uiStrings.settingsPurpose)
            SettingsSectionLabel(uiStrings.settingsSectionDetection)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LisaWhite)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(uiStrings.sensitivity, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = LisaBlueDark)
                    Text("Level ${settingsState.sensitivityLevel}", fontSize = 12.sp, color = LisaBlueDark.copy(alpha = 0.7f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onSensitivityDecrease,
                        enabled = settingsState.sensitivityLevel > MIN_SENSITIVITY_LEVEL,
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) { Text("−", fontSize = 16.sp) }
                    OutlinedButton(
                        onClick = onSensitivityIncrease,
                        enabled = settingsState.sensitivityLevel < MAX_SENSITIVITY_LEVEL,
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) { Text("+", fontSize = 16.sp) }
                }
            }

            SettingsToggleRow(
                title = uiStrings.calibrationTitle,
                subtitle = uiStrings.calibrationSubtitle,
                checked = settingsState.calibrationEnabled,
                onCheckedChange = { onPlaceholderChange(settingsState.copy(calibrationEnabled = it)) }
            )

            SettingsSectionLabel(uiStrings.settingsSectionCommunication)
            SettingsSliderRow(
                title = uiStrings.confirmationCountdownTitle,
                valueLabel = "${settingsState.countdownDurationSec} sec",
                value = settingsState.countdownDurationSec.toFloat(),
                valueRange = 2f..5f,
                steps = 2,
                onValueChange = {
                    onPlaceholderChange(settingsState.copy(countdownDurationSec = it.toInt()))
                }
            )
            ResponseSpeedPicker(
                uiStrings = uiStrings,
                selected = settingsState.responseSpeed,
                onSelect = { speed -> onPlaceholderChange(settingsState.copy(responseSpeed = speed)) }
            )

            SettingsSectionLabel(uiStrings.settingsSectionDisplay)
            SettingsSliderRow(
                title = uiStrings.textSize,
                valueLabel = "${(settingsState.textSizeScale * 100).toInt()}%",
                value = settingsState.textSizeScale,
                valueRange = 0.8f..1.4f,
                onValueChange = { onPlaceholderChange(settingsState.copy(textSizeScale = it)) }
            )

            SettingsSectionLabel(uiStrings.settingsSectionEmergency)
            SettingsSliderRow(
                title = uiStrings.emergencyAlarmVolumeTitle,
                valueLabel = "${(settingsState.emergencyAlarmVolume * 100).toInt()}%",
                value = settingsState.emergencyAlarmVolume,
                valueRange = 0.5f..1f,
                onValueChange = { onPlaceholderChange(settingsState.copy(emergencyAlarmVolume = it)) }
            )

            TrainingSettingsSection(
                uiStrings = uiStrings,
                preferences = trainingPreferences,
                learningProgress = learningProgress,
                onReplayTutorial = onTrainingReplayTutorial,
                onPracticeCommunication = onTrainingPracticeCommunication,
                onPracticeNavigation = onTrainingPracticeNavigation,
                onResetProgress = onTrainingResetProgress,
                onPreferencesChange = onTrainingPreferencesChange
            )

            SettingsSectionLabel(uiStrings.settingsSectionSupportDiagnostics)
            SettingsLinkRow(
                title = uiStrings.runDeviceCheckTitle,
                subtitle = uiStrings.runDeviceCheckSubtitle,
                onClick = onOpenDeviceCheck
            )

            SettingsSectionLabel(uiStrings.settingsSectionData)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LisaWhite)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(uiStrings.profileBackupTitle, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = LisaBlueDark)
                    Text(uiStrings.profileBackupSubtitle, fontSize = 12.sp, color = LisaBlueDark.copy(alpha = 0.7f))
                }
                OutlinedButton(onClick = { }, enabled = false) {
                    Text(uiStrings.exportLabel, fontSize = 12.sp)
                }
            }

            SettingsSectionLabel(uiStrings.settingsSectionAdvanced)
            SettingsToggleRow(
                title = uiStrings.developerModeTitle,
                subtitle = uiStrings.developerModeSubtitle,
                checked = settingsState.developerMode,
                onCheckedChange = onDeveloperModeChange
            )
            if (BuildConfig.DEBUG) {
                SettingsLinkRow(
                    title = uiStrings.developerTools,
                    subtitle = uiStrings.developerToolsPurpose,
                    onClick = onOpenDeveloperTools
                )
            }
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
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    if (filled) {
        Button(
            onClick = onClick,
            modifier = modifier.height(44.dp),
            shape = shape,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LisaBlue)
        ) {
            Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(44.dp),
            shape = shape,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (danger) LisaEmergencyRed else LisaBlueDark,
                containerColor = LisaWhite.copy(alpha = 0.92f)
            )
        ) {
            Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}
