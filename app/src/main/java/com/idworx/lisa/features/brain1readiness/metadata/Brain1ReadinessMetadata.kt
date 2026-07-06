package com.idworx.lisa.features.brain1readiness.metadata

object Brain1ReadinessMetadata {
    const val FEATURE_NAME: String = "LISA Brain 1 Readiness Review"
    const val VERSION: String = "V1"

    const val SCORE_READY: Int = 90
    const val SCORE_READY_WITH_WARNINGS: Int = 75
    const val SCORE_NOT_READY: Int = 50

    val BRAIN1_AUTHORITIES: List<String> = listOf(
        "GuidedTrainingAuthorityV1",
        "PersonalityEngineAuthorityV1",
        "CompanionMemoryAuthorityV1",
        "CoreCommunicationReliabilityAuthorityV1",
        "CalibrationReliabilityAuthorityV1",
        "CommunicationAccuracyAnalyticsAuthorityV1",
        "AccessibilityConsistencyAuthorityV1",
        "OfflineReliabilityAuthorityV1"
    )

    val FORBIDDEN_DEPENDENCY_MARKERS: List<String> = listOf(
        "Brain2",
        "OpenAI",
        "ChatGPT",
        "GenerativeModel",
        "LLM",
        "FirebaseAuth",
        "FirebaseFirestore"
    )

    val STANDARD_DEVICE_TESTING_GAPS: List<String> = listOf(
        "Real Android device testing still needed",
        "Different lighting environments still untested",
        "Different phone positions still untested",
        "Long-session fatigue still untested",
        "Caregiver setup workflow still needs testing",
        "TTS voice availability varies by device",
        "Emergency workflow needs real-world simulation"
    )
}
