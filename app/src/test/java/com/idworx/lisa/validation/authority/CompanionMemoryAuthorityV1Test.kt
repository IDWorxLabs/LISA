package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.companionmemory.engine.CompanionMemoryEngines
import com.idworx.lisa.features.companionmemory.engine.DefaultCompanionMemoryEngine
import com.idworx.lisa.features.companionmemory.engine.MemoryPrivacyGuard
import com.idworx.lisa.features.companionmemory.integration.GuidedLearningMemoryAdapter
import com.idworx.lisa.features.companionmemory.integration.PersonalityMemoryAdapter
import com.idworx.lisa.features.companionmemory.integration.PracticeMemoryAdapter
import com.idworx.lisa.features.companionmemory.model.GreetingScenario
import com.idworx.lisa.features.companionmemory.model.LearningMilestone
import com.idworx.lisa.features.companionmemory.model.MemoryCategory
import com.idworx.lisa.features.companionmemory.model.MemoryEvent
import com.idworx.lisa.features.companionmemory.model.MemoryImportance
import com.idworx.lisa.features.companionmemory.model.PreferenceMemorySnapshot
import com.idworx.lisa.features.companionmemory.repository.InMemoryCompanionMemoryRepository
import com.idworx.lisa.features.companionmemory.validation.CompanionMemoryAuthorityV1
import com.idworx.lisa.features.onboardingguide.model.TrainingPreferences
import com.idworx.lisa.features.personality.model.DialogueContext
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CompanionMemoryAuthorityV1Test {

    private lateinit var engine: DefaultCompanionMemoryEngine

    @Before
    fun setUp() {
        engine = CompanionMemoryEngines.createForTests(InMemoryCompanionMemoryRepository())
    }

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = CompanionMemoryAuthorityV1.validate()

        assertEquals(ValidationOutcome.PASS, report.outcome)
        assertEquals(CompanionMemoryAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        assertTrue(report.checkResults.all { it.passed })

        println(report.formatReport())
        println(CompanionMemoryAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun firstLaunch_recordsFirstLaunchMemory() {
        engine.startSession()
        val greeting = engine.getGreetingContext()
        assertTrue(greeting.isFirstLaunch)
        assertEquals(GreetingScenario.FirstLaunch, greeting.scenario)
    }

    @Test
    fun returningUser_afterSessionEnd() {
        engine.startSession()
        engine.endSession()
        val returning = engine.getGreetingContext()
        assertTrue(returning.returningUser)
        assertFalse(returning.isFirstLaunch)
    }

    @Test
    fun firstBlinkMilestone_recordedOnce() {
        val first = engine.recordMilestone(
            LearningMilestone.FirstSuccessfulBlink,
            "Blink sequence detected in lesson comm_1"
        )
        val second = engine.recordMilestone(
            LearningMilestone.FirstSuccessfulBlink,
            "Blink sequence detected again"
        )
        assertNotNull(first)
        assertNull(second)
        assertTrue(LearningMilestone.FirstSuccessfulBlink in engine.getMilestones())
    }

    @Test
    fun firstPhraseMilestone_recordedOnce() {
        GuidedLearningMemoryAdapter.onFirstSpokenPhrase(engine, "comm_hello")
        GuidedLearningMemoryAdapter.onFirstSpokenPhrase(engine, "comm_yes")
        assertEquals(1, engine.getMilestones().count { it == LearningMilestone.FirstPhrase })
    }

    @Test
    fun lessonCompletion_recordedInLearningHistory() {
        GuidedLearningMemoryAdapter.onLessonCompleted(engine, "comm_hello", "Communication", 3)
        val history = engine.getLearningHistory()
        assertEquals(1, history.size)
        assertEquals("comm_hello", history.first().lessonId)
        assertNotNull(history.first().completedAtMs)
    }

    @Test
    fun practiceSession_recordedInPracticeHistory() {
        PracticeMemoryAdapter.onPracticeSessionStarted(engine)
        PracticeMemoryAdapter.onPracticeExerciseCompleted(engine, "practice_0", successful = true)
        PracticeMemoryAdapter.onPracticeSessionEnded(engine)
        assertEquals(1, engine.getPracticeHistory().size)
        assertTrue(engine.getPracticeHistory().first().successful)
    }

    @Test
    fun sessionSummary_tracksDuration() {
        engine.startSession()
        engine.endSession()
        assertEquals(1, engine.getStatistics().totalSessions)
        assertTrue(engine.getStatistics().lastSessionEndMs > 0L)
    }

    @Test
    fun preferenceSnapshot_persistedViaAdapter() {
        GuidedLearningMemoryAdapter.syncPreferences(
            engine,
            TrainingPreferences(narrationEnabled = true, narrationSpeed = 0.85f, narrationVolume = 0.9f)
        )
        val exported = engine.exportMemory()
        assertTrue(exported.contains("narrationSpeed"))
    }

    @Test
    fun memoryImportance_lowMemoriesTrimmed() {
        repeat(110) { i ->
            engine.recordEvent(
                MemoryEvent("open_lesson_$i", evidence = "User opened lesson index $i"),
                MemoryCategory.Learning,
                MemoryImportance.Low,
                "Lesson opened",
                "User opened lesson $i"
            )
        }
        val lowCount = engine.exportMemory().split("\"importance\":\"Low\"").size - 1
        assertTrue(lowCount <= 100)
    }

    @Test
    fun memoryExportImport_roundTrip() {
        engine.recordMilestone(LearningMilestone.GraduationCompleted, "Graduation ceremony completed")
        val json = engine.exportMemory()
        val fresh = CompanionMemoryEngines.createForTests()
        assertTrue(fresh.importMemory(json))
        assertTrue(LearningMilestone.GraduationCompleted in fresh.getMilestones())
    }

    @Test
    fun privacyBlocks_inferredEmotions() {
        val rejected = engine.recordEvent(
            MemoryEvent("emotion", evidence = "User was frustrated during practice"),
            MemoryCategory.Personality,
            MemoryImportance.High,
            "Emotion",
            "User was frustrated"
        )
        assertNull(rejected)
        assertFalse(MemoryPrivacyGuard.isObservableEvidenceValid("User was frustrated"))
    }

    @Test
    fun guidedLearningIntegration_recordsTutorialEvents() {
        GuidedLearningMemoryAdapter.onTutorialStarted(engine)
        GuidedLearningMemoryAdapter.onTutorialSkipped(engine)
        GuidedLearningMemoryAdapter.onGraduation(engine)
        assertTrue(LearningMilestone.GraduationCompleted in engine.getMilestones())
    }

    @Test
    fun personalityIntegration_enrichesDialogueContext() {
        engine.startSession()
        val greeting = engine.getGreetingContext()
        val enriched = PersonalityMemoryAdapter.enrichDialogueContext(DialogueContext(), greeting)
        assertNotNull(enriched)
    }

    @Test
    fun preferenceMemory_doesNotDuplicateStore() {
        engine.recordPreference(
            PreferenceMemorySnapshot(narrationEnabled = true, narrationSpeed = 0.9f)
        )
        assertTrue(engine.exportMemory().contains("narrationEnabled"))
    }
}
