package com.idworx.lisa.validation.authority

import com.idworx.lisa.GuidedNavigationController
import com.idworx.lisa.GuidedVocabularyCatalog
import com.idworx.lisa.LisaUiStrings
import com.idworx.lisa.PreferredLanguage
import com.idworx.lisa.SEQUENCE_IDLE_TIMEOUT_MS
import com.idworx.lisa.SequenceProcessingDelay
import com.idworx.lisa.features.coreruntime.alwayswelcomevisiblespeech.metadata.CoreRuntimeAlwaysWelcomeVisibleSpeechMetadata
import com.idworx.lisa.features.coreruntime.alwayswelcomevisiblespeech.validation.CoreRuntimeAlwaysWelcomeVisibleSpeechAuthorityV1
import com.idworx.lisa.features.launchwelcomestatepriority.WelcomeStatePriorityGate
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.features.onboardingguide.navigation.GuidedTrainingNavigator
import com.idworx.lisa.features.onboardingguide.state.TrainingEvent
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreRuntimeAlwaysWelcomeVisibleSpeechAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = CoreRuntimeAlwaysWelcomeVisibleSpeechAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(CoreRuntimeAlwaysWelcomeVisibleSpeechAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(CoreRuntimeAlwaysWelcomeVisibleSpeechAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun coldLaunch_alwaysWelcomeEvenWhenSkipped() {
        val saved = TrainingProgress(
            tutorialSkipped = true,
            firstLaunchChoiceMade = true,
            currentPhase = TrainingPhase.Completion
        )
        val gated = WelcomeStatePriorityGate.applyForColdLaunch(saved)
        assertEquals(TrainingPhase.FirstLaunchChoice, gated.currentPhase)
        assertFalse(gated.firstLaunchChoiceMade)
    }

    @Test
    fun idleTimeout_matchesDefaultResponseTime() {
        assertEquals(SequenceProcessingDelay.toMillis(SequenceProcessingDelay.DEFAULT_SECONDS), SEQUENCE_IDLE_TIMEOUT_MS)
        assertEquals(5000L, SEQUENCE_IDLE_TIMEOUT_MS)
    }

    @Test
    fun hiddenPagePhrase_doesNotResolve() {
        val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)
        val context = com.idworx.lisa.GuidedCatalogContext()
        val entries = GuidedVocabularyCatalog.currentPageEntries(0, PreferredLanguage.English, uiStrings, context)
        if (entries.size <= GuidedVocabularyCatalog.DEFAULT_VISIBLE_ENTRY_CAP) return
        val hidden = entries[GuidedVocabularyCatalog.DEFAULT_VISIBLE_ENTRY_CAP]
        val match = GuidedVocabularyCatalog.findMatchOnVisiblePage(
            left = hidden.left,
            right = hidden.right,
            pageIndex = 0,
            phrasePageIndex = 0,
            language = PreferredLanguage.English,
            uiStrings = uiStrings,
            catalogContext = context
        )
        assertNull(match)
    }

    @Test
    fun beginLearning_requiredBeforeTeaching() {
        val progress = GuidedTrainingNavigator().reduce(TrainingProgress(), TrainingEvent.BeginLearning)
        assertEquals(TrainingPhase.Setup, progress.currentPhase)
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_CORE_RUNTIME_ALWAYS_WELCOME_VISIBLE_SPEECH_V1_PASS",
            CoreRuntimeAlwaysWelcomeVisibleSpeechMetadata.PASS_TOKEN
        )
    }
}
