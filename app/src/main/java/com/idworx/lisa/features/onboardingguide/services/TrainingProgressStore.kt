package com.idworx.lisa.features.onboardingguide.services

import android.content.Context
import com.idworx.lisa.LisaProfileStore
import com.idworx.lisa.features.launchwelcomestatepriority.WelcomeStatePriorityGate
import com.idworx.lisa.features.onboardingguide.model.PracticeStatistics
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.model.TrainingPreferences
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import org.json.JSONArray
import org.json.JSONObject

class TrainingProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences(LisaProfileStore.PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): TrainingProgress {
        val completedRaw = prefs.getString(KEY_COMPLETED_LESSONS, null)
        val completedIds = if (completedRaw.isNullOrBlank()) {
            emptySet()
        } else {
            try {
                JSONArray(completedRaw).let { array ->
                    buildSet {
                        for (i in 0 until array.length()) {
                            add(array.getString(i))
                        }
                    }
                }
            } catch (_: Exception) {
                emptySet()
            }
        }
        val phase = resolvePhase(prefs)
        val loaded = TrainingProgress(
            tutorialStarted = prefs.getBoolean(KEY_STARTED, false),
            tutorialCompleted = prefs.getBoolean(KEY_COMPLETED, false),
            tutorialSkipped = prefs.getBoolean(KEY_SKIPPED, false),
            firstLaunchChoiceMade = prefs.getBoolean(KEY_FIRST_LAUNCH_CHOICE, false),
            certifiedCommunicator = prefs.getBoolean(KEY_CERTIFIED, false),
            calibrationCompleted = prefs.getBoolean(KEY_CALIBRATION_DONE, false),
            currentPhase = phase,
            communicationLessonIndex = prefs.getInt(KEY_COMM_INDEX, 0),
            masteryRoundIndex = prefs.getInt(KEY_MASTERY_INDEX, 0),
            masteryPhraseOrder = prefs.getString(KEY_MASTERY_ORDER, "") ?: "",
            navigationLessonIndex = prefs.getInt(KEY_NAV_INDEX, 0),
            completedLessonIds = completedIds,
            statistics = PracticeStatistics(
                totalAttempts = prefs.getInt(KEY_TOTAL_ATTEMPTS, 0),
                successfulAttempts = prefs.getInt(KEY_SUCCESS_ATTEMPTS, 0),
                consecutiveFailures = prefs.getInt(KEY_CONSECUTIVE_FAILURES, 0)
            ),
            preferences = TrainingPreferences(
                narrationEnabled = prefs.getBoolean(KEY_NARRATION_ENABLED, true),
                narrationSpeed = prefs.getFloat(KEY_NARRATION_SPEED, 0.9f),
                narrationVolume = prefs.getFloat(KEY_NARRATION_VOLUME, 1.0f),
                narrationLanguage = prefs.getString(KEY_NARRATION_LANGUAGE, "en") ?: "en"
            ),
            practiceModeOnly = prefs.getBoolean(KEY_PRACTICE_ONLY, false),
            practiceCommunication = prefs.getBoolean(KEY_PRACTICE_COMM, false),
            practiceNavigation = prefs.getBoolean(KEY_PRACTICE_NAV, false),
            currentLessonSuccessCount = prefs.getInt(KEY_LESSON_SUCCESS_COUNT, 0),
            sessionLessonsThisVisit = prefs.getInt(KEY_SESSION_LESSONS, 0)
        )
        return WelcomeStatePriorityGate.applyForColdLaunch(loaded)
    }

    private fun resolvePhase(prefs: android.content.SharedPreferences): TrainingPhase {
        if (!prefs.getBoolean(KEY_COMPLETED, false) &&
            !prefs.getBoolean(KEY_SKIPPED, false) &&
            !prefs.getBoolean(KEY_FIRST_LAUNCH_CHOICE, false)
        ) {
            return TrainingPhase.FirstLaunchChoice
        }
        val byName = prefs.getString(KEY_PHASE_NAME, null)
        if (!byName.isNullOrBlank()) {
            val phase = TrainingPhase.entries.find { it.name == byName }
                ?: defaultPhaseForLegacyUser(prefs)
            return enforceFirstLaunchChoice(phase, prefs)
        }
        return migrateLegacyPhaseOrdinal(prefs.getInt(KEY_PHASE, -1), prefs)
    }

    private fun enforceFirstLaunchChoice(
        phase: TrainingPhase,
        prefs: android.content.SharedPreferences
    ): TrainingPhase {
        if (prefs.getBoolean(KEY_COMPLETED, false) || prefs.getBoolean(KEY_SKIPPED, false)) {
            return phase
        }
        if (!prefs.getBoolean(KEY_FIRST_LAUNCH_CHOICE, false) &&
            phase != TrainingPhase.FirstLaunchChoice
        ) {
            return TrainingPhase.FirstLaunchChoice
        }
        return phase
    }

    private fun migrateLegacyPhaseOrdinal(ordinal: Int, prefs: android.content.SharedPreferences): TrainingPhase {
        if (ordinal < 0) return TrainingPhase.FirstLaunchChoice
        val legacy = listOf(
            TrainingPhase.Welcome,
            TrainingPhase.Setup,
            TrainingPhase.CommunicationLesson,
            TrainingPhase.NavigationLesson,
            TrainingPhase.Completion,
            TrainingPhase.SkipConfirm
        )
        if (ordinal in legacy.indices) {
            val mapped = legacy[ordinal]
            return enforceFirstLaunchChoice(mapped, prefs)
        }
        return defaultPhaseForLegacyUser(prefs)
    }

    private fun defaultPhaseForLegacyUser(prefs: android.content.SharedPreferences): TrainingPhase =
        when {
            prefs.getBoolean(KEY_COMPLETED, false) || prefs.getBoolean(KEY_SKIPPED, false) ->
                TrainingPhase.Completion
            !prefs.getBoolean(KEY_FIRST_LAUNCH_CHOICE, false) ->
                TrainingPhase.FirstLaunchChoice
            prefs.getBoolean(KEY_STARTED, false) -> TrainingPhase.Welcome
            else -> TrainingPhase.FirstLaunchChoice
        }

    fun save(progress: TrainingProgress) {
        val completedArray = JSONArray()
        progress.completedLessonIds.forEach { completedArray.put(it) }
        prefs.edit()
            .putBoolean(KEY_STARTED, progress.tutorialStarted)
            .putBoolean(KEY_COMPLETED, progress.tutorialCompleted)
            .putBoolean(KEY_SKIPPED, progress.tutorialSkipped)
            .putBoolean(KEY_FIRST_LAUNCH_CHOICE, progress.firstLaunchChoiceMade)
            .putBoolean(KEY_CERTIFIED, progress.certifiedCommunicator)
            .putBoolean(KEY_CALIBRATION_DONE, progress.calibrationCompleted)
            .putString(KEY_PHASE_NAME, progress.currentPhase.name)
            .putInt(KEY_PHASE, progress.currentPhase.ordinal)
            .putInt(KEY_COMM_INDEX, progress.communicationLessonIndex)
            .putInt(KEY_MASTERY_INDEX, progress.masteryRoundIndex)
            .putString(KEY_MASTERY_ORDER, progress.masteryPhraseOrder)
            .putInt(KEY_NAV_INDEX, progress.navigationLessonIndex)
            .putString(KEY_COMPLETED_LESSONS, completedArray.toString())
            .putInt(KEY_TOTAL_ATTEMPTS, progress.statistics.totalAttempts)
            .putInt(KEY_SUCCESS_ATTEMPTS, progress.statistics.successfulAttempts)
            .putInt(KEY_CONSECUTIVE_FAILURES, progress.statistics.consecutiveFailures)
            .putBoolean(KEY_NARRATION_ENABLED, progress.preferences.narrationEnabled)
            .putFloat(KEY_NARRATION_SPEED, progress.preferences.narrationSpeed)
            .putFloat(KEY_NARRATION_VOLUME, progress.preferences.narrationVolume)
            .putString(KEY_NARRATION_LANGUAGE, progress.preferences.narrationLanguage)
            .putBoolean(KEY_PRACTICE_ONLY, progress.practiceModeOnly)
            .putBoolean(KEY_PRACTICE_COMM, progress.practiceCommunication)
            .putBoolean(KEY_PRACTICE_NAV, progress.practiceNavigation)
            .putInt(KEY_LESSON_SUCCESS_COUNT, progress.currentLessonSuccessCount)
            .putInt(KEY_SESSION_LESSONS, progress.sessionLessonsThisVisit)
            .apply()
    }

    fun reset() {
        prefs.edit()
            .remove(KEY_STARTED)
            .remove(KEY_COMPLETED)
            .remove(KEY_SKIPPED)
            .remove(KEY_FIRST_LAUNCH_CHOICE)
            .remove(KEY_CERTIFIED)
            .remove(KEY_CALIBRATION_DONE)
            .remove(KEY_PHASE)
            .remove(KEY_PHASE_NAME)
            .remove(KEY_COMM_INDEX)
            .remove(KEY_MASTERY_INDEX)
            .remove(KEY_MASTERY_ORDER)
            .remove(KEY_NAV_INDEX)
            .remove(KEY_COMPLETED_LESSONS)
            .remove(KEY_TOTAL_ATTEMPTS)
            .remove(KEY_SUCCESS_ATTEMPTS)
            .remove(KEY_CONSECUTIVE_FAILURES)
            .remove(KEY_PRACTICE_ONLY)
            .remove(KEY_PRACTICE_COMM)
            .remove(KEY_PRACTICE_NAV)
            .remove(KEY_LESSON_SUCCESS_COUNT)
            .remove(KEY_SESSION_LESSONS)
            .apply()
    }

    companion object {
        private const val KEY_STARTED = "guided_training_started"
        private const val KEY_COMPLETED = "guided_training_completed"
        private const val KEY_SKIPPED = "guided_training_skipped"
        private const val KEY_FIRST_LAUNCH_CHOICE = "guided_training_first_launch_choice"
        private const val KEY_CERTIFIED = "guided_training_certified"
        private const val KEY_CALIBRATION_DONE = "guided_training_calibration_done"
        private const val KEY_PHASE = "guided_training_phase"
        private const val KEY_PHASE_NAME = "guided_training_phase_name"
        private const val KEY_COMM_INDEX = "guided_training_comm_index"
        private const val KEY_MASTERY_INDEX = "guided_training_mastery_index"
        private const val KEY_MASTERY_ORDER = "guided_training_mastery_order"
        private const val KEY_NAV_INDEX = "guided_training_nav_index"
        private const val KEY_COMPLETED_LESSONS = "guided_training_completed_lessons"
        private const val KEY_TOTAL_ATTEMPTS = "guided_training_total_attempts"
        private const val KEY_SUCCESS_ATTEMPTS = "guided_training_success_attempts"
        private const val KEY_CONSECUTIVE_FAILURES = "guided_training_consecutive_failures"
        private const val KEY_NARRATION_ENABLED = "guided_training_narration_enabled"
        private const val KEY_NARRATION_SPEED = "guided_training_narration_speed"
        private const val KEY_NARRATION_VOLUME = "guided_training_narration_volume"
        private const val KEY_NARRATION_LANGUAGE = "guided_training_narration_language"
        private const val KEY_PRACTICE_ONLY = "guided_training_practice_only"
        private const val KEY_PRACTICE_COMM = "guided_training_practice_comm"
        private const val KEY_PRACTICE_NAV = "guided_training_practice_nav"
        private const val KEY_LESSON_SUCCESS_COUNT = "guided_training_lesson_success_count"
        private const val KEY_SESSION_LESSONS = "guided_training_session_lessons"
    }
}
