package com.idworx.lisa.features.systembarinsets

import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier

/**
 * RC8.43 — single authoritative safe-area policy for normal LISA application content.
 *
 * Splash may remain edge-to-edge. After the branded splash dismisses, [safeApplicationContent]
 * must be applied exactly once around LisaRootUI (and nowhere else as a competing root).
 * Do not re-apply status/navigation padding on every destination screen.
 */
object LisaSystemBarInsetAuthority {

    /**
     * Pads content below the status bar and above the navigation / gesture inset.
     * Apply once at the normal-application Compose root.
     */
    fun safeApplicationContent(): Modifier = Modifier.systemBarsPadding()

    /**
     * Usable Compose content height after system-bar insets are applied at the root.
     * Use this model in layout assertions — not the full physical display height alone.
     */
    fun safeContentHeightDp(
        physicalScreenHeightDp: Int,
        statusBarInsetDp: Int,
        navigationBarInsetDp: Int
    ): Int = (physicalScreenHeightDp - statusBarInsetDp - navigationBarInsetDp).coerceAtLeast(0)

    /**
     * True when a vertical content span stays inside the safe application viewport.
     * [contentTopDp] / [contentBottomDp] are measured from the top of the padded root.
     */
    fun contentFitsSafeViewport(
        contentTopDp: Int,
        contentBottomDp: Int,
        safeHeightDp: Int
    ): Boolean =
        contentTopDp >= 0 &&
            contentBottomDp <= safeHeightDp &&
            contentTopDp < contentBottomDp

    /** Representative Samsung Galaxy-style insets for regression layout checks. */
    object SamsungViewportFixtures {
        const val PHYSICAL_HEIGHT_DP = 800
        const val PHYSICAL_WIDTH_DP = 360
        const val STATUS_BAR_DP = 24
        const val THREE_BUTTON_NAV_DP = 48
        const val GESTURE_NAV_DP = 16

        /** Approximate Welcome Continue band (header…pinned action) inside the safe root. */
        const val WELCOME_HEADER_TOP_DP = 0
        const val WELCOME_CONTINUE_BOTTOM_DP = 700

        /** Approximate Communication Menu control bottom edge inside the safe root. */
        const val COMMUNICATION_HEADER_TOP_DP = 0
        const val COMMUNICATION_MENU_BOTTOM_DP = 720
    }
}
