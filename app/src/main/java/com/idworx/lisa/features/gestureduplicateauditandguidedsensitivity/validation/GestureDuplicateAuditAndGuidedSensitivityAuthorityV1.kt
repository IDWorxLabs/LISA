package com.idworx.lisa.features.gestureduplicateauditandguidedsensitivity.validation

import com.idworx.lisa.features.gestureduplicateauditandguidedsensitivity.audit.GestureDuplicateAuditAndGuidedSensitivityAuditor
import com.idworx.lisa.features.gestureduplicateauditandguidedsensitivity.metadata.GestureDuplicateAuditAndGuidedSensitivityMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object GestureDuplicateAuditAndGuidedSensitivityAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_GESTURE_DUPLICATE_AUDIT_AND_GUIDED_SENSITIVITY_V1"
    const val PASS_TOKEN: String = GestureDuplicateAuditAndGuidedSensitivityMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check("GDAGS_001", "Full gesture audit scans all catalogs", GestureDuplicateAuditAndGuidedSensitivityAuditor.auditEngineScansAllCatalogs()),
            check("GDAGS_002", "Guided Learning essentials have unique sequences", GestureDuplicateAuditAndGuidedSensitivityAuditor.guidedEssentialsHaveUniqueSequences()),
            check("GDAGS_003", "NO and PLEASE use distinct sequences", GestureDuplicateAuditAndGuidedSensitivityAuditor.noAndPleaseAreDistinct()),
            check("GDAGS_004", "No invalid duplicate among Guided Learning essentials", GestureDuplicateAuditAndGuidedSensitivityAuditor.noInvalidEssentialDuplicates()),
            check("GDAGS_005", "Valid contextual reuse documented in audit", GestureDuplicateAuditAndGuidedSensitivityAuditor.validContextualReuseDocumented()),
            check("GDAGS_006", "Reserved gesture conflicts detected and classified", GestureDuplicateAuditAndGuidedSensitivityAuditor.reservedConflictsDetected()),
            check("GDAGS_007", "Workspace visible-only resolver remains in codebase", GestureDuplicateAuditAndGuidedSensitivityAuditor.workspaceVisibleOnlyResolverIntact()),
            check("GDAGS_008", "Setup screen exposes sensitivity adjustment", GestureDuplicateAuditAndGuidedSensitivityAuditor.setupScreenHasSensitivityControl()),
            check("GDAGS_009", "Lesson screen exposes sensitivity adjustment", GestureDuplicateAuditAndGuidedSensitivityAuditor.lessonScreenHasSensitivityControl()),
            check("GDAGS_010", "Sensitivity changes apply to BlinkDetectionProcessor", GestureDuplicateAuditAndGuidedSensitivityAuditor.sensitivityWiredToChangeSensitivity()),
            check("GDAGS_011", "Sensitivity changes do not reset current lesson", GestureDuplicateAuditAndGuidedSensitivityAuditor.sensitivityDoesNotResetLesson()),
            check("GDAGS_012", "Essential phrase gesture mapping matches tuned spec", GestureDuplicateAuditAndGuidedSensitivityAuditor.essentialMappingMatchesSpec()),
            check("GDAGS_013", "Phrase-translation-only speech preserved", GestureDuplicateAuditAndGuidedSensitivityAuditor.phraseSpeechOnly()),
            check("GDAGS_014", "Tests pass and Gradle validation task defined", GestureDuplicateAuditAndGuidedSensitivityAuditor.testClassExists() && GestureDuplicateAuditAndGuidedSensitivityAuditor.gradleTaskRegistered())
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Gesture duplicate audit and Guided Learning sensitivity verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(
                GestureDuplicateAuditAndGuidedSensitivityMetadata.ESSENTIAL_GESTURE_RULE,
                GestureDuplicateAuditAndGuidedSensitivityMetadata.SENSITIVITY_RULE
            ),
            affectedLicArticles = listOf("Part II — Gesture uniqueness and Guided Learning tuning"),
            affectedLiecArticles = listOf("Article 2.6 — Gesture audit and sensitivity control"),
            affectedLvcArticles = listOf("Article 3.20 — Gesture Duplicate Audit validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "Guided Learning essentials are unique; NO/PLEASE fixed; sensitivity +/- available during training."
            } else {
                "${failed.size} gesture audit or sensitivity checks failed."
            },
            subsystem = "Gesture Duplicate Audit and Guided Sensitivity"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
