package com.idworx.lisa.features.brain1interactionstandard.metadata

object Brain1InteractionStandardMetadata {
    const val STANDARD_NAME: String = "LISA Brain 1 Interaction Standard"
    const val VERSION: String = "V1"
    const val CONSTITUTIONAL_PRIORITY: Int = 2

    const val NO_SINGLE_BLINK_RULE: String =
        "Brain 1 shall never use a single blink as a complete interaction command. Minimum intentional command: two deliberate blinks."

    const val DECISION_MODEL: String =
        "Decision → Lisa repeats choice → User confirms (L1 R1) or cancels (R1 L1) → Action executes"

    val UNIVERSAL_GESTURES: Map<String, String> = mapOf(
        "L2" to "Option A / Primary choice (L2 R0)",
        "R2" to "Option B / Secondary choice (L0 R2)",
        "L1_R1" to "Confirm / Yes / Proceed (L1 R1 — left blink first)",
        "R1_L1" to "Cancel / Go back / Choose again (R1 L1 — right blink first)"
    )

    val PROGRESSIVE_LEVELS: List<String> = listOf(
        "Level 1 — Very Easy (2 blinks)",
        "Level 2 — Easy (3 blinks)",
        "Level 3 — Intermediate (4 blinks)",
        "Level 4 — Advanced (5 blinks)",
        "Level 5 — Expert (6+ blinks)"
    )
}
