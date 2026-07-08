package com.idworx.lisa.features.coreruntime.alwayswelcomevisiblespeech.audit

import com.idworx.lisa.GuidedNavigationController
import com.idworx.lisa.GuidedVocabularyCatalog
import com.idworx.lisa.LisaUiStrings
import com.idworx.lisa.PreferredLanguage
import com.idworx.lisa.SEQUENCE_IDLE_TIMEOUT_MS
import com.idworx.lisa.SequenceProcessingDelay
import com.idworx.lisa.features.launchwelcomestatepriority.WelcomeStatePriorityGate
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.features.onboardingguide.navigation.GuidedTrainingNavigator
import com.idworx.lisa.features.onboardingguide.state.TrainingEvent
import com.idworx.lisa.features.silentwelcome.LisaSpeechPolicy
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe

object CoreRuntimeAlwaysWelcomeVisibleSpeechAuditor {

    fun coldLaunchGateAlwaysWelcome(): Boolean {
        val gate = readGate() ?: return false
        return gate.contains("mustShowWelcome") &&
            gate.contains("applyForColdLaunch") &&
            gate.contains("TrainingPhase.FirstLaunchChoice") &&
            gate.contains("requiresPersistMigration") &&
            gate.contains("false")
    }

    fun storeDoesNotPersistLaunchOverride(): Boolean {
        val store = readStore() ?: return false
        return store.contains("applyForColdLaunch") &&
            !store.contains("requiresPersistMigration(loaded, gated)")
    }

    fun mainActivityResetsSessionUi(): Boolean {
        val main = readMainActivity() ?: return false
        return main.contains("applyColdLaunchSessionState") &&
            main.contains("uiOnboardingCompleted.value = false") &&
            main.contains("GuidedNavigationState()")
    }

    fun trainingUiKeepsCameraAlive(): Boolean {
        val ui = readAccessibilityUi() ?: return false
        return ui.contains("cameraView()") &&
            ui.contains("alpha(0f)") &&
            ui.contains("trainingBlocksMainUi")
    }

    fun shouldShowTrainingUsesActivePhases(): Boolean {
        val controller = readController() ?: return false
        return controller.contains("ACTIVE_TRAINING_PHASES") &&
            controller.contains("currentPhase in ACTIVE_TRAINING_PHASES")
    }

    fun confirmSkipExitsTrainingFlow(): Boolean {
        val navigator = readNavigator() ?: return false
        val blockStart = navigator.indexOf("TrainingEvent.ConfirmSkip ->")
        if (blockStart < 0) return false
        val blockEnd = navigator.indexOf("TrainingEvent.ReturnToTutorial ->", blockStart)
        if (blockEnd < 0) return false
        return navigator.substring(blockStart, blockEnd).contains("TrainingPhase.Completion")
    }

    fun runtime_skippedWorkspaceState_returnsWelcome(): Boolean {
        val saved = TrainingProgress(
            tutorialSkipped = true,
            tutorialCompleted = false,
            firstLaunchChoiceMade = true,
            currentPhase = TrainingPhase.Completion
        )
        val gated = WelcomeStatePriorityGate.applyForColdLaunch(saved)
        return gated.currentPhase == TrainingPhase.FirstLaunchChoice &&
            !gated.firstLaunchChoiceMade
    }

    fun runtime_helloLessonState_returnsWelcome(): Boolean {
        val saved = TrainingProgress(
            tutorialStarted = true,
            firstLaunchChoiceMade = true,
            currentPhase = TrainingPhase.CommunicationLesson,
            communicationLessonIndex = 0
        )
        return WelcomeStatePriorityGate.applyForColdLaunch(saved).currentPhase ==
            TrainingPhase.FirstLaunchChoice
    }

    fun runtime_beginLearningRequiredForSetup(): Boolean {
        val progress = GuidedTrainingNavigator().reduce(TrainingProgress(), TrainingEvent.BeginLearning)
        return progress.currentPhase == TrainingPhase.Setup &&
            progress.firstLaunchChoiceMade
    }

    fun runtime_confirmSkipRequiredForWorkspaceExit(): Boolean {
        val progress = GuidedTrainingNavigator().reduce(
            TrainingProgress(currentPhase = TrainingPhase.SkipConfirm),
            TrainingEvent.ConfirmSkip
        )
        return progress.currentPhase == TrainingPhase.Completion &&
            progress.tutorialSkipped
    }

    fun runtime_visiblePhraseResolves(): Boolean {
        val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)
        val context = com.idworx.lisa.GuidedCatalogContext()
        val entries = GuidedVocabularyCatalog.currentPageEntries(0, PreferredLanguage.English, uiStrings, context)
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

    fun runtime_hiddenPageDoesNotResolve(): Boolean {
        val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)
        val context = com.idworx.lisa.GuidedCatalogContext()
        val entries = GuidedVocabularyCatalog.currentPageEntries(0, PreferredLanguage.English, uiStrings, context)
        if (entries.size <= GuidedVocabularyCatalog.DEFAULT_VISIBLE_ENTRY_CAP) return true
        val hidden = entries[GuidedVocabularyCatalog.DEFAULT_VISIBLE_ENTRY_CAP]
        return GuidedVocabularyCatalog.findMatchOnVisiblePage(
            left = hidden.left,
            right = hidden.right,
            pageIndex = 0,
            phrasePageIndex = 0,
            language = PreferredLanguage.English,
            uiStrings = uiStrings,
            catalogContext = context
        ) == null
    }

    fun finalizeSequenceResetsTrainingBuffer(): Boolean {
        val main = readMainActivity() ?: return false
        return main.contains("handleTrainingSequence(capturedLeft, capturedRight)") &&
            main.contains("resetSequence()") &&
            main.contains("handleGuidedOverlaySequence")
    }

    fun idleTimeoutMatchesDefaultResponseTime(): Boolean =
        SEQUENCE_IDLE_TIMEOUT_MS == SequenceProcessingDelay.toMillis(SequenceProcessingDelay.DEFAULT_SECONDS)

    fun phraseTranslationOnly(): Boolean =
        LisaSpeechPolicy.PHRASE_TRANSLATION_ONLY &&
            !LisaSpeechPolicy.allowsNarration() &&
            LisaSpeechPolicy.allowsPhraseTranslation()

    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/CoreRuntimeAlwaysWelcomeVisibleSpeechAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaCoreRuntimeAlwaysWelcomeVisibleSpeechV1")
    }

    private fun readGate(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/launchwelcomestatepriority/WelcomeStatePriorityGate.kt"
    )

    private fun readStore(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingProgressStore.kt"
    )

    private fun readMainActivity(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/MainActivity.kt"
    )

    private fun readAccessibilityUi(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt"
    )

    private fun readController(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingSessionController.kt"
    )

    private fun readNavigator(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/navigation/GuidedTrainingNavigator.kt"
    )
}
