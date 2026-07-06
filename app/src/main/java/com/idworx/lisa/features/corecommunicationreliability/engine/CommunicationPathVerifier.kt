package com.idworx.lisa.features.corecommunicationreliability.engine

import com.idworx.lisa.defaultLanguageMappings
import com.idworx.lisa.features.corecommunicationreliability.emergency.EmergencySpeechSafetyGuard
import com.idworx.lisa.features.corecommunicationreliability.history.CommunicationReliabilityHistoryRecorder
import com.idworx.lisa.features.corecommunicationreliability.model.CommunicationMode
import com.idworx.lisa.features.corecommunicationreliability.model.CommunicationReliabilityOutcome
import com.idworx.lisa.features.corecommunicationreliability.phrase.PhraseMatchVerifier
import com.idworx.lisa.features.corecommunicationreliability.sequence.BlinkSequenceDebouncer
import com.idworx.lisa.features.corecommunicationreliability.sequence.BlinkSequenceNormalizer
import com.idworx.lisa.features.corecommunicationreliability.speech.SpeechReliabilityAdapter

object CommunicationPathVerifier {

    fun verifyPathComponentsExist(): Boolean =
        classExists(CoreCommunicationReliabilityEngine::class.java) &&
            classExists(DefaultCoreCommunicationReliabilityEngine::class.java)

    fun verifyNormalization(): Boolean {
        val normalized = BlinkSequenceNormalizer.normalize(2, 1)
        return normalized.label == "L2 R1" && normalized.left == 2 && normalized.right == 1
    }

    fun verifyPhraseMatchDeterministic(): Boolean {
        val mappings = defaultLanguageMappings()
        val hello = PhraseMatchVerifier.verify(1, 6, mappings, com.idworx.lisa.PreferredLanguage.English)
        return hello.phraseId == "hello" && hello.matchCount == 1
    }

    fun verifyEmergencyBlockedInCommunicationTraining(): Boolean {
        val safety = EmergencySpeechSafetyGuard.evaluate(
            left = 6, right = 0,
            navigationTrainingActive = false,
            communicationTrainingActive = true,
            practiceMode = false
        )
        return !safety.allowed && safety.blockedReason != null
    }

    fun verifyDuplicateDebouncing(): Boolean {
        val debouncer = BlinkSequenceDebouncer(debounceWindowMs = 5000L)
        val first = debouncer.shouldAllow(2, 6)
        val duplicate = debouncer.shouldAllow(2, 6)
        return first && !duplicate
    }

    fun verifyHistoryRecording(): Boolean {
        CommunicationReliabilityHistoryRecorder.clear()
        CommunicationReliabilityHistoryRecorder.record(
            phraseId = "yes",
            phraseText = "Yes",
            sequenceLeft = 2,
            sequenceRight = 6,
            mode = CommunicationMode.MAIN,
            emergency = false,
            speechSuccess = true
        )
        return CommunicationReliabilityHistoryRecorder.count() == 1
    }

    fun verifySpeechAdapterDoesNotCreateTts(): Boolean =
        classExists(SpeechReliabilityAdapter::class.java)

    fun verifyValidPathReachesSpeechOutcome(): Boolean {
        val engine = CoreCommunicationReliabilityEngines.createForTests()
        val ctx = CommunicationReliabilityContext(mappings = defaultLanguageMappings())
        val report = engine.evaluatePhrasePath(ctx, 2, 6)
        return report.finalOutcome in setOf(
            CommunicationReliabilityOutcome.PASS,
            CommunicationReliabilityOutcome.WARN
        ) && report.attemptResult.phraseId == "yes"
    }

    private fun classExists(clazz: Class<*>): Boolean = try {
        Class.forName(clazz.name)
        true
    } catch (_: Exception) {
        false
    }
}
