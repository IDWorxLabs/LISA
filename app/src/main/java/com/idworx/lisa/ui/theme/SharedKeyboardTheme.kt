package com.idworx.lisa.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * RC8.2 — canonical visual language for every keyboard-driven communication workspace
 * (Feedback text editing, Phrase Management / Custom Phrase composer, and future keyboards).
 *
 * Feedback is the reference implementation. Phrase Management inherits these tokens so both
 * screens share one system: surface, status, input card, outlined actions, and key tray.
 */
object SharedKeyboardTheme {

    /** Opaque dark workspace surface (Feedback reference). */
    val SurfaceBackground: Color = LisaWorkspaceVisualStyle.SolidPanelBackground
    val SurfaceCornerRadius: Dp = LisaWorkspaceVisualStyle.PanelCornerRadius
    val SurfaceContentPadding: Dp = LisaWorkspaceVisualStyle.PanelContentPadding
    val OverlayScrim: Color = Color.Black.copy(alpha = 0.40f)
    val HorizontalInset: Dp = 8.dp
    val VerticalInset: Dp = 6.dp

    /** Section rhythm shared across keyboard workspaces. */
    val SectionSpacing: Dp = 8.dp
    val TightSpacing: Dp = 6.dp
    val ActionRowSpacing: Dp = 4.dp

    /**
     * Neutral status chrome — never a full-width green/dark-green overlay.
     * Only the indicator dot uses [LisaStatusGreen] when tracking is ready.
     */
    val StatusBackground: Color = Color.White.copy(alpha = 0.06f)
    val StatusCornerRadius: Dp = 8.dp
    val StatusHorizontalPadding: Dp = 10.dp
    val StatusVerticalPadding: Dp = 4.dp
    val StatusIndicatorSize: Dp = 7.dp
    val StatusReadyIndicator: Color = LisaStatusGreen
    val StatusIdleIndicator: Color = LisaWhite.copy(alpha = 0.75f)
    val StatusLabelColor: Color = LisaWhite
    val StatusSecondaryLabelColor: Color = LisaWhite.copy(alpha = 0.82f)
    val StatusLabelSize: TextUnit = 12.sp

    /** Input / draft phrase card (Feedback draft field reference). */
    val InputCardBackground: Color = LisaWorkspaceVisualStyle.CardBackground
    val InputCardCornerRadius: Dp = LisaWorkspaceVisualStyle.CardCornerRadius
    val InputCardPadding: Dp = 12.dp
    val InputTitleColor: Color = LisaBlueDark.copy(alpha = 0.70f)
    val InputTitleSize: TextUnit = 11.sp
    val InputBodyColor: Color = LisaBlueDark
    val InputBodySize: TextUnit = 18.sp
    val InputPlaceholderColor: Color = LisaBlueDark.copy(alpha = 0.55f)

    /** Outlined action chrome (Feedback legend + command bar reference). */
    val ActionBackground: Color = LisaWorkspaceVisualStyle.OutlinedKeyboardNavBackground
    val ActionBorder: Color = LisaWorkspaceVisualStyle.OutlinedKeyboardNavBorder
    val ActionContent: Color = LisaWorkspaceVisualStyle.OutlinedKeyboardNavContent
    val ActionCornerRadius: Dp = LisaWorkspaceVisualStyle.OutlinedKeyboardNavCornerRadius
    val ActionBorderWidth: Dp = LisaWorkspaceVisualStyle.OutlinedKeyboardNavBorderWidth
    val ActionSelectedBackground: Color = LisaBlue.copy(alpha = 0.22f)
    val ActionSelectedBorder: Color = LisaWhite
    val ActionDisabledBackground: Color = Color.White.copy(alpha = 0.06f)
    val ActionDisabledBorder: Color = Color.White.copy(alpha = 0.28f)
    val ActionDisabledContent: Color = LisaGray
    val ActionTitleSize: TextUnit = 11.sp
    val ActionSequenceSize: TextUnit = 10.sp
    val ActionIconSize: TextUnit = 18.sp
    val ActionMinHeight: Dp = 52.dp
    val ActionGridBackground: Color = Color.Transparent

    /** Bottom-anchored eye keyboard tray + keys. */
    val KeyboardTrayBackground: Color = Color(0xFF1A2332).copy(alpha = 0.92f)
    val KeyboardTrayTopCornerRadius: Dp = 14.dp
    val KeyboardTrayHorizontalPadding: Dp = 6.dp
    val KeyboardTrayVerticalPadding: Dp = 8.dp
    val KeyBackground: Color = Color.White.copy(alpha = 0.94f)
    val KeyHighlightFill: Color = LisaBlue.copy(alpha = 0.58f)
    val KeyHighlightBorder: Color = LisaBlueDark
    val KeyCornerRadius: Dp = 10.dp
    val KeyHighlightBorderWidth: Dp = 2.5.dp
}
