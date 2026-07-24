package com.idworx.lisa.validation.authority

import com.idworx.lisa.GuidedCategoryShortcuts
import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.GuidedNavigationGestureAudit
import com.idworx.lisa.GuidedNavigationGestureAudit.GestureBinding
import com.idworx.lisa.GuidedPageSequences
import com.idworx.lisa.GuidedVocabularyCatalogValidation
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureConflictAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = GestureConflictAuthorityV1.validate()

        assertEquals(ValidationOutcome.PASS, report.outcome)
        assertEquals(GestureConflictAuthorityV1.PASS_TOKEN, report.passToken)
        assertEquals(GestureConflictAuthorityV1.AUTHORITY_NAME, report.authorityName)
        assertTrue(report.failedChecks.isEmpty())
        assertTrue(report.checkResults.all { it.passed })
        assertEquals("Gesture Conflict", report.subsystem)

        println(report.formatReport())
        println(GestureConflictAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun structuredReport_containsAllAuditDomains() {
        val checkIds = GestureConflictAuthorityV1.validate().checkResults.map { it.checkId }.toSet()

        assertTrue(checkIds.any { it.startsWith("RESV_") })
        assertTrue(checkIds.any { it.startsWith("GLOB_") })
        assertTrue(checkIds.any { it.startsWith("VOCAB_") })
        assertTrue(checkIds.any { it.startsWith("CAT_") })
        assertTrue(checkIds.any { it.startsWith("ADJ_") })
        assertTrue(checkIds.any { it.startsWith("MODE_") })
        assertTrue(checkIds.any { it.startsWith("RESOL_") })
    }

    @Test
    fun validatesConstitutionalGlobalGestures() {
        val globals = GestureConflictAuthorityV1.constitutionalGlobalGestures
        assertEquals(6, globals.size)
        assertEquals("Emergency", globals[6 to 0])
        assertTrue(GestureConflictAuthorityV1.ReservedGestureRegistryAudit.constitutionalGlobalsRegistered().passed)
    }

    @Test
    fun detectsGlobalGestureUniquenessViolation() {
        val duplicateGlobals = listOf(
            GestureBinding(1, 1, "SelectSave"),
            GestureBinding(1, 1, "Duplicate")
        )
        assertFalse(GestureConflictAuthorityV1.GlobalGestureAudit.globalGesturesUnique(duplicateGlobals))
    }

    @Test
    fun detectsLocalVocabularyUniquenessViolationWithinPage() {
        val duplicates = listOf(2 to 1, 2 to 1, 1 to 2)
        assertFalse(GestureConflictAuthorityV1.LocalVocabularyGestureAudit.isUniqueWithinPage(duplicates))
        assertTrue(GestureConflictAuthorityV1.LocalVocabularyGestureAudit.isUniqueWithinPage(listOf(2 to 1, 1 to 2)))
    }

    @Test
    fun allowsLocalVocabularyReuseAcrossCategories() {
        assertTrue(GestureConflictAuthorityV1.LocalVocabularyGestureAudit.reuseAcrossCategoriesAllowed().passed)
        assertTrue(GuidedVocabularyCatalogValidation.sequencesRepeatAcrossPages())
    }

    @Test
    fun detectsReservedGesturesUsedAsVocabulary() {
        assertTrue(
            GestureConflictAuthorityV1.ReservedGestureRegistryAudit.vocabularyUsesReservedGesture(
                GuidedModeNavigation.SELECT_LEFT,
                GuidedModeNavigation.SELECT_RIGHT
            )
        )
        assertFalse(
            GestureConflictAuthorityV1.ReservedGestureRegistryAudit.vocabularyUsesReservedGesture(2, 1)
        )
    }

    @Test
    fun validatesCategoryShortcutModeOnlyBehavior() {
        assertTrue(GestureConflictAuthorityV1.CategoryShortcutGestureAudit.shortcutsOnlyInCategoryMenuMode().passed)
    }

    @Test
    fun validatesExpectedCategoryShortcuts() {
        val expected = GestureConflictAuthorityV1.expectedCategoryShortcuts
        assertEquals(7, expected.size)
        assertEquals("Medical", expected[2].first)
        assertEquals(3 to 1, expected[2].second)
        assertEquals("Settings & Controls", expected[6].first)
        assertEquals(
            GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_LEFT to
                GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_RIGHT,
            expected[6].second
        )
        assertEquals(GuidedCategoryShortcuts.allGestures(), expected.map { it.second })
        assertTrue(GestureConflictAuthorityV1.CategoryShortcutGestureAudit.expectedShortcutAssignments().passed)
    }

    @Test
    fun validatesAdjustmentGestureModeOnlyBehavior() {
        assertTrue(GestureConflictAuthorityV1.AdjustmentGestureAudit.adjustmentOnlyInAdjustmentModes().passed)
        assertTrue(GestureConflictAuthorityV1.AdjustmentGestureAudit.scrollRemainsScrollInAdjustment().passed)
    }

    @Test
    fun detectsModeSpecificGestureConflict() {
        val conflicting = listOf(
            GestureBinding(2, 1, "Phrase:Hello"),
            GestureBinding(2, 1, "CategoryShortcut:0")
        )
        assertTrue(GestureConflictAuthorityV1.ModeConflictAudit.hasDuplicateMeaningInMode(conflicting))
    }

    @Test
    fun validatesEmergencyPrecedence() {
        assertTrue(GestureConflictAuthorityV1.ResolutionOrderAudit.emergencyCannotBeShadowed().passed)
        assertTrue(GestureConflictAuthorityV1.GlobalGestureAudit.emergencyHighestPrecedence().passed)
    }

    @Test
    fun validatesGlobalGesturePrecedence() {
        assertTrue(GestureConflictAuthorityV1.GlobalGestureAudit.globalPrecedenceOverLocal().passed)
        assertTrue(GestureConflictAuthorityV1.ResolutionOrderAudit.globalGesturesCannotBeShadowed(
            com.idworx.lisa.LisaUiStrings.forLanguage(com.idworx.lisa.PreferredLanguage.English),
            com.idworx.lisa.GuidedCatalogContext(
                responseTimeSec = com.idworx.lisa.SequenceProcessingDelay.DEFAULT_SECONDS,
                sensitivityLevel = 5
            )
        ).passed)
    }

    @Test
    fun validatesHiddenPageVocabularyCannotTrigger() {
        val uiStrings = com.idworx.lisa.LisaUiStrings.forLanguage(com.idworx.lisa.PreferredLanguage.English)
        val catalogContext = com.idworx.lisa.GuidedCatalogContext(
            responseTimeSec = com.idworx.lisa.SequenceProcessingDelay.DEFAULT_SECONDS,
            sensitivityLevel = 5
        )
        assertTrue(
            GestureConflictAuthorityV1.LocalVocabularyGestureAudit.hiddenPageCannotTrigger(
                uiStrings,
                catalogContext
            ).passed
        )
    }

    @Test
    fun prohibitsSingleEyeAccidentalVocabularyTriggers() {
        assertTrue(GestureConflictAuthorityV1.LocalVocabularyGestureAudit.singleEyeTriggersProhibited().passed)
        assertTrue(GuidedPageSequences.forbiddenForVocabulary.contains(1 to 0))
        assertTrue(GuidedPageSequences.forbiddenForVocabulary.contains(0 to 1))
    }

    @Test
    fun failOutcome_doesNotEmitPassToken() {
        val duplicateBindings = listOf(
            GestureBinding(4, 4, "Categories"),
            GestureBinding(4, 4, "DuplicateCategories")
        )
        assertFalse(GestureConflictAuthorityV1.GlobalGestureAudit.globalGesturesUnique(duplicateBindings))

        val report = GestureConflictAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            assertNull(report.passToken)
        }
    }

    @Test
    fun evidenceReport_includesRemediationOnFailure() {
        val failed = GestureConflictAuthorityV1.GlobalGestureAudit.oneCommandPerGlobalGesture()
        assertTrue(failed.passed)

        val syntheticFail = GestureConflictAuthorityV1.ModeConflictAudit.oneMeaningPerActiveMode(
            listOf(
                GuidedNavigationAuthorityV1.buildModeContexts().first().copy(
                    gestureBindings = listOf(
                        GestureBinding(1, 1, "A"),
                        GestureBinding(1, 1, "B")
                    )
                )
            )
        )
        assertFalse(syntheticFail.passed)
        assertNotNull(syntheticFail.remediation)
    }
}
