package com.idworx.lisa.features.gesturesequenceaudit

enum class GestureSourceContext {
    GUIDED_LEARNING_ESSENTIAL,
    GUIDED_LEARNING_FULL,
    WORKSPACE_DEFAULT,
    SYSTEM_COMMAND,
    GLOBAL_NAVIGATION,
    PRACTICE_MODE,
    EMERGENCY,
    BRAIN1_DECISION
}

enum class GestureDuplicateClass {
    /** Same L/R counts for different phrases/actions in one active context. */
    INVALID_DUPLICATE,
    /** Same sequence in isolated contexts (only one active at a time). */
    VALID_CONTEXTUAL_REUSE,
    /** Phrase uses a reserved navigation/system gesture. */
    RESERVED_CONFLICT
}

data class GestureSequenceEntry(
    val source: String,
    val context: GestureSourceContext,
    val left: Int,
    val right: Int,
    val blinkOrder: String? = null,
    val label: String,
    val vocabularyId: String? = null
) {
    val sequenceKey: String get() = "L$left R$right"
    val orderKey: String get() = if (blinkOrder.isNullOrBlank()) sequenceKey else "$sequenceKey:$blinkOrder"
}

data class GestureDuplicateFinding(
    val classification: GestureDuplicateClass,
    val sequenceKey: String,
    val entries: List<GestureSequenceEntry>,
    val note: String
)

data class GestureSequenceAuditReport(
    val entries: List<GestureSequenceEntry>,
    val findings: List<GestureDuplicateFinding>,
    val guidedEssentialsUnique: Boolean,
    val noAndPleaseDistinct: Boolean
) {
    val invalidDuplicates: List<GestureDuplicateFinding> =
        findings.filter { it.classification == GestureDuplicateClass.INVALID_DUPLICATE }

    val reservedConflicts: List<GestureDuplicateFinding> =
        findings.filter { it.classification == GestureDuplicateClass.RESERVED_CONFLICT }

    val validReuses: List<GestureDuplicateFinding> =
        findings.filter { it.classification == GestureDuplicateClass.VALID_CONTEXTUAL_REUSE }
}
