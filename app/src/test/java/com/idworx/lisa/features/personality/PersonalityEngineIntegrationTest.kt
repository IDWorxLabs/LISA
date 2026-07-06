package com.idworx.lisa.features.personality

import com.idworx.lisa.features.onboardingguide.services.EncouragementEngine
import com.idworx.lisa.features.personality.engine.DefaultLisaPersonalityEngine
import com.idworx.lisa.features.personality.engine.LisaPersonalityEngines
import com.idworx.lisa.features.personality.model.DialogueContext
import com.idworx.lisa.features.personality.model.MilestoneType
import com.idworx.lisa.features.personality.state.DialogueHistoryTracker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalityEngineIntegrationTest {

    @Test
    fun guidedLearning_welcomeUsesPersonalityEngine() {
        val lines = EncouragementEngine.welcomeNarration()
        assertTrue(lines.size >= 4)
        assertTrue(lines.first().contains("Hello", ignoreCase = true))
    }

    @Test
    fun guidedLearning_completionUsesGraduationCatalog() {
        val lines = EncouragementEngine.completionNarration()
        assertTrue(lines.any { it.contains("Congratulations", ignoreCase = true) })
    }

    @Test
    fun ttsPath_narrationControllerAcceptsDialogue() {
        val engine = LisaPersonalityEngines.default
        val dialogue = engine.generateEncouragement(DialogueContext())
        assertTrue(dialogue.text.isNotBlank())
        assertTrue(dialogue.timing.recommendedSpeechRate in 0.5f..1.5f)
    }

    @Test
    fun repetitionPrevention_acrossEncouragementCalls() {
        val tracker = DialogueHistoryTracker()
        val engine = DefaultLisaPersonalityEngine(history = tracker)
        val ids = (1..5).map {
            engine.generateEncouragement(DialogueContext()).id
        }.toSet()
        assertTrue(ids.size >= 2)
    }

    @Test
    fun comfortNeverUsesForbiddenPhrases() {
        val engine = LisaPersonalityEngines.default
        repeat(10) { i ->
            val text = engine.generateComfort(DialogueContext(consecutiveFailures = i, deterministicSeed = i)).text
            assertFalse(text.contains("Wrong", ignoreCase = true))
            assertFalse(text.contains("I know how you feel", ignoreCase = true))
        }
    }

    @Test
    fun milestoneContext_firstBlink() {
        val engine = LisaPersonalityEngines.default
        val dialogue = engine.generateMilestoneMessage(
            DialogueContext(milestoneType = MilestoneType.FirstSuccessfulBlink, firstSuccessfulBlink = true)
        )
        assertNotEquals("", dialogue.text)
    }
}
