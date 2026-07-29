package com.idworx.lisa.features.onboardingguide.ui

import com.idworx.lisa.EMERGENCY_LEFT_WINKS
import com.idworx.lisa.EMERGENCY_RIGHT_WINKS
import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.LisaCoreVocabulary
import com.idworx.lisa.LisaUiStrings
import com.idworx.lisa.PreferredLanguage
import com.idworx.lisa.features.experiencepolish.patientcommunicationcoach.PatientCommunicationCoachEngine
import com.idworx.lisa.features.onboardingguide.coach.CoachLessonDecision
import com.idworx.lisa.features.onboardingguide.coach.CommunicationCoachEngine
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.navigation.GuidedTrainingNavigator
import com.idworx.lisa.features.onboardingguide.services.TrainingSessionController
import com.idworx.lisa.features.onboardingguide.state.GuidedTrainingUiState
import com.idworx.lisa.features.onboardingguide.state.TrainingEvent
import com.idworx.lisa.features.onboardingguide.state.TrainingFeedback
import com.idworx.lisa.formatWinkSequenceShort

object NavigationLessonContent {
    fun title(action: NavigationAction, uiStrings: LisaUiStrings): String = when (action) {
        NavigationAction.OpenCategories -> uiStrings.t("Categories", "Kategorieë", "Izigaba")
        NavigationAction.MoveToMedicalCategory ->
            com.idworx.lisa.features.guidedmedicalcategoryjourney.GuidedMedicalCategoryJourneyAuthority
                .MOVE_LESSON_TITLE
        NavigationAction.SelectCategory ->
            com.idworx.lisa.features.guidedmedicalcategoryjourney.GuidedMedicalCategoryJourneyAuthority
                .OPEN_MEDICAL_TITLE
        NavigationAction.SelectPhrase ->
            com.idworx.lisa.features.guidedmedicalcategoryjourney.GuidedMedicalCategoryJourneyAuthority
                .sayPhraseLessonTitle(uiStrings.language, uiStrings)
        NavigationAction.CloseMenu -> uiStrings.t("Go Back", "Gaan Terug", "Buyela Emuva")
        NavigationAction.RepeatLastPhrase -> uiStrings.t("Repeat", "Herhaal", "Phinda")
        NavigationAction.OpenMenu -> uiStrings.t("Menu", "Kieslys", "Imenyu")
        NavigationAction.OpenCommunicationHistory -> uiStrings.t("History", "Geskiedenis", "Umlando")
        NavigationAction.NextPage -> uiStrings.t("Next Page", "Volgende Bladsy", "Ikhasi Elilandelayo")
        NavigationAction.PreviousPage -> uiStrings.t("Previous Page", "Vorige Bladsy", "Ikhasi Elidlule")
        NavigationAction.ResetSequence -> uiStrings.t(
            "Continue to the Menu",
            "Gaan voort na die Kieslys",
            "Qhubekela kwiMenyu"
        )
        NavigationAction.TriggerEmergency -> uiStrings.t("Emergency", "Nood", "Usizo oluphuthumayo")
        NavigationAction.OpenQuickControls -> uiStrings.t("Quick Phrases", "Vinnige Kontroles", "Izilawuli Esisheshayo")
        NavigationAction.OpenSettings -> uiStrings.t("Settings", "Instellings", "Izilungiselelo")
        NavigationAction.OpenCaregiver -> uiStrings.t("Caregiver", "Versorger", "Umnakekeli")
        NavigationAction.OpenVoice -> uiStrings.t("Voice", "Stem", "Izwi")
        NavigationAction.MenuSelectVoice -> uiStrings.t("Voice", "Stem", "Izwi")
        NavigationAction.MenuSelectSettings -> uiStrings.t("Settings", "Instellings", "Izilungiselelo")
        NavigationAction.BackFromDestination -> uiStrings.t("Go Back", "Gaan Terug", "Buyela Emuva")
        NavigationAction.FinishGuidedLearning ->
            com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.LESSON_TITLE
        NavigationAction.AdjustSensitivity ->
            com.idworx.lisa.features.guidedsensitivitylesson.GuidedSensitivityLessonAuthority.LESSON_TITLE
    }

    fun instruction(action: NavigationAction, uiStrings: LisaUiStrings): String = when (action) {
        NavigationAction.OpenCategories -> uiStrings.t(
            "These are your communication categories. Open Categories with " +
                "${formatWinkSequenceShort(GuidedModeNavigation.CATEGORIES_LEFT, GuidedModeNavigation.CATEGORIES_RIGHT)}.",
            "Hier is jou kategorieë.",
            "Lezi yizigaba zakho zokuxhumana."
        )
        NavigationAction.MoveToMedicalCategory ->
            com.idworx.lisa.features.guidedmedicalcategoryjourney.GuidedMedicalCategoryJourneyAuthority
                .MOVE_INSTRUCTION
        NavigationAction.SelectCategory -> uiStrings.t(
            "Open Medical with its category sequence.",
            "Open Medies met sy kategorie-reeks.",
            "Vula Ezokwelapha ngomlingo waso."
        )
        NavigationAction.SelectPhrase ->
            com.idworx.lisa.features.guidedmedicalcategoryjourney.GuidedMedicalCategoryJourneyAuthority
                .sayPhraseLessonTitle(uiStrings.language, uiStrings)
        NavigationAction.CloseMenu -> uiStrings.t(
            "Use Back (L2 R2) to return to categories.",
            "Gebruik Terug (L2 R2).",
            "Sebenzisa Emuva (L2 R2)."
        )
        NavigationAction.RepeatLastPhrase -> uiStrings.t(
            "Repeat your last phrase with L1 R1.",
            "Herhaal met L1 R1.",
            "Phinda nge-L1 R1."
        )
        NavigationAction.OpenMenu ->
            com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.instructionFor(NavigationAction.OpenMenu)
        NavigationAction.OpenCommunicationHistory -> uiStrings.t(
            "Open My Communication from the menu to view your history.",
            "Open My Kommunikasie.",
            "Vula umlando wakho wokuxhumana."
        )
        NavigationAction.NextPage -> uiStrings.t(
            "Move to the next page with ${formatWinkSequenceShort(
                GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT,
                GuidedModeNavigation.NEXT_CATEGORY_PAGE_RIGHT
            )}.",
            "Beweeg na die volgende bladsy met L0 R4.",
            "Yiya ekhasini elilandelayo nge-L0 R4."
        )
        NavigationAction.PreviousPage -> uiStrings.t(
            "Move to the previous page with ${formatWinkSequenceShort(
                GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_LEFT,
                GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_RIGHT
            )}.",
            "Beweeg na die vorige bladsy met L4 R0.",
            "Yiya ekhasini elidlule nge-L4 R0."
        )
        NavigationAction.ResetSequence -> uiStrings.t(
            "You have learned the Communication controls. Continue with " +
                "${formatWinkSequenceShort(GuidedModeNavigation.FINISH_TRAINING_LEFT, GuidedModeNavigation.FINISH_TRAINING_RIGHT)} " +
                "to learn how to use the Menu.",
            "Jy het die Kommunikasie-kontroles geleer. Gaan voort om die Kieslys te leer.",
            "Ufunde izilawuli zokuxhumana. Qhubeka ukuze ufunde ukusebenzisa iMenyu."
        )
        NavigationAction.TriggerEmergency -> uiStrings.t(
            "This is the real Emergency alert, just like in Communication. Blink ${formatWinkSequenceShort(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS)}, " +
                "then Left then Right to confirm and send it, or Right then Left to cancel.",
            "Dit is die regte Nood-waarskuwing. Blink L6 R0, dan Links dan Regs om te bevestig, of Regs dan Links om te kanselleer.",
            "Lesi isexwayiso sesimo esiphuthumayo sangempela. Bheca L6 R0, bese Kokudla bese Kwesokudla ukuqinisekisa, noma Kwesokudla bese Kokudla ukukhansela."
        )
        NavigationAction.OpenQuickControls -> uiStrings.t(
            "Open Quick Controls with ${formatWinkSequenceShort(0, 4)}.",
            "Open Vinnige Kontroles met L0 R4.",
            "Vula Izilawuli nge-L0 R4."
        )
        NavigationAction.OpenSettings ->
            com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.instructionFor(NavigationAction.OpenSettings)
        NavigationAction.OpenCaregiver -> uiStrings.t(
            "Caregiver linking is available from the menu when configured.",
            "Versorger-koppeling is beskikbaar.",
            "Ukuxhumanisa umnakekeli kuyatholakala."
        )
        NavigationAction.OpenVoice ->
            com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.instructionFor(NavigationAction.OpenVoice)
        NavigationAction.MenuSelectVoice ->
            com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.instructionFor(NavigationAction.MenuSelectVoice)
        NavigationAction.MenuSelectSettings ->
            com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.instructionFor(
                NavigationAction.MenuSelectSettings
            )
        NavigationAction.BackFromDestination ->
            com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.instructionFor(
                NavigationAction.BackFromDestination
            )
        NavigationAction.FinishGuidedLearning ->
            com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.instructionFor(
                NavigationAction.FinishGuidedLearning
            )
        NavigationAction.AdjustSensitivity ->
            com.idworx.lisa.features.guidedsensitivitylesson.GuidedSensitivityLessonAuthority.LESSON_INTRO
    }

    private fun LisaUiStrings.t(en: String, af: String, zu: String): String = when (language) {
        PreferredLanguage.English -> en
        PreferredLanguage.Afrikaans -> af
        PreferredLanguage.IsiZulu -> zu
    }
}

/**
 * Main training flow composable — orchestrates all training phases.
 */
@androidx.compose.runtime.Composable
fun GuidedTrainingFlow(
    state: GuidedTrainingUiState,
    uiStrings: LisaUiStrings,
    language: PreferredLanguage,
    primaryUserName: String,
    isReturningUser: Boolean = false,
    onEvent: (TrainingEvent) -> Unit,
    onWelcomeNarration: () -> Unit,
    onFirstLaunchNarration: () -> Unit = {},
    onSkipConfirmNarration: () -> Unit = {},
    onCompletionNarration: () -> Unit,
    onLessonNarration: (phrase: String, instruction: String) -> Unit,
    onNavigationNarration: (title: String, instruction: String) -> Unit,
    onPrimaryUserNameChange: (String) -> Unit,
    onRequestCameraPermission: () -> Unit,
    onTouchLeftWink: () -> Unit,
    onTouchRightWink: () -> Unit,
    onReduceSensitivity: () -> Unit,
    onIncreaseSensitivity: () -> Unit = {},
    sensitivityLevel: Int = com.idworx.lisa.DEFAULT_SENSITIVITY_LEVEL,
    onDecreaseResponseTime: () -> Unit = {},
    onIncreaseResponseTime: () -> Unit = {},
    setupStep: Int,
    onSetupStepChange: (Int) -> Unit,
    eyeTracking: TrainingEyeTrackingState = TrainingEyeTrackingState(),
    eyeTrackingStatus: com.idworx.lisa.features.eyetrackingstatus.EyeTrackingStatusUiState =
        com.idworx.lisa.features.eyetrackingstatus.EyeTrackingStatusUiState(),
    blinkDiagnostics: com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionDiagnostics =
        com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionDiagnostics(),
    showBlinkDiagnostics: Boolean = false,
    onCalibrationStarted: () -> Unit = {},
    onAdvanceCalibrationDot: () -> Unit = {},
    onReplayTutorial: () -> Unit = {},
    onPracticeCommunication: () -> Unit = {},
    onPracticeNavigation: () -> Unit = {},
    onResetLearningProgress: () -> Unit = {},
    onLearningPreferencesChange: (
        com.idworx.lisa.features.onboardingguide.model.TrainingPreferences
    ) -> Unit = {}
) {
    val progress = state.progress

    androidx.compose.runtime.LaunchedEffect(
        progress.currentPhase,
        eyeTracking.eyesDetected,
        setupStep
    ) {
        if (progress.currentPhase == TrainingPhase.Setup &&
            setupStep == TrainingSessionController.SETUP_STEP_EYE_DETECTION &&
            eyeTracking.eyesDetected
        ) {
            onSetupStepChange(TrainingSessionController.SETUP_STEP_READY)
        }
    }

    when (progress.currentPhase) {
        TrainingPhase.FirstLaunchChoice -> TrainingFirstLaunchChoiceScreen(
            uiStrings = uiStrings,
            welcomeStage = state.welcomeStage,
            onContinueToDestination = { onEvent(TrainingEvent.WelcomeContinueToDestination) },
            onStartGuidedLearning = { onEvent(TrainingEvent.BeginLearning) },
            onSkipToWorkspace = { onEvent(TrainingEvent.ConfirmSkip) },
            onBackToIntroduction = { onEvent(TrainingEvent.WelcomeBackToIntroduction) },
            onSkipToNavigationTraining = { onEvent(TrainingEvent.SkipToNavigationTraining) },
            eyeTrackingStatus = eyeTrackingStatus,
            selectedChoiceLabel = state.brain1Decision.choiceLabel,
            invalidSequenceWarning = state.invalidSequenceWarning,
            onDecreaseSensitivity = onReduceSensitivity,
            onIncreaseSensitivity = onIncreaseSensitivity,
            onDecreaseResponseTime = onDecreaseResponseTime,
            onIncreaseResponseTime = onIncreaseResponseTime
        )

        TrainingPhase.Welcome -> TrainingWelcomeScreen(
            narrationSpeaking = state.narrationSpeaking,
            isReturningUser = isReturningUser,
            onNarrationStarted = onWelcomeNarration
        )

        TrainingPhase.SkipConfirm -> TrainingSkipConfirmScreen(
            narrationSpeaking = state.narrationSpeaking,
            choiceLabel = state.brain1Decision.choiceLabel,
            awaitingConfirm = state.brain1Decision.phase ==
                com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionPhase.AwaitingConfirm,
            onNarrationStarted = onSkipConfirmNarration
        )

        TrainingPhase.Setup -> TrainingSetupScreen(
            uiStrings = uiStrings,
            step = setupStep,
            eyeTracking = eyeTracking,
            sensitivityLevel = sensitivityLevel,
            onDecreaseSensitivity = onReduceSensitivity,
            onIncreaseSensitivity = onIncreaseSensitivity,
            responseTimeSec = progress.preferences.guidedResponseTimeSec,
            onDecreaseResponseTime = onDecreaseResponseTime,
            onIncreaseResponseTime = onIncreaseResponseTime,
            onRequestCameraPermission = onRequestCameraPermission,
            onAdvance = {
                onEvent(TrainingEvent.CompleteSetup)
            },
            onBack = {
                // RC8.9 — readiness Back returns to Welcome destination selection.
                onEvent(TrainingEvent.ReturnToWelcomeDestination)
            },
            eyeTrackingStatus = eyeTrackingStatus,
            invalidSequenceWarning = state.invalidSequenceWarning
        )

        TrainingPhase.Calibration -> TrainingCalibrationScreen(
            dotIndex = state.calibrationDotIndex,
            totalDots = state.calibrationTotalDots,
            onCalibrationStarted = onCalibrationStarted,
            onAdvanceDot = onAdvanceCalibrationDot
        )

        TrainingPhase.CommunicationLesson -> {
            val lesson = TrainingLessonCatalog.communicationLessonAt(progress.communicationLessonIndex)
            if (lesson != null) {
                val phrase = LisaCoreVocabulary.text(lesson.vocabularyId, language)
                val lessonProgress = TrainingLessonCatalog.guidedLessonProgress(progress)
                CommunicationLessonScreen(
                    phrase = phrase,
                    left = lesson.left,
                    right = lesson.right,
                    instruction = formatWinkInstruction(lesson.left, lesson.right),
                    eyeTracking = eyeTracking,
                    lessonInteraction = state.lessonInteraction,
                    feedback = state.feedback,
                    feedbackMessage = state.feedbackMessage,
                    blinkDiagnostics = blinkDiagnostics,
                    showBlinkDiagnostics = showBlinkDiagnostics,
                    sensitivityLevel = sensitivityLevel,
                    onDecreaseSensitivity = onReduceSensitivity,
                    onIncreaseSensitivity = onIncreaseSensitivity,
                    responseTimeSec = progress.preferences.guidedResponseTimeSec,
                    onDecreaseResponseTime = onDecreaseResponseTime,
                    onIncreaseResponseTime = onIncreaseResponseTime,
                    coachPacingBlocked = state.coachPacingBlocked,
                    lessonNumber = lessonProgress?.first,
                    totalLessons = lessonProgress?.second,
                    onLessonNarration = {
                        onLessonNarration(phrase, formatWinkInstruction(lesson.left, lesson.right))
                    },
                    uiStrings = uiStrings,
                    eyeTrackingStatus = eyeTrackingStatus
                )
            }
        }

        TrainingPhase.CommunicationMastery -> {
            val lesson = TrainingLessonCatalog.masteryLessonAt(
                progress.masteryRoundIndex,
                progress.masteryPhraseOrder
            )
            if (lesson != null) {
                val phrase = LisaCoreVocabulary.text(lesson.vocabularyId, language)
                val lessonProgress = TrainingLessonCatalog.guidedLessonProgress(progress)
                CommunicationLessonScreen(
                    phrase = phrase,
                    left = lesson.left,
                    right = lesson.right,
                    instruction = formatWinkInstruction(lesson.left, lesson.right),
                    eyeTracking = eyeTracking,
                    lessonInteraction = state.lessonInteraction,
                    feedback = state.feedback,
                    feedbackMessage = state.feedbackMessage,
                    blinkDiagnostics = blinkDiagnostics,
                    showBlinkDiagnostics = showBlinkDiagnostics,
                    sensitivityLevel = sensitivityLevel,
                    onDecreaseSensitivity = onReduceSensitivity,
                    onIncreaseSensitivity = onIncreaseSensitivity,
                    responseTimeSec = progress.preferences.guidedResponseTimeSec,
                    onDecreaseResponseTime = onDecreaseResponseTime,
                    onIncreaseResponseTime = onIncreaseResponseTime,
                    coachPacingBlocked = state.coachPacingBlocked,
                    lessonNumber = lessonProgress?.first,
                    totalLessons = lessonProgress?.second,
                    onLessonNarration = {
                        onLessonNarration(phrase, formatWinkInstruction(lesson.left, lesson.right))
                    },
                    uiStrings = uiStrings,
                    eyeTrackingStatus = eyeTrackingStatus
                )
            }
        }

        TrainingPhase.NavigationLesson -> {
            val lesson = TrainingLessonCatalog.navigationLessonAt(progress.navigationLessonIndex)
            if (lesson != null) {
                val lessonProgress = TrainingLessonCatalog.guidedLessonProgress(progress)
                NavigationLessonScreen(
                    title = NavigationLessonContent.title(lesson.action, uiStrings),
                    instruction = NavigationLessonContent.instruction(lesson.action, uiStrings),
                    awaitingAction = state.awaitingNavigationAction,
                    lessonNumber = lessonProgress?.first,
                    totalLessons = lessonProgress?.second,
                    onNarration = {
                        onNavigationNarration(
                            NavigationLessonContent.title(lesson.action, uiStrings),
                            NavigationLessonContent.instruction(lesson.action, uiStrings)
                        )
                    }
                )
            }
        }

        TrainingPhase.Completion -> TrainingCompletionScreen(
            uiStrings = uiStrings,
            showCelebration = state.showCelebration,
            onNarrationStarted = onCompletionNarration,
            eyeTrackingStatus = eyeTrackingStatus,
            onStartCommunicating = { onEvent(TrainingEvent.StartUsingLisa) },
            onRestartGuidedLearning = {
                onEvent(TrainingEvent.ReplayTutorial)
            },
            onDecreaseSensitivity = onReduceSensitivity,
            onIncreaseSensitivity = onIncreaseSensitivity,
            onDecreaseResponseTime = onDecreaseResponseTime,
            onIncreaseResponseTime = onIncreaseResponseTime
        )
    }
}

private fun LisaUiStrings.t(en: String, af: String, zu: String): String = when (language) {
    PreferredLanguage.English -> en
    PreferredLanguage.Afrikaans -> af
    PreferredLanguage.IsiZulu -> zu
}

/** Returns true when training should occupy the full screen (blocking main UI). */
fun trainingBlocksMainUi(phase: TrainingPhase): Boolean = when (phase) {
    TrainingPhase.NavigationLesson -> false
    else -> true
}

/** Apply a training event and return updated UI state. */
fun applyTrainingEvent(
    state: GuidedTrainingUiState,
    event: TrainingEvent,
    navigator: GuidedTrainingNavigator = GuidedTrainingNavigator(),
    feedbackSeed: Int = (0..100).random(),
    phraseLabel: String? = null
): GuidedTrainingUiState {
    val coachDecision = coachDecisionForEvent(state, event, phraseLabel)
    val newProgress = navigator.reduce(state.progress, event)
    val (feedback, message) = navigator.feedbackFor(event, feedbackSeed, newProgress)
    val resolvedPhrase = phraseLabel ?: coachDecision?.let {
        CommunicationCoachEngine.currentCommunicationLesson(state.progress)?.vocabularyId
    }
    val caregiverSnapshot = CommunicationCoachEngine.caregiverSnapshot(
        newProgress,
        CommunicationCoachEngine.currentCommunicationLesson(newProgress),
        resolvedPhrase
    )
    val adaptiveOffer = navigator.adaptiveOffer(newProgress)
    val coachInstruction = coachInstructionFor(state, newProgress, coachDecision, resolvedPhrase)
    val showCelebration = when {
        newProgress.currentPhase == TrainingPhase.Completion -> true
        coachDecision?.showCelebrationOverlay == true -> true
        else -> false
    }
    return state.copy(
        progress = newProgress,
        feedback = if (event is TrainingEvent.NavigationActionCompleted) TrainingFeedback.Success else feedback,
        feedbackMessage = coachDecision?.learnerMessage ?: if (event is TrainingEvent.NavigationActionCompleted) {
            com.idworx.lisa.features.personality.engine.LisaPersonalityEngines.default
                .generateEncouragement(
                    com.idworx.lisa.features.onboardingguide.services.TrainingDialogueContext.from(
                        state.copy(progress = newProgress),
                        com.idworx.lisa.features.onboardingguide.services.TrainingDialogueContext.DialogueContextExtras(
                            deterministicSeed = feedbackSeed
                        )
                    )
                ).text
        } else {
            message
        },
        adaptiveOffer = adaptiveOffer,
        coachUiState = PatientCommunicationCoachEngine.buildCoachUiState(
            newProgress,
            adaptiveOffer,
            caregiverSnapshot,
            coachDecision
        ),
        showCelebration = showCelebration,
        awaitingNavigationAction = newProgress.currentPhase == TrainingPhase.NavigationLesson,
        coachDecision = coachDecision,
        caregiverSnapshot = caregiverSnapshot,
        coachInstruction = coachInstruction
    )
}

private fun coachDecisionForEvent(
    state: GuidedTrainingUiState,
    event: TrainingEvent,
    phraseLabel: String?
): CoachLessonDecision? {
    return when (event) {
        TrainingEvent.SequenceSuccess -> {
            val interim = state.progress.copy(
                statistics = state.progress.statistics.recordAttempt(success = true),
                currentLessonSuccessCount = state.progress.currentLessonSuccessCount + 1
            )
            val lesson = CommunicationCoachEngine.currentCommunicationLesson(interim) ?: return null
            CommunicationCoachEngine.evaluateSuccess(
                interim,
                lesson,
                CommunicationCoachEngine.previousCommunicationLesson(state.progress),
                phraseLabel ?: lesson.vocabularyId
            )
        }
        TrainingEvent.SequenceRetry,
        TrainingEvent.SequenceAlmost -> CommunicationCoachEngine.evaluateRetry(state.progress)
        else -> null
    }
}

private fun coachInstructionFor(
    state: GuidedTrainingUiState,
    progress: com.idworx.lisa.features.onboardingguide.model.TrainingProgress,
    coachDecision: CoachLessonDecision?,
    phraseLabel: String?
): String? {
    val lesson = CommunicationCoachEngine.currentCommunicationLesson(progress) ?: return null
    return formatWinkInstruction(lesson.left, lesson.right)
}

private fun coachInstructionText(state: GuidedTrainingUiState, phrase: String): String {
    val lesson = CommunicationCoachEngine.currentCommunicationLesson(state.progress)
    return if (lesson != null) {
        formatWinkInstruction(lesson.left, lesson.right)
    } else {
        "When you're ready, try the gesture for $phrase."
    }
}
