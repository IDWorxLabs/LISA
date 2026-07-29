package com.idworx.lisa

import com.idworx.lisa.features.guidedmedicalcategoryjourney.GuidedMedicalCategoryJourneyAuthority
import com.idworx.lisa.features.guidedworkspacelessoncard.GuidedWorkspaceLessonCardAuthority
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.features.onboardingguide.navigation.GuidedTrainingFocusPolicy
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceHighlightTarget
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceTrainingSpec
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC8.17 — Guided Medical journey lessons 16–18: single selection authority, state continuity,
 * and production-gated completion.
 */
class Rc8_17GuidedMedicalJourneyStateContinuityTest {

    private val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)
    private val authority = GuidedMedicalCategoryJourneyAuthority

    private fun read(pathUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$pathUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source: $path")
    }

    // --- Lesson 16 copy -----------------------------------------------------------------

    @Test
    fun lesson16CopyExplainsWorkspacePurposeAndMoveAction() {
        val instruction = GuidedWorkspaceTrainingSpec.lessonCardInstruction(
            NavigationAction.MoveToMedicalCategory,
            authority.ID_MOVE_TO_MEDICAL
        ).orEmpty()
        assertEquals(authority.MOVE_DESCRIPTION, instruction)
        assertTrue(instruction.contains("Scroll", ignoreCase = true) || instruction.contains("open", ignoreCase = true))
        assertEquals(
            "Sequence: L0 R2",
            GuidedWorkspaceLessonCardAuthority.displayedSequenceFor(NavigationAction.MoveToMedicalCategory)
        )
    }

    // --- Lesson 16 entry / single selection ---------------------------------------------

    @Test
    fun lesson16EntryStartsWithConversationSelectedAndCategoryWorkspaceOpen() {
        val start = GuidedNavigationController.communicationWorkspaceRoot(GuidedNavigationState())
        assertTrue(authority.isLesson16StartState(start))
        assertEquals(GuidedOverlayScreenMode.CategoryMenu, start.screenMode)
        assertEquals(authority.conversationCategoryIndex, start.categoryMenuSelection)
        assertEquals(0, start.categoryMenuSelection)
        assertNotEquals(authority.medicalCategoryIndex, start.categoryMenuSelection)
        assertTrue(authority.exactlyOneCategorySelected(start.categoryMenuSelection))
        assertEquals(
            GuidedWorkspaceHighlightTarget.NextPage,
            GuidedWorkspaceTrainingSpec.highlightTargetFor(NavigationAction.MoveToMedicalCategory)
        )
    }

    @Test
    fun exactlyOneCategorySelectedAssertionHoldsAcrossLesson16Moves() {
        var state = GuidedNavigationController.communicationWorkspaceRoot(GuidedNavigationState())
        assertTrue(authority.exactlyOneCategorySelected(state.categoryMenuSelection))
        assertEquals(0, state.categoryMenuSelection)

        state = (GuidedNavigationController.processSequence(
            GuidedModeNavigation.NEXT_LEFT,
            GuidedModeNavigation.NEXT_RIGHT,
            state,
            PreferredLanguage.English,
            uiStrings
        ) as GuidedSequenceResult.Navigate).newState
        assertEquals(
            GuidedVocabularyCategory.ordered.indexOf(GuidedVocabularyCategory.BasicNeeds),
            state.categoryMenuSelection
        )
        assertTrue(authority.exactlyOneCategorySelected(state.categoryMenuSelection))
        assertFalse(authority.isMedicalSelectedInCategoryMenu(state))
        assertNotEquals(0, state.categoryMenuSelection)
        assertNotEquals(authority.medicalCategoryIndex, state.categoryMenuSelection)

        state = (GuidedNavigationController.processSequence(
            GuidedModeNavigation.NEXT_LEFT,
            GuidedModeNavigation.NEXT_RIGHT,
            state,
            PreferredLanguage.English,
            uiStrings
        ) as GuidedSequenceResult.Navigate).newState
        assertTrue(authority.isMedicalSelectedInCategoryMenu(state))
        assertTrue(authority.exactlyOneCategorySelected(state.categoryMenuSelection))
        assertEquals(authority.medicalCategoryIndex, state.categoryMenuSelection)
    }

    @Test
    fun categoryUiUsesProductionSelectionOnlyNoMedicalForceHighlight() {
        val ui = read("LisaGuidedModeUi.kt")
        assertTrue(ui.contains("index == categoryMenuSelection"))
        assertFalse(ui.contains("index == GuidedWorkspaceTrainingSpec.medicalCategoryIndex"))
        assertFalse(ui.contains("forceHighlightMedical"))
        assertFalse(ui.contains("trainingTargetHighlight"))
        assertEquals(
            GuidedWorkspaceHighlightTarget.NextPage,
            GuidedWorkspaceTrainingSpec.highlightTargetFor(NavigationAction.MoveToMedicalCategory)
        )
        assertNull(GuidedWorkspaceTrainingSpec.highlightTargetFor(NavigationAction.SelectCategory))
        // Selection styling must not OR a lesson target onto production selection.
        assertFalse(
            ui.contains("|| lessonTarget") ||
                ui.contains("|| trainingTarget") ||
                ui.contains("productionSelected")
        )
    }

    @Test
    fun lesson16EntryPreparationIsIdempotentAndGuardedInMainActivity() {
        val main = read("MainActivity.kt")
        assertTrue(main.contains("prepareMedicalJourneyLessonWorkspaceIfNeeded"))
        assertTrue(main.contains("preparedMedicalJourneyLessonId"))
        assertTrue(main.contains("if (lessonId == preparedMedicalJourneyLessonId) return"))
        assertTrue(main.contains("ID_MOVE_TO_MEDICAL"))
        assertTrue(main.contains("communicationWorkspaceRoot(GuidedNavigationState())"))
        // Must not reset on every recomposition path without the guard.
        val prep = main.substringAfter("private fun prepareMedicalJourneyLessonWorkspaceIfNeeded()")
            .substringBefore("private fun isMedicalPhraseWorkspaceOpen()")
        assertTrue(prep.contains("preparedMedicalJourneyLessonId = lessonId"))
        assertTrue(prep.contains("ID_OPEN_MEDICAL"))
        assertTrue(prep.contains("ID_USE_MEDICAL_PHRASE"))
    }

    // --- Lesson 17 ----------------------------------------------------------------------

    @Test
    fun lesson17BeginsReadyForDirectOpenWithL3R1() {
        val lesson = TrainingLessonCatalog.navigationLessons[1]
        assertEquals(NavigationAction.SelectCategory, lesson.action)
        assertEquals(authority.OPEN_DIRECT_TITLE, GuidedWorkspaceTrainingSpec.lessonCardTitle(lesson.action, uiStrings))
        assertEquals("L3 R1", GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(lesson.action))
        // RC8.25 — Lesson 17 starts from Conversation Category Menu (clean direct-open).
        val start = GuidedNavigationController.communicationWorkspaceRoot(GuidedNavigationState())
        assertTrue(authority.isLesson16StartState(start))
    }

    @Test
    fun lesson17L3R1InvokesRealProductionMedicalOpenAndStateGatesCompletion() {
        val (left, right) = authority.openMedicalGesture()
        assertEquals(3 to 1, left to right)
        assertTrue(authority.matchesOpenMedical(left, right))
        val menu = GuidedNavigationState(
            screenMode = GuidedOverlayScreenMode.CategoryMenu,
            categoryMenuSelection = authority.medicalCategoryIndex
        )
        val opened = (GuidedNavigationController.processSequence(
            left, right, menu, PreferredLanguage.English, uiStrings
        ) as GuidedSequenceResult.Navigate).newState
        assertTrue(authority.isMedicalPhraseWorkspaceOpen(opened))
        assertEquals(GuidedOverlayScreenMode.Vocabulary, opened.screenMode)
        assertEquals(authority.medicalCategoryIndex, opened.categoryIndex)

        val main = read("MainActivity.kt")
        val selectBlock = main.substringAfter("NavigationAction.SelectCategory -> {")
            .substringBefore("NavigationAction.MenuSelectVoice")
        assertTrue(selectBlock.contains("matchesOpenMedical"))
        assertTrue(selectBlock.contains("handleGuidedOverlaySequence"))
        assertTrue(selectBlock.contains("isMedicalOpenedViaDirectShortcut"))
        assertTrue(selectBlock.contains("verifyTrainingNavigation(NavigationAction.SelectCategory)"))
        // Must not verify before production open is confirmed.
        assertTrue(selectBlock.indexOf("handleGuidedOverlaySequence") <
            selectBlock.indexOf("verifyTrainingNavigation(NavigationAction.SelectCategory)"))
    }

    @Test
    fun touchingMedicalAndL3R1ResolveToSameProductionOpenAction() {
        val medical = authority.medicalCategoryIndex
        val fromTouch = GuidedNavigationController.openCategoryDirectly(
            GuidedNavigationState(screenMode = GuidedOverlayScreenMode.CategoryMenu),
            medical
        )
        val (left, right) = authority.openMedicalGesture()
        val fromBlink = (GuidedNavigationController.processSequence(
            left,
            right,
            GuidedNavigationState(
                screenMode = GuidedOverlayScreenMode.CategoryMenu,
                categoryMenuSelection = medical
            ),
            PreferredLanguage.English,
            uiStrings
        ) as GuidedSequenceResult.Navigate).newState
        assertEquals(fromTouch.screenMode, fromBlink.screenMode)
        assertEquals(fromTouch.categoryIndex, fromBlink.categoryIndex)
        assertTrue(authority.isMedicalPhraseWorkspaceOpen(fromTouch))
        assertTrue(authority.isMedicalPhraseWorkspaceOpen(fromBlink))
        assertTrue(
            GuidedTrainingFocusPolicy.isTargetAllowed(
                NavigationAction.SelectCategory,
                NavigationAction.SelectCategory,
                isAttemptedTargetHighlighted = true
            )
        )
        val main = read("MainActivity.kt")
        assertTrue(main.contains("openCategoryDirectly"))
        assertTrue(main.contains("openGuidedCategoryFromTouch"))
    }

    @Test
    fun lesson17DoesNotAdvanceUntilMedicalPhraseWorkspaceOpen() {
        val stillMenu = GuidedNavigationState(
            screenMode = GuidedOverlayScreenMode.CategoryMenu,
            categoryMenuSelection = authority.medicalCategoryIndex
        )
        assertFalse(authority.isMedicalPhraseWorkspaceOpen(stillMenu))
        val main = read("MainActivity.kt")
        assertTrue(main.contains("isMedicalOpenedViaDirectShortcut"))
        assertTrue(
            main.contains("if (authority.isMedicalOpenedViaDirectShortcut(uiGuidedNavigationState.value))") ||
                main.contains("isMedicalOpenedViaDirectShortcut(uiGuidedNavigationState.value)")
        )
    }

    // --- Lesson 18 ----------------------------------------------------------------------

    @Test
    fun lesson18BeginsInsideMedicalPhraseWorkspaceWithPainPhrase() {
        val medicalOpen = GuidedNavigationController.openCategoryDirectly(
            GuidedNavigationState(),
            authority.medicalCategoryIndex
        )
        assertTrue(authority.isMedicalPhraseWorkspaceOpen(medicalOpen))
        assertNotEquals(GuidedOverlayScreenMode.CategoryMenu, medicalOpen.screenMode)
        val entry = authority.firstMedicalPhraseEntry()
        assertEquals("I am in pain.", entry.phrase)
        assertEquals("L2 R1", entry.sequenceLabel)
        assertEquals(0, medicalOpen.phrasePageIndex)
        val lesson = TrainingLessonCatalog.navigationLessons[2]
        assertEquals(NavigationAction.SelectPhrase, lesson.action)
        assertEquals(
            authority.sayPhraseLessonTitle(),
            GuidedWorkspaceTrainingSpec.lessonCardTitle(lesson.action, uiStrings)
        )
        assertEquals(
            "Sequence: L2 R1",
            GuidedWorkspaceLessonCardAuthority.displayedSequenceFor(lesson.action, entry.sequenceLabel)
        )
    }

    @Test
    fun lesson18L2R1ExecutesProductionSpeakPathAndGatesOnMedicalWorkspace() {
        val medicalOpen = GuidedNavigationController.openCategoryDirectly(
            GuidedNavigationState(),
            authority.medicalCategoryIndex
        )
        val entry = authority.firstMedicalPhraseEntry()
        val speak = GuidedNavigationController.processSequence(
            entry.left, entry.right, medicalOpen, PreferredLanguage.English, uiStrings
        )
        assertTrue(speak is GuidedSequenceResult.Speak)
        assertEquals(entry.phrase, (speak as GuidedSequenceResult.Speak).entry.phrase)
        val main = read("MainActivity.kt")
        val speakBranch = main.substringAfter("is GuidedSequenceResult.Speak -> {")
            .substringBefore("else ->")
        assertTrue(speakBranch.contains("speak(phrase)"))
        assertTrue(speakBranch.contains("NavigationAction.SelectPhrase"))
        assertTrue(speakBranch.contains("isMedicalPhraseWorkspaceOpen()"))
        assertTrue(speakBranch.contains("verifyTrainingNavigation(NavigationAction.SelectPhrase)"))
    }

    // --- Continuity / layout / sequences ------------------------------------------------

    @Test
    fun continuousJourneyResetsToConversationBetween16And17ForDirectOpen() {
        var state = GuidedNavigationController.communicationWorkspaceRoot(GuidedNavigationState())
        repeat(authority.downsFromConversationToMedical()) {
            state = (GuidedNavigationController.processSequence(
                GuidedModeNavigation.NEXT_LEFT,
                GuidedModeNavigation.NEXT_RIGHT,
                state,
                PreferredLanguage.English,
                uiStrings
            ) as GuidedSequenceResult.Navigate).newState
        }
        assertTrue(authority.isMedicalSelectedInCategoryMenu(state))
        // RC8.25 — Lesson 17 prep restores a clean Category Menu so L3 R1 is required.
        val main = read("MainActivity.kt")
        val openPrep = main.substringAfter("authority.ID_OPEN_MEDICAL -> {")
            .substringBefore("authority.ID_USE_MEDICAL_PHRASE")
        assertTrue(openPrep.contains("communicationWorkspaceRoot"))
        assertFalse(openPrep.contains("isMedicalSelectedInCategoryMenu"))
    }

    @Test
    fun compactCardSizingUnchangedAndNoProductionSequencesChanged() {
        assertEquals(0.45f, GuidedWorkspaceLessonCardAuthority.MaxHeightFraction, 0.001f)
        val card = read("features/onboardingguide/ui/TrainingComponents.kt")
        assertTrue(card.contains("wrapContentHeight()"))
        assertFalse(card.contains("verticalScroll"))
        assertEquals(3 to 1, GuidedCategoryShortcuts.gestureForCategory(2))
        assertEquals(2 to 1, GuidedPageSequences.slotAt(0))
        assertEquals(
            "L0 R2",
            formatWinkSequenceShort(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT)
        )
        assertEquals(
            GuidedWorkspaceHighlightTarget.PhraseRow,
            GuidedWorkspaceTrainingSpec.highlightTargetFor(NavigationAction.SelectPhrase)
        )
    }

    @Test
    fun noLessonSpecificSelectionOverrideCanCreateTwoSelectedCategories() {
        // Authority: selection is a single index; UI derives selected solely from it.
        assertTrue(authority.exactlyOneCategorySelected(0))
        assertTrue(authority.exactlyOneCategorySelected(2))
        assertFalse(authority.exactlyOneCategorySelected(-1))
        assertFalse(authority.exactlyOneCategorySelected(GuidedVocabularyCategory.PAGE_COUNT))
        val ui = read("LisaGuidedModeUi.kt")
        // Selected styling must come only from production categoryMenuSelection.
        assertTrue(ui.contains("val isSelectedRow = index == categoryMenuSelection"))
        assertTrue(ui.contains("selected = isSelectedRow"))
        assertTrue(ui.contains("destinationCategoryIndex"))
        assertFalse(ui.contains("selected = isSelectedRow ||"))
        assertFalse(ui.contains("selected = index == categoryMenuSelection ||"))
        assertFalse(ui.contains("selected = isDestinationRow"))
        assertFalse(ui.contains("index == GuidedWorkspaceTrainingSpec.medicalCategoryIndex"))
    }
}
