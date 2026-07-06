package com.idworx.lisa.features.guidedprogresswordingpolish.audit

import com.idworx.lisa.features.experiencepolish.guidedlearningsimplification.validation.GuidedLearningSimplificationAuthorityV1
import com.idworx.lisa.features.guidedblinkacceptancevisualfeedback.validation.GuidedBlinkAcceptanceVisualFeedbackAuthorityV1
import com.idworx.lisa.features.guidedcurriculumandnavigationcontext.validation.GuidedCurriculumAndNavigationContextAuthorityV1
import com.idworx.lisa.features.guidedlearninginteractivelessons.validation.GuidedLearningInteractiveLessonsAuthorityV1
import com.idworx.lisa.features.guidedlearningsetupbeforehello.validation.GuidedLearningSetupBeforeHelloAuthorityV1
import com.idworx.lisa.features.guidedlessonprogresslabel.validation.GuidedLessonProgressLabelAuthorityV1
import com.idworx.lisa.features.guidedpartialtimeoutandwrongeyefeedback.validation.GuidedPartialTimeoutAndWrongEyeFeedbackAuthorityV1
import com.idworx.lisa.features.guidedremoveredundanthelpertext.validation.GuidedRemoveRedundantHelperTextAuthorityV1
import com.idworx.lisa.features.guidedsuccesstimingfix.validation.GuidedSuccessTimingFixAuthorityV1
import com.idworx.lisa.features.guidedtotalsequenceprogress.validation.GuidedTotalSequenceProgressAuthorityV1
import com.idworx.lisa.features.guideduioverlapandfalseblinkfix.validation.GuidedUiOverlapAndFalseBlinkFixAuthorityV1
import com.idworx.lisa.features.guidedwrongblinkrestartssequence.validation.GuidedWrongBlinkRestartsSequenceAuthorityV1
import com.idworx.lisa.features.onboardingguide.lessoninteraction.LessonInteractionEngine
import com.idworx.lisa.features.onboardingguide.model.CommunicationLesson
import com.idworx.lisa.features.onboardingguide.validation.GuidedTrainingAuthorityV1
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import com.idworx.lisa.validation.ValidationOutcome

object GuidedProgressWordingPolishAuditor {

    private fun lesson(left: Int, right: Int, blinkOrder: String? = null): CommunicationLesson =
        CommunicationLesson(
            id = "t_${left}_${right}_${blinkOrder ?: "none"}",
            vocabularyId = "test",
            left = left,
            right = right,
            displayOrder = 0,
            blinkOrder = blinkOrder
        )

    fun progressDisplaysXOfYBlinksWording(): Boolean {
        val l1r3 = lesson(1, 3)
        val step1 = LessonInteractionEngine.totalSequenceProgressLabel(0, 1, l1r3)
        val step4 = LessonInteractionEngine.totalSequenceProgressLabel(1, 3, l1r3)
        val l2 = lesson(2, 0)
        val l2Step1 = LessonInteractionEngine.totalSequenceProgressLabel(1, 0, l2)
        val l2Step2 = LessonInteractionEngine.totalSequenceProgressLabel(2, 0, l2)
        val engineSource = readLessonInteractionEngine() ?: return false
        return step1 == "1 of 4 blinks" &&
            step4 == "4 of 4 blinks" &&
            l2Step1 == "1 of 2 blinks" &&
            l2Step2 == "2 of 2 blinks" &&
            !engineSource.contains("\"Blink \$completed of \$total\"") &&
            engineSource.contains("\"\$completed of \$total blinks\"")
    }

    fun totalBlinkCountRemainsCorrect(): Boolean {
        val l1r3 = LessonInteractionEngine.totalBlinksRequired(lesson(1, 3))
        val l3r2 = LessonInteractionEngine.totalBlinksRequired(lesson(3, 2))
        val l1r1 = LessonInteractionEngine.totalBlinksRequired(lesson(1, 1))
        return l1r3 == 4 && l3r2 == 5 && l1r1 == 2
    }

    fun progressIncrementsCorrectlyAfterEveryAcceptedBlink(): Boolean {
        val l3r2 = lesson(3, 2)
        val labels = listOf(
            LessonInteractionEngine.totalSequenceProgressLabel(1, 0, l3r2),
            LessonInteractionEngine.totalSequenceProgressLabel(2, 0, l3r2),
            LessonInteractionEngine.totalSequenceProgressLabel(3, 0, l3r2),
            LessonInteractionEngine.totalSequenceProgressLabel(3, 1, l3r2),
            LessonInteractionEngine.totalSequenceProgressLabel(3, 2, l3r2)
        )
        return labels == listOf(
            "1 of 5 blinks",
            "2 of 5 blinks",
            "3 of 5 blinks",
            "4 of 5 blinks",
            "5 of 5 blinks"
        )
    }

    fun waitingLabelRemainsUnchanged(): Boolean {
        val l1r3 = lesson(1, 3)
        val afterZero = LessonInteractionEngine.waitingForLabel(l1r3, emptyList(), 0, 0)
        val afterOneLeft = LessonInteractionEngine.waitingForLabel(l1r3, emptyList(), 1, 0)
        val screens = readLessonScreens() ?: return false
        return afterZero == "Left blink" &&
            afterOneLeft == "Right blink" &&
            screens.contains("\"Waiting for:\"") &&
            screens.contains("lessonInteraction.waitingForLabel")
    }

    fun sequenceCompleteStillAppearsAfterFinalBlink(): Boolean {
        val l1r3 = lesson(1, 3)
        val finalProgress = LessonInteractionEngine.totalSequenceProgressLabel(1, 3, l1r3)
        val finalWaiting = LessonInteractionEngine.waitingForLabel(l1r3, emptyList(), 1, 3)
        val screens = readLessonScreens() ?: return false
        return finalProgress == "4 of 4 blinks" &&
            finalWaiting == null &&
            screens.contains("Sequence complete")
    }

    fun existingGuidedLearningValidationsRemainGreen(): Boolean {
        val outcomes = listOf(
            GuidedLearningInteractiveLessonsAuthorityV1.validate().outcome,
            GuidedTrainingAuthorityV1.validate().outcome,
            GuidedCurriculumAndNavigationContextAuthorityV1.validate().outcome,
            GuidedSuccessTimingFixAuthorityV1.validate().outcome,
            GuidedWrongBlinkRestartsSequenceAuthorityV1.validate().outcome,
            GuidedUiOverlapAndFalseBlinkFixAuthorityV1.validate().outcome,
            GuidedPartialTimeoutAndWrongEyeFeedbackAuthorityV1.validate().outcome,
            GuidedLessonProgressLabelAuthorityV1.validate().outcome,
            GuidedLearningSetupBeforeHelloAuthorityV1.validate().outcome,
            GuidedLearningSimplificationAuthorityV1.validate().outcome,
            GuidedBlinkAcceptanceVisualFeedbackAuthorityV1.validate().outcome,
            GuidedRemoveRedundantHelperTextAuthorityV1.validate().outcome,
            GuidedTotalSequenceProgressAuthorityV1.validate().outcome
        )
        return outcomes.all { it == ValidationOutcome.PASS }
    }

    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/GuidedProgressWordingPolishAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaGuidedProgressWordingPolishV1")
    }

    private fun readLessonScreens(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingLessonScreens.kt"
    )

    private fun readLessonInteractionEngine(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/lessoninteraction/LessonInteractionEngine.kt"
    )
}
