package com.idworx.lisa.features.onboardingguide.ui

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * RC7D.38/39 — layout + typography tokens for the single-screen Welcome introduction.
 * Larger readable type uses available card height; Continue stays pinned without outer scroll.
 */
object WelcomeIntroductionLayoutStyle {
    /** Typical phone content height used for the no-scroll contract (dp). */
    val TargetViewportHeight: Dp = 720.dp
    val TargetViewportWidth: Dp = 360.dp

    val ScreenHorizontalPadding: Dp = 16.dp
    val ScreenTopPadding: Dp = 8.dp
    val ScreenBottomPadding: Dp = 10.dp

    val StatusToContentGap: Dp = 6.dp
    val ContentToActionGap: Dp = 8.dp

    val SubtitleMaxLines: Int = 3

    val CardCornerRadius: Dp = 16.dp
    val CardContentPadding: Dp = 14.dp
    val CardVerticalSpacing: Dp = 10.dp

    val ExplanationCardPaddingHorizontal: Dp = 12.dp
    val ExplanationCardPaddingVertical: Dp = 12.dp
    val ExplanationLineSpacing: Dp = 8.dp

    val ContinueInstructionTopPadding: Dp = 6.dp
    val ContinueSequenceTopPadding: Dp = 2.dp
    val ContinueButtonHeight: Dp = 56.dp

    /** Below this middle-section height, only the explanation may scroll (Continue stays pinned). */
    val ExplanationInternalScrollThreshold: Dp = 160.dp

    // --- RC7D.39 typography tokens (intro screen only) ---

    val WelcomeTitleTextStyle = TextStyle(
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 30.sp,
        textAlign = TextAlign.Center
    )

    val WelcomeIntroTextStyle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 22.sp,
        textAlign = TextAlign.Center
    )

    val WelcomeExplanationTitleTextStyle = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 24.sp,
        textAlign = TextAlign.Center
    )

    val WelcomeExplanationBodyTextStyle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 22.sp,
        textAlign = TextAlign.Center
    )

    val WelcomeSequenceExampleTextStyle = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 23.sp,
        textAlign = TextAlign.Center
    )

    val WelcomeContinueTextStyle = TextStyle(
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 24.sp,
        textAlign = TextAlign.Center
    )

    val WelcomeContinueInstructionTextStyle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 20.sp,
        textAlign = TextAlign.Center
    )

    val WelcomeContinueSequenceTextStyle = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 22.sp,
        textAlign = TextAlign.Center
    )

    /** @deprecated Prefer [WelcomeTitleTextStyle]; kept for RC7D.38 budget helpers. */
    val TitleFontSize get() = WelcomeTitleTextStyle.fontSize

    /** @deprecated Prefer [WelcomeIntroTextStyle]. */
    val SubtitleFontSize get() = WelcomeIntroTextStyle.fontSize

    /** @deprecated Prefer [WelcomeIntroTextStyle]. */
    val SubtitleLineHeight get() = WelcomeIntroTextStyle.lineHeight

    /** @deprecated Prefer [WelcomeExplanationTitleTextStyle]. */
    val ExplanationTitleSize get() = WelcomeExplanationTitleTextStyle.fontSize

    /** @deprecated Prefer [WelcomeExplanationBodyTextStyle]. */
    val ExplanationBodySize get() = WelcomeExplanationBodyTextStyle.fontSize

    /** @deprecated Prefer [WelcomeExplanationBodyTextStyle]. */
    val ExplanationBodyLineHeight get() = WelcomeExplanationBodyTextStyle.lineHeight

    /** @deprecated Prefer [WelcomeSequenceExampleTextStyle]. */
    val ExplanationExampleSize get() = WelcomeSequenceExampleTextStyle.fontSize

    /** @deprecated Prefer [WelcomeSequenceExampleTextStyle]. */
    val ExplanationExampleLineHeight get() = WelcomeSequenceExampleTextStyle.lineHeight

    /**
     * Conservative height budget (dp) for the ordinary target viewport with RC7D.39 type.
     * Still fits [TargetViewportHeight] without outer scrolling.
     */
    const val BudgetStatusBlockDp: Int = 210
    const val BudgetTitleBlockDp: Int = 95
    const val BudgetExplanationBlockDp: Int = 230
    const val BudgetContinueBlockDp: Int = 110
    const val BudgetChromePaddingDp: Int = 40

    fun estimatedContentHeightDp(): Int =
        BudgetStatusBlockDp + BudgetTitleBlockDp + BudgetExplanationBlockDp +
            BudgetContinueBlockDp + BudgetChromePaddingDp

    fun fitsTargetViewportWithoutOuterScroll(
        viewportHeightDp: Int = TargetViewportHeight.value.toInt()
    ): Boolean = estimatedContentHeightDp() <= viewportHeightDp

    /** RC7D.39 — typography is larger than the RC7D.38 compact baseline. */
    fun typographyLargerThanRc7d38Baseline(): Boolean =
        WelcomeTitleTextStyle.fontSize.value >= 24f &&
            WelcomeIntroTextStyle.fontSize.value >= 16f &&
            WelcomeExplanationTitleTextStyle.fontSize.value >= 20f &&
            WelcomeExplanationBodyTextStyle.fontSize.value >= 16f &&
            WelcomeSequenceExampleTextStyle.fontSize.value >= 17f &&
            WelcomeContinueTextStyle.fontSize.value >= 18f &&
            WelcomeContinueInstructionTextStyle.fontSize.value >= 16f &&
            WelcomeContinueSequenceTextStyle.fontSize.value >= 18f
}

/**
 * Structural contracts for RC7D.38/39 single-screen Welcome introduction.
 */
object WelcomeIntroductionLayoutAuthority {
    fun introductionSourceOmitsDecorativeLogo(source: String): Boolean {
        val start = source.indexOf("fun WelcomeBlinkSequenceIntroductionScreen")
        if (start < 0) return false
        val end = source.indexOf("fun WelcomeDestinationSelectionScreen", start)
            .takeIf { it > start } ?: source.length
        val block = source.substring(start, end)
        return !block.contains("TrainingLisaLogo")
    }

    fun introductionSourceOmitsOuterVerticalScroll(source: String): Boolean {
        val start = source.indexOf("fun WelcomeBlinkSequenceIntroductionScreen")
        if (start < 0) return false
        val end = source.indexOf("fun WelcomeDestinationSelectionScreen", start)
            .takeIf { it > start } ?: source.length
        val block = source.substring(start, end)
        val hasPinnedContinue = block.contains("WelcomeIntroductionContinueAction") &&
            block.contains("weight(1f)")
        val outerScrollRemoved = !block.contains(".fillMaxSize()") ||
            !Regex("""fillMaxSize\(\)\s*\n\s*\.verticalScroll""").containsMatchIn(block)
        val fallbackOnly = block.contains("ExplanationInternalScrollThreshold")
        return hasPinnedContinue && outerScrollRemoved && fallbackOnly
    }

    fun continueAnchoredBelowContent(source: String): Boolean {
        val start = source.indexOf("fun WelcomeBlinkSequenceIntroductionScreen")
        if (start < 0) return false
        val end = source.indexOf("fun WelcomeDestinationSelectionScreen", start)
            .takeIf { it > start } ?: source.length
        val block = source.substring(start, end)
        val continueIndex = block.indexOf("WelcomeIntroductionContinueAction")
        val explanationIndex = block.indexOf("WelcomeBlinkNotationExplanation")
        return continueIndex > explanationIndex && continueIndex >= 0
    }

    fun introductionUsesSharedTypographyTokens(source: String): Boolean {
        val start = source.indexOf("fun WelcomeBlinkSequenceIntroductionScreen")
        if (start < 0) return false
        val end = source.indexOf("fun WelcomeDestinationSelectionScreen", start)
            .takeIf { it > start } ?: source.length
        val introThroughContinue = source.substring(start, end)
        return introThroughContinue.contains("WelcomeTitleTextStyle") &&
            introThroughContinue.contains("WelcomeIntroTextStyle") &&
            source.contains("WelcomeExplanationTitleTextStyle") &&
            source.contains("WelcomeSequenceExampleTextStyle") &&
            source.contains("WelcomeContinueTextStyle") &&
            source.contains("WelcomeContinueInstructionTextStyle") &&
            source.contains("WelcomeContinueSequenceTextStyle")
    }

    fun explanationExpandsToFillAvailableSpace(source: String): Boolean {
        val start = source.indexOf("fun WelcomeBlinkSequenceIntroductionScreen")
        if (start < 0) return false
        val end = source.indexOf("fun WelcomeDestinationSelectionScreen", start)
            .takeIf { it > start } ?: source.length
        val block = source.substring(start, end)
        return block.contains("fillMaxHeight()") ||
            block.contains("SpaceEvenly") ||
            block.contains("expandVertically")
    }
}
