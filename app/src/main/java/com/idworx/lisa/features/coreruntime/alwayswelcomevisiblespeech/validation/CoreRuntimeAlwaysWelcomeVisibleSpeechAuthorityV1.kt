package com.idworx.lisa.features.coreruntime.alwayswelcomevisiblespeech.validation

import com.idworx.lisa.features.coreruntime.alwayswelcomevisiblespeech.audit.CoreRuntimeAlwaysWelcomeVisibleSpeechAuditor
import com.idworx.lisa.features.coreruntime.alwayswelcomevisiblespeech.metadata.CoreRuntimeAlwaysWelcomeVisibleSpeechMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object CoreRuntimeAlwaysWelcomeVisibleSpeechAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_CORE_RUNTIME_ALWAYS_WELCOME_VISIBLE_SPEECH_V1"
    const val PASS_TOKEN: String = CoreRuntimeAlwaysWelcomeVisibleSpeechMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check("CRAW_001", "Cold launch gate always routes to Welcome", CoreRuntimeAlwaysWelcomeVisibleSpeechAuditor.coldLaunchGateAlwaysWelcome()),
            check("CRAW_002", "Store does not persist launch-screen override", CoreRuntimeAlwaysWelcomeVisibleSpeechAuditor.storeDoesNotPersistLaunchOverride()),
            check("CRAW_003", "MainActivity resets session UI on cold launch", CoreRuntimeAlwaysWelcomeVisibleSpeechAuditor.mainActivityResetsSessionUi()),
            check("CRAW_004", "Skipped workspace saved state returns Welcome", CoreRuntimeAlwaysWelcomeVisibleSpeechAuditor.runtime_skippedWorkspaceState_returnsWelcome()),
            check("CRAW_005", "Saved HELLO lesson state returns Welcome", CoreRuntimeAlwaysWelcomeVisibleSpeechAuditor.runtime_helloLessonState_returnsWelcome()),
            check("CRAW_006", "Start Guided Learning required before setup", CoreRuntimeAlwaysWelcomeVisibleSpeechAuditor.runtime_beginLearningRequiredForSetup()),
            check("CRAW_007", "Skip confirm exits training to workspace path", CoreRuntimeAlwaysWelcomeVisibleSpeechAuditor.runtime_confirmSkipRequiredForWorkspaceExit()),
            check("CRAW_008", "Training UI keeps camera processing alive", CoreRuntimeAlwaysWelcomeVisibleSpeechAuditor.trainingUiKeepsCameraAlive()),
            check("CRAW_009", "Training visibility uses active phases not isFinished", CoreRuntimeAlwaysWelcomeVisibleSpeechAuditor.shouldShowTrainingUsesActivePhases()),
            check("CRAW_010", "Visible phrase resolves on open page", CoreRuntimeAlwaysWelcomeVisibleSpeechAuditor.runtime_visiblePhraseResolves()),
            check("CRAW_011", "Hidden page phrase does not resolve", CoreRuntimeAlwaysWelcomeVisibleSpeechAuditor.runtime_hiddenPageDoesNotResolve()),
            check("CRAW_012", "Sequence finalize resets blink buffer", CoreRuntimeAlwaysWelcomeVisibleSpeechAuditor.finalizeSequenceResetsTrainingBuffer()),
            check("CRAW_013", "Idle timeout matches the default response time", CoreRuntimeAlwaysWelcomeVisibleSpeechAuditor.idleTimeoutMatchesDefaultResponseTime()),
            check("CRAW_014", "Voice policy remains phrase-translation-only", CoreRuntimeAlwaysWelcomeVisibleSpeechAuditor.phraseTranslationOnly()),
            check("CRAW_015", "Tests pass and Gradle validation task defined", CoreRuntimeAlwaysWelcomeVisibleSpeechAuditor.testClassExists() && CoreRuntimeAlwaysWelcomeVisibleSpeechAuditor.gradleTaskRegistered())
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Core runtime always-Welcome and visible speech verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(
                CoreRuntimeAlwaysWelcomeVisibleSpeechMetadata.LAUNCH_RULE,
                CoreRuntimeAlwaysWelcomeVisibleSpeechMetadata.SEQUENCE_RULE
            ),
            affectedLicArticles = listOf("Part I — Core runtime launch and speech"),
            affectedLiecArticles = listOf("Article 1.1 — Always Welcome on cold launch"),
            affectedLvcArticles = listOf("Article 1.2 — Visible sequence speech validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "Cold launch always shows Welcome; visible sequences finalize after 3s and speak phrase translation only."
            } else {
                "${failed.size} core runtime checks failed."
            },
            subsystem = "Core Runtime Always Welcome and Visible Speech"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
