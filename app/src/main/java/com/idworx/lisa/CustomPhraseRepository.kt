package com.idworx.lisa

import android.content.Context

/** Failure reasons for custom phrase persistence and management (RC7D.10). */
enum class PhraseSaveFailureReason {
    Empty,
    TooLong,
    Duplicate,
    NoSequenceAvailable,
    StorageWriteFailed,
    StorageVerificationFailed,
    RuntimeCatalogMissing,
    PhraseNotFound,
    MalformedStoredData
}

sealed class PhraseSaveTransactionResult {
    data class Success(
        val mapping: WinkMapping,
        val category: CustomPhraseEngine.CaregiverPhraseCategory,
        val persisted: Boolean,
        val runtimeVisible: Boolean,
        val phrasePageIndex: Int
    ) : PhraseSaveTransactionResult()

    data class Failed(
        val reason: PhraseSaveFailureReason,
        val duplicateMatch: DuplicatePhraseMatch? = null
    ) : PhraseSaveTransactionResult()
}

sealed class PhraseManagementResult {
    data class Success(val mapping: WinkMapping) : PhraseManagementResult()
    data class Failed(
        val reason: PhraseSaveFailureReason,
        val duplicateMatch: DuplicatePhraseMatch? = null
    ) : PhraseManagementResult()
}

data class CustomPhraseStorageAuditFinding(
    val phrase: String,
    val detail: String
)

/** Canonical custom-phrase storage and transactional save authority (RC7D.10). */
object CustomPhraseRepository {

    private const val PREFS_NAME = "lisa_prefs"
    private const val KEY_CUSTOM_MAPS = "custom_maps"

    /** Test hook — when set, bypasses Android SharedPreferences. */
    var testStorage: MutableMap<String, String>? = null

    fun listCustomPhrases(allMappings: List<WinkMapping>): List<WinkMapping> =
        allMappings.filter { it.isCustom && !it.customPhrase.isNullOrBlank() }

    fun createPhrase(
        rawPhrase: String,
        category: CustomPhraseEngine.CaregiverPhraseCategory,
        allocatedSequence: Pair<Int, Int>,
        existingMappings: List<WinkMapping>,
        language: PreferredLanguage = PreferredLanguage.English,
        uiStrings: LisaUiStrings = LisaUiStrings.forLanguage(language),
        visibleEntryCap: Int = GuidedVocabularyCatalog.DEFAULT_VISIBLE_ENTRY_CAP,
        context: Context? = null
    ): PhraseSaveTransactionResult {
        val engineResult = CustomPhraseEngine.saveNewPhraseWithAllocatedSequence(
            rawPhrase = rawPhrase,
            category = category,
            allocatedSequence = allocatedSequence,
            existingMappings = existingMappings,
            language = language,
            uiStrings = uiStrings
        )
        return when (engineResult) {
            is CustomPhraseEngine.SavePhraseResult.Success ->
                persistAndVerify(
                    updatedCustom = existingMappings.filter { it.isCustom } + engineResult.mapping,
                    expectedMapping = engineResult.mapping,
                    language = language,
                    uiStrings = uiStrings,
                    visibleEntryCap = visibleEntryCap,
                    context = context
                )
            is CustomPhraseEngine.SavePhraseResult.ValidationFailed ->
                PhraseSaveTransactionResult.Failed(
                    reason = engineResult.reason.toSaveFailureReason(),
                    duplicateMatch = engineResult.duplicateMatch
                )
            CustomPhraseEngine.SavePhraseResult.NoSequenceAvailable ->
                PhraseSaveTransactionResult.Failed(PhraseSaveFailureReason.NoSequenceAvailable)
        }
    }

    fun updatePhraseText(
        identity: CustomPhraseIdentity,
        rawPhrase: String,
        existingMappings: List<WinkMapping>,
        language: PreferredLanguage = PreferredLanguage.English,
        uiStrings: LisaUiStrings = LisaUiStrings.forLanguage(language),
        visibleEntryCap: Int = GuidedVocabularyCatalog.DEFAULT_VISIBLE_ENTRY_CAP,
        context: Context? = null
    ): PhraseManagementResult {
        val current = findCustom(existingMappings, identity)
            ?: return PhraseManagementResult.Failed(PhraseSaveFailureReason.PhraseNotFound)
        val others = existingMappings.filter { it.isCustom && !sameIdentity(it, identity) }
        when (val validation = CustomPhraseEngine.validatePhrase(rawPhrase, others, language, uiStrings)) {
            is CustomPhraseEngine.PhraseValidationResult.Invalid ->
                return PhraseManagementResult.Failed(
                    validation.reason.toSaveFailureReason(),
                    validation.duplicateMatch
                )
            is CustomPhraseEngine.PhraseValidationResult.Valid -> {
                val updated = current.copy(
                    customPhrase = validation.normalizedPhrase,
                    vocabularyId = validation.normalizedPhrase
                )
        return persistUpdatedMapping(
            existingMappings = existingMappings,
            updated = updated,
            replacedIdentity = identity,
            language = language,
            uiStrings = uiStrings,
            visibleEntryCap = visibleEntryCap,
            context = context
        )
            }
        }
    }

    fun movePhrase(
        identity: CustomPhraseIdentity,
        targetCategory: CustomPhraseEngine.CaregiverPhraseCategory,
        existingMappings: List<WinkMapping>,
        language: PreferredLanguage = PreferredLanguage.English,
        uiStrings: LisaUiStrings = LisaUiStrings.forLanguage(language),
        visibleEntryCap: Int = GuidedVocabularyCatalog.DEFAULT_VISIBLE_ENTRY_CAP,
        context: Context? = null
    ): PhraseManagementResult {
        val current = findCustom(existingMappings, identity)
            ?: return PhraseManagementResult.Failed(PhraseSaveFailureReason.PhraseNotFound)
        if (current.caregiverCategory == targetCategory) {
            return PhraseManagementResult.Success(current)
        }
        val withoutCurrent = existingMappings.filter { !sameIdentity(it, identity) }
        val currentSequence = current.left to current.right
        val sequence = if (CustomPhraseEngine.rankedCandidatesForCategory(targetCategory, withoutCurrent)
                .contains(currentSequence)
        ) {
            currentSequence
        } else {
            CustomPhraseEngine.allocateSequence(targetCategory, withoutCurrent)
                ?: return PhraseManagementResult.Failed(PhraseSaveFailureReason.NoSequenceAvailable)
        }
        val updated = current.copy(
            caregiverCategory = targetCategory,
            left = sequence.first,
            right = sequence.second
        )
        return persistUpdatedMapping(
            existingMappings = existingMappings,
            updated = updated,
            replacedIdentity = identity,
            language = language,
            uiStrings = uiStrings,
            visibleEntryCap = visibleEntryCap,
            context = context
        )
    }

    fun deletePhrase(
        identity: CustomPhraseIdentity,
        existingMappings: List<WinkMapping>,
        context: Context? = null
    ): PhraseManagementResult {
        if (findCustom(existingMappings, identity) == null) {
            return PhraseManagementResult.Failed(PhraseSaveFailureReason.PhraseNotFound)
        }
        val remainingCustom = existingMappings
            .filter { it.isCustom && !sameIdentity(it, identity) }
        if (!writeCustomMappings(remainingCustom, context)) {
            return PhraseManagementResult.Failed(PhraseSaveFailureReason.StorageWriteFailed)
        }
        val verified = loadCustomMappings(context)
        if (verified.any { sameIdentity(it, identity) }) {
            return PhraseManagementResult.Failed(PhraseSaveFailureReason.StorageVerificationFailed)
        }
        return PhraseManagementResult.Success(
            WinkMapping(
                left = identity.left,
                right = identity.right,
                vocabularyId = identity.phrase,
                isCustom = true,
                customPhrase = identity.phrase
            )
        )
    }

    fun verifyPersistedMapping(mapping: WinkMapping, context: Context? = null): Boolean {
        val stored = loadCustomMappings(context)
        return stored.count {
            it.left == mapping.left &&
                it.right == mapping.right &&
                PhraseDuplicateEngine.canonicalIdentity(it.customPhrase.orEmpty()) ==
                PhraseDuplicateEngine.canonicalIdentity(mapping.customPhrase.orEmpty()) &&
                it.caregiverCategory == mapping.caregiverCategory
        } == 1
    }

    fun auditStoredMappings(
        runtimeMappings: List<WinkMapping>,
        language: PreferredLanguage = PreferredLanguage.English,
        uiStrings: LisaUiStrings = LisaUiStrings.forLanguage(language),
        context: Context? = null
    ): List<CustomPhraseStorageAuditFinding> = buildList {
        val raw = testStorage?.get(KEY_CUSTOM_MAPS).orEmpty().ifBlank {
            prefs(context)?.getString(KEY_CUSTOM_MAPS, "").orEmpty()
        }
        raw.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isBlank()) return@forEach
            val parts = trimmed.split("|")
            if (parts.size < 2) {
                add(CustomPhraseStorageAuditFinding(trimmed, "Malformed stored mapping"))
                return@forEach
            }
            val phrase = parts[1].trim()
            if (phrase.isBlank()) {
                add(CustomPhraseStorageAuditFinding(trimmed, "Malformed stored mapping: empty phrase"))
            }
        }
        val stored = loadCustomMappings(context)
        val parsedIds = stored.map { identityKey(it) }
        parsedIds.groupBy { it }.filter { it.value.size > 1 }.forEach { (key, _) ->
            add(CustomPhraseStorageAuditFinding(key, "Duplicate stored mapping"))
        }
        stored.forEach { mapping ->
            if (mapping.customPhrase.isNullOrBlank()) {
                add(CustomPhraseStorageAuditFinding(identityKey(mapping), "Malformed stored mapping: empty phrase"))
            }
            if (mapping.caregiverCategory == null) {
                add(CustomPhraseStorageAuditFinding(identityKey(mapping), "Invalid category"))
            }
        }
        stored.map { PhraseDuplicateEngine.canonicalIdentity(it.customPhrase.orEmpty()) }
            .groupBy { it }
            .filter { it.key.isNotBlank() && it.value.size > 1 }
            .forEach { (phraseKey, _) ->
                add(CustomPhraseStorageAuditFinding(phraseKey, "Duplicate stored mapping"))
            }
        val catalogContext = GuidedCatalogContext(
            caregiverCustomPhrases = CustomPhraseEngine.toCatalogEntries(runtimeMappings.filter { it.isCustom })
        )
        stored.forEach { mapping ->
            val category = mapping.caregiverCategory ?: return@forEach
            val page = GuidedVocabularyCatalog.categoryAt(
                category.toGuidedCategory().ordinal,
                language,
                uiStrings,
                catalogContext
            )
            val visible = page?.entries?.any {
                PhraseDuplicateEngine.canonicalIdentity(it.phrase) ==
                    PhraseDuplicateEngine.canonicalIdentity(mapping.customPhrase.orEmpty())
            } == true
            if (!visible) {
                add(
                    CustomPhraseStorageAuditFinding(
                        identityKey(mapping),
                        "Phrase missing from runtime catalog"
                    )
                )
            }
        }
    }

    fun loadCustomMappings(context: Context? = null): List<WinkMapping> {
        testStorage?.let { store ->
            return CustomPhraseEngine.parseCustomMappings(store[KEY_CUSTOM_MAPS].orEmpty())
        }
        val prefs = prefs(context) ?: return emptyList()
        val raw = prefs.getString(KEY_CUSTOM_MAPS, "").orEmpty()
        return CustomPhraseEngine.parseCustomMappings(raw)
    }

    fun writeCustomMappings(custom: List<WinkMapping>, context: Context? = null): Boolean {
        val text = CustomPhraseEngine.serializeCustomMappings(custom)
        testStorage?.let { store ->
            store[KEY_CUSTOM_MAPS] = text
            return true
        }
        val prefs = prefs(context) ?: return false
        return prefs.edit().putString(KEY_CUSTOM_MAPS, text).commit()
    }

    /** Prefer applicationContext so Activity contexts are never retained. */
    private fun prefs(context: Context?): android.content.SharedPreferences? {
        val resolved = context?.applicationContext ?: context ?: return null
        return resolved.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun catalogLocationForMapping(
        mapping: WinkMapping,
        allMappings: List<WinkMapping>,
        language: PreferredLanguage,
        uiStrings: LisaUiStrings,
        visibleEntryCap: Int
    ): Pair<Int, Int> {
        val category = mapping.caregiverCategory ?: CustomPhraseEngine.CaregiverPhraseCategory.Conversation
        val catalogContext = GuidedCatalogContext(
            caregiverCustomPhrases = CustomPhraseEngine.toCatalogEntries(allMappings.filter { it.isCustom })
        )
        val page = GuidedVocabularyCatalog.categoryAt(
            category.toGuidedCategory().ordinal,
            language,
            uiStrings,
            catalogContext
        ) ?: return category.toGuidedCategory().ordinal to 0
        val index = page.entries.indexOfFirst { entry ->
            entry.left == mapping.left &&
                entry.right == mapping.right &&
                PhraseDuplicateEngine.canonicalIdentity(entry.phrase) ==
                PhraseDuplicateEngine.canonicalIdentity(mapping.customPhrase.orEmpty())
        }
        if (index < 0) return category.toGuidedCategory().ordinal to 0
        return category.toGuidedCategory().ordinal to (index / visibleEntryCap)
    }

    private fun persistAndVerify(
        updatedCustom: List<WinkMapping>,
        expectedMapping: WinkMapping,
        language: PreferredLanguage,
        uiStrings: LisaUiStrings,
        visibleEntryCap: Int,
        context: Context? = null
    ): PhraseSaveTransactionResult {
        if (!writeCustomMappings(updatedCustom, context)) {
            return PhraseSaveTransactionResult.Failed(PhraseSaveFailureReason.StorageWriteFailed)
        }
        // Verification must use the same Context/prefs file as the write (device failure otherwise).
        if (!verifyPersistedMapping(expectedMapping, context)) {
            return PhraseSaveTransactionResult.Failed(PhraseSaveFailureReason.StorageVerificationFailed)
        }
        val allMappings = defaultLanguageMappings() + updatedCustom
        val (categoryIndex, pageIndex) = catalogLocationForMapping(
            mapping = expectedMapping,
            allMappings = allMappings,
            language = language,
            uiStrings = uiStrings,
            visibleEntryCap = visibleEntryCap
        )
        val catalogContext = GuidedCatalogContext(
            caregiverCustomPhrases = CustomPhraseEngine.toCatalogEntries(updatedCustom)
        )
        val visible = GuidedVocabularyCatalog.categoryAt(
            categoryIndex,
            language,
            uiStrings,
            catalogContext
        )?.entries?.any {
            it.left == expectedMapping.left &&
                it.right == expectedMapping.right &&
                PhraseDuplicateEngine.canonicalIdentity(it.phrase) ==
                PhraseDuplicateEngine.canonicalIdentity(expectedMapping.customPhrase.orEmpty())
        } == true
        if (!visible) {
            return PhraseSaveTransactionResult.Failed(PhraseSaveFailureReason.RuntimeCatalogMissing)
        }
        return PhraseSaveTransactionResult.Success(
            mapping = expectedMapping,
            category = expectedMapping.caregiverCategory ?: CustomPhraseEngine.CaregiverPhraseCategory.Conversation,
            persisted = true,
            runtimeVisible = true,
            phrasePageIndex = pageIndex
        )
    }

    private fun persistUpdatedMapping(
        existingMappings: List<WinkMapping>,
        updated: WinkMapping,
        replacedIdentity: CustomPhraseIdentity,
        language: PreferredLanguage,
        uiStrings: LisaUiStrings,
        visibleEntryCap: Int,
        context: Context? = null
    ): PhraseManagementResult {
        val updatedCustom = existingMappings
            .filter { it.isCustom && !sameIdentity(it, replacedIdentity) } + updated
        val transaction = persistAndVerify(
            updatedCustom = updatedCustom,
            expectedMapping = updated,
            language = language,
            uiStrings = uiStrings,
            visibleEntryCap = visibleEntryCap,
            context = context
        )
        return when (transaction) {
            is PhraseSaveTransactionResult.Success -> PhraseManagementResult.Success(transaction.mapping)
            is PhraseSaveTransactionResult.Failed -> PhraseManagementResult.Failed(
                transaction.reason,
                transaction.duplicateMatch
            )
        }
    }

    private fun findCustom(mappings: List<WinkMapping>, identity: CustomPhraseIdentity): WinkMapping? =
        mappings.firstOrNull { it.isCustom && sameIdentity(it, identity) }

    private fun sameIdentity(mapping: WinkMapping, identity: CustomPhraseIdentity): Boolean =
        mapping.left == identity.left &&
            mapping.right == identity.right &&
            PhraseDuplicateEngine.canonicalIdentity(mapping.customPhrase.orEmpty()) ==
            PhraseDuplicateEngine.canonicalIdentity(identity.phrase)

    private fun identityKey(mapping: WinkMapping): String =
        "${mapping.left},${mapping.right}|${mapping.customPhrase.orEmpty()}"

    private fun CustomPhraseEngine.PhraseValidationFailure.toSaveFailureReason(): PhraseSaveFailureReason =
        when (this) {
            CustomPhraseEngine.PhraseValidationFailure.Empty -> PhraseSaveFailureReason.Empty
            CustomPhraseEngine.PhraseValidationFailure.TooLong -> PhraseSaveFailureReason.TooLong
            CustomPhraseEngine.PhraseValidationFailure.Duplicate -> PhraseSaveFailureReason.Duplicate
        }
}

data class CustomPhraseIdentity(
    val left: Int,
    val right: Int,
    val phrase: String
) {
    companion object {
        fun from(mapping: WinkMapping): CustomPhraseIdentity = CustomPhraseIdentity(
            left = mapping.left,
            right = mapping.right,
            phrase = mapping.customPhrase.orEmpty()
        )
    }
}

private fun WinkMapping.toIdentity(): CustomPhraseIdentity = CustomPhraseIdentity.from(this)
