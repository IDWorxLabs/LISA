package com.idworx.lisa.features.guidedcurriculumandnavigationcontext.audit

import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.features.gesturesequenceaudit.GestureSequenceAuditEngine
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.metadata.TrainingMetadata
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.navigation.GuidedTrainingNavigator
import com.idworx.lisa.features.onboardingguide.navigation.NavigationTrainingGestureHandler
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.features.onboardingguide.state.TrainingEvent
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe

object GuidedCurriculumAndNavigationContextAuditor {

    private val navigator = GuidedTrainingNavigator()

    fun phraseCurriculumCountInRange(): Boolean {
        val count = TrainingMetadata.GUIDED_LEARNING_ESSENTIAL_PHRASE_COUNT
        return count in 10..15
    }

    fun essentialPhrasesUnique(): Boolean =
        GestureSequenceAuditEngine.guidedLearningEssentialsHaveUniqueSequences()

    fun essentialPhrasesMatchCatalogOrder(): Boolean {
        val catalogIds = TrainingLessonCatalog.communicationFundamentals
            .take(TrainingMetadata.GUIDED_LEARNING_ESSENTIAL_PHRASE_COUNT)
            .map { it.vocabularyId }
        return catalogIds == TrainingMetadata.GUIDED_LEARNING_ESSENTIAL_VOCABULARY_IDS
    }

    fun navigationBeginsAfterPhraseCurriculum(): Boolean {
        val progress = TrainingProgress(
            currentPhase = TrainingPhase.CommunicationLesson,
            communicationLessonIndex = TrainingMetadata.GUIDED_LEARNING_ESSENTIAL_PHRASE_COUNT - 1,
            currentLessonSuccessCount = 0
        )
        val afterSuccess = navigator.reduce(progress, TrainingEvent.SequenceSuccess)
        return afterSuccess.currentPhase == TrainingPhase.NavigationLesson &&
            afterSuccess.navigationLessonIndex == 0
    }

    fun l4r4OpensCategoriesDuringNavigationTraining(): Boolean =
        NavigationTrainingGestureHandler.resolveAction(
            GuidedModeNavigation.CATEGORIES_LEFT,
            GuidedModeNavigation.CATEGORIES_RIGHT
        ) == NavigationAction.OpenCategories

    fun l4r4DoesNotResolveToPleaseTurnMe(): Boolean {
        val mapping = com.idworx.lisa.defaultLanguageMappings()
            .firstOrNull { it.left == 4 && it.right == 4 }
            ?: return false
        return mapping.vocabularyId == "please_turn_me" &&
            NavigationTrainingGestureHandler.blocksWorkspacePhraseResolver(4, 4)
    }

    fun navigationTrainingBlocksPhraseResolverInMainActivity(): Boolean {
        val main = readMainActivity() ?: return false
        return main.contains("handleNavigationTrainingSequence") &&
            main.contains("isNavigationTrainingActive()") &&
            main.indexOf("if (trainingSession.isNavigationTrainingActive())") <
            main.indexOf("communicationReliability.evaluatePhrasePath")
    }

    fun contextPriorityEnforced(): Boolean {
        val main = readMainActivity() ?: return false
        val navTraining = main.indexOf("if (trainingSession.isNavigationTrainingActive())")
        val phrasePath = main.indexOf("communicationReliability.evaluatePhrasePath")
        if (navTraining < 0 || phrasePath < 0) return false
        return navTraining < phrasePath
    }

    fun phraseResolverResumesAfterTraining(): Boolean {
        val main = readMainActivity() ?: return false
        val controller = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingSessionController.kt"
        ) ?: return false
        return main.contains("evaluatePhrasePath") &&
            main.contains("shouldShowTraining()") &&
            controller.contains("blocksMainUi()") &&
            controller.contains("isNavigationTrainingActive()")
    }

    fun navigationHandlerExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/navigation/NavigationTrainingGestureHandler.kt"
        )

    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/GuidedCurriculumAndNavigationContextAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaGuidedCurriculumAndNavigationContextV1")
    }

    private fun readMainActivity(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/MainActivity.kt"
    )
}
