package com.idworx.lisa.features.accessibilityconsistency.model

enum class AccessibilitySeverity {
    Info,
    Warning,
    Error,
    Critical
}

enum class AccessibilityCategory {
    Typography,
    TouchTarget,
    Contrast,
    Layout,
    Navigation,
    GuidedLearning,
    Communication,
    Emergency,
    Settings,
    ScreenConsistency,
    CognitiveLoad
}

enum class AccessibilityScoreBand {
    Excellent,
    Good,
    Acceptable,
    NeedsImprovement,
    Critical;

    companion object {
        fun fromScore(score: Int): AccessibilityScoreBand = when {
            score >= com.idworx.lisa.features.accessibilityconsistency.metadata.AccessibilityMetadata.SCORE_EXCELLENT -> Excellent
            score >= com.idworx.lisa.features.accessibilityconsistency.metadata.AccessibilityMetadata.SCORE_GOOD -> Good
            score >= com.idworx.lisa.features.accessibilityconsistency.metadata.AccessibilityMetadata.SCORE_ACCEPTABLE -> Acceptable
            score >= com.idworx.lisa.features.accessibilityconsistency.metadata.AccessibilityMetadata.SCORE_NEEDS_IMPROVEMENT -> NeedsImprovement
            else -> Critical
        }
    }
}

data class AccessibilityIssue(
    val issueId: String,
    val category: AccessibilityCategory,
    val severity: AccessibilitySeverity,
    val screen: String,
    val description: String,
    val evidence: String
)

data class AccessibilityRecommendation(
    val recommendationId: String,
    val category: AccessibilityCategory,
    val message: String,
    val evidence: String
)

data class AccessibilityMetrics(
    val typographyChecksPassed: Int,
    val touchTargetChecksPassed: Int,
    val navigationChecksPassed: Int,
    val totalChecks: Int,
    val totalIssues: Int,
    val criticalIssues: Int,
    val evidence: String
)

data class AccessibilityScore(
    val overall: Int,
    val band: AccessibilityScoreBand,
    val metrics: AccessibilityMetrics,
    val evidence: String
)

data class AccessibilityAudit(
    val auditId: String,
    val timestampMs: Long,
    val issues: List<AccessibilityIssue>,
    val score: AccessibilityScore,
    val checksPerformed: Int,
    val checksPassed: Int
)

data class AccessibilityReport(
    val reportId: String,
    val generatedAtMs: Long,
    val audit: AccessibilityAudit,
    val score: AccessibilityScore,
    val issues: List<AccessibilityIssue>,
    val recommendations: List<AccessibilityRecommendation>,
    val warnings: List<String>,
    val summary: String,
    val affectedScreens: List<String>,
    val evidenceSummary: String
)
