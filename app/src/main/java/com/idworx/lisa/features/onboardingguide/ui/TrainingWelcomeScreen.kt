package com.idworx.lisa.features.onboardingguide.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.ui.theme.LisaBlueDark
import com.idworx.lisa.ui.theme.LisaGray
import com.idworx.lisa.features.silentwelcome.LisaSpeechPolicy

/** First launch — exact simple Welcome to Lisa screen with two button choices only. */
@Composable
fun TrainingFirstLaunchChoiceScreen(
    onStartGuidedLearning: () -> Unit,
    onSkipToWorkspace: () -> Unit,
    onSkipToNavigationTraining: () -> Unit = {}
) {
    TrainingSoftBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 72.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TrainingLisaLogo(modifier = Modifier.size(100.dp), animated = false)
            Spacer(Modifier.height(32.dp))
            TrainingCard {
                Text(
                    text = "Welcome to Lisa",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = LisaBlueDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Lisa will guide the primary user through their first communication journey using only their eyes.",
                    fontSize = 15.sp,
                    color = LisaBlueDark.copy(alpha = 0.72f),
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                TrainingPrimaryButton(
                    text = "Start Guided Learning",
                    onClick = onStartGuidedLearning
                )
                TrainingSecondaryButton(
                    text = "Skip to Communication Workspace",
                    onClick = onSkipToWorkspace
                )
            }
            Spacer(Modifier.height(20.dp))
            CaregiverTestingSkipLink(onClick = onSkipToNavigationTraining)
        }
    }
}

/**
 * Caregiver/testing-only affordance — deliberately small and secondary so the primary user's
 * two-choice screen stays exact and simple. Jumps straight to Lesson 16 of 23 (navigation
 * training) inside the real Communication Workspace in GUIDED_TRAINING mode.
 */
@Composable
private fun CaregiverTestingSkipLink(onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) {
        Text(
            text = "Caregiver / testing: Skip to Navigation Training",
            fontSize = 12.sp,
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
            Spacer(Modifier.height(32.dp))
            Text(
                text = "Skip Guided Learning?",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = LisaBlueDark,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Guided Learning is strongly recommended for first-time users.",
                fontSize = 17.sp,
                color = LisaGray,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center
            )
            if (awaitingConfirm) {
                Spacer(Modifier.height(16.dp))
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
            Spacer(Modifier.height(36.dp))
            Text(
                text = if (isReturningUser) "Welcome back" else "Meet Lisa",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = LisaBlueDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
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
            Spacer(Modifier.height(36.dp))
            Text(
                text = "You're ready to communicate",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = LisaBlueDark,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "You now know the basics.",
                fontSize = 20.sp,
                color = LisaBlueDark.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Let's go to your Communication Workspace.",
                fontSize = 18.sp,
                color = LisaGray,
                lineHeight = 26.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Lisa is speaking…",
                fontSize = 14.sp,
                color = LisaGray,
                textAlign = TextAlign.Center
            )
        }
    }
}
