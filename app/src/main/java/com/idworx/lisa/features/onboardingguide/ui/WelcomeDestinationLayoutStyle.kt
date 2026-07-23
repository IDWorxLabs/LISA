package com.idworx.lisa.features.onboardingguide.ui

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * RC7D.40+ — Welcome destination selection: readable type, large targets, even vertical balance.
 * Prefer reclaiming whitespace by distributing actions before introducing scroll.
 */
object WelcomeDestinationLayoutStyle {
    val TargetViewportHeight: Dp = WelcomeIntroductionLayoutStyle.TargetViewportHeight
    val TargetViewportWidth: Dp = WelcomeIntroductionLayoutStyle.TargetViewportWidth

    val ScreenHorizontalPadding: Dp = 16.dp
    val ScreenTopPadding: Dp = 6.dp
    val ScreenBottomPadding: Dp = 4.dp

    val StatusToCardSpacing: Dp = 4.dp
    val CardCornerRadius: Dp = 16.dp
    val CardPadding: Dp = 12.dp

    val TitleToSubtitleSpacing: Dp = 4.dp
    val SubtitleToActionSpacing: Dp = 4.dp
    /** Minimum visual separation between action groups when not using SpaceEvenly. */
    val ActionGroupSpacing: Dp = 4.dp
    val ButtonToInstructionSpacing: Dp = 4.dp

    val CaregiverSpacing: Dp = 2.dp
    val BottomPadding: Dp = 2.dp

    val PrimaryButtonHeight: Dp = 64.dp
    val SecondaryButtonHeight: Dp = 60.dp

    val TitleTextStyle = TextStyle(
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 30.sp,
        textAlign = TextAlign.Center
    )

    val SubtitleTextStyle = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 22.sp,
        textAlign = TextAlign.Center
    )

    val PrimaryButtonTextStyle = TextStyle(
        fontSize = 19.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp,
        textAlign = TextAlign.Center
    )

    val SecondaryButtonTextStyle = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 22.sp,
        textAlign = TextAlign.Center
    )

    val InstructionLineTextStyle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 20.sp,
        textAlign = TextAlign.Center
    )

    val CaregiverTextStyle = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 14.sp,
        textAlign = TextAlign.Center
    )

    // Conservative height budget (dp) for the ordinary 720dp target viewport.
    // Actions fill remaining card height via SpaceEvenly; budget confirms no outer scroll.
    const val BudgetStatusBlockDp: Int = 190
    const val BudgetTitleBlockDp: Int = 64
    const val BudgetActionGroupDp: Int = 96
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

    /** Compact chrome vs the pre-RC7D.40 destination layout (hardcoded 16/10/12/16). */
    fun spacingMoreCompactThanPreRc7d40(): Boolean =
        StatusToCardSpacing <= 6.dp &&
            ActionGroupSpacing <= 6.dp &&
            ButtonToInstructionSpacing <= 6.dp &&
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
            (block.contains("ActionGroupSpacing") || block.contains("SpaceEvenly")) &&
            block.contains("StatusToCardSpacing")
    }

    fun destinationActionsSpreadEvenly(source: String): Boolean {
        val block = destinationBlock(source) ?: return false
        return block.contains("SpaceEvenly") &&
            block.contains("weight(1f")
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
        // Instruction under the button and detached sequence Text must not remain.
        val instructionRemoved = !block.contains("continueInstruction")
        val detachedSequenceRemoved = !Regex(
            """continueInstruction\(\)[\s\S]*?Text\(\s*\n\s*text = WelcomeEyeNavigationAuthority\.continueSequenceLabel"""
        ).containsMatchIn(block)
        return hasSecondaryInButton && instructionRemoved && detachedSequenceRemoved
    }

    private fun destinationBlock(source: String): String? {
        val start = source.indexOf("fun WelcomeDestinationSelectionScreen")
        if (start < 0) return null
        val end = source.indexOf("fun WelcomeBlinkNotationExplanation", start)
            .takeIf { it > start } ?: source.length
        return source.substring(start, end)
    }
}
