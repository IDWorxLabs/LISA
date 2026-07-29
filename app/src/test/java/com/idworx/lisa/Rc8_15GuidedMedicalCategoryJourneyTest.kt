package com.idworx.lisa

import com.idworx.lisa.features.guidedmedicalcategoryjourney.GuidedMedicalCategoryJourneyAuthority
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceHighlightTarget
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceTrainingSpec
import com.idworx.lisa.features.onboardingguide.state.TrainingEvent
import com.idworx.lisa.features.onboardingguide.navigation.GuidedTrainingNavigator
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC8.15 — Guided Communication lessons 16–18: Medical category journey accuracy.
 */
class Rc8_15GuidedMedicalCategoryJourneyTest {

    private val navigator = GuidedTrainingNavigator()
    private val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun read(pathUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$pathUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source: $path")
    }

    @Test
    fun lesson16NoLongerContainsOpenCategoriesOrL3R0() {
        val lesson16 = TrainingLessonCatalog.navigationLessons.first()
        assertEquals(NavigationAction.MoveToMedicalCategory, lesson16.action)
        assertEquals(GuidedMedicalCategoryJourneyAuthority.ID_MOVE_TO_MEDICAL, lesson16.id)
        val title = GuidedWorkspaceTrainingSpec.lessonCardTitle(lesson16.action, uiStrings)
        val instruction = GuidedWorkspaceTrainingSpec.lessonCardInstruction(lesson16.action, lesson16.id)
        val gesture = GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(lesson16.action)
        assertFalse(title.contains("Open Categories", ignoreCase = true))
        assertFalse(instruction.orEmpty().contains("Open Categories", ignoreCase = true))
        assertNotEquals("L3 R0", gesture)
        assertEquals("L0 R2", gesture)
        assertEquals(GuidedMedicalCategoryJourneyAuthority.MOVE_LESSON_TITLE, title)
        assertEquals(GuidedMedicalCategoryJourneyAuthority.MOVE_DESCRIPTION, instruction)
    }

    @Test
    fun lesson16BeginsInAlreadyOpenCategoryWorkspace() {
        val progress = navigator.reduce(TrainingProgress(), TrainingEvent.SkipToNavigationTraining)
        assertEquals(TrainingPhase.NavigationLesson, progress.currentPhase)
        assertEquals(0, progress.navigationLessonIndex)
        assertEquals(
            NavigationAction.MoveToMedicalCategory,
            TrainingLessonCatalog.navigationLessonAt(0)?.action
        )
        // Production landing for Communication / skip-to-nav is Category Selection (already open).
        val root = GuidedNavigationController.communicationWorkspaceRoot(GuidedNavigationState())
        assertEquals(GuidedOverlayScreenMode.CategoryMenu, root.screenMode)
        assertEquals(0, root.categoryMenuSelection) // General Conversation
        val main = read("MainActivity.kt")
        assertTrue(main.contains("GuidedNavigationController.communicationWorkspaceRoot"))
        // Lesson 16 must not re-open Categories via L3 R0.
        assertFalse(
            TrainingLessonCatalog.navigationLessons
                .take(3)
                .any { it.action == NavigationAction.OpenCategories }
        )
    }

    @Test
    fun lesson16RequiresRepeatedL0R2UntilMedicalSelected() {
        val medical = GuidedMedicalCategoryJourneyAuthority.medicalCategoryIndex
        assertEquals(2, medical)
        assertEquals(2, GuidedMedicalCategoryJourneyAuthority.downsFromConversationToMedical())
        var state = GuidedNavigationController.communicationWorkspaceRoot(GuidedNavigationState())
        assertEquals(0, state.categoryMenuSelection)
        // First L0 R2 → Basic Needs (1); must not complete yet.
        val afterFirst = GuidedNavigationController.processSequence(
            GuidedModeNavigation.NEXT_LEFT,
            GuidedModeNavigation.NEXT_RIGHT,
            state,
            PreferredLanguage.English,
            uiStrings
        )
        assertTrue(afterFirst is GuidedSequenceResult.Navigate)
        state = (afterFirst as GuidedSequenceResult.Navigate).newState
        assertEquals(
            GuidedVocabularyCategory.ordered.indexOf(GuidedVocabularyCategory.BasicNeeds),
            state.categoryMenuSelection
        )
        assertNotEquals(medical, state.categoryMenuSelection)
        // Second L0 R2 → Medical (2).
        val afterSecond = GuidedNavigationController.processSequence(
            GuidedModeNavigation.NEXT_LEFT,
            GuidedModeNavigation.NEXT_RIGHT,
            state,
            PreferredLanguage.English,
            uiStrings
        )
        assertTrue(afterSecond is GuidedSequenceResult.Navigate)
        state = (afterSecond as GuidedSequenceResult.Navigate).newState
        assertEquals(medical, state.categoryMenuSelection)
        val main = read("MainActivity.kt")
        assertTrue(main.contains("NavigationAction.MoveToMedicalCategory"))
        assertTrue(
            main.contains("isMedicalSelectedInCategoryMenu") ||
                (main.contains("categoryMenuSelection ==") &&
                    main.contains("GuidedWorkspaceTrainingSpec.medicalCategoryIndex"))
        )
        // Completion is gated on Medical selection — not after a single down.
        val moveBranch = main.substringAfter("handleMoveToMedicalLessonPhase")
            .ifEmpty {
                main.substringAfter("NavigationAction.MoveToMedicalCategory ->")
                    .substringBefore("NavigationAction.MenuSelectVoice")
            }
        assertTrue(
            moveBranch.contains("verifyTrainingNavigationPhase(NavigationAction.MoveToMedicalCategory)") ||
                moveBranch.contains("verifyTrainingNavigation(NavigationAction.MoveToMedicalCategory)") ||
                main.contains("verifyTrainingNavigationPhase(NavigationAction.MoveToMedicalCategory)")
        )
    }

    @Test
    fun lesson17DisplaysOpenMedicalWithL3R1() {
        val lesson17 = TrainingLessonCatalog.navigationLessons[1]
        assertEquals(NavigationAction.SelectCategory, lesson17.action)
        assertEquals(GuidedMedicalCategoryJourneyAuthority.ID_OPEN_MEDICAL, lesson17.id)
        assertEquals(
            GuidedMedicalCategoryJourneyAuthority.OPEN_MEDICAL_TITLE,
            GuidedWorkspaceTrainingSpec.lessonCardTitle(lesson17.action, uiStrings)
        )
        assertEquals("L3 R1", GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(lesson17.action))
        assertEquals(
            GuidedCategoryShortcuts.sequenceLabelForCategory(
                GuidedWorkspaceTrainingSpec.medicalCategoryIndex
            ),
            GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(lesson17.action)
        )
        assertEquals(3 to 1, GuidedMedicalCategoryJourneyAuthority.openMedicalGesture())
        // Must not use Conversation / L2 R1 / generic L1 R1.
        assertNotEquals(
            GuidedCategoryShortcuts.sequenceLabelForCategory(
                GuidedWorkspaceTrainingSpec.conversationCategoryIndex
            ),
            GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(lesson17.action)
        )
        assertNotEquals("L2 R1", GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(lesson17.action))
        assertNotEquals("L1 R1", GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(lesson17.action))
        assertFalse(
            GuidedWorkspaceTrainingSpec.lessonCardTitle(lesson17.action, uiStrings)
                .contains("Conversation", ignoreCase = true)
        )
    }

    @Test
    fun lesson17OpensRealMedicalThroughProductionNavigation() {
        val (left, right) = GuidedMedicalCategoryJourneyAuthority.openMedicalGesture()
        val menu = GuidedNavigationState(
            screenMode = GuidedOverlayScreenMode.CategoryMenu,
            categoryMenuSelection = GuidedWorkspaceTrainingSpec.medicalCategoryIndex
        )
        val result = GuidedNavigationController.processSequence(
            left, right, menu, PreferredLanguage.English, uiStrings
        )
        assertTrue(result is GuidedSequenceResult.Navigate)
        val opened = (result as GuidedSequenceResult.Navigate).newState
        assertEquals(GuidedOverlayScreenMode.Vocabulary, opened.screenMode)
        assertEquals(GuidedWorkspaceTrainingSpec.medicalCategoryIndex, opened.categoryIndex)
        val main = read("MainActivity.kt")
        assertTrue(main.contains("targetCategoryIndex == GuidedWorkspaceTrainingSpec.medicalCategoryIndex"))
        assertTrue(main.contains("verifyTrainingNavigation(NavigationAction.SelectCategory)"))
    }

    @Test
    fun lesson18RunsInsideMedicalPhraseWorkspaceWithRealPhrase() {
        val lesson18 = TrainingLessonCatalog.navigationLessons[2]
        assertEquals(NavigationAction.SelectPhrase, lesson18.action)
        val entry = GuidedMedicalCategoryJourneyAuthority.firstMedicalPhraseEntry()
        assertEquals("I am in pain.", entry.phrase)
        assertEquals("L2 R1", entry.sequenceLabel)
        assertEquals(
            GuidedMedicalCategoryJourneyAuthority.sayPhraseLessonTitle(),
            GuidedWorkspaceTrainingSpec.lessonCardTitle(lesson18.action, uiStrings)
        )
        assertEquals(
            entry.sequenceLabel,
            GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(lesson18.action, entry.sequenceLabel)
        )
        // Phrase exists in production Medical catalogue — not invented for training.
        val medicalPage = GuidedVocabularyCatalog.categoryAt(
            GuidedWorkspaceTrainingSpec.medicalCategoryIndex,
            PreferredLanguage.English,
            uiStrings
        )!!
        assertTrue(medicalPage.entries.any { it.phrase == entry.phrase && it.sequenceLabel == entry.sequenceLabel })
        assertEquals(GuidedVocabularyCategory.Medical, medicalPage.category)
        assertEquals(
            GuidedWorkspaceHighlightTarget.PhraseRow,
            GuidedWorkspaceTrainingSpec.highlightTargetFor(NavigationAction.SelectPhrase)
        )
        val main = read("MainActivity.kt")
        assertTrue(main.contains("verifyTrainingNavigation(NavigationAction.SelectPhrase)"))
        assertTrue(main.contains("speak(phrase)"))
    }

    @Test
    fun generalConversationNotHighlightedAcrossLessons16To18() {
        val conversation = GuidedWorkspaceTrainingSpec.conversationCategoryIndex
        assertEquals(0, conversation)
        assertEquals(2, GuidedWorkspaceTrainingSpec.medicalCategoryIndex)
        assertNotEquals(conversation, GuidedWorkspaceTrainingSpec.medicalCategoryIndex)
        for (action in listOf(
            NavigationAction.MoveToMedicalCategory
        )) {
            assertEquals(
                GuidedWorkspaceHighlightTarget.NextPage,
                GuidedWorkspaceTrainingSpec.highlightTargetFor(action)
            )
        }
        assertEquals(null, GuidedWorkspaceTrainingSpec.highlightTargetFor(NavigationAction.SelectCategory))
        val ui = read("LisaGuidedModeUi.kt")
        assertTrue(ui.contains("index == categoryMenuSelection"))
        assertTrue(ui.contains("destinationCategoryIndex"))
        assertFalse(ui.contains("index == GuidedWorkspaceTrainingSpec.medicalCategoryIndex"))
        assertFalse(ui.contains("index == GuidedWorkspaceTrainingSpec.conversationCategoryIndex"))
        // Titles must not keep Conversation as the trained destination.
        assertFalse(
            GuidedWorkspaceTrainingSpec.lessonCardTitle(NavigationAction.SelectCategory, uiStrings)
                .contains("Conversation", ignoreCase = true)
        )
        assertFalse(
            GuidedWorkspaceTrainingSpec.lessonCardTitle(NavigationAction.SelectPhrase, uiStrings)
                .contains("Conversation", ignoreCase = true)
        )
    }

    @Test
    fun stateFlowsContinuouslyFromLesson16Through18() {
        // Catalog order encodes the continuous journey.
        assertEquals(
            listOf(
                NavigationAction.MoveToMedicalCategory,
                NavigationAction.SelectCategory,
                NavigationAction.SelectPhrase
            ),
            TrainingLessonCatalog.navigationLessons.take(3).map { it.action }
        )
        // Production state machine supports the continuous path without resets between steps.
        var state = GuidedNavigationController.communicationWorkspaceRoot(GuidedNavigationState())
        repeat(GuidedMedicalCategoryJourneyAuthority.downsFromConversationToMedical()) {
            val result = GuidedNavigationController.processSequence(
                GuidedModeNavigation.NEXT_LEFT,
                GuidedModeNavigation.NEXT_RIGHT,
                state,
                PreferredLanguage.English,
                uiStrings
            )
            state = (result as GuidedSequenceResult.Navigate).newState
        }
        assertEquals(GuidedOverlayScreenMode.CategoryMenu, state.screenMode)
        assertEquals(GuidedWorkspaceTrainingSpec.medicalCategoryIndex, state.categoryMenuSelection)
        val (openL, openR) = GuidedMedicalCategoryJourneyAuthority.openMedicalGesture()
        state = (GuidedNavigationController.processSequence(
            openL, openR, state, PreferredLanguage.English, uiStrings
        ) as GuidedSequenceResult.Navigate).newState
        assertEquals(GuidedOverlayScreenMode.Vocabulary, state.screenMode)
        assertEquals(GuidedWorkspaceTrainingSpec.medicalCategoryIndex, state.categoryIndex)
        val phrase = GuidedMedicalCategoryJourneyAuthority.firstMedicalPhraseEntry()
        val speak = GuidedNavigationController.processSequence(
            phrase.left, phrase.right, state, PreferredLanguage.English, uiStrings
        )
        assertTrue(speak is GuidedSequenceResult.Speak)
        assertEquals(phrase.phrase, (speak as GuidedSequenceResult.Speak).entry.phrase)
        // MainActivity must not reset category selection between lessons 16 and 17.
        val main = read("MainActivity.kt")
        val moveBlock = main.substringAfter("NavigationAction.MoveToMedicalCategory ->")
            .substringBefore("NavigationAction.MenuSelectVoice")
        assertFalse(moveBlock.contains("communicationWorkspaceRoot"))
        assertFalse(moveBlock.contains("openCategoryMenu("))
    }

    @Test
    fun noProductionSequenceAssignmentsChanged() {
        assertEquals(3 to 1, GuidedCategoryShortcuts.gestureForCategory(2))
        assertEquals(2 to 1, GuidedPageSequences.slotAt(0))
        assertEquals("L3 R0", formatWinkSequenceShort(
            GuidedModeNavigation.CATEGORIES_LEFT,
            GuidedModeNavigation.CATEGORIES_RIGHT
        ))
        assertEquals("L0 R2", formatWinkSequenceShort(
            GuidedModeNavigation.NEXT_LEFT,
            GuidedModeNavigation.NEXT_RIGHT
        ))
        val medicalFirst = GuidedMedicalCategoryJourneyAuthority.firstMedicalPhraseEntry()
        assertEquals(2, medicalFirst.left)
        assertEquals(1, medicalFirst.right)
        // Catalogue still lists the same Medical phrases in the same order.
        val medicalSource = read("LisaGuidedMode.kt")
        val medicalBlock = medicalSource.substringAfter("category = GuidedVocabularyCategory.Medical,")
            .substringBefore("category = GuidedVocabularyCategory.Family,")
        assertTrue(medicalBlock.contains("\"i_am_in_pain\""))
        assertTrue(medicalBlock.indexOf("\"i_am_in_pain\"") < medicalBlock.indexOf("\"please_call_the_nurse\""))
    }

    @Test
    fun trainingCardsRemainCompactAndContentWrapped() {
        val ui = read("LisaAccessibilityUi.kt")
        assertTrue(ui.contains("compact = true"))
        assertTrue(ui.contains("GuidedWorkspaceLessonCardAuthority"))
        val components = read("features/onboardingguide/ui/TrainingComponents.kt")
        assertTrue(components.contains("wrapContentHeight()"))
        assertFalse(components.substringAfter("fun GuidedWorkspaceLessonCard(")
            .substringBefore("fun GuidedLessonPhraseTitle(")
            .contains("verticalScroll"))
        assertTrue(components.contains("formatSequenceLabel"))
        assertTrue(ui.contains("MaxHeightFraction"))
        assertEquals(
            GuidedWorkspaceTrainingSpec.cardDockFor(GuidedWorkspaceHighlightTarget.NextPage),
            GuidedWorkspaceTrainingSpec.cardDockForLesson(
                NavigationAction.MoveToMedicalCategory,
                GuidedMedicalCategoryJourneyAuthority.ID_MOVE_TO_MEDICAL
            )
        )
    }

    @Test
    fun lessonProgressNumbersAre16Through18Of23() {
        fun progressAt(navIndex: Int) = TrainingLessonCatalog.guidedLessonProgress(
            TrainingProgress(
                currentPhase = TrainingPhase.NavigationLesson,
                navigationLessonIndex = navIndex
            )
        )
        assertEquals(16 to 23, progressAt(0))
        assertEquals(17 to 23, progressAt(1))
        assertEquals(18 to 23, progressAt(2))
    }
}
