package com.idworx.lisa.features.eyediagnostic

/**
 * How a guided Eye Test step finished — used for failure-tolerant diagnosis.
 */
enum class EyeTestStepCompletion {
    Success,
    TimedOut,
    Skipped,
    NotCompletedDueToDetectionFailure
}

data class EyeTestStepRecord(
    val stepNumber: Int,
    val title: String,
    val completion: EyeTestStepCompletion,
    val targetDescription: String,
    val detectedDescription: String,
    val elapsedMs: Long,
    val acceptedSamples: Int,
    val rejectedSamples: Int,
    val nullLeftSamples: Int,
    val nullRightSamples: Int,
    val mostCommonRejectionReason: String?
) {
    fun reportLine(): String =
        "Step $stepNumber ($title): ${completion.name} — target=$targetDescription; " +
            "detected=$detectedDescription; elapsedMs=$elapsedMs; " +
            "accepted=$acceptedSamples rejected=$rejectedSamples; " +
            "nullL=$nullLeftSamples nullR=$nullRightSamples; " +
            "topReject=${mostCommonRejectionReason ?: "none"}"
}

enum class EyeTestSequenceOutcome {
    Success,
    Failed,
    NotCompleted
}
