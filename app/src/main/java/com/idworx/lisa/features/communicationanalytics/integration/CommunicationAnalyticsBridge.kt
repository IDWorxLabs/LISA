package com.idworx.lisa.features.communicationanalytics.integration

import com.idworx.lisa.features.calibrationreliability.engine.CalibrationReliabilityEngines
import com.idworx.lisa.features.calibrationreliability.monitoring.CalibrationDiagnostics
import com.idworx.lisa.features.communicationanalytics.engine.CommunicationAnalyticsEngines
import com.idworx.lisa.features.communicationanalytics.model.AttemptTiming
import com.idworx.lisa.features.communicationanalytics.model.CommunicationAttemptAnalytics
import com.idworx.lisa.features.corecommunicationreliability.diagnostics.CommunicationDiagnostics
import com.idworx.lisa.features.corecommunicationreliability.engine.CommunicationReliabilityContext
import com.idworx.lisa.features.corecommunicationreliability.history.CommunicationReliabilityHistoryRecorder
import com.idworx.lisa.features.corecommunicationreliability.model.CommunicationReliabilityReport
import com.idworx.lisa.features.corecommunicationreliability.model.SpokenPhraseRecord
import com.idworx.lisa.features.personality.engine.LisaPersonalityEngine
import com.idworx.lisa.features.personality.model.AppFeature
import com.idworx.lisa.features.personality.model.DialogueContext

/**
 * Read-only bridge: observes CCR and calibration without altering communication decisions.
 */
object CommunicationAnalyticsBridge {

    private var attached = false
    private var sequenceStartMs: Long? = null

    fun attach(engine: com.idworx.lisa.features.communicationanalytics.engine.CommunicationAnalyticsEngine =
        CommunicationAnalyticsEngines.default) {
        if (attached) return
        attached = true
        CommunicationDiagnostics.addAttemptObserver { report, context ->
            engine.recordAttempt(mapAttempt(report, context))
        }
        CommunicationReliabilityHistoryRecorder.addSpeechObserver { record ->
            linkSpeechToLastAttempt(engine, record)
        }
    }

    fun detach() {
        if (!attached) return
        CommunicationDiagnostics.clearAttemptObservers()
        CommunicationReliabilityHistoryRecorder.clearSpeechObservers()
        attached = false
    }

    fun markSequenceStart() {
        sequenceStartMs = System.currentTimeMillis()
    }

    fun markSequenceComplete() {
        // sequence complete timestamp captured at attempt evaluation time
    }

    fun observationContextFrom(context: CommunicationReliabilityContext): CommunicationDiagnostics.AnalyticsObservationContext =
        CommunicationDiagnostics.AnalyticsObservationContext(
            mode = context.mode,
            calibrationHealth = context.calibrationHealthState,
            navigationTraining = context.navigationTrainingActive,
            communicationTraining = context.communicationTrainingActive,
            practiceMode = context.practiceMode,
            sequenceStartMs = sequenceStartMs
        )

    fun setObservationContext(context: CommunicationReliabilityContext) {
        CommunicationDiagnostics.setPendingObservationContext(observationContextFrom(context))
    }

    private fun mapAttempt(
        report: CommunicationReliabilityReport,
        context: CommunicationDiagnostics.AnalyticsObservationContext?
    ): CommunicationAttemptAnalytics {
        val blockedReason = report.failureReason ?: report.attemptResult.blockedReason
        val now = System.currentTimeMillis()
        val calibrationScore = CalibrationDiagnostics.currentScore()
        return CommunicationAttemptAnalytics(
            attemptId = report.attemptId,
            timestampMs = report.timestampMs,
            mode = context?.mode ?: com.idworx.lisa.features.corecommunicationreliability.model.CommunicationMode.MAIN,
            calibrationHealth = context?.calibrationHealth ?: CalibrationReliabilityEngines.default.currentHealth(),
            calibrationScore = calibrationScore,
            phraseId = report.matchedPhraseId,
            phraseText = report.matchedPhraseText,
            confidenceScore = report.confidenceScore,
            action = report.attemptResult.action,
            finalOutcome = report.finalOutcome,
            blockedReason = blockedReason,
            emergency = report.emergencySafetyResult?.isEmergencySequence == true,
            emergencyTraining = report.attemptResult.action ==
                com.idworx.lisa.features.corecommunicationreliability.model.PhraseReliabilityAction.ROUTE_EMERGENCY_TRAINING,
            navigationTraining = context?.navigationTraining == true,
            communicationTraining = context?.communicationTraining == true,
            practiceMode = context?.practiceMode == true,
            duplicateBlocked = blockedReason?.contains("Duplicate", ignoreCase = true) == true,
            calibrationBlocked = blockedReason?.contains("calibration", ignoreCase = true) == true ||
                context?.calibrationHealth?.name?.contains("Invalid") == true,
            timing = AttemptTiming(
                firstBlinkMs = context?.sequenceStartMs,
                sequenceCompleteMs = now,
                phraseMatchMs = now,
                speechRequestMs = null,
                speechCompleteMs = null
            ),
            sequenceLabel = report.normalizedSequence
        )
    }

    private fun linkSpeechToLastAttempt(
        engine: com.idworx.lisa.features.communicationanalytics.engine.CommunicationAnalyticsEngine,
        record: SpokenPhraseRecord
    ) {
        val last = CommunicationDiagnostics.lastAttempt()
        val attemptId = last?.attemptId ?: record.recordId
        engine.recordSpeechDelivery(attemptId, record, record.timestampMs)
    }
}

object AnalyticsPersonalityAdapter {

    fun supportiveMessageForAnalytics(
        personality: LisaPersonalityEngine,
        consecutiveFailures: Int,
        recommendRecalibration: Boolean
    ): String? = when {
        recommendRecalibration ->
            personality.generateEncouragement(
                DialogueContext(feature = AppFeature.Settings, consecutiveFailures = consecutiveFailures)
            ).text
        consecutiveFailures >= 5 ->
            "Let's practice together."
        consecutiveFailures >= 3 ->
            "We can slow things down."
        else -> null
    }
}
