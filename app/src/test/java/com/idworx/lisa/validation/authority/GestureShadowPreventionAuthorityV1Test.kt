package com.idworx.lisa.validation.authority

import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.PracticeModeCatalog
import com.idworx.lisa.defaultLanguageMappings
import com.idworx.lisa.features.gestureshadowprevention.audit.GestureShadowPreventionAuditor
import com.idworx.lisa.features.gestureshadowprevention.metadata.GestureShadowPreventionMetadata
import com.idworx.lisa.features.gestureshadowprevention.validation.GestureShadowPreventionAuthorityV1
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureShadowPreventionAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = GestureShadowPreventionAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(GestureShadowPreventionAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(GestureShadowPreventionAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_GESTURE_SHADOW_PREVENTION_V1_PASS",
            GestureShadowPreventionMetadata.PASS_TOKEN
        )
    }

    @Test
    fun navigationGestures_areUnchangedByTheFix() {
        assertEquals(3, GuidedModeNavigation.CATEGORIES_LEFT)
        assertEquals(0, GuidedModeNavigation.CATEGORIES_RIGHT)
        assertEquals(0, GuidedModeNavigation.FINISH_TRAINING_LEFT)
        assertEquals(3, GuidedModeNavigation.FINISH_TRAINING_RIGHT)
        assertTrue(GestureShadowPreventionAuditor.categoriesGestureStillUsesL3R0())
        assertTrue(GestureShadowPreventionAuditor.finishTrainingGestureStillUsesL0R3())
    }

    @Test
    fun goodMorningAndINeedWater_moveToConflictFreeGestures() {
        val goodMorning = defaultLanguageMappings().first { it.vocabularyId == "good_morning" }
        val water = defaultLanguageMappings().first { it.vocabularyId == "i_need_water" }

        assertNotEquals(
            GuidedModeNavigation.CATEGORIES_LEFT to GuidedModeNavigation.CATEGORIES_RIGHT,
            goodMorning.left to goodMorning.right
        )
        assertNotEquals(
            GuidedModeNavigation.FINISH_TRAINING_LEFT to GuidedModeNavigation.FINISH_TRAINING_RIGHT,
            water.left to water.right
        )
        assertTrue(GestureShadowPreventionAuditor.goodMorningNoLongerSharesCategoriesGesture())
        assertTrue(GestureShadowPreventionAuditor.iNeedWaterNoLongerSharesFinishTrainingGesture())
        assertTrue(GestureShadowPreventionAuditor.replacementGesturesAreConflictFree())
    }

    @Test
    fun practiceMode_teachesTheUpdatedINeedWaterGesture() {
        val item = PracticeModeCatalog.items.first { it.vocabularyId == "i_need_water" }
        val mapping = defaultLanguageMappings().first { it.vocabularyId == "i_need_water" }
        assertEquals(mapping.left, item.left)
        assertEquals(mapping.right, item.right)
        assertTrue(GestureShadowPreventionAuditor.practiceModeTeachesTheUpdatedINeedWaterGesture())
    }

    @Test
    fun everyVocabularyPhrase_isUniqueAndReachable() {
        assertTrue(GestureShadowPreventionAuditor.noDuplicatePhraseGestures())
        assertTrue(GestureShadowPreventionAuditor.everyVocabularyPhraseReachable())
        assertTrue(GestureShadowPreventionAuditor.noNavigationGestureShadowsAPhrase())
        assertTrue(GestureShadowPreventionAuditor.noPhraseShadowsNavigation())
        assertTrue(GestureShadowPreventionAuditor.noReservedGestureConflicts())
    }

    @Test
    fun gestureAllocationSystem_generalizedAgainstFutureShadowing() {
        assertTrue(GestureShadowPreventionAuditor.auditEngineGenericallyDetectsWorkspaceReservedConflicts())
        assertTrue(GestureShadowPreventionAuditor.engineWouldHaveCaughtTheOriginalDefect())
    }

    @Test
    fun repositoryAudit_reportsZeroConflictsAndZeroUnreachablePhrases() {
        val report = GestureShadowPreventionAuditor.buildConflictReport()
        assertEquals(0, report.remainingConflicts)
        assertEquals(0, report.unreachablePhrases)
        assertTrue(report.totalNavigationGestures > 0)
        assertTrue(report.totalVocabularyGestures > 0)
        assertTrue(report.totalReservedGestures > 0)
        assertTrue(report.totalSystemGestures > 0)
        assertTrue(GestureShadowPreventionAuditor.repositoryAuditReportsZeroConflicts())
        assertTrue(GestureShadowPreventionAuditor.repositoryAuditReportsZeroUnreachablePhrases())
    }

    @Test
    fun existingNeighbouringAuthorities_remainGreen() {
        assertTrue(GestureShadowPreventionAuditor.existingGuidedTrainingAndGestureAuthoritiesRemainGreen())
    }
}
