package com.idworx.lisa

/**
 * Caregiver phrase creation — validation, sequence allocation, and catalog integration.
 *
 * UI-agnostic so RC7C can swap touch entry for eye-controlled input without changing this engine.
 */
object CustomPhraseEngine {

    const val MAX_PHRASE_LENGTH = 80

    /** Standard + extended vocabulary slot indices available per category page. */
    private val approvedSlotCount: Int =
        GuidedPageSequences.slots.size + GuidedPageSequences.extendedSlots.size

    enum class CaregiverPhraseCategory {
        Conversation,
        BasicNeeds,
        Medical,
        Family,
        Custom;

        fun storageKey(): String = name

        fun toGuidedCategory(): GuidedVocabularyCategory = when (this) {
            Conversation -> GuidedVocabularyCategory.Conversation
            BasicNeeds -> GuidedVocabularyCategory.BasicNeeds
            Medical -> GuidedVocabularyCategory.Medical
            Family -> GuidedVocabularyCategory.Family
            Custom -> GuidedVocabularyCategory.Custom
        }

        companion object {
            fun fromStorageKey(value: String): CaregiverPhraseCategory =
                entries.find { it.name.equals(value, ignoreCase = true) } ?: Conversation
        }
    }

    data class CaregiverCustomPhraseEntry(
        val phrase: String,
        val left: Int,
        val right: Int,
        val category: CaregiverPhraseCategory
    )

    sealed class PhraseValidationResult {
        data class Valid(val normalizedPhrase: String) : PhraseValidationResult()
        data class Invalid(val reason: PhraseValidationFailure) : PhraseValidationResult()
    }

    enum class PhraseValidationFailure {
        Empty,
        TooLong,
        Duplicate
    }

    sealed class SavePhraseResult {
        data class Success(val mapping: WinkMapping) : SavePhraseResult()
        data class ValidationFailed(val reason: PhraseValidationFailure) : SavePhraseResult()
        object NoSequenceAvailable : SavePhraseResult()
    }

    data class AllocationAuditFinding(
        val phrase: String,
        val category: CaregiverPhraseCategory,
        val currentSequence: Pair<Int, Int>,
        val suggestedSequence: Pair<Int, Int>?,
        val reason: String
    )

    data class CustomCategoryMigrationResult(
        val mappings: List<WinkMapping>,
        val migratedCount: Int,
        val reallocatedCount: Int
    )

    /** Destination categories for post-compose save — Custom is an entry point, not storage. */
    val selectableCategories: List<CaregiverPhraseCategory> = listOf(
        CaregiverPhraseCategory.Conversation,
        CaregiverPhraseCategory.BasicNeeds,
        CaregiverPhraseCategory.Medical,
        CaregiverPhraseCategory.Family
    )

    /**
     * One-time migration: move legacy Custom-category phrases to General Conversation.
     * Preserves phrase text; keeps sequence when safe in Conversation, else reallocates.
     */
    fun migrateCustomCategoryMappings(mappings: List<WinkMapping>): CustomCategoryMigrationResult {
        var migratedCount = 0
        var reallocatedCount = 0
        val mutable = mappings.toMutableList()
        for (index in mutable.indices) {
            val mapping = mutable[index]
            if (!mapping.isCustom || mapping.caregiverCategory != CaregiverPhraseCategory.Custom) continue
            migratedCount++
            val others = mutable.filterIndexed { otherIndex, _ -> otherIndex != index }
            val target = CaregiverPhraseCategory.Conversation
            val currentSequence = mapping.left to mapping.right
            val validInTarget = rankedCandidatesForCategory(target, others).contains(currentSequence)
            mutable[index] = if (validInTarget) {
                mapping.copy(caregiverCategory = target)
            } else {
                reallocatedCount++
                val replacement = allocateSequence(target, others) ?: currentSequence
                mapping.copy(
                    caregiverCategory = target,
                    left = replacement.first,
                    right = replacement.second
                )
            }
        }
        return CustomCategoryMigrationResult(
            mappings = mutable,
            migratedCount = migratedCount,
            reallocatedCount = reallocatedCount
        )
    }

    fun normalizePhrase(raw: String): String = raw.trim()

    fun validatePhrase(raw: String, existingCustom: List<WinkMapping>): PhraseValidationResult {
        val normalized = normalizePhrase(raw)
        if (normalized.isBlank()) return PhraseValidationResult.Invalid(PhraseValidationFailure.Empty)
        if (normalized.length > MAX_PHRASE_LENGTH) {
            return PhraseValidationResult.Invalid(PhraseValidationFailure.TooLong)
        }
        if (isDuplicatePhrase(normalized, existingCustom)) {
            return PhraseValidationResult.Invalid(PhraseValidationFailure.Duplicate)
        }
        return PhraseValidationResult.Valid(normalized)
    }

    fun saveNewPhrase(
        rawPhrase: String,
        category: CaregiverPhraseCategory,
        existingMappings: List<WinkMapping>
    ): SavePhraseResult {
        val existingCustom = existingMappings.filter { it.isCustom }
        when (val validation = validatePhrase(rawPhrase, existingCustom)) {
            is PhraseValidationResult.Invalid -> return SavePhraseResult.ValidationFailed(validation.reason)
            is PhraseValidationResult.Valid -> {
                val sequence = allocateSequence(category, existingMappings)
                    ?: return SavePhraseResult.NoSequenceAvailable
                return SavePhraseResult.Success(
                    WinkMapping(
                        left = sequence.first,
                        right = sequence.second,
                        vocabularyId = validation.normalizedPhrase,
                        isCustom = true,
                        customPhrase = validation.normalizedPhrase,
                        caregiverCategory = category
                    )
                )
            }
        }
    }

    /**
     * Assigns the shortest safe sequence for [category], scoped to that category's visible page.
     * Performs a self-audit before returning.
     */
    fun allocateSequence(
        category: CaregiverPhraseCategory,
        existingMappings: List<WinkMapping>
    ): Pair<Int, Int>? {
        val selected = selectShortestValidSequence(category, existingMappings) ?: return null
        check(allocationSelfAuditPasses(category, selected, existingMappings)) {
            "Allocation self-audit failed: a simpler valid candidate exists for $category"
        }
        return selected
    }

    /** Reports saved custom mappings that could use a shorter category-local sequence (no remapping). */
    fun auditExistingCustomMappings(mappings: List<WinkMapping>): List<AllocationAuditFinding> {
        val custom = mappings.filter { it.isCustom }
        return custom.mapNotNull { mapping ->
            val category = mapping.caregiverCategory ?: CaregiverPhraseCategory.Conversation
            val others = custom.filterNot { it === mapping }
            val optimal = selectShortestValidSequence(category, others + mappings.filter { !it.isCustom })
            val current = mapping.left to mapping.right
            when {
                optimal == null -> AllocationAuditFinding(
                    phrase = mapping.customPhrase.orEmpty(),
                    category = category,
                    currentSequence = current,
                    suggestedSequence = null,
                    reason = "No valid replacement sequence found"
                )
                optimal != current && sequenceRank(optimal) < sequenceRank(current) ->
                    AllocationAuditFinding(
                        phrase = mapping.customPhrase.orEmpty(),
                        category = category,
                        currentSequence = current,
                        suggestedSequence = optimal,
                        reason = "Shorter category-local sequence available"
                    )
                categoryLocalOccupiedSequences(category, others).contains(current) &&
                    others.any {
                        it.caregiverCategory == category &&
                            it.left == mapping.left &&
                            it.right == mapping.right
                    } ->
                    AllocationAuditFinding(
                        phrase = mapping.customPhrase.orEmpty(),
                        category = category,
                        currentSequence = current,
                        suggestedSequence = optimal,
                        reason = "Duplicate sequence within category"
                    )
                globallyExcludedSequences().contains(current) ->
                    AllocationAuditFinding(
                        phrase = mapping.customPhrase.orEmpty(),
                        category = category,
                        currentSequence = current,
                        suggestedSequence = optimal,
                        reason = "Conflicts with a globally reserved sequence"
                    )
                else -> null
            }
        }
    }

    fun rankedCandidatesForCategory(
        category: CaregiverPhraseCategory,
        existingMappings: List<WinkMapping>
    ): List<Pair<Int, Int>> = buildRankedCandidates(category, existingMappings)
        .filter { isValidCandidate(category, it, existingMappings) }

    fun allocationSelfAuditPasses(
        category: CaregiverPhraseCategory,
        selected: Pair<Int, Int>,
        existingMappings: List<WinkMapping>
    ): Boolean = rankedCandidatesForCategory(category, existingMappings).firstOrNull() == selected

    fun categoryLocalOccupiedSequences(
        category: CaregiverPhraseCategory,
        existingMappings: List<WinkMapping>
    ): Set<Pair<Int, Int>> {
        val builtIn = builtInSequencesForCategory(category)
        val customInCategory = existingMappings
            .filter { it.isCustom && it.caregiverCategory == category }
            .map { it.left to it.right }
            .toSet()
        return builtIn + customInCategory
    }

    fun globallyExcludedSequences(): Set<Pair<Int, Int>> =
        LisaSystemLanguage.allReservedSequences() +
            GuidedPageSequences.forbiddenForVocabulary +
            setOf(
                GuidedModeNavigation.DECREASE_VALUE_LEFT to GuidedModeNavigation.DECREASE_VALUE_RIGHT,
                GuidedModeNavigation.INCREASE_VALUE_LEFT to GuidedModeNavigation.INCREASE_VALUE_RIGHT
            )

    fun toCatalogEntries(customMappings: List<WinkMapping>): List<CaregiverCustomPhraseEntry> =
        customMappings.mapNotNull { mapping ->
            val phrase = mapping.customPhrase?.trim().orEmpty()
            if (phrase.isBlank()) return@mapNotNull null
            CaregiverCustomPhraseEntry(
                phrase = phrase,
                left = mapping.left,
                right = mapping.right,
                category = mapping.caregiverCategory ?: CaregiverPhraseCategory.Conversation
            )
        }

    fun serializeCustomMappings(custom: List<WinkMapping>): String = buildString {
        custom.forEach { mapping ->
            val category = mapping.caregiverCategory?.storageKey() ?: CaregiverPhraseCategory.Conversation.storageKey()
            append("${mapping.left},${mapping.right}|${mapping.phrase.replace("\n", " ")}|$category\n")
        }
    }

    fun parseCustomMappings(raw: String): List<WinkMapping> {
        if (raw.isBlank()) return emptyList()
        return raw.lines().mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isBlank()) return@mapNotNull null
            val parts = trimmed.split("|")
            if (parts.size < 2) return@mapNotNull null
            val lr = parts[0].split(",", limit = 2)
            if (lr.size != 2) return@mapNotNull null
            val left = lr[0].toIntOrNull() ?: return@mapNotNull null
            val right = lr[1].toIntOrNull() ?: return@mapNotNull null
            val phrase = parts[1].trim()
            if (phrase.isBlank()) return@mapNotNull null
            val category = if (parts.size >= 3) {
                CaregiverPhraseCategory.fromStorageKey(parts[2].trim())
            } else {
                CaregiverPhraseCategory.Conversation
            }
            WinkMapping(
                left = left,
                right = right,
                vocabularyId = phrase,
                isCustom = true,
                customPhrase = phrase,
                caregiverCategory = category
            )
        }
    }

    private fun selectShortestValidSequence(
        category: CaregiverPhraseCategory,
        existingMappings: List<WinkMapping>
    ): Pair<Int, Int>? = rankedCandidatesForCategory(category, existingMappings).firstOrNull()

    private fun isValidCandidate(
        category: CaregiverPhraseCategory,
        candidate: Pair<Int, Int>,
        existingMappings: List<WinkMapping>
    ): Boolean {
        val (left, right) = candidate
        if (!isSequenceEligibleForSpeech(left, right)) return false
        if (left < 1 || right < 1) return false
        if (candidate in globallyExcludedSequences()) return false
        if (GuidedModeNavigation.isGlobalNavigationSequence(left, right)) return false
        if (isEmergencySequence(left, right)) return false
        if (candidate in categoryLocalOccupiedSequences(category, existingMappings)) return false
        return true
    }

    private fun builtInSequencesForCategory(category: CaregiverPhraseCategory): Set<Pair<Int, Int>> {
        val guidedCategory = category.toGuidedCategory()
        val pages = GuidedVocabularyCatalog.buildPages(
            PreferredLanguage.English,
            LisaUiStrings.forLanguage(PreferredLanguage.English)
        )
        return pages.firstOrNull { it.category == guidedCategory }
            ?.entries
            ?.map { it.left to it.right }
            ?.toSet()
            .orEmpty()
    }

    private fun buildRankedCandidates(
        category: CaregiverPhraseCategory,
        existingMappings: List<WinkMapping>
    ): List<Pair<Int, Int>> {
        val approvedSlots = (0 until approvedSlotCount).map { GuidedPageSequences.slotAt(it) }
        val approvedSet = approvedSlots.toSet()
        val boundedExtras = boundedFallbackCandidates()
            .filter { it !in approvedSet }
            .sortedWith(sequenceRankComparator())
        // Approved slots keep catalog slot order (next-free-slot policy); extras are rank-sorted.
        return approvedSlots + boundedExtras
    }

    private fun boundedFallbackCandidates(): List<Pair<Int, Int>> {
        val results = mutableListOf<Pair<Int, Int>>()
        for (left in 1..7) {
            for (right in 1..7) {
                results.add(left to right)
            }
        }
        return results.sortedWith(sequenceRankComparator())
    }

    private fun sequenceRank(sequence: Pair<Int, Int>): Int {
        val (left, right) = sequence
        val total = left + right
        val maxEye = maxOf(left, right)
        val imbalance = kotlin.math.abs(left - right)
        return total * 10_000 + maxEye * 100 + imbalance * 10 + left
    }

    private fun sequenceRankComparator(): Comparator<Pair<Int, Int>> =
        compareBy(
            { it.first + it.second },
            { maxOf(it.first, it.second) },
            { kotlin.math.abs(it.first - it.second) },
            { it.first },
            { it.second }
        )

    private fun isDuplicatePhrase(normalized: String, existingCustom: List<WinkMapping>): Boolean {
        val target = normalized.lowercase()
        if (existingCustom.any { it.customPhrase.orEmpty().trim().lowercase() == target }) return true
        return defaultLanguageMappings().any { mapping ->
            PreferredLanguage.entries.any { language ->
                mapping.localizedPhrase(language).trim().lowercase() == target
            }
        }
    }
}
