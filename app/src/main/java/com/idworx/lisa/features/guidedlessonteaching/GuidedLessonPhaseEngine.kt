package com.idworx.lisa.features.guidedlessonteaching

/**
 * RC8.23 / RC8.24 — Result of completing the current practical phase within a multi-phase lesson.
 */
sealed class GuidedLessonPhaseAdvanceResult {
    /**
     * Intermediate phase finished. Show feedback when [showCompletionFeedback] is true; after
     * the delay (or immediately when silent), advance [nextPhaseIndex] and optionally reset.
     */
    data class IntermediatePhaseCompleted(
        val completedPhase: GuidedLessonTeachingPhase,
        val nextPhaseIndex: Int,
        val resetWorkspaceBeforeNextPhase: Boolean,
        val showCompletionFeedback: Boolean
    ) : GuidedLessonPhaseAdvanceResult()

    /** Final phase finished — caller should complete the catalog lesson normally. */
    data class FinalPhaseCompleted(
        val completedPhase: GuidedLessonTeachingPhase
    ) : GuidedLessonPhaseAdvanceResult()

    /** Lesson has no phases, or phase index is invalid — treat as a normal single-step verify. */
    data object SingleStepLesson : GuidedLessonPhaseAdvanceResult()
}

/**
 * RC8.23 / RC8.24 — Pure progression helper for multi-phase guided lessons.
 *
 * Keeps phase indexing out of production navigation logic so future lessons 16–32 can reuse
 * sequential practical exercises without splitting catalog entries.
 */
object GuidedLessonPhaseEngine {

    fun advanceResult(
        presentation: GuidedLessonTeachingPresentation,
        currentPhaseIndex: Int
    ): GuidedLessonPhaseAdvanceResult {
        val phases = presentation.phases
        if (phases.isEmpty()) return GuidedLessonPhaseAdvanceResult.SingleStepLesson
        val index = currentPhaseIndex.coerceIn(0, phases.lastIndex)
        val phase = phases[index]
        return if (index >= phases.lastIndex) {
            GuidedLessonPhaseAdvanceResult.FinalPhaseCompleted(phase)
        } else {
            GuidedLessonPhaseAdvanceResult.IntermediatePhaseCompleted(
                completedPhase = phase,
                nextPhaseIndex = index + 1,
                resetWorkspaceBeforeNextPhase = phase.resetWorkspaceBeforeNextPhase,
                showCompletionFeedback = phase.showCompletionFeedback
            )
        }
    }

    fun clampPhaseIndex(presentation: GuidedLessonTeachingPresentation, phaseIndex: Int): Int {
        if (presentation.phases.isEmpty()) return 0
        return phaseIndex.coerceIn(0, presentation.phases.lastIndex)
    }
}
