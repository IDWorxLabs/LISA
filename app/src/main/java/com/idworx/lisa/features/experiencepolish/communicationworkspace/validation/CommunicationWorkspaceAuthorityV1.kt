package com.idworx.lisa.features.experiencepolish.communicationworkspace.validation

import com.idworx.lisa.features.experiencepolish.communicationworkspace.audit.CommunicationWorkspaceAuditor
import com.idworx.lisa.features.experiencepolish.communicationworkspace.metadata.CommunicationWorkspaceMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object CommunicationWorkspaceAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_EXPERIENCE_PHASE_B_COMMUNICATION_WORKSPACE_V1"
    const val PASS_TOKEN: String = CommunicationWorkspaceMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check("P5B_001", "Phase B polish documented", CommunicationWorkspaceAuditor.phaseDocumented()),
            check("P5B_002", "Workspace entry after Guided Learning", CommunicationWorkspaceAuditor.workspaceEntryAfterTraining()),
            check("P5B_003", "Workspace navigation clarity improved", CommunicationWorkspaceAuditor.navigationClarityImproved()),
            check("P5B_004", "Phrase category guidance exists", CommunicationWorkspaceAuditor.categoryMenuGuidanceExists()),
            check("P5B_005", "Phrase selection guidance exists", CommunicationWorkspaceAuditor.phraseSelectionGuidanceExists()),
            check("P5B_006", "Back behavior documented (L2 R2 workspace back)", CommunicationWorkspaceAuditor.backBehaviorDocumented()),
            check("P5B_007", "Gesture conflict layers separated", CommunicationWorkspaceAuditor.gestureLayersSeparated()),
            check("P5B_008", "Fatigue reduction hints present", CommunicationWorkspaceAuditor.fatigueReductionPresent()),
            check("P5B_009", "Emergency access documented with confirmation", CommunicationWorkspaceAuditor.emergencyAccessDocumented()),
            check("P5B_010", "Guided Communication screen free of Caregiver panel", CommunicationWorkspaceAuditor.freeOfCaregiverPanel()),
            check("P5B_011", "Personality Engine workspace dialogues", CommunicationWorkspaceAuditor.personalityEngineDialogues()),
            check("P5B_012", "Zero-Touch workspace overlay preserved", CommunicationWorkspaceAuditor.zeroTouchPreserved()),
            check("P5B_013", "Workspace metadata defines navigation gestures", CommunicationWorkspaceAuditor.metadataDefinesGestures()),
            check("P5B_014", "Guided overlay wired for eye navigation", CommunicationWorkspaceAuditor.workspaceOverlayUsesGuidedNavigation()),
            check("P5B_015", "No Brain 2 dependency introduced", CommunicationWorkspaceAuditor.noBrain2Dependency()),
            check("P5B_016", "Android Device Testing checklist updated", CommunicationWorkspaceAuditor.deviceTestingChecklistUpdated()),
            check("P5B_017", "Tests pass and Gradle validation task defined", CommunicationWorkspaceAuditor.testClassExists() && CommunicationWorkspaceAuditor.gradleTaskRegistered()),
            check("P5B_018", "Header free of duplicate Vocabulary/Choose Category/Conversation labels", CommunicationWorkspaceAuditor.headerFreeOfDuplicateVocabularyLabels())
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Communication Workspace Phase B verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(
                CommunicationWorkspaceMetadata.GESTURE_LAYER_RULE,
                "Workspace gestures: ${CommunicationWorkspaceMetadata.WORKSPACE_NAVIGATION_GESTURES.keys.joinToString()}"
            ),
            affectedLicArticles = listOf("Part II — Communication Workspace daily use"),
            affectedLiecArticles = listOf("Article 2.1 — Workspace navigation gestures"),
            affectedLvcArticles = listOf("Article 3.9 — Experience Polish Phase B validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "LISA Experience Polish Phase B makes the Communication Workspace calm, clear, and zero-touch."
            } else {
                "${failed.size} Communication Workspace Phase B checks failed."
            },
            subsystem = "Experience Polish — Phase B"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
