package com.idworx.lisa.features.feedbackemail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.GuidedNavigationActionButton
import com.idworx.lisa.LisaUiStrings
import com.idworx.lisa.formatWinkSequenceShort
import com.idworx.lisa.ui.theme.LisaBlueDark
import com.idworx.lisa.ui.theme.LisaWhite
import com.idworx.lisa.ui.theme.SharedKeyboardTheme

/**
 * Two-stage caregiver-assistance surface for Feedback email handoff.
 * No scrolling. Touch and wink share the same MainActivity action paths.
 */
@Composable
fun FeedbackCaregiverAssistScreen(
    uiStrings: LisaUiStrings,
    step: FeedbackCaregiverAssistStep,
    onPrimaryAction: () -> Unit,
    onGoBack: () -> Unit
) {
    val primary = FeedbackCaregiverAssistAuthority.PRIMARY_SEQUENCE
    val back = FeedbackCaregiverAssistAuthority.BACK_SEQUENCE
    val primarySequence = formatWinkSequenceShort(primary.first, primary.second)
    val backSequence = formatWinkSequenceShort(back.first, back.second)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (step) {
            FeedbackCaregiverAssistStep.SpeakRequest -> {
                Text(
                    text = uiStrings.feedbackCaregiverSpeakHeading,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = LisaBlueDark,
                    lineHeight = 28.sp
                )
                Text(
                    text = uiStrings.feedbackCaregiverSpeakInstruction,
                    fontSize = 16.sp,
                    color = LisaBlueDark,
                    lineHeight = 22.sp
                )
                PhraseCard(phrase = FeedbackCaregiverAssistAuthority.SPOKEN_HELP_REQUEST)
                Spacer(modifier = Modifier.height(4.dp))
                GuidedNavigationActionButton(
                    symbol = "🔊",
                    title = uiStrings.feedbackCaregiverSayRequest,
                    gestureHint = "",
                    sequenceLabel = primarySequence,
                    enabled = true,
                    compact = false,
                    onClick = onPrimaryAction
                )
                GuidedNavigationActionButton(
                    symbol = "←",
                    title = uiStrings.feedbackCaregiverGoBack,
                    gestureHint = "",
                    sequenceLabel = backSequence,
                    enabled = true,
                    compact = false,
                    onClick = onGoBack
                )
            }
            FeedbackCaregiverAssistStep.OpenEmailApp -> {
                Text(
                    text = uiStrings.feedbackCaregiverReadyHeading,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = LisaBlueDark,
                    lineHeight = 26.sp
                )
                Text(
                    text = uiStrings.feedbackCaregiverReadyMessage,
                    fontSize = 14.sp,
                    color = LisaBlueDark,
                    lineHeight = 19.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                GuidedNavigationActionButton(
                    symbol = "✉",
                    title = uiStrings.feedbackCaregiverOpenEmail,
                    gestureHint = "",
                    sequenceLabel = primarySequence,
                    enabled = true,
                    compact = false,
                    onClick = onPrimaryAction
                )
                GuidedNavigationActionButton(
                    symbol = "←",
                    title = uiStrings.feedbackCaregiverGoBack,
                    gestureHint = "",
                    sequenceLabel = backSequence,
                    enabled = true,
                    compact = false,
                    onClick = onGoBack
                )
            }
        }
    }
}

@Composable
private fun PhraseCard(phrase: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SharedKeyboardTheme.SurfaceCornerRadius))
            .background(LisaWhite)
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "“$phrase”",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = LisaBlueDark,
            lineHeight = 24.sp,
            textAlign = TextAlign.Start
        )
    }
}
