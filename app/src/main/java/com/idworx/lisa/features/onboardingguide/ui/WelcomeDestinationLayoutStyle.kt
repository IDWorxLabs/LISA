package com.idworx.lisa.features.onboardingguide.ui

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * RC7D.40 — compact spacing/typography for single-screen Welcome destination selection.
 * Prefer reclaiming whitespace before shrinking accessible button heights.
 */
object WelcomeDestinationLayoutStyle {
    val TargetViewportHeight: Dp = WelcomeIntroductionLayoutStyle.TargetViewportHeight
    val TargetViewportWidth: Dp = WelcomeIntroductionLayoutStyle.TargetViewportWidth

    val ScreenHorizontalPadding: Dp = 16.dp
    val ScreenTopPadding: Dp = 6.dp
    val ScreenBottomPadding: Dp = 4.dp

    val StatusToCardSpacing: Dp = 4.dp
    val CardCornerRadius: Dp = 16.dp
    val CardPadding: Dp = 10.dp

    val TitleToSubtitleSpacing: Dp = 2.dp
    val SubtitleToActionSpacing: Dp = 6.dp
    val ActionGroupSpacing: Dp = 4.dp
    val ButtonToInstructionSpacing: Dp = 2.dp

    val CaregiverSpacing: Dp = 2.dp
    val BottomPadding: Dp = 2.dp

    val PrimaryButtonHeight: Dp = 56.dp
    val SecondaryButtonHeight: Dp = 48.dp

    val TitleTextStyle = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 24.sp,
        textAlign = TextAlign.Center
    )

    val SubtitleTextStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 18.sp,
        textAlign = TextAlign.Center
    )

    val InstructionLineTextStyle = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 16.sp,
        textAlign = TextAlign.Center
    )

    val CaregiverTextStyle = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 14.sp,
        textAlign = TextAlign.Center
    )

    // Conservative height budget (dp) for the ordinary 720dp target viewport.
    const val BudgetStatusBlockDp: Int = 190
    const val BudgetTitleBlockDp: Int = 48
    const val BudgetActionGroupDp: Int = 72
    const val BudgetCaregiverDp: Int = 36
    const val BudgetChromePaddingDp: Int = 28

    fun estimatedContentHeightDp(): Int =
        BudgetStatusBlockDp +
            BudgetTitleBlockDp +
            (BudgetActionGroupDp * 3) +
            BudgetCaregiverDp +
            BudgetChromePaddingDp

    fun fitsTargetViewportWithoutOuterScroll(
        viewportHeightDp: Int = TargetViewportHeight.value.toInt()
    ): Boolean = estimatedContentHeightDp() <= viewportHeightDp

    /** Compact gaps vs the pre-RC7D.40 destination layout (hardcoded 16/10/12/16). */
    fun spacingMoreCompactThanPreRc7d40(): Boolean =
        StatusToCardSpacing <= 6.dp &&
            ActionGroupSpacing <= 6.dp &&
            ButtonToInstructionSpacing <= 4.dp &&
            CardPadding <= 12.dp &&
            SubtitleToActionSpacing <= 8.dp
}

/**
 * Structural contracts for RC7D.40 single-screen destination selection.
 */
object WelcomeDestinationLayoutAuthority {
    fun destinationSourceOmitsOuterVerticalScroll(source: String): Boolean {
        val block = destinationBlock(source) ?: return false
        return !Regex("""fillMaxSize\(\)\s*\n\s*\.verticalScroll""").containsMatchIn(block) &&
            !block.contains(".verticalScroll(rememberScrollState())")
    }

    fun destinationUsesLayoutTokens(source: String): Boolean {
        val block = destinationBlock(source) ?: return false
        return block.contains("WelcomeDestinationLayoutStyle") &&
            block.contains("ActionGroupSpacing") &&
            block.contains("StatusToCardSpacing")
    }

    fun destinationUsesCombinedInstructionLine(source: String): Boolean {
        val block = destinationBlock(source) ?: return false
        return source.contains("combinedActionHint") ||
            block.contains(" · ") ||
            block.contains("combinedInstruction")
    }

    fun caregiverRemainsInDestinationScreen(source: String): Boolean {
        val block = destinationBlock(source) ?: return false
        return block.contains("CaregiverAdvancedSkipLink") &&
            block.contains("caregiverAdvancedSkipNavigation")
    }

    fun continueMergesSequenceIntoButton(source: String): Boolean {
        val start = source.indexOf("fun WelcomeIntroductionContinueAction")
        if (start < 0) return false
        val end = source.indexOf("fun WelcomeDestinationSelectionScreen", start)
            .takeIf { it > start } ?: source.length
        val block = source.substring(start, end)
        val hasSecondaryInButton = block.contains("secondaryText") &&
            block.contains("continueSequenceLabel")
        val instructionBelow = block.contains("continueInstruction")
        // Standalone sequence Text after instruction must not remain.
        val detachedSequenceRemoved = !Regex(
            """continueInstruction\(\)[\s\S]*?Text\(\s*\n\s*text = WelcomeEyeNavigationAuthority\.continueSequenceLabel"""
        ).containsMatchIn(block)
        return hasSecondaryInButton && instructionBelow && detachedSequenceRemoved
    }

    private fun destinationBlock(source: String): String? {
        val start = source.indexOf("fun WelcomeDestinationSelectionScreen")
        if (start < 0) return null
        val end = source.indexOf("fun WelcomeBlinkNotationExplanation", start)
            .takeIf { it > start } ?: source.length
        return source.substring(start, end)
    }
}
