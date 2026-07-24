package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC8.1 — Communication workspace default landing is Category Selection ("Choose a Category"),
 * not an automatically opened General Conversation phrase page.
 */
class Rc8_1CommunicationWorkspaceCategorySelectionLandingTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)
    private val catalogContext = GuidedCatalogContext(
        responseTimeSec = SequenceProcessingDelay.DEFAULT_SECONDS,
        sensitivityLevel = DEFAULT_SENSITIVITY_LEVEL
    )

    private fun process(
        left: Int,
        right: Int,
        state: GuidedNavigationState
    ): GuidedSequenceResult =
        GuidedNavigationController.processSequence(
            left = left,
            right = right,
            state = state,
            language = PreferredLanguage.English,
            uiStrings = english,
            catalogContext = catalogContext
        )

    @Test
    fun defaultGuidedStateLandsOnCategorySelectionNotVocabulary() {
        val entry = GuidedNavigationState()
        assertEquals(GuidedOverlayScreenMode.CategoryMenu, entry.screenMode)
        assertEquals(0, entry.categoryMenuSelection)
        assertEquals(GuidedPreferencesAdjustMode.None, entry.preferencesAdjustMode)
    }

    @Test
    fun communicationWorkspaceRootOpensCategorySelectionWithoutOpeningCategory() {
        val fromVocabulary = GuidedNavigationState(
            screenMode = GuidedOverlayScreenMode.Vocabulary,
            categoryIndex = 0,
            phrasePageIndex = 2
        )
        val root = GuidedNavigationController.communicationWorkspaceRoot(fromVocabulary)
        assertEquals(GuidedOverlayScreenMode.CategoryMenu, root.screenMode)
        assertEquals(0, root.phrasePageIndex)
        assertEquals(GuidedPreferencesAdjustMode.None, root.preferencesAdjustMode)
        // Focus may highlight the prior category row without opening its phrase page.
        assertEquals(fromVocabulary.categoryIndex, root.categoryMenuSelection)
    }

    @Test
    fun generalConversationIsNotOpenedAutomaticallyOnEntry() {
        val entry = GuidedNavigationController.communicationWorkspaceRoot()
        assertEquals(GuidedOverlayScreenMode.CategoryMenu, entry.screenMode)
        assertFalse(entry.screenMode == GuidedOverlayScreenMode.Vocabulary)
        assertEquals(
            "General Conversation",
            english.guidedCategoryTitle(GuidedVocabularyCategory.Conversation)
        )
    }

    @Test
    fun firstCategoryCanBeFocusedWithoutBeingOpened() {
        val focused = GuidedNavigationState(
            screenMode = GuidedOverlayScreenMode.CategoryMenu,
            categoryMenuSelection = 0,
            categoryIndex = 0
        )
        assertEquals(GuidedOverlayScreenMode.CategoryMenu, focused.screenMode)
        assertEquals(0, focused.categoryMenuSelection)
        // No Select yet — still on Category Selection.
        assertEquals(GuidedOverlayScreenMode.CategoryMenu, focused.screenMode)
    }

    @Test
    fun selectingGeneralConversationExplicitlyOpensIt() {
        val menu = GuidedNavigationState(
            screenMode = GuidedOverlayScreenMode.CategoryMenu,
            categoryMenuSelection = 0
        )
        val result = process(
            GuidedModeNavigation.SELECT_LEFT,
            GuidedModeNavigation.SELECT_RIGHT,
            menu
        )
        assertTrue(result is GuidedSequenceResult.Navigate)
        val opened = (result as GuidedSequenceResult.Navigate).newState
        assertEquals(GuidedOverlayScreenMode.Vocabulary, opened.screenMode)
        assertEquals(0, opened.categoryIndex)
        assertEquals(
            GuidedVocabularyCategory.Conversation,
            GuidedVocabularyCategory.ordered[opened.categoryIndex]
        )
    }

    @Test
    fun backFromOpenedCategoryReturnsToCategorySelection() {
        val vocabulary = GuidedNavigationState(
            screenMode = GuidedOverlayScreenMode.Vocabulary,
            categoryIndex = 0,
            phrasePageIndex = 0
        )
        val result = process(
            GuidedModeNavigation.BACK_LEFT,
            GuidedModeNavigation.BACK_RIGHT,
            vocabulary
        )
        assertTrue(result is GuidedSequenceResult.Navigate)
        assertEquals(
            GuidedOverlayScreenMode.CategoryMenu,
            (result as GuidedSequenceResult.Navigate).newState.screenMode
        )
    }

    @Test
    fun welcomeAndSkipEntryPathsWireCategorySelectionRoot() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        val complete = main.substringAfter("private fun completeOnboarding()")
            .substringBefore("private fun maybePlayWorkspaceEntryIntro()")
        assertTrue(complete.contains("communicationWorkspaceRoot("))

        val cold = main.substringAfter("private fun applyColdLaunchSessionState()")
            .substringBefore("private fun completeOnboarding()")
        assertTrue(cold.contains("communicationWorkspaceRoot("))
    }

    @Test
    fun mainMenuCloseLandsOnCategorySelection() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        val closeFn = main.substringAfter("private fun closeAllPanels()")
            .substringBefore("private fun openMainMenu()")
        assertTrue(closeFn.contains("communicationWorkspaceRoot("))
        assertFalse(closeFn.contains("uiGuidedNavigationState.value = GuidedNavigationState()"))
    }

    @Test
    fun chooseACategoryLabelRemainsTheMenuTitle() {
        assertEquals("Choose a Category", english.guidedCategoryMenuTitle)
    }

    @Test
    fun emergencyAndExistingNavigationAuthoritiesRemainGreen() {
        assertTrue(
            com.idworx.lisa.validation.authority.GuidedNavigationAuthorityV1.validate()
                .outcome == com.idworx.lisa.validation.ValidationOutcome.PASS
        )
        assertTrue(
            com.idworx.lisa.validation.authority.NavigationReachabilityAuthorityV1.validate()
                .outcome == com.idworx.lisa.validation.ValidationOutcome.PASS
        )
    }

    private fun readSource(relativePath: String): String {
        val normalized = relativePath.replace('/', java.io.File.separatorChar)
        val roots = listOfNotNull(
            java.io.File(System.getProperty("user.dir")),
            java.io.File(System.getProperty("user.dir")).parentFile
        )
        for (root in roots) {
            val direct = root.resolve(normalized)
            if (direct.isFile) return direct.readText()
            if (normalized.startsWith("app${java.io.File.separatorChar}")) {
                val withoutApp = root.resolve(normalized.removePrefix("app${java.io.File.separatorChar}"))
                if (withoutApp.isFile) return withoutApp.readText()
            }
        }
        error("Missing source: $relativePath")
    }
}
