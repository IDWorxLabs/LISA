package com.idworx.lisa

import com.idworx.lisa.features.guidedlessonteaching.GuidedLessonTeachingSpec
import com.idworx.lisa.features.guidedsensitivitylesson.GuidedSensitivityLessonAuthority
import com.idworx.lisa.features.guidedworkspacelessoncard.GuidedWorkspaceLessonCardAuthority
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceHighlightTarget
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceLessonCardDock
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceTrainingSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * RC8.33 — Compact non-blocking Lesson 23 cards and clear adjustment introduction.
 */
class Rc8_33CompactLesson23CardsAndIntroductionTest {

    private val sensitivity = GuidedSensitivityLessonAuthority
    private val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun read(relative: String): String =
        File("src/main/java/com/idworx/lisa/$relative").readText()

    private fun fullLesson() = GuidedLessonTeachingSpec.fullPresentationFor(
        NavigationAction.AdjustSensitivity,
        sensitivity.ID_ADJUST_SENSITIVITY,
        uiStrings
    )

    // --- Introduction ----------------------------------------------------------------------------

    @Test
    fun openingCardExplainsAdjustmentPurposeBeforeL0R4() {
        val phase0 = fullLesson().phases[0]
        assertEquals(sensitivity.PHASE1_TITLE, phase0.title)
        assertEquals(sensitivity.LESSON_CONTEXT, phase0.context)
        assertTrue(phase0.context!!.contains("Sensitivity", ignoreCase = true))
        assertTrue(phase0.context!!.contains("Response Time", ignoreCase = true))
        assertTrue(phase0.context!!.contains("example", ignoreCase = true))
        assertFalse(phase0.context!!.contains("separately tested", ignoreCase = true))
        assertEquals(sensitivity.PHASE1_INSTRUCTION, phase0.description)
        assertTrue(phase0.description!!.contains("L0 R4"))
        assertEquals("L0 R4", phase0.rawGestureLabel)
        val intro = GuidedWorkspaceTrainingSpec.lessonCardInstruction(
            NavigationAction.AdjustSensitivity
        ).orEmpty()
        assertTrue(intro.contains("Sensitivity", ignoreCase = true))
        assertTrue(intro.contains("Response Time", ignoreCase = true))
    }

    // --- Duplicate copy --------------------------------------------------------------------------

    @Test
    fun eachPhaseInstructionAppearsOnceInPresentationModel() {
        assertTrue(sensitivity.instructionAppearsOnce(0, sensitivity.PHASE1_INSTRUCTION))
        assertTrue(sensitivity.instructionAppearsOnce(1, sensitivity.PHASE2_INSTRUCTION))
        assertTrue(sensitivity.instructionAppearsOnce(2, sensitivity.PHASE3_INSTRUCTION))
        assertTrue(sensitivity.instructionAppearsOnce(3, sensitivity.PHASE4_INSTRUCTION))
        assertTrue(sensitivity.instructionAppearsOnce(4, sensitivity.PHASE5_INSTRUCTION))
        fullLesson().phases.forEachIndexed { index, phase ->
            assertTrue("phase $index should have empty methods", phase.methods.isEmpty())
            val lines = sensitivity.visibleInstructionLinesForPhase(index)
            val instruction = phase.description!!
            assertEquals(1, lines.count { it == instruction })
        }
    }

    @Test
    fun cardUiDoesNotRenderDescriptionAndMatchingMethodLine() {
        val components = read("features/onboardingguide/ui/TrainingComponents.kt")
        val card = components.substringAfter("fun GuidedWorkspaceLessonCard(")
            .substringBefore("private val LessonCardSuccessGreen")
        assertTrue(card.contains("instructionLine !in methodLines") || card.contains("!in methodLines"))
        assertTrue(card.contains("trimmed == instructionLine") || card.contains("== instructionLine"))
        assertTrue(card.contains("teaching.context"))
        assertTrue(card.contains("contentDescription"))
        assertTrue(card.contains("buildString"))
        val semanticsBlock = card.substringAfter("contentDescription = buildString")
            .substringBefore("shape = RoundedCornerShape")
        // Semantics append instruction once via instructionLine — not twice.
        assertTrue(semanticsBlock.contains("instructionLine"))
        assertEquals(1, Regex("instructionLine\\?\\.let").findAll(semanticsBlock).count())
    }

    // --- Compact layout --------------------------------------------------------------------------

    @Test
    fun lesson23CardWrapsWithoutFixedOversizedHeightOrScroll() {
        val components = read("features/onboardingguide/ui/TrainingComponents.kt")
        val card = components.substringAfter("fun GuidedWorkspaceLessonCard(")
            .substringBefore("private val LessonCardSuccessGreen")
        assertTrue(card.contains("wrapContentHeight()"))
        assertTrue(card.contains("widthIn(max = GuidedWorkspaceLessonCardAuthority.MaxCardWidth)"))
        assertFalse(card.contains("verticalScroll"))
        assertFalse(card.contains("fillMaxHeight()"))
        val ui = read("LisaAccessibilityUi.kt")
        assertTrue(ui.contains("heightIn(max = lessonCardMaxHeight)"))
        assertTrue(ui.contains("wrapContentHeight()"))
        assertTrue(
            GuidedWorkspaceLessonCardAuthority.MaxHeightFraction in 0.20f..0.50f
        )
    }

    // --- Target avoidance ------------------------------------------------------------------------

    @Test
    fun phaseAwareDocksAvoidProtectedTargets() {
        val full = fullLesson()
        full.phases.forEach { phase ->
            val highlight = phase.navigationControlHighlight
            val dock = GuidedWorkspaceTrainingSpec.cardDockFor(highlight)
            assertTrue(
                "dock $dock must avoid targets for $highlight",
                GuidedWorkspaceLessonCardAuthority.dockAvoidsProtectedTargets(highlight, dock)
            )
        }
        assertEquals(
            GuidedWorkspaceLessonCardDock.TopStart,
            GuidedWorkspaceTrainingSpec.cardDockFor(GuidedWorkspaceHighlightTarget.CategoryNextPage)
        )
        assertEquals(
            GuidedWorkspaceLessonCardDock.TopStart,
            GuidedWorkspaceTrainingSpec.cardDockFor(GuidedWorkspaceHighlightTarget.CategoryRow)
        )
        assertEquals(
            GuidedWorkspaceLessonCardDock.BottomEnd,
            GuidedWorkspaceTrainingSpec.cardDockFor(GuidedWorkspaceHighlightTarget.SettingsHubSensitivity)
        )
        assertEquals(
            GuidedWorkspaceLessonCardDock.TopStart,
            GuidedWorkspaceTrainingSpec.cardDockFor(GuidedWorkspaceHighlightTarget.IncreaseOrDecreaseValue)
        )
        assertEquals(
            GuidedWorkspaceLessonCardDock.TopStart,
            GuidedWorkspaceTrainingSpec.cardDockFor(GuidedWorkspaceHighlightTarget.Back)
        )
        val ui = read("LisaAccessibilityUi.kt")
        assertTrue(ui.contains("cardDockFor("))
        assertTrue(ui.contains("guidedWorkspaceHighlight"))
        assertTrue(ui.contains("Alignment.TopStart"))
        assertTrue(ui.contains("Alignment.TopEnd"))
        assertFalse(ui.contains("absoluteOffset"))
    }

    @Test
    fun dualAdjustSequencesDisplayOnceEach() {
        val label = sensitivity.adjustSequencesLabel()
        assertTrue(label.contains(sensitivity.PHASE4_DECREASE_SEQUENCE_LABEL))
        assertTrue(label.contains(sensitivity.PHASE4_INCREASE_SEQUENCE_LABEL))
        assertEquals(1, label.split("Decrease:").size - 1)
        assertEquals(1, label.split("Increase:").size - 1)
        assertEquals(
            "L0 R4",
            GuidedWorkspaceLessonCardAuthority.stripSequencePrefix(
                GuidedWorkspaceLessonCardAuthority.formatSequenceLabel("L0 R4")
            )
        )
    }

    // --- Regression ------------------------------------------------------------------------------

    @Test
    fun productionPhasesAndCatalogueUnchanged() {
        val full = fullLesson()
        assertEquals(5, full.phases.size)
        assertEquals(sensitivity.PHASE_MOVE_TO_SETTINGS_PAGE, full.phases[0].id)
        assertEquals(sensitivity.PHASE_RETURN_TO_SETTINGS, full.phases[4].id)
        assertTrue(full.phases[4].showCompletionFeedback)
        assertTrue(full.phases.take(4).all { !it.showCompletionFeedback })
        assertEquals("L0 R4", sensitivity.moveToSettingsPageSequenceLabel())
        assertEquals("L5 R5", sensitivity.openSettingsSequenceLabel())
        assertEquals("L2 R0", sensitivity.openSensitivitySequenceLabel())
        assertEquals("L3 R1", sensitivity.decreaseSequenceLabel())
        assertEquals("L1 R3", sensitivity.increaseSequenceLabel())
        assertEquals("L2 R2", sensitivity.backSequenceLabel())
        assertEquals(8, TrainingLessonCatalog.navigationLessons.size)
        assertEquals(sensitivity.ID_ADJUST_SENSITIVITY, TrainingLessonCatalog.navigationLessons.last().id)
        assertEquals("Start Communicating", sensitivity.START_COMMUNICATING_LABEL)
        assertEquals("Restart Guided Learning", sensitivity.RESTART_GUIDED_LEARNING_LABEL)
    }
}
