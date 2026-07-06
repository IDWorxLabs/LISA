package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.launchwelcomestatepriority.WelcomeStatePriorityGate
import com.idworx.lisa.features.launchwelcomestatepriority.metadata.LaunchWelcomeStatePriorityMetadata
import com.idworx.lisa.features.launchwelcomestatepriority.validation.LaunchWelcomeStatePriorityAuthorityV1
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchWelcomeStatePriorityAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = LaunchWelcomeStatePriorityAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(LaunchWelcomeStatePriorityAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(LaunchWelcomeStatePriorityAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun savedHelloLesson_resetsToWelcome() {
        val saved = TrainingProgress(
            tutorialStarted = true,
            firstLaunchChoiceMade = true,
            currentPhase = TrainingPhase.CommunicationLesson,
            communicationLessonIndex = 0
        )
        val gated = WelcomeStatePriorityGate.apply(saved)
        assertEquals(TrainingPhase.FirstLaunchChoice, gated.currentPhase)
        assertFalse(gated.firstLaunchChoiceMade)
    }

    @Test
    fun completedTraining_resetsToWelcomeOnColdLaunch() {
        val saved = TrainingProgress(
            tutorialCompleted = true,
            currentPhase = TrainingPhase.Completion
        )
        assertEquals(TrainingPhase.FirstLaunchChoice, WelcomeStatePriorityGate.apply(saved).currentPhase)
        assertFalse(WelcomeStatePriorityGate.apply(saved).firstLaunchChoiceMade)
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_LAUNCH_WELCOME_STATE_PRIORITY_V1_PASS",
            LaunchWelcomeStatePriorityMetadata.PASS_TOKEN
        )
    }
}
