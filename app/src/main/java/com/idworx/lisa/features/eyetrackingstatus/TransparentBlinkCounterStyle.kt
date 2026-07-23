package com.idworx.lisa.features.eyetrackingstatus

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.ui.theme.LisaBlueDark

/**
 * Canonical blink-counter visual authority shared by Communication and pre-Communication.
 *
 * Labels and ● dots always use [LabelColor] ([LisaBlueDark]) for permanent high contrast
 * over camera, dark chrome, and light onboarding surfaces. Background stays transparent.
 */
object TransparentBlinkCounterStyle {
    /** No fill — screen / parent chrome shows through. */
    val Background: Color = Color.Transparent

    /** Permanent high-contrast label and dot colour (Left / Right / ● / —). */
    val LabelColor: Color = LisaBlueDark

    val CornerRadius: Dp = 8.dp
    val HorizontalPadding: Dp = 12.dp
    val VerticalPadding: Dp = 5.dp
    val CompactHorizontalPadding: Dp = 10.dp
    val CompactVerticalPadding: Dp = 4.dp
    val FontSize: TextUnit = 14.sp
    val CompactFontSize: TextUnit = 12.sp
    val LabelWeight: FontWeight = FontWeight.Medium

    const val CanonicalComposableName: String = "BlinkCounterRow"
}

/**
 * Structural contracts for the single shared transparent blink-counter authority.
 */
object TransparentBlinkCounterAuthority {
    fun surfacesDefineCanonicalCounter(source: String): Boolean =
        source.contains("fun BlinkCounterRow") &&
            source.contains("TransparentBlinkCounterStyle") &&
            source.contains("leftDots") &&
            source.contains("rightDots") &&
            source.contains("SpaceBetween")

    fun counterUsesTransparentChrome(source: String): Boolean {
        val start = source.indexOf("fun BlinkCounterRow")
        if (start < 0) return false
        val end = source.indexOf("fun CalibrationEyeTrackingStatusStrip", start)
            .takeIf { it > start } ?: source.length
        val block = source.substring(start, end)
        return block.contains("Color.Transparent") ||
            block.contains("style.Background") ||
            block.contains("TransparentBlinkCounterStyle.Background")
    }

    fun counterHasNoOpaqueGreyOrBlackFill(source: String): Boolean {
        val start = source.indexOf("fun BlinkCounterRow")
        if (start < 0) return false
        val end = source.indexOf("fun CalibrationEyeTrackingStatusStrip", start)
            .takeIf { it > start } ?: source.length
        val block = source.substring(start, end)
        return !block.contains("Color.Black") &&
            !block.contains("LisaGray") &&
            !block.contains("LisaBlueLight.copy") &&
            !block.contains(".border(") &&
            TransparentBlinkCounterStyle.Background == Color.Transparent
    }

    fun labelsUsePermanentLisaBlue(): Boolean =
        TransparentBlinkCounterStyle.LabelColor == LisaBlueDark

    fun noDarkLightLabelBranch(styleSource: String, surfacesSource: String): Boolean {
        val styleObjectStart = styleSource.indexOf("object TransparentBlinkCounterStyle")
        val authorityStart = styleSource.indexOf("object TransparentBlinkCounterAuthority")
        if (styleObjectStart < 0 || authorityStart <= styleObjectStart) return false
        val styleObject = styleSource.substring(styleObjectStart, authorityStart)
        return !styleObject.contains("LabelColorOnDark") &&
            !styleObject.contains("LabelColorOnLight") &&
            !styleObject.contains("fun labelColor(") &&
            !surfacesSource.contains("onLightSurface") &&
            styleObject.contains("val LabelColor:")
    }
    fun communicationUsesSharedCounter(accessibilityUi: String, keyboardUi: String): Boolean =
        accessibilityUi.contains("BlinkCounterRow(") &&
            keyboardUi.contains("BlinkCounterRow(")

    fun noDuplicateBlinkSourceInSurfaces(source: String): Boolean =
        !source.contains("BlinkDetectionProcessor") &&
            !source.contains("leftWinks +=")

    fun styleAuthorityIsTransparent(): Boolean =
        TransparentBlinkCounterStyle.Background == Color.Transparent
}
