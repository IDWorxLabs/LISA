package com.idworx.lisa

import com.idworx.lisa.features.guidedlessonteaching.GuidedLessonTeachingSpec
import com.idworx.lisa.features.guidedmedicalcategoryjourney.GuidedMedicalCategoryJourneyAuthority
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
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
 * RC8.21 / RC8.23 — Lesson 16 Part 1 still completes via production Move Down selection.
 */
class Rc8_21Lesson16ManualNavigationTeachingFlowTest {

    private val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun read(pathUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$pathUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source: $path")
    }

    @Test
    fun popupShowsMethod1ConnectingSequenceToMedical() {
        val teaching = GuidedLessonTeachingSpec.presentationFor(
            NavigationAction.MoveToMedicalCategory,
            GuidedMedicalCategoryJourneyAuthority.ID_MOVE_TO_MEDICAL,
            uiStrings,
            phaseIndex = 0
        )
        assertTrue(teaching.usesStructuredMethods)
        assertEquals("Method 1", teaching.methods[0].title)
        assertTrue(
            teaching.methods[0].instructionalLines.first().contains("L0 R2") &&
                teaching.methods[0].instructionalLines.first().contains("Medical")
        )
        assertEquals("L0 R2", teaching.methods[0].highlightedSequence)
        assertEquals("L0 R2", teaching.rawGestureLabel)
    }

    @Test
    fun lesson16HasNoDestinationGlowAndOnlyMoveDownHighlight() {
        val teaching = GuidedLessonTeachingSpec.presentationFor(
            NavigationAction.MoveToMedicalCategory,
            TrainingLessonCatalog.navigationLessons[0].id,
            uiStrings,
            phaseIndex = 0
        )
        assertNull(teaching.destinationCategoryIndex)
        assertNull(GuidedLessonTeachingSpec.destinationCategoryIndexFor(NavigationAction.MoveToMedicalCategory))
        assertEquals(
            GuidedMedicalCategoryJourneyAuthority.medicalCategoryIndex,
            teaching.productionTargetCategoryIndex
        )
        assertEquals(GuidedWorkspaceHighlightTarget.NextPage, teaching.navigationControlHighlight)
        assertEquals(
            GuidedWorkspaceHighlightTarget.NextPage,
            GuidedWorkspaceTrainingSpec.highlightTargetFor(NavigationAction.MoveToMedicalCategory)
        )

        val ui = read("LisaGuidedModeUi.kt")
        assertTrue(ui.contains("selected = isSelectedRow"))
        assertTrue(ui.contains("destinationCategoryIndex != null && index == destinationCategoryIndex"))
        val host = read("LisaAccessibilityUi.kt")
        assertTrue(host.contains("destinationCategoryIndex = guidedDestinationCategoryIndex"))
    }

    @Test
    fun startsWithConversationSelectedAndManualL0R2ReachesMedical() {
        var state = GuidedNavigationController.communicationWorkspaceRoot(GuidedNavigationState())
        assertTrue(GuidedMedicalCategoryJourneyAuthority.isLesson16StartState(state))
        assertEquals(0, state.categoryMenuSelection)
        assertNotEquals(
            GuidedMedicalCategoryJourneyAuthority.medicalCategoryIndex,
            state.categoryMenuSelection
        )

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
        assertFalse(GuidedMedicalCategoryJourneyAuthority.isMedicalSelectedInCategoryMenu(state))

        state = (GuidedNavigationController.processSequence(
            GuidedModeNavigation.NEXT_LEFT,
            GuidedModeNavigation.NEXT_RIGHT,
            state,
            PreferredLanguage.English,
            uiStrings
        ) as GuidedSequenceResult.Navigate).newState
        assertTrue(GuidedMedicalCategoryJourneyAuthority.isMedicalSelectedInCategoryMenu(state))
        assertEquals(
            GuidedMedicalCategoryJourneyAuthority.medicalCategoryIndex,
            state.categoryMenuSelection
        )
    }

    @Test
    fun completionRemainsProductionStateGatedAndCardShowsFullInstruction() {
        val main = read("MainActivity.kt")
        assertTrue(main.contains("completeNavigationLessonPhase") || main.contains("verifyTrainingNavigation"))
        assertTrue(
            main.contains("isMedicalSelectedInCategoryMenu") ||
                (main.contains("categoryMenuSelection ==") &&
                    main.contains("GuidedWorkspaceTrainingSpec.medicalCategoryIndex"))
        )
        val card = read("features/onboardingguide/ui/TrainingComponents.kt")
            .substringAfter("fun GuidedWorkspaceLessonCard(")
            .substringBefore("fun GuidedLessonPhraseTitle(")
        assertFalse(card.contains("verticalScroll"))
        assertTrue(card.contains("usesStructuredMethods"))
        assertTrue(card.contains("teaching.methods.forEachIndexed"))
        assertTrue(card.contains("GuidedLessonSequenceEmphasisBox"))
    }
}
