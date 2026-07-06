package com.idworx.lisa.features.onboardingguide.ui

/** Plain-language gesture labels for Guided Learning — no L/R technical notation on screen. */
fun formatWinkGestureFriendly(left: Int, right: Int): String {
    fun times(n: Int): String = when (n) {
        1 -> "Once"
        2 -> "Twice"
        3 -> "Three Times"
        else -> "$n Times"
    }
    return when {
        left > 0 && right == 0 -> "Blink Left ${times(left)}"
        right > 0 && left == 0 -> "Blink Right ${times(right)}"
        left > 0 && right > 0 -> {
            val parts = buildList {
                add("Left ${times(left)}")
                add("Right ${times(right)}")
            }
            "Blink ${parts.joinToString(", then ")}"
        }
        else -> "Blink when you're ready"
    }
}

fun formatWinkInstruction(left: Int, right: Int): String {
    val gesture = formatWinkGestureFriendly(left, right).lowercase()
    return "When you're ready, $gesture."
}
