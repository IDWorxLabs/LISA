package com.idworx.lisa.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Canonical Communication workspace panel / card metrics.
 *
 * [SolidPanelBackground] is the opaque dark grey behind phrase and category cards.
 * Guided overlay may still blend with the camera via [OverlayPanelBackground]; Main Menu
 * must use the solid token so empty space never exposes the preview (RC7D.30).
 */
object LisaWorkspaceVisualStyle {

    /** Base dark grey used behind Communication phrase/category cards. */
    val SolidPanelBackground: Color = Color(0xFF0D1B2A)

    /** Existing guided/composer overlay blend — keep alpha only outside Main Menu. */
    val OverlayPanelBackground: Color = SolidPanelBackground.copy(alpha = 0.72f)

    val PanelCornerRadius = 14.dp
    val PanelContentPadding = 10.dp

    val CardCornerRadius = 12.dp
    val CardHorizontalPadding = 12.dp
    val CardVerticalPadding = 12.dp
    val CardBackground: Color = Color.White.copy(alpha = 0.90f)
    val CardSelectedBackground: Color = LisaBlue.copy(alpha = 0.30f)
    val CardNumberSize = 16.sp
    val CardTitleSize = 17.sp
    val CardTitleLineHeight = 22.sp

    val SectionHeadingSize = 13.sp
    val MenuTitleSize = 16.sp
    val IndicatorSize = 13.sp

    val NavPanelWidth = 118.dp
    val NavPanelCornerRadius = 12.dp
    val NavPanelBackground: Color = Color.White.copy(alpha = 0.88f)

    /** Horizontal inset matching EverydayCommunicationPanel / blink-status header. */
    val FullWidthChromeHorizontalPadding = 10.dp
    val FullWidthActionMinHeight = 52.dp
    val FullWidthActionCornerRadius = 10.dp

    /**
     * Canonical composer / guided keyboard navigation action colours (RC7D.36).
     * Reuse these tokens — do not invent per-file white or alternate blues.
     */
    val NavActionEnabledBackground: Color = LisaBlue.copy(alpha = 0.18f)
    val NavActionEnabledBorder: Color = LisaBlue.copy(alpha = 0.45f)
    val NavActionSelectedBackground: Color = LisaBlue.copy(alpha = 0.32f)
    val NavActionSelectedBorder: Color = LisaBlue.copy(alpha = 0.70f)
    val NavActionDisabledBackground: Color = LisaSoftGray.copy(alpha = 0.65f)
    val NavActionDisabledBorder: Color = LisaGray.copy(alpha = 0.35f)
    val NavActionGridBackground: Color = LisaBlueLight.copy(alpha = 0.55f)
    val NavActionPartialHighlight: Color = LisaBlue.copy(alpha = 0.12f)
    val NavActionPrimaryBackground: Color = LisaBlue.copy(alpha = 0.22f)
    val NavActionPrimaryBorder: Color = LisaBlue.copy(alpha = 0.55f)
}
