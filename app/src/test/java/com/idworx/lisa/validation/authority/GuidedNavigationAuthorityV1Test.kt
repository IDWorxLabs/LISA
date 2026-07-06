package com.idworx.lisa.validation.authority

import com.idworx.lisa.GuidedNavigationGestureAudit
import com.idworx.lisa.GuidedNavigationGestureAudit.GestureBinding
import com.idworx.lisa.GuidedNavigationPanelSpec
import com.idworx.lisa.GuidedNavPanelAction
import com.idworx.lisa.GuidedTouchNavigationSpec
import com.idworx.lisa.LisaUiStrings
import com.idworx.lisa.PreferredLanguage
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidedNavigationAuthorityV1Test {

    private val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = GuidedNavigationAuthorityV1.validate()

        assertEquals(ValidationOutcome.PASS, report.outcome)
        assertEquals(GuidedNavigationAuthorityV1.PASS_TOKEN, report.passToken)
        assertEquals(GuidedNavigationAuthorityV1.AUTHORITY_NAME, report.authorityName)
        assertTrue(report.failedChecks.isEmpty())
        assertTrue(report.checkResults.all { it.passed })
        assertNotNull(report.evidenceSummary)
        assertTrue(report.affectedLicArticles.isNotEmpty())
        assertTrue(report.affectedLiecArticles.isNotEmpty())
        assertTrue(report.affectedLvcArticles.isNotEmpty())

        println(report.formatReport())
        println(GuidedNavigationAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun structuredReport_containsAllAuditDomains() {
        val report = GuidedNavigationAuthorityV1.validate()
        val checkIds = report.checkResults.map { it.checkId }.toSet()

        assertTrue(checkIds.any { it.startsWith("NAV_CTX_") })
        assertTrue(checkIds.any { it.startsWith("MODE_TX_") })
        assertTrue(checkIds.any { it.startsWith("REACH_") })
        assertTrue(checkIds.any { it.startsWith("GEST_") })
        assertTrue(checkIds.any { it.startsWith("LABEL_") })
        assertTrue(checkIds.any { it.startsWith("TOUCH_") })
        assertTrue(checkIds.any { it.startsWith("RECOV_") })
    }

    @Test
    fun detectsMissingCategoriesReachability() {
        val bindingsWithoutCategories = GuidedNavigationGestureAudit.globalPanelBindings()
            .filterNot { it.action == "Categories" }

        assertFalse(GuidedNavigationAuthorityV1.ReachabilityAudit.categoriesReachableInBindings(bindingsWithoutCategories))

        val result = GuidedNavigationAuthorityV1.ReachabilityAudit.categoriesReachable(
            GuidedNavigationAuthorityV1.buildModeContexts().map { ctx ->
                ctx.copy(gestureBindings = bindingsWithoutCategories)
            }
        )
        assertFalse(result.passed)
        assertNotNull(result.remediation)
    }

    @Test
    fun detectsMissingBackCancelReachability() {
        val bindingsWithoutBack = GuidedNavigationGestureAudit.globalPanelBindings()
            .filterNot { it.action == "BackCancel" }

        assertFalse(GuidedNavigationAuthorityV1.ReachabilityAudit.backCancelReachableInBindings(bindingsWithoutBack))
    }

    @Test
    fun detectsMissingEmergencyReachability() {
        val bindingsWithoutEmergency = GuidedNavigationGestureAudit.globalPanelBindings()
            .filterNot { it.action == "Emergency" }

        assertFalse(GuidedNavigationAuthorityV1.ReachabilityAudit.emergencyReachableInBindings(bindingsWithoutEmergency))
    }

    @Test
    fun detectsDuplicateActiveGestureMeanings() {
        val duplicateBindings = listOf(
            GestureBinding(1, 1, "SelectSave"),
            GestureBinding(1, 1, "DuplicateSelect")
        )

        assertFalse(GuidedNavigationAuthorityV1.GestureMeaningAudit.noDuplicateGesturesInBindings(duplicateBindings))
        assertFalse(GuidedNavigationGestureAudit.noDuplicateGestures(duplicateBindings))
    }

    @Test
    fun detectsUnlabelledRightPanelActions() {
        val unlabelled = listOf(
            GuidedNavPanelAction("", "Back", "hint", "L2 R2")
        )

        assertFalse(GuidedNavigationAuthorityV1.LabellingAudit.panelActionsFullyLabeled(unlabelled))
        assertFalse(GuidedNavigationPanelSpec.allActionsLabeled(unlabelled))
    }

    @Test
    fun detectsMissingTouchParity() {
        assertTrue(GuidedNavigationAuthorityV1.HumanTouchParityAudit.touchMirrorsPanelGesture(1, 1))
        assertFalse(
            GuidedNavigationAuthorityV1.HumanTouchParityAudit.touchMirrorsPanelGesture(9, 9)
        )
        assertFalse(GuidedTouchNavigationSpec.touchMirrorsEyeGesture(9, 9))
    }

    @Test
    fun detectsDeadEndNavigationStates() {
        val contexts = GuidedNavigationAuthorityV1.buildModeContexts()
        val trappedContext = contexts.first { it.logicalMode == GuidedNavigationAuthorityV1.GuidedLogicalMode.Vocabulary }

        assertTrue(
            GuidedNavigationAuthorityV1.RecoveryAudit.hasRecoveryPath(
                trappedContext,
                uiStrings,
                com.idworx.lisa.GuidedCatalogContext(responseTimeSec = 3, sensitivityLevel = 5)
            )
        )

        val fakeTrapped = trappedContext.copy(
            gestureBindings = emptyList()
        )
        assertFalse(
            GuidedNavigationAuthorityV1.ReachabilityAudit.categoriesReachable(
                listOf(fakeTrapped)
            ).passed
        )
    }

    @Test
    fun failOutcome_doesNotEmitPassToken() {
        val duplicateBindings = listOf(
            GestureBinding(4, 4, "Categories"),
            GestureBinding(4, 4, "DuplicateCategories")
        )
        val modeContext = GuidedNavigationAuthorityV1.buildModeContexts().first().copy(
            gestureBindings = duplicateBindings
        )
        val gestureCheck = GuidedNavigationAuthorityV1.GestureMeaningAudit.oneMeaningPerGesturePerMode(
            listOf(modeContext)
        )

        assertFalse(gestureCheck.passed)

        val report = GuidedNavigationAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            assertNull(report.passToken)
        }
    }

    @Test
    fun validatesAllFiveGuidedModes() {
        val contexts = GuidedNavigationAuthorityV1.buildModeContexts()
        assertEquals(5, contexts.size)
        assertEquals(
            setOf(
                GuidedNavigationAuthorityV1.GuidedLogicalMode.Vocabulary,
                GuidedNavigationAuthorityV1.GuidedLogicalMode.Preferences,
                GuidedNavigationAuthorityV1.GuidedLogicalMode.CategoryMenu,
                GuidedNavigationAuthorityV1.GuidedLogicalMode.ResponseTimeAdjustment,
                GuidedNavigationAuthorityV1.GuidedLogicalMode.SensitivityAdjustment
            ),
            contexts.map { it.logicalMode }.toSet()
        )
    }

    @Test
    fun evidenceReport_includesRemediationOnFailure() {
        val failed = GuidedNavigationAuthorityV1.GestureMeaningAudit.oneMeaningPerGesturePerMode(
            listOf(
                GuidedNavigationAuthorityV1.buildModeContexts().first().copy(
                    gestureBindings = listOf(
                        GestureBinding(1, 1, "A"),
                        GestureBinding(1, 1, "B")
                    )
                )
            )
        )

        assertFalse(failed.passed)
        assertNotNull(failed.remediation)
    }
}
