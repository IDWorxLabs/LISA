package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchAuditor
import com.idworx.lisa.features.zerotouchprinciple.experience.FirstConversationExperience
import com.idworx.lisa.features.zerotouchprinciple.metadata.ZeroTouchPrincipleMetadata
import com.idworx.lisa.features.zerotouchprinciple.validation.ZeroTouchPrincipleAuthorityV1
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZeroTouchPrincipleAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = ZeroTouchPrincipleAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(ZeroTouchPrincipleAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(ZeroTouchPrincipleAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun metadata_definesConstitutionalPriority() {
        assertEquals(1, ZeroTouchPrincipleMetadata.CONSTITUTIONAL_PRIORITY)
        assertTrue(ZeroTouchPrincipleMetadata.CORE_RULE.contains("eye movement"))
    }

    @Test
    fun firstConversationExperience_hasStagesAndPatience() {
        assertTrue(FirstConversationExperience.Stage.entries.size >= 7)
        assertTrue(FirstConversationExperience.patienceDialogues().any { it.contains("wait", ignoreCase = true) })
        assertTrue(FirstConversationExperience.gettingReadyDialogues().isNotEmpty())
    }

    @Test
    fun auditor_welcomeIsConversational() {
        assertTrue(ZeroTouchAuditor.welcomeIsConversational())
    }

    @Test
    fun auditor_noUserBlameInCatalog() {
        assertTrue(ZeroTouchAuditor.noUserBlameInCatalog())
    }

    @Test
    fun auditor_noTapContinueInGuidedLearning() {
        assertTrue(ZeroTouchAuditor.noTapContinueInGuidedLearning())
    }
}
