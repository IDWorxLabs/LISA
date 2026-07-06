package com.idworx.lisa.features.guidedlearningsetupbeforehello.validation

import com.idworx.lisa.features.guidedlearningsetupbeforehello.audit.GuidedLearningSetupBeforeHelloAuditor
import com.idworx.lisa.features.guidedlearningsetupbeforehello.metadata.GuidedLearningSetupBeforeHelloMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object GuidedLearningSetupBeforeHelloAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_GUIDED_LEARNING_SETUP_BEFORE_HELLO_V1"
    const val PASS_TOKEN: String = GuidedLearningSetupBeforeHelloMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check("GLSBH_001", "BeginLearning routes to setup not HELLO", GuidedLearningSetupBeforeHelloAuditor.beginLearningRoutesToSetupNotHello()),
            check("GLSBH_002", "Face detect does not auto-complete setup to HELLO", GuidedLearningSetupBeforeHelloAuditor.faceDetectDoesNotAutoCompleteSetup()),
            check("GLSBH_003", "Setup screen shows camera/face/eye status", GuidedLearningSetupBeforeHelloAuditor.setupScreenHasEyeStatus()),
            check("GLSBH_004", "Ready step Continue routes to HELLO via CompleteSetup", GuidedLearningSetupBeforeHelloAuditor.completeSetupRoutesToHello()),
            check("GLSBH_005", "Continue requires eyes ready on setup step 2", GuidedLearningSetupBeforeHelloAuditor.readyStepRequiresContinue()),
            check("GLSBH_006", "HELLO lesson shows eye-tracking indicator", GuidedLearningSetupBeforeHelloAuditor.lessonHasEyeIndicator()),
            check("GLSBH_007", "Camera remains composed during training UI", GuidedLearningSetupBeforeHelloAuditor.cameraComposedDuringTraining()),
            check("GLSBH_008", "Setup step constants and ready-check helper defined", GuidedLearningSetupBeforeHelloAuditor.setupStepConstantsDefined()),
            check("GLSBH_009", "Voice policy remains phrase-translation-only", GuidedLearningSetupBeforeHelloAuditor.phraseTranslationOnly()),
            check("GLSBH_010", "Tests pass and Gradle validation task defined", GuidedLearningSetupBeforeHelloAuditor.testClassExists() && GuidedLearningSetupBeforeHelloAuditor.gradleTaskRegistered())
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Guided Learning setup-before-HELLO verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(
                GuidedLearningSetupBeforeHelloMetadata.FLOW_RULE,
                GuidedLearningSetupBeforeHelloMetadata.NO_AUTO_HELLO
            ),
            affectedLicArticles = listOf("Part II — Guided Learning start flow"),
            affectedLiecArticles = listOf("Article 2.3 — Setup before HELLO teaching"),
            affectedLvcArticles = listOf("Article 3.17 — Guided Learning Setup Before HELLO validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "Start Guided Learning enters visual setup; Continue on ready check opens HELLO; eye status visible during setup and lessons."
            } else {
                "${failed.size} Guided Learning setup-before-HELLO checks failed."
            },
            subsystem = "Guided Learning Setup Before HELLO"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
