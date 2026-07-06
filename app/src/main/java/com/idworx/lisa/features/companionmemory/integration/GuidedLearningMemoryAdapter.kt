package com.idworx.lisa.features.companionmemory.integration

import com.idworx.lisa.features.companionmemory.engine.CompanionMemoryEngine
import com.idworx.lisa.features.companionmemory.engine.DefaultCompanionMemoryEngine
import com.idworx.lisa.features.companionmemory.model.LearningHistoryEntry
import com.idworx.lisa.features.companionmemory.model.LearningMilestone
import com.idworx.lisa.features.companionmemory.model.MemoryCategory
import com.idworx.lisa.features.companionmemory.model.MemoryEvent
import com.idworx.lisa.features.companionmemory.model.MemoryImportance
import com.idworx.lisa.features.companionmemory.model.PreferenceMemorySnapshot
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.metadata.TrainingMetadata
import com.idworx.lisa.features.onboardingguide.model.TrainingPreferences

object GuidedLearningMemoryAdapter {

    fun onTutorialStarted(engine: CompanionMemoryEngine) {
        engine.recordEvent(
            MemoryEvent("tutorial_started", evidence = "User began LISA Learning Journey"),
            MemoryCategory.Learning,
            MemoryImportance.Medium,
            "Learning journey started",
            "LISA Learning Journey was started"
        )
    }

    fun onMetLisa(engine: CompanionMemoryEngine) {
        engine.recordMilestone(
            LearningMilestone.MetLisa,
            "User met Lisa during Learning Journey Stage 1"
        )
    }

    fun onTutorialSkipped(engine: CompanionMemoryEngine) {
        engine.recordEvent(
            MemoryEvent("tutorial_skipped", evidence = "Caregiver skipped Learning Journey to Communication Workspace"),
            MemoryCategory.Learning,
            MemoryImportance.Medium,
            "Learning journey skipped",
            "Learning Journey was skipped on first install"
        )
    }

    fun onTutorialResumed(engine: CompanionMemoryEngine) {
        engine.recordEvent(
            MemoryEvent("tutorial_resumed", evidence = "User resumed Learning Journey"),
            MemoryCategory.Learning,
            MemoryImportance.Low,
            "Learning journey resumed",
            "Learning Journey was resumed"
        )
    }

    fun onTutorialReset(engine: CompanionMemoryEngine) {
        engine.clearLearningMemory()
        engine.recordEvent(
            MemoryEvent("tutorial_reset", evidence = "User reset Learning Journey progress"),
            MemoryCategory.Learning,
            MemoryImportance.Medium,
            "Learning journey reset",
            "Learning Journey progress was reset"
        )
    }

    fun onCalibrationComplete(engine: CompanionMemoryEngine) {
        engine.recordMilestone(
            LearningMilestone.CalibrationComplete,
            "Calibration completed during Learning Journey Stage 3"
        )
    }

    fun onLessonCompleted(engine: CompanionMemoryEngine, lessonId: String, phase: String, attemptCount: Int) {
        (engine as? DefaultCompanionMemoryEngine)?.recordLearningEntry(
            LearningHistoryEntry(
                lessonId = lessonId,
                phase = phase,
                completedAtMs = System.currentTimeMillis(),
                attemptCount = attemptCount
            )
        )
        (engine as? DefaultCompanionMemoryEngine)?.incrementSessionLessonCompleted()
        engine.recordEvent(
            MemoryEvent(
                "lesson_completed",
                evidence = "Completed lesson $lessonId in phase $phase after $attemptCount attempts"
            ),
            MemoryCategory.Learning,
            MemoryImportance.Medium,
            "Lesson completed",
            "Completed lesson $lessonId"
        )
    }

    fun onFirstSuccessfulBlink(engine: CompanionMemoryEngine) {
        engine.recordMilestone(
            LearningMilestone.FirstSuccessfulBlink,
            "First successful blink sequence detected during Learning Journey"
        )
    }

    fun onFirstSpokenPhrase(engine: CompanionMemoryEngine, lessonId: String) {
        engine.recordMilestone(
            LearningMilestone.FirstPhrase,
            "First spoken phrase completed in lesson $lessonId"
        )
        (engine as? DefaultCompanionMemoryEngine)?.incrementSessionCommunicationSuccess()
    }

    fun onTenPhrases(engine: CompanionMemoryEngine) {
        engine.recordMilestone(
            LearningMilestone.TenPhrases,
            "Ten communication phrases completed during Learning Journey"
        )
    }

    fun onFundamentalsComplete(engine: CompanionMemoryEngine) {
        engine.recordMilestone(
            LearningMilestone.CommunicationFundamentalsComplete,
            "Communication Fundamentals stage completed"
        )
        engine.recordMilestone(
            LearningMilestone.CommunicationCompleted,
            "All fundamental communication phrases completed"
        )
    }

    fun onMasteryComplete(engine: CompanionMemoryEngine) {
        engine.recordMilestone(
            LearningMilestone.CommunicationMasteryComplete,
            "Communication Mastery stage completed"
        )
    }

    fun onCommunicationTrainingCompleted(engine: CompanionMemoryEngine) {
        onFundamentalsComplete(engine)
    }

    fun onNavigationTrainingCompleted(engine: CompanionMemoryEngine) {
        engine.recordMilestone(
            LearningMilestone.WorkspaceNavigationComplete,
            "Communication Workspace navigation completed"
        )
        engine.recordMilestone(
            LearningMilestone.NavigationCompleted,
            "All workspace navigation lessons completed"
        )
        (engine as? DefaultCompanionMemoryEngine)?.incrementSessionNavigationCompleted()
    }

    fun onEmergencyTrainingCompleted(engine: CompanionMemoryEngine) {
        engine.recordMilestone(
            LearningMilestone.EmergencyTrainingCompleted,
            "Emergency navigation practiced safely during Learning Journey"
        )
    }

    fun onCertification(engine: CompanionMemoryEngine) {
        engine.recordMilestone(
            LearningMilestone.LisaCertifiedCommunicator,
            "User earned LISA Certified Communicator certification"
        )
        engine.recordMilestone(
            LearningMilestone.GraduationCompleted,
            "User completed Learning Journey graduation"
        )
        engine.recordMilestone(
            LearningMilestone.GuidedLearningCompleted,
            "User completed entire LISA Learning Journey"
        )
    }

    fun onGraduation(engine: CompanionMemoryEngine) = onCertification(engine)

    fun syncPreferences(engine: CompanionMemoryEngine, preferences: TrainingPreferences) {
        engine.recordPreference(
            PreferenceMemorySnapshot(
                narrationEnabled = preferences.narrationEnabled,
                narrationSpeed = preferences.narrationSpeed,
                narrationVolume = preferences.narrationVolume,
                preferredLanguage = preferences.narrationLanguage
            )
        )
    }

    fun checkPhaseCompletions(engine: CompanionMemoryEngine, completedLessonIds: Set<String>) {
        val commIds = TrainingLessonCatalog.communicationFundamentals.map { it.id }.toSet()
        val commCompleted = commIds.count { completedLessonIds.contains(it) }
        if (commCompleted >= 10) {
            if (LearningMilestone.TenPhrases !in engine.getMilestones()) {
                onTenPhrases(engine)
            }
        }
        if (commIds.isNotEmpty() && completedLessonIds.containsAll(commIds)) {
            if (LearningMilestone.CommunicationFundamentalsComplete !in engine.getMilestones()) {
                onFundamentalsComplete(engine)
            }
        }
        val navIds = TrainingLessonCatalog.navigationLessons.map { it.id }.toSet()
        if (navIds.isNotEmpty() && completedLessonIds.containsAll(navIds)) {
            onNavigationTrainingCompleted(engine)
        }
    }
}
