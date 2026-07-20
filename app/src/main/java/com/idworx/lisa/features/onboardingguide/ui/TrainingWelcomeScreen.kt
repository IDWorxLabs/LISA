package com.idworx.lisa.features.onboardingguide.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.LisaUiStrings
import com.idworx.lisa.features.eyetrackingstatus.CompactEyeTrackingHeader
import com.idworx.lisa.features.eyetrackingstatus.EyeTrackingStatusUiState
import com.idworx.lisa.features.intelligentstartup.authority.WelcomeEyeNavigationAuthority
import com.idworx.lisa.features.intelligentstartup.authority.WelcomeStage
import com.idworx.lisa.features.silentwelcome.LisaSpeechPolicy
import com.idworx.lisa.features.silentwelcome.metadata.SilentWelcomeLaunchFlowMetadata
import com.idworx.lisa.ui.theme.LisaBlue
import com.idworx.lisa.ui.theme.LisaBlueDark
import com.idworx.lisa.ui.theme.LisaBlueLight
import com.idworx.lisa.ui.theme.LisaGray
import com.idworx.lisa.ui.theme.LisaWhite
import com.idworx.lisa.ui.theme.LisaWorkspaceVisualStyle

/**
 * RC7D.37–40 — two-step Welcome; Continue merges L1 R1; destination fits one screen.
 */
@Composable
fun TrainingFirstLaunchChoiceScreen(
    uiStrings: LisaUiStrings,
    welcomeStage: WelcomeStage = WelcomeStage.BlinkSequenceIntroduction,
    onContinueToDestination: () -> Unit = {},
    onStartGuidedLearning: () -> Unit,
    onSkipToWorkspace: () -> Unit,
    onBackToIntroduction: () -> Unit = {},
    onSkipToNavigationTraining: () -> Unit = {},
    eyeTrackingStatus: EyeTrackingStatusUiState = EyeTrackingStatusUiState(),
    selectedChoiceLabel: String? = null,
    onDecreaseSensitivity: () -> Unit = {},
    onIncreaseSensitivity: () -> Unit = {},
    onDecreaseResponseTime: () -> Unit = {},
    onIncreaseResponseTime: () -> Unit = {}
) {
    when (welcomeStage) {
        WelcomeStage.BlinkSequenceIntroduction -> WelcomeBlinkSequenceIntroductionScreen(
            uiStrings = uiStrings,
            eyeTrackingStatus = eyeTrackingStatus,
            onContinue = onContinueToDestination,
            onDecreaseSensitivity = onDecreaseSensitivity,
            onIncreaseSensitivity = onIncreaseSensitivity,
            onDecreaseResponseTime = onDecreaseResponseTime,
            onIncreaseResponseTime = onIncreaseResponseTime
        )
        WelcomeStage.DestinationSelection -> WelcomeDestinationSelectionScreen(
            uiStrings = uiStrings,
            eyeTrackingStatus = eyeTrackingStatus,
            selectedChoiceLabel = selectedChoiceLabel,
            onStartGuidedLearning = onStartGuidedLearning,
            onSkipToWorkspace = onSkipToWorkspace,
            onBackToIntroduction = onBackToIntroduction,
            onSkipToNavigationTraining = onSkipToNavigationTraining,
            onDecreaseSensitivity = onDecreaseSensitivity,
            onIncreaseSensitivity = onIncreaseSensitivity,
            onDecreaseResponseTime = onDecreaseResponseTime,
            onIncreaseResponseTime = onIncreaseResponseTime
        )
    }
}

@Composable
private fun WelcomeBlinkSequenceIntroductionScreen(
    uiStrings: LisaUiStrings,
    eyeTrackingStatus: EyeTrackingStatusUiState,
    onContinue: () -> Unit,
    onDecreaseSensitivity: () -> Unit,
    onIncreaseSensitivity: () -> Unit,
    onDecreaseResponseTime: () -> Unit,
    onIncreaseResponseTime: () -> Unit
) {
    val style = WelcomeIntroductionLayoutStyle
    TrainingSoftBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = style.ScreenHorizontalPadding)
                .padding(top = style.ScreenTopPadding, bottom = style.ScreenBottomPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP — live tracking chrome (compact). No decorative LISA logo (RC7D.38).
            CompactEyeTrackingHeader(
                state = eyeTrackingStatus,
                uiStrings = uiStrings,
                showSensitivityControls = true,
                compact = true,
                onDecreaseSensitivity = onDecreaseSensitivity,
                onIncreaseSensitivity = onIncreaseSensitivity,
                onDecreaseResponseTime = onDecreaseResponseTime,
                onIncreaseResponseTime = onIncreaseResponseTime,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(style.StatusToContentGap))

            // MIDDLE — teaching content expands into available card height (RC7D.39).
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(style.CardCornerRadius))
                    .background(LisaWhite)
                    .padding(style.CardContentPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = uiStrings.welcomeToLisa,
                    style = style.WelcomeTitleTextStyle,
                    color = LisaBlueDark,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = SilentWelcomeLaunchFlowMetadata.SUBTITLE,
                    style = style.WelcomeIntroTextStyle,
                    color = LisaBlueDark.copy(alpha = 0.78f),
                    maxLines = style.SubtitleMaxLines,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(style.CardVerticalSpacing))
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .fillMaxWidth()
                ) {
                    val constrained = maxHeight < style.ExplanationInternalScrollThreshold
                    val explanationModifier = if (constrained) {
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                    }
                    Column(
                        modifier = explanationModifier,
                        verticalArrangement = if (constrained) {
                            Arrangement.Top
                        } else {
                            Arrangement.SpaceEvenly
                        },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WelcomeBlinkNotationExplanation(
                            expandVertically = !constrained
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(style.ContentToActionGap))

            // BOTTOM — Continue always visible without outer scrolling.
            WelcomeIntroductionContinueAction(onContinue = onContinue)
        }
    }
}

@Composable
private fun WelcomeIntroductionContinueAction(onContinue: () -> Unit) {
    val style = WelcomeIntroductionLayoutStyle
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // RC7D.40 — single clickable control: Continue + L1 R1 inside the button.
        TrainingPrimaryButton(
            text = WelcomeEyeNavigationAuthority.continueButtonLabel(),
            onClick = onContinue,
            contentDescription = WelcomeEyeNavigationAuthority.continueContentDescription(),
            textStyle = style.WelcomeContinueTextStyle,
            secondaryText = WelcomeEyeNavigationAuthority.continueSequenceLabel(),
            secondaryTextStyle = style.WelcomeContinueSequenceTextStyle,
            minHeight = 64.dp
        )
        Text(
            text = WelcomeEyeNavigationAuthority.continueInstruction(),
            style = style.WelcomeContinueInstructionTextStyle,
            color = LisaBlueDark,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = style.ContinueInstructionTopPadding)
        )
    }
}

@Composable
private fun WelcomeDestinationSelectionScreen(
    uiStrings: LisaUiStrings,
    eyeTrackingStatus: EyeTrackingStatusUiState,
    selectedChoiceLabel: String?,
    onStartGuidedLearning: () -> Unit,
    onSkipToWorkspace: () -> Unit,
    onBackToIntroduction: () -> Unit,
    onSkipToNavigationTraining: () -> Unit,
    onDecreaseSensitivity: () -> Unit,
    onIncreaseSensitivity: () -> Unit,
    onDecreaseResponseTime: () -> Unit,
    onIncreaseResponseTime: () -> Unit
) {
    val style = WelcomeDestinationLayoutStyle
    val startSelected = selectedChoiceLabel?.contains("Guided", ignoreCase = true) == true ||
        selectedChoiceLabel?.contains("Start", ignoreCase = true) == true
    val skipSelected = selectedChoiceLabel?.contains("Skip", ignoreCase = true) == true ||
        selectedChoiceLabel?.contains("Communication", ignoreCase = true) == true

    TrainingSoftBackground {
        // RC7D.40 — destination fits the target viewport without outer scrolling.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = style.ScreenHorizontalPadding)
                .padding(top = style.ScreenTopPadding, bottom = style.ScreenBottomPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CompactEyeTrackingHeader(
                state = eyeTrackingStatus,
                uiStrings = uiStrings,
                showSensitivityControls = true,
                compact = true,
                onDecreaseSensitivity = onDecreaseSensitivity,
                onIncreaseSensitivity = onIncreaseSensitivity,
                onDecreaseResponseTime = onDecreaseResponseTime,
                onIncreaseResponseTime = onIncreaseResponseTime,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(style.StatusToCardSpacing))
            Column(
                modifier = Modifier
                    .weight(1f, fill = true)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(style.CardCornerRadius))
                    .background(LisaWhite)
                    .padding(style.CardPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = WelcomeEyeNavigationAuthority.destinationSelectionTitle(),
                    style = style.TitleTextStyle,
                    color = LisaBlueDark,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = WelcomeEyeNavigationAuthority.destinationSelectionSubtitle(),
                    style = style.SubtitleTextStyle,
                    color = LisaBlueDark.copy(alpha = 0.75f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = style.TitleToSubtitleSpacing,
                            bottom = style.SubtitleToActionSpacing
                        )
                )
                // Spread the three primary actions evenly through remaining card height.
                Column(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    WelcomeChoiceBlock(
                        title = uiStrings.startGuidedLearning,
                        instruction = WelcomeEyeNavigationAuthority.startGuidedLearningInstruction(),
                        sequenceLabel = WelcomeEyeNavigationAuthority.startGuidedLearningSequenceLabel(),
                        selected = startSelected,
                        primary = true,
                        onClick = onStartGuidedLearning
                    )
                    WelcomeChoiceBlock(
                        title = uiStrings.skipToCommunication,
                        instruction = WelcomeEyeNavigationAuthority.skipToCommunicationInstruction(),
                        sequenceLabel = WelcomeEyeNavigationAuthority.skipToCommunicationSequenceLabel(),
                        selected = skipSelected,
                        primary = false,
                        onClick = onSkipToWorkspace
                    )
                    WelcomeChoiceBlock(
                        title = uiStrings.back,
                        instruction = WelcomeEyeNavigationAuthority.backInstruction(),
                        sequenceLabel = WelcomeEyeNavigationAuthority.backSequenceLabel(),
                        selected = false,
                        primary = false,
                        onClick = onBackToIntroduction
                    )
                }
            }
            Spacer(modifier = Modifier.height(style.CaregiverSpacing))
            CaregiverAdvancedSkipLink(
                text = uiStrings.caregiverAdvancedSkipNavigation,
                onClick = onSkipToNavigationTraining
            )
            Spacer(modifier = Modifier.height(style.BottomPadding))
        }
    }
}

@Composable
private fun WelcomeBlinkNotationExplanation(
    expandVertically: Boolean = true
) {
    val style = WelcomeIntroductionLayoutStyle
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (expandVertically) Modifier.fillMaxHeight() else Modifier)
            .clip(RoundedCornerShape(12.dp))
            .background(LisaBlueLight.copy(alpha = 0.9f))
            .border(1.dp, LisaBlue.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
            .padding(
                horizontal = style.ExplanationCardPaddingHorizontal,
                vertical = style.ExplanationCardPaddingVertical
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (expandVertically) {
            Arrangement.SpaceEvenly
        } else {
            Arrangement.spacedBy(style.ExplanationLineSpacing)
        }
    ) {
        Text(
            text = WelcomeEyeNavigationAuthority.howToChooseTitle(),
            style = style.WelcomeExplanationTitleTextStyle,
            color = LisaBlueDark,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = WelcomeEyeNavigationAuthority.notationExplanationBody(),
            style = style.WelcomeExplanationBodyTextStyle,
            color = LisaBlueDark.copy(alpha = 0.9f),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = WelcomeEyeNavigationAuthority.notationCompleteLeftExample(),
            style = style.WelcomeSequenceExampleTextStyle,
            color = LisaBlueDark,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = WelcomeEyeNavigationAuthority.notationCompleteRightExample(),
            style = style.WelcomeSequenceExampleTextStyle,
            color = LisaBlueDark,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun WelcomeChoiceBlock(
    title: String,
    instruction: String,
    sequenceLabel: String,
    selected: Boolean,
    primary: Boolean,
    onClick: () -> Unit
) {
    val style = WelcomeDestinationLayoutStyle
    val hint = WelcomeEyeNavigationAuthority.combinedActionHint(instruction, sequenceLabel)
    val a11y = "$title, $instruction, $sequenceLabel"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LisaWorkspaceVisualStyle.CardCornerRadius))
            .then(
                if (selected) {
                    Modifier
                        .background(LisaWorkspaceVisualStyle.NavActionSelectedBackground)
                        .border(
                            2.dp,
                            LisaWorkspaceVisualStyle.NavActionSelectedBorder,
                            RoundedCornerShape(LisaWorkspaceVisualStyle.CardCornerRadius)
                        )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (primary) {
            TrainingPrimaryButton(
                text = title,
                onClick = onClick,
                contentDescription = a11y,
                textStyle = style.PrimaryButtonTextStyle,
                minHeight = style.PrimaryButtonHeight
            )
        } else {
            TrainingSecondaryButton(
                text = title,
                onClick = onClick,
                contentDescription = a11y,
                textStyle = style.SecondaryButtonTextStyle,
                minHeight = style.SecondaryButtonHeight
            )
        }
        // RC7D.40 — instruction + sequence on one compact line (e.g. "Blink left twice · L2 R0").
        Text(
            text = hint,
            style = style.InstructionLineTextStyle,
            color = LisaBlueDark,
            maxLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = style.ButtonToInstructionSpacing)
                .semantics { contentDescription = hint }
        )
    }
}

/**
 * Caregiver-only affordance — deliberately small and secondary so the primary user's
 * two-choice screen stays exact and simple. Jumps straight to Lesson 16 of 23 (navigation
 * training) inside the real Communication screen in GUIDED_TRAINING mode.
 */
@Composable
private fun CaregiverAdvancedSkipLink(text: String, onClick: () -> Unit) {
    val style = WelcomeDestinationLayoutStyle
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = text }
    ) {
        Text(
            text = text,
            style = style.CaregiverTextStyle,
            color = LisaGray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TrainingSkipConfirmScreen(
    narrationSpeaking: Boolean,
    choiceLabel: String?,
    awaitingConfirm: Boolean,
    onNarrationStarted: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (LisaSpeechPolicy.allowsNarration()) onNarrationStarted()
    }

    TrainingSoftBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TrainingLisaLogo()
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Skip Guided Learning?",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = LisaBlueDark,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Guided Learning is strongly recommended for first-time users.",
                fontSize = 17.sp,
                color = LisaGray,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center
            )
            if (awaitingConfirm) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "L1 R1 to continue · R1 L1 to choose again",
                    fontSize = 15.sp,
                    color = LisaBlueDark.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun TrainingWelcomeScreen(
    narrationSpeaking: Boolean,
    isReturningUser: Boolean,
    spokenHint: String? = null,
    onNarrationStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fadeAlpha by animateFloatAsState(targetValue = 1f, label = "welcomeFade")
    LaunchedEffect(Unit) {
        if (LisaSpeechPolicy.allowsNarration()) onNarrationStarted()
    }

    TrainingSoftBackground {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(28.dp)
                .alpha(fadeAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TrainingLisaLogo(modifier = Modifier.size(112.dp))
            Spacer(modifier = Modifier.height(36.dp))
            Text(
                text = if (isReturningUser) "Welcome back" else "Meet Lisa",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = LisaBlueDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (narrationSpeaking) {
                    "Lisa is speaking…"
                } else {
                    spokenHint ?: "I'll guide you step by step."
                },
                fontSize = 18.sp,
                color = if (narrationSpeaking) LisaGray else LisaBlueDark.copy(alpha = 0.9f),
                lineHeight = 26.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TrainingCompletionScreen(
    uiStrings: LisaUiStrings,
    showCelebration: Boolean,
    onNarrationStarted: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (LisaSpeechPolicy.allowsNarration()) onNarrationStarted()
    }

    TrainingSoftBackground {
        TrainingCelebrationOverlay(visible = showCelebration)
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TrainingLisaLogo(modifier = Modifier.size(112.dp))
            Spacer(modifier = Modifier.height(36.dp))
            Text(
                text = "You're ready to communicate",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = LisaBlueDark,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "You now know the basics.",
                fontSize = 20.sp,
                color = LisaBlueDark.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiStrings.goToCommunication,
                fontSize = 18.sp,
                color = LisaGray,
                lineHeight = 26.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Lisa is speaking…",
                fontSize = 14.sp,
                color = LisaGray,
                textAlign = TextAlign.Center
            )
        }
    }
}
