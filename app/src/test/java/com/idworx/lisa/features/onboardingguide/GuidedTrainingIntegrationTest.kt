package com.idworx.lisa.features.onboardingguide

import com.idworx.lisa.features.onboardingguide.metadata.TrainingMetadata
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.navigation.GuidedTrainingNavigator
import com.idworx.lisa.features.onboardingguide.services.EncouragementEngine
import com.idworx.lisa.features.onboardingguide.services.TrainingProgressStore
import com.idworx.lisa.features.onboardingguide.state.TrainingEvent
import com.idworx.lisa.features.silentwelcome.LisaSpeechPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Integration-style tests for the guided training flow without Android context.
 */
class GuidedTrainingIntegrationTest {

    private val navigator = GuidedTrainingNavigator()

    @Test
    fun fullCommunicationPath_reachesNavigationPhase() {
        var progress = com.idworx.lisa.features.onboardingguide.model.TrainingProgress(
            tutorialStarted = true,
            currentPhase = TrainingPhase.CommunicationLesson
        )
        if (LisaSpeechPolicy.PHRASE_TRANSLATION_ONLY) {
            repeat(TrainingMetadata.GUIDED_LEARNING_ESSENTIAL_PHRASE_COUNT) {
                progress = navigator.reduce(progress, TrainingEvent.SequenceSuccess)
            }
            assertEquals(TrainingPhase.NavigationLesson, progress.currentPhase)
            assertEquals(0, progress.navigationLessonIndex)
            return
        }
        repeat(20) {
            progress = navigator.reduce(progress, TrainingEvent.SequenceSuccess)
            progress = navigator.reduce(progress, TrainingEvent.SequenceSuccess)
        }
        assertEquals(TrainingPhase.CommunicationMastery, progress.currentPhase)
        repeat(10) {
            progress = navigator.reduce(progress, TrainingEvent.SequenceSuccess)
        }
        assertEquals(TrainingPhase.NavigationLesson, progress.currentPhase)
        assertEquals(0, progress.navigationLessonIndex)
    }

    @Test
    fun fullTrainingPath_reachesCompletion() {
        var progress = com.idworx.lisa.features.onboardingguide.model.TrainingProgress(
            tutorialStarted = true,
            currentPhase = TrainingPhase.CommunicationLesson
        )
        if (LisaSpeechPolicy.PHRASE_TRANSLATION_ONLY) {
            repeat(TrainingMetadata.GUIDED_LEARNING_ESSENTIAL_PHRASE_COUNT) {
                progress = navigator.reduce(progress, TrainingEvent.SequenceSuccess)
            }
        } else {
            repeat(20) {
                progress = navigator.reduce(progress, TrainingEvent.SequenceSuccess)
                progress = navigator.reduce(progress, TrainingEvent.SequenceSuccess)
            }
            repeat(10) {
                progress = navigator.reduce(progress, TrainingEvent.SequenceSuccess)
            }
        }
        repeat(8) {
            val lessonId = com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
                .navigationLessonAt(progress.navigationLessonIndex)!!.id
            progress = navigator.reduce(progress, TrainingEvent.NavigationActionCompleted(lessonId))
        }
        assertEquals(TrainingPhase.Completion, progress.currentPhase)
        assertTrue(progress.tutorialCompleted)
        assertTrue(progress.certifiedCommunicator)
    }

    @Test
    fun skipFlow_marksSkippedWithoutCompletion() {
        var progress = com.idworx.lisa.features.onboardingguide.model.TrainingProgress()
        progress = navigator.reduce(progress, TrainingEvent.SkipTutorial)
        progress = navigator.reduce(progress, TrainingEvent.ConfirmSkip)
        assertTrue(progress.tutorialSkipped)
        assertFalse(progress.tutorialCompleted)
    }

    @Test
    fun encouragementMessages_neverUseForbiddenWords() {
        val forbidden = listOf("Wrong", "Failed", "Incorrect")
        val all = (0..25).flatMap { i ->
            listOf(
                EncouragementEngine.successMessage(i),
                EncouragementEngine.retryMessage(i),
                EncouragementEngine.almostMessage(i)
            )
        }
        forbidden.forEach { word ->
            assertTrue(all.none { it.contains(word, ignoreCase = true) })
        }
    }

    @Test
    fun persistenceStore_keysAreDefined() {
        assertTrue(TrainingProgressStore::class.java.declaredFields.any { it.name == "KEY_COMPLETED" })
        assertTrue(TrainingProgressStore::class.java.declaredFields.any { it.name == "KEY_STARTED" })
    }
}
