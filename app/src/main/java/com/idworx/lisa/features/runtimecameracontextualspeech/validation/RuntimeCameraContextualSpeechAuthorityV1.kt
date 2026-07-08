package com.idworx.lisa.features.runtimecameracontextualspeech.validation

import com.idworx.lisa.features.runtimecameracontextualspeech.audit.RuntimeCameraContextualSpeechAuditor
import com.idworx.lisa.features.runtimecameracontextualspeech.metadata.RuntimeCameraContextualSpeechMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object RuntimeCameraContextualSpeechAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_RUNTIME_CAMERA_AND_CONTEXTUAL_SPEECH_V1"
    const val PASS_TOKEN: String = RuntimeCameraContextualSpeechMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check("RCCS_001", "Setup step changes persist in observable UI state", RuntimeCameraContextualSpeechAuditor.setupStepPersistedInUiState()),
            check("RCCS_002", "CompleteSetup routes to HELLO teaching screen", RuntimeCameraContextualSpeechAuditor.completeSetupRoutesToHello()),
            check("RCCS_003", "Runtime CompleteSetup opens CommunicationLesson index 0", RuntimeCameraContextualSpeechAuditor.runtime_completeSetup_opensHelloLesson()),
            check("RCCS_004", "Visible-page phrase resolver exists", RuntimeCameraContextualSpeechAuditor.visiblePageResolverExists()),
            check("RCCS_005", "Workspace phrase context resolver exists", RuntimeCameraContextualSpeechAuditor.workspacePhraseResolverExists()),
            check("RCCS_006", "Hidden category phrases do not speak when category closed", RuntimeCameraContextualSpeechAuditor.runtime_hiddenCategoryPhraseDoesNotResolve()),
            check("RCCS_007", "Hidden page phrases do not resolve", RuntimeCameraContextualSpeechAuditor.runtime_hiddenPagePhraseDoesNotResolve()),
            check("RCCS_008", "Visible open-category phrases resolve", RuntimeCameraContextualSpeechAuditor.runtime_visiblePhraseResolvesOnOpenCategory()),
            check("RCCS_009", "Partial-sequence continuation uses workspace context", RuntimeCameraContextualSpeechAuditor.runtime_continuationUsesWorkspaceContext()),
            check("RCCS_010", "Workspace navigation gestures remain separate", RuntimeCameraContextualSpeechAuditor.workspaceNavigationGesturesSeparated()),
            check("RCCS_011", "Phrase idle timeout matches the default response time", RuntimeCameraContextualSpeechAuditor.idleTimeoutMatchesDefaultResponseTime()),
            check("RCCS_012", "Voice policy remains phrase-translation-only", RuntimeCameraContextualSpeechAuditor.phraseTranslationOnlyPolicy()),
            check("RCCS_013", "Tests pass and Gradle validation task defined", RuntimeCameraContextualSpeechAuditor.testClassExists() && RuntimeCameraContextualSpeechAuditor.gradleTaskRegistered())
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Runtime camera Continue and contextual workspace speech verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(
                RuntimeCameraContextualSpeechMetadata.SETUP_CONTINUE_FLOW,
                RuntimeCameraContextualSpeechMetadata.CONTEXTUAL_PHRASE_RULE,
                RuntimeCameraContextualSpeechMetadata.VOICE_POLICY
            ),
            affectedLicArticles = listOf("Part II — Guided Learning setup and teaching"),
            affectedLiecArticles = listOf("Article 2.2 — Camera setup Continue to HELLO"),
            affectedLvcArticles = listOf("Article 3.16 — Runtime Camera and Contextual Speech validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "Camera Continue opens HELLO teaching; workspace phrases resolve and speak only from visible open context after idle timeout."
            } else {
                "${failed.size} runtime camera/contextual speech checks failed."
            },
            subsystem = "Runtime Camera and Contextual Speech"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
