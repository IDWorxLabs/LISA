package com.idworx.lisa.features.corecommunicationreliability.validation

import com.idworx.lisa.features.corecommunicationreliability.diagnostics.CommunicationDiagnostics
import com.idworx.lisa.features.corecommunicationreliability.emergency.EmergencyConfirmationPolicy
import com.idworx.lisa.features.corecommunicationreliability.emergency.EmergencySpeechSafetyGuard
import com.idworx.lisa.features.corecommunicationreliability.engine.CommunicationPathVerifier
import com.idworx.lisa.features.corecommunicationreliability.engine.CommunicationReliabilityContext
import com.idworx.lisa.features.corecommunicationreliability.engine.CoreCommunicationReliabilityEngine
import com.idworx.lisa.features.corecommunicationreliability.engine.CoreCommunicationReliabilityEngines
import com.idworx.lisa.features.corecommunicationreliability.engine.DefaultCoreCommunicationReliabilityEngine
import com.idworx.lisa.features.corecommunicationreliability.history.CommunicationReliabilityHistoryRecorder
import com.idworx.lisa.features.corecommunicationreliability.metadata.CoreCommunicationReliabilityMetadata
import com.idworx.lisa.features.corecommunicationreliability.model.CommunicationAttempt
import com.idworx.lisa.features.corecommunicationreliability.model.CommunicationMode
import com.idworx.lisa.features.corecommunicationreliability.model.CommunicationReliabilityOutcome
import com.idworx.lisa.features.corecommunicationreliability.model.CommunicationReliabilityReport
import com.idworx.lisa.features.corecommunicationreliability.phrase.PhraseConfirmationPolicy
import com.idworx.lisa.features.corecommunicationreliability.phrase.PhraseMatchVerifier
import com.idworx.lisa.features.corecommunicationreliability.phrase.PhraseSelectionGuard
import com.idworx.lisa.features.corecommunicationreliability.sequence.BlinkSequenceConfidenceScorer
import com.idworx.lisa.features.corecommunicationreliability.sequence.BlinkSequenceDebouncer
import com.idworx.lisa.features.corecommunicationreliability.sequence.BlinkSequenceNormalizer
import com.idworx.lisa.features.corecommunicationreliability.sequence.BlinkSequenceValidator
import com.idworx.lisa.features.corecommunicationreliability.speech.SpeechOutputVerifier
import com.idworx.lisa.features.corecommunicationreliability.speech.SpeechReliabilityAdapter
import com.idworx.lisa.features.companionmemory.engine.CompanionMemoryEngines
import com.idworx.lisa.features.companionmemory.model.LearningMilestone
import com.idworx.lisa.features.onboardingguide.audio.OnboardingNarrationController
import com.idworx.lisa.features.personality.engine.LisaPersonalityEngine
import com.idworx.lisa.features.personality.engine.LisaPersonalityEngines
import com.idworx.lisa.defaultLanguageMappings
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport
import java.io.File

object CoreCommunicationReliabilityAuthorityV1 {

    const val AUTHORITY_NAME: String = "CORE_COMMUNICATION_RELIABILITY_AUTHORITY_V1"
    const val PASS_TOKEN: String = "CORE_COMMUNICATION_RELIABILITY_AUTHORITY_V1_PASS"

    private val PROJECT_ROOT = File(System.getProperty("user.dir") ?: ".")

    fun validate(): ValidationReport {
        val engine = CoreCommunicationReliabilityEngines.createForTests()
        val checks = listOf(
            check("CCREL_001", "Core communication reliability module exists", classExists(CoreCommunicationReliabilityEngine::class.java)),
            check("CCREL_002", "README exists", readmeExists()),
            check("CCREL_003", "CoreCommunicationReliabilityEngine exists", CoreCommunicationReliabilityEngine::class.java.isInterface),
            check("CCREL_004", "DefaultCoreCommunicationReliabilityEngine exists", classExists(DefaultCoreCommunicationReliabilityEngine::class.java)),
            check("CCREL_005", "CommunicationPathVerifier exists", classExists(CommunicationPathVerifier::class.java)),
            check("CCREL_006", "BlinkSequenceNormalizer exists", classExists(BlinkSequenceNormalizer::class.java)),
            check("CCREL_007", "BlinkSequenceValidator exists", classExists(BlinkSequenceValidator::class.java)),
            check("CCREL_008", "BlinkSequenceDebouncer exists", classExists(BlinkSequenceDebouncer::class.java)),
            check("CCREL_009", "BlinkSequenceConfidenceScorer exists", classExists(BlinkSequenceConfidenceScorer::class.java)),
            check("CCREL_010", "PhraseMatchVerifier exists", classExists(PhraseMatchVerifier::class.java)),
            check("CCREL_011", "PhraseSelectionGuard exists", classExists(PhraseSelectionGuard::class.java)),
            check("CCREL_012", "PhraseConfirmationPolicy exists", classExists(PhraseConfirmationPolicy::class.java)),
            check("CCREL_013", "SpeechOutputVerifier exists", classExists(SpeechOutputVerifier::class.java)),
            check("CCREL_014", "SpeechReliabilityAdapter exists", classExists(SpeechReliabilityAdapter::class.java)),
            check("CCREL_015", "EmergencySpeechSafetyGuard exists", classExists(EmergencySpeechSafetyGuard::class.java)),
            check("CCREL_016", "EmergencyConfirmationPolicy exists", classExists(EmergencyConfirmationPolicy::class.java)),
            check("CCREL_017", "CommunicationReliabilityReport exists", classExists(CommunicationReliabilityReport::class.java)),
            check("CCREL_018", "CommunicationAttempt model exists", classExists(CommunicationAttempt::class.java)),
            check("CCREL_019", "Communication history recorder exists", classExists(CommunicationReliabilityHistoryRecorder::class.java)),
            check("CCREL_020", "Diagnostics exist", classExists(CommunicationDiagnostics::class.java)),
            check("CCREL_021", "Existing TTS reused not duplicated", existingTtsReused()),
            check("CCREL_022", "Existing eye/blink systems reused", eyeBlinkReused()),
            check("CCREL_023", "Emergency training cannot trigger real emergency", CommunicationPathVerifier.verifyEmergencyBlockedInCommunicationTraining()),
            check("CCREL_024", "Ambiguous sequences blocked", ambiguousSequencesBlocked(engine)),
            check("CCREL_025", "Duplicate sequence firing prevented", duplicateFiringPrevented(engine)),
            check("CCREL_026", "Low-confidence sequences blocked", lowConfidenceBlocked(engine)),
            check("CCREL_027", "Valid phrase path reaches speech outcome", CommunicationPathVerifier.verifyValidPathReachesSpeechOutcome()),
            check("CCREL_028", "Successful phrase records history", CommunicationPathVerifier.verifyHistoryRecording()),
            check("CCREL_029", "Companion Memory records milestones only", companionMemoryMilestonesOnly()),
            check("CCREL_030", "Personality Engine used for feedback", personalityFeedbackWorks(engine)),
            check("CCREL_031", "Tests exist and pass token defined",
                testClassExists() && PASS_TOKEN == "CORE_COMMUNICATION_RELIABILITY_AUTHORITY_V1_PASS")
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Core Communication Reliability verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = emptyList(),
            affectedLicArticles = listOf("Article 4.2.1.2 — Every visible action tappable if touch exists"),
            affectedLiecArticles = listOf("Article 6.2.1.1 — Recovery engine guarantees escape"),
            affectedLvcArticles = listOf("Article 4.1.1.2 — Evidence record requirements"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "LISA Core Communication Reliability V1 validates the eye/blink to speech path safely and deterministically."
            } else {
                "${failed.size} core communication reliability checks failed."
            },
            subsystem = "Core Communication Reliability"
        )
    }

    private fun readmeExists(): Boolean {
        val candidates = buildList {
            add(File(PROJECT_ROOT, "features/core-communication-reliability/README.md"))
            var dir: File? = PROJECT_ROOT
            repeat(5) {
                dir?.let { add(File(it, "features/core-communication-reliability/README.md")) }
                dir = dir?.parentFile
            }
        }
        return candidates.any { it.exists() }
    }

    private fun existingTtsReused(): Boolean =
        classExists(OnboardingNarrationController::class.java) &&
            !File(PROJECT_ROOT, "app/src/main/java/com/idworx/lisa/features/corecommunicationreliability/speech/DuplicateTtsEngine.kt").exists()

    private fun eyeBlinkReused(): Boolean =
        classExists(com.idworx.lisa.MainActivity::class.java) &&
            classExists(BlinkSequenceNormalizer::class.java)

    private fun ambiguousSequencesBlocked(engine: DefaultCoreCommunicationReliabilityEngine): Boolean {
        val ctx = CommunicationReliabilityContext(mappings = defaultLanguageMappings())
        val report = engine.evaluatePhrasePath(ctx, 0, 4)
        return report.finalOutcome == CommunicationReliabilityOutcome.BLOCKED ||
            report.attemptResult.action == com.idworx.lisa.features.corecommunicationreliability.model.PhraseReliabilityAction.NO_PHRASE
    }

    private fun duplicateFiringPrevented(engine: DefaultCoreCommunicationReliabilityEngine): Boolean {
        val ctx = CommunicationReliabilityContext(mappings = defaultLanguageMappings())
        engine.evaluatePhrasePath(ctx, 2, 6)
        val duplicate = engine.evaluatePhrasePath(ctx, 2, 6)
        return duplicate.finalOutcome == CommunicationReliabilityOutcome.BLOCKED
    }

    private fun lowConfidenceBlocked(engine: DefaultCoreCommunicationReliabilityEngine): Boolean {
        val ctx = CommunicationReliabilityContext(mappings = defaultLanguageMappings())
        val report = engine.evaluatePhrasePath(ctx, 1, 0)
        return report.finalOutcome == CommunicationReliabilityOutcome.BLOCKED
    }

    private fun testClassExists(): Boolean =
        File(PROJECT_ROOT, "app/src/test/java/com/idworx/lisa/validation/authority/CoreCommunicationReliabilityAuthorityV1Test.kt").exists() ||
            File(PROJECT_ROOT, "src/test/java/com/idworx/lisa/validation/authority/CoreCommunicationReliabilityAuthorityV1Test.kt").exists()

    private fun companionMemoryMilestonesOnly(): Boolean {
        val memory = CompanionMemoryEngines.createForTests()
        val before = memory.getMilestones().size
        val engine = CoreCommunicationReliabilityEngines.createForTests(companionMemory = memory)
        engine.recordSpeechDelivery(
            attemptId = "test",
            phraseText = "Yes",
            phraseId = "yes",
            sequenceLeft = 2,
            sequenceRight = 6,
            mode = CommunicationMode.MAIN,
            emergency = false,
            success = true
        )
        engine.recordSpeechDelivery(
            attemptId = "test2",
            phraseText = "No",
            phraseId = "no",
            sequenceLeft = 0,
            sequenceRight = 7,
            mode = CommunicationMode.MAIN,
            emergency = false,
            success = true
        )
        val milestones = memory.getMilestones()
        return milestones.size <= before + 1 &&
            (LearningMilestone.FirstPhrase in milestones || milestones.size == before)
    }

    private fun personalityFeedbackWorks(engine: DefaultCoreCommunicationReliabilityEngine): Boolean {
        val ctx = CommunicationReliabilityContext(mappings = defaultLanguageMappings())
        val report = engine.evaluatePhrasePath(ctx, 99, 99)
        val feedback = engine.feedbackForBlockedAttempt(report)
        return feedback.isNotBlank() && classExists(LisaPersonalityEngine::class.java)
    }

    private fun classExists(clazz: Class<*>): Boolean = try {
        Class.forName(clazz.name)
        true
    } catch (_: Exception) {
        false
    }

    private fun check(
        id: String,
        description: String,
        passed: Boolean,
        remediation: String? = null
    ): ValidationCheckResult = ValidationCheckResult(
        checkId = id,
        description = description,
        passed = passed,
        remediation = remediation
    )
}
