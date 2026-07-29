package com.idworx.lisa.features.guidedsensitivitylesson

import com.idworx.lisa.features.onboardingguide.metadata.TrainingMetadata
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress

/**
 * RC8.28 — migrate saved guided progress from the previous 32-lesson catalogue
 * (17 navigation lessons including Explore) to the shortened 23-lesson catalogue
 * (8 navigation lessons ending in Adjust Sensitivity).
 *
 * Rules:
 * - Lessons 1–22 (nav indices 0–6) keep their semantic position.
 * - Old Lesson 23 (`nav_reset`) and old Lessons 24–32 (Explore) map to new Lesson 23
 *   (`nav_adjust_sensitivity`, nav index 7) when still in NavigationLesson.
 * - Already-completed / certified training stays completed.
 * - Invalid nav indices are clamped; never crash.
 */
object GuidedCatalogueMigrationRc828 {

    /** Navigation lesson count before RC8.28 (workspace 8 + Explore 9). */
    const val LEGACY_NAVIGATION_LESSON_COUNT: Int = 17

    /** First nav index that belonged to the removed Explore / Reset block. */
    const val LEGACY_RESET_OR_EXPLORE_START_INDEX: Int = 7

    fun migrate(progress: TrainingProgress): TrainingProgress {
        if (progress.tutorialCompleted || progress.tutorialSkipped || progress.certifiedCommunicator) {
            return progress.copy(
                navigationLessonIndex = progress.navigationLessonIndex
                    .coerceIn(0, (TrainingMetadata.NAVIGATION_LESSON_COUNT - 1).coerceAtLeast(0)),
                currentPhase = if (progress.tutorialCompleted || progress.certifiedCommunicator) {
                    TrainingPhase.Completion
                } else {
                    progress.currentPhase
                }
            )
        }
        if (progress.currentPhase != TrainingPhase.NavigationLesson) {
            return progress.copy(
                navigationLessonIndex = progress.navigationLessonIndex.coerceAtLeast(0)
            )
        }
        val navCount = TrainingMetadata.NAVIGATION_LESSON_COUNT
        val idx = progress.navigationLessonIndex
        val migratedIndex = when {
            idx < 0 -> 0
            idx < LEGACY_RESET_OR_EXPLORE_START_INDEX -> idx
            // Old nav_reset (7) and Explore (8–16) → final Adjust Sensitivity lesson.
            idx >= LEGACY_RESET_OR_EXPLORE_START_INDEX -> (navCount - 1).coerceAtLeast(0)
            else -> idx.coerceIn(0, navCount - 1)
        }
        return progress.copy(navigationLessonIndex = migratedIndex)
    }
}
