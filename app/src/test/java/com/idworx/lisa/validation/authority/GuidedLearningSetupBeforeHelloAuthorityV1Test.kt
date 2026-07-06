package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.guidedlearningsetupbeforehello.metadata.GuidedLearningSetupBeforeHelloMetadata
import com.idworx.lisa.features.guidedlearningsetupbeforehello.validation.GuidedLearningSetupBeforeHelloAuthorityV1
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.navigation.GuidedTrainingNavigator
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.features.onboardingguide.state.TrainingEvent
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidedLearningSetupBeforeHelloAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = GuidedLearningSetupBeforeHelloAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(GuidedLearningSetupBeforeHelloAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(GuidedLearningSetupBeforeHelloAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun beginLearning_opensSetupNotHello() {
        val progress = GuidedTrainingNavigator().reduce(TrainingProgress(), TrainingEvent.BeginLearning)
        assertEquals(TrainingPhase.Setup, progress.currentPhase)
        assertNotEquals(TrainingPhase.CommunicationLesson, progress.currentPhase)
    }

    @Test
    fun completeSetup_opensHello() {
        val progress = GuidedTrainingNavigator().reduce(
            TrainingProgress(currentPhase = TrainingPhase.Setup),
            TrainingEvent.CompleteSetup
        )
        assertEquals(TrainingPhase.CommunicationLesson, progress.currentPhase)
        assertEquals(0, progress.communicationLessonIndex)
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_GUIDED_LEARNING_SETUP_BEFORE_HELLO_V1_PASS",
            GuidedLearningSetupBeforeHelloMetadata.PASS_TOKEN
        )
    }
}
