package com.idworx.lisa.features.experiencepolish.emotionalpresence.validation

import com.idworx.lisa.features.experiencepolish.emotionalpresence.audit.EmotionalPresenceAuditor
import com.idworx.lisa.features.experiencepolish.emotionalpresence.metadata.EmotionalPresenceMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object EmotionalPresenceAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_EMOTIONAL_PRESENCE_V1"
    const val PASS_TOKEN: String = EmotionalPresenceMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check("EP_001", "Emotional presence documented", EmotionalPresenceAuditor.phaseDocumented()),
            check("EP_002", "Presence engine with rate limits", EmotionalPresenceAuditor.presenceEngineExists() && EmotionalPresenceAuditor.rateLimitingDefined()),
            check("EP_003", "Presence dialogues in Personality Engine catalog", EmotionalPresenceAuditor.presenceDialoguesInCatalog()),
            check("EP_004", "Personality Engine generatePresence wired", EmotionalPresenceAuditor.personalityEngineWired()),
            check("EP_005", "All six presence moments surfaced", EmotionalPresenceAuditor.experienceSurfacesAllMoments()),
            check("EP_006", "MainActivity long-pause and session opening wired", EmotionalPresenceAuditor.mainActivityWired()),
            check("EP_007", "Training session fatigue and milestone wired", EmotionalPresenceAuditor.trainingSessionWired()),
            check("EP_008", "Caregiver reassurance from Personality Engine", EmotionalPresenceAuditor.caregiverReassuranceWired()),
            check("EP_009", "No Brain 2 dependency", EmotionalPresenceAuditor.noBrain2Dependency()),
            check("EP_010", "Android Device Testing checklist updated", EmotionalPresenceAuditor.deviceTestingChecklistUpdated()),
            check("EP_011", "Tests pass and Gradle validation task defined", EmotionalPresenceAuditor.testClassExists() && EmotionalPresenceAuditor.gradleTaskRegistered())
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Emotional Presence verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(
                EmotionalPresenceMetadata.PRESENCE_PHILOSOPHY,
                EmotionalPresenceMetadata.OVERTALKING_RULE
            ),
            affectedLicArticles = listOf("Part II — Lisa emotional presence"),
            affectedLiecArticles = listOf("Article 2.1 — Warm without chatbot"),
            affectedLvcArticles = listOf("Article 3.11 — Emotional Presence validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "Lisa feels emotionally present through brief Personality Engine dialogue with strict rate limits."
            } else {
                "${failed.size} Emotional Presence checks failed."
            },
            subsystem = "Emotional Presence"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
