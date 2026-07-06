package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.experiencepolish.patientcommunicationcoach.metadata.PatientCommunicationCoachMetadata
import com.idworx.lisa.features.experiencepolish.patientcommunicationcoach.validation.PatientCommunicationCoachAuthorityV1
import com.idworx.lisa.features.onboardingguide.coach.CommunicationCoachEngine
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.metadata.TrainingMetadata
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.features.silentwelcome.LisaSpeechPolicy
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatientCommunicationCoachAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = PatientCommunicationCoachAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(PatientCommunicationCoachAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(PatientCommunicationCoachAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun catalog_hasNoOverwhelmingGestureJumps() {
        assertTrue(TrainingLessonCatalog.adjacentDifficultyJumpsWithin(CommunicationCoachEngine.MAX_DIFFICULTY_JUMP))
    }

    @Test
    fun coach_requiresTwoSuccessesBeforeAdvanceInFundamentals() {
        if (LisaSpeechPolicy.PHRASE_TRANSLATION_ONLY) {
            assertEquals(
                1,
                CommunicationCoachEngine.successesNeededToAdvance(TrainingPhase.CommunicationLesson)
            )
            val afterOne = TrainingProgress(
                currentPhase = TrainingPhase.CommunicationLesson,
                currentLessonSuccessCount = 1
            )
            assertTrue(CommunicationCoachEngine.shouldAdvanceAfterSuccess(afterOne))
            return
        }
        var progress = TrainingProgress(
            currentPhase = TrainingPhase.CommunicationLesson,
            currentLessonSuccessCount = 1
        )
        assertFalse(CommunicationCoachEngine.shouldAdvanceAfterSuccess(progress))
        progress = progress.copy(currentLessonSuccessCount = 2)
        assertTrue(CommunicationCoachEngine.shouldAdvanceAfterSuccess(progress))
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_PATIENT_COMMUNICATION_COACH_V1_PASS",
            PatientCommunicationCoachMetadata.PASS_TOKEN
        )
    }
}
