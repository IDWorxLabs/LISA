package com.idworx.lisa.features.accessibilityconsistency.engine

import com.idworx.lisa.defaultLanguageMappings
import com.idworx.lisa.features.accessibilityconsistency.model.AccessibilityScoreBand
import com.idworx.lisa.features.accessibilityconsistency.validators.TypographyValidator
import com.idworx.lisa.features.corecommunicationreliability.engine.CommunicationReliabilityContext
import com.idworx.lisa.features.corecommunicationreliability.engine.CoreCommunicationReliabilityEngines

object AccessibilityVerifier {

    fun verifyAuditProducesScore(): Boolean {
        val engine = AccessibilityConsistencyEngines.createForTests()
        val report = engine.generateReport()
        return report.score.overall in 0..100 &&
            report.score.band != AccessibilityScoreBand.Critical &&
            report.audit.checksPerformed > 0
    }

    fun verifyTypographyValidation(): Boolean {
        val result = TypographyValidator.validate()
        return result.checksPassed >= 3 && result.checksPerformed >= 4
    }

    fun verifyReportGeneration(): Boolean {
        val engine = AccessibilityConsistencyEngines.createForTests()
        val report = engine.generateReport()
        return report.summary.isNotBlank() &&
            report.evidenceSummary.isNotBlank() &&
            report.affectedScreens.isNotEmpty() || report.issues.isEmpty()
    }

    fun verifyNoCommunicationBehaviorChange(): Boolean {
        val ctx = CommunicationReliabilityContext(mappings = defaultLanguageMappings())
        val baseline = CoreCommunicationReliabilityEngines.createForTests()
        val observed = CoreCommunicationReliabilityEngines.createForTests()
        val before = baseline.evaluatePhrasePath(ctx, 2, 6)
        AccessibilityConsistencyEngines.createForTests().generateReport()
        val after = observed.evaluatePhrasePath(ctx, 2, 6)
        return before.finalOutcome == after.finalOutcome &&
            before.matchedPhraseId == after.matchedPhraseId
    }

    fun verifyExistingAccessibilityPreserved(): Boolean =
        com.idworx.lisa.features.accessibilityconsistency.validators.AccessibilityFileProbe
            .fileExists("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
}
