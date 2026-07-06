package com.idworx.lisa.features.guidedlessonprogresslabel.audit

import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.metadata.TrainingMetadata
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.features.silentwelcome.LisaSpeechPolicy
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe

object GuidedLessonProgressLabelAuditor {

    fun currentLessonNumberDisplaysCorrectly(): Boolean {
        val first = TrainingLessonCatalog.guidedLessonProgress(
            TrainingProgress(currentPhase = TrainingPhase.CommunicationLesson, communicationLessonIndex = 0)
        )
        val eighth = TrainingLessonCatalog.guidedLessonProgress(
            TrainingProgress(currentPhase = TrainingPhase.CommunicationLesson, communicationLessonIndex = 7)
        )
        val essentialsThreshold = TrainingMetadata.GUIDED_LEARNING_ESSENTIAL_PHRASE_COUNT
        val last = TrainingLessonCatalog.guidedLessonProgress(
            TrainingProgress(
                currentPhase = TrainingPhase.CommunicationLesson,
                communicationLessonIndex = essentialsThreshold - 1
            )
        )
        return first?.first == 1 && eighth?.first == 8 && last?.first == essentialsThreshold
    }

    fun totalLessonCountDisplaysCorrectly(): Boolean {
        val expectedTotal = if (LisaSpeechPolicy.PHRASE_TRANSLATION_ONLY) {
            TrainingMetadata.GUIDED_LEARNING_ESSENTIAL_PHRASE_COUNT + TrainingMetadata.NAVIGATION_LESSON_COUNT
        } else {
            TrainingMetadata.COMMUNICATION_LESSON_COUNT +
                TrainingMetadata.MASTERY_ROUND_COUNT +
                TrainingMetadata.NAVIGATION_LESSON_COUNT
        }
        val commResult = TrainingLessonCatalog.guidedLessonProgress(
            TrainingProgress(currentPhase = TrainingPhase.CommunicationLesson, communicationLessonIndex = 3)
        )
        val navResult = TrainingLessonCatalog.guidedLessonProgress(
            TrainingProgress(currentPhase = TrainingPhase.NavigationLesson, navigationLessonIndex = 2)
        )
        return commResult?.second == expectedTotal && navResult?.second == expectedTotal
    }

    fun totalIsDerivedNotHardcoded(): Boolean {
        val catalog = readCatalog() ?: return false
        val start = catalog.indexOf("fun guidedLessonProgress")
        if (start < 0) return false
        val end = catalog.indexOf("fun stageLabel", start)
        if (end < 0) return false
        val block = catalog.substring(start, end)
        return block.contains("TrainingMetadata.GUIDED_LEARNING_ESSENTIAL_PHRASE_COUNT") &&
            block.contains("TrainingMetadata.COMMUNICATION_LESSON_COUNT") &&
            block.contains("TrainingMetadata.MASTERY_ROUND_COUNT") &&
            block.contains("TrainingMetadata.NAVIGATION_LESSON_COUNT") &&
            !block.contains(" 23") &&
            !block.contains(" 38")
    }

    fun updatesAutomaticallyAsLessonsAdvance(): Boolean {
        val early = TrainingLessonCatalog.guidedLessonProgress(
            TrainingProgress(currentPhase = TrainingPhase.CommunicationLesson, communicationLessonIndex = 0)
        )
        val later = TrainingLessonCatalog.guidedLessonProgress(
            TrainingProgress(currentPhase = TrainingPhase.CommunicationLesson, communicationLessonIndex = 5)
        )
        val flow = readFlow() ?: return false
        val callSites = Regex("TrainingLessonCatalog\\.guidedLessonProgress\\(progress\\)")
            .findAll(flow).count()
        return early?.first == 1 && later?.first == 6 && early?.second == later?.second && callSites >= 3
    }

    fun navigationLessonsContinueNumbering(): Boolean {
        val essentialsThreshold = TrainingMetadata.GUIDED_LEARNING_ESSENTIAL_PHRASE_COUNT
        val lastPhrase = TrainingLessonCatalog.guidedLessonProgress(
            TrainingProgress(
                currentPhase = TrainingPhase.CommunicationLesson,
                communicationLessonIndex = essentialsThreshold - 1
            )
        )
        val firstNav = TrainingLessonCatalog.guidedLessonProgress(
            TrainingProgress(currentPhase = TrainingPhase.NavigationLesson, navigationLessonIndex = 0)
        )
        val secondNav = TrainingLessonCatalog.guidedLessonProgress(
            TrainingProgress(currentPhase = TrainingPhase.NavigationLesson, navigationLessonIndex = 1)
        )
        return lastPhrase != null && firstNav != null && secondNav != null &&
            firstNav.first == lastPhrase.first + 1 &&
            secondNav.first == firstNav.first + 1 &&
            firstNav.second == lastPhrase.second
    }

    fun labelPlacedAbovePhraseTitleAndSmallerFont(): Boolean {
        val screens = readScreens() ?: return false
        val labelIndex = screens.indexOf("GuidedLessonProgressLabel(")
        val titleIndex = screens.indexOf("GuidedLessonPhraseTitle(")
        val navTitleIndex = screens.indexOf("text = title,")
        if (labelIndex < 0 || titleIndex < 0 || navTitleIndex < 0) return false
        val components = readComponents() ?: return false
        val labelBlockStart = components.indexOf("fun GuidedLessonProgressLabel")
        if (labelBlockStart < 0) return false
        val labelBlockEnd = components.indexOf("fun GuidedLessonPhraseTitle", labelBlockStart)
        val labelBlock = if (labelBlockEnd > 0) {
            components.substring(labelBlockStart, labelBlockEnd)
        } else {
            components.substring(labelBlockStart)
        }
        return labelIndex < titleIndex &&
            labelBlock.contains("fontSize = 14.sp") &&
            labelBlock.contains("LisaBlueDark") &&
            labelBlock.contains("TextAlign.Center")
    }

    fun uiRemainsMinimal(): Boolean {
        val components = readComponents() ?: return false
        val labelBlockStart = components.indexOf("fun GuidedLessonProgressLabel")
        if (labelBlockStart < 0) return false
        val labelBlockEnd = components.indexOf("fun GuidedLessonPhraseTitle", labelBlockStart)
        val labelBlock = if (labelBlockEnd > 0) {
            components.substring(labelBlockStart, labelBlockEnd)
        } else {
            components.substring(labelBlockStart)
        }
        return !labelBlock.contains("LinearProgressIndicator") &&
            !labelBlock.contains("%") &&
            labelBlock.contains("\"Lesson \$current of \$total\"")
    }

    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/GuidedLessonProgressLabelAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaGuidedLessonProgressLabelV1")
    }

    private fun readCatalog(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/lessons/TrainingLessonCatalog.kt"
    )

    private fun readFlow(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/GuidedTrainingFlow.kt"
    )

    private fun readScreens(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingLessonScreens.kt"
    )

    private fun readComponents(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingComponents.kt"
    )
}
