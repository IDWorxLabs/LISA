package com.idworx.lisa.features.feedbackemail

/**
 * Session-only Feedback form rules (LISA V1).
 *
 * Feedback field text lives in memory for the running process only. It is never restored
 * from durable preferences after a fresh launch. Email handoff must not clear the form;
 * the neutral “email was opened” message must wait until LISA is foreground again.
 */
object LisaFeedbackSessionAuthority {
    /** Delay after onResume before showing the return message (cancels if onPause fires again). */
    const val RETURN_MESSAGE_DELAY_MS: Long = 700L

    /** Auto-dismiss for the in-panel return / no-handler status. */
    const val STATUS_AUTO_DISMISS_MS: Long = 8_000L

    /** Durable SharedPreferences key used by older builds — removed on startup. */
    const val LEGACY_FEEDBACK_DRAFT_PREFS_KEY: String = "feedback_draft_json"

    /**
     * After a successful email-app launch, defer the neutral status until LISA stays resumed.
     * Do not show toast/status while the system chooser may still be visible.
     */
    fun shouldDeferOpenedMessageUntilReturn(): Boolean = true

    /** No-handler failures stay in LISA — show the fallback immediately. */
    fun shouldShowNoHandlerMessageImmediately(): Boolean = true
}
