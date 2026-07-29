package com.idworx.lisa

import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import com.idworx.lisa.ui.theme.LisaSplashBackground
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * RC8.42 — neutralise the visible Android system splash; keep only branded Compose splash.
 */
class Rc8_42NeutralSystemSplashKeepBrandedOnlyTest {

    private fun readMain(path: String): String =
        ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/$path")
            ?: error("Missing $path")

    private fun readRes(path: String): String {
        val candidates = listOf(File(path), File("app/$path"), File("../$path"))
        return candidates.firstOrNull { it.isFile }?.readText()
            ?: error("Missing resource $path")
    }

    @Test
    fun systemSplashDoesNotUseSplashIconOrSplashLogo() {
        val values = readRes("src/main/res/values/themes.xml")
        val v31 = readRes("src/main/res/values-v31/themes.xml")
        listOf(values, v31).forEach { themes ->
            assertTrue(themes.contains("@drawable/splash_system_neutral_icon"))
            assertFalse(themes.contains("@drawable/splash_icon"))
            assertFalse(themes.contains("@drawable/splash_logo"))
            assertFalse(themes.contains("@mipmap/ic_launcher"))
        }
        val neutral = readRes("src/main/res/drawable/splash_system_neutral_icon.xml")
        assertTrue(neutral.contains("@android:color/transparent"))
    }

    @Test
    fun legacySplashScreenHasNoCentredBitmap() {
        val splash = readRes("src/main/res/drawable/splash_screen.xml")
        assertTrue(splash.contains("@color/splash_background"))
        assertFalse(splash.contains("<bitmap"))
        assertFalse(splash.contains("@drawable/splash_logo"))
        assertFalse(splash.contains("@drawable/splash_icon"))
        assertFalse(splash.contains("android:gravity=\"center\""))
    }

    @Test
    fun sharedAuthoritativeSplashBackgroundColour() {
        val colors = readRes("src/main/res/values/colors.xml")
        assertTrue(colors.contains("name=\"splash_background\">#FFFFFFFF"))
        assertEquals(1f, LisaSplashBackground.red, 0.001f)
        assertEquals(1f, LisaSplashBackground.green, 0.001f)
        assertEquals(1f, LisaSplashBackground.blue, 0.001f)
        assertEquals(1f, LisaSplashBackground.alpha, 0.001f)
        val splashUi = readMain("features/brandedsplash/LisaBrandedSplashScreen.kt")
        assertTrue(splashUi.contains("LisaSplashBackground"))
        assertTrue(splashUi.contains(".background(LisaSplashBackground)"))
        val values = readRes("src/main/res/values/themes.xml")
        assertTrue(values.contains("windowSplashScreenBackground\">@color/splash_background"))
        assertTrue(values.contains("android:windowBackground\">@color/splash_background"))
        assertTrue(values.contains("android:statusBarColor\">@color/splash_background"))
        assertTrue(values.contains("android:navigationBarColor\">@color/splash_background"))
    }

    @Test
    fun noKeepOnScreenConditionAndInstantExitAnimation() {
        val main = readMain("MainActivity.kt")
        assertFalse(main.contains("setKeepOnScreenCondition"))
        assertFalse(main.contains("keepNativeSplashOnScreen"))
        assertTrue(main.contains("setOnExitAnimationListener"))
        assertTrue(main.contains("provider.remove()"))
        val v31 = readRes("src/main/res/values-v31/themes.xml")
        assertTrue(v31.contains("windowSplashScreenAnimationDuration\">0"))
    }

    @Test
    fun brandedSplashIsExclusiveFirstComposeSurface() {
        val main = readMain("MainActivity.kt")
        val setContent = main.substringAfter("setContent {").substringBefore("override fun onPause")
        assertTrue(setContent.contains("if (uiBrandedSplashVisible.value)"))
        assertTrue(setContent.contains("LisaBrandedSplashScreen"))
        // Branded branch comes before LisaRootUI in the exclusive if/else.
        assertTrue(
            setContent.indexOf("LisaBrandedSplashScreen") <
                setContent.indexOf("LisaRootUI(")
        )
        assertTrue(setContent.contains("} else {"))
        assertFalse(setContent.contains("Box(modifier = Modifier.fillMaxSize())"))
    }

    @Test
    fun brandedComposeSplashAndTimingRemain() {
        val splash = readMain("features/brandedsplash/LisaBrandedSplashScreen.kt")
        assertTrue(splash.contains("R.drawable.splash_logo"))
        assertTrue(splash.contains("SoftSplashWaves"))
        assertTrue(splash.contains("Communicator") || splash.contains("COMMUNICATOR"))
        val auth = readMain("features/brandedsplash/LisaBrandedSplashAuthority.kt")
        assertTrue(auth.contains("MIN_VISIBLE_MS: Long = 1_400L"))
        assertTrue(auth.contains("MAX_VISIBLE_MS: Long = 2_800L"))
    }

    @Test
    fun startupReducersUnchangedAndSingleActivity() {
        val authority = readMain("features/intelligentstartup/authority/StartupFlowAuthority.kt")
        assertFalse(authority.contains("BrandedSplash"))
        val manifest = readRes("src/main/AndroidManifest.xml")
        assertEquals(1, Regex("""android:name="\.MainActivity"""").findAll(manifest).count())
        assertFalse(manifest.contains("SplashActivity"))
    }
}
