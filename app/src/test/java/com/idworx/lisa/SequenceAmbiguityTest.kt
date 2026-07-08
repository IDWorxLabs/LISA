package com.idworx.lisa

import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the real-device bug: on the Conversation phrase page, Yes = L2 R1 is a strict prefix of
 * Stop = L2 R3 (both visible on the same page). LISA fired Yes as soon as L2 R1 completed instead of
 * waiting for the configured response-time idle allowance to see whether the user continued toward
 * Stop. Fix: [isAmbiguousVisibleMatch] scopes ambiguity to only the currently *visible* gesture set
 * (visible phrase rows, visible category rows, visible navigation-panel actions, Emergency), and
 * [MainActivity]'s finalize gate ("quick resolve") now only fires early for gestures that are
 * unambiguous against that set — an ambiguous match like Yes always falls through to the full
 * configured idle timeout instead.
 */
class SequenceAmbiguityTest {

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

    /** Mirrors [MainActivity.currentVisibleGestureSet] using only public building blocks. */
    private fun visibleGestureSetFor(state: GuidedNavigationState): Set<Pair<Int, Int>> {
        val emergencyAndSystem = setOf(
            EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS,
            GuidedModeNavigation.FINISH_TRAINING_LEFT to GuidedModeNavigation.FINISH_TRAINING_RIGHT
        )
        val navigation = GuidedNavigationController.visibleGlobalNavigationGestures(
            state = state,
            language = PreferredLanguage.English,
            uiStrings = uiStrings,
            catalogContext = catalogContext
        )
        val content = WorkspacePhraseResolver.continuationMappings(
            state = state,
            language = PreferredLanguage.English,
            uiStrings = uiStrings,
            catalogContext = catalogContext
        ).map { it.left to it.right }.toSet()
        return emergencyAndSystem + navigation + content
    }

    /** Mirrors [MainActivity.isQuicklyResolvableGesture] (outside Guided Training). */
    private fun isQuicklyResolvable(left: Int, right: Int, state: GuidedNavigationState): Boolean {
        val isSingleEyeGlobalNavigation = GuidedModeNavigation.isGlobalNavigationSequence(left, right) &&
            (left == 0 || right == 0)
        if (isSingleEyeGlobalNavigation) return true
        return isUnambiguousVisibleMatch(left, right, visibleGestureSetFor(state))
    }

    private fun finalizes(
        left: Int,
        right: Int,
        state: GuidedNavigationState,
        idleMs: Long,
        sequenceAgeMs: Long = idleMs,
        idleTimeoutSec: Int = catalogContext.responseTimeSec
    ): Boolean {
        val idleTimeoutMs = SequenceProcessingDelay.toMillis(idleTimeoutSec)
        val quickResolved = isQuicklyResolvable(left, right, state) &&
            idleMs >= GuidedModeNavigation.QUICK_RESOLVE_IDLE_MS
        return quickResolved || shouldFinalizeSequence(
            left = left,
            right = right,
            idleMs = idleMs,
            sequenceAgeMs = sequenceAgeMs,
            idleTimeoutMs = idleTimeoutMs,
            maxWindowMs = SequenceProcessingDelay.maxWindowMs(idleTimeoutSec)
        )
    }

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

    // --- Core ambiguity rule -----------------------------------------------------------------

    @Test
    fun yes_isAmbiguous_becauseStopIsALongerVisibleContinuation() {
        val visible = visibleGestureSetFor(conversationState())
        assertTrue(isAmbiguousVisibleMatch(2, 1, visible))
        assertFalse(isUnambiguousVisibleMatch(2, 1, visible))
    }

    @Test
    fun stop_isUnambiguous_becauseNothingLongerIsVisible() {
        val visible = visibleGestureSetFor(conversationState())
        assertFalse(isAmbiguousVisibleMatch(2, 3, visible))
        assertTrue(isUnambiguousVisibleMatch(2, 3, visible))
    }

    // --- Acceptance criteria: Yes must not fire early, Stop must be reachable -----------------

    @Test
    fun enteringL2R1_doesNotImmediatelyTriggerYes_whenResponseTimeHasNotExpired() {
        val state = conversationState()
        // Just past the short quick-resolve grace window, nowhere near the full 5s response time.
        val idleMs = GuidedModeNavigation.QUICK_RESOLVE_IDLE_MS + 200L
        assertFalse(finalizes(2, 1, state, idleMs = idleMs))
    }

    @Test
    fun continuingToL2R3WithinFiveSeconds_triggersStop_notYes() {
        val state = conversationState()
        // Stop is unambiguous, so it only needs the short quick-resolve grace window, well inside 5s.
        val idleMs = GuidedModeNavigation.QUICK_RESOLVE_IDLE_MS + 50L
        assertTrue(idleMs < SequenceProcessingDelay.toMillis(catalogContext.responseTimeSec))
        assertTrue(finalizes(2, 3, state, idleMs = idleMs))
        val result = GuidedNavigationController.processSequence(
            left = 2, right = 3, state = state, language = PreferredLanguage.English, uiStrings = uiStrings,
            catalogContext = catalogContext
        )
        assertTrue(result is GuidedSequenceResult.Speak)
        assertEquals("Stop", (result as GuidedSequenceResult.Speak).entry.phrase)
    }

    @Test
    fun enteringL2R1ThenWaitingFiveSeconds_triggersYes() {
        val state = conversationState()
        val idleTimeoutMs = SequenceProcessingDelay.toMillis(catalogContext.responseTimeSec)
        assertTrue(finalizes(2, 1, state, idleMs = idleTimeoutMs))
        val result = GuidedNavigationController.processSequence(
            left = 2, right = 1, state = state, language = PreferredLanguage.English, uiStrings = uiStrings,
            catalogContext = catalogContext
        )
        assertTrue(result is GuidedSequenceResult.Speak)
        assertEquals("Yes", (result as GuidedSequenceResult.Speak).entry.phrase)
    }

    @Test
    fun everyNewBlink_resetsTheIdleTimer_soAlmostTimedOutYesNeverBleedsIntoStop() {
        val state = conversationState()
        val idleTimeoutMs = SequenceProcessingDelay.toMillis(catalogContext.responseTimeSec)
        // At L2 R1, idle has crept up right to the edge of the full timeout — one more tick and Yes
        // would fire on its own...
        assertFalse(finalizes(2, 1, state, idleMs = idleTimeoutMs - 1))
        // ...but a fresh right-wink lands right then, completing Stop (L2 R3) and resetting idle to
        // zero. If the timer hadn't truly reset per-blink, this would finalize almost instantly using
        // the stale near-timeout idle instead of a fresh measurement.
        assertFalse(finalizes(2, 3, state, idleMs = 0L, sequenceAgeMs = idleTimeoutMs))
        // Stop is unambiguous, so once its own short quick-resolve grace window elapses (measured from
        // the reset point, not the earlier near-timeout), it may finalize well before the full timeout.
        assertTrue(finalizes(2, 3, state, idleMs = GuidedModeNavigation.QUICK_RESOLVE_IDLE_MS + 1, sequenceAgeMs = idleTimeoutMs))
    }

    // --- Hidden/off-screen gestures never create ambiguity or execute ------------------------

    @Test
    fun hiddenSecondPageEntry_isNotInTheVisibleGestureSet() {
        // "I need help" is slot 7 (L4 R1), on phrase page 1 — not visible while page 0 is showing.
        val hiddenEntry = conversationPage.entries.first { it.phrase == "I need help" }
        val visibleOnPageZero = visibleGestureSetFor(conversationState(phrasePageIndex = 0))
        assertFalse((hiddenEntry.left to hiddenEntry.right) in visibleOnPageZero)
    }

    @Test
    fun hiddenSecondPageEntry_doesNotCreateAmbiguityForAVisibleFirstPageMatch() {
        // Even though "I need help" (L4 R1) has left >= 2, it's not visible on page 0, so it must not
        // make some hypothetical visible (2, x) match ambiguous merely by numeric coincidence.
        val visibleOnPageZero = visibleGestureSetFor(conversationState(phrasePageIndex = 0))
        val hiddenEntry = conversationPage.entries.first { it.phrase == "I need help" }
        assertFalse((hiddenEntry.left to hiddenEntry.right) in visibleOnPageZero)
        // Stop (L2 R3) remains unambiguous on page 0 regardless of what's hidden on page 1.
        assertTrue(isUnambiguousVisibleMatch(2, 3, visibleOnPageZero))
    }

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

    // --- Previous/Next phrase navigation still works ------------------------------------------

    @Test
    fun previousNextPhraseNavigation_stillWorks_andIsQuicklyResolvable() {
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

        assertTrue(isQuicklyResolvable(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT, pageZero))
        assertTrue(isQuicklyResolvable(GuidedModeNavigation.PREVIOUS_LEFT, GuidedModeNavigation.PREVIOUS_RIGHT, pageOne))
    }

    @Test
    fun previousPhraseNavigation_isUnmatched_notQuicklyResolvable_onFirstPage() {
        val pageZero = conversationState(phrasePageIndex = 0)
        val result = GuidedNavigationController.processSequence(
            GuidedModeNavigation.PREVIOUS_LEFT, GuidedModeNavigation.PREVIOUS_RIGHT, pageZero,
            PreferredLanguage.English, uiStrings, catalogContext = catalogContext
        )
        assertEquals(GuidedSequenceResult.Unmatched, result)
    }

    // --- Two-eye reserved navigation codes (Select, Back) share the vocabulary ambiguity rule ---

    @Test
    fun selectCode_isNotEvenAValidActionOnTheVocabularyPage_soItMustNeverQuickResolveThere() {
        // Select (L1 R1) isn't a recognised action while viewing phrases at all (it only means
        // something on the Category Menu / preferences-adjustment screens) — this was the real-device
        // bug: the OLD code treated ANY exact match of a reserved global-nav code as always
        // "quickly resolvable" purely by number, regardless of whether it was actually valid on the
        // current screen. Since L1 R1 is a component-wise prefix of both Yes (L2 R1) and Stop
        // (L2 R3), that stray early finalize silently reset the whole sequence — cutting the user off
        // before they could complete either phrase.
        val state = conversationState()
        val result = GuidedNavigationController.processSequence(
            left = GuidedModeNavigation.SELECT_LEFT, right = GuidedModeNavigation.SELECT_RIGHT, state = state,
            language = PreferredLanguage.English, uiStrings = uiStrings, catalogContext = catalogContext
        )
        assertEquals(GuidedSequenceResult.Unmatched, result)
        assertFalse(
            (GuidedModeNavigation.SELECT_LEFT to GuidedModeNavigation.SELECT_RIGHT) in visibleGestureSetFor(state)
        )
        assertFalse(isQuicklyResolvable(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT, state))
        assertFalse(
            finalizes(
                GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT, state,
                idleMs = GuidedModeNavigation.QUICK_RESOLVE_IDLE_MS + 200L
            )
        )
    }

    private fun categoryMenuState(): GuidedNavigationState =
        GuidedNavigationState(
            screenMode = GuidedOverlayScreenMode.CategoryMenu,
            categoryMenuSelection = 0,
            draftResponseTimeSec = catalogContext.responseTimeSec,
            draftSensitivityLevel = catalogContext.sensitivityLevel
        )

    @Test
    fun selectSequence_isAmbiguous_onCategoryMenu_becauseDirectCategoryShortcutsDominateIt() {
        // On the Category Menu, Select (L1 R1) IS a real action (confirm the highlighted category) —
        // but every one of the six direct category shortcuts (e.g. L2 R1) also component-wise
        // dominates L1 R1, so Select must wait for the full idle timeout here too rather than
        // quick-resolving, exactly like Yes/Stop on the phrase page.
        val state = categoryMenuState()
        val visible = visibleGestureSetFor(state)
        assertTrue((GuidedModeNavigation.SELECT_LEFT to GuidedModeNavigation.SELECT_RIGHT) in visible)
        assertTrue(isAmbiguousVisibleMatch(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT, visible))
        assertFalse(isQuicklyResolvable(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT, state))
        val result = GuidedNavigationController.processSequence(
            left = GuidedModeNavigation.SELECT_LEFT, right = GuidedModeNavigation.SELECT_RIGHT, state = state,
            language = PreferredLanguage.English, uiStrings = uiStrings, catalogContext = catalogContext
        )
        assertTrue(result is GuidedSequenceResult.Navigate)
    }

    @Test
    fun backSequence_isAmbiguous_onCategoryMenu_whenALongerVisibleCategoryShortcutDominatesIt() {
        // Back is L2 R2. Category shortcuts L3 R2 and L2 R3 both component-wise dominate it, so Back
        // must also wait rather than always quick-resolve.
        val state = categoryMenuState()
        val visible = visibleGestureSetFor(state)
        assertTrue((GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT) in visible)
        assertTrue(isAmbiguousVisibleMatch(GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT, visible))
        assertFalse(isQuicklyResolvable(GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT, state))
    }

    @Test
    fun selectAndBack_stillResolveAfterTheFullIdleTimeout_onCategoryMenu() {
        val state = categoryMenuState()
        val idleTimeoutMs = SequenceProcessingDelay.toMillis(catalogContext.responseTimeSec)
        assertTrue(finalizes(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT, state, idleMs = idleTimeoutMs))
        assertTrue(finalizes(GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT, state, idleMs = idleTimeoutMs))
    }

    @Test
    fun previousNextCategories_remainAlwaysQuicklyResolvable_asSingleEyeCodes() {
        // Previous (L2 R0), Next (L0 R2) and Categories (L3 R0) each have a zero blink count on one
        // eye — structurally impossible to confuse with any two-eye vocabulary content — so they must
        // keep the original fast-resolve behavior regardless of what else is visible.
        val state = conversationState()
        assertTrue(isQuicklyResolvable(GuidedModeNavigation.PREVIOUS_LEFT, GuidedModeNavigation.PREVIOUS_RIGHT, state))
        assertTrue(isQuicklyResolvable(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT, state))
        assertTrue(
            isQuicklyResolvable(GuidedModeNavigation.CATEGORIES_LEFT, GuidedModeNavigation.CATEGORIES_RIGHT, state)
        )
    }

    // --- Emergency arming is unaffected by ambiguity gating -----------------------------------

    @Test
    fun emergencySequence_isNeverAmbiguous_andSixLeftWinksStillArmsAcrossFiveSecondGaps() {
        val visible = visibleGestureSetFor(conversationState())
        assertTrue((EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS) in visible)
        assertFalse(isAmbiguousVisibleMatch(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS, visible))

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
    }

    @Test
    fun emergencyCheck_stillPrecedesTheQuickResolveAndFinalizeGate_inMainActivity() {
        // MainActivity short-circuits straight to finalizeSequence() the instant the emergency
        // sequence completes, before quickResolved/shouldFinalizeSequence are even computed — so
        // the new ambiguity gating can never delay or interfere with emergency arming/firing.
        val main = ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main != null)
        val body = main!!.substringAfter("private fun processSequenceWinks(")
        val firstEmergencyCheck = body.indexOf("isEmergencySequence(leftWinks, rightWinks)")
        val quickResolvedComputation = body.indexOf("val quickResolved = isQuicklyResolvableGesture")
        assertTrue("expected an emergency short-circuit in processSequenceWinks", firstEmergencyCheck >= 0)
        assertTrue("expected the generalized quick-resolve gate", quickResolvedComputation >= 0)
        assertTrue(firstEmergencyCheck < quickResolvedComputation)
    }

    // --- Response-time controls still drive the idle timeout used here ------------------------

    @Test
    fun responseTimeControls_stillDriveTheIdleTimeoutUsedByFinalization() {
        val state = conversationState()
        val minWaitMs = SequenceProcessingDelay.toMillis(SequenceProcessingDelay.MIN_SECONDS)
        // At the fastest configured response time, waiting exactly that long finalizes the ambiguous
        // Yes match (full-timeout fallback, since Yes is not eligible for the quick-resolve path).
        assertTrue(finalizes(2, 1, state, idleMs = minWaitMs, idleTimeoutSec = SequenceProcessingDelay.MIN_SECONDS))
        // The same real wait, but with the slowest configured response time selected, must not yet
        // finalize — proving the idle timeout used really is the currently selected response time.
        assertFalse(finalizes(2, 1, state, idleMs = minWaitMs, idleTimeoutSec = SequenceProcessingDelay.MAX_SECONDS))
    }

    @Test
    fun mainActivity_usesEffectiveIdleTimeout_inTheGeneralizedQuickResolveGate() {
        val main = ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main != null)
        assertTrue(main!!.contains("effectiveSequenceIdleTimeoutMs()"))
        assertTrue(main.contains("private fun isQuicklyResolvableGesture(left: Int, right: Int): Boolean"))
        assertTrue(main.contains("isUnambiguousVisibleMatch(left, right, currentVisibleGestureSet())"))
    }
}
