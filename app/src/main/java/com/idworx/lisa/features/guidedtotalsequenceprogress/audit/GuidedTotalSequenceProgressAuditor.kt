package com.idworx.lisa.features.guidedtotalsequenceprogress.audit

import com.idworx.lisa.features.experiencepolish.guidedlearningsimplification.validation.GuidedLearningSimplificationAuthorityV1
import com.idworx.lisa.features.guidedblinkacceptancevisualfeedback.validation.GuidedBlinkAcceptanceVisualFeedbackAuthorityV1
import com.idworx.lisa.features.guidedcurriculumandnavigationcontext.validation.GuidedCurriculumAndNavigationContextAuthorityV1
import com.idworx.lisa.features.guidedlearninginteractivelessons.validation.GuidedLearningInteractiveLessonsAuthorityV1
import com.idworx.lisa.features.guidedlearningsetupbeforehello.validation.GuidedLearningSetupBeforeHelloAuthorityV1
import com.idworx.lisa.features.guidedlessonprogresslabel.validation.GuidedLessonProgressLabelAuthorityV1
import com.idworx.lisa.features.guidedpartialtimeoutandwrongeyefeedback.validation.GuidedPartialTimeoutAndWrongEyeFeedbackAuthorityV1
import com.idworx.lisa.features.guidedremoveredundanthelpertext.validation.GuidedRemoveRedundantHelperTextAuthorityV1
import com.idworx.lisa.features.guidedsuccesstimingfix.validation.GuidedSuccessTimingFixAuthorityV1
import com.idworx.lisa.features.guideduioverlapandfalseblinkfix.validation.GuidedUiOverlapAndFalseBlinkFixAuthorityV1
import com.idworx.lisa.features.guidedwrongblinkrestartssequence.validation.GuidedWrongBlinkRestartsSequenceAuthorityV1
import com.idworx.lisa.features.onboardingguide.lessoninteraction.LessonInteractionEngine
import com.idworx.lisa.features.onboardingguide.model.CommunicationLesson
import com.idworx.lisa.features.onboardingguide.validation.GuidedTrainingAuthorityV1
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import com.idworx.lisa.validation.ValidationOutcome

object GuidedTotalSequenceProgressAuditor {

    private fun lesson(left: Int, right: Int, blinkOrder: String? = null): CommunicationLesson =
        CommunicationLesson(
            id = "t_${left}_${right}_${blinkOrder ?: "none"}",
            vocabularyId = "test",
            left = left,
            right = right,
            displayOrder = 0,
            blinkOrder = blinkOrder
        )

    fun progressUsesTotalSequenceLength(): Boolean {
        val l1r3 = lesson(1, 3)
        val step1 = LessonInteractionEngine.totalSequenceProgressLabel(0, 1, l1r3)
        val step2 = LessonInteractionEngine.totalSequenceProgressLabel(0, 2, l1r3)
        val step3 = LessonInteractionEngine.totalSequenceProgressLabel(0, 3, l1r3)
        val step4 = LessonInteractionEngine.totalSequenceProgressLabel(1, 3, l1r3)
        return step1 == "1 of 4 blinks" &&
            step2 == "2 of 4 blinks" &&
            step3 == "3 of 4 blinks" &&
            step4 == "4 of 4 blinks"
    }

    fun totalEqualsLeftPlusRightRequirements(): Boolean {
        val l2 = LessonInteractionEngine.totalBlinksRequired(lesson(2, 0))
        val r2 = LessonInteractionEngine.totalBlinksRequired(lesson(0, 2))
        val l1r1 = LessonInteractionEngine.totalBlinksRequired(lesson(1, 1))
        val l1r3 = LessonInteractionEngine.totalBlinksRequired(lesson(1, 3))
        val l3r2 = LessonInteractionEngine.totalBlinksRequired(lesson(3, 2))
        return l2 == 2 && r2 == 2 && l1r1 == 2 && l1r3 == 4 && l3r2 == 5
    }

    fun progressIncrementsOnEveryAcceptedBlink(): Boolean {
        val l3r2 = lesson(3, 2)
        val labels = listOf(
            LessonInteractionEngine.totalSequenceProgressLabel(1, 0, l3r2),
            LessonInteractionEngine.totalSequenceProgressLabel(2, 0, l3r2),
            LessonInteractionEngine.totalSequenceProgressLabel(3, 0, l3r2),
            LessonInteractionEngine.totalSequenceProgressLabel(3, 1, l3r2),
            LessonInteractionEngine.totalSequenceProgressLabel(3, 2, l3r2)
        )
        return labels == listOf("1 of 5 blinks", "2 of 5 blinks", "3 of 5 blinks", "4 of 5 blinks", "5 of 5 blinks")
    }

    fun waitingLabelReflectsNextExpectedEye(): Boolean {
        val l1r3 = lesson(1, 3)
        val afterZero = LessonInteractionEngine.waitingForLabel(l1r3, emptyList(), 0, 0)
        val afterOneLeft = LessonInteractionEngine.waitingForLabel(l1r3, emptyList(), 1, 0)
        val afterOneRight = LessonInteractionEngine.waitingForLabel(l1r3, emptyList(), 1, 1)
        return afterZero == "Left blink" && afterOneLeft == "Right blink" && afterOneRight == "Right blink"
    }

    fun finalAcceptedBlinkShowsCompleteSequence(): Boolean {
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
            GuidedRemoveRedundantHelperTextAuthorityV1.validate().outcome
        )
        return outcomes.all { it == ValidationOutcome.PASS }
    }

    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/GuidedTotalSequenceProgressAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaGuidedTotalSequenceProgressV1")
    }

    private fun readLessonScreens(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingLessonScreens.kt"
    )
}
