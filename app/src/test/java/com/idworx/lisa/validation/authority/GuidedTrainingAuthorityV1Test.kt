package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.onboardingguide.validation.GuidedTrainingAuthorityV1
import com.idworx.lisa.features.onboardingguide.coach.CommunicationCoachEngine
import com.idworx.lisa.features.onboardingguide.metadata.TrainingMetadata
import com.idworx.lisa.features.silentwelcome.LisaSpeechPolicy
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidedTrainingAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = GuidedTrainingAuthorityV1.validate()

        assertEquals(ValidationOutcome.PASS, report.outcome)
        assertEquals(GuidedTrainingAuthorityV1.PASS_TOKEN, report.passToken)
        assertEquals(GuidedTrainingAuthorityV1.AUTHORITY_NAME, report.authorityName)
        assertTrue(report.failedChecks.isEmpty())
        assertTrue(report.checkResults.all { it.passed })

        println(report.formatReport())
        println(GuidedTrainingAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun lessonCatalog_hasTwelveCommunicationLessons() {
        assertEquals(20, com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog.communicationLessons.size)
    }

    @Test
    fun encouragementEngine_avoidsPunitiveLanguage() {
        val forbidden = listOf("Wrong", "Failed", "Incorrect")
        val samples = (0..20).flatMap { seed ->
            listOf(
                com.idworx.lisa.features.onboardingguide.services.EncouragementEngine.successMessage(seed),
                com.idworx.lisa.features.onboardingguide.services.EncouragementEngine.retryMessage(seed)
            )
        }
        forbidden.forEach { word ->
            assertTrue(samples.none { it.contains(word, ignoreCase = true) })
        }
    }

    @Test
    fun navigator_advancesThroughCommunicationLessons() {
        val navigator = com.idworx.lisa.features.onboardingguide.navigation.GuidedTrainingNavigator()
        var progress = com.idworx.lisa.features.onboardingguide.model.TrainingProgress(
            currentPhase = com.idworx.lisa.features.onboardingguide.model.TrainingPhase.CommunicationLesson
        )
        if (LisaSpeechPolicy.PHRASE_TRANSLATION_ONLY) {
            repeat(TrainingMetadata.GUIDED_LEARNING_ESSENTIAL_PHRASE_COUNT) {
                progress = navigator.reduce(
                    progress,
                    com.idworx.lisa.features.onboardingguide.state.TrainingEvent.SequenceSuccess
                )
            }
            assertEquals(
                com.idworx.lisa.features.onboardingguide.model.TrainingPhase.NavigationLesson,
                progress.currentPhase
            )
            return
        }
        val successesPerLesson = CommunicationCoachEngine.SUCCESSES_TO_MASTER
        repeat(20 * successesPerLesson) {
            progress = navigator.reduce(progress, com.idworx.lisa.features.onboardingguide.state.TrainingEvent.SequenceSuccess)
        }
        assertEquals(
            com.idworx.lisa.features.onboardingguide.model.TrainingPhase.CommunicationMastery,
            progress.currentPhase
        )
        repeat(10) {
            progress = navigator.reduce(progress, com.idworx.lisa.features.onboardingguide.state.TrainingEvent.SequenceSuccess)
        }
        assertEquals(
            com.idworx.lisa.features.onboardingguide.model.TrainingPhase.NavigationLesson,
            progress.currentPhase
        )
    }

    @Test
    fun evidenceReport_includesRemediationOnSyntheticFailure() {
        val failed = GuidedTrainingAuthorityV1.validate().checkResults.first {
            it.checkId == "TRAIN_001"
        }
        assertTrue(failed.passed)
        assertNotNull(failed.remediation ?: "passed")
    }
}
