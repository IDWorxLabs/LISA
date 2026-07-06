package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.onboardingguide.services.EncouragementEngine
import com.idworx.lisa.features.personality.engine.DefaultLisaPersonalityEngine
import com.idworx.lisa.features.personality.engine.LisaPersonalityEngines
import com.idworx.lisa.features.personality.model.DialogueCategory
import com.idworx.lisa.features.personality.model.DialogueContext
import com.idworx.lisa.features.personality.model.LisaPersonalityProfile
import com.idworx.lisa.features.personality.model.MilestoneType
import com.idworx.lisa.features.personality.state.DialogueHistoryTracker
import com.idworx.lisa.features.personality.validation.PersonalityEngineAuthorityV1
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalityEngineAuthorityV1Test {

    private val engine: DefaultLisaPersonalityEngine =
        LisaPersonalityEngines.default as DefaultLisaPersonalityEngine

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = PersonalityEngineAuthorityV1.validate()

        assertEquals(ValidationOutcome.PASS, report.outcome)
        assertEquals(PersonalityEngineAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        assertTrue(report.checkResults.all { it.passed })

        println(report.formatReport())
        println(PersonalityEngineAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun generateEncouragement_noForbiddenLanguage() {
        val forbidden = LisaPersonalityProfile.DEFAULT_FORBIDDEN_PHRASES
        repeat(20) { seed ->
            val text = engine.generateEncouragement(DialogueContext(deterministicSeed = seed)).text
            forbidden.forEach { word ->
                assertFalse(text.contains(word, ignoreCase = true))
            }
        }
    }

    @Test
    fun noImmediateRepetition() {
        val tracker = DialogueHistoryTracker()
        val testEngine = DefaultLisaPersonalityEngine(history = tracker)
        val ctx = DialogueContext()
        val first = testEngine.generateEncouragement(ctx)
        val second = testEngine.generateEncouragement(ctx)
        assertNotEquals(first.id, second.id)
    }

    @Test
    fun deterministicSelection_producesSameDialogue() {
        val testEngine = DefaultLisaPersonalityEngine(history = DialogueHistoryTracker())
        val ctx = DialogueContext(deterministicSeed = 3)
        val a = testEngine.generateEncouragement(ctx)
        val b = testEngine.generateEncouragement(ctx)
        assertEquals(a.id, b.id)
    }

    @Test
    fun greeting_firstLaunchSequence_hasMultipleLines() {
        val lines = engine.generateGreetingSequence(
            DialogueContext(returningUser = false)
        )
        assertTrue(lines.size >= 4)
    }

    @Test
    fun greeting_returningUser_differsFromFirstLaunch() {
        val first = engine.generateGreetingSequence(DialogueContext(returningUser = false))
        val returning = engine.generateGreetingSequence(DialogueContext(returningUser = true, daysSinceLastSession = 3))
        assertNotEquals(first.first().id, returning.first().id)
    }

    @Test
    fun comfort_afterRepeatedFailures_isSupportive() {
        val ctx = DialogueContext(consecutiveFailures = 4)
        val text = engine.generateComfort(ctx).text
        assertFalse(text.contains("Wrong", ignoreCase = true))
        assertTrue(text.isNotBlank())
    }

    @Test
    fun celebration_tierSelection() {
        val minor = engine.generateCelebration(DialogueContext(celebrationTier = 1))
        val major = engine.generateCelebration(
            DialogueContext(celebrationTier = 2, milestoneType = MilestoneType.LessonComplete)
        )
        assertEquals(DialogueCategory.MinorCelebration, minor.category)
        assertTrue(major.category == DialogueCategory.MajorCelebration || major.category == DialogueCategory.MilestoneCelebration)
    }

    @Test
    fun graduationMessage_hasExpectedContent() {
        val lines = engine.generateGraduationMessage(
            DialogueContext(milestoneType = MilestoneType.GuidedLearningComplete)
        )
        assertTrue(lines.any { it.text.contains("Congratulations", ignoreCase = true) })
    }

    @Test
    fun guidedLearningIntegration_encouragementEngineDelegates() {
        val msg = EncouragementEngine.successMessage(0)
        assertNotNull(msg)
        assertFalse(msg.contains("Failed", ignoreCase = true))
    }

    @Test
    fun milestone_firstSpokenPhrase() {
        val dialogue = engine.generateMilestoneMessage(
            DialogueContext(milestoneType = MilestoneType.FirstSpokenPhrase, firstSpokenPhrase = true)
        )
        assertTrue(dialogue.text.contains("communicat", ignoreCase = true) || dialogue.text.contains("eyes", ignoreCase = true))
    }
}
