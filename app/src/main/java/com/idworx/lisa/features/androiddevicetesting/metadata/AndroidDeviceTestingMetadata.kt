package com.idworx.lisa.features.androiddevicetesting.metadata

object AndroidDeviceTestingMetadata {
    const val FEATURE_NAME: String = "LISA Android Device Testing Protocol"
    const val VERSION: String = "V1"
    const val PROTOCOL_VERSION: String = "1.0"

    val STANDARD_PHRASES: List<String> = listOf(
        "Hello",
        "Yes",
        "No",
        "I'm thirsty",
        "I need help",
        "I love you",
        "Call my caregiver",
        "Emergency phrase training only"
    )

    val LIGHTING_CONDITIONS: List<String> = listOf(
        "Bright indoor",
        "Dim indoor",
        "Side lighting",
        "Back lighting",
        "Low light",
        "Changing light during session"
    )

    val PHONE_POSITIONS: List<String> = listOf(
        "Centered",
        "Slightly left",
        "Slightly right",
        "Slightly high",
        "Slightly low",
        "Close",
        "Far",
        "Stable stand or surface"
    )

    val LONG_SESSION_DURATIONS_MIN: List<Int> = listOf(10, 20, 30)

    val FORBIDDEN_DEPENDENCY_MARKERS: List<String> = listOf(
        "Brain2", "OpenAI", "ChatGPT", "GenerativeModel", "LLM"
    )
}
