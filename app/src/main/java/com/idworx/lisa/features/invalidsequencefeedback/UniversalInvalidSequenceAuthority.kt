package com.idworx.lisa.features.invalidsequencefeedback

import com.idworx.lisa.formatWinkSequenceShort
import com.idworx.lisa.features.intelligentstartup.authority.WelcomeEyeNavigationAuthority
import com.idworx.lisa.features.intelligentstartup.authority.WelcomeStage
import com.idworx.lisa.features.universalsequenceexecution.GuidedReadinessSequenceAuthority
import com.idworx.lisa.isEmergencySequence

/**
 * RC8.44 — shared invalid-sequence feedback for startup / guided-entry surfaces.
 *
 * Priority (callers must honour):
 * 1. Specialised lesson mismatch (Wrong eye / Wrong gesture) — handled elsewhere
 * 2. Global Emergency (L6 R0) — handled before this authority sees the sequence
 * 3. Current-page valid action — execute existing production path
 * 4. Completed unrelated sequence — show red warning; execute nothing
 *
 * Incomplete sequences never reach this authority (finalization already requires completion).
 */
object UniversalInvalidSequenceAuthority {

    /** Matches phrase-lesson wrong-eye clear timing for readable transient feedback. */
    const val WARNING_CLEAR_MS: Long = 2_500L

    const val PRIMARY_LINE: String = "Sequence not available here"
    const val PRIMARY_LINE_MULTI_PAGE: String = "Sequence not available on this page"

    enum class Surface {
        WelcomeIntroduction,
        WelcomeDestination,
        GuidedReadiness
    }

    data class ValidSequence(
        val left: Int,
        val right: Int,
        val label: String,
        /** Short purpose clause, e.g. "to continue" — used in the context line. */
        val purpose: String? = null
    )

    data class Warning(
        val primaryLine: String,
        val contextLine: String
    ) {
        fun displayLines(): List<String> = listOf(primaryLine, contextLine)
        fun combinedDisplay(): String = "$primaryLine\n$contextLine"
    }

    sealed class Decision {
        /** Sequence matches a visible action on this surface — caller executes existing path. */
        data object ExecuteValidAction : Decision()

        /** Completed sequence is unrelated — show warning; do not execute. */
        data class ShowInvalidWarning(val warning: Warning) : Decision()

        /** Not a surface this authority owns, or input should be ignored silently. */
        data object NotApplicable : Decision()
    }

    fun surfaceForWelcome(stage: WelcomeStage): Surface = when (stage) {
        WelcomeStage.BlinkSequenceIntroduction -> Surface.WelcomeIntroduction
        WelcomeStage.DestinationSelection -> Surface.WelcomeDestination
    }

    fun validSequences(surface: Surface): List<ValidSequence> = when (surface) {
        Surface.WelcomeIntroduction -> listOf(
            ValidSequence(
                left = WelcomeEyeNavigationAuthority.continueLeft,
                right = WelcomeEyeNavigationAuthority.continueRight,
                label = WelcomeEyeNavigationAuthority.continueSequenceLabel(),
                purpose = "to continue"
            )
        )
        Surface.WelcomeDestination -> listOf(
            ValidSequence(
                left = WelcomeEyeNavigationAuthority.startGuidedLearningLeft,
                right = WelcomeEyeNavigationAuthority.startGuidedLearningRight,
                label = WelcomeEyeNavigationAuthority.startGuidedLearningSequenceLabel(),
                purpose = null
            ),
            ValidSequence(
                left = WelcomeEyeNavigationAuthority.skipToCommunicationLeft,
                right = WelcomeEyeNavigationAuthority.skipToCommunicationRight,
                label = WelcomeEyeNavigationAuthority.skipToCommunicationSequenceLabel(),
                purpose = null
            ),
            ValidSequence(
                left = WelcomeEyeNavigationAuthority.backLeft,
                right = WelcomeEyeNavigationAuthority.backRight,
                label = WelcomeEyeNavigationAuthority.backSequenceLabel(),
                purpose = null
            )
        )
        Surface.GuidedReadiness -> listOf(
            ValidSequence(
                left = GuidedReadinessSequenceAuthority.CONTINUE_LEFT,
                right = GuidedReadinessSequenceAuthority.CONTINUE_RIGHT,
                label = GuidedReadinessSequenceAuthority.continueSequenceLabel(),
                purpose = "to continue"
            ),
            ValidSequence(
                left = GuidedReadinessSequenceAuthority.BACK_LEFT,
                right = GuidedReadinessSequenceAuthority.BACK_RIGHT,
                label = GuidedReadinessSequenceAuthority.backSequenceLabel(),
                purpose = "to go back"
            )
        )
    }

    fun buildWarning(surface: Surface): Warning {
        val valid = validSequences(surface)
        val primary = when (surface) {
            Surface.WelcomeDestination -> PRIMARY_LINE_MULTI_PAGE
            else -> PRIMARY_LINE
        }
        return Warning(primaryLine = primary, contextLine = contextLine(valid))
    }

    fun contextLine(valid: List<ValidSequence>): String {
        require(valid.isNotEmpty()) { "invalid-sequence warning requires at least one valid sequence" }
        return when {
            valid.size == 1 -> {
                val only = valid.first()
                val purpose = only.purpose
                if (purpose.isNullOrBlank()) "Use ${only.label}" else "Use ${only.label} $purpose"
            }
            valid.all { it.purpose.isNullOrBlank() } -> {
                val labels = valid.map { it.label }
                "Use " + labels.dropLast(1).joinToString(", ") + " or " + labels.last()
            }
            else -> {
                // e.g. "Use L1 R1 to continue or L2 R2 to go back"
                valid.joinToString(separator = " or ", prefix = "Use ") { seq ->
                    val purpose = seq.purpose
                    if (purpose.isNullOrBlank()) seq.label else "${seq.label} $purpose"
                }
            }
        }
    }

    /**
     * Evaluate a completed eye sequence for a startup / guided-entry surface.
     * Emergency must already have been handled by the caller (or will be before this runs).
     */
    fun evaluate(
        surface: Surface,
        left: Int,
        right: Int,
        @Suppress("UNUSED_PARAMETER") blinkOrder: List<Boolean> = emptyList(),
        matchesValidAction: Boolean
    ): Decision {
        if (isEmergencySequence(left, right)) {
            // Global emergency stays outside this authority.
            return Decision.NotApplicable
        }
        if (matchesValidAction) return Decision.ExecuteValidAction
        if (left + right < 2) return Decision.NotApplicable
        return Decision.ShowInvalidWarning(buildWarning(surface))
    }

    fun evaluateWelcome(
        stage: WelcomeStage,
        left: Int,
        right: Int,
        blinkOrder: List<Boolean>,
        matchesValidAction: Boolean
    ): Decision = evaluate(
        surface = surfaceForWelcome(stage),
        left = left,
        right = right,
        blinkOrder = blinkOrder,
        matchesValidAction = matchesValidAction
    )

    fun evaluateGuidedReadiness(
        left: Int,
        right: Int,
        blinkOrder: List<Boolean>,
        matchesValidAction: Boolean
    ): Decision = evaluate(
        surface = Surface.GuidedReadiness,
        left = left,
        right = right,
        blinkOrder = blinkOrder,
        matchesValidAction = matchesValidAction
    )

    fun labelOf(left: Int, right: Int): String = formatWinkSequenceShort(left, right)
}
