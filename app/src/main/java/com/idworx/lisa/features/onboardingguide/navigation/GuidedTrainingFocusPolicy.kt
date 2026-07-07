package com.idworx.lisa.features.onboardingguide.navigation

import com.idworx.lisa.features.onboardingguide.model.NavigationAction

/**
 * Reusable "one target at a time" focus policy for Guided Training's real-workspace navigation
 * lessons. Layered strictly ON TOP of MainActivity's existing gesture classification
 * (`classifyNavigationGesture`) and coarse lesson gate (`acceptedByCurrentNavigationLesson`) —
 * it never replaces either. Those two answer "is this the right KIND of action for the active
 * lesson"; this policy answers the narrower question they cannot: for lessons whose real
 * workspace screen shows more than one candidate row at once (Select Category, Select Phrase),
 * is THIS specific row the one the lesson highlighted, or an unrelated row that must stay inert.
 *
 * Never hardcoded to a specific lesson number, phrase, or category: driven purely by the active
 * lesson's expected [NavigationAction] plus a caller-supplied flag describing whether the
 * specific target being attempted is the one currently highlighted for that lesson. Every real
 * workspace entry point (blink gesture resolution AND touch) is expected to consult this same
 * policy so a learner can only ever act on the single highlighted target while a lesson is
 * focused, whichever input method they used.
 */
object GuidedTrainingFocusPolicy {

    /**
     * True when acting on [attemptedTarget] may proceed while [expected] is the lesson currently
     * being taught. [isAttemptedTargetHighlighted] only matters for lessons with several visible
     * candidates (Select Category / Select Phrase); every other lesson has exactly one on-screen
     * target so the flag defaults to true and is ignored.
     */
    fun isTargetAllowed(
        expected: NavigationAction,
        attemptedTarget: NavigationAction,
        isAttemptedTargetHighlighted: Boolean = true
    ): Boolean {
        if (attemptedTarget != expected) return false
        return when (attemptedTarget) {
            NavigationAction.SelectCategory, NavigationAction.SelectPhrase -> isAttemptedTargetHighlighted
            else -> true
        }
    }
}
