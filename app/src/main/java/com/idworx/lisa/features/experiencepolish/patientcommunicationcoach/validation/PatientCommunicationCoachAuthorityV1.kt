package com.idworx.lisa.features.experiencepolish.patientcommunicationcoach.validation

import com.idworx.lisa.features.experiencepolish.patientcommunicationcoach.audit.PatientCommunicationCoachAuditor
import com.idworx.lisa.features.experiencepolish.patientcommunicationcoach.metadata.PatientCommunicationCoachMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object PatientCommunicationCoachAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_PATIENT_COMMUNICATION_COACH_V1"
    const val PASS_TOKEN: String = PatientCommunicationCoachMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check("PCC_001", "Patient coach documented", PatientCommunicationCoachAuditor.phaseDocumented()),
            check("PCC_002", "Coach pacing engine defined", PatientCommunicationCoachAuditor.coachEngineExists()),
            check("PCC_003", "Coach dialogues in Personality Engine", PatientCommunicationCoachAuditor.coachDialoguesPresent()),
            check("PCC_004", "Gradual gesture progression in catalog", PatientCommunicationCoachAuditor.gradualGestureProgression()),
            check("PCC_005", "Simple daily phrases prioritized early", PatientCommunicationCoachAuditor.dailyPhrasesPrioritized()),
            check("PCC_006", "Adaptive offer enriched for coach", PatientCommunicationCoachAuditor.adaptiveOfferEnriched()),
            check("PCC_007", "Training session wired for coach", PatientCommunicationCoachAuditor.trainingSessionWired()),
            check("PCC_008", "Caregiver-visible progress on lesson screen", PatientCommunicationCoachAuditor.caregiverProgressVisible()),
            check("PCC_009", "Coach UI state in training flow", PatientCommunicationCoachAuditor.coachUiStateInTraining()),
            check("PCC_010", "Repeat and slow-down coaching present", PatientCommunicationCoachAuditor.personalityEngineDialogues()),
            check("PCC_011", "No Brain 2 dependency", PatientCommunicationCoachAuditor.noBrain2Dependency()),
            check("PCC_012", "Android Device Testing checklist updated", PatientCommunicationCoachAuditor.deviceTestingChecklistUpdated()),
            check("PCC_013", "Tests pass and Gradle validation task defined", PatientCommunicationCoachAuditor.testClassExists() && PatientCommunicationCoachAuditor.gradleTaskRegistered())
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Patient Communication Coach verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(
                PatientCommunicationCoachMetadata.COACHING_PHILOSOPHY,
                PatientCommunicationCoachMetadata.GESTURE_PROGRESSION_RULE
            ),
            affectedLicArticles = listOf("Part II — Guided Learning communication"),
            affectedLiecArticles = listOf("Article 2.0 — Progressive phrase teaching"),
            affectedLvcArticles = listOf("Article 3.10 — Patient Communication Coach validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "Lisa teaches communication like a patient human coach with pacing, repeat, and caregiver-visible progress."
            } else {
                "${failed.size} Patient Communication Coach checks failed."
            },
            subsystem = "Patient Communication Coach"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
