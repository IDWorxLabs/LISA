package com.idworx.lisa

import com.idworx.lisa.features.brandedsplash.LisaBrandedSplashAuthority
import com.idworx.lisa.features.systembarinsets.LisaSystemBarInsetAuthority
import com.idworx.lisa.features.systembarinsets.LisaSystemBarInsetAuthority.SamsungViewportFixtures as Samsung
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * RC8.43 — restore global safe viewport after splash edge-to-edge; do not leak splash insets policy.
 */
class Rc8_43RestoreGlobalScreenFitSystemBarInsetsTest {

    private fun readMain(path: String): String =
        ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/$path")
            ?: error("Missing $path")

    private fun readRes(path: String): String {
        val candidates = listOf(File(path), File("app/$path"), File("../$path"))
        return candidates.firstOrNull { it.isFile }?.readText()
            ?: error("Missing resource $path")
    }

    private fun setContentBlock(): String {
        val main = readMain("MainActivity.kt")
        return main.substringAfter("setContent {").substringBefore("private fun scheduleBrandedSplash")
    }

    @Test
    fun rootCauseWasEdgeToEdgeWithoutLisaRootInsets() {
        val main = readMain("MainActivity.kt")
        assertTrue(main.contains("enableEdgeToEdge()"))
        assertTrue(main.contains("LisaSystemBarInsetAuthority"))
        assertTrue(main.contains("safeApplicationContent()"))
        // Splash branch must not be the only place that handles insets.
        val splash = readMain("features/brandedsplash/LisaBrandedSplashScreen.kt")
        assertTrue(splash.contains("statusBarsPadding()"))
        assertTrue(splash.contains("navigationBarsPadding()"))
    }

    @Test
    fun postSplashThemeIsNotFullscreen() {
        val themes = readRes("src/main/res/values/themes.xml")
        assertTrue(themes.contains("name=\"Theme.LISA\""))
        assertFalse(themes.contains("windowFullscreen\">true"))
        assertFalse(themes.contains("android:windowFullscreen\">true"))
        val themeLisa = themes.substringAfter("name=\"Theme.LISA\"")
            .substringBefore("name=\"Theme.LISA.Splash\"")
        assertFalse(themeLisa.contains("Fullscreen"))
        assertTrue(themes.contains("postSplashScreenTheme\">@style/Theme.LISA"))
        val v31 = readRes("src/main/res/values-v31/themes.xml")
        assertTrue(v31.contains("postSplashScreenTheme\">@style/Theme.LISA"))
    }

    @Test
    fun splashWindowPolicyDoesNotRemainActiveForLisaRootUi() {
        val setContent = setContentBlock()
        assertTrue(setContent.contains("if (uiBrandedSplashVisible.value)"))
        assertTrue(setContent.contains("LisaBrandedSplashScreen"))
        assertTrue(setContent.contains("LisaRootUI("))
        assertTrue(
            setContent.indexOf("LisaBrandedSplashScreen") <
                setContent.indexOf("LisaSystemBarInsetAuthority")
        )
        assertTrue(
            setContent.indexOf("LisaSystemBarInsetAuthority") <
                setContent.indexOf("LisaRootUI(")
        )
        // Inset authority wraps only the else / LisaRootUI branch.
        val elseBranch = setContent.substringAfter("} else {")
        assertTrue(elseBranch.contains("LisaSystemBarInsetAuthority"))
        assertTrue(elseBranch.contains("safeApplicationContent()"))
        val splashBranch = setContent.substringBefore("} else {")
        assertFalse(splashBranch.contains("LisaSystemBarInsetAuthority"))
        assertFalse(splashBranch.contains("safeApplicationContent()"))
    }

    @Test
    fun oneRootInsetAuthorityProtectsNormalApplicationContent() {
        val authority = readMain("features/systembarinsets/LisaSystemBarInsetAuthority.kt")
        assertTrue(authority.contains("systemBarsPadding()"))
        assertTrue(authority.contains("fun safeApplicationContent()"))
        val main = readMain("MainActivity.kt")
        val applications = Regex("safeApplicationContent\\(\\)").findAll(main).count()
        assertEquals(1, applications)
        val accessibility = readMain("LisaAccessibilityUi.kt")
        assertFalse(accessibility.contains("systemBarsPadding()"))
        assertFalse(accessibility.contains("statusBarsPadding()"))
        assertFalse(accessibility.contains("navigationBarsPadding()"))
    }

    @Test
    fun statusAndNavigationInsetsAreNotMissingOrDoubleApplied() {
        val emergency = readMain("LisaEmergencyUi.kt")
        assertFalse(emergency.contains("statusBarsPadding()"))
        assertFalse(emergency.contains("navigationBarsPadding()"))
        assertFalse(emergency.contains("systemBarsPadding()"))
        assertTrue(emergency.contains("RC8.43"))

        val welcome = readMain("features/onboardingguide/ui/TrainingWelcomeScreen.kt")
        assertFalse(welcome.contains("statusBarsPadding()"))
        assertFalse(welcome.contains("navigationBarsPadding()"))
        assertFalse(welcome.contains("systemBarsPadding()"))

        val setContent = setContentBlock()
        assertEquals(
            1,
            Regex("safeApplicationContent\\(\\)").findAll(setContent).count()
        )
        // Splash may pad itself; normal root uses systemBarsPadding via authority only.
        val authority = readMain("features/systembarinsets/LisaSystemBarInsetAuthority.kt")
        assertEquals(1, Regex("systemBarsPadding\\(\\)").findAll(authority).count())
    }

    @Test
    fun samsungThreeButtonNavigationWelcomeContinueFitsSafeViewport() {
        val safeHeight = LisaSystemBarInsetAuthority.safeContentHeightDp(
            physicalScreenHeightDp = Samsung.PHYSICAL_HEIGHT_DP,
            statusBarInsetDp = Samsung.STATUS_BAR_DP,
            navigationBarInsetDp = Samsung.THREE_BUTTON_NAV_DP
        )
        assertEquals(728, safeHeight)
        assertTrue(
            LisaSystemBarInsetAuthority.contentFitsSafeViewport(
                contentTopDp = Samsung.WELCOME_HEADER_TOP_DP,
                contentBottomDp = Samsung.WELCOME_CONTINUE_BOTTOM_DP,
                safeHeightDp = safeHeight
            )
        )
        // Without insets, Continue at 700 would still "fit" a full 800 physical height —
        // but the real failure mode is drawing into the nav band. Model the clipped band:
        val physicalOnly = Samsung.PHYSICAL_HEIGHT_DP
        val continueBottomIfIgnoringInsets = safeHeight + Samsung.THREE_BUTTON_NAV_DP - 20
        assertFalse(
            LisaSystemBarInsetAuthority.contentFitsSafeViewport(
                contentTopDp = 0,
                contentBottomDp = continueBottomIfIgnoringInsets,
                safeHeightDp = safeHeight
            )
        )
        assertTrue(continueBottomIfIgnoringInsets <= physicalOnly)
    }

    @Test
    fun samsungGestureNavigationCommunicationMenuFitsSafeViewport() {
        val safeHeight = LisaSystemBarInsetAuthority.safeContentHeightDp(
            physicalScreenHeightDp = Samsung.PHYSICAL_HEIGHT_DP,
            statusBarInsetDp = Samsung.STATUS_BAR_DP,
            navigationBarInsetDp = Samsung.GESTURE_NAV_DP
        )
        assertEquals(760, safeHeight)
        assertTrue(
            LisaSystemBarInsetAuthority.contentFitsSafeViewport(
                contentTopDp = Samsung.COMMUNICATION_HEADER_TOP_DP,
                contentBottomDp = Samsung.COMMUNICATION_MENU_BOTTOM_DP,
                safeHeightDp = safeHeight
            )
        )
    }

    @Test
    fun samsungThreeButtonNavigationCommunicationMenuFitsSafeViewport() {
        val safeHeight = LisaSystemBarInsetAuthority.safeContentHeightDp(
            physicalScreenHeightDp = Samsung.PHYSICAL_HEIGHT_DP,
            statusBarInsetDp = Samsung.STATUS_BAR_DP,
            navigationBarInsetDp = Samsung.THREE_BUTTON_NAV_DP
        )
        assertTrue(
            LisaSystemBarInsetAuthority.contentFitsSafeViewport(
                contentTopDp = Samsung.COMMUNICATION_HEADER_TOP_DP,
                contentBottomDp = Samsung.COMMUNICATION_MENU_BOTTOM_DP,
                safeHeightDp = safeHeight
            )
        )
    }

    @Test
    fun universalEyeTrackingHeaderRemainsInsideSafeViewportModel() {
        val safeHeight = LisaSystemBarInsetAuthority.safeContentHeightDp(
            physicalScreenHeightDp = Samsung.PHYSICAL_HEIGHT_DP,
            statusBarInsetDp = Samsung.STATUS_BAR_DP,
            navigationBarInsetDp = Samsung.THREE_BUTTON_NAV_DP
        )
        // Header occupies the top of the padded root (y=0), not under the status bar.
        assertTrue(
            LisaSystemBarInsetAuthority.contentFitsSafeViewport(
                contentTopDp = 0,
                contentBottomDp = 96,
                safeHeightDp = safeHeight
            )
        )
        val header = readMain("features/eyetrackingstatus/UniversalEyeTrackingHeader.kt")
        assertFalse(header.contains("statusBarsPadding()"))
        assertFalse(header.contains("systemBarsPadding()"))
    }

    @Test
    fun rc841AndRc842SplashContractsRemainIntact() {
        assertTrue(LisaBrandedSplashAuthority.MIN_VISIBLE_MS == 1_400L)
        assertTrue(LisaBrandedSplashAuthority.MAX_VISIBLE_MS == 2_800L)
        val main = readMain("MainActivity.kt")
        assertTrue(main.contains("installSplashScreen()"))
        assertFalse(main.contains("setKeepOnScreenCondition"))
        assertTrue(main.contains("setOnExitAnimationListener"))
        val values = readRes("src/main/res/values/themes.xml")
        assertTrue(values.contains("@drawable/splash_system_neutral_icon"))
        assertFalse(values.contains("@drawable/splash_icon"))
        val splash = readMain("features/brandedsplash/LisaBrandedSplashScreen.kt")
        assertTrue(splash.contains("R.drawable.splash_logo"))
        assertTrue(splash.contains("SoftSplashWaves"))
    }

    @Test
    fun startupReducersAndNavigationFlowsUnchanged() {
        val authority = readMain("features/intelligentstartup/authority/StartupFlowAuthority.kt")
        assertFalse(authority.contains("SystemBarInset"))
        assertFalse(authority.contains("BrandedSplash"))
        val welcomeNav = readMain("features/intelligentstartup/authority/WelcomeEyeNavigationAuthority.kt")
        assertFalse(welcomeNav.contains("systemBarsPadding"))
        val manifest = readRes("src/main/AndroidManifest.xml")
        assertEquals(1, Regex("""android:name="\.MainActivity"""").findAll(manifest).count())
    }
}
