package com.idworx.lisa.validation.authority

import com.idworx.lisa.defaultLanguageMappings
import com.idworx.lisa.features.companionmemory.engine.CompanionMemoryEngines
import com.idworx.lisa.features.companionmemory.model.LearningMilestone
import com.idworx.lisa.features.companionmemory.repository.InMemoryCompanionMemoryRepository
import com.idworx.lisa.features.corecommunicationreliability.diagnostics.CommunicationDiagnostics
import com.idworx.lisa.features.corecommunicationreliability.emergency.EmergencySpeechSafetyGuard
import com.idworx.lisa.features.corecommunicationreliability.engine.CommunicationReliabilityContext
import com.idworx.lisa.features.corecommunicationreliability.engine.CommunicationPathVerifier
import com.idworx.lisa.features.corecommunicationreliability.engine.CoreCommunicationReliabilityEngines
import com.idworx.lisa.features.corecommunicationreliability.engine.DefaultCoreCommunicationReliabilityEngine
import com.idworx.lisa.features.corecommunicationreliability.history.CommunicationReliabilityHistoryRecorder
import com.idworx.lisa.features.corecommunicationreliability.model.CommunicationMode
import com.idworx.lisa.features.corecommunicationreliability.model.CommunicationReliabilityOutcome
import com.idworx.lisa.features.corecommunicationreliability.model.PhraseReliabilityAction
import com.idworx.lisa.features.corecommunicationreliability.sequence.BlinkSequenceDebouncer
import com.idworx.lisa.features.corecommunicationreliability.sequence.BlinkSequenceNormalizer
import com.idworx.lisa.features.corecommunicationreliability.speech.SpeechOutputVerifier
import com.idworx.lisa.features.corecommunicationreliability.validation.CoreCommunicationReliabilityAuthorityV1
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CoreCommunicationReliabilityAuthorityV1Test {

    private lateinit var engine: DefaultCoreCommunicationReliabilityEngine
    private val mappings = defaultLanguageMappings()

    @Before
    fun setUp() {
        CommunicationReliabilityHistoryRecorder.clear()
        EmergencySpeechSafetyGuard.resetDebouncer()
        engine = CoreCommunicationReliabilityEngines.createForTests(
            companionMemory = CompanionMemoryEngines.createForTests(InMemoryCompanionMemoryRepository())
        )
    }

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = CoreCommunicationReliabilityAuthorityV1.validate()
        assertEquals(ValidationOutcome.PASS, report.outcome)
        assertEquals(CoreCommunicationReliabilityAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(CoreCommunicationReliabilityAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun validSequence_normalizesCorrectly() {
        val normalized = BlinkSequenceNormalizer.normalize(2, 1)
        assertEquals("L2 R1", normalized.label)
        assertTrue(CommunicationPathVerifier.verifyNormalization())
    }

    @Test
    fun validSequence_mapsToOnePhrase() {
        val ctx = CommunicationReliabilityContext(mappings = mappings)
        val report = engine.evaluatePhrasePath(ctx, 2, 6)
        assertEquals("yes", report.matchedPhraseId)
        assertEquals(1, report.metrics.find { it.name == "match_count" }?.value?.toInt())
    }

    @Test
    fun validPhrase_proceedsToConfirmation() {
        val ctx = CommunicationReliabilityContext(mappings = mappings)
        val report = engine.evaluatePhrasePath(ctx, 2, 6)
        assertEquals(PhraseReliabilityAction.PROCEED_TO_CONFIRMATION, report.attemptResult.action)
        assertTrue(report.attemptResult.phraseText!!.contains("Yes", ignoreCase = true))
    }

    @Test
    fun speechSuccess_recordsCommunicationHistory() {
        engine.recordSpeechDelivery(
            attemptId = "a1",
            phraseText = "Yes",
            phraseId = "yes",
            sequenceLeft = 2,
            sequenceRight = 6,
            mode = CommunicationMode.MAIN,
            emergency = false,
            success = true
        )
        assertEquals(1, CommunicationReliabilityHistoryRecorder.count())
        assertEquals("Yes", CommunicationReliabilityHistoryRecorder.last()?.phraseText)
    }

    @Test
    fun invalidSequence_doesNotProceedToSpeech() {
        val ctx = CommunicationReliabilityContext(mappings = mappings)
        val report = engine.evaluatePhrasePath(ctx, 1, 0)
        assertFalse(report.attemptResult.action == PhraseReliabilityAction.PROCEED_TO_CONFIRMATION)
    }

    @Test
    fun ambiguousOrUnknownSequence_blocked() {
        val ctx = CommunicationReliabilityContext(mappings = mappings)
        val report = engine.evaluatePhrasePath(ctx, 99, 99)
        assertEquals(CommunicationReliabilityOutcome.BLOCKED, report.finalOutcome)
    }

    @Test
    fun reservedNavigationSequence_doesNotMatchPhrase() {
        val ctx = CommunicationReliabilityContext(mappings = mappings)
        val report = engine.evaluatePhrasePath(ctx, 0, 4)
        assertNotNull(report.attemptResult.blockedReason)
    }

    @Test
    fun duplicateSequenceFiring_prevented() {
        val ctx = CommunicationReliabilityContext(mappings = mappings)
        engine.evaluatePhrasePath(ctx, 2, 6)
        val duplicate = engine.evaluatePhrasePath(ctx, 2, 6)
        assertEquals(CommunicationReliabilityOutcome.BLOCKED, duplicate.finalOutcome)
    }

    @Test
    fun lowConfidenceSequence_blocked() {
        val ctx = CommunicationReliabilityContext(mappings = mappings)
        val report = engine.evaluatePhrasePath(ctx, 1, 0)
        assertEquals(CommunicationReliabilityOutcome.BLOCKED, report.finalOutcome)
    }

    @Test
    fun emergencySequence_blockedInCommunicationTraining() {
        val safety = EmergencySpeechSafetyGuard.evaluate(
            6, 0,
            navigationTrainingActive = false,
            communicationTrainingActive = true,
            practiceMode = false
        )
        assertFalse(safety.allowed)
        assertNotNull(safety.blockedReason)
    }

    @Test
    fun emergencyDuplicateActivation_prevented() {
        EmergencySpeechSafetyGuard.resetDebouncer()
        val first = EmergencySpeechSafetyGuard.evaluate(6, 0, false, false, false)
        val second = EmergencySpeechSafetyGuard.evaluate(6, 0, false, false, false)
        assertTrue(first.allowed || first.navigationTraining)
        assertFalse(second.allowed)
    }

    @Test
    fun emergencyConfirmationPolicy_routesTrainingVerification() {
        val report = engine.evaluateEmergency(
            CommunicationReliabilityContext(mappings = mappings, navigationTrainingActive = true),
            6, 0
        )
        assertEquals(PhraseReliabilityAction.ROUTE_EMERGENCY_TRAINING, report.attemptResult.action)
        assertEquals(CommunicationReliabilityOutcome.PASS, report.finalOutcome)
    }

    @Test
    fun ttsFailure_producesSafeFailureResult() {
        val result = SpeechOutputVerifier.verifyRequest("Hello", "hello", ttsAvailable = false, speechBlocked = false)
        assertFalse(result.success)
        assertNotNull(result.failureReason)
    }

    @Test
    fun companionMemory_recordsMilestoneOnly() {
        val memory = CompanionMemoryEngines.createForTests(InMemoryCompanionMemoryRepository())
        val testEngine = CoreCommunicationReliabilityEngines.createForTests(companionMemory = memory)
        testEngine.recordSpeechDelivery("a", "Yes", "yes", 2, 6, CommunicationMode.MAIN, false, true)
        testEngine.recordSpeechDelivery("b", "No", "no", 0, 7, CommunicationMode.MAIN, false, true)
        assertEquals(1, memory.getMilestones().count { it == LearningMilestone.FirstPhrase })
    }

    @Test
    fun personalityEngineFeedback_usedForBlockedAttempt() {
        val ctx = CommunicationReliabilityContext(mappings = mappings)
        val report = engine.evaluatePhrasePath(ctx, 99, 99)
        val feedback = engine.feedbackForBlockedAttempt(report)
        assertTrue(feedback.isNotBlank())
    }

    @Test
    fun diagnostics_reportLastAttempt() {
        val ctx = CommunicationReliabilityContext(mappings = mappings)
        engine.evaluatePhrasePath(ctx, 2, 6)
        assertNotNull(CommunicationDiagnostics.lastAttempt())
        assertEquals("L2 R6", CommunicationDiagnostics.lastSequence())
    }

    @Test
    fun reliabilityReport_containsFailureReasonWhenBlocked() {
        val ctx = CommunicationReliabilityContext(mappings = mappings)
        val report = engine.evaluatePhrasePath(ctx, 99, 99)
        assertNotNull(report.failureReason)
    }

    @Test
    fun debouncer_preventsDoubleActivation() {
        val debouncer = BlinkSequenceDebouncer(5000L)
        assertTrue(debouncer.shouldAllow(2, 6))
        assertFalse(debouncer.shouldAllow(2, 6))
    }
}
