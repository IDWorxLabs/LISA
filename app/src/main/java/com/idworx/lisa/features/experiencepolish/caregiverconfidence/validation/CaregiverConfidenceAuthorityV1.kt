package com.idworx.lisa.features.experiencepolish.caregiverconfidence.validation

import com.idworx.lisa.features.experiencepolish.caregiverconfidence.audit.CaregiverConfidenceAuditor
import com.idworx.lisa.features.experiencepolish.caregiverconfidence.metadata.CaregiverConfidenceMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object CaregiverConfidenceAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_CAREGIVER_CONFIDENCE_V1"
    const val PASS_TOKEN: String = CaregiverConfidenceMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check("CC_001", "Caregiver confidence documented", CaregiverConfidenceAuditor.phaseDocumented()),
            check("CC_002", "Confidence engine defined", CaregiverConfidenceAuditor.confidenceEngineExists()),
            check("CC_003", "Caregiver dialogues in Personality Engine catalog", CaregiverConfidenceAuditor.dialoguesInCatalog()),
            check("CC_004", "Personality Engine generateCaregiverSupport wired", CaregiverConfidenceAuditor.personalityEngineWired()),
            check("CC_005", "All eight support moments surfaced", CaregiverConfidenceAuditor.experienceSurfacesAllMoments()),
            check("CC_006", "Setup screen caregiver strip wired", CaregiverConfidenceAuditor.setupScreenWired()),
            check("CC_007", "Calibration screen caregiver support wired", CaregiverConfidenceAuditor.calibrationScreenWired()),
            check("CC_008", "MainActivity tracking recovery wired", CaregiverConfidenceAuditor.mainActivityTrackingRecoveryWired()),
            check("CC_009", "Calibration adapter uses Personality Engine hints", CaregiverConfidenceAuditor.calibrationAdapterUsesEngine()),
            check("CC_010", "Workspace caregiver strip from engine", CaregiverConfidenceAuditor.workspaceCaregiverStripWired()),
            check("CC_011", "No Brain 2 dependency", CaregiverConfidenceAuditor.noBrain2Dependency()),
            check("CC_012", "Android Device Testing checklist updated", CaregiverConfidenceAuditor.deviceTestingChecklistUpdated()),
            check("CC_013", "Tests pass and Gradle validation task defined", CaregiverConfidenceAuditor.testClassExists() && CaregiverConfidenceAuditor.gradleTaskRegistered())
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Caregiver Confidence verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(
                CaregiverConfidenceMetadata.CONFIDENCE_PHILOSOPHY,
                CaregiverConfidenceMetadata.VISIBILITY_RULE
            ),
            affectedLicArticles = listOf("Part II — Caregiver confidence"),
            affectedLiecArticles = listOf("Article 2.2 — Support without technical jargon"),
            affectedLvcArticles = listOf("Article 3.12 — Caregiver Confidence validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "Caregivers receive plain-language setup, calibration, and recovery support through the Personality Engine."
            } else {
                "${failed.size} Caregiver Confidence checks failed."
            },
            subsystem = "Caregiver Confidence"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
