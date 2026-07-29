package com.idworx.lisa

import com.idworx.lisa.features.explorelisa.ExploreLisaAuthority
import com.idworx.lisa.features.guidedmedicalcategoryjourney.GuidedMedicalCategoryJourneyAuthority
import com.idworx.lisa.features.guidedworkspacelessoncard.GuidedWorkspaceLessonCardAuthority
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceLessonCardDock
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceTrainingSpec
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC8.16 — Guided workspace lesson-card consistency for lessons 16–32.
 */
class Rc8_16GuidedWorkspaceLessonCardConsistencyTest {

    private val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun read(pathUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$pathUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source: $path")
    }

    private fun phraseGestureFor(action: NavigationAction): String? =
        if (action == NavigationAction.SelectPhrase) {
            GuidedMedicalCategoryJourneyAuthority.firstMedicalPhraseEntry().sequenceLabel
        } else {
            null
        }

    @Test
    fun everyLesson16Through23HasNonEmptyDisplayedSequence() {
        assertEquals(8, TrainingLessonCatalog.navigationLessons.size)
        TrainingLessonCatalog.navigationLessons.forEach { lesson ->
            assertTrue(
                "Lesson ${lesson.id} (${lesson.action}) must show an executable sequence",
                GuidedWorkspaceLessonCardAuthority.hasExecutableSequence(
                    lesson.action,
                    phraseGestureFor(lesson.action)
                )
            )
            val displayed = GuidedWorkspaceLessonCardAuthority.displayedSequenceFor(
                lesson.action,
                phraseGestureFor(lesson.action)
            )
            assertTrue(displayed.startsWith(GuidedWorkspaceLessonCardAuthority.SEQUENCE_PREFIX))
            assertTrue(displayed.contains(Regex("""L-?\d+\s+R-?\d+""")))
        }
    }

    @Test
    fun lesson16DisplaysMoveDownInstructionAndSequenceL0R2() {
        val lesson = TrainingLessonCatalog.navigationLessons[0]
        assertEquals(NavigationAction.MoveToMedicalCategory, lesson.action)
        assertEquals(
            GuidedMedicalCategoryJourneyAuthority.MOVE_DESCRIPTION,
            GuidedWorkspaceTrainingSpec.lessonCardInstruction(lesson.action, lesson.id)
        )
        assertEquals(
            "Sequence: L0 R2",
            GuidedWorkspaceLessonCardAuthority.displayedSequenceFor(lesson.action)
        )
        assertEquals(
            GuidedMedicalCategoryJourneyAuthority.MOVE_LESSON_TITLE,
            GuidedWorkspaceTrainingSpec.lessonCardTitle(lesson.action, uiStrings)
        )
    }

    @Test
    fun lesson17DisplaysOpenMedicalAndSequenceL3R1() {
        val lesson = TrainingLessonCatalog.navigationLessons[1]
        assertEquals(NavigationAction.SelectCategory, lesson.action)
        assertEquals(
            GuidedMedicalCategoryJourneyAuthority.OPEN_MEDICAL_TITLE,
            GuidedWorkspaceTrainingSpec.lessonCardTitle(lesson.action, uiStrings)
        )
        assertEquals(
            "Sequence: L3 R1",
            GuidedWorkspaceLessonCardAuthority.displayedSequenceFor(lesson.action)
        )
    }

    @Test
    fun lesson18DisplaysSayIAmInPainAndSequenceL2R1() {
        val lesson = TrainingLessonCatalog.navigationLessons[2]
        assertEquals(NavigationAction.SelectPhrase, lesson.action)
        val phrase = GuidedMedicalCategoryJourneyAuthority.firstMedicalPhraseEntry()
        assertEquals("I am in pain.", phrase.phrase)
        assertEquals("L2 R1", phrase.sequenceLabel)
        assertEquals(
            GuidedMedicalCategoryJourneyAuthority.sayPhraseLessonTitle(),
            GuidedWorkspaceTrainingSpec.lessonCardTitle(lesson.action, uiStrings)
        )
        assertEquals(
            "Sequence: L2 R1",
            GuidedWorkspaceLessonCardAuthority.displayedSequenceFor(
                lesson.action,
                phrase.sequenceLabel
            )
        )
    }

    @Test
    fun exploreVoiceSettingsAndFinishSequencesMatchProductionOutsideCatalogue() {
        assertEquals(
            "Sequence: L3 R1",
            GuidedWorkspaceLessonCardAuthority.displayedSequenceFor(NavigationAction.OpenVoice)
        )
        assertEquals(
            "Sequence: L5 R5",
            GuidedWorkspaceLessonCardAuthority.displayedSequenceFor(NavigationAction.OpenSettings)
        )
        assertEquals(
            "Sequence: L1 R1",
            GuidedWorkspaceLessonCardAuthority.displayedSequenceFor(
                NavigationAction.FinishGuidedLearning
            )
        )
        assertEquals("L3 R1", ExploreLisaAuthority.voiceSequenceLabel())
        assertEquals("L5 R5", ExploreLisaAuthority.settingsSequenceLabel())
        assertEquals("L1 R1", ExploreLisaAuthority.finishSequenceLabel())
        assertEquals(
            "Sequence: L5 R5",
            GuidedWorkspaceLessonCardAuthority.displayedSequenceFor(NavigationAction.AdjustSensitivity)
        )
    }

    @Test
    fun allLessons16Through32UseSharedCompactCardComponent() {
        TrainingLessonCatalog.navigationLessons.forEach { lesson ->
            assertTrue(
                GuidedWorkspaceLessonCardAuthority.usesSharedCompactCard(lesson.action)
            )
        }
        val components = read("features/onboardingguide/ui/TrainingComponents.kt")
        val card = components.substringAfter("fun GuidedWorkspaceLessonCard(")
            .substringBefore("fun GuidedLessonPhraseTitle(")
        assertTrue(card.contains("GuidedWorkspaceLessonCardAuthority.formatSequenceLabel"))
        assertTrue(card.contains("wrapContentHeight()"))
        assertFalse(card.contains("verticalScroll"))
        assertFalse(card.contains("fillMaxHeight()"))
        assertFalse(card.contains("Arrangement.SpaceBetween"))
        assertFalse(card.contains("modifier.weight("))
        val ui = read("LisaAccessibilityUi.kt")
        assertTrue(ui.contains("compact = true"))
        assertTrue(ui.contains("MaxHeightFraction"))
        assertEquals(1, Regex("GuidedWorkspaceLessonCard\\(").findAll(ui).count())
    }

    @Test
    fun cardUsesContentWrappedHeightWithMaximumConstraint() {
        assertEquals(0.45f, GuidedWorkspaceLessonCardAuthority.MaxHeightFraction, 0.001f)
        assertEquals(236, GuidedWorkspaceLessonCardAuthority.MaxCardWidth.value.toInt())
        val components = read("features/onboardingguide/ui/TrainingComponents.kt")
        val card = components.substringAfter("fun GuidedWorkspaceLessonCard(")
            .substringBefore("fun GuidedLessonPhraseTitle(")
        assertTrue(card.contains("wrapContentHeight()"))
        assertFalse(card.contains("verticalScroll"))
        assertFalse(card.contains("fillMaxHeight()"))
        assertFalse(card.contains("fillMaxSize()"))
        val host = read("LisaAccessibilityUi.kt")
        assertTrue(host.contains("heightIn(max = lessonCardMaxHeight)"))
    }

    @Test
    fun cardPlacementConstrainedBelowUniversalEyeTrackingHeader() {
        val ui = read("LisaAccessibilityUi.kt")
        val headerIndex = ui.indexOf("UniversalEyeTrackingHeader(")
        val workspaceBoxIndex = ui.indexOf(
            "RC8.16 / RC8.18 — workspace content region below UniversalEyeTrackingHeader"
        )
        val cardIndex = ui.indexOf("GuidedWorkspaceLessonCard(")
        assertTrue(headerIndex >= 0)
        assertTrue(workspaceBoxIndex > headerIndex)
        assertTrue(cardIndex > workspaceBoxIndex)
        // Card must live inside the weighted workspace Box, not the root fillMaxSize overlay.
        val rootBoxLessonCard = ui.contains("Floating lesson card renders last so it is always drawn above")
        assertFalse(rootBoxLessonCard)
        assertTrue(ui.contains("end RC8.16/RC8.18 workspace content BoxWithConstraints"))
        assertTrue(
            ui.substring(workspaceBoxIndex, cardIndex).contains("weight(1f)") ||
                ui.substring(headerIndex, cardIndex).contains(".weight(1f)")
        )
    }

    @Test
    fun dockKeepsRelevantControlVisibleForEveryLesson() {
        TrainingLessonCatalog.navigationLessons.forEach { lesson ->
            val dock = GuidedWorkspaceTrainingSpec.cardDockForLesson(lesson.action, lesson.id)
            assertTrue(
                GuidedWorkspaceLessonCardAuthority.dockKeepsTargetVisible(dock)
            )
            when (lesson.action) {
                NavigationAction.MoveToMedicalCategory,
                NavigationAction.NextPage,
                NavigationAction.PreviousPage,
                NavigationAction.TriggerEmergency ->
                    assertEquals(GuidedWorkspaceLessonCardDock.BottomStart, dock)
                NavigationAction.SelectCategory,
                NavigationAction.SelectPhrase ->
                    assertEquals(GuidedWorkspaceLessonCardDock.BottomEnd, dock)
                NavigationAction.CloseMenu -> {
                    // Workspace Back → BottomStart; Explore Close Menu → BottomEnd.
                    if (lesson.id == "nav_back") {
                        assertEquals(GuidedWorkspaceLessonCardDock.BottomStart, dock)
                    }
                }
                else -> Unit
            }
        }
    }

    @Test
    fun displayedSequencesResolveToExecutableProductionActions() {
        val expected = mapOf(
            NavigationAction.MoveToMedicalCategory to (0 to 2),
            NavigationAction.SelectCategory to (3 to 1),
            NavigationAction.SelectPhrase to (2 to 1),
            NavigationAction.CloseMenu to (2 to 2),
            NavigationAction.NextPage to (0 to 4),
            NavigationAction.PreviousPage to (4 to 0),
            NavigationAction.TriggerEmergency to (6 to 0),
            NavigationAction.AdjustSensitivity to (5 to 5),
            NavigationAction.ResetSequence to (0 to 3),
            NavigationAction.OpenMenu to (4 to 6),
            NavigationAction.MenuSelectVoice to (0 to 2),
            NavigationAction.OpenVoice to (3 to 1),
            NavigationAction.BackFromDestination to (2 to 2),
            NavigationAction.MenuSelectSettings to (0 to 2),
            NavigationAction.OpenSettings to (5 to 5),
            NavigationAction.FinishGuidedLearning to (1 to 1)
        )
        TrainingLessonCatalog.navigationLessons.forEach { lesson ->
            val pair = expected[lesson.action] ?: return@forEach
            val displayed = GuidedWorkspaceLessonCardAuthority.displayedSequenceFor(
                lesson.action,
                phraseGestureFor(lesson.action)
            )
            val raw = GuidedWorkspaceLessonCardAuthority.stripSequencePrefix(displayed)
            assertEquals(
                "Mismatch for ${lesson.id} / ${lesson.action}",
                formatWinkSequenceShort(pair.first, pair.second),
                raw
            )
        }
        // Production catalogue unchanged.
        assertEquals(3 to 1, GuidedCategoryShortcuts.gestureForCategory(2))
        assertEquals(2 to 1, GuidedPageSequences.slotAt(0))
        assertEquals(0 to 2, GuidedModeNavigation.NEXT_LEFT to GuidedModeNavigation.NEXT_RIGHT)
        assertEquals(4 to 6, GuidedModeNavigation.OPEN_MAIN_MENU_LEFT to GuidedModeNavigation.OPEN_MAIN_MENU_RIGHT)
    }

    @Test
    fun lessonOrderAndObjectivesPreserved() {
        val actions = TrainingLessonCatalog.navigationLessons.map { it.action }
        assertEquals(NavigationAction.MoveToMedicalCategory, actions[0])
        assertEquals(NavigationAction.SelectCategory, actions[1])
        assertEquals(NavigationAction.SelectPhrase, actions[2])
        assertEquals(NavigationAction.CloseMenu, actions[3])
        assertEquals(NavigationAction.NextPage, actions[4])
        assertEquals(NavigationAction.PreviousPage, actions[5])
        assertEquals(NavigationAction.TriggerEmergency, actions[6])
        assertEquals(NavigationAction.AdjustSensitivity, actions[7])
        assertEquals(8, actions.size)
    }

    @Test
    fun sequencePrefixAlwaysAppliedInCardComponent() {
        assertEquals(
            "Sequence: L0 R2",
            GuidedWorkspaceLessonCardAuthority.formatSequenceLabel("L0 R2")
        )
        assertEquals(
            "Sequence: L0 R2",
            GuidedWorkspaceLessonCardAuthority.formatSequenceLabel("Sequence: L0 R2")
        )
        val components = read("features/onboardingguide/ui/TrainingComponents.kt")
        assertTrue(
            components.contains("formatSequenceLabel(") &&
                (components.contains("formatSequenceLabel(gestureLabel)") ||
                    components.contains("formatSequenceLabel(\n") ||
                    components.contains("GuidedWorkspaceLessonCardAuthority.formatSequenceLabel"))
        )
        assertFalse(
            components.substringAfter("fun GuidedWorkspaceLessonCard(")
                .substringBefore("fun GuidedLessonPhraseTitle(")
                .contains("if (compact) gestureLabel else")
        )
    }
}
