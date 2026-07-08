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

    fun categoriesGestureOpensCategoriesDuringNavigationTraining(): Boolean =
        NavigationTrainingGestureHandler.resolveAction(
            GuidedModeNavigation.CATEGORIES_LEFT,
            GuidedModeNavigation.CATEGORIES_RIGHT
        ) == NavigationAction.OpenCategories

    /**
     * The Categories gesture (currently L3 R0) must never collide with a default-language
     * vocabulary phrase mapping. An earlier revision briefly shared this exact sequence with
     * the legacy "good_morning" phrase — the same shadowing pattern the old L4 R4 /
     * "please_turn_me" collision had — until "good_morning" was reassigned to a conflict-free
     * gesture (see [com.idworx.lisa.LisaDefaultLanguage] and
     * [com.idworx.lisa.features.gesturesequenceaudit.GestureSequenceAuditEngine.workspaceDefaultsFreeOfReservedConflicts]).
     * This reads the shared constants dynamically and also proves navigation would still win
     * defensively even if a future vocabulary change reintroduced a collision.
     */
    fun categoriesGestureDoesNotResolveToShadowedLegacyPhrase(): Boolean {
        val left = GuidedModeNavigation.CATEGORIES_LEFT
        val right = GuidedModeNavigation.CATEGORIES_RIGHT
        val noLongerShadowsAVocabularyPhrase = com.idworx.lisa.defaultLanguageMappings()
            .none { it.left == left && it.right == right }
        return noLongerShadowsAVocabularyPhrase &&
            NavigationTrainingGestureHandler.blocksWorkspacePhraseResolver(left, right)
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
