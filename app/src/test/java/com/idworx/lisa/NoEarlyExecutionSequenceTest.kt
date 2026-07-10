package com.idworx.lisa

import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the real-device bug: on the Conversation phrase page, Yes = L2 R1 is a strict prefix of
 * Stop = L2 R3 (both visible on the same page). Previous fixes let an "unambiguous" completed
 * gesture (e.g. Stop) resolve as soon as the user stopped blinking, without waiting the full
 * configured response-time idle allowance — but real-device testing showed users were still being
 * interrupted mid-sequence. The current, stricter rule removes ALL early execution: no gesture —
 * phrase, category, navigation, confirm, cancel, or Emergency — may execute while the user is still
 * blinking/winking. [shouldFinalizeSequence] is the *only* finalize gate left anywhere in the app,
 * and it only ever returns true once the full configured idle window has elapsed with no further
 * input (or the absolute max-window safety cap is hit for a sequence that never goes idle).
 */
class NoEarlyExecutionSequenceTest {

    private val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)
    private val catalogContext = GuidedCatalogContext(responseTimeSec = 5, sensitivityLevel = 5)
    private val pages = GuidedVocabularyCatalog.buildPages(PreferredLanguage.English, uiStrings, catalogContext)

    private val conversationPage = pages[GuidedVocabularyCategory.Conversation.ordinal]
    private val yesEntry = conversationPage.entries.first { it.phrase == "Yes" }
    private val stopEntry = conversationPage.entries.first { it.phrase == "Stop" }

    private fun conversationState(phrasePageIndex: Int = 0): GuidedNavigationState =
        GuidedNavigationState(
            screenMode = GuidedOverlayScreenMode.Vocabulary,
            categoryIndex = GuidedVocabularyCategory.Conversation.ordinal,
            phrasePageIndex = phrasePageIndex,
            draftResponseTimeSec = catalogContext.responseTimeSec,
            draftSensitivityLevel = catalogContext.sensitivityLevel
        )

    private fun categoryMenuState(): GuidedNavigationState =
        GuidedNavigationState(
            screenMode = GuidedOverlayScreenMode.CategoryMenu,
            categoryMenuSelection = 0,
            draftResponseTimeSec = catalogContext.responseTimeSec,
            draftSensitivityLevel = catalogContext.sensitivityLevel
        )

    /** Mirrors [MainActivity.processSequenceWinks]'s sole finalize gate — no quick-resolve branch. */
    private fun finalizes(
        left: Int,
        right: Int,
        idleMs: Long,
        sequenceAgeMs: Long = idleMs,
        idleTimeoutSec: Int = catalogContext.responseTimeSec
    ): Boolean =
        shouldFinalizeSequence(
            left = left,
            right = right,
            idleMs = idleMs,
            sequenceAgeMs = sequenceAgeMs,
            idleTimeoutMs = SequenceProcessingDelay.toMillis(idleTimeoutSec),
            maxWindowMs = SequenceProcessingDelay.maxWindowMs(idleTimeoutSec)
        )

    // --- Fixture sanity ---------------------------------------------------------------------

    @Test
    fun fixture_yesAndStopAreBothVisibleOnTheSameConversationPage() {
        assertEquals(2 to 1, yesEntry.left to yesEntry.right)
        assertEquals(2 to 3, stopEntry.left to stopEntry.right)
        val visible = WorkspacePhraseResolver.visibleEntriesForState(
            state = conversationState(),
            language = PreferredLanguage.English,
            uiStrings = uiStrings,
            catalogContext = catalogContext
        )
        assertTrue(visible.any { it.phrase == "Yes" })
        assertTrue(visible.any { it.phrase == "Stop" })
    }

    // --- Acceptance criteria: nothing fires before the full idle timeout, period --------------

    @Test
    fun enteringL2R1_doesNotTriggerYes_anyTimeBeforeTheFullIdleTimeoutExpires() {
        val idleTimeoutMs = SequenceProcessingDelay.toMillis(catalogContext.responseTimeSec)
        assertFalse(finalizes(2, 1, idleMs = 0L))
        assertFalse(finalizes(2, 1, idleMs = idleTimeoutMs / 2))
        assertFalse(finalizes(2, 1, idleMs = idleTimeoutMs - 1))
    }

    @Test
    fun enteringL2R3_doesNotTriggerStopEarlyEither_evenThoughItIsUnambiguous() {
        // Stop used to be allowed to "quick resolve" the instant it was unambiguous. Under the
        // stricter no-early-execution rule there is no such fast path left: Stop must wait exactly
        // as long as Yes does.
        val idleTimeoutMs = SequenceProcessingDelay.toMillis(catalogContext.responseTimeSec)
        assertFalse(finalizes(2, 3, idleMs = 0L))
        assertFalse(finalizes(2, 3, idleMs = idleTimeoutMs - 1))
    }

    @Test
    fun continuingFromL2R1ToL2R3_withinTheIdleWindow_resolvesStop_notYes() {
        val state = conversationState()
        val idleTimeoutMs = SequenceProcessingDelay.toMillis(catalogContext.responseTimeSec)
        assertTrue(finalizes(2, 3, idleMs = idleTimeoutMs))
        val result = GuidedNavigationController.processSequence(
            left = 2, right = 3, state = state, language = PreferredLanguage.English, uiStrings = uiStrings,
            catalogContext = catalogContext
        )
        assertTrue(result is GuidedSequenceResult.Speak)
        assertEquals("Stop", (result as GuidedSequenceResult.Speak).entry.phrase)
    }

    @Test
    fun enteringL2R1ThenWaitingTheFullIdleWindow_triggersYes() {
        val state = conversationState()
        val idleTimeoutMs = SequenceProcessingDelay.toMillis(catalogContext.responseTimeSec)
        assertTrue(finalizes(2, 1, idleMs = idleTimeoutMs))
        val result = GuidedNavigationController.processSequence(
            left = 2, right = 1, state = state, language = PreferredLanguage.English, uiStrings = uiStrings,
            catalogContext = catalogContext
        )
        assertTrue(result is GuidedSequenceResult.Speak)
        assertEquals("Yes", (result as GuidedSequenceResult.Speak).entry.phrase)
    }

    @Test
    fun everyNewBlink_resetsTheIdleTimer_soAnAlmostTimedOutSequenceNeverFinalizesOnAStaleMeasurement() {
        val idleTimeoutMs = SequenceProcessingDelay.toMillis(catalogContext.responseTimeSec)
        // At L2 R1, idle has crept up right to the edge of the full timeout — one more tick and Yes
        // would fire on its own...
        assertFalse(finalizes(2, 1, idleMs = idleTimeoutMs - 1))
        // ...but a fresh right-wink lands right then, completing Stop (L2 R3) and resetting idle to
        // zero. If the timer hadn't truly reset per-blink, this would finalize instantly using the
        // stale near-timeout idle instead of a fresh measurement.
        assertFalse(finalizes(2, 3, idleMs = 0L, sequenceAgeMs = idleTimeoutMs))
        // Only once the FULL window has elapsed again, measured from that reset point, may it finalize.
        assertTrue(finalizes(2, 3, idleMs = idleTimeoutMs, sequenceAgeMs = idleTimeoutMs * 2))
    }

    // --- Hidden/off-screen gestures never execute ---------------------------------------------

    @Test
    fun hiddenSecondPageEntry_doesNotExecute_whileFirstPageIsVisible() {
        val hiddenEntry = conversationPage.entries.first { it.phrase == "I need help" }
        val state = conversationState(phrasePageIndex = 0)
        val result = GuidedNavigationController.processSequence(
            left = hiddenEntry.left, right = hiddenEntry.right, state = state,
            language = PreferredLanguage.English, uiStrings = uiStrings, catalogContext = catalogContext
        )
        assertEquals(GuidedSequenceResult.Unmatched, result)
    }

    // --- Previous/Next phrase navigation still works, but only after the full idle window -----

    @Test
    fun previousNextPhraseNavigation_stillWorks_afterTheFullIdleWindow() {
        val pageZero = conversationState(phrasePageIndex = 0)
        val pageOne = conversationState(phrasePageIndex = 1)

        val next = GuidedNavigationController.processSequence(
            GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT, pageZero,
            PreferredLanguage.English, uiStrings, catalogContext = catalogContext
        )
        assertTrue(next is GuidedSequenceResult.Navigate)

        val previous = GuidedNavigationController.processSequence(
            GuidedModeNavigation.PREVIOUS_LEFT, GuidedModeNavigation.PREVIOUS_RIGHT, pageOne,
            PreferredLanguage.English, uiStrings, catalogContext = catalogContext
        )
        assertTrue(previous is GuidedSequenceResult.Navigate)
    }

    @Test
    fun nextPageNavigation_doesNotFinalizeBeforeTheFullIdleWindow() {
        val idleTimeoutMs = SequenceProcessingDelay.toMillis(catalogContext.responseTimeSec)
        assertFalse(
            finalizes(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT, idleMs = idleTimeoutMs - 1)
        )
        assertTrue(
            finalizes(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT, idleMs = idleTimeoutMs)
        )
    }

    @Test
    fun previousPhraseNavigation_isUnmatched_onFirstPage() {
        val pageZero = conversationState(phrasePageIndex = 0)
        val result = GuidedNavigationController.processSequence(
            GuidedModeNavigation.PREVIOUS_LEFT, GuidedModeNavigation.PREVIOUS_RIGHT, pageZero,
            PreferredLanguage.English, uiStrings, catalogContext = catalogContext
        )
        assertEquals(GuidedSequenceResult.Unmatched, result)
    }

    // --- Select/Back reserved codes wait for the idle window exactly like everything else -----

    @Test
    fun selectCode_isNotEvenAValidActionOnTheVocabularyPage() {
        // Select (L1 R1) isn't a recognised action while viewing phrases at all (it only means
        // something on the Category Menu / preferences-adjustment screens).
        val state = conversationState()
        val result = GuidedNavigationController.processSequence(
            left = GuidedModeNavigation.SELECT_LEFT, right = GuidedModeNavigation.SELECT_RIGHT, state = state,
            language = PreferredLanguage.English, uiStrings = uiStrings, catalogContext = catalogContext
        )
        assertEquals(GuidedSequenceResult.Unmatched, result)
    }

    @Test
    fun selectAndBack_resolveOnTheCategoryMenu_onlyAfterTheFullIdleTimeout() {
        val state = categoryMenuState()
        val idleTimeoutMs = SequenceProcessingDelay.toMillis(catalogContext.responseTimeSec)
        assertFalse(
            finalizes(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT, idleMs = idleTimeoutMs - 1)
        )
        assertFalse(
            finalizes(GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT, idleMs = idleTimeoutMs - 1)
        )
        assertTrue(
            finalizes(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT, idleMs = idleTimeoutMs)
        )
        val result = GuidedNavigationController.processSequence(
            left = GuidedModeNavigation.SELECT_LEFT, right = GuidedModeNavigation.SELECT_RIGHT, state = state,
            language = PreferredLanguage.English, uiStrings = uiStrings, catalogContext = catalogContext
        )
        assertTrue(result is GuidedSequenceResult.Navigate)
    }

    // --- Emergency waits for the idle window like everything else, but is never cut off -------

    @Test
    fun emergencySequence_neverCutOffAcrossFiveSecondGaps_butAlsoNeverFiresBeforeTheIdleWindow() {
        val idleTimeoutMs = SequenceProcessingDelay.toMillis(SequenceProcessingDelay.DEFAULT_SECONDS)
        val maxWindowMs = SequenceProcessingDelay.maxWindowMs(SequenceProcessingDelay.DEFAULT_SECONDS)
        val gapMs = idleTimeoutMs - 1
        for (leftSoFar in 1..EMERGENCY_LEFT_WINKS) {
            val sequenceAgeMs = gapMs * (leftSoFar - 1)
            assertFalse(
                "must not cut off at blink $leftSoFar of $EMERGENCY_LEFT_WINKS",
                shouldFinalizeSequence(
                    left = leftSoFar, right = 0, idleMs = 0L, sequenceAgeMs = sequenceAgeMs,
                    idleTimeoutMs = idleTimeoutMs, maxWindowMs = maxWindowMs
                )
            )
        }
        // Even once all 6 left winks land, Emergency itself still waits for the idle timeout —
        // there is no immediate short-circuit anywhere, including for Emergency.
        assertFalse(
            shouldFinalizeSequence(
                left = EMERGENCY_LEFT_WINKS, right = 0, idleMs = idleTimeoutMs - 1,
                sequenceAgeMs = idleTimeoutMs - 1, idleTimeoutMs = idleTimeoutMs, maxWindowMs = maxWindowMs
            )
        )
        assertTrue(
            shouldFinalizeSequence(
                left = EMERGENCY_LEFT_WINKS, right = 0, idleMs = idleTimeoutMs,
                sequenceAgeMs = idleTimeoutMs, idleTimeoutMs = idleTimeoutMs, maxWindowMs = maxWindowMs
            )
        )
    }

    // --- Response-time controls still drive the idle timeout used here ------------------------

    @Test
    fun responseTimeControls_stillDriveTheIdleTimeoutUsedByFinalization() {
        val minWaitMs = SequenceProcessingDelay.toMillis(SequenceProcessingDelay.MIN_SECONDS)
        // At the fastest configured response time, waiting exactly that long finalizes.
        assertTrue(finalizes(2, 1, idleMs = minWaitMs, idleTimeoutSec = SequenceProcessingDelay.MIN_SECONDS))
        // The same real wait, but with the slowest configured response time selected, must not yet
        // finalize — proving the idle timeout used really is the currently selected response time.
        assertFalse(finalizes(2, 1, idleMs = minWaitMs, idleTimeoutSec = SequenceProcessingDelay.MAX_SECONDS))
    }

    // --- Structural proof: no early-execution / quick-resolve path remains in MainActivity -----

    @Test
    fun mainActivity_hasNoQuickResolveOrEarlyExecutionPathRemaining() {
        val main = ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main != null)
        assertTrue(main!!.contains("effectiveSequenceIdleTimeoutMs()"))
        assertFalse(main.contains("QUICK_RESOLVE_IDLE_MS"))
        assertFalse(main.contains("isQuicklyResolvableGesture"))
        assertFalse(main.contains("currentVisibleGestureSet"))
        assertFalse(main.contains("quickResolved"))
    }

    @Test
    fun mainActivity_hasNoImmediateEmergencyShortCircuitInsideTheWinkAcceptBlocks() {
        val main = ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main != null)
        val body = main!!.substringAfter("private fun processSequenceWinks(")
            .substringBefore("private fun updateSequencePauseState(")
        val acceptLeftBlock = body.substringAfter("if (result.acceptLeft)").substringBefore("if (result.acceptRight)")
        val acceptRightBlock = body.substringAfter("if (result.acceptRight)").substringBefore("val hasCountedWinks")
        assertFalse(acceptLeftBlock.contains("finalizeSequence()"))
        assertFalse(acceptRightBlock.contains("finalizeSequence()"))
    }
}
