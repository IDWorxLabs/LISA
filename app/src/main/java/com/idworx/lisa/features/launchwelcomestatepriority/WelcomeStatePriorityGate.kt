package com.idworx.lisa.features.launchwelcomestatepriority

import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress

/**
 * Hard startup priority: Welcome to Lisa wins on every cold launch.
 * Saved lesson, workspace, skip, or completion state must not restore the first screen.
 */
object WelcomeStatePriorityGate {

    /** Every process start shows Welcome — persisted phase never wins. */
    fun mustShowWelcome(@Suppress("UNUSED_PARAMETER") progress: TrainingProgress): Boolean = true

    fun applyForColdLaunch(progress: TrainingProgress): TrainingProgress =
        progress.copy(
            currentPhase = TrainingPhase.FirstLaunchChoice,
            firstLaunchChoiceMade = false
        )

    fun apply(progress: TrainingProgress): TrainingProgress = applyForColdLaunch(progress)

    /** Cold-launch gate is in-memory only; do not rewrite persisted progress on load. */
    fun requiresPersistMigration(@Suppress("UNUSED_PARAMETER") before: TrainingProgress, @Suppress("UNUSED_PARAMETER") after: TrainingProgress): Boolean = false
}
