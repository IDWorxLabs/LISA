package com.idworx.lisa.features.corecommunicationreliability.history

import com.idworx.lisa.features.corecommunicationreliability.metadata.CoreCommunicationReliabilityMetadata
import com.idworx.lisa.features.corecommunicationreliability.model.CommunicationMode
import com.idworx.lisa.features.corecommunicationreliability.model.SpokenPhraseRecord
import com.idworx.lisa.formatWinkSequenceShort
import java.util.UUID

object CommunicationReliabilityHistoryRecorder {

    private val records = mutableListOf<SpokenPhraseRecord>()
    private val speechObservers = mutableListOf<(SpokenPhraseRecord) -> Unit>()

    fun addSpeechObserver(observer: (SpokenPhraseRecord) -> Unit) {
        speechObservers.add(observer)
    }

    fun clearSpeechObservers() {
        speechObservers.clear()
    }

    fun record(
        phraseId: String?,
        phraseText: String,
        sequenceLeft: Int,
        sequenceRight: Int,
        mode: CommunicationMode,
        emergency: Boolean,
        speechSuccess: Boolean
    ): SpokenPhraseRecord {
        val entry = SpokenPhraseRecord(
            recordId = UUID.randomUUID().toString(),
            phraseId = phraseId,
            phraseText = phraseText,
            timestampMs = System.currentTimeMillis(),
            sequenceLeft = sequenceLeft,
            sequenceRight = sequenceRight,
            sequenceLabel = formatWinkSequenceShort(sequenceLeft, sequenceRight),
            mode = mode,
            emergency = emergency,
            speechSuccess = speechSuccess
        )
        records.add(entry)
        trim()
        speechObservers.forEach { it(entry) }
        return entry
    }

    fun recent(limit: Int = 20): List<SpokenPhraseRecord> =
        records.takeLast(limit)

    fun last(): SpokenPhraseRecord? = records.lastOrNull()

    fun count(): Int = records.size

    fun clear() = records.clear()

    private fun trim() {
        while (records.size > CoreCommunicationReliabilityMetadata.MAX_HISTORY_ENTRIES) {
            records.removeAt(0)
        }
    }
}
