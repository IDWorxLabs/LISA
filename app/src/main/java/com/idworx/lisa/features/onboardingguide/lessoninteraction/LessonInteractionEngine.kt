package com.idworx.lisa.features.onboardingguide.lessoninteraction

import com.idworx.lisa.features.brain1interactionstandard.model.BlinkSequenceOrder
import com.idworx.lisa.features.onboardingguide.model.CommunicationLesson

object LessonInteractionEngine {

    fun successVisualMessage(lessonIndex: Int): String =
        if (lessonIndex % 2 == 0) "Well done" else "You did it"

    fun retryVisualMessage(failureCount: Int): String =
        if (failureCount % 2 == 0) "Try again" else "That blink was not part of this one"

    fun expectedSideOrder(lesson: CommunicationLesson): List<Boolean>? {
        val required = lesson.blinkOrder ?: return null
        if (required.isBlank()) return null
        return required.map { it.uppercaseChar() == 'L' }
    }

    fun isValidPartial(
        left: Int,
        right: Int,
        blinkOrder: List<Boolean>,
        lesson: CommunicationLesson
    ): Boolean {
        if (left > lesson.left || right > lesson.right) return false
        val expected = expectedSideOrder(lesson)
        if (expected != null) {
            if (blinkOrder.size > expected.size) return false
            return blinkOrder.indices.all { blinkOrder[it] == expected[it] }
        }
        return true
    }

    fun progressLabel(
        left: Int,
        right: Int,
        blinkOrder: List<Boolean>,
        lesson: CommunicationLesson
    ): String? {
        if (left == 0 && right == 0) return null
        val expected = expectedSideOrder(lesson)
        if (expected != null && blinkOrder.isNotEmpty()) {
            val side = blinkOrder.last()
            val step = blinkOrder.size
            return if (side) {
                "Left blink $step of 1"
            } else {
                "Right blink $step of 1"
            }
        }
        if (lesson.left > 0 && lesson.right == 0 && left > 0) {
            return "Left blink $left of ${lesson.left}"
        }
        if (lesson.right > 0 && lesson.left == 0 && right > 0) {
            return "Right blink $right of ${lesson.right}"
        }
        if (blinkOrder.isNotEmpty()) {
            val side = blinkOrder.last()
            val count = if (side) left else right
            val total = if (side) lesson.left else lesson.right
            if (total > 0) {
                return "${if (side) "Left" else "Right"} blink $count of $total"
            }
        }
        return BlinkSequenceOrder.label(blinkOrder)
    }

    fun lessonMatchesGesture(
        lesson: CommunicationLesson,
        left: Int,
        right: Int,
        blinkOrder: List<Boolean>
    ): Boolean =
        lesson.left == left &&
            lesson.right == right &&
            BlinkSequenceOrder.matches(blinkOrder, lesson.blinkOrder)
}
