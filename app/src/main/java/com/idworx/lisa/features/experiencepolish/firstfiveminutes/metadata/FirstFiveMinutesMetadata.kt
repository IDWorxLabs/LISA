package com.idworx.lisa.features.experiencepolish.firstfiveminutes.metadata

object FirstFiveMinutesMetadata {
    const val VERSION: String = "V1"
    const val PASS_TOKEN: String = "LISA_EXPERIENCE_PHASE_A_FIRST_FIVE_MINUTES_V1_PASS"
    const val GRADLE_TASK: String = "validateLisaExperiencePhaseAFirstFiveMinutesV1"

    const val CANCEL_GESTURE: String = "R1 L1 only — no L2 R2 cancel in Brain 1 interaction commands"

    val FORBIDDEN_PUNITIVE_WORDS: List<String> = listOf(
        "Incorrect",
        "Invalid",
        "Failed",
        "Retry"
    )

    val FIRST_FOUR_PHRASES: List<String> = listOf(
        "HELLO = L2",
        "YES = R2",
        "NO = L1 R1",
        "PLEASE = L1 R2",
        "THANK YOU = L3 R1",
        "WATER = L1 R3"
    )
}
