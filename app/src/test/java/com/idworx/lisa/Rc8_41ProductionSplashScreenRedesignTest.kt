package com.idworx.lisa

import com.idworx.lisa.features.brandedsplash.LisaBrandedSplashAuthority
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * RC8.41 — two-stage splash: brief native SplashScreen + branded Compose surface.
 */
class Rc8_41ProductionSplashScreenRedesignTest {

    private fun readMain(path: String): String =
        ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/$path")
            ?: error("Missing $path")

    private fun readRes(path: String): String {
        val candidates = listOf(File(path), File("app/$path"), File("../$path"))
        return candidates.firstOrNull { it.isFile }?.readText()
            ?: error("Missing resource $path")
    }

    @Test
    fun rootCauseIsSystemIconSlotNotFullBleedBranding() {
        val v31 = readRes("src/main/res/values-v31/themes.xml")
        assertTrue(v31.contains("windowSplashScreenAnimatedIcon"))
        // RC8.42 — system slot is neutral, not splash_icon / splash_logo.
        assertTrue(v31.contains("@drawable/splash_system_neutral_icon"))
        assertFalse(v31.contains("@drawable/splash_icon"))
        assertFalse(v31.contains("@drawable/splash_logo"))
        val splashLayer = readRes("src/main/res/drawable/splash_screen.xml")
        assertFalse(splashLayer.contains("@drawable/splash_logo"))
        assertTrue(splashLayer.contains("@color/splash_background"))
    }

    @Test
    fun splashThemeUsesAndroidXSplashScreenAndPostTheme() {
        val themes = readRes("src/main/res/values/themes.xml")
        assertTrue(themes.contains("parent=\"Theme.SplashScreen\""))
        assertTrue(themes.contains("postSplashScreenTheme\">@style/Theme.LISA"))
        assertTrue(themes.contains("windowSplashScreenBackground"))
        val gradle = File("app/build.gradle.kts").takeIf { it.isFile }?.readText()
            ?: File("build.gradle.kts").readText()
        assertTrue(gradle.contains("androidx.core:core-splashscreen"))
    }

    @Test
    fun mainActivityInstallsSplashScreenAndShowsBrandedCompose() {
        val main = readMain("MainActivity.kt")
        assertTrue(main.contains("installSplashScreen()"))
        assertTrue(main.contains("enableEdgeToEdge()"))
        assertTrue(main.contains("LisaBrandedSplashScreen"))
        assertTrue(main.contains("uiBrandedSplashVisible"))
        assertTrue(main.contains("scheduleBrandedSplashDismissCheck"))
        // RC8.42 — system splash is not artificially held.
        assertFalse(main.contains("setKeepOnScreenCondition"))
        assertFalse(main.contains("keepNativeSplashOnScreen"))
    }

    @Test
    fun brandedSplashMatchesApprovedCopyAndAssets() {
        val splash = readMain("features/brandedsplash/LisaBrandedSplashScreen.kt")
        assertTrue(splash.contains("R.drawable.splash_logo"))
        assertTrue(splash.contains("SoftSplashWaves"))
        assertTrue(splash.contains("Canvas"))
        assertFalse(splash.contains("ChatGPT_Image"))
        assertEquals("Communicator", LisaBrandedSplashAuthority.COMMUNICATOR)
        assertEquals("I can't speak.", LisaBrandedSplashAuthority.SLOGAN_LINE_1)
        assertEquals("LISA", LisaBrandedSplashAuthority.SLOGAN_LISA)
        assertTrue(LisaBrandedSplashAuthority.SLOGAN_LINE_2_REST.contains("speaks for me"))
    }

    @Test
    fun dismissAuthorityUsesMinHoldAndDoesNotBlockForever() {
        assertTrue(
            LisaBrandedSplashAuthority.shouldKeepShowing(
                elapsedMs = 0L,
                composeContentReady = false
            )
        )
        assertTrue(
            LisaBrandedSplashAuthority.shouldKeepShowing(
                elapsedMs = 500L,
                composeContentReady = true
            )
        )
        assertFalse(
            LisaBrandedSplashAuthority.shouldKeepShowing(
                elapsedMs = LisaBrandedSplashAuthority.MIN_VISIBLE_MS,
                composeContentReady = true
            )
        )
        assertFalse(
            LisaBrandedSplashAuthority.shouldKeepShowing(
                elapsedMs = LisaBrandedSplashAuthority.MAX_VISIBLE_MS,
                composeContentReady = false
            )
        )
    }

    @Test
    fun responsiveSizingUsesFractionsNotHardcodedPixels() {
        val splash = readMain("features/brandedsplash/LisaBrandedSplashScreen.kt")
        assertTrue(splash.contains("BoxWithConstraints"))
        assertTrue(splash.contains("logoWidthFraction"))
        assertTrue(splash.contains("waveHeightFraction"))
        assertFalse(splash.contains("Modifier = Modifier.size(72.dp)"))
        val auth = readMain("features/brandedsplash/LisaBrandedSplashAuthority.kt")
        assertTrue(auth.contains("shortestSideDp"))
    }

    @Test
    fun startupAuthorityUntouchedBySplashPresentation() {
        val authority = readMain("features/intelligentstartup/authority/StartupFlowAuthority.kt")
        assertFalse(authority.contains("BrandedSplash"))
        assertFalse(authority.contains("LisaBrandedSplash"))
        val splashAuth = readMain("features/brandedsplash/LisaBrandedSplashAuthority.kt")
        assertTrue(splashAuth.contains("presentation-only"))
        assertTrue(splashAuth.contains("Does not alter Intelligent Startup"))
    }

    @Test
    fun manifestStillUsesSplashThemeOnLauncher() {
        val manifest = readRes("src/main/AndroidManifest.xml")
        assertTrue(manifest.contains("android:theme=\"@style/Theme.LISA.Splash\""))
    }
}
