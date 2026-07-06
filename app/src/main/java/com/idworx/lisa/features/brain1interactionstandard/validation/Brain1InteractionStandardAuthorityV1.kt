package com.idworx.lisa.features.brain1interactionstandard.validation

import com.idworx.lisa.features.brain1interactionstandard.audit.Brain1InteractionStandardAuditor
import com.idworx.lisa.features.brain1interactionstandard.metadata.Brain1InteractionStandardMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object Brain1InteractionStandardAuthorityV1 {

    const val AUTHORITY_NAME: String = "BRAIN1_INTERACTION_STANDARD_AUTHORITY_V1"
    const val PASS_TOKEN: String = "BRAIN1_INTERACTION_STANDARD_AUTHORITY_V1_PASS"

    fun validate(): ValidationReport {
        val checks = listOf(
            check("B1IS_001", "No single-blink Brain 1 interaction commands", Brain1InteractionStandardAuditor.noSingleBlinkCommands()),
            check("B1IS_002", "Universal interaction gesture table exists", Brain1InteractionStandardAuditor.universalGestureTableExists()),
            check("B1IS_003", "L2 standardized", Brain1InteractionStandardAuditor.l2Standardized()),
            check("B1IS_004", "R2 standardized", Brain1InteractionStandardAuditor.r2Standardized()),
            check("B1IS_005", "L1 R1 standardized", Brain1InteractionStandardAuditor.confirmStandardized()),
            check("B1IS_006", "R1 L1 cancel standardized (no L2 R2)", Brain1InteractionStandardAuditor.cancelStandardized()),
            check("B1IS_007", "First launch uses eye interaction", Brain1InteractionStandardAuditor.firstLaunchEyeDriven()),
            check("B1IS_008", "Guided Learning requires confirmation", Brain1InteractionStandardAuditor.guidedLearningRequiresConfirmation()),
            check("B1IS_009", "Workspace skip requires confirmation", Brain1InteractionStandardAuditor.workspaceSkipRequiresConfirmation()),
            check("B1IS_010", "Universal decision model implemented", Brain1InteractionStandardAuditor.universalDecisionModelImplemented()),
            check("B1IS_011", "Emergency confirmation implemented", Brain1InteractionStandardAuditor.emergencyConfirmationImplemented()),
            check("B1IS_012", "Reset confirmation implemented", Brain1InteractionStandardAuditor.resetConfirmationImplemented()),
            check("B1IS_013", "Recalibration confirmation implemented", Brain1InteractionStandardAuditor.recalibrationConfirmationImplemented()),
            check("B1IS_014", "Progressive gesture difficulty implemented", Brain1InteractionStandardAuditor.progressiveDifficultyImplemented()),
            check("B1IS_015", "Early lessons use only simple gestures", Brain1InteractionStandardAuditor.earlyLessonsUseSimpleGestures()),
            check("B1IS_016", "Calibration introduction improved", Brain1InteractionStandardAuditor.calibrationIntroImproved()),
            check("B1IS_017", "Calibration visuals improved", Brain1InteractionStandardAuditor.calibrationVisualsImproved()),
            check("B1IS_018", "Personality Engine reused", Brain1InteractionStandardAuditor.personalityEngineReused()),
            check("B1IS_019", "Zero-Touch Principle maintained", Brain1InteractionStandardAuditor.zeroTouchMaintained()),
            check("B1IS_020", "Existing Brain 1 systems reused", Brain1InteractionStandardAuditor.existingBrain1SystemsReused()),
            check("B1IS_021", "Documentation completed", Brain1InteractionStandardAuditor.documentationCompleted()),
            check("B1IS_022", "Tests pass and Gradle validation task defined", Brain1InteractionStandardAuditor.testClassExists() && Brain1InteractionStandardAuditor.gradleTaskRegistered())
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Brain 1 Interaction Standard verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(
                Brain1InteractionStandardMetadata.NO_SINGLE_BLINK_RULE,
                Brain1InteractionStandardMetadata.DECISION_MODEL
            ),
            affectedLicArticles = listOf("Part I — Brain 1 universal interaction language"),
            affectedLiecArticles = listOf("Article 1.1 — No single-blink interaction commands"),
            affectedLvcArticles = listOf("Article 3.7.2 — Brain 1 Interaction Standard validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "LISA Brain 1 Interaction Standard V1 establishes universal two-blink gestures with decision confirmation."
            } else {
                "${failed.size} Brain 1 Interaction Standard checks failed."
            },
            subsystem = "Brain 1 Interaction Standard"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
