package com.idworx.lisa.validation.authority

import com.idworx.lisa.GuidedNavigationController
import com.idworx.lisa.GuidedVocabularyCatalog
import com.idworx.lisa.LisaUiStrings
import com.idworx.lisa.PreferredLanguage
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.navigation.GuidedTrainingNavigator
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.features.onboardingguide.state.TrainingEvent
import com.idworx.lisa.features.runtimecameracontextualspeech.metadata.RuntimeCameraContextualSpeechMetadata
import com.idworx.lisa.features.runtimecameracontextualspeech.validation.RuntimeCameraContextualSpeechAuthorityV1
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeCameraContextualSpeechAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = RuntimeCameraContextualSpeechAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(RuntimeCameraContextualSpeechAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(RuntimeCameraContextualSpeechAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun completeSetup_routesToHelloLesson() {
        val progress = GuidedTrainingNavigator().reduce(TrainingProgress(), TrainingEvent.CompleteSetup)
        assertEquals(TrainingPhase.CommunicationLesson, progress.currentPhase)
        assertEquals(0, progress.communicationLessonIndex)
    }

    @Test
    fun hiddenCategoryPhrase_resolvesOpenCategoryNotForeignPhrase() {
        val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)
        val context = com.idworx.lisa.GuidedCatalogContext()
        val medicalEntries = GuidedVocabularyCatalog.currentPageEntries(2, PreferredLanguage.English, uiStrings, context)
        val conversationEntries = GuidedVocabularyCatalog.currentPageEntries(0, PreferredLanguage.English, uiStrings, context)
        val medicalPhrase = medicalEntries.first()
        val conversationPhrase = conversationEntries.first()
        val match = GuidedVocabularyCatalog.findMatchOnVisiblePage(
            left = medicalPhrase.left,
            right = medicalPhrase.right,
            pageIndex = 0,
            phrasePageIndex = 0,
            language = PreferredLanguage.English,
            uiStrings = uiStrings,
            catalogContext = context
        )
        assertEquals(conversationPhrase.phrase, match?.phrase)
        assertTrue(match?.phrase != medicalPhrase.phrase)
    }

    @Test
    fun visiblePhrase_resolvesOnCurrentPage() {
        val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)
        val context = com.idworx.lisa.GuidedCatalogContext()
        val entries = GuidedVocabularyCatalog.currentPageEntries(0, PreferredLanguage.English, uiStrings, context)
        val visible = GuidedNavigationController.visiblePhraseEntries(entries, 0, GuidedVocabularyCatalog.DEFAULT_VISIBLE_ENTRY_CAP).first()
        val match = GuidedVocabularyCatalog.findMatchOnVisiblePage(
            left = visible.left,
            right = visible.right,
            pageIndex = 0,
            phrasePageIndex = 0,
            language = PreferredLanguage.English,
            uiStrings = uiStrings,
            catalogContext = context
        )
        assertEquals(visible.phrase, match?.phrase)
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_RUNTIME_CAMERA_AND_CONTEXTUAL_SPEECH_V1_PASS",
            RuntimeCameraContextualSpeechMetadata.PASS_TOKEN
        )
    }
}
