package com.idworx.lisa.features.experiencepolish.firstfiveminutes.validation

import com.idworx.lisa.features.experiencepolish.firstfiveminutes.audit.FirstFiveMinutesAuditor
import com.idworx.lisa.features.experiencepolish.firstfiveminutes.metadata.FirstFiveMinutesMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object FirstFiveMinutesAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_EXPERIENCE_PHASE_A_FIRST_FIVE_MINUTES_V1"
    const val PASS_TOKEN: String = FirstFiveMinutesMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check("P5M_001", "Phase A polish documented", FirstFiveMinutesAuditor.phaseDocumented()),
            check("P5M_002", "First launch choice is eye-driven", FirstFiveMinutesAuditor.firstLaunchEyeDriven()),
            check("P5M_003", "Choice confirmation required before execution", FirstFiveMinutesAuditor.choiceConfirmationRequired()),
            check("P5M_004", "Cancel gesture is R1 L1 only", FirstFiveMinutesAuditor.cancelIsR1L1Only()),
            check("P5M_005", "L2 R2 is not used as cancel", FirstFiveMinutesAuditor.noL2R2CancelInBrain1()),
            check("P5M_006", "Universal gesture table matches agreed standard", FirstFiveMinutesAuditor.universalGestureTableMatches()),
            check("P5M_007", "Meet Lisa narration is richer than a simple welcome", FirstFiveMinutesAuditor.meetLisaNarrationRich()),
            check("P5M_008", "Getting Ready narration exists", FirstFiveMinutesAuditor.gettingReadyNarrationExists()),
            check("P5M_009", "Calibration introduction exists", FirstFiveMinutesAuditor.calibrationIntroExists()),
            check("P5M_010", "Calibration dot is visually obvious", FirstFiveMinutesAuditor.calibrationDotVisible()),
            check("P5M_011", "Calibration result dialogue exists", FirstFiveMinutesAuditor.calibrationResultDialogueExists()),
            check("P5M_012", "HELLO does not use L1 R6", FirstFiveMinutesAuditor.helloNotL1R6()),
            check("P5M_013", "First phrase uses two-blink difficulty", FirstFiveMinutesAuditor.firstPhraseTwoBlink()),
            check("P5M_014", "Early phrases are progressive", FirstFiveMinutesAuditor.earlyPhrasesProgressive()),
            check("P5M_015", "Failed attempt dialogue is gentle", FirstFiveMinutesAuditor.gentleFailureDialogue()),
            check("P5M_016", "No forbidden punitive wording", FirstFiveMinutesAuditor.noForbiddenPunitiveWording()),
            check("P5M_017", "Personality Engine provides user-facing dialogue", FirstFiveMinutesAuditor.personalityEngineProvidesDialogue()),
            check("P5M_018", "Zero-Touch Principle preserved", FirstFiveMinutesAuditor.zeroTouchPreserved()),
            check("P5M_019", "Android Device Testing checklist updated", FirstFiveMinutesAuditor.deviceTestingChecklistUpdated()),
            check("P5M_020", "Tests pass and Gradle validation task defined", FirstFiveMinutesAuditor.testClassExists() && FirstFiveMinutesAuditor.gradleTaskRegistered())
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "First Five Minutes Phase A verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(
                FirstFiveMinutesMetadata.CANCEL_GESTURE,
                FirstFiveMinutesMetadata.FIRST_FOUR_PHRASES.joinToString("; ")
            ),
            affectedLicArticles = listOf("Part I — First five minutes experience"),
            affectedLiecArticles = listOf("Article 1.2 — Universal interaction gestures"),
            affectedLvcArticles = listOf("Article 3.8 — Experience Polish Phase A validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "LISA Experience Polish Phase A makes the first five minutes calm, guided, and zero-touch."
            } else {
                "${failed.size} First Five Minutes Phase A checks failed."
            },
            subsystem = "Experience Polish — Phase A"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
