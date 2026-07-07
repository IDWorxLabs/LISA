package com.idworx.lisa.features.guidedpreferencesgestureconsistency.validation

import com.idworx.lisa.features.guidedpreferencesgestureconsistency.audit.GuidedPreferencesGestureConsistencyAuditor
import com.idworx.lisa.features.guidedpreferencesgestureconsistency.metadata.GuidedPreferencesGestureConsistencyMetadata
import com.idworx.lisa.features.guidedtraininggesturemismatch.validation.GuidedTrainingGestureMismatchAuthorityV1
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object GuidedPreferencesGestureConsistencyAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_GUIDED_PREFERENCES_GESTURE_CONSISTENCY_V1"
    const val PASS_TOKEN: String = GuidedPreferencesGestureConsistencyMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check(
                "GPGC_001",
                "Every displayed Preferences gesture matches the gesture that actually executes the action",
                GuidedPreferencesGestureConsistencyAuditor.preferencesGesturesMatchExecutableActions(),
                "Keep processPreferencesAdjustmentGesture accepting exactly GuidedModeNavigation.DECREASE_VALUE_*/INCREASE_VALUE_*/SELECT_*/BACK_*/CATEGORIES_* for Decrease/Increase/Save/Cancel/Categories."
            ),
            check(
                "GPGC_002",
                "No hardcoded gesture literals remain in the Preferences / Adjustment panel",
                GuidedPreferencesGestureConsistencyAuditor.noHardcodedGestureLiteralsRemainInPreferencesPanel(),
                "Remove any standalone \"L<n> R<n>\" literal from PreferencesAdjustmentPanel; every sequenceLabel must be computed via formatWinkSequenceShort(...)."
            ),
            check(
                "GPGC_003",
                "Preferences gesture labels are derived from the shared gesture authority",
                GuidedPreferencesGestureConsistencyAuditor.preferencesLabelsDeriveFromSharedGestureAuthority(),
                "Keep every PreferencesAdjustmentPanel row computing its sequenceLabel from formatWinkSequenceShort(GuidedModeNavigation.*) / EMERGENCY_LEFT_WINKS/EMERGENCY_RIGHT_WINKS."
            ),
            check(
                "GPGC_004",
                "A gesture definition change automatically changes the displayed label everywhere it appears",
                GuidedPreferencesGestureConsistencyAuditor.preferencesLabelIsAPureFunctionOfTheSharedConstant(),
                "Keep the Preferences panel, the main navigation panel and the category access row all calling formatWinkSequenceShort on the exact same GuidedModeNavigation/EMERGENCY_* constants — never a second, independently maintained literal."
            ),
            check(
                "GPGC_005",
                "Quick Controls gesture labels match the shared LisaSystemLanguage gesture authority",
                GuidedPreferencesGestureConsistencyAuditor.quickControlsGestureLabelsMatchSharedAuthority(),
                "Keep QuickControlsOverlay rendering every badge via quickControlGesture(action), which reads LisaSystemLanguage.quickControlCommands — the same list resolveQuickControlCommand checks against."
            ),
            check(
                "GPGC_006",
                "Repository-wide production UI sweep finds no hardcoded gesture literals",
                GuidedPreferencesGestureConsistencyAuditor.productionUiFilesFreeOfHardcodedGestureLiterals(),
                "Audit app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt, LisaCommunicationAssistUi.kt and LisaAccessibilityUi.kt for standalone \"L<n> R<n>\" literals and replace any with formatWinkSequenceShort(...)."
            ),
            check(
                "GPGC_007",
                "Regression coverage — existing Guided Training gesture-mismatch validation remains green",
                GuidedTrainingGestureMismatchAuthorityV1.validate().outcome == ValidationOutcome.PASS,
                "Re-run GuidedTrainingGestureMismatchAuthorityV1 and fix any regression before shipping the Preferences gesture consistency fix."
            ),
            check(
                "GPGC_008",
                "Test class exists and Gradle validation task is registered",
                GuidedPreferencesGestureConsistencyAuditor.testClassExists() &&
                    GuidedPreferencesGestureConsistencyAuditor.gradleTaskRegistered(),
                "Add GuidedPreferencesGestureConsistencyAuthorityV1Test and register ${GuidedPreferencesGestureConsistencyMetadata.GRADLE_TASK} in app/build.gradle.kts."
            )
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Guided Preferences Gesture Consistency V1 verified ${checks.size} checks. " +
                "Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(GuidedPreferencesGestureConsistencyMetadata.DESIGN_RULE),
            affectedLicArticles = listOf(
                "Article 1.4.1.3 — User must never become trapped"
            ),
            affectedLiecArticles = listOf(
                "Article 2.20 — LISA must never teach or display a gesture that differs from real workspace behavior"
            ),
            affectedLvcArticles = listOf(
                "Article 3.36 — Guided Preferences Gesture Consistency V1 validation"
            ),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "The Preferences / Adjustment panel and the Quick Controls overlay no longer own a " +
                    "second copy of any gesture. Every displayed badge is computed at render time from " +
                    "formatWinkSequenceShort(...) over the exact GuidedModeNavigation/EMERGENCY_* " +
                    "constants and LisaSystemLanguage.quickControlCommands entries the real gesture " +
                    "handlers check against, so a future change to any constant is reflected everywhere " +
                    "automatically and no screen can silently drift out of sync."
            } else {
                "${failed.size} Guided Preferences Gesture Consistency V1 checks failed."
            },
            subsystem = "Guided Preferences Gesture Consistency"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
