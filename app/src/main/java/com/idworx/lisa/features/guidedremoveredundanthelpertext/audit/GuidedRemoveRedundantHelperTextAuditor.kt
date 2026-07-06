package com.idworx.lisa.features.guidedremoveredundanthelpertext.audit

import com.idworx.lisa.features.experiencepolish.guidedlearningsimplification.validation.GuidedLearningSimplificationAuthorityV1
import com.idworx.lisa.features.guidedblinkacceptancevisualfeedback.validation.GuidedBlinkAcceptanceVisualFeedbackAuthorityV1
import com.idworx.lisa.features.guidedcurriculumandnavigationcontext.validation.GuidedCurriculumAndNavigationContextAuthorityV1
import com.idworx.lisa.features.guidedlearninginteractivelessons.validation.GuidedLearningInteractiveLessonsAuthorityV1
import com.idworx.lisa.features.guidedlearningsetupbeforehello.validation.GuidedLearningSetupBeforeHelloAuthorityV1
import com.idworx.lisa.features.guidedlessonprogresslabel.validation.GuidedLessonProgressLabelAuthorityV1
import com.idworx.lisa.features.guidedpartialtimeoutandwrongeyefeedback.validation.GuidedPartialTimeoutAndWrongEyeFeedbackAuthorityV1
import com.idworx.lisa.features.guidedsuccesstimingfix.validation.GuidedSuccessTimingFixAuthorityV1
import com.idworx.lisa.features.guideduioverlapandfalseblinkfix.validation.GuidedUiOverlapAndFalseBlinkFixAuthorityV1
import com.idworx.lisa.features.guidedwrongblinkrestartssequence.validation.GuidedWrongBlinkRestartsSequenceAuthorityV1
import com.idworx.lisa.features.onboardingguide.ui.formatWinkInstruction
import com.idworx.lisa.features.onboardingguide.validation.GuidedTrainingAuthorityV1
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import com.idworx.lisa.validation.ValidationOutcome

object GuidedRemoveRedundantHelperTextAuditor {

    fun noCommunicationLessonRendersHelperSentence(): Boolean {
        val block = communicationBlock() ?: return false
        return !block.contains("text = instruction") && block.contains("instruction: String")
    }

    fun noNavigationLessonRendersHelperSentence(): Boolean {
        val block = navigationBlock() ?: return false
        return !block.contains("text = instruction") && block.contains("instruction: String")
    }

    fun removalIsSystemWideNotPerLesson(): Boolean {
        val lessons = readLessonScreens() ?: return false
        val ui = readGuidedLearningUi() ?: return false
        val flow = readGuidedTrainingFlow() ?: return false
        // The sentence text is generated from a single shared source, not hardcoded per lesson —
        // proving the removal of its rendering applies to every lesson at once, not lesson-by-lesson.
        return !lessons.contains("text = instruction") &&
            ui.contains("fun formatWinkInstruction") &&
            flow.contains("formatWinkInstruction") &&
            formatWinkInstruction(1, 3).contains("blink left once, then right three times")
    }

    fun gestureInstructionRemainsVisible(): Boolean {
        val block = communicationBlock() ?: return false
        return block.contains("SimplifiedGestureDisplay(left = left, right = right)")
    }

    fun phraseTitleRemainsVisible(): Boolean {
        val block = communicationBlock() ?: return false
        return block.contains("GuidedLessonPhraseTitle(") && block.contains("phrase = phrase")
    }

    fun lessonLayoutRemainsVerticallyBalanced(): Boolean {
        val commBlock = communicationBlock() ?: return false
        val navBlock = navigationBlock() ?: return false
        val commBalanced = Regex(
            "SimplifiedGestureDisplay\\(left = left, right = right\\)[\\s\\S]{0,60}" +
                "Spacer\\(Modifier\\.height\\(28\\.dp\\)\\)"
        ).containsMatchIn(commBlock)
        val navBalanced = Regex(
            "text = title,[\\s\\S]{0,200}Spacer\\(Modifier\\.height\\(20\\.dp\\)\\)"
        ).containsMatchIn(navBlock)
        return commBalanced && navBalanced
    }

    fun existingGuidedLearningFunctionalityUnchanged(): Boolean {
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
            GuidedBlinkAcceptanceVisualFeedbackAuthorityV1.validate().outcome
        )
        return outcomes.all { it == ValidationOutcome.PASS }
    }

    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/GuidedRemoveRedundantHelperTextAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaGuidedRemoveRedundantHelperTextV1")
    }

    private fun communicationBlock(): String? {
        val lessons = readLessonScreens() ?: return null
        val start = lessons.indexOf("fun CommunicationLessonScreen(")
        val end = lessons.indexOf("fun NavigationLessonScreen(", start)
        if (start < 0 || end < 0) return null
        return lessons.substring(start, end)
    }

    private fun navigationBlock(): String? {
        val lessons = readLessonScreens() ?: return null
        val start = lessons.indexOf("fun NavigationLessonScreen(")
        val end = lessons.indexOf("fun TrainingCalibrationScreen(", start)
        if (start < 0 || end < 0) return null
        return lessons.substring(start, end)
    }

    private fun readLessonScreens(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingLessonScreens.kt"
    )

    private fun readGuidedLearningUi(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/GuidedLearningUi.kt"
    )

    private fun readGuidedTrainingFlow(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/GuidedTrainingFlow.kt"
    )
}
