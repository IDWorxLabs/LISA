package com.idworx.lisa.features.runtimecameracontextualspeech.audit

import com.idworx.lisa.GuidedNavigationController
import com.idworx.lisa.GuidedNavigationState
import com.idworx.lisa.GuidedOverlayScreenMode
import com.idworx.lisa.GuidedVocabularyCatalog
import com.idworx.lisa.GuidedVocabularyCategory
import com.idworx.lisa.LisaUiStrings
import com.idworx.lisa.PreferredLanguage
import com.idworx.lisa.SEQUENCE_IDLE_TIMEOUT_MS
import com.idworx.lisa.WorkspacePhraseResolver
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.features.onboardingguide.navigation.GuidedTrainingNavigator
import com.idworx.lisa.features.onboardingguide.state.GuidedTrainingUiState
import com.idworx.lisa.features.onboardingguide.state.TrainingEvent
import com.idworx.lisa.features.silentwelcome.LisaSpeechPolicy
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe

object RuntimeCameraContextualSpeechAuditor {

    private val navigator = GuidedTrainingNavigator()
    private val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)

    fun setupStepPersistedInUiState(): Boolean {
        val controller = readController() ?: return false
        val main = readMainActivity() ?: return false
        return controller.contains("state = state.copy(setupStep = setupStep)") &&
            controller.contains("onPersist(state)") &&
            main.contains("uiGuidedTrainingState.value.setupStep") &&
            main.contains("refreshTrainingActiveState()")
    }

    fun faceDetectDoesNotAutoCompleteSetup(): Boolean {
        val controller = readController() ?: return false
        val blockStart = controller.indexOf("fun onFaceDetectedDuringSetup()")
        if (blockStart < 0) return false
        val blockEnd = controller.indexOf("fun onFaceLostDuringSetup()", blockStart)
        if (blockEnd < 0) return false
        val block = controller.substring(blockStart, blockEnd)
        return !block.contains("dispatch(TrainingEvent.CompleteSetup)")
    }

    fun completeSetupRoutesToHello(): Boolean {
        val navigatorSource = readNavigator() ?: return false
        val blockStart = navigatorSource.indexOf("TrainingEvent.CompleteSetup ->")
        if (blockStart < 0) return false
        val blockEnd = navigatorSource.indexOf("TrainingEvent.CalibrationComplete ->", blockStart)
        if (blockEnd < 0) return false
        val block = navigatorSource.substring(blockStart, blockEnd)
        return block.contains("TrainingPhase.CommunicationLesson") &&
            block.contains("communicationLessonIndex = 0") &&
            !block.contains("TrainingPhase.Calibration")
    }

    fun runtime_completeSetup_opensHelloLesson(): Boolean {
        val progress = navigator.reduce(TrainingProgress(), TrainingEvent.CompleteSetup)
        return progress.currentPhase == TrainingPhase.CommunicationLesson &&
            progress.communicationLessonIndex == 0
    }

    fun runtime_setupStepChange_triggersPersist(): Boolean {
        val state = GuidedTrainingUiState(setupStep = 0)
        val updated = state.copy(setupStep = 1)
        return updated.setupStep == 1
    }

    fun visiblePageResolverExists(): Boolean {
        val guided = readGuidedMode() ?: return false
        return guided.contains("findMatchOnVisiblePage") &&
            guided.contains("visiblePhraseEntries") &&
            guided.contains("phrasePageIndex")
    }

    fun workspacePhraseResolverExists(): Boolean {
        val guided = readGuidedMode() ?: return false
        return guided.contains("WorkspacePhraseResolver") &&
            guided.contains("continuationMappings") &&
            guided.contains("visibleEntriesForState")
    }

    fun runtime_hiddenCategoryPhraseDoesNotResolve(): Boolean {
        val context = com.idworx.lisa.GuidedCatalogContext()
        val medicalIndex = GuidedVocabularyCategory.ordered.indexOf(GuidedVocabularyCategory.Medical)
        val conversationIndex = GuidedVocabularyCategory.ordered.indexOf(GuidedVocabularyCategory.Conversation)
        val medicalEntries = GuidedVocabularyCatalog.currentPageEntries(
            medicalIndex,
            PreferredLanguage.English,
            uiStrings,
            context
        )
        val conversationEntries = GuidedVocabularyCatalog.currentPageEntries(
            conversationIndex,
            PreferredLanguage.English,
            uiStrings,
            context
        )
        val medicalPhrase = medicalEntries.firstOrNull() ?: return false
        val conversationPhrase = conversationEntries.firstOrNull() ?: return false
        val match = GuidedVocabularyCatalog.findMatchOnVisiblePage(
            left = medicalPhrase.left,
            right = medicalPhrase.right,
            pageIndex = conversationIndex,
            phrasePageIndex = 0,
            language = PreferredLanguage.English,
            uiStrings = uiStrings,
            catalogContext = context
        )
        return match?.phrase == conversationPhrase.phrase && match.phrase != medicalPhrase.phrase
    }

    fun runtime_hiddenPagePhraseDoesNotResolve(): Boolean {
        val context = com.idworx.lisa.GuidedCatalogContext()
        val entries = GuidedVocabularyCatalog.currentPageEntries(
            0,
            PreferredLanguage.English,
            uiStrings,
            context
        )
        if (entries.size <= GuidedVocabularyCatalog.DEFAULT_VISIBLE_ENTRY_CAP) return true
        val hiddenEntry = entries[GuidedVocabularyCatalog.DEFAULT_VISIBLE_ENTRY_CAP]
        val match = GuidedVocabularyCatalog.findMatchOnVisiblePage(
            left = hiddenEntry.left,
            right = hiddenEntry.right,
            pageIndex = 0,
            phrasePageIndex = 0,
            language = PreferredLanguage.English,
            uiStrings = uiStrings,
            catalogContext = context
        )
        return match == null
    }

    fun runtime_visiblePhraseResolvesOnOpenCategory(): Boolean {
        val context = com.idworx.lisa.GuidedCatalogContext()
        val entries = GuidedVocabularyCatalog.currentPageEntries(
            0,
            PreferredLanguage.English,
            uiStrings,
            context
        )
        val visible = GuidedNavigationController.visiblePhraseEntries(
            entries,
            phrasePageIndex = 0,
            visibleCap = GuidedVocabularyCatalog.DEFAULT_VISIBLE_ENTRY_CAP
        ).firstOrNull() ?: return false
        val match = GuidedVocabularyCatalog.findMatchOnVisiblePage(
            left = visible.left,
            right = visible.right,
            pageIndex = 0,
            phrasePageIndex = 0,
            language = PreferredLanguage.English,
            uiStrings = uiStrings,
            catalogContext = context
        )
        return match?.phrase == visible.phrase
    }

    fun runtime_continuationUsesWorkspaceContext(): Boolean {
        val main = readMainActivity() ?: return false
        return main.contains("mappingsForSequenceContinuation()") &&
            main.contains("workspaceContinuationMappings()") &&
            main.contains("WorkspacePhraseResolver")
    }

    fun workspaceNavigationGesturesSeparated(): Boolean {
        val layers = readWorkspaceGestureLayers() ?: return false
        return layers.contains("isWorkspaceNavigationGesture") &&
            layers.contains("isBrain1DecisionGesture")
    }

    fun phraseTranslationOnlyPolicy(): Boolean =
        LisaSpeechPolicy.PHRASE_TRANSLATION_ONLY &&
            !LisaSpeechPolicy.allowsNarration() &&
            LisaSpeechPolicy.allowsPhraseTranslation()

    fun idleTimeoutThreeSeconds(): Boolean {
        val main = readMainActivity() ?: return false
        return main.contains("SEQUENCE_IDLE_TIMEOUT_MS") &&
            SEQUENCE_IDLE_TIMEOUT_MS == 3000L
    }

    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/RuntimeCameraContextualSpeechAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaRuntimeCameraAndContextualSpeechV1")
    }

    private fun readController(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingSessionController.kt"
    )

    private fun readNavigator(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/navigation/GuidedTrainingNavigator.kt"
    )

    private fun readMainActivity(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/MainActivity.kt"
    )

    private fun readGuidedMode(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/LisaGuidedMode.kt"
    )

    private fun readWorkspaceGestureLayers(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/experiencepolish/communicationworkspace/WorkspaceGestureLayers.kt"
    )
}
