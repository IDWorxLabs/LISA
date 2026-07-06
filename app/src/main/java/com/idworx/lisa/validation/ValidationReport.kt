package com.idworx.lisa.validation

/**
 * Structured evidence report produced by a Validation Authority.
 * Reusable by all current and future LISA validation authorities.
 */
data class ValidationReport(
    val authorityName: String,
    val outcome: ValidationOutcome,
    val passToken: String?,
    val evidenceSummary: String,
    val checksPerformed: List<String>,
    val failedChecks: List<String>,
    val observations: List<String>,
    val affectedLicArticles: List<String>,
    val affectedLiecArticles: List<String>,
    val affectedLvcArticles: List<String>,
    val remediationGuidance: List<String>,
    val checkResults: List<ValidationCheckResult>,
    val rootCause: String? = null,
    val validationReasoning: String,
    val subsystem: String = "Guided Navigation"
) {
    fun formatReport(): String = buildString {
        appendLine("=== LISA Validation Report ===")
        appendLine("Authority: $authorityName")
        appendLine("Outcome: $outcome")
        if (passToken != null) appendLine("Pass Token: $passToken")
        appendLine("Subsystem: $subsystem")
        appendLine()
        appendLine("Evidence Summary:")
        appendLine(evidenceSummary)
        appendLine()
        appendLine("Validation Reasoning:")
        appendLine(validationReasoning)
        rootCause?.let {
            appendLine()
            appendLine("Root Cause:")
            appendLine(it)
        }
        appendLine()
        appendLine("Checks Performed (${checksPerformed.size}):")
        checksPerformed.forEach { appendLine("  - $it") }
        if (observations.isNotEmpty()) {
            appendLine()
            appendLine("Observations:")
            observations.forEach { appendLine("  - $it") }
        }
        if (failedChecks.isNotEmpty()) {
            appendLine()
            appendLine("Failed Checks:")
            failedChecks.forEach { appendLine("  - $it") }
        }
        appendLine()
        appendLine("Affected LIC Articles:")
        affectedLicArticles.forEach { appendLine("  - $it") }
        appendLine("Affected LIEC Articles:")
        affectedLiecArticles.forEach { appendLine("  - $it") }
        appendLine("Affected LVC Articles:")
        affectedLvcArticles.forEach { appendLine("  - $it") }
        if (remediationGuidance.isNotEmpty()) {
            appendLine()
            appendLine("Remediation Guidance:")
            remediationGuidance.forEach { appendLine("  - $it") }
        }
    }

    companion object {
        fun resolveOutcome(
            checkResults: List<ValidationCheckResult>,
            blocked: Boolean = false
        ): ValidationOutcome = when {
            blocked -> ValidationOutcome.BLOCKED
            checkResults.any { !it.passed } -> ValidationOutcome.FAIL
            checkResults.any { !it.observation.isNullOrBlank() } -> ValidationOutcome.PASS_WITH_OBSERVATIONS
            else -> ValidationOutcome.PASS
        }
    }
}
