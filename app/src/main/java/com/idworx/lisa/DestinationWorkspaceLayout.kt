package com.idworx.lisa

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Deterministic responsive allocation for the destination content and fixed command panel. */
data class DestinationWorkspaceWidths(
    val contentWidthDp: Dp,
    val navigationWidthDp: Dp,
    val usesKeyboardFocusedLayout: Boolean
)

object DestinationWorkspaceWidthAuthority {
    val MinimumContentWidth = 200.dp
    val MinimumNavigationWidth = 88.dp
    val PreferredNavigationWidth = 118.dp
    val MinimumCompleteKeyboardWidth = 340.dp
    const val MaximumNavigationFraction = 0.28f

    fun calculateDestinationWorkspaceWidths(
        availableWidthDp: Dp,
        horizontalSpacingDp: Dp,
        keyboardActive: Boolean = false
    ): DestinationWorkspaceWidths {
        val available = availableWidthDp.value.coerceAtLeast(0f)
        val spacing = horizontalSpacingDp.value.coerceIn(0f, available)
        val usable = (available - spacing).coerceAtLeast(0f)

        if (usable == 0f) {
            return DestinationWorkspaceWidths(0.dp, 0.dp, keyboardActive)
        }

        val keyboardFocused = keyboardActive &&
            usable < MinimumCompleteKeyboardWidth.value + MinimumNavigationWidth.value
        if (keyboardFocused) {
            return DestinationWorkspaceWidths(
                contentWidthDp = usable.dp,
                navigationWidthDp = 0.dp,
                usesKeyboardFocusedLayout = true
            )
        }

        val requestedNavigation = minOf(
            PreferredNavigationWidth.value,
            available * MaximumNavigationFraction
        ).coerceAtLeast(MinimumNavigationWidth.value)
        val contentMinimum = if (keyboardActive) {
            MinimumCompleteKeyboardWidth.value
        } else {
            MinimumContentWidth.value
        }

        val navigation = if (usable >= contentMinimum + MinimumNavigationWidth.value) {
            requestedNavigation.coerceAtMost(usable - contentMinimum)
        } else {
            minOf(MinimumNavigationWidth.value, usable * MaximumNavigationFraction)
        }.coerceIn(0f, usable)
        val content = (usable - navigation).coerceAtLeast(0f)

        return DestinationWorkspaceWidths(
            contentWidthDp = content.dp,
            navigationWidthDp = navigation.dp,
            usesKeyboardFocusedLayout = false
        )
    }
}
