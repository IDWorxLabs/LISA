package com.idworx.lisa.features.onboardingguide

import com.idworx.lisa.LisaUiStrings
import com.idworx.lisa.PreferredLanguage
import com.idworx.lisa.features.engineeringtools.EngineeringToolsHubAccess
import com.idworx.lisa.features.glassessetup.WelcomeLaunchDestinationAuthority
import com.idworx.lisa.features.intelligentstartup.authority.WelcomeEyeNavigationAuthority
import com.idworx.lisa.features.onboardingguide.ui.WelcomeDestinationLayoutAuthority
import com.idworx.lisa.features.onboardingguide.ui.WelcomeDestinationLayoutStyle
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Launch layout: Choose where to begin is production-only (debug APK included). */
class WelcomeDestinationProductionLayoutTest {

    private fun welcomeSource(): String =
        ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingWelcomeScreen.kt"
        ) ?: error("missing TrainingWelcomeScreen")

    private fun destinationBlock(): String {
        val source = welcomeSource()
        val start = source.indexOf("fun WelcomeDestinationSelectionScreen")
        val end = source.indexOf("fun WelcomeBlinkNotationExplanation", start)
        return source.substring(start, end)
    }

    @Test
    fun chooseWhereToBegin_hasExactlyThreeProductionActions() {
        val titles = WelcomeLaunchDestinationAuthority.productionChoiceTitles(
            LisaUiStrings.forLanguage(PreferredLanguage.English)
        )
        assertEquals(3, titles.size)
        val block = destinationBlock()
        assertTrue(block.contains("startGuidedLearning"))
        assertTrue(block.contains("skipToCommunication"))
        assertTrue(block.contains("uiStrings.back"))
        assertEquals(3, Regex("WelcomeChoiceBlock\\(").findAll(block).count())
        assertTrue(WelcomeDestinationLayoutAuthority.caregiverAbsentFromDestinationScreen(welcomeSource()))
        assertFalse(block.contains("CaregiverAdvancedSkipLink"))
        assertFalse(block.contains("onSkipToNavigationTraining"))
        assertFalse(block.contains("For caregivers"))
    }

    @Test
    fun sequencesRemainUnchanged() {
        assertEquals("L2 R0", WelcomeEyeNavigationAuthority.startGuidedLearningSequenceLabel())
        assertEquals("L0 R2", WelcomeEyeNavigationAuthority.skipToCommunicationSequenceLabel())
        assertEquals("L2 R2", WelcomeEyeNavigationAuthority.backSequenceLabel())
        assertEquals(
            "Blink left twice · L2 R0",
            WelcomeEyeNavigationAuthority.combinedActionHint("Blink left twice", "L2 R0")
        )
    }

    @Test
    fun noResearchEntriesOnDestinationInDebugOrRelease() {
        assertFalse(WelcomeLaunchDestinationAuthority.allowsDiagnosticWelcomeEntries(true))
        assertFalse(WelcomeLaunchDestinationAuthority.allowsDiagnosticWelcomeEntries(false))
        assertTrue(WelcomeDestinationLayoutAuthority.destinationOmitsResearchToolEntries(welcomeSource()))
        val block = destinationBlock()
        assertFalse(block.contains("Eye Test Mode"))
        assertFalse(block.contains("Signal Investigation"))
        assertFalse(block.contains("Glasses Characterisation"))
        assertFalse(block.contains("Eye Tracking Profile"))
    }

    @Test
    fun engineeringHubIsDebugOnlyAndSeparateFromWelcome() {
        assertTrue(EngineeringToolsHubAccess.isHubAllowed(true))
        assertFalse(EngineeringToolsHubAccess.isHubAllowed(false))
        assertFalse(destinationBlock().contains("EngineeringToolsHub"))
        val ui = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt"
        ) ?: error("missing LisaAccessibilityUi")
        assertTrue(ui.contains("EngineeringToolsHubScreen"))
        assertTrue(ui.contains("showEngineeringToolsHubEntry"))
    }

    @Test
    fun productionDestinationFitsWithoutOuterScroll() {
        assertTrue(WelcomeDestinationLayoutStyle.fitsTargetViewportWithoutOuterScroll())
        assertTrue(
            WelcomeDestinationLayoutAuthority.destinationSourceOmitsOuterVerticalScroll(welcomeSource())
        )
        assertFalse(destinationBlock().contains("CaregiverSpacing"))
    }
}
