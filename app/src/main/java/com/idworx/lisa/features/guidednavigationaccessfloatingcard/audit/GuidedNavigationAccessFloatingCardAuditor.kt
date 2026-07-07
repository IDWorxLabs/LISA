package com.idworx.lisa.features.guidednavigationaccessfloatingcard.audit

import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.GuidedVocabularyOverlayVisibility
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.features.onboardingguide.navigation.GuidedTrainingNavigator
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceHighlightTarget
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceLessonCardDock
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceTrainingSpec
import com.idworx.lisa.features.onboardingguide.state.TrainingEvent
import com.idworx.lisa.features.realworkspaceguidednavigationtraining.validation.RealWorkspaceGuidedNavigationTrainingAuthorityV1
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import com.idworx.lisa.isEmergencySequence
import com.idworx.lisa.validation.ValidationOutcome

object GuidedNavigationAccessFloatingCardAuditor {

    private val navigator = GuidedTrainingNavigator()

    // --- 1. Welcome/setup exposes Skip to Navigation Training -----------------------------------
    fun welcomeExposesSkipToNavigationTraining(): Boolean {
        val welcome = readWelcomeScreen() ?: return false
        val flow = readGuidedTrainingFlow() ?: return false
        return welcome.contains("onSkipToNavigationTraining") &&
            welcome.contains("Skip to Navigation Training") &&
            flow.contains("onSkipToNavigationTraining = { onEvent(TrainingEvent.SkipToNavigationTraining) }")
    }

    // --- 2. Skip to Navigation Training starts at Lesson 16 -------------------------------------
    fun skipStartsAtLesson16(): Boolean {
        val progress = navigator.reduce(TrainingProgress(), TrainingEvent.SkipToNavigationTraining)
        val atLesson16 = progress.currentPhase == TrainingPhase.NavigationLesson &&
            progress.navigationLessonIndex == 0
        val lessonProgress = TrainingLessonCatalog.guidedLessonProgress(progress)
        return atLesson16 && lessonProgress == 16 to 23
    }

    // --- 3. It enters real workspace GUIDED_TRAINING mode ----------------------------------------
    fun entersRealWorkspaceGuidedTrainingMode(): Boolean {
        val progress = navigator.reduce(TrainingProgress(), TrainingEvent.SkipToNavigationTraining)
        val guidedWorkspaceTrainingActive = progress.currentPhase == TrainingPhase.NavigationLesson
        val workspaceVisible = GuidedVocabularyOverlayVisibility.shouldShowOverlay(
            onboardingCompleted = false,
            cameraPermissionGranted = true,
            emergencyActive = false,
            practiceModeOpen = false,
            quickControlsOpen = false,
            guidedWorkspaceTrainingActive = guidedWorkspaceTrainingActive
        )
        val ui = readAccessibilityUi() ?: return false
        return guidedWorkspaceTrainingActive && workspaceVisible &&
            ui.contains("GuidedWorkspaceMode.GUIDED_TRAINING") &&
            ui.contains("guidedWorkspaceTrainingActive =")
    }

    // --- 4. It bypasses the 15 phrase lessons -----------------------------------------------------
    fun bypassesPhraseLessons(): Boolean {
        val progress = navigator.reduce(TrainingProgress(), TrainingEvent.SkipToNavigationTraining)
        val neverVisitedPhraseLessons = progress.communicationLessonIndex == 0 &&
            progress.completedLessonIds.isEmpty() &&
            progress.currentPhase == TrainingPhase.NavigationLesson
        val navigatorSource = readNavigator() ?: return false
        val skipBlockStart = navigatorSource.indexOf("TrainingEvent.SkipToNavigationTraining ->")
        val skipBlockEnd = navigatorSource.indexOf("TrainingEvent.ReturnToTutorial ->", skipBlockStart)
        if (skipBlockStart < 0 || skipBlockEnd < 0 || skipBlockEnd <= skipBlockStart) return false
        val skipBlock = navigatorSource.substring(skipBlockStart, skipBlockEnd)
        val routesDirectlyToNavigationLesson = skipBlock.contains("TrainingPhase.NavigationLesson") &&
            !skipBlock.contains("TrainingPhase.CommunicationLesson") &&
            !skipBlock.contains("TrainingPhase.Setup")
        return neverVisitedPhraseLessons && routesDirectlyToNavigationLesson
    }

    // --- 5. Lesson card is not rendered behind the Listening banner -----------------------------
    fun lessonCardNotBehindListeningBanner(): Boolean {
        val ui = readAccessibilityUi() ?: return false
        val bannerIndex = ui.indexOf("EverydayCommunicationPanel(")
        val cardIndex = ui.indexOf("GuidedWorkspaceLessonCard(")
        if (bannerIndex < 0 || cardIndex < 0) return false
        // The card must be composed *after* the top Listening banner in the same Box, so it is
        // drawn on top of (not behind) it, and it must dock at the bottom, never the top.
        val cardRenderedAfterBanner = cardIndex > bannerIndex
        val guardIndex = ui.lastIndexOf(
            "if (guidedWorkspaceTrainingActive && activeNavigationLesson != null)",
            cardIndex
        )
        if (guardIndex < 0) return false
        val cardBlock = ui.substring(guardIndex, cardIndex)
        val dockedAtBottomNotTop = cardBlock.contains("cardDockFor(guidedWorkspaceHighlight)") &&
            (cardBlock.contains("Alignment.BottomStart") || cardBlock.contains("Alignment.BottomEnd")) &&
            !cardBlock.contains("Alignment.TopCenter")
        // Exactly one such guard must exist — the old top-of-screen card block must be gone.
        val onlyOneCardBlock = countOccurrences(ui, "GuidedWorkspaceLessonCard(") == 1
        return cardRenderedAfterBanner && dockedAtBottomNotTop && onlyOneCardBlock
    }

    // --- 6. Lesson card remains visible/readable --------------------------------------------------
    fun lessonCardRemainsVisibleReadable(): Boolean {
        val components = readTrainingComponents() ?: return false
        val cardStart = components.indexOf("fun GuidedWorkspaceLessonCard")
        if (cardStart < 0) return false
        val nextFunctionStart = components.indexOf("@Composable", cardStart + 1)
        val cardEnd = if (nextFunctionStart > cardStart) nextFunctionStart else components.length
        val card = components.substring(cardStart, cardEnd)
        return card.contains("LisaWhite.copy(alpha = 0.98f)") &&
            card.contains("CardDefaults.cardElevation(defaultElevation = 6.dp)") &&
            card.contains("\"Lesson \$lessonNumber of \$totalLessons\"") &&
            card.contains("\"Gesture: \$gestureLabel\"")
    }

    // --- 7. Real workspace layout is not structurally changed -------------------------------------
    fun realWorkspaceLayoutNotStructurallyChanged(): Boolean {
        val overlaySource = readGuidedModeUi() ?: return false
        val ui = readAccessibilityUi() ?: return false
        val overlayStructureIntact = overlaySource.contains("GuidedCategoryMenuRow") &&
            overlaySource.contains("GuidedVocabularyEntryRow") &&
            overlaySource.contains("GuidedNavigationActionButton") &&
            overlaySource.contains("GuidedEmergencyNavButton")
        val workspaceCallUnchanged = ui.contains("GuidedVocabularyOverlay(") &&
            ui.contains(".weight(1f)") &&
            ui.contains("uiStrings.menu") &&
            ui.contains("uiStrings.reset")
        return overlayStructureIntact && workspaceCallUnchanged
    }

    // --- 8. Highlighted target remains visible (card docks on the opposite side) ------------------
    fun highlightedTargetRemainsVisible(): Boolean {
        val rightPanelTargets = listOf(
            GuidedWorkspaceHighlightTarget.Back,
            GuidedWorkspaceHighlightTarget.NextPage,
            GuidedWorkspaceHighlightTarget.PreviousPage,
            GuidedWorkspaceHighlightTarget.Emergency
        )
        val leftContentTargets = listOf(
            GuidedWorkspaceHighlightTarget.OpenCategories,
            GuidedWorkspaceHighlightTarget.CategoryRow,
            GuidedWorkspaceHighlightTarget.PhraseRow
        )
        val rightPanelDocksLeft = rightPanelTargets.all {
            GuidedWorkspaceTrainingSpec.cardDockFor(it) == GuidedWorkspaceLessonCardDock.BottomStart
        }
        val leftContentDocksRight = leftContentTargets.all {
            GuidedWorkspaceTrainingSpec.cardDockFor(it) == GuidedWorkspaceLessonCardDock.BottomEnd
        }
        val ui = readAccessibilityUi() ?: return false
        val highlightStillWired = ui.contains("trainingHighlight = guidedWorkspaceHighlight")
        return rightPanelDocksLeft && leftContentDocksRight && highlightStillWired
    }

    // --- 9. Gesture filtering still works -----------------------------------------------------------
    fun gestureFilteringStillWorks(): Boolean {
        val main = readMainActivity() ?: return false
        val gateIntact = main.contains("private fun acceptedByCurrentNavigationLesson") &&
            main.contains("if (!acceptedByCurrentNavigationLesson(left, right)) {")
        val openCategories = classify(GuidedModeNavigation.CATEGORIES_LEFT, GuidedModeNavigation.CATEGORIES_RIGHT)
        val emergency = classify(6, 0)
        val back = classify(GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT)
        val classificationIntact = openCategories == NavigationAction.OpenCategories &&
            emergency == NavigationAction.TriggerEmergency &&
            back == NavigationAction.CloseMenu &&
            accepted(NavigationAction.OpenCategories, GuidedModeNavigation.CATEGORIES_LEFT, GuidedModeNavigation.CATEGORIES_RIGHT) &&
            !accepted(NavigationAction.OpenCategories, GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT)
        return gateIntact && classificationIntact
    }

    // --- 10. Existing full Guided Learning flow still works ------------------------------------------
    fun existingFullGuidedLearningFlowStillWorks(): Boolean {
        val fullFlowRegressionGreen = RealWorkspaceGuidedNavigationTrainingAuthorityV1.validate().outcome ==
            ValidationOutcome.PASS
        var progress = navigator.reduce(TrainingProgress(), TrainingEvent.BeginLearning)
        val startsInSetup = progress.currentPhase == TrainingPhase.Setup
        progress = navigator.reduce(progress, TrainingEvent.CompleteSetup)
        val startsCommunicationLessons = progress.currentPhase == TrainingPhase.CommunicationLesson &&
            progress.communicationLessonIndex == 0
        return fullFlowRegressionGreen && startsInSetup && startsCommunicationLessons
    }

    // --- Infra: test class + gradle task ----------------------------------------------------------
    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/GuidedNavigationAccessFloatingCardAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaGuidedNavigationAccessAndFloatingCardV1")
    }

    /** Mirrors MainActivity's `classifyNavigationGesture` — see RealWorkspaceGuidedNavigationTrainingAuditor. */
    private fun classify(left: Int, right: Int): NavigationAction = when {
        isEmergencySequence(left, right) -> NavigationAction.TriggerEmergency
        GuidedModeNavigation.isCategoriesSequence(left, right) -> NavigationAction.OpenCategories
        GuidedModeNavigation.isBackSequence(left, right) -> NavigationAction.CloseMenu
        GuidedModeNavigation.isNextSequence(left, right) -> NavigationAction.NextPage
        GuidedModeNavigation.isPreviousSequence(left, right) -> NavigationAction.PreviousPage
        GuidedModeNavigation.isSelectSequence(left, right) -> NavigationAction.SelectCategory
        else -> NavigationAction.SelectPhrase
    }

    /** Mirrors MainActivity's `acceptedByCurrentNavigationLesson` gate. */
    private fun accepted(expected: NavigationAction, left: Int, right: Int): Boolean {
        val classified = classify(left, right)
        if (classified == expected) return true
        return (expected == NavigationAction.SelectCategory && classified == NavigationAction.SelectPhrase) ||
            (expected == NavigationAction.SelectPhrase && classified == NavigationAction.SelectCategory)
    }

    private fun countOccurrences(source: String, needle: String): Int {
        var count = 0
        var index = source.indexOf(needle)
        while (index >= 0) {
            count++
            index = source.indexOf(needle, index + needle.length)
        }
        return count
    }

    private fun readWelcomeScreen(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingWelcomeScreen.kt"
    )

    private fun readGuidedTrainingFlow(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/GuidedTrainingFlow.kt"
    )

    private fun readNavigator(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/navigation/GuidedTrainingNavigator.kt"
    )

    private fun readAccessibilityUi(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt"
    )

    private fun readGuidedModeUi(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt"
    )

    private fun readTrainingComponents(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingComponents.kt"
    )

    private fun readMainActivity(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/MainActivity.kt"
    )
}
