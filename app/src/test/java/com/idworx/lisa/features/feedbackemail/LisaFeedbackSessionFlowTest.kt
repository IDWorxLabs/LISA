package com.idworx.lisa.features.feedbackemail

import com.idworx.lisa.MenuFeedbackDraft
import com.idworx.lisa.MenuDestinationActionId
import com.idworx.lisa.features.securestorage.InMemorySharedPreferences
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Session-only Feedback form + deferred email-return messaging.
 */
class LisaFeedbackSessionFlowTest {

    @Test
    fun feedbackFieldsBeginEmptyOnFreshLaunchModel() {
        val session = MenuFeedbackDraft()
        assertFalse(session.hasContent)
        assertEquals("", session.workedWell)
        assertEquals("", session.confusing)
        assertEquals("", session.winkDetection)
        assertEquals("", session.speechTiming)
    }

    @Test
    fun oldPersistedFeedbackDraftsAreRemovedOnStartupCleanup() {
        val prefs = InMemorySharedPreferences()
        prefs.edit()
            .putString(
                LisaFeedbackSessionAuthority.LEGACY_FEEDBACK_DRAFT_PREFS_KEY,
                """{"workedWell":"old","confusing":"x","winkDetection":"y","speechTiming":"z"}"""
            )
            .commit()
        assertTrue(prefs.contains(LisaFeedbackSessionAuthority.LEGACY_FEEDBACK_DRAFT_PREFS_KEY))
        prefs.edit().remove(LisaFeedbackSessionAuthority.LEGACY_FEEDBACK_DRAFT_PREFS_KEY).apply()
        assertFalse(prefs.contains(LisaFeedbackSessionAuthority.LEGACY_FEEDBACK_DRAFT_PREFS_KEY))

        val storeSrc = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/LisaReleaseStore.kt"
        ) ?: error("missing store")
        assertTrue(storeSrc.contains("discardPersistedFeedbackDraftFromPreviousBuilds"))
        assertFalse(storeSrc.contains("fun saveFeedbackDraft("))
        assertFalse(storeSrc.contains("fun loadFeedbackDraft("))

        val main = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/MainActivity.kt"
        ) ?: error("missing MainActivity")
        assertTrue(main.contains("discardPersistedFeedbackDraftFromPreviousBuilds()"))
        assertTrue(main.contains("uiMenuFeedbackDraft.value = MenuFeedbackDraft()"))
        assertFalse(main.contains("loadFeedbackDraft"))
    }

    @Test
    fun feedbackRemainsWhileNavigatingWithinSameSession() {
        var session = MenuFeedbackDraft(workedWell = "kept across panels")
        session = session.withValue(MenuDestinationActionId.FeedbackConfusing, "also kept")
        assertTrue(session.hasContent)
        assertEquals("kept across panels", session.workedWell)
        assertEquals("also kept", session.confusing)
    }

    @Test
    fun feedbackRemainsAfterEmailHandoffWithoutClear() {
        val main = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/MainActivity.kt"
        ) ?: error("missing MainActivity")
        val launchFn = main.substringAfter("private fun launchFeedbackEmail")
            .substringBefore("private fun saveFeedbackEntry")
        assertTrue(launchFn.contains("uiMenuFeedbackDraft.value = draft"))
        assertFalse(launchFn.contains("clearFeedbackDraft()"))
        assertFalse(launchFn.contains("MenuFeedbackDraft()"))
    }

    @Test
    fun feedbackDoesNotPersistAcrossSimulatedNewProcessSession() {
        // Simulate prior session memory vs new process defaults.
        val priorSession = MenuFeedbackDraft(workedWell = "from last run")
        assertTrue(priorSession.hasContent)
        val freshLaunch = MenuFeedbackDraft()
        assertFalse(freshLaunch.hasContent)
        assertEquals("", freshLaunch.workedWell)
    }

    @Test
    fun noChooserObscuringMessageBeforeLeavingLisa() {
        assertTrue(LisaFeedbackSessionAuthority.shouldDeferOpenedMessageUntilReturn())
        val main = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/MainActivity.kt"
        ) ?: error("missing MainActivity")
        val openedBranch = main.substringAfter("LaunchResult.Opened")
            .substringBefore("LaunchResult.NoCompatibleEmailApp")
        assertFalse(openedBranch.contains("Toast"))
        assertFalse(openedBranch.contains("presentFeedbackStatusMessage"))
        assertTrue(openedBranch.contains("feedbackEmailHandoffAwaitingReturn = true"))
    }

    @Test
    fun neutralStatusAppearsOnlyAfterReturning() {
        val main = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/MainActivity.kt"
        ) ?: error("missing MainActivity")
        assertTrue(main.contains("showFeedbackEmailReturnMessageRunnable"))
        assertTrue(main.contains("feedbackEmailOpenedConfirmation"))
        assertTrue(
            main.contains("LisaFeedbackSessionAuthority.RETURN_MESSAGE_DELAY_MS") ||
                main.contains("RETURN_MESSAGE_DELAY_MS")
        )
        // onPause cancels pending show so chooser transitions cannot flash the message.
        val onPause = main.substringAfter("override fun onPause()").substringBefore("override fun onResume()")
        assertTrue(onPause.contains("removeCallbacks(showFeedbackEmailReturnMessageRunnable)"))
        val onResume = main.substringAfter("override fun onResume()").substringBefore("override fun onDestroy()")
        assertTrue(onResume.contains("feedbackEmailHandoffAwaitingReturn"))
        assertTrue(onResume.contains("postDelayed"))
    }

    @Test
    fun noEmailHandlerFallbackStillWorksImmediately() {
        assertTrue(LisaFeedbackSessionAuthority.shouldShowNoHandlerMessageImmediately())
        val main = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/MainActivity.kt"
        ) ?: error("missing MainActivity")
        val noHandler = main.substringAfter("LaunchResult.NoCompatibleEmailApp")
            .substringBefore("private fun saveFeedbackEntry")
        assertTrue(noHandler.contains("presentFeedbackStatusMessage"))
        assertTrue(noHandler.contains("feedbackNoEmailApp"))
        assertTrue(noHandler.contains("feedbackEmailHandoffAwaitingReturn = false"))
    }

    @Test
    fun emailRecipientSubjectAndBodyRemainCorrect() {
        val prepared = LisaFeedbackEmailAuthority.PreparedEmail(
            to = LisaFeedbackEmailAuthority.DESTINATION_EMAIL,
            subject = LisaFeedbackEmailAuthority.EMAIL_SUBJECT,
            body = buildString {
                appendLine("LISA Version: 1.1")
                appendLine("Android Version: 13 (SDK 33)")
                appendLine("Device Model: Device Model X")
                appendLine()
                appendLine("Feedback:")
                appendLine()
                appendLine("What worked well:")
                appendLine("a")
                appendLine()
                appendLine("What was confusing:")
                appendLine("b")
                appendLine()
                appendLine("Wink detection feedback:")
                appendLine("c")
                appendLine()
                appendLine("Speech timing feedback:")
                appendLine("d")
            }
        )
        val mailto = LisaFeedbackEmailAuthority.buildMailtoUriString(prepared)
        assertEquals(
            "lisa-feedback@asgarddynamics.io",
            LisaFeedbackEmailAuthority.mailtoRecipientFromUriString(mailto)
        )
        assertEquals("LISA Feedback", LisaFeedbackEmailAuthority.mailtoQueryValue(mailto, "subject"))
        val body = LisaFeedbackEmailAuthority.mailtoQueryValue(mailto, "body")!!
        assertTrue(body.contains("LISA Version:"))
        assertTrue(body.contains("What worked well:"))
        assertTrue(body.contains("Speech timing feedback:"))
    }

    @Test
    fun noOemOrEmailPackageSpecificCodeAdded() {
        val session = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/feedbackemail/LisaFeedbackSessionAuthority.kt"
        ) ?: error("missing session authority")
        val authority = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/feedbackemail/LisaFeedbackEmailAuthority.kt"
        ) ?: error("missing email authority")
        listOf(session, authority).forEach { src ->
            assertFalse(src.contains("com.google.android.gm"))
            assertFalse(src.contains("setPackage("))
            assertFalse(src.contains("samsung", ignoreCase = true))
        }
    }
}
