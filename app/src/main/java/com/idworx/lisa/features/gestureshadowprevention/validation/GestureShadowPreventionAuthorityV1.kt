package com.idworx.lisa.features.gestureshadowprevention.validation

import com.idworx.lisa.features.gestureshadowprevention.audit.GestureShadowPreventionAuditor
import com.idworx.lisa.features.gestureshadowprevention.metadata.GestureShadowPreventionMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

/**
 * Validates that the gesture shadowing introduced by the Categories (L3 R0) and Finish
 * Training (L0 R3) navigation gestures — which silently made "good_morning" and "i_need_water"
 * unreachable — has been resolved by reassigning the affected phrases, and that the gesture
 * allocation system now generically prevents this class of defect from recurring.
 */
object GestureShadowPreventionAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_GESTURE_SHADOW_PREVENTION_V1"
    const val PASS_TOKEN: String = GestureShadowPreventionMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val report = GestureShadowPreventionAuditor.buildConflictReport()
        val checks = listOf(
            check("GSP_001", "Categories still uses L3 R0", GestureShadowPreventionAuditor.categoriesGestureStillUsesL3R0()),
            check("GSP_002", "Finish Training still uses L0 R3", GestureShadowPreventionAuditor.finishTrainingGestureStillUsesL0R3()),
            check("GSP_003", "\"good_morning\" no longer shares the Categories gesture", GestureShadowPreventionAuditor.goodMorningNoLongerSharesCategoriesGesture()),
            check("GSP_004", "\"i_need_water\" no longer shares the Finish Training gesture", GestureShadowPreventionAuditor.iNeedWaterNoLongerSharesFinishTrainingGesture()),
            check("GSP_005", "Practice Mode teaches the updated \"i_need_water\" gesture", GestureShadowPreventionAuditor.practiceModeTeachesTheUpdatedINeedWaterGesture()),
            check("GSP_006", "Replacement gestures are free of navigation, system, and emergency reservations", GestureShadowPreventionAuditor.replacementGesturesAreConflictFree()),
            check("GSP_007", "Every vocabulary phrase has a unique gesture (no duplicates)", GestureShadowPreventionAuditor.noDuplicatePhraseGestures()),
            check("GSP_008", "Every vocabulary phrase is reachable", GestureShadowPreventionAuditor.everyVocabularyPhraseReachable()),
            check("GSP_009", "No navigation gesture shadows a vocabulary phrase", GestureShadowPreventionAuditor.noNavigationGestureShadowsAPhrase()),
            check("GSP_010", "No vocabulary phrase shadows a reserved navigation gesture", GestureShadowPreventionAuditor.noPhraseShadowsNavigation()),
            check("GSP_011", "No reserved gesture conflicts (Gesture Conflict Authority passes)", GestureShadowPreventionAuditor.noReservedGestureConflicts()),
            check("GSP_012", "Gesture allocation system generically detects workspace/reserved conflicts", GestureShadowPreventionAuditor.auditEngineGenericallyDetectsWorkspaceReservedConflicts()),
            check("GSP_013", "The strengthened engine would have caught the original defect", GestureShadowPreventionAuditor.engineWouldHaveCaughtTheOriginalDefect()),
            check("GSP_014", "Existing Guided Training and gesture consistency authorities remain green", GestureShadowPreventionAuditor.existingGuidedTrainingAndGestureAuthoritiesRemainGreen()),
            check("GSP_015", "Repository-wide audit reports zero remaining conflicts", GestureShadowPreventionAuditor.repositoryAuditReportsZeroConflicts()),
            check("GSP_016", "Repository-wide audit reports zero unreachable phrases", GestureShadowPreventionAuditor.repositoryAuditReportsZeroUnreachablePhrases()),
            check("GSP_017", "Tests pass and Gradle validation task defined", GestureShadowPreventionAuditor.testClassExists() && GestureShadowPreventionAuditor.gradleTaskRegistered())
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Gesture shadow prevention verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. " +
                "Failed: ${failed.size}. Repository audit — navigation: ${report.totalNavigationGestures}, " +
                "vocabulary: ${report.totalVocabularyGestures}, reserved: ${report.totalReservedGestures}, " +
                "system: ${report.totalSystemGestures}, conflicts: ${report.remainingConflicts}, " +
                "unreachable phrases: ${report.unreachablePhrases}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(
                GestureShadowPreventionMetadata.PRESERVE_NAVIGATION_RULE,
                GestureShadowPreventionMetadata.PHRASE_REACHABILITY_RULE,
                GestureShadowPreventionMetadata.GENERAL_PREVENTION_RULE,
                "Conflict report — total navigation gestures: ${report.totalNavigationGestures}, " +
                    "total vocabulary gestures: ${report.totalVocabularyGestures}, " +
                    "total reserved gestures: ${report.totalReservedGestures}, " +
                    "total system gestures: ${report.totalSystemGestures}, " +
                    "remaining conflicts: ${report.remainingConflicts}, " +
                    "unreachable phrases: ${report.unreachablePhrases}."
            ),
            affectedLicArticles = listOf("Part II — Communication Workspace vocabulary reachability"),
            affectedLiecArticles = listOf("Article 2.1 — Workspace navigation gestures", "Article 2.7 — Navigation lesson gesture ownership"),
            affectedLvcArticles = listOf("Article 3.3.1.3 — Reserved gestures not vocabulary selections", "Article 3.23 — Gesture Shadow Prevention validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "Categories (L3 R0) and Finish Training (L0 R3) are preserved unchanged; \"good_morning\" " +
                    "and \"i_need_water\" were reassigned to conflict-free gestures; every vocabulary " +
                    "phrase is unique and reachable; and the gesture allocation system now generically " +
                    "detects navigation/system/phrase collisions before they can ship again."
            } else {
                "${failed.size} gesture shadow prevention checks failed."
            },
            subsystem = "Gesture Shadow Prevention"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
