package com.idworx.lisa.features.brandedsplash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.R
import com.idworx.lisa.ui.theme.LisaBrandAccent
import com.idworx.lisa.ui.theme.LisaNavy
import com.idworx.lisa.ui.theme.LisaSplashBackground
import com.idworx.lisa.ui.theme.LisaWaveDeep
import com.idworx.lisa.ui.theme.LisaWaveMid
import com.idworx.lisa.ui.theme.LisaWaveSoft

/**
 * RC8.41 — Stage 2 branded splash matching the approved production layout.
 *
 * Uses existing [R.drawable.splash_logo] (eye + LISA wordmark) plus Compose typography,
 * divider, and vector-drawn soft waves. Not a flat bitmap of the reference image.
 */
@Composable
fun LisaBrandedSplashScreen(
    onFirstFrame: () -> Unit = {}
) {
    LaunchedEffect(Unit) { onFirstFrame() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(LisaSplashBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val layoutMaxWidth = maxWidth
        val layoutMaxHeight = maxHeight
        val shortestDp = minOf(layoutMaxWidth, layoutMaxHeight).value
        val logoFraction = LisaBrandedSplashAuthority.logoWidthFraction(shortestDp)
        val waveFraction = LisaBrandedSplashAuthority.waveHeightFraction(shortestDp)
        val logoWidth = layoutMaxWidth * logoFraction
        val density = LocalDensity.current
        val titleSp = with(density) { (logoWidth * 0.22f).toSp() }.value.coerceIn(36f, 72f)
        val subtitleSp = (titleSp * 0.38f).coerceIn(16f, 28f)
        val sloganSp = (titleSp * 0.32f).coerceIn(14f, 22f)

        SoftSplashWaves(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(layoutMaxHeight * waveFraction)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = layoutMaxWidth * 0.08f)
                .padding(
                    top = layoutMaxHeight * 0.10f,
                    bottom = layoutMaxHeight * waveFraction * 0.55f
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(layoutMaxHeight * 0.04f))
            Image(
                painter = painterResource(id = R.drawable.splash_logo),
                contentDescription = "LISA",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(logoWidth)
                    .height(logoWidth * 1.05f)
            )
            Spacer(modifier = Modifier.height(layoutMaxHeight * 0.018f))
            Text(
                text = LisaBrandedSplashAuthority.COMMUNICATOR,
                color = LisaBrandAccent,
                fontSize = subtitleSp.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.4.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(layoutMaxHeight * 0.022f))
            Box(
                modifier = Modifier
                    .width(layoutMaxWidth * 0.16f)
                    .height(1.5.dp)
                    .background(LisaBrandAccent)
            )
            Spacer(modifier = Modifier.height(layoutMaxHeight * 0.028f))
            Text(
                text = LisaBrandedSplashAuthority.SLOGAN_LINE_1,
                color = LisaNavy,
                fontSize = sloganSp.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                lineHeight = (sloganSp * 1.35f).sp
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = LisaBrandAccent, fontWeight = FontWeight.SemiBold)) {
                        append(LisaBrandedSplashAuthority.SLOGAN_LISA)
                    }
                    withStyle(SpanStyle(color = LisaNavy, fontWeight = FontWeight.Normal)) {
                        append(LisaBrandedSplashAuthority.SLOGAN_LINE_2_REST)
                    }
                },
                fontSize = sloganSp.sp,
                textAlign = TextAlign.Center,
                lineHeight = (sloganSp * 1.35f).sp
            )
        }
    }
}

@Composable
private fun SoftSplashWaves(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        fun wavePath(
            yBase: Float,
            amp: Float,
            phase: Float
        ): Path = Path().apply {
            moveTo(0f, h)
            lineTo(0f, yBase)
            var x = 0f
            while (x <= w) {
                val y = yBase + kotlin.math.sin((x / w) * Math.PI * 2.0 + phase).toFloat() * amp
                lineTo(x, y)
                x += w / 48f
            }
            lineTo(w, h)
            close()
        }

        drawPath(
            path = wavePath(yBase = h * 0.22f, amp = h * 0.10f, phase = 0.2f),
            color = LisaWaveSoft.copy(alpha = 0.95f),
            style = Fill
        )
        drawPath(
            path = wavePath(yBase = h * 0.38f, amp = h * 0.12f, phase = 1.1f),
            color = LisaWaveMid.copy(alpha = 0.85f),
            style = Fill
        )
        drawPath(
            path = wavePath(yBase = h * 0.55f, amp = h * 0.11f, phase = 2.0f),
            color = LisaWaveDeep.copy(alpha = 0.70f),
            style = Fill
        )
        // Soft highlight crest
        drawCircle(
            color = Color.White.copy(alpha = 0.18f),
            radius = w * 0.18f,
            center = Offset(w * 0.72f, h * 0.42f)
        )
    }
}
