package com.idworx.lisa.features.onboardingguide.services

import com.idworx.lisa.LisaCoreVocabulary
import com.idworx.lisa.PreferredLanguage
import com.idworx.lisa.features.onboardingguide.audio.OnboardingNarrationController
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.navigation.GuidedTrainingNavigator
import com.idworx.lisa.features.onboardingguide.lessoninteraction.LessonInteractionEngine
import com.idworx.lisa.features.onboardingguide.lessoninteraction.GuidedFeedbackPhrases
import com.idworx.lisa.features.onboardingguide.model.CommunicationLesson
import com.idworx.lisa.features.onboardingguide.state.GuidedTrainingUiState
import com.idworx.lisa.features.onboardingguide.state.LessonInteractionState
import com.idworx.lisa.features.onboardingguide.state.TrainingEvent
import com.idworx.lisa.features.onboardingguide.state.TrainingFeedback
import com.idworx.lisa.features.onboardingguide.ui.applyTrainingEvent
import com.idworx.lisa.features.companionmemory.engine.CompanionMemoryEngine
import com.idworx.lisa.features.companionmemory.engine.CompanionMemoryEngines
import com.idworx.lisa.features.companionmemory.integration.GuidedLearningMemoryAdapter
import com.idworx.lisa.features.onboardingguide.journey.LearningJourneyDialogues
import com.idworx.lisa.features.calibrationreliability.engine.CalibrationReliabilityEngines
import com.idworx.lisa.features.calibrationreliability.model.CalibrationQualityCategory
import com.idworx.lisa.features.calibrationreliability.model.CalibrationSessionSource
import com.idworx.lisa.features.companionmemory.integration.PersonalityMemoryAdapter
import com.idworx.lisa.features.brain1interactionstandard.dialogue.Brain1DecisionDialogueProvider
import com.idworx.lisa.features.brain1interactionstandard.engine.Brain1DecisionEngine
import com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionKind
import com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionOutcome
import com.idworx.lisa.features.experiencepolish.firstfiveminutes.FirstFiveMinutesExperience
import com.idworx.lisa.features.experiencepolish.patientcommunicationcoach.PatientCommunicationCoachExperience
import com.idworx.lisa.features.experiencepolish.emotionalpresence.EmotionalPresenceEngine
import com.idworx.lisa.features.experiencepolish.emotionalpresence.model.PresenceSessionTracker
import com.idworx.lisa.features.personality.model.DialogueContext
import com.idworx.lisa.features.personality.model.PresenceMoment
import com.idworx.lisa.features.onboardingguide.coach.CoachCelebrationTier
import com.idworx.lisa.features.onboardingguide.coach.CoachLessonDecision
import com.idworx.lisa.features.onboardingguide.coach.CoachPacingAction
import com.idworx.lisa.features.onboardingguide.coach.CommunicationCoachEngine
import com.idworx.lisa.features.experiencepolish.patientcommunicationcoach.PatientCommunicationCoachEngine
import com.idworx.lisa.features.silentwelcome.LisaSpeechPolicy
import kotlin.math.abs

/**
 * Host-side training session logic extracted from MainActivity for modularity.
 */
class TrainingSessionController(
    private val store: TrainingProgressStore,
    private val narration: OnboardingNarrationController,
    private val speakPhrase: (String) -> Unit,
    private val onPersist: (GuidedTrainingUiState) -> Unit,
    private val onTrainingFinished: () -> Unit,
    private val onCompleteSetupOnboarding: () -> Unit,
    private val personality: com.idworx.lisa.features.personality.engine.LisaPersonalityEngine =
        com.idworx.lisa.features.personality.engine.LisaPersonalityEngines.default,
    private val memory: CompanionMemoryEngine = CompanionMemoryEngines.default
) {
    private val navigator = GuidedTrainingNavigator()
    private val calibrationEngine = CalibrationReliabilityEngines.default
    private var calibrationSession = calibrationEngine.startSession(
        totalPoints = 5,
        sensitivityLevel = null,
        source = CalibrationSessionSource.GuidedLearning
    )
    var state: GuidedTrainingUiState = GuidedTrainingUiState.fromProgress(store.load())
        private set

    var setupStep: Int = 0
        private set

    /** Invoked when user confirms Emergency Mode via L1 R1. */
    var onEmergencyConfirmed: (() -> Unit)? = null

    /** Invoked when user confirms Recalibration via L1 R1. */
    var onRecalibrationConfirmed: (() -> Unit)? = null

    private var pendingInteractiveLessonSuccess: PendingInteractiveLessonSuccess? = null
    private var partialTimeoutCount = 0
    private var wrongEyeRestartCount = 0

    private data class PendingInteractiveLessonSuccess(
        val phrase: String,
        val lesson: CommunicationLesson,
        val phase: TrainingPhase
    )

    fun hasPendingInteractiveLessonSuccess(): Boolean = pendingInteractiveLessonSuccess != null

    fun isCommunicationLessonPhase(): Boolean {
        val phase = state.progress.currentPhase
        return phase == TrainingPhase.CommunicationLesson || phase == TrainingPhase.CommunicationMastery
    }

    private fun currentCommunicationLesson(): CommunicationLesson? = when (state.progress.currentPhase) {
        TrainingPhase.CommunicationLesson ->
            TrainingLessonCatalog.communicationLessonAt(state.progress.communicationLessonIndex)
        TrainingPhase.CommunicationMastery ->
            TrainingLessonCatalog.masteryLessonAt(
                state.progress.masteryRoundIndex,
                state.progress.masteryPhraseOrder
            )
        else -> null
    }

    /**
     * Updates live lesson blink feedback. Returns true when the blink should reset the buffer.
     */
    fun onLessonWink(isLeft: Boolean, left: Int, right: Int, blinkOrder: List<Boolean>): Boolean {
        if (!isCommunicationLessonPhase()) return false
        val lesson = currentCommunicationLesson() ?: return false
        if (state.lessonInteraction.awaitingSuccessSpeech) return false

        if (!LessonInteractionEngine.isValidPartial(left, right, blinkOrder, lesson)) {
            applyInteractiveRetryVisual(state.progress.statistics.consecutiveFailures)
            return true
        }

        val progressLabel = LessonInteractionEngine.totalSequenceProgressLabel(left, right, lesson)
        val waitingFor = LessonInteractionEngine.waitingForLabel(lesson, blinkOrder, left, right)
        state = state.copy(
            feedback = TrainingFeedback.None,
            feedbackMessage = null,
            leftWinkDots = left,
            rightWinkDots = right,
            lessonInteraction = state.lessonInteraction.copy(
                detectedProgress = progressLabel,
                liveLeftBlinks = left,
                liveRightBlinks = right,
                retryVisualMessage = null,
                successVisualMessage = null,
                wrongEyeMessage = null,
                waitingForLabel = waitingFor
            )
        )
        onPersist(state)
        return false
    }

    fun isPartialSequenceInProgress(left: Int, right: Int, blinkOrder: List<Boolean>): Boolean {
        if (!isCommunicationLessonPhase()) return false
        val lesson = currentCommunicationLesson() ?: return false
        if (state.lessonInteraction.awaitingSuccessSpeech) return false
        return LessonInteractionEngine.isPartialSequenceInProgress(left, right, blinkOrder, lesson)
    }

    fun isWrongEyeBlink(isLeft: Boolean, left: Int, right: Int, blinkOrder: List<Boolean>): Boolean {
        if (!isCommunicationLessonPhase()) return false
        val lesson = currentCommunicationLesson() ?: return false
        if (state.lessonInteraction.awaitingSuccessSpeech) return false
        return LessonInteractionEngine.isWrongEyeBlink(isLeft, left, right, blinkOrder, lesson)
    }

    fun applyWrongEyeFeedback() {
        val lesson = currentCommunicationLesson() ?: return
        val message = LessonInteractionEngine.wrongEyeRestartFeedbackMessage(lesson, wrongEyeRestartCount++)
        state = state.copy(
            leftWinkDots = 0,
            rightWinkDots = 0,
            lessonInteraction = LessonInteractionState(
                wrongEyeMessage = message,
                detectedProgress = null,
                liveLeftBlinks = 0,
                liveRightBlinks = 0
            )
        )
        onPersist(state)
        mainThreadDelayed(WRONG_EYE_FEEDBACK_CLEAR_MS) {
            if (state.lessonInteraction.wrongEyeMessage == message) {
                state = state.copy(
                    lessonInteraction = state.lessonInteraction.copy(wrongEyeMessage = null)
                )
                onPersist(state)
            }
        }
    }

    fun applyPartialSequenceTimeout() {
        if (!isCommunicationLessonPhase()) return
        val msg = LessonInteractionEngine.partialTimeoutVisualMessage(partialTimeoutCount++)
        state = state.copy(
            feedback = TrainingFeedback.Retry,
            feedbackMessage = msg,
            leftWinkDots = 0,
            rightWinkDots = 0,
            lessonInteraction = LessonInteractionState(
                retryVisualMessage = msg,
                detectedProgress = null,
                liveLeftBlinks = 0,
                liveRightBlinks = 0
            )
        )
        onPersist(state)
        mainThreadDelayed(RETRY_FEEDBACK_CLEAR_MS) { clearLessonInteractionFeedback() }
    }

    fun clearLessonInteractionFeedback() {
        if (state.lessonInteraction == LessonInteractionState()) return
        state = state.copy(
            feedback = TrainingFeedback.None,
            feedbackMessage = null,
            lessonInteraction = LessonInteractionState()
        )
        onPersist(state)
    }

    fun completePendingInteractiveLessonSuccess() {
        val pending = pendingInteractiveLessonSuccess ?: return
        pendingInteractiveLessonSuccess = null

        val wasFirstSuccess = state.progress.statistics.successfulAttempts == 0
        val beforePhase = pending.phase
        val beforeLessonIndex = state.progress.communicationLessonIndex
        val beforeMasteryIndex = state.progress.masteryRoundIndex

        val newProgress = navigator.reduce(state.progress, TrainingEvent.SequenceSuccess)
        state = state.copy(
            progress = newProgress,
            feedback = TrainingFeedback.None,
            feedbackMessage = null,
            showCelebration = false,
            leftWinkDots = 0,
            rightWinkDots = 0,
            lessonInteraction = LessonInteractionState(),
            coachDecision = null,
            coachInstruction = null,
            caregiverSnapshot = null,
            coachUiState = null
        )
        store.save(state.progress)
        onPersist(state)

        if (wasFirstSuccess) {
            GuidedLearningMemoryAdapter.onFirstSuccessfulBlink(memory)
            GuidedLearningMemoryAdapter.onFirstSpokenPhrase(memory, pending.lesson.id)
        }
        GuidedLearningMemoryAdapter.onLessonCompleted(
            memory,
            pending.lesson.id,
            if (beforePhase == TrainingPhase.CommunicationMastery) "Mastery" else "Fundamentals",
            state.progress.statistics.totalAttempts
        )
        GuidedLearningMemoryAdapter.checkPhaseCompletions(memory, state.progress.completedLessonIds)
        recordMemoryForEvent(TrainingEvent.SequenceSuccess, state)

        val advanced = state.progress.communicationLessonIndex != beforeLessonIndex ||
            state.progress.masteryRoundIndex != beforeMasteryIndex ||
            state.progress.currentPhase != beforePhase
        if (advanced && LisaSpeechPolicy.allowsNarration()) {
            mainThreadDelayed {
                when {
                    beforePhase == TrainingPhase.CommunicationLesson &&
                        state.progress.currentPhase == TrainingPhase.CommunicationMastery ->
                        fundamentalsCompleteNarration()
                    beforePhase == TrainingPhase.CommunicationMastery &&
                        state.progress.currentPhase == TrainingPhase.NavigationLesson ->
                        masteryCompleteNarration()
                }
            }
        }
    }

    private fun applyInteractiveRetryVisual(consecutiveFailures: Int) {
        val retryMsg = LessonInteractionEngine.retryVisualMessage(consecutiveFailures)
        dispatch(TrainingEvent.SequenceRetry)
        state = state.copy(
            feedback = TrainingFeedback.Retry,
            feedbackMessage = retryMsg,
            showCelebration = false,
            leftWinkDots = 0,
            rightWinkDots = 0,
            lessonInteraction = LessonInteractionState(
                retryVisualMessage = retryMsg,
                detectedProgress = null
            ),
            coachDecision = null,
            coachInstruction = null
        )
        onPersist(state)
        mainThreadDelayed(RETRY_FEEDBACK_CLEAR_MS) { clearLessonInteractionFeedback() }
    }

    /**
     * Sequence finalized and matched. Speaks the translated phrase first, while the lesson
     * phrase/gesture stays visible (no "Well done" yet) so the visual never gets ahead of the
     * speech. [onPhraseSpeechFinished] reveals "Well done" only after the phrase is spoken.
     */
    private fun beginInteractiveLessonSuccess(
        phrase: String,
        lesson: CommunicationLesson,
        phase: TrainingPhase
    ) {
        pendingInteractiveLessonSuccess = PendingInteractiveLessonSuccess(phrase, lesson, phase)
        state = state.copy(
            feedback = TrainingFeedback.None,
            feedbackMessage = null,
            showCelebration = false,
            lessonInteraction = LessonInteractionState(
                successVisualMessage = null,
                awaitingSuccessSpeech = true,
                detectedProgress = null,
                liveLeftBlinks = 0,
                liveRightBlinks = 0
            ),
            coachDecision = null,
            coachInstruction = null,
            caregiverSnapshot = null,
            coachUiState = null
        )
        onPersist(state)
        speakPhrase(phrase)
    }

    /**
     * Called once TTS finishes speaking the translated phrase. Reveals "Well done", holds it
     * briefly so the visual and speech never contradict each other, then advances the lesson.
     */
    fun onPhraseSpeechFinished(onAdvanced: () -> Unit = {}) {
        val pending = pendingInteractiveLessonSuccess
        if (pending == null) {
            onAdvanced()
            return
        }
        val successMsg = LessonInteractionEngine.successVisualMessage(
            when (pending.phase) {
                TrainingPhase.CommunicationMastery -> state.progress.masteryRoundIndex
                else -> state.progress.communicationLessonIndex
            }
        )
        state = state.copy(
            feedback = TrainingFeedback.Success,
            feedbackMessage = successMsg,
            lessonInteraction = state.lessonInteraction.copy(
                successVisualMessage = successMsg,
                awaitingSuccessSpeech = true
            )
        )
        onPersist(state)
        mainThreadDelayed(SUCCESS_VISUAL_PAUSE_MS) {
            completePendingInteractiveLessonSuccess()
            onAdvanced()
        }
    }

    fun hasActiveBrain1Decision(): Boolean =
        state.brain1Decision.isActive ||
            state.progress.currentPhase == TrainingPhase.FirstLaunchChoice ||
            state.progress.currentPhase == TrainingPhase.SkipConfirm

    fun firstLaunchChoiceNarration() {
        if (!LisaSpeechPolicy.allowsNarration()) return
        state = state.copy(
            brain1Decision = Brain1DecisionEngine.beginDecision(Brain1DecisionKind.FirstLaunchGuidedLearning)
        )
        onPersist(state)
        speakBrain1Dialogues(Brain1DecisionDialogueProvider.firstLaunchPrompt())
    }

    fun skipConfirmNarration() {
        if (!LisaSpeechPolicy.allowsNarration()) return
        state = state.copy(
            brain1Decision = Brain1DecisionEngine.beginAwaitingConfirm(
                Brain1DecisionKind.FirstLaunchSkipWorkspace
            )
        )
        onPersist(state)
        speakBrain1Dialogues(
            Brain1DecisionDialogueProvider.repeatForKind(
                Brain1DecisionKind.FirstLaunchSkipWorkspace,
                Brain1DecisionEngine.choiceLabel(Brain1DecisionKind.FirstLaunchSkipWorkspace)
            )
        )
    }

    fun beginAwaitingBrain1Decision(kind: Brain1DecisionKind) {
        state = state.copy(brain1Decision = Brain1DecisionEngine.beginAwaitingConfirm(kind))
        onPersist(state)
        speakBrain1Dialogues(
            Brain1DecisionDialogueProvider.repeatForKind(kind, Brain1DecisionEngine.choiceLabel(kind))
        )
    }

    fun beginEmergencyConfirm() {
        beginAwaitingBrain1Decision(Brain1DecisionKind.EmergencyMode)
    }

    fun handleBrain1Interaction(left: Int, right: Int, blinkOrder: List<Boolean> = emptyList()): Boolean {
        if (!hasActiveBrain1Decision()) return false

        val current = when {
            state.brain1Decision.isActive -> state.brain1Decision
            state.progress.currentPhase == TrainingPhase.FirstLaunchChoice ->
                Brain1DecisionEngine.beginDecision(Brain1DecisionKind.FirstLaunchGuidedLearning)
            state.progress.currentPhase == TrainingPhase.SkipConfirm ->
                Brain1DecisionEngine.beginAwaitingConfirm(Brain1DecisionKind.FirstLaunchSkipWorkspace)
            else -> return false
        }

        val (updated, outcome) = Brain1DecisionEngine.handleSequence(current, left, right, blinkOrder)
        state = state.copy(brain1Decision = updated)
        onPersist(state)

        when (outcome) {
            is Brain1DecisionOutcome.ChoiceDetected ->
                speakBrain1Dialogues(
                    Brain1DecisionDialogueProvider.repeatForKind(outcome.kind, outcome.label)
                )
            Brain1DecisionOutcome.ChooseAgain -> when (state.progress.currentPhase) {
                TrainingPhase.FirstLaunchChoice -> firstLaunchChoiceNarration()
                TrainingPhase.SkipConfirm -> {
                    state = state.copy(
                        progress = state.progress.copy(currentPhase = TrainingPhase.FirstLaunchChoice),
                        brain1Decision = Brain1DecisionEngine.beginDecision(
                            Brain1DecisionKind.FirstLaunchGuidedLearning
                        )
                    )
                    store.save(state.progress)
                    onPersist(state)
                    firstLaunchChoiceNarration()
                }
                else -> beginAwaitingBrain1Decision(updated.kind)
            }
            is Brain1DecisionOutcome.Confirmed -> executeBrain1Confirmed(outcome.kind)
            Brain1DecisionOutcome.None -> Unit
        }
        return true
    }

    private fun executeBrain1Confirmed(kind: Brain1DecisionKind) {
        state = state.copy(brain1Decision = state.brain1Decision.clear())
        onPersist(state)
        when (kind) {
            Brain1DecisionKind.FirstLaunchGuidedLearning -> dispatch(TrainingEvent.BeginLearning)
            Brain1DecisionKind.FirstLaunchSkipWorkspace -> dispatch(TrainingEvent.ConfirmSkip)
            Brain1DecisionKind.EmergencyMode -> onEmergencyConfirmed?.invoke()
            Brain1DecisionKind.ResetLearningProgress -> {
                dispatch(TrainingEvent.ResetProgress)
                mainThreadDelayed { firstLaunchChoiceNarration() }
            }
            Brain1DecisionKind.ReplayLearning -> {
                dispatch(TrainingEvent.ReplayTutorial)
                mainThreadDelayed { welcomeNarration() }
            }
            Brain1DecisionKind.Recalibration -> onRecalibrationConfirmed?.invoke()
            Brain1DecisionKind.None -> Unit
        }
    }

    fun startRecalibrationFlow() {
        state = state.copy(
            progress = state.progress.copy(
                currentPhase = TrainingPhase.Calibration,
                calibrationCompleted = false
            ),
            calibrationDotIndex = 0
        )
        calibrationSession = calibrationEngine.startSession(
            totalPoints = 5,
            sensitivityLevel = null,
            source = CalibrationSessionSource.GuidedLearning
        )
        store.save(state.progress)
        onPersist(state)
        calibrationNarration()
    }

    private fun speakBrain1Dialogues(lines: List<String>) {
        if (!LisaSpeechPolicy.allowsNarration()) return
        narration.speakSequence(lines)
    }

    fun shouldShowTraining(): Boolean =
        state.progress.currentPhase in ACTIVE_TRAINING_PHASES || state.progress.practiceModeOnly

    fun blocksMainUi(): Boolean =
        shouldShowTraining() && com.idworx.lisa.features.onboardingguide.ui.trainingBlocksMainUi(state.phase)

    fun isNavigationTrainingActive(): Boolean =
        state.progress.currentPhase == TrainingPhase.NavigationLesson &&
            (shouldShowTraining() || state.progress.practiceNavigation)

    fun shouldPauseForCalibration(): Boolean =
        com.idworx.lisa.features.calibrationreliability.integration.CalibrationGuidedLearningAdapter
            .shouldBlockLesson(com.idworx.lisa.features.calibrationreliability.engine.CalibrationReliabilityEngines.default)

    fun dispatch(event: TrainingEvent) {
        val before = state
        when (event) {
            TrainingEvent.PauseNarration -> narration.pause()
            TrainingEvent.ResumeNarration -> narration.resume()
            TrainingEvent.RepeatNarration -> narration.repeatLast()
            TrainingEvent.SkipNarration -> narration.skip()
            TrainingEvent.BeginLearning -> {
                state = applyTrainingEvent(state, event, navigator).copy(setupStep = 0)
                setupStep = 0
                GuidedLearningMemoryAdapter.onMetLisa(memory)
            }
            TrainingEvent.ConfirmSkip -> {
                state = applyTrainingEvent(state, event, navigator)
                onCompleteSetupOnboarding()
                onTrainingFinished()
            }
            TrainingEvent.StartUsingLisa -> {
                state = applyTrainingEvent(state, event, navigator)
                onCompleteSetupOnboarding()
                onTrainingFinished()
            }
            TrainingEvent.ResetProgress -> {
                store.reset()
                state = GuidedTrainingUiState.fromProgress(store.load()).copy(setupStep = 0)
                setupStep = 0
            }
            TrainingEvent.ReplayTutorial -> {
                store.reset()
                state = applyTrainingEvent(
                    GuidedTrainingUiState.fromProgress(store.load()),
                    TrainingEvent.ReplayTutorial,
                    navigator
                ).copy(setupStep = 0)
                setupStep = 0
            }
            TrainingEvent.ExitPractice -> {
                state = applyTrainingEvent(state, event, navigator)
            }
            else -> {
                state = applyTrainingEvent(state, event, navigator)
            }
        }
        store.save(state.progress)
        onPersist(state)
        recordMemoryForEvent(event, before)
        if (state.progress.tutorialCompleted && !state.progress.practiceModeOnly) {
            onCompleteSetupOnboarding()
        }
    }

    fun welcomeNarration() {
        if (!LisaSpeechPolicy.allowsNarration()) return
        if (isReturningUser()) {
            val greeting = memory.getGreetingContext()
            val base = TrainingDialogueContext.from(
                state,
                TrainingDialogueContext.DialogueContextExtras(firstLaunch = greeting.isFirstLaunch)
            )
            val ctx = PersonalityMemoryAdapter.enrichDialogueContext(base, greeting)
            val moment = PresenceMoment.WarmReturnGreeting
            if (EmotionalPresenceEngine.shouldSpeak(moment, ctx, presenceTracker)) {
                val lines = EmotionalPresenceEngine.dialogueTexts(ctx, moment)
                if (lines.isNotEmpty()) {
                    narration.speakSequence(lines)
                    presenceTracker = EmotionalPresenceEngine.recordSpoken(presenceTracker, moment)
                    return
                }
            }
            narration.speakDialogueSequence(personality.generateGreetingSequence(ctx))
        } else {
            narration.speakSequence(FirstFiveMinutesExperience.meetLisaDialogues())
        }
    }

    fun setupNarration() {
        if (!LisaSpeechPolicy.allowsNarration()) return
        setupFaceAnnounced = false
        narration.speakSequence(FirstFiveMinutesExperience.gettingReadyDialogues())
    }

    fun onFaceDetectedDuringSetup() {
        if (state.progress.currentPhase != TrainingPhase.Setup || setupFaceAnnounced) return
        setupFaceAnnounced = true
        if (!LisaSpeechPolicy.allowsNarration()) return
        narration.speakSequence(FirstFiveMinutesExperience.faceDetectedDialogues())
        pendingAfterNarration = PendingNarration.AfterFaceDetected
    }

    /** Step 0 → step 1 (ready check). Does not open HELLO — user must press Continue. */
    fun advanceSetupToReadyCheck() {
        if (state.progress.currentPhase != TrainingPhase.Setup) return
        if (setupStep >= SETUP_STEP_READY) return
        setSetupStep(SETUP_STEP_READY)
    }

    fun onFaceLostDuringSetup() {
        if (state.progress.currentPhase != TrainingPhase.Setup) return
        if (!LisaSpeechPolicy.allowsNarration()) return
        val now = System.currentTimeMillis()
        if (now - lastPatienceSpokenMs < 12_000L) return
        lastPatienceSpokenMs = now
        narration.speakSequence(FirstFiveMinutesExperience.faceLostDialogues())
    }

    fun onFaceWaitingDuringSetup() {
        if (state.progress.currentPhase != TrainingPhase.Setup || setupFaceAnnounced) return
        if (!LisaSpeechPolicy.allowsNarration()) return
        val now = System.currentTimeMillis()
        if (now - lastPatienceSpokenMs < 15_000L) return
        lastPatienceSpokenMs = now
        narration.speakSequence(FirstFiveMinutesExperience.faceWaitingDialogues())
    }

    fun patienceNarration() {
        if (!LisaSpeechPolicy.allowsNarration()) return
        val now = System.currentTimeMillis()
        if (now - lastPatienceSpokenMs < 20_000L) return
        lastPatienceSpokenMs = now
        val lines = when (state.progress.currentPhase) {
            TrainingPhase.CommunicationLesson,
            TrainingPhase.CommunicationMastery -> PatientCommunicationCoachExperience.patienceDialogues()
            else -> FirstFiveMinutesExperience.patienceDialogues()
        }
        narration.speakSequence(lines)
    }

    fun coachRepeatNarration() {
        if (!LisaSpeechPolicy.allowsNarration()) return
        val now = System.currentTimeMillis()
        if (now - lastPatienceSpokenMs < 12_000L) return
        lastPatienceSpokenMs = now
        narration.speakSequence(PatientCommunicationCoachExperience.repeatPhraseDialogues())
    }

    fun coachSlowDownNarration() {
        if (!LisaSpeechPolicy.allowsNarration()) return
        val now = System.currentTimeMillis()
        if (now - lastPatienceSpokenMs < 15_000L) return
        lastPatienceSpokenMs = now
        narration.speakSequence(PatientCommunicationCoachExperience.slowDownDialogues())
    }

    fun gentleMissedBlinkNarration() {
        if (!LisaSpeechPolicy.allowsNarration()) return
        narration.speakSequence(FirstFiveMinutesExperience.gentleMissedBlinkDialogues())
    }

    private var pendingAfterNarration: PendingNarration = PendingNarration.None
    private var lastCalibrationPoor: Boolean = false
    private var setupFaceAnnounced: Boolean = false
    private var lastPatienceSpokenMs: Long = 0L
    private var pendingHelloPhrase: String? = null
    private var lastCompletedLessonId: String? = null
    private var pendingCoachPhrase: String? = null
    private var pendingCoachInstruction: String? = null
    private var presenceTracker = PresenceSessionTracker()

    private enum class PendingNarration {
        None,
        AfterFaceDetected,
        AfterCalibrationResult,
        AfterFirstHelloCelebration,
        AfterFirstHelloEncouragement,
        AfterNextPhraseIntro,
        AfterCoachIntro,
        AfterFundamentals,
        AfterMastery
    }

    fun onNarrationSequenceComplete() {
        when (pendingAfterNarration) {
            PendingNarration.AfterFaceDetected -> {
                pendingAfterNarration = PendingNarration.None
                advanceSetupToReadyCheck()
            }
            PendingNarration.AfterFirstHelloCelebration -> {
                pendingAfterNarration = PendingNarration.None
                pendingHelloPhrase?.let { phrase ->
                    speakPhrase(phrase)
                    mainThreadDelayed {
                        pendingAfterNarration = PendingNarration.AfterFirstHelloEncouragement
                        narration.speakSequence(FirstFiveMinutesExperience.firstHelloEncouragementDialogues())
                    }
                }
            }
            PendingNarration.AfterFirstHelloEncouragement -> {
                pendingAfterNarration = PendingNarration.None
                pendingHelloPhrase = null
                pendingAfterNarration = PendingNarration.AfterNextPhraseIntro
                narration.speakSequence(FirstFiveMinutesExperience.nextPhraseIntroDialogues())
            }
            PendingNarration.AfterNextPhraseIntro -> {
                pendingAfterNarration = PendingNarration.None
                val yesLesson = TrainingLessonCatalog.communicationLessonAt(1)
                yesLesson?.let { lesson ->
                    val phrase = LisaCoreVocabulary.text(lesson.vocabularyId, PreferredLanguage.English)
                    coachBeginLesson(phrase, "")
                }
            }
            PendingNarration.AfterCoachIntro -> {
                pendingAfterNarration = PendingNarration.None
                val phrase = pendingCoachPhrase.orEmpty()
                val instruction = pendingCoachInstruction.orEmpty()
                pendingCoachPhrase = null
                pendingCoachInstruction = null
                lessonNarration(phrase, instruction)
            }
            PendingNarration.AfterCalibrationResult -> {
                pendingAfterNarration = PendingNarration.None
                if (lastCalibrationPoor && calibrationSession.retries < 2) {
                    dispatch(TrainingEvent.CalibrationRetry)
                    state = state.copy(calibrationDotIndex = 0, calibrationPoorRetry = true)
                    onPersist(state)
                    calibrationSession = calibrationEngine.startSession(5, null, CalibrationSessionSource.GuidedLearning)
                    mainThreadDelayed { calibrationNarration() }
                } else {
                    dispatch(TrainingEvent.CalibrationComplete)
                    state = state.copy(calibrationDotIndex = 0, calibrationPoorRetry = false)
                    onPersist(state)
                }
            }
            PendingNarration.AfterFundamentals -> {
                pendingAfterNarration = PendingNarration.None
                masteryIntroNarration()
            }
            PendingNarration.AfterMastery -> {
                pendingAfterNarration = PendingNarration.None
                narration.speakSequence(LearningJourneyDialogues.workspaceIntro())
            }
            PendingNarration.None -> when (state.progress.currentPhase) {
                TrainingPhase.Welcome -> {
                    dispatch(TrainingEvent.WelcomeNarrationComplete)
                    GuidedLearningMemoryAdapter.onMetLisa(memory)
                }
                TrainingPhase.Setup -> Unit
                TrainingPhase.Calibration -> {
                    if (state.calibrationDotIndex == 0) {
                        mainThreadDelayed { calibrationDotNarration(0) }
                    }
                }
                TrainingPhase.Completion -> {
                    dispatch(TrainingEvent.CompletionNarrationComplete)
                    onCompleteSetupOnboarding()
                    onTrainingFinished()
                }
                else -> Unit
            }
        }
    }

    fun calibrationNarration() {
        if (!LisaSpeechPolicy.allowsNarration()) return
        narration.speakSequence(FirstFiveMinutesExperience.calibrationIntroDialogues())
    }

    fun calibrationDotNarration(dotIndex: Int) {
        if (!LisaSpeechPolicy.allowsNarration()) return
        val lines = FirstFiveMinutesExperience.calibrationDotDialogues()
        narration.speak(lines.getOrElse(dotIndex.coerceAtMost(lines.lastIndex)) { "Please look at the blue dot." })
    }

    fun startCalibrationIfNeeded() {
        if (state.progress.currentPhase != TrainingPhase.Calibration) return
        if (state.calibrationDotIndex == 0) {
            calibrationSession = calibrationEngine.startSession(5, null, CalibrationSessionSource.GuidedLearning)
        }
    }

    fun advanceCalibrationDot() {
        if (state.progress.currentPhase != TrainingPhase.Calibration) return
        calibrationEngine.recordSuccessfulSample(calibrationSession)
        calibrationEngine.recordPointCompleted(calibrationSession)
        val next = state.calibrationDotIndex + 1
        state = state.copy(calibrationDotIndex = next)
        onPersist(state)
        if (next >= state.calibrationTotalDots) {
            mainThreadDelayed { finalizeCalibration() }
        } else if (LisaSpeechPolicy.allowsNarration()) {
            mainThreadDelayed {
                narration.speakSequence(FirstFiveMinutesExperience.calibrationDotCapturedDialogues())
            }
            mainThreadDelayed { calibrationDotNarration(next) }
        }
    }

    private fun finalizeCalibrationSilent() {
        val result = calibrationEngine.completeSession(calibrationSession)
        lastCalibrationPoor = result.score.category == CalibrationQualityCategory.Poor ||
            result.score.category == CalibrationQualityCategory.Failed
        state = state.copy(calibrationPoorRetry = lastCalibrationPoor)
        onPersist(state)
        if (lastCalibrationPoor && calibrationSession.retries < 2) {
            dispatch(TrainingEvent.CalibrationRetry)
            state = state.copy(calibrationDotIndex = 0, calibrationPoorRetry = true)
            onPersist(state)
            calibrationSession = calibrationEngine.startSession(5, null, CalibrationSessionSource.GuidedLearning)
        } else {
            dispatch(TrainingEvent.CalibrationComplete)
            state = state.copy(calibrationDotIndex = 0, calibrationPoorRetry = false)
            onPersist(state)
        }
    }

    private fun finalizeCalibration() {
        if (!LisaSpeechPolicy.allowsNarration()) {
            finalizeCalibrationSilent()
            return
        }
        val result = calibrationEngine.completeSession(calibrationSession)
        lastCalibrationPoor = result.score.category == CalibrationQualityCategory.Poor ||
            result.score.category == CalibrationQualityCategory.Failed
        state = state.copy(calibrationPoorRetry = lastCalibrationPoor)
        onPersist(state)
        val lines = when (result.score.category) {
            CalibrationQualityCategory.Excellent,
            CalibrationQualityCategory.Good -> FirstFiveMinutesExperience.calibrationExcellentDialogues()
            CalibrationQualityCategory.Acceptable -> FirstFiveMinutesExperience.calibrationAcceptableDialogues()
            else -> FirstFiveMinutesExperience.calibrationPoorDialogues()
        }
        pendingAfterNarration = PendingNarration.AfterCalibrationResult
        narration.speakSequence(lines)
    }

    fun fundamentalsCompleteNarration() {
        if (!LisaSpeechPolicy.allowsNarration()) return
        pendingAfterNarration = PendingNarration.AfterFundamentals
        narration.speakSequence(LearningJourneyDialogues.fundamentalsComplete())
    }

    fun masteryIntroNarration() {
        if (!LisaSpeechPolicy.allowsNarration()) return
        narration.speakSequence(LearningJourneyDialogues.masteryIntro())
    }

    fun masteryCompleteNarration() {
        if (!LisaSpeechPolicy.allowsNarration()) return
        pendingAfterNarration = PendingNarration.AfterMastery
        narration.speakSequence(LearningJourneyDialogues.masteryComplete())
    }

    fun isReturningUser(): Boolean = !memory.getGreetingContext().isFirstLaunch

    fun completionNarration() {
        if (!LisaSpeechPolicy.allowsNarration()) {
            dispatch(TrainingEvent.CompletionNarrationComplete)
            onCompleteSetupOnboarding()
            onTrainingFinished()
            return
        }
        pendingAfterNarration = PendingNarration.None
        val transition = com.idworx.lisa.features.experiencepolish.guidedlearningsimplification.GuidedLearningSimplificationExperience
            .workspaceTransitionDialogues()
        narration.speakSequence(
            if (transition.isNotEmpty()) transition else LearningJourneyDialogues.certification()
        )
    }

    fun lessonNarration(phrase: String, instruction: String) {
        if (!LisaSpeechPolicy.allowsNarration()) return
        val dialogue = personality.generateInstruction(
            TrainingDialogueContext.from(
                state,
                TrainingDialogueContext.DialogueContextExtras(targetPhrase = phrase)
            )
        )
        narration.speakDialogue(dialogue)
    }

    fun coachBeginLesson(phrase: String, instruction: String) {
        if (!LisaSpeechPolicy.allowsNarration()) return
        refreshCoachUiState()
        val progress = state.progress
        val lesson = CommunicationCoachEngine.currentCommunicationLesson(progress) ?: run {
            lessonNarration(phrase, instruction)
            return
        }
        val previous = CommunicationCoachEngine.previousCommunicationLesson(progress)
        val prefixLines = buildList {
            if (progress.sessionLessonsThisVisit > 0 &&
                progress.sessionLessonsThisVisit % CommunicationCoachEngine.SESSION_BREAK_THRESHOLD == 0
            ) {
                addAll(fatigueCheckInLines())
            }
            if (CommunicationCoachEngine.gesturePatternBridgeNeeded(previous, lesson)) {
                addAll(PatientCommunicationCoachExperience.difficultyBridgeDialogues())
            } else if (previous != null && lesson.difficultyLevel > previous.difficultyLevel) {
                addAll(PatientCommunicationCoachExperience.difficultyBridgeDialogues())
            }
            addAll(PatientCommunicationCoachExperience.phraseIntroDialogues())
        }
        if (prefixLines.isNotEmpty()) {
            pendingCoachPhrase = phrase
            pendingCoachInstruction = instruction
            pendingAfterNarration = PendingNarration.AfterCoachIntro
            narration.speakSequence(prefixLines)
        } else {
            lessonNarration(phrase, instruction)
        }
    }

    private fun refreshCoachUiState() {
        val offer = navigator.adaptiveOffer(state.progress)
        val lesson = CommunicationCoachEngine.currentCommunicationLesson(state.progress)
        val snapshot = CommunicationCoachEngine.caregiverSnapshot(
            state.progress,
            lesson,
            lesson?.vocabularyId
        )
        state = state.copy(
            adaptiveOffer = offer,
            caregiverSnapshot = snapshot,
            coachUiState = PatientCommunicationCoachEngine.buildCoachUiState(
                state.progress,
                offer,
                snapshot,
                state.coachDecision
            )
        )
        onPersist(state)
    }

    private fun scheduleCoachPacing(decision: CoachLessonDecision) {
        if (decision.pacingDelayMs <= 0L) return
        state = state.copy(coachPacingBlocked = true)
        onPersist(state)
        mainThreadDelayed(decision.pacingDelayMs) {
            state = state.copy(coachPacingBlocked = false)
            onPersist(state)
        }
    }

    private fun speakCoachCelebration(tier: CoachCelebrationTier) {
        val lines = when (tier) {
            CoachCelebrationTier.Major,
            CoachCelebrationTier.Milestone ->
                PatientCommunicationCoachExperience.levelCelebrationDialogues()
            CoachCelebrationTier.Standard ->
                PatientCommunicationCoachExperience.minorCelebrationDialogues()
            CoachCelebrationTier.Quiet -> return
        }
        mainThreadDelayed(400) { narration.speakSequence(lines) }
        if (tier == CoachCelebrationTier.Major || tier == CoachCelebrationTier.Milestone) {
            speakEmotionalMilestone()
        }
    }

    private fun speakEmotionalMilestone() {
        val ctx = presenceDialogueContext(
            TrainingDialogueContext.DialogueContextExtras(celebrationTier = 2)
        )
        if (!EmotionalPresenceEngine.shouldSpeak(PresenceMoment.EmotionalMilestone, ctx, presenceTracker)) return
        val lines = EmotionalPresenceEngine.dialogueTexts(ctx, PresenceMoment.EmotionalMilestone)
        if (lines.isEmpty()) return
        mainThreadDelayed(800) { narration.speakSequence(lines) }
        presenceTracker = EmotionalPresenceEngine.recordSpoken(presenceTracker, PresenceMoment.EmotionalMilestone)
    }

    private fun speakFatigueCheckIn() {
        narration.speakSequence(fatigueCheckInLines())
    }

    private fun fatigueCheckInLines(): List<String> {
        val ctx = presenceDialogueContext(
            TrainingDialogueContext.DialogueContextExtras(fatigueSuggested = true)
        )
        if (!EmotionalPresenceEngine.shouldSpeak(PresenceMoment.FatigueCheckIn, ctx, presenceTracker)) {
            return PatientCommunicationCoachExperience.restSuggestionDialogues()
        }
        val lines = EmotionalPresenceEngine.dialogueTexts(ctx, PresenceMoment.FatigueCheckIn)
        if (lines.isEmpty()) {
            return PatientCommunicationCoachExperience.restSuggestionDialogues()
        }
        presenceTracker = EmotionalPresenceEngine.recordSpoken(presenceTracker, PresenceMoment.FatigueCheckIn)
        return lines
    }

    private fun presenceDialogueContext(
        extras: TrainingDialogueContext.DialogueContextExtras = TrainingDialogueContext.DialogueContextExtras()
    ): DialogueContext {
        val base = TrainingDialogueContext.from(state, extras)
        return PersonalityMemoryAdapter.enrichDialogueContext(base, memory.getGreetingContext())
    }

    private fun speakCoachPacingAction(decision: CoachLessonDecision) {
        when (decision.pacingAction) {
            CoachPacingAction.RepeatSamePhrase ->
                narration.speakSequence(PatientCommunicationCoachExperience.repeatPhraseDialogues())
            CoachPacingAction.SlowDown ->
                narration.speakSequence(PatientCommunicationCoachExperience.slowDownDialogues())
            CoachPacingAction.SuggestBreak -> speakFatigueCheckIn()
            CoachPacingAction.RestBeforeNext,
            CoachPacingAction.Continue -> Unit
        }
    }

    fun navigationNarration(title: String, instruction: String) {
        if (!LisaSpeechPolicy.allowsNarration()) return
        val dialogue = personality.generateNavigationGuidance(
            TrainingDialogueContext.from(
                state,
                TrainingDialogueContext.DialogueContextExtras(
                    navigationAction = title,
                    emergencyTraining = title.contains("Emergency", ignoreCase = true)
                )
            )
        )
        narration.speakDialogue(dialogue)
    }

    fun advanceSetupStep(delta: Int) {
        setSetupStep((setupStep + delta).coerceAtLeast(0))
    }

    fun setSetupStep(step: Int) {
        setupStep = step.coerceAtLeast(0)
        state = state.copy(setupStep = setupStep)
        onPersist(state)
    }

    fun updateWinkDots(left: Int, right: Int) {
        state = state.copy(leftWinkDots = left, rightWinkDots = right)
        onPersist(state)
    }

    fun registerTouchWink(isLeft: Boolean, finalizeSequence: () -> Unit, incrementLeft: () -> Unit, incrementRight: () -> Unit) {
        if (isLeft) incrementLeft() else incrementRight()
        finalizeSequence()
    }

    fun handleSequence(left: Int, right: Int, language: PreferredLanguage, blinkOrder: List<Boolean> = emptyList()) {
        val phase = state.progress.currentPhase
        if (phase != TrainingPhase.CommunicationLesson && phase != TrainingPhase.CommunicationMastery) return
        val beforePhase = phase
        val lesson = when (phase) {
            TrainingPhase.CommunicationLesson ->
                TrainingLessonCatalog.communicationLessonAt(state.progress.communicationLessonIndex)
            TrainingPhase.CommunicationMastery ->
                TrainingLessonCatalog.masteryLessonAt(
                    state.progress.masteryRoundIndex,
                    state.progress.masteryPhraseOrder
                )
            else -> null
        } ?: return

        if (LisaSpeechPolicy.PHRASE_TRANSLATION_ONLY) {
            handleInteractiveSequence(left, right, blinkOrder, language, lesson, beforePhase)
            return
        }

        when {
            TrainingLessonCatalog.lessonMatchesGesture(lesson, left, right, blinkOrder) -> {
                val phrase = LisaCoreVocabulary.text(lesson.vocabularyId, language)
                val wasFirstSuccess = state.progress.statistics.successfulAttempts == 0
                val beforeLessonIndex = state.progress.communicationLessonIndex
                val beforeMasteryIndex = state.progress.masteryRoundIndex
                val previous = CommunicationCoachEngine.previousCommunicationLesson(state.progress)
                speakPhrase(phrase)
                dispatch(TrainingEvent.SequenceSuccess)
                val advanced = state.progress.communicationLessonIndex != beforeLessonIndex ||
                    state.progress.masteryRoundIndex != beforeMasteryIndex ||
                    state.progress.currentPhase != beforePhase
                val interimForCoach = if (advanced) {
                    state.progress.copy(
                        currentLessonSuccessCount = CommunicationCoachEngine.successesNeededToAdvance(beforePhase)
                    )
                } else {
                    state.progress
                }
                val coachDecision = CommunicationCoachEngine.evaluateSuccess(
                    interimForCoach,
                    lesson,
                    previous,
                    phrase
                )
                if (LisaSpeechPolicy.allowsNarration()) {
                    speakCoachCelebration(coachDecision.celebrationTier)
                    if (!coachDecision.shouldRepeatPhrase) {
                        speakCoachPacingAction(coachDecision)
                    }
                    scheduleCoachPacing(coachDecision)
                }
                if (wasFirstSuccess) {
                    GuidedLearningMemoryAdapter.onFirstSuccessfulBlink(memory)
                    GuidedLearningMemoryAdapter.onFirstSpokenPhrase(memory, lesson.id)
                }
                if (coachDecision.shouldAdvance) {
                    GuidedLearningMemoryAdapter.onLessonCompleted(
                        memory,
                        lesson.id,
                        if (phase == TrainingPhase.CommunicationMastery) "Mastery" else "Fundamentals",
                        state.progress.statistics.totalAttempts
                    )
                    GuidedLearningMemoryAdapter.checkPhaseCompletions(memory, state.progress.completedLessonIds)
                }
                val celebrationTier = PatientCommunicationCoachEngine.celebrationTier(coachDecision)
                val ctx = PersonalityMemoryAdapter.enrichDialogueContext(
                    TrainingDialogueContext.from(
                        state,
                        TrainingDialogueContext.DialogueContextExtras(
                            lessonId = lesson.id,
                            targetPhrase = phrase,
                            firstSpokenPhrase = wasFirstSuccess,
                            milestoneType = com.idworx.lisa.features.personality.model.MilestoneType.FirstSpokenPhrase,
                            celebrationTier = celebrationTier
                        )
                    ),
                    memory.getGreetingContext()
                )
                state = state.copy(
                    feedback = TrainingFeedback.Success,
                    feedbackMessage = coachDecision.learnerMessage ?: personality.generateCelebration(ctx).text,
                    showCelebration = coachDecision.showCelebrationOverlay,
                    coachDecision = coachDecision,
                    coachInstruction = coachDecision.learnerMessage
                )
                refreshCoachUiState()
                onPersist(state)
                mainThreadDelayed {
                    state = state.copy(showCelebration = false, feedback = TrainingFeedback.None, feedbackMessage = null)
                    onPersist(state)
                    if (LisaSpeechPolicy.allowsNarration()) {
                        when {
                            beforePhase == TrainingPhase.CommunicationLesson &&
                                state.progress.currentPhase == TrainingPhase.CommunicationMastery ->
                                fundamentalsCompleteNarration()
                            beforePhase == TrainingPhase.CommunicationMastery &&
                                state.progress.currentPhase == TrainingPhase.NavigationLesson ->
                                masteryCompleteNarration()
                        }
                    }
                }
            }
            abs(left - lesson.left) + abs(right - lesson.right) <= 2 -> {
                dispatch(TrainingEvent.SequenceAlmost)
            }
            else -> {
                gentleMissedBlinkNarration()
                val nextFailures = state.progress.statistics.consecutiveFailures + 1
                dispatch(TrainingEvent.SequenceRetry)
                val retryDecision = CommunicationCoachEngine.evaluateRetry(state.progress)
                if (PatientCommunicationCoachEngine.REPEAT_THRESHOLD <= nextFailures &&
                    nextFailures < PatientCommunicationCoachEngine.SLOW_DOWN_THRESHOLD
                ) {
                    coachRepeatNarration()
                }
                if (nextFailures >= PatientCommunicationCoachEngine.SLOW_DOWN_THRESHOLD) {
                    coachSlowDownNarration()
                    patienceNarration()
                    val offer = navigator.adaptiveOffer(state.progress)
                    if (offer.showRecalibrate) {
                        mainThreadDelayed {
                            beginAwaitingBrain1Decision(Brain1DecisionKind.Recalibration)
                        }
                    }
                }
                speakCoachPacingAction(retryDecision)
                refreshCoachUiState()
            }
        }
    }

    private fun handleInteractiveSequence(
        left: Int,
        right: Int,
        blinkOrder: List<Boolean>,
        language: PreferredLanguage,
        lesson: CommunicationLesson,
        phase: TrainingPhase
    ) {
        when {
            LessonInteractionEngine.lessonMatchesGesture(lesson, left, right, blinkOrder) -> {
                val phrase = LisaCoreVocabulary.text(lesson.vocabularyId, language)
                beginInteractiveLessonSuccess(phrase, lesson, phase)
            }
            abs(left - lesson.left) + abs(right - lesson.right) <= 2 -> Unit
            else -> applyInteractiveRetryVisual(state.progress.statistics.consecutiveFailures)
        }
    }

    /** The one real workspace action the current navigation lesson is teaching, or null. */
    fun expectedNavigationAction(): NavigationAction? =
        navigator.expectedNavigationAction(state.progress)

    fun verifyNavigation(action: NavigationAction) {
        if (state.progress.currentPhase != TrainingPhase.NavigationLesson) return
        // While the final lesson's completion feedback is being shown/spoken, the Completion
        // transition is deliberately on hold — ignore any further gestures until it fires.
        if (state.completionPendingFeedback) return
        val expected = navigator.expectedNavigationAction(state.progress) ?: return
        if (expected != action) return
        val lessonId = navigator.navigationActionId(state.progress) ?: return
        val completedLessonIndex = state.progress.navigationLessonIndex
        val isFinalNavigationLesson =
            TrainingLessonCatalog.navigationLessonAt(completedLessonIndex + 1) == null

        if (isFinalNavigationLesson) {
            beginFinalNavigationCompletionFeedback(completedLessonIndex, lessonId)
        } else {
            dispatch(TrainingEvent.NavigationActionCompleted(lessonId))
            applyNavigationCompletionFeedback(completedLessonIndex)
            mainThreadDelayed {
                if (state.progress.currentPhase == TrainingPhase.Completion) {
                    completionNarration()
                }
            }
        }
    }

    /**
     * Immediate, friendly acknowledgement ("Well done.", "Great job.", "You did it.") after a
     * correct real-workspace navigation gesture — spoken through the existing narration/TTS
     * system and shown briefly on the floating lesson card before the next instruction appears.
     * Reusable for every navigation lesson; never tied to a specific lesson or screen.
     */
    private fun applyNavigationCompletionFeedback(completedLessonIndex: Int) {
        val phrase = GuidedFeedbackPhrases.positive(completedLessonIndex)
        state = state.copy(navigationFeedbackMessage = phrase)
        onPersist(state)
        if (LisaSpeechPolicy.allowsNarration()) {
            narration.speak(phrase)
        }
        mainThreadDelayed(NAVIGATION_FEEDBACK_VISIBLE_MS) {
            if (state.navigationFeedbackMessage == phrase) {
                state = state.copy(navigationFeedbackMessage = null)
                onPersist(state)
            }
        }
    }

    /**
     * Reusable "post-success transition delay" for the FINAL navigation lesson only: the real
     * workspace and floating lesson card stay exactly as they are — [TrainingProgress] is not
     * advanced yet — while the positive acknowledgement is shown and spoken, so the user always
     * sees/hears it before the Completion screen takes over. General for any lesson catalog
     * length: driven by [TrainingLessonCatalog.navigationLessonAt], never a hardcoded lesson.
     */
    private fun beginFinalNavigationCompletionFeedback(completedLessonIndex: Int, lessonId: String) {
        val phrase = GuidedFeedbackPhrases.positive(completedLessonIndex)
        state = state.copy(
            navigationFeedbackMessage = phrase,
            completionPendingFeedback = true
        )
        onPersist(state)
        if (LisaSpeechPolicy.allowsNarration()) {
            narration.speak(phrase)
        }
        mainThreadDelayed(FINAL_NAVIGATION_COMPLETION_DELAY_MS) {
            dispatch(TrainingEvent.NavigationActionCompleted(lessonId))
            state = state.copy(
                navigationFeedbackMessage = null,
                completionPendingFeedback = false
            )
            onPersist(state)
            if (state.progress.currentPhase == TrainingPhase.Completion) {
                completionNarration()
            }
        }
    }

    /**
     * Reusable red "wrong sequence" acknowledgement for real-workspace navigation lessons — shown
     * on the floating lesson card whenever the caller (MainActivity) blocks an unrelated
     * gesture/action or an off-target row while a lesson is focused on one specific control. Only
     * ever surfaces while a navigation lesson is active; never advances or resets lesson progress
     * itself — the caller is responsible for resetting the active blink sequence separately.
     * Reusable for every navigation lesson; never tied to a specific lesson or screen.
     */
    fun applyNavigationWrongGestureFeedback() {
        if (state.progress.currentPhase != TrainingPhase.NavigationLesson) return
        val message = GuidedFeedbackPhrases.wrongGesture()
        state = state.copy(navigationWrongGestureMessage = message)
        onPersist(state)
        mainThreadDelayed(NAVIGATION_FEEDBACK_VISIBLE_MS) {
            if (state.navigationWrongGestureMessage == message) {
                state = state.copy(navigationWrongGestureMessage = null)
                onPersist(state)
            }
        }
    }

    fun updatePreferences(transform: (com.idworx.lisa.features.onboardingguide.model.TrainingPreferences) ->
        com.idworx.lisa.features.onboardingguide.model.TrainingPreferences) {
        dispatch(TrainingEvent.UpdatePreferences(transform))
        GuidedLearningMemoryAdapter.syncPreferences(memory, state.progress.preferences)
        narration.applyPreferences()
    }

    private fun recordMemoryForEvent(event: TrainingEvent, @Suppress("UNUSED_PARAMETER") before: GuidedTrainingUiState) {
        when (event) {
            TrainingEvent.BeginLearning -> GuidedLearningMemoryAdapter.onTutorialStarted(memory)
            TrainingEvent.WelcomeNarrationComplete -> GuidedLearningMemoryAdapter.onTutorialStarted(memory)
            TrainingEvent.ConfirmSkip -> GuidedLearningMemoryAdapter.onTutorialSkipped(memory)
            TrainingEvent.ReturnToTutorial -> GuidedLearningMemoryAdapter.onTutorialResumed(memory)
            TrainingEvent.ReplayTutorial, TrainingEvent.ResetProgress -> GuidedLearningMemoryAdapter.onTutorialReset(memory)
            TrainingEvent.CompleteTraining, TrainingEvent.StartUsingLisa,
            TrainingEvent.CompletionNarrationComplete -> GuidedLearningMemoryAdapter.onCertification(memory)
            TrainingEvent.CalibrationComplete -> GuidedLearningMemoryAdapter.onCalibrationComplete(memory)
            TrainingEvent.FundamentalsComplete -> GuidedLearningMemoryAdapter.onFundamentalsComplete(memory)
            TrainingEvent.MasteryComplete -> GuidedLearningMemoryAdapter.onMasteryComplete(memory)
            is TrainingEvent.NavigationActionCompleted -> {
                GuidedLearningMemoryAdapter.onLessonCompleted(
                    memory,
                    event.actionId,
                    "Navigation",
                    state.progress.statistics.totalAttempts
                )
                if (event.actionId.contains("emergency", ignoreCase = true)) {
                    GuidedLearningMemoryAdapter.onEmergencyTrainingCompleted(memory)
                }
                GuidedLearningMemoryAdapter.checkPhaseCompletions(memory, state.progress.completedLessonIds)
            }
            else -> Unit
        }
    }

    private var delayedHandler: ((Long, () -> Unit) -> Unit)? = null

    fun attachDelayedHandler(handler: (delayMs: Long, block: () -> Unit) -> Unit) {
        delayedHandler = handler
    }

    private fun mainThreadDelayed(delayMs: Long = 1_800L, block: () -> Unit) {
        delayedHandler?.invoke(delayMs, block) ?: block()
    }

    companion object {
        const val SETUP_STEP_EYE_DETECTION: Int = 0
        const val SETUP_STEP_READY: Int = 1
        /** Brief pause showing "Well done" after phrase speech, before advancing to the next lesson. */
        const val SUCCESS_VISUAL_PAUSE_MS: Long = 900L
        private const val RETRY_FEEDBACK_CLEAR_MS: Long = 2_500L
        private const val WRONG_EYE_FEEDBACK_CLEAR_MS: Long = 2_500L
        const val NAVIGATION_FEEDBACK_VISIBLE_MS: Long = 1_600L
        /** How long the final navigation lesson's success feedback is held before Completion. */
        const val FINAL_NAVIGATION_COMPLETION_DELAY_MS: Long = 1_800L

        val ACTIVE_TRAINING_PHASES: Set<TrainingPhase> = setOf(
            TrainingPhase.FirstLaunchChoice,
            TrainingPhase.Welcome,
            TrainingPhase.Setup,
            TrainingPhase.Calibration,
            TrainingPhase.CommunicationLesson,
            TrainingPhase.CommunicationMastery,
            TrainingPhase.NavigationLesson,
            TrainingPhase.SkipConfirm
        )
    }
}
