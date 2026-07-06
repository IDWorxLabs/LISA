package com.idworx.lisa.features.brain1interactionstandard.model

/**
 * Order-sensitive blink matching (L then R vs R then L).
 * Required when counts alone cannot distinguish gestures (e.g. L1 R1 vs R1 L1).
 */
object BlinkSequenceOrder {

    /** True = left blink, false = right blink. */
    fun isLeftThenRight(order: List<Boolean>): Boolean =
        order.size == 2 && order[0] && !order[1]

    fun isRightThenLeft(order: List<Boolean>): Boolean =
        order.size == 2 && !order[0] && order[1]

    fun matches(order: List<Boolean>, required: String?): Boolean {
        if (required.isNullOrBlank()) return true
        if (order.size != required.length) return false
        return order.indices.all { i ->
            val expectLeft = required[i].uppercaseChar() == 'L'
            order[i] == expectLeft
        }
    }

    fun label(order: List<Boolean>): String {
        val left = order.count { it }
        val right = order.size - left
        return if (order.size == 2) {
            when {
                isLeftThenRight(order) -> "L1 R1"
                isRightThenLeft(order) -> "R1 L1"
                else -> "L$left R$right"
            }
        } else {
            "L$left R$right"
        }
    }
}
