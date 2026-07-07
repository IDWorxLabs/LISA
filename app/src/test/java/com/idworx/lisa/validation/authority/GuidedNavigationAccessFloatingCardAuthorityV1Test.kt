package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.guidednavigationaccessfloatingcard.audit.GuidedNavigationAccessFloatingCardAuditor
import com.idworx.lisa.features.guidednavigationaccessfloatingcard.metadata.GuidedNavigationAccessFloatingCardMetadata
import com.idworx.lisa.features.guidednavigationaccessfloatingcard.validation.GuidedNavigationAccessFloatingCardAuthorityV1
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidedNavigationAccessFloatingCardAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = GuidedNavigationAccessFloatingCardAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(GuidedNavigationAccessFloatingCardAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(GuidedNavigationAccessFloatingCardAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_GUIDED_NAVIGATION_ACCESS_AND_FLOATING_CARD_V1_PASS",
            GuidedNavigationAccessFloatingCardMetadata.PASS_TOKEN
        )
    }

    @Test
    fun welcome_exposesSkipToNavigationTraining() {
        assertTrue(GuidedNavigationAccessFloatingCardAuditor.welcomeExposesSkipToNavigationTraining())
    }

    @Test
    fun skip_startsAtLesson16() {
        assertTrue(GuidedNavigationAccessFloatingCardAuditor.skipStartsAtLesson16())
    }

    @Test
    fun skip_entersRealWorkspaceGuidedTrainingMode() {
        assertTrue(GuidedNavigationAccessFloatingCardAuditor.entersRealWorkspaceGuidedTrainingMode())
    }

    @Test
    fun skip_bypassesPhraseLessons() {
        assertTrue(GuidedNavigationAccessFloatingCardAuditor.bypassesPhraseLessons())
    }

    @Test
    fun lessonCard_notBehindListeningBanner() {
        assertTrue(GuidedNavigationAccessFloatingCardAuditor.lessonCardNotBehindListeningBanner())
    }

    @Test
    fun lessonCard_remainsVisibleReadable() {
        assertTrue(GuidedNavigationAccessFloatingCardAuditor.lessonCardRemainsVisibleReadable())
    }

    @Test
    fun realWorkspaceLayout_notStructurallyChanged() {
        assertTrue(GuidedNavigationAccessFloatingCardAuditor.realWorkspaceLayoutNotStructurallyChanged())
    }

    @Test
    fun highlightedTarget_remainsVisible() {
        assertTrue(GuidedNavigationAccessFloatingCardAuditor.highlightedTargetRemainsVisible())
    }

    @Test
    fun gestureFiltering_stillWorks() {
        assertTrue(GuidedNavigationAccessFloatingCardAuditor.gestureFilteringStillWorks())
    }

    @Test
    fun existingFullGuidedLearningFlow_stillWorks() {
        assertTrue(GuidedNavigationAccessFloatingCardAuditor.existingFullGuidedLearningFlowStillWorks())
    }
}
