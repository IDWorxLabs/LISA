package com.idworx.lisa.features.corecommunicationreliability.engine

import com.idworx.lisa.PreferredLanguage
import com.idworx.lisa.WinkMapping
import com.idworx.lisa.features.companionmemory.engine.CompanionMemoryEngine
import com.idworx.lisa.features.companionmemory.integration.GuidedLearningMemoryAdapter
import com.idworx.lisa.features.companionmemory.model.LearningMilestone
import com.idworx.lisa.features.corecommunicationreliability.diagnostics.CommunicationDiagnostics
import com.idworx.lisa.features.corecommunicationreliability.emergency.EmergencySpeechSafetyGuard
import com.idworx.lisa.features.corecommunicationreliability.history.CommunicationReliabilityHistoryRecorder
import com.idworx.lisa.features.corecommunicationreliability.model.CommunicationAttemptResult
import com.idworx.lisa.features.corecommunicationreliability.model.CommunicationMode
import com.idworx.lisa.features.corecommunicationreliability.model.CommunicationReliabilityOutcome
import com.idworx.lisa.features.corecommunicationreliability.model.CommunicationReliabilityReport
import com.idworx.lisa.features.corecommunicationreliability.model.PhraseReliabilityAction
import com.idworx.lisa.features.corecommunicationreliability.model.ReliabilityMetric
import com.idworx.lisa.features.corecommunicationreliability.model.SpeechOutputResult
import com.idworx.lisa.features.corecommunicationreliability.phrase.PhraseConfirmationPolicy
import com.idworx.lisa.features.corecommunicationreliability.phrase.PhraseMatchVerifier
import com.idworx.lisa.features.corecommunicationreliability.phrase.PhraseSelectionGuard
import com.idworx.lisa.features.corecommunicationreliability.sequence.BlinkSequenceConfidenceScorer
import com.idworx.lisa.features.corecommunicationreliability.sequence.BlinkSequenceDebouncer
import com.idworx.lisa.features.corecommunicationreliability.sequence.BlinkSequenceValidator
import com.idworx.lisa.features.corecommunicationreliability.speech.SpeechReliabilityAdapter
import com.idworx.lisa.features.personality.engine.LisaPersonalityEngine
import com.idworx.lisa.features.personality.model.DialogueContext
import com.idworx.lisa.features.calibrationreliability.integration.CalibrationCommunicationReliabilityBridge
import com.idworx.lisa.features.calibrationreliability.model.CalibrationHealthState
import com.idworx.lisa.isEmergencySequence
import java.util.UUID

data class CommunicationReliabilityContext(
    val mode: CommunicationMode = CommunicationMode.MAIN,
    val mappings: List<WinkMapping>,
    val language: PreferredLanguage = PreferredLanguage.English,
    val listeningPaused: Boolean = false,
    val navigationTrainingActive: Boolean = false,
    val communicationTrainingActive: Boolean = false,
    val practiceMode: Boolean = false,
    val ttsAvailable: Boolean = true,
    val debounceWindowMs: Long = com.idworx.lisa.features.corecommunicationreliability.metadata.CoreCommunicationReliabilityMetadata.DEFAULT_DEBOUNCE_MS,
    val calibrationHealthState: CalibrationHealthState = CalibrationHealthState.Healthy,
    val calibrationAllowsCommunication: Boolean = true
)

interface CoreCommunicationReliabilityEngine {
    fun evaluatePhrasePath(context: CommunicationReliabilityContext, left: Int, right: Int): CommunicationReliabilityReport
    fun evaluateEmergency(context: CommunicationReliabilityContext, left: Int, right: Int): CommunicationReliabilityReport
    fun recordSpeechDelivery(
        attemptId: String,
        phraseText: String,
        phraseId: String?,
        sequenceLeft: Int,
        sequenceRight: Int,
        mode: CommunicationMode,
        emergency: Boolean,
        success: Boolean,
        failureReason: String? = null
    ): SpeechOutputResult
    fun shouldBlockFinalizationForContinuation(left: Int, right: Int, mappings: List<WinkMapping>): Boolean
    fun feedbackForBlockedAttempt(report: CommunicationReliabilityReport): String
    fun lastReport(): CommunicationReliabilityReport?
    fun speechAdapter(): SpeechReliabilityAdapter
}

class DefaultCoreCommunicationReliabilityEngine(
    private val debouncer: BlinkSequenceDebouncer = BlinkSequenceDebouncer(),
    private val speechAdapter: SpeechReliabilityAdapter = SpeechReliabilityAdapter(),
    private val personality: LisaPersonalityEngine = com.idworx.lisa.features.personality.engine.LisaPersonalityEngines.default,
    private val companionMemory: CompanionMemoryEngine = com.idworx.lisa.features.companionmemory.engine.CompanionMemoryEngines.default
) : CoreCommunicationReliabilityEngine {

    private var lastReportInternal: CommunicationReliabilityReport? = null
    private var firstMainCommunicationRecorded = false

    override fun evaluatePhrasePath(
        context: CommunicationReliabilityContext,
        left: Int,
        right: Int
    ): CommunicationReliabilityReport {
        val attemptId = UUID.randomUUID().toString()
        if (!context.calibrationAllowsCommunication) {
            return buildCalibrationBlockedReport(
                attemptId, left, right, context
            )
        }
        val validation = BlinkSequenceValidator.validate(
            left, right, context.mappings, context.mode, context.listeningPaused
        )
        val duplicate = !debouncer.shouldAllow(left, right)
        val match = PhraseMatchVerifier.verify(left, right, context.mappings, context.language)
        val confidence = BlinkSequenceConfidenceScorer.score(
            validation, duplicate, match.mapping != null
        )
        val emergencyPolicy = EmergencySpeechSafetyGuard.evaluate(
            left, right,
            context.navigationTrainingActive,
            context.communicationTrainingActive,
            context.practiceMode
        )
        val selection = PhraseSelectionGuard.evaluate(
            validation, match, confidence, context.mode,
            context.listeningPaused, duplicate, emergencyPolicy.requiresConfirmation,
            calibrationAllowsCommunication = context.calibrationAllowsCommunication
        )
        val confirmation = PhraseConfirmationPolicy.decide(
            confidence, match.isEmergency,
            context.communicationTrainingActive || context.navigationTrainingActive
        )

        val attemptResult = buildAttemptResult(
            selection, match, confidence, confirmation, validation
        )
        val outcome = resolveOutcome(attemptResult, selection.allowed, match.mapping != null)

        val report = CommunicationReliabilityReport(
            attemptId = attemptId,
            timestampMs = System.currentTimeMillis(),
            rawSequenceSummary = "L$left R$right",
            normalizedSequence = validation.normalizedSequence,
            validationResult = validation,
            confidenceScore = confidence,
            matchedPhraseId = match.phraseId,
            matchedPhraseText = match.phraseText,
            confirmationRequired = confirmation.requiresConfirmation,
            attemptResult = attemptResult,
            emergencySafetyResult = if (isEmergencySequence(left, right)) emergencyPolicy else null,
            finalOutcome = outcome,
            failureReason = attemptResult.blockedReason,
            warnings = attemptResult.warnings,
            metrics = listOf(
                ReliabilityMetric("confidence", confidence),
                ReliabilityMetric("match_count", match.matchCount.toFloat())
            )
        )
        lastReportInternal = report
        CommunicationDiagnostics.recordAttempt(report)
        return report
    }

    override fun evaluateEmergency(
        context: CommunicationReliabilityContext,
        left: Int,
        right: Int
    ): CommunicationReliabilityReport {
        val safety = EmergencySpeechSafetyGuard.evaluate(
            left, right,
            context.navigationTrainingActive,
            context.communicationTrainingActive,
            context.practiceMode
        )
        CommunicationDiagnostics.recordEmergencyStatus(
            when {
                safety.navigationTraining -> "training_verification"
                !safety.allowed -> "blocked: ${safety.blockedReason}"
                else -> "allowed"
            }
        )
        val action = when {
            safety.navigationTraining -> PhraseReliabilityAction.ROUTE_EMERGENCY_TRAINING
            safety.allowed -> PhraseReliabilityAction.ROUTE_EMERGENCY
            else -> PhraseReliabilityAction.BLOCK
        }
        val outcome = when {
            safety.navigationTraining -> CommunicationReliabilityOutcome.PASS
            !safety.allowed -> CommunicationReliabilityOutcome.BLOCKED
            else -> CommunicationReliabilityOutcome.WARN
        }
        val report = CommunicationReliabilityReport(
            attemptId = UUID.randomUUID().toString(),
            timestampMs = System.currentTimeMillis(),
            rawSequenceSummary = "L$left R$right",
            normalizedSequence = "L$left R$right",
            validationResult = BlinkSequenceValidator.validate(
                left, right, context.mappings, CommunicationMode.EMERGENCY
            ),
            confidenceScore = if (safety.allowed) 1f else 0f,
            matchedPhraseId = "emergency",
            matchedPhraseText = null,
            confirmationRequired = true,
            attemptResult = CommunicationAttemptResult(
                action = action,
                outcome = outcome,
                phraseId = "emergency",
                blockedReason = safety.blockedReason
            ),
            emergencySafetyResult = safety,
            finalOutcome = outcome,
            failureReason = safety.blockedReason,
            metrics = listOf(ReliabilityMetric("emergency_allowed", if (safety.allowed) 1f else 0f))
        )
        lastReportInternal = report
        CommunicationDiagnostics.recordAttempt(report)
        return report
    }

    override fun recordSpeechDelivery(
        attemptId: String,
        phraseText: String,
        phraseId: String?,
        sequenceLeft: Int,
        sequenceRight: Int,
        mode: CommunicationMode,
        emergency: Boolean,
        success: Boolean,
        failureReason: String?
    ): SpeechOutputResult {
        val result = speechAdapter.onSpeechRequested(
            phraseText, phraseId, ttsAvailable = true, blocked = false
        ).let { SpeechOutputResult(
            requested = it.requested,
            phraseText = phraseText,
            phraseId = phraseId,
            success = success,
            failureReason = failureReason,
            timestampMs = System.currentTimeMillis()
        ) }
        speechAdapter.onSpeechFinished(success, failureReason)
        CommunicationReliabilityHistoryRecorder.record(
            phraseId, phraseText, sequenceLeft, sequenceRight, mode, emergency, success
        )
        if (success && mode == CommunicationMode.MAIN && !emergency && !firstMainCommunicationRecorded) {
            firstMainCommunicationRecorded = true
            if (LearningMilestone.FirstPhrase !in companionMemory.getMilestones()) {
                GuidedLearningMemoryAdapter.onFirstSpokenPhrase(companionMemory, phraseId ?: "main_communication")
            }
        }
        return result
    }

    override fun shouldBlockFinalizationForContinuation(
        left: Int,
        right: Int,
        mappings: List<WinkMapping>
    ): Boolean = com.idworx.lisa.hasLongerContinuation(left, right, mappings)

    override fun feedbackForBlockedAttempt(report: CommunicationReliabilityReport): String {
        val ctx = DialogueContext(
            consecutiveFailures = 1,
            failureCount = 1
        )
        return personality.generateComfort(ctx).text
    }

    override fun lastReport(): CommunicationReliabilityReport? = lastReportInternal

    override fun speechAdapter(): SpeechReliabilityAdapter = speechAdapter

    private fun buildCalibrationBlockedReport(
        attemptId: String,
        left: Int,
        right: Int,
        context: CommunicationReliabilityContext
    ): CommunicationReliabilityReport {
        val blockedReason = CalibrationCommunicationReliabilityBridge.calibrationBlockReason(
            context.calibrationHealthState
        ) ?: "Calibration health prevents communication"
        val report = CommunicationReliabilityReport(
            attemptId = attemptId,
            timestampMs = System.currentTimeMillis(),
            rawSequenceSummary = "L$left R$right",
            normalizedSequence = "L$left R$right",
            validationResult = BlinkSequenceValidator.validate(
                left, right, context.mappings, context.mode, context.listeningPaused
            ),
            confidenceScore = 0f,
            matchedPhraseId = null,
            matchedPhraseText = null,
            confirmationRequired = false,
            attemptResult = CommunicationAttemptResult(
                action = PhraseReliabilityAction.BLOCK,
                outcome = CommunicationReliabilityOutcome.BLOCKED,
                blockedReason = blockedReason
            ),
            finalOutcome = CommunicationReliabilityOutcome.BLOCKED,
            failureReason = blockedReason,
            metrics = listOf(
                ReliabilityMetric("calibration_health", context.calibrationHealthState.ordinal.toFloat())
            )
        )
        lastReportInternal = report
        CommunicationDiagnostics.recordAttempt(report)
        return report
    }

    private fun buildAttemptResult(
        selection: com.idworx.lisa.features.corecommunicationreliability.phrase.PhraseSelectionDecision,
        match: com.idworx.lisa.features.corecommunicationreliability.phrase.PhraseMatchResult,
        confidence: Float,
        confirmation: com.idworx.lisa.features.corecommunicationreliability.phrase.ConfirmationDecision,
        validation: com.idworx.lisa.features.corecommunicationreliability.model.SequenceRecognitionResult
    ): CommunicationAttemptResult {
        if (!selection.allowed) {
            return CommunicationAttemptResult(
                action = PhraseReliabilityAction.BLOCK,
                outcome = CommunicationReliabilityOutcome.BLOCKED,
                confidenceScore = confidence,
                blockedReason = selection.blockedReason,
                warnings = listOfNotNull(validation.blockedReason)
            )
        }
        if (match.mapping == null && !match.isEmergency) {
            return CommunicationAttemptResult(
                action = PhraseReliabilityAction.NO_PHRASE,
                outcome = CommunicationReliabilityOutcome.BLOCKED,
                blockedReason = "No phrase matched"
            )
        }
        val action = if (confirmation.allowImmediate) {
            PhraseReliabilityAction.PROCEED_IMMEDIATE
        } else {
            PhraseReliabilityAction.PROCEED_TO_CONFIRMATION
        }
        return CommunicationAttemptResult(
            action = action,
            outcome = if (confidence < 0.65f) CommunicationReliabilityOutcome.WARN else CommunicationReliabilityOutcome.PASS,
            phraseId = match.phraseId,
            phraseText = match.phraseText,
            confidenceScore = confidence,
            requiresConfirmation = confirmation.requiresConfirmation,
            warnings = listOfNotNull(confirmation.reason.takeIf { confidence < 0.85f })
        )
    }

    private fun resolveOutcome(
        attempt: CommunicationAttemptResult,
        allowed: Boolean,
        hasMatch: Boolean
    ): CommunicationReliabilityOutcome = when {
        !allowed -> CommunicationReliabilityOutcome.BLOCKED
        !hasMatch && attempt.action == PhraseReliabilityAction.NO_PHRASE -> CommunicationReliabilityOutcome.BLOCKED
        attempt.outcome == CommunicationReliabilityOutcome.WARN -> CommunicationReliabilityOutcome.WARN
        else -> CommunicationReliabilityOutcome.PASS
    }
}

object CoreCommunicationReliabilityEngines {
    val default: CoreCommunicationReliabilityEngine by lazy {
        DefaultCoreCommunicationReliabilityEngine()
    }

    fun createForTests(
        personality: LisaPersonalityEngine = com.idworx.lisa.features.personality.engine.LisaPersonalityEngines.default,
        companionMemory: CompanionMemoryEngine = com.idworx.lisa.features.companionmemory.engine.CompanionMemoryEngines.default
    ): DefaultCoreCommunicationReliabilityEngine =
        DefaultCoreCommunicationReliabilityEngine(
            debouncer = BlinkSequenceDebouncer(),
            speechAdapter = SpeechReliabilityAdapter(),
            personality = personality,
            companionMemory = companionMemory
        )
}
