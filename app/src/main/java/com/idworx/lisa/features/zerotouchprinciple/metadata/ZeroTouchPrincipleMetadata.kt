package com.idworx.lisa.features.zerotouchprinciple.metadata

object ZeroTouchPrincipleMetadata {
    const val PRINCIPLE_NAME: String = "LISA Zero-Touch Principle"
    const val VERSION: String = "V1"
    const val CONSTITUTIONAL_PRIORITY: Int = 1

    const val CORE_RULE: String =
        "Every Brain 1 workflow must be completable using only eye movement, blink sequences, vision, and hearing."

    val FORBIDDEN_USER_BLAME_PHRASES: List<String> = listOf(
        "Incorrect",
        "Invalid",
        "Failed",
        "Retry",
        "You failed",
        "Wrong"
    )

    val FORBIDDEN_TOUCH_ASSUMPTIONS: List<String> = listOf(
        "Tap Continue",
        "Press Next",
        "Swipe to",
        "Tap the menu button",
        "Open Settings from the menu"
    )

    val REQUIRED_FIRST_CONVERSATION_ELEMENTS: List<String> = listOf(
        "Hello.",
        "My name is Lisa.",
        "using your eyes",
        "no rush",
        "cannot move your hands"
    )
}
