package com.idworx.lisa

import com.idworx.lisa.features.guidedcategorypagenavigation.CategoryPageNavigationAuthority
import com.idworx.lisa.features.guidedemergencylesson.GuidedEmergencyLessonAuthority
import com.idworx.lisa.features.guidedlessonexecutionauthority.GuidedLessonExecutionAuthority
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceHighlightTarget
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceTrainingSpec
import com.idworx.lisa.features.onboardingguide.ui.NavigationLessonContent
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC8.26 — Real Page Navigation Lessons (20–21) and Emergency Active blink feedback (22).
 */
class Rc8_26RealPageNavigationAndEmergencyBlinkFeedbackTest {

    private val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun read(pathUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$pathUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source: $path")
    }

    private fun lesson20(): com.idworx.lisa.features.onboardingguide.model.NavigationLesson =
        TrainingLessonCatalog.navigationLessons.first { it.id == CategoryPageNavigationAuthority.ID_NEXT_PAGE }

    private fun lesson21(): com.idworx.lisa.features.onboardingguide.model.NavigationLesson =
        TrainingLessonCatalog.navigationLessons.first { it.id == CategoryPageNavigationAuthority.ID_PREVIOUS_PAGE }

    private fun lesson22(): com.idworx.lisa.features.onboardingguide.model.NavigationLesson =
        TrainingLessonCatalog.navigationLessons.first { it.id == GuidedEmergencyLessonAuthority.ID_EMERGENCY }

    private fun page1State(pageCount: Int = 2): GuidedNavigationState =
        GuidedNavigationState(
            screenMode = GuidedOverlayScreenMode.CategoryMenu,
            categoryViewportPage = 0,
            categoryViewportPageCount = pageCount,
            categoryMenuSelection = 0,
            categoryNavigationCause = CategoryNavigationCause.MENU_RESTORE
        )

    private fun page2State(pageCount: Int = 2): GuidedNavigationState =
        GuidedNavigationState(
            screenMode = GuidedOverlayScreenMode.CategoryMenu,
            categoryViewportPage = 1,
            categoryViewportPageCount = pageCount,
            categoryMenuSelection = GuidedVocabularyCategory.PAGE_COUNT - 1,
            categoryNavigationCause = CategoryNavigationCause.PAGE_MOVEMENT
        )

    // --- Lesson 20 card / highlight -------------------------------------------------------------

    @Test
    fun lesson20CardDisplaysL0R4NotL0R2() {
        val gesture = GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(lesson20().action)
        assertEquals("L0 R4", gesture)
        assertNotEquals("L0 R2", gesture)
        assertFalse(gesture.contains("L0 R2"))
        assertEquals(
            CategoryPageNavigationAuthority.nextPageSequenceLabel(),
            gesture
        )
        val instruction = GuidedWorkspaceTrainingSpec.lessonCardInstruction(lesson20().action)
        assertEquals("Move to the next page.", instruction)
        assertEquals("Next Page", GuidedWorkspaceTrainingSpec.lessonCardTitle(lesson20().action, uiStrings))
        val flowInstruction = NavigationLessonContent.instruction(lesson20().action, uiStrings)
        assertTrue(flowInstruction.contains("L0 R4"))
        assertFalse(flowInstruction.contains("L0 R2"))
    }

    @Test
    fun lesson20HighlightsNextPageControlNotMoveDown() {
        assertEquals(
            GuidedWorkspaceHighlightTarget.CategoryNextPage,
            GuidedWorkspaceTrainingSpec.highlightTargetFor(NavigationAction.NextPage)
        )
        assertNotEquals(
            GuidedWorkspaceHighlightTarget.NextPage,
            GuidedWorkspaceTrainingSpec.highlightTargetFor(NavigationAction.NextPage)
        )
        val ui = read("LisaGuidedModeUi.kt")
        assertTrue(
            ui.contains(
                "GuidedPanelActionKind.NextCategoryPage ->\n" +
                    "            highlightTarget == GuidedWorkspaceHighlightTarget.CategoryNextPage"
            ) || ui.contains(
                "highlightTarget == GuidedWorkspaceHighlightTarget.CategoryNextPage"
            )
        )
        assertTrue(
            ui.contains(
                "GuidedPanelActionKind.ScrollDown -> highlightTarget == GuidedWorkspaceHighlightTarget.NextPage"
            )
        )
        val nextPagePanel = GuidedNavigationPanelSpec.panelActions(
            uiStrings,
            GuidedNavigationPanelSpec.PanelContext.CategoryMenu
        ).first { it.kind == GuidedPanelActionKind.NextCategoryPage }
        assertEquals("L0 R4", nextPagePanel.sequenceLabel)
        assertEquals(uiStrings.guidedNextCategoryPage, nextPagePanel.title)
        val moveDown = GuidedNavigationPanelSpec.panelActions(
            uiStrings,
            GuidedNavigationPanelSpec.PanelContext.CategoryMenu
        ).first { it.kind == GuidedPanelActionKind.ScrollDown }
        assertEquals("L0 R2", moveDown.sequenceLabel)
    }

    // --- Lesson 20 production page change -------------------------------------------------------

    @Test
    fun lesson20BeginsOnPage1AndL0R4ProducesNextPageResult() {
        val before = page1State()
        assertTrue(CategoryPageNavigationAuthority.isNextPageStartState(before))
        assertEquals(1, CategoryPageNavigationAuthority.displayPageNumber(before))
        val after = GuidedNavigationController.nextCategoryPage(before)
        assertEquals(1, after.categoryViewportPage)
        assertEquals(2, CategoryPageNavigationAuthority.displayPageNumber(after))
        assertEquals(CategoryNavigationCause.PAGE_MOVEMENT, after.categoryNavigationCause)
        assertNotEquals(
            CategoryPageNavigationAuthority.visiblePageContentIdentity(before),
            CategoryPageNavigationAuthority.visiblePageContentIdentity(after)
        )
        assertTrue(
            CategoryPageNavigationAuthority.isNextPageCompleted(
                before,
                after,
                GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT,
                GuidedModeNavigation.NEXT_CATEGORY_PAGE_RIGHT
            )
        )
        val result = CategoryPageNavigationAuthority.evaluate(
            before,
            after,
            GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT,
            GuidedModeNavigation.NEXT_CATEGORY_PAGE_RIGHT,
            executionIdentifier = 26L
        )
        assertNotNull(result)
        assertEquals(CategoryPageNavigationAuthority.PageNavigationAction.NEXT_PAGE, result!!.navigationAction)
        assertEquals(0, result.previousPageIndex)
        assertEquals(1, result.resultingPageIndex)
        assertEquals("L0 R4", result.sequenceUsed)
        assertEquals(26L, result.executionIdentifier)
    }

    @Test
    fun lesson20MoveDownDoesNotCompleteAndLabelOnlyChangeIsRejected() {
        val before = page1State()
        val movedDown = GuidedNavigationController.moveCategorySelectionDown(before)
        assertEquals(0, movedDown.categoryViewportPage)
        assertEquals(CategoryNavigationCause.ITEM_MOVEMENT, movedDown.categoryNavigationCause)
        assertFalse(
            CategoryPageNavigationAuthority.isNextPageCompleted(
                before,
                movedDown,
                GuidedModeNavigation.NEXT_LEFT,
                GuidedModeNavigation.NEXT_RIGHT
            )
        )
        // Label-only: page index bumped without PAGE_MOVEMENT / content identity.
        val labelOnly = before.copy(categoryViewportPage = 1)
        assertFalse(
            CategoryPageNavigationAuthority.isNextPageCompleted(
                before,
                labelOnly,
                GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT,
                GuidedModeNavigation.NEXT_CATEGORY_PAGE_RIGHT
            )
        )
        val main = read("MainActivity.kt")
        assertTrue(main.contains("CategoryPageNavigationAuthority.matchesNextPage"))
        assertTrue(main.contains("pageAuthority.isNextPageCompleted"))
        assertTrue(main.contains("NavigationAction.NextPage ->"))
    }

    @Test
    fun lesson20SuccessPreservesPage2ForLesson21() {
        val afterNext = GuidedNavigationController.nextCategoryPage(page1State())
        assertTrue(CategoryPageNavigationAuthority.isPreviousPageStartState(afterNext))
        assertEquals(2, CategoryPageNavigationAuthority.displayPageNumber(afterNext))
        // Prep must not call previousCategoryPage / nextCategoryPage on Lesson 21 entry.
        val main = read("MainActivity.kt")
        val prep = main.substringAfter("CategoryPageNavigationAuthority.ID_PREVIOUS_PAGE")
            .substringBefore("GuidedEmergencyLessonAuthority.ID_EMERGENCY")
        assertFalse(prep.contains("previousCategoryPage("))
        assertFalse(prep.contains("nextCategoryPage("))
        assertTrue(prep.contains("isPreviousPageStartState"))
    }

    // --- Lesson 21 ------------------------------------------------------------------------------

    @Test
    fun lesson21CardDisplaysL4R0NotL2R0() {
        val gesture = GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(lesson21().action)
        assertEquals("L4 R0", gesture)
        assertNotEquals("L2 R0", gesture)
        assertEquals(
            CategoryPageNavigationAuthority.previousPageSequenceLabel(),
            gesture
        )
        assertEquals("Move to the previous page.", GuidedWorkspaceTrainingSpec.lessonCardInstruction(lesson21().action))
        assertEquals("Previous Page", GuidedWorkspaceTrainingSpec.lessonCardTitle(lesson21().action, uiStrings))
        val flowInstruction = NavigationLessonContent.instruction(lesson21().action, uiStrings)
        assertTrue(flowInstruction.contains("L4 R0"))
        assertFalse(flowInstruction.contains("L2 R0"))
    }

    @Test
    fun lesson21HighlightsPreviousPageControlNotMoveUp() {
        assertEquals(
            GuidedWorkspaceHighlightTarget.CategoryPreviousPage,
            GuidedWorkspaceTrainingSpec.highlightTargetFor(NavigationAction.PreviousPage)
        )
        assertNotEquals(
            GuidedWorkspaceHighlightTarget.PreviousPage,
            GuidedWorkspaceTrainingSpec.highlightTargetFor(NavigationAction.PreviousPage)
        )
        val previousPagePanel = GuidedNavigationPanelSpec.panelActions(
            uiStrings,
            GuidedNavigationPanelSpec.PanelContext.CategoryMenu
        ).first { it.kind == GuidedPanelActionKind.PreviousCategoryPage }
        assertEquals("L4 R0", previousPagePanel.sequenceLabel)
        assertEquals(uiStrings.guidedPreviousCategoryPage, previousPagePanel.title)
    }

    @Test
    fun lesson21BeginsOnPage2AndL4R0ProducesPreviousPageResult() {
        val before = page2State()
        assertTrue(CategoryPageNavigationAuthority.isPreviousPageStartState(before))
        assertEquals(2, CategoryPageNavigationAuthority.displayPageNumber(before))
        val after = GuidedNavigationController.previousCategoryPage(before)
        assertEquals(0, after.categoryViewportPage)
        assertEquals(1, CategoryPageNavigationAuthority.displayPageNumber(after))
        assertEquals(CategoryNavigationCause.PAGE_MOVEMENT, after.categoryNavigationCause)
        assertNotEquals(
            CategoryPageNavigationAuthority.visiblePageContentIdentity(before),
            CategoryPageNavigationAuthority.visiblePageContentIdentity(after)
        )
        assertTrue(
            CategoryPageNavigationAuthority.isPreviousPageCompleted(
                before,
                after,
                GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_LEFT,
                GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_RIGHT
            )
        )
        val moveUp = GuidedNavigationController.moveCategorySelectionUp(before)
        assertEquals(1, moveUp.categoryViewportPage)
        assertFalse(
            CategoryPageNavigationAuthority.isPreviousPageCompleted(
                before,
                moveUp,
                GuidedModeNavigation.PREVIOUS_LEFT,
                GuidedModeNavigation.PREVIOUS_RIGHT
            )
        )
        val labelOnly = before.copy(categoryViewportPage = 0)
        assertFalse(
            CategoryPageNavigationAuthority.isPreviousPageCompleted(
                before,
                labelOnly,
                GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_LEFT,
                GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_RIGHT
            )
        )
    }

    // --- Production mapping regression ----------------------------------------------------------

    @Test
    fun productionGestureMappingsRemainDistinct() {
        assertTrue(GuidedModeNavigation.isPreviousSequence(2, 0))
        assertTrue(GuidedModeNavigation.isNextSequence(0, 2))
        assertTrue(GuidedModeNavigation.isPreviousCategoryPageSequence(4, 0))
        assertTrue(GuidedModeNavigation.isNextCategoryPageSequence(0, 4))
        assertFalse(GuidedModeNavigation.isNextCategoryPageSequence(0, 2))
        assertFalse(GuidedModeNavigation.isPreviousCategoryPageSequence(2, 0))
        assertEquals(2, GuidedModeNavigation.PREVIOUS_LEFT)
        assertEquals(0, GuidedModeNavigation.PREVIOUS_RIGHT)
        assertEquals(0, GuidedModeNavigation.NEXT_LEFT)
        assertEquals(2, GuidedModeNavigation.NEXT_RIGHT)
        assertEquals(4, GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_LEFT)
        assertEquals(0, GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_RIGHT)
        assertEquals(0, GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT)
        assertEquals(4, GuidedModeNavigation.NEXT_CATEGORY_PAGE_RIGHT)
        assertTrue(GuidedLessonExecutionAuthority.requiresProductionStateGate(NavigationAction.NextPage))
        assertTrue(GuidedLessonExecutionAuthority.requiresProductionStateGate(NavigationAction.PreviousPage))
        assertFalse(GuidedLessonExecutionAuthority.mayRestorePreconditionOnEntry(NavigationAction.NextPage))
        // Outside guided: production controller still pages.
        val outside = GuidedNavigationController.processSequence(
            GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT,
            GuidedModeNavigation.NEXT_CATEGORY_PAGE_RIGHT,
            page1State(),
            PreferredLanguage.English,
            uiStrings
        )
        assertTrue(outside is GuidedSequenceResult.Navigate)
        assertEquals(1, (outside as GuidedSequenceResult.Navigate).newState.categoryViewportPage)
    }

    // --- Lesson 22 emergency phases + Active blink feedback -------------------------------------

    @Test
    fun lesson22PhaseModelAndCompletionOnlyAfterStop() {
        assertEquals(NavigationAction.TriggerEmergency, lesson22().action)
        assertEquals(
            GuidedEmergencyLessonAuthority.Phase.AwaitEmergencyTrigger,
            GuidedEmergencyLessonAuthority.phase(
                emergencyAwaitingConfirm = false,
                emergencyActive = false
            )
        )
        assertEquals(
            GuidedEmergencyLessonAuthority.Phase.AwaitEmergencyConfirmation,
            GuidedEmergencyLessonAuthority.phase(
                emergencyAwaitingConfirm = true,
                emergencyActive = false
            )
        )
        assertEquals(
            GuidedEmergencyLessonAuthority.Phase.AwaitEmergencyStop,
            GuidedEmergencyLessonAuthority.phase(
                emergencyAwaitingConfirm = false,
                emergencyActive = true
            )
        )
        assertTrue(
            GuidedEmergencyLessonAuthority.mayCompleteAfterStop(
                wasEmergencyActive = true,
                isEmergencyActiveNow = false
            )
        )
        assertFalse(
            GuidedEmergencyLessonAuthority.mayCompleteAfterStop(
                wasEmergencyActive = false,
                isEmergencyActiveNow = false
            )
        )
        assertTrue(
            GuidedEmergencyLessonAuthority.isPreExecuteForbiddenAtEntry(
                emergencyAwaitingConfirm = false,
                emergencyActive = false
            )
        )
        val main = read("MainActivity.kt")
        assertTrue(main.contains("GuidedEmergencyLessonAuthority.mayCompleteAfterStop"))
        assertTrue(main.contains("verifyTrainingNavigation(NavigationAction.TriggerEmergency)"))
        val cancelOrStop = main.substringAfter("private fun cancelOrStopEmergency()")
            .substringBefore("private fun refreshCameraPermissionState")
        assertTrue(cancelOrStop.contains("wasEmergencyActive"))
        // Confirm-only must not complete — completion is gated on stoppingActiveAlarm.
        assertTrue(cancelOrStop.contains("mayCompleteAfterStop"))
    }

    @Test
    fun emergencyActiveShowsLiveBlinkCountersAndStopL1R1() {
        val emergency = read("LisaEmergencyUi.kt")
        val alarm = emergency.substringAfter("fun EmergencyAlarmOverlay(")
            .substringBefore("fun Brain1EmergencyConfirmOverlay(")
        assertTrue(alarm.contains("blinkFeedback"))
        assertTrue(alarm.contains("EmergencyBlinkFeedbackRows"))
        assertTrue(alarm.contains("eyeTrackingStatusWatching"))
        assertTrue(alarm.contains("stopEmergency"))
        assertTrue(alarm.contains("guidedEmergencyStopSequenceLabel"))
        assertFalse(alarm.contains("guidedConfirmCancelSequenceLabel"))
        assertTrue(alarm.contains("emergencyActiveTitle"))
        assertTrue(alarm.contains("emergencyAlarmActiveMessage"))
        assertTrue(alarm.contains("callingForHelp"))
        assertTrue(emergency.contains("BlinkCounterRow("))
        assertTrue(emergency.contains("leftWinkCount"))
        assertTrue(emergency.contains("rightWinkCount"))
        val layer = emergency.substringAfter("fun GlobalEmergencyOverlayLayer(")
            .substringBefore("fun EmergencyAlarmOverlay(")
        assertTrue(layer.contains("blinkFeedback = blinkFeedback"))
        val main = read("MainActivity.kt")
        assertTrue(main.contains("processActiveEmergencyStopWinks"))
        val stopWinks = main.substringAfter("private fun processActiveEmergencyStopWinks")
            .substringBefore("private fun processSequenceWinks")
        assertTrue(stopWinks.contains("isConfirm("))
        assertTrue(stopWinks.contains("recordWinkSide"))
        assertTrue(main.contains("resetSequence()"))
        // Outside guided learning uses the same overlay with blinkFeedback.
        val accessibility = read("LisaAccessibilityUi.kt")
        assertTrue(accessibility.contains("GlobalEmergencyOverlayLayer("))
        assertTrue(accessibility.contains("blinkFeedback = composerEyeFeedback"))
    }

    @Test
    fun noPreExecutePageOrEmergencyAtLessonEntry() {
        assertFalse(GuidedLessonExecutionAuthority.mayRestorePreconditionOnEntry(NavigationAction.NextPage))
        assertFalse(GuidedLessonExecutionAuthority.mayRestorePreconditionOnEntry(NavigationAction.PreviousPage))
        assertFalse(GuidedLessonExecutionAuthority.mayRestorePreconditionOnEntry(NavigationAction.TriggerEmergency))
        val main = read("MainActivity.kt")
        val nextPrep = main.substringAfter("CategoryPageNavigationAuthority.ID_NEXT_PAGE")
            .substringBefore("CategoryPageNavigationAuthority.ID_PREVIOUS_PAGE")
        assertFalse(nextPrep.contains("nextCategoryPage("))
        assertTrue(nextPrep.contains("categoryViewportPage = 0"))
        assertTrue(nextPrep.contains("MENU_RESTORE"))
        val emergencyPrep = main.substringAfter("GuidedEmergencyLessonAuthority.ID_EMERGENCY")
            .substringBefore("else -> Unit")
        assertFalse(emergencyPrep.contains("beginEmergencyConfirm("))
        assertFalse(emergencyPrep.contains("startEmergencyMode("))
        assertFalse(emergencyPrep.contains("cancelOrStopEmergency("))
    }

    @Test
    fun catalogueLessons20To22Unchanged() {
        assertEquals(CategoryPageNavigationAuthority.ID_NEXT_PAGE, lesson20().id)
        assertEquals(CategoryPageNavigationAuthority.ID_PREVIOUS_PAGE, lesson21().id)
        assertEquals(GuidedEmergencyLessonAuthority.ID_EMERGENCY, lesson22().id)
        assertEquals(NavigationAction.NextPage, lesson20().action)
        assertEquals(NavigationAction.PreviousPage, lesson21().action)
        assertEquals(NavigationAction.TriggerEmergency, lesson22().action)
        // Lesson 16 still teaches Move Down L0 R2 via NextPage highlight target.
        assertEquals(
            GuidedWorkspaceHighlightTarget.NextPage,
            GuidedWorkspaceTrainingSpec.highlightTargetFor(NavigationAction.MoveToMedicalCategory)
        )
        assertEquals(
            "L0 R2",
            GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(NavigationAction.MoveToMedicalCategory)
        )
    }
}
