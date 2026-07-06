package com.idworx.lisa.features.offlinereliability.metadata

object OfflineReliabilityMetadata {
    const val FEATURE_NAME: String = "LISA Offline Reliability Authority"
    const val VERSION: String = "V1"
    const val SCORE_EXCELLENT: Int = 90
    const val SCORE_GOOD: Int = 75
    const val SCORE_ACCEPTABLE: Int = 60
    const val SCORE_NEEDS_IMPROVEMENT: Int = 40

    val BRAIN1_FEATURE_PACKAGES: List<String> = listOf(
        "features/corecommunicationreliability",
        "features/calibrationreliability",
        "features/communicationanalytics",
        "features/accessibilityconsistency",
        "features/onboardingguide",
        "features/personality",
        "features/companionmemory"
    )

    val MANDATORY_NETWORK_PATTERNS: List<String> = listOf(
        "HttpURLConnection",
        "okhttp3.",
        "retrofit2.",
        "FirebaseApp",
        "FirebaseAuth",
        "FirebaseFirestore"
    )

    val OPTIONAL_CLOUD_MARKERS: List<String> = listOf(
        "TODO:",
        "isNetworkConnectionRequired",
        "VoiceSource.Cloud",
        "Future cloud"
    )
}
