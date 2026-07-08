package com.idworx.lisa.validation.authority

import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.GuidedNavigationState
import com.idworx.lisa.GuidedOverlayScreenMode
import com.idworx.lisa.GuidedPreferencesAdjustMode
import com.idworx.lisa.GuidedSequenceResult
import com.idworx.lisa.GuidedTouchNavigationSpec
import com.idworx.lisa.GuidedVocabularyCategory
import com.idworx.lisa.LisaUiStrings
import com.idworx.lisa.PreferredLanguage
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationReachabilityAuthorityV1Test {

    private val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)
    private val catalogContext = com.idworx.lisa.GuidedCatalogContext(
        responseTimeSec = com.idworx.lisa.SequenceProcessingDelay.DEFAULT_SECONDS,
        sensitivityLevel = 5
    )

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = NavigationReachabilityAuthorityV1.validate()

        assertEquals(ValidationOutcome.PASS, report.outcome)
        assertEquals(NavigationReachabilityAuthorityV1.PASS_TOKEN, report.passToken)
        assertEquals(NavigationReachabilityAuthorityV1.AUTHORITY_NAME, report.authorityName)
        assertTrue(report.failedChecks.isEmpty())
        assertTrue(report.checkResults.all { it.passed })
        assertEquals("Navigation Reachability", report.subsystem)

        println(report.formatReport())
        println(NavigationReachabilityAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun structuredReport_containsAllAuditDomains() {
        val checkIds = NavigationReachabilityAuthorityV1.validate().checkResults.map { it.checkId }.toSet()

        assertTrue(checkIds.any { it.startsWith("CAT_REACH_") })
        assertTrue(checkIds.any { it.startsWith("BACK_REACH_") })
        assertTrue(checkIds.any { it.startsWith("EMER_REACH_") })
        assertTrue(checkIds.any { it.startsWith("SCROLL_REACH_") })
        assertTrue(checkIds.any { it.startsWith("RECOV_ROUTE_") })
        assertTrue(checkIds.any { it.startsWith("DEAD_END_") })
        assertTrue(checkIds.any { it.startsWith("LOOP_") })
        assertTrue(checkIds.any { it.startsWith("STATE_REACH_") })
        assertTrue(checkIds.any { it.startsWith("HUMAN_REACH_") })
    }

    @Test
    fun categoriesReachableFromEveryGuidedState() {
        val states = NavigationReachabilityAuthorityV1.buildGuidedStates()
        assertTrue(
            NavigationReachabilityAuthorityV1.CategoriesReachabilityAudit
                .categoriesReachableFromEveryState(states, uiStrings, catalogContext)
                .passed
        )
    }

    @Test
    fun categoriesGesture_opensCategoryMenuFromVocabulary() {
        val vocabulary = NavigationReachabilityAuthorityV1.buildGuidedStates().first()
        val result = NavigationReachabilityAuthorityV1.processGesture(
            GuidedModeNavigation.CATEGORIES_LEFT, GuidedModeNavigation.CATEGORIES_RIGHT, vocabulary.state, uiStrings, catalogContext
        )
        assertTrue(result is GuidedSequenceResult.Navigate)
        assertEquals(
            GuidedOverlayScreenMode.CategoryMenu,
            (result as GuidedSequenceResult.Navigate).newState.screenMode
        )
    }

    @Test
    fun backReachableFromCategoryMenuAndAdjustments() {
        val states = NavigationReachabilityAuthorityV1.buildGuidedStates()
        val menu = states.first { it.label == "Category Menu Mode" }
        val back = NavigationReachabilityAuthorityV1.processGesture(
            2, 2, menu.state, uiStrings, catalogContext
        )
        assertTrue(back is GuidedSequenceResult.Navigate)
        assertEquals(
            GuidedOverlayScreenMode.Vocabulary,
            (back as GuidedSequenceResult.Navigate).newState.screenMode
        )
    }

    @Test
    fun vocabularyRecoveryViaCategoriesWhenBackInert() {
        val vocabulary = NavigationReachabilityAuthorityV1.buildGuidedStates().first()
        val back = NavigationReachabilityAuthorityV1.processGesture(2, 2, vocabulary.state, uiStrings, catalogContext)
        val categories = NavigationReachabilityAuthorityV1.processGesture(
            GuidedModeNavigation.CATEGORIES_LEFT, GuidedModeNavigation.CATEGORIES_RIGHT, vocabulary.state, uiStrings, catalogContext
        )
        assertEquals(GuidedSequenceResult.Unmatched, back)
        assertTrue(categories is GuidedSequenceResult.Navigate)
        assertTrue(
            NavigationReachabilityAuthorityV1.BackCancelReachabilityAudit
                .hasBackOrCancelRecovery(vocabulary, uiStrings, catalogContext)
        )
    }

    @Test
    fun emergencyNeverTriggersSpeakInGuidedController() {
        val states = NavigationReachabilityAuthorityV1.buildGuidedStates()
        assertTrue(
            NavigationReachabilityAuthorityV1.EmergencyReachabilityAudit
                .emergencyNeverVocabularySpeak(states, uiStrings, catalogContext)
                .passed
        )
    }

    @Test
    fun scrollReachableInAllGuidedStates() {
        val states = NavigationReachabilityAuthorityV1.buildGuidedStates()
        assertTrue(
            NavigationReachabilityAuthorityV1.ScrollReachabilityAudit
                .scrollUpAvailableEveryState(states, uiStrings, catalogContext)
                .passed
        )
        assertTrue(
            NavigationReachabilityAuthorityV1.ScrollReachabilityAudit
                .scrollDownAvailableEveryState(states, uiStrings, catalogContext)
                .passed
        )
    }

    @Test
    fun detectsMissingCategoriesReachability() {
        val bindingsWithoutCategories = com.idworx.lisa.GuidedNavigationGestureAudit
            .globalPanelBindings()
            .filterNot { it.action == "Categories" }
        assertFalse(bindingsWithoutCategories.any { it.action == "Categories" })

        val misconfiguredMenu = NavigationReachabilityAuthorityV1.GuidedReachabilityState(
            label = "Category Menu Mode",
            state = GuidedNavigationState(screenMode = GuidedOverlayScreenMode.Vocabulary)
        )
        val failed = NavigationReachabilityAuthorityV1.BackCancelReachabilityAudit
            .backDestinationCategoryMenu(
                listOf(misconfiguredMenu),
                uiStrings,
                catalogContext
            )
        assertFalse(failed.passed)
        assertNotNull(failed.remediation)
    }

    @Test
    fun allGuidedModesReachableFromEntry() {
        assertTrue(
            NavigationReachabilityAuthorityV1.StateReachabilityAudit
                .noOrphanedModes(uiStrings, catalogContext)
                .passed
        )
    }

    @Test
    fun navigationGesturesHaveTouchParity() {
        assertTrue(
            NavigationReachabilityAuthorityV1.HumanReachabilityAudit
                .navigationGesturesHaveTouchParity()
                .passed
        )
        assertTrue(
            GuidedTouchNavigationSpec.touchMirrorsEyeGesture(
                GuidedModeNavigation.CATEGORIES_LEFT,
                GuidedModeNavigation.CATEGORIES_RIGHT
            )
        )
    }

    @Test
    fun noDeadEndStatesInCanonicalGuidedStates() {
        val states = NavigationReachabilityAuthorityV1.buildGuidedStates()
        assertTrue(
            NavigationReachabilityAuthorityV1.DeadEndAudit
                .noRecoveryDeadEnds(states, uiStrings, catalogContext)
                .passed
        )
    }

    @Test
    fun failOutcome_doesNotEmitPassToken() {
        val report = NavigationReachabilityAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            assertNull(report.passToken)
        }
    }

    @Test
    fun evidenceReport_includesRemediationOnSyntheticFailure() {
        val misconfiguredMenu = NavigationReachabilityAuthorityV1.GuidedReachabilityState(
            label = "Category Menu Mode",
            state = GuidedNavigationState(screenMode = GuidedOverlayScreenMode.Vocabulary)
        )
        val failed = NavigationReachabilityAuthorityV1.BackCancelReachabilityAudit
            .backDestinationCategoryMenu(
                listOf(misconfiguredMenu),
                uiStrings,
                catalogContext
            )
        assertFalse(failed.passed)
        assertNotNull(failed.remediation)
    }
}
