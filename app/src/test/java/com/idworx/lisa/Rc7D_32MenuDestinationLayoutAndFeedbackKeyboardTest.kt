package com.idworx.lisa

import androidx.compose.ui.unit.dp
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Rc7D_32MenuDestinationLayoutAndFeedbackKeyboardTest {

    private fun openFeedback() = MenuDestinationNavigationController.open(
        MainMenuDestination.Feedback,
        LisaPanel.Feedback,
        listOf(
            MenuDestinationAction(
                MenuDestinationActionId.FeedbackWorkedWell,
                "What worked well?",
                MenuDestinationActionType.TextField
            ),
            MenuDestinationAction(
                MenuDestinationActionId.FeedbackSave,
                "Save feedback",
                MenuDestinationActionType.Save,
                isEnabled = false
            )
        )
    )

    private fun beginFeedback(value: String = "original") =
        MenuDestinationNavigationController.beginTextEditing(
            openFeedback(),
            MenuDestinationActionId.FeedbackWorkedWell,
            value,
            requiresReview = true
        )

    @Test
    fun normalWidthAllocationRetainsBothSafeMinimums() {
        val widths = DestinationWorkspaceWidthAuthority.calculateDestinationWorkspaceWidths(
            600.dp,
            8.dp
        )
        assertTrue(widths.contentWidthDp >= DestinationWorkspaceWidthAuthority.MinimumContentWidth)
        assertTrue(widths.navigationWidthDp >= DestinationWorkspaceWidthAuthority.MinimumNavigationWidth)
        assertFalse(widths.usesKeyboardFocusedLayout)
    }

    @Test
    fun allocatedWidthsNeverExceedAvailableWidth() {
        listOf(0, 120, 280, 360, 600, 1200).forEach { available ->
            val widths = DestinationWorkspaceWidthAuthority.calculateDestinationWorkspaceWidths(
                available.dp,
                8.dp
            )
            assertTrue(widths.contentWidthDp.value >= 0f)
            assertTrue(widths.navigationWidthDp.value >= 0f)
            assertTrue(
                widths.contentWidthDp.value + widths.navigationWidthDp.value + 8f <=
                    available.toFloat().coerceAtLeast(8f)
            )
        }
    }

    @Test
    fun invalidMeasurementsAreClampedSafely() {
        val widths = DestinationWorkspaceWidthAuthority.calculateDestinationWorkspaceWidths(
            (-100).dp,
            40.dp
        )
        assertEquals(0.dp, widths.contentWidthDp)
        assertEquals(0.dp, widths.navigationWidthDp)
    }

    @Test
    fun smallScreenAllocationIsDeterministic() {
        val first = DestinationWorkspaceWidthAuthority.calculateDestinationWorkspaceWidths(
            320.dp,
            8.dp
        )
        val second = DestinationWorkspaceWidthAuthority.calculateDestinationWorkspaceWidths(
            320.dp,
            8.dp
        )
        assertEquals(first, second)
        assertTrue(first.contentWidthDp > 0.dp)
        assertTrue(first.navigationWidthDp > 0.dp)
    }

    @Test
    fun navigationPanelNeverExceedsConfiguredFractionAtWideSizes() {
        val widths = DestinationWorkspaceWidthAuthority.calculateDestinationWorkspaceWidths(
            1000.dp,
            8.dp
        )
        assertTrue(widths.navigationWidthDp <= 280.dp)
        assertTrue(widths.navigationWidthDp <= DestinationWorkspaceWidthAuthority.PreferredNavigationWidth)
    }

    @Test
    fun keyboardUsesDedicatedFullWidthLayoutWhenSidePanelWouldClipKeys() {
        val widths = DestinationWorkspaceWidthAuthority.calculateDestinationWorkspaceWidths(
            400.dp,
            8.dp,
            keyboardActive = true
        )
        assertTrue(widths.usesKeyboardFocusedLayout)
        assertEquals(0.dp, widths.navigationWidthDp)
        assertEquals(392.dp, widths.contentWidthDp)
    }

    @Test
    fun wideKeyboardKeepsReadableNavigationPanel() {
        val widths = DestinationWorkspaceWidthAuthority.calculateDestinationWorkspaceWidths(
            700.dp,
            8.dp,
            keyboardActive = true
        )
        assertFalse(widths.usesKeyboardFocusedLayout)
        assertTrue(widths.contentWidthDp >= DestinationWorkspaceWidthAuthority.MinimumCompleteKeyboardWidth)
        assertTrue(widths.navigationWidthDp >= DestinationWorkspaceWidthAuthority.MinimumNavigationWidth)
    }

    @Test
    fun canonicalKeyboardContainsAllTwentySixLetters() {
        val letters = KeyboardLayout.letterRows.flatten().map(Char::uppercaseChar).toSet()
        assertEquals(('A'..'Z').toSet(), letters)
    }

    @Test
    fun canonicalKeyboardContainsRequiredRowBoundaries() {
        assertTrue('Q' in KeyboardLayout.letterRows.first())
        assertTrue('P' in KeyboardLayout.letterRows.first())
        assertTrue('A' in KeyboardLayout.letterRows[1])
        assertTrue('L' in KeyboardLayout.letterRows[1])
        assertTrue('Z' in KeyboardLayout.letterRows[2])
        assertTrue('M' in KeyboardLayout.letterRows[2])
    }

    @Test
    fun canonicalKeyboardContainsAllSupportedPunctuation() {
        assertEquals(
            setOf(',', '.', '\'', '?', '!', '-', ':', ';'),
            KeyboardLayout.letterPunctuationRow.toSet()
        )
    }

    @Test
    fun everyCanonicalKeyboardSlotIsReachable() {
        assertTrue(KeyboardLayout.allKeysReachable(EyeKeyboardLayoutMode.Letters))
        assertTrue(KeyboardLayout.allKeysReachable(EyeKeyboardLayoutMode.Numbers))
    }

    @Test
    fun keyboardNavigationCanReachLastLetterInEveryRow() {
        KeyboardLayout.letterRows.forEachIndexed { row, keys ->
            var cursor = KeyboardCursor(row, 0)
            repeat(keys.lastIndex) {
                cursor = KeyboardNavigator.move(
                    cursor,
                    PhraseComposerActionId.MoveRight,
                    EyeKeyboardLayoutMode.Letters
                )
            }
            assertEquals(keys.lastIndex, cursor.col)
        }
    }

    @Test
    fun feedbackEditingCapturesOriginalAndDraft() {
        val stage = beginFeedback().interactionStage as
            MenuDestinationInteractionStage.TextEditing
        assertEquals("original", stage.originalText)
        assertEquals("original", stage.draftText)
        assertEquals(FeedbackFieldEditingStage.Keyboard, stage.fieldEditingStage)
    }

    @Test
    fun selectingAKeyUpdatesDraftWithoutSavingField() {
        val typed = MenuDestinationNavigationController.selectTextKey(
            beginFeedback("")
        )
        val stage = typed.interactionStage as MenuDestinationInteractionStage.TextEditing
        assertEquals(FeedbackFieldEditingStage.Keyboard, stage.fieldEditingStage)
        assertEquals("q", stage.draftText)
        assertTrue(typed.isActive)
    }

    @Test
    fun doneEditingEntersReviewWithoutCommitting() {
        val typed = MenuDestinationNavigationController.selectTextKey(beginFeedback(""))
        val review = MenuDestinationNavigationController.finishKeyboardEditing(typed)
        val stage = review.interactionStage as MenuDestinationInteractionStage.TextEditing
        assertEquals(FeedbackFieldEditingStage.Review, stage.fieldEditingStage)
        assertEquals("q", stage.draftText)
        assertEquals("", stage.originalText)
    }

    @Test
    fun keyboardSelectionIsSuspendedDuringReview() {
        val review = MenuDestinationNavigationController.finishKeyboardEditing(
            beginFeedback("")
        )
        assertEquals(review, MenuDestinationNavigationController.selectTextKey(review))
        assertEquals(
            review,
            MenuDestinationNavigationController.touchTextKey(review, 0, 1)
        )
    }

    @Test
    fun continueEditingReturnsToKeyboardWithDraftPreserved() {
        val edited = MenuDestinationNavigationController.updateTextDraft(
            beginFeedback(),
            "changed"
        )
        val review = MenuDestinationNavigationController.finishKeyboardEditing(edited)
        val continued = MenuDestinationNavigationController.continueKeyboardEditing(review)
        val stage = continued.interactionStage as MenuDestinationInteractionStage.TextEditing
        assertEquals(FeedbackFieldEditingStage.Keyboard, stage.fieldEditingStage)
        assertEquals("changed", stage.draftText)
        assertEquals("original", stage.originalText)
    }

    @Test
    fun saveFieldLeavesEditingAndRetainsDraftForCallerCommit() {
        val edited = MenuDestinationNavigationController.updateTextDraft(
            beginFeedback(),
            "saved"
        )
        val review = MenuDestinationNavigationController.finishKeyboardEditing(edited)
        val reviewStage = review.interactionStage as MenuDestinationInteractionStage.TextEditing
        val form = MenuFeedbackDraft().withValue(reviewStage.actionId, reviewStage.draftText)
        val completed = MenuDestinationNavigationController.confirmTextEditing(review)
        assertEquals("saved", form.workedWell)
        assertEquals(MenuDestinationInteractionStage.Browsing, completed.interactionStage)
    }

    @Test
    fun cancelRestoresPreviouslyCommittedFormValue() {
        val form = MenuFeedbackDraft(workedWell = "committed")
        val editing = MenuDestinationNavigationController.updateTextDraft(
            beginFeedback(form.workedWell),
            "discard me"
        )
        val cancelled = MenuDestinationNavigationController.cancelCurrentStage(editing)
        assertEquals("committed", form.workedWell)
        assertEquals(MenuDestinationInteractionStage.Browsing, cancelled.interactionStage)
    }

    @Test
    fun editingAnotherFieldDoesNotEraseSavedFields() {
        val firstSaved = MenuFeedbackDraft(workedWell = "first")
        val secondSaved = firstSaved.withValue(
            MenuDestinationActionId.FeedbackConfusing,
            "second"
        )
        assertEquals("first", secondSaved.workedWell)
        assertEquals("second", secondSaved.confusing)
    }

    @Test
    fun fieldSaveAndFormSubmissionUseDistinctStableActions() {
        assertNotEquals(
            MenuDestinationActionId.FeedbackWorkedWell,
            MenuDestinationActionId.FeedbackSave
        )
        assertFalse(MenuFeedbackDraft().hasContent)
        assertTrue(MenuFeedbackDraft(workedWell = "ready").hasContent)
    }

    @Test
    fun keyboardAndReviewExposeUnambiguousCommands() {
        val source = source("MenuDestinationWorkspaceUi.kt")
        assertTrue(source.contains("MenuDestinationPanelCommand.DoneEditing"))
        assertTrue(source.contains("\"Save Field\""))
        assertTrue(source.contains("\"Continue Editing\""))
        assertTrue(source.contains("FeedbackFieldEditingStage.Review"))
    }

    @Test
    fun destinationWorkspaceAndContentPaneFillAvailableSpace() {
        val workspace = source("MenuDestinationWorkspaceUi.kt")
        val shell = source("LisaAccessibilityUi.kt")
        assertTrue(workspace.contains(".fillMaxSize()"))
        assertTrue(workspace.contains(".fillMaxHeight()"))
        assertTrue(shell.contains("fillDestinationPane"))
        assertTrue(shell.contains("Modifier.fillMaxHeight()"))
    }

    @Test
    fun destinationScrollColumnsNoLongerUseUndersizedFixedMaximums() {
        listOf(
            source("LisaAboutUi.kt"),
            source("LisaPrivacyPolicyUi.kt"),
            source("LisaVoicePlatformUi.kt")
        ).forEach { text ->
            assertFalse(text.contains(".heightIn(max = 420.dp)"))
            assertFalse(text.contains(".heightIn(max = 460.dp)"))
            assertTrue(text.contains(".fillMaxSize()"))
        }
    }

    @Test
    fun horizontalTextSafeguardsCoverBackAndVoiceStatuses() {
        val shell = source("LisaAccessibilityUi.kt")
        val voice = source("LisaVoicePlatformUi.kt")
        assertTrue(shell.contains("modifier = Modifier.widthIn(min = 76.dp)"))
        assertTrue(shell.contains("maxLines = 1"))
        assertTrue(voice.contains("private fun VoiceStatusBadge"))
        assertTrue(voice.contains(".widthIn(min = 96.dp, max = 140.dp)"))
        assertTrue(voice.contains("overflow = TextOverflow.Ellipsis"))
    }

    @Test
    fun canonicalBottomKeyboardDistributesLetterRowsAcrossAvailableWidth() {
        val keyboard = source("EyeControlledKeyboard.kt")
        val letterBranch = keyboard.substringAfter("EyeKeyboardLayoutMode.Letters ->")
            .substringBefore("EyeKeyboardLayoutMode.Numbers ->")
        assertTrue(letterBranch.contains("KeyboardLayout.letterRows"))
        assertTrue(letterBranch.contains("spreadHorizontally = bottomAnchored"))
        assertFalse(source("MenuDestinationWorkspaceUi.kt").contains("OutlinedTextField("))
    }

    @Test
    fun feedbackEditorAlwaysShowsTitleDraftAndInstructions() {
        val workspace = source("MenuDestinationWorkspaceUi.kt")
        assertTrue(workspace.contains("fieldTitle"))
        assertTrue(workspace.contains("\"Start typing…\""))
        assertTrue(workspace.contains("\"Review your feedback\""))
        assertTrue(workspace.contains("\"Save Field  L1 R1"))
    }

    @Test
    fun aboutAndPrivacyRetainSharedBlinkPagingScrollState() {
        assertTrue(source("LisaAboutUi.kt").contains("rememberDestinationScrollState()"))
        assertTrue(source("LisaPrivacyPolicyUi.kt").contains("rememberDestinationScrollState()"))
        val state = openFeedback()
        val paged = MenuDestinationNavigationController.nextPage(state, 100, 300)
        assertEquals(1, paged.viewportPage)
        assertEquals(100, paged.scrollRequestPx)
    }

    @Test
    fun emergencyStillRoutesAheadOfDestinationReview() {
        val context = LisaGestureContext(
            activePanel = LisaPanel.Feedback,
            guidedOverlayActive = false,
            guidedScreenMode = null,
            isAdjustingPreference = false,
            phraseComposerMode = null
        )
        assertEquals(
            GestureRoutingTarget.Emergency,
            ModeScopedGestureAuthority.routingTarget(
                context,
                EMERGENCY_LEFT_WINKS,
                EMERGENCY_RIGHT_WINKS
            )
        )
    }

    private fun source(name: String): String {
        val candidates = listOf(
            File("src/main/java/com/idworx/lisa/$name"),
            File("app/src/main/java/com/idworx/lisa/$name")
        )
        return candidates.firstOrNull(File::exists)?.readText()
            ?: error("Missing source $name")
    }
}
