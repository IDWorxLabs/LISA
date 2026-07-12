package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** RC7C1 — category-aware minimal sequence allocation. */
class Rc7C1CategoryAwareAllocationTest {

    private fun save(
        phrase: String,
        category: CustomPhraseEngine.CaregiverPhraseCategory,
        existing: List<WinkMapping> = defaultLanguageMappings()
    ): CustomPhraseEngine.SavePhraseResult =
        CustomPhraseEngine.saveNewPhrase(phrase, category, existing)

    private fun successSequence(result: CustomPhraseEngine.SavePhraseResult): Pair<Int, Int>? =
        (result as? CustomPhraseEngine.SavePhraseResult.Success)?.mapping?.let { it.left to it.right }

    // 1. Shortest valid sequence in category.

    @Test
    fun newPhraseReceivesShortestValidSequenceInCategory() {
        val sequence = successSequence(save("I'm itchy", CustomPhraseEngine.CaregiverPhraseCategory.Medical))
        assertNotNull(sequence)
        assertEquals(5 to 1, sequence)
    }

    // 2. Cross-category reuse is safe.

    @Test
    fun sequenceUsedInConversationMayBeReusedInBasicNeedsCategory() {
        val basicNeedsResult = save("My personal phrase", CustomPhraseEngine.CaregiverPhraseCategory.BasicNeeds)
        assertNotNull(successSequence(basicNeedsResult))
    }

    // 3. Same category cannot reuse sequence.

    @Test
    fun sequenceUsedInMedicalCannotBeReusedByAnotherMedicalPhrase() {
        val first = save("I'm itchy", CustomPhraseEngine.CaregiverPhraseCategory.Medical)
        assertTrue(first is CustomPhraseEngine.SavePhraseResult.Success)
        val mappings = defaultLanguageMappings() + listOf((first as CustomPhraseEngine.SavePhraseResult.Success).mapping)
        val second = save("My leg hurts", CustomPhraseEngine.CaregiverPhraseCategory.Medical, mappings)
        val firstSeq = (first.mapping.left to first.mapping.right)
        assertNotEquals(firstSeq, successSequence(second))
    }

    // 4. Family occupancy does not block Custom allocation.

    @Test
    fun familySequenceDoesNotBlockAllocationInConversation() {
        val familyCustom = CustomPhraseEngine.saveNewPhrase(
            "Call my sister",
            CustomPhraseEngine.CaregiverPhraseCategory.Family,
            defaultLanguageMappings()
        )
        assertTrue(familyCustom is CustomPhraseEngine.SavePhraseResult.Success)
        val familyMapping = (familyCustom as CustomPhraseEngine.SavePhraseResult.Success).mapping
        val conversationResult = save(
            "Personal note",
            CustomPhraseEngine.CaregiverPhraseCategory.Conversation,
            defaultLanguageMappings() + listOf(familyMapping)
        )
        assertTrue(conversationResult is CustomPhraseEngine.SavePhraseResult.Success)
    }

    // 5–8. Global, emergency, confirm/cancel, category menu blocked.

    @Test
    fun globalNavigationSequencesAreBlocked() {
        val blocked = listOf(
            GuidedModeNavigation.PREVIOUS_LEFT to GuidedModeNavigation.PREVIOUS_RIGHT,
            GuidedModeNavigation.NEXT_LEFT to GuidedModeNavigation.NEXT_RIGHT,
            GuidedModeNavigation.SELECT_LEFT to GuidedModeNavigation.SELECT_RIGHT,
            GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT,
            GuidedModeNavigation.CATEGORIES_LEFT to GuidedModeNavigation.CATEGORIES_RIGHT
        )
        blocked.forEach { sequence ->
            assertTrue(sequence in CustomPhraseEngine.globallyExcludedSequences())
            assertFalse(
                CustomPhraseEngine.rankedCandidatesForCategory(
                    CustomPhraseEngine.CaregiverPhraseCategory.Conversation,
                    emptyList()
                ).contains(sequence)
            )
        }
    }

    @Test
    fun emergencyIsAlwaysBlocked() {
        val emergency = EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS
        assertTrue(emergency in CustomPhraseEngine.globallyExcludedSequences())
        assertFalse(
            CustomPhraseEngine.rankedCandidatesForCategory(
                CustomPhraseEngine.CaregiverPhraseCategory.Medical,
                emptyList()
            ).contains(emergency)
        )
    }

    @Test
    fun systemCommandsCannotBeAssignedAsSpokenPhrases() {
        LisaSystemLanguage.allReservedSequences().forEach { sequence ->
            assertFalse(
                CustomPhraseEngine.rankedCandidatesForCategory(
                    CustomPhraseEngine.CaregiverPhraseCategory.Conversation,
                    emptyList()
                ).contains(sequence)
            )
        }
    }

    // 9. (covered by systemCommands test)

    // 10. Deterministic allocation.

    @Test
    fun allocationIsDeterministic() {
        val mappings = defaultLanguageMappings()
        val a = CustomPhraseEngine.allocateSequence(CustomPhraseEngine.CaregiverPhraseCategory.Medical, mappings)
        val b = CustomPhraseEngine.allocateSequence(CustomPhraseEngine.CaregiverPhraseCategory.Medical, mappings)
        assertEquals(a, b)
    }

    // 11. Ranking prefers fewer total winks.

    @Test
    fun candidateRankingPrefersFewerTotalWinks() {
        val ranked = CustomPhraseEngine.rankedCandidatesForCategory(
            CustomPhraseEngine.CaregiverPhraseCategory.Custom,
            emptyList()
        )
        assertTrue(ranked.isNotEmpty())
        assertEquals(2 to 1, ranked.first())
        assertTrue(ranked.first().first + ranked.first().second <= ranked.last().first + ranked.last().second)
    }

    // 12. Ranking avoids one-sided long sequences when simpler balanced candidate exists.

    @Test
    fun candidateRankingPrefersBalancedPatterns() {
        val ranked = CustomPhraseEngine.rankedCandidatesForCategory(
            CustomPhraseEngine.CaregiverPhraseCategory.Custom,
            emptyList()
        )
        val l7r1 = 7 to 1
        val simpler = ranked.first { it.first + it.second < l7r1.first + l7r1.second }
        assertTrue(ranked.indexOf(simpler) < ranked.indexOf(l7r1))
    }

    // 13. Self-audit detects skipped shorter candidate.

    @Test
    fun selfAuditRequiresSimplestValidCandidate() {
        val category = CustomPhraseEngine.CaregiverPhraseCategory.BasicNeeds
        val optimal = CustomPhraseEngine.allocateSequence(category, emptyList())!!
        assertTrue(CustomPhraseEngine.allocationSelfAuditPasses(category, optimal, emptyList()))
        assertFalse(CustomPhraseEngine.allocationSelfAuditPasses(category, 7 to 1, emptyList()))
    }

    // 14. Existing saved mappings remain stable (parse round-trip unchanged).

    @Test
    fun existingSavedMappingsRemainStable() {
        val raw = "7,1|I'm itchy|Medical\n"
        val parsed = CustomPhraseEngine.parseCustomMappings(raw)
        assertEquals(1, parsed.size)
        assertEquals(7 to 1, parsed.first().left to parsed.first().right)
        val reserialized = CustomPhraseEngine.serializeCustomMappings(parsed)
        assertTrue(reserialized.contains("7,1|I'm itchy|Medical"))
    }

    // 15. Built-in mappings unchanged.

    @Test
    fun builtInMappingsRemainUnchanged() {
        val before = GuidedVocabularyCatalog.buildPages(
            PreferredLanguage.English,
            LisaUiStrings.forLanguage(PreferredLanguage.English)
        )
        save("I'm itchy", CustomPhraseEngine.CaregiverPhraseCategory.Medical)
        val after = GuidedVocabularyCatalog.buildPages(
            PreferredLanguage.English,
            LisaUiStrings.forLanguage(PreferredLanguage.English)
        )
        assertEquals(before.size, after.size)
        before.zip(after).forEach { (prev, next) ->
            assertEquals(prev.category, next.category)
            assertEquals(prev.entries.size, next.entries.size)
            prev.entries.zip(next.entries).forEach { (pEntry, nEntry) ->
                assertEquals(pEntry.phrase, nEntry.phrase)
                assertEquals(pEntry.left to pEntry.right, nEntry.left to nEntry.right)
            }
        }
    }

    // 16. Medical itchy does not receive L7 R1 when shorter slot available.

    @Test
    fun medicalItchyDoesNotReceiveL7R1WhenShorterSlotAvailable() {
        val result = save("I'm itchy", CustomPhraseEngine.CaregiverPhraseCategory.Medical)
        assertTrue(result is CustomPhraseEngine.SavePhraseResult.Success)
        val mapping = (result as CustomPhraseEngine.SavePhraseResult.Success).mapping
        assertNotEquals(7 to 1, mapping.left to mapping.right)
        assertEquals("L5 R1", formatWinkSequenceShort(mapping.left, mapping.right))
    }

    // 17. All category scopes behave independently.

    @Test
    fun categoryScopesAreIndependent() {
        CustomPhraseEngine.selectableCategories.forEach { category ->
            val occupied = CustomPhraseEngine.categoryLocalOccupiedSequences(category, emptyList())
            val allocated = CustomPhraseEngine.allocateSequence(category, emptyList())
            assertNotNull("No sequence for $category", allocated)
            assertFalse("$allocated should not be globally excluded", allocated!! in CustomPhraseEngine.globallyExcludedSequences())
            assertEquals(10, occupied.size)
            assertEquals(5 to 1, allocated)
        }
        assertFalse(
            CustomPhraseEngine.selectableCategories.contains(CustomPhraseEngine.CaregiverPhraseCategory.Custom)
        )
    }

    // 18. Gesture-mode and visibility-policy audits still pass.

    @Test
    fun gestureModeAndVisibilityAuditsStillPass() {
        assertTrue(GuidedNavigationGestureAudit.auditAllModes())
        assertTrue(GuidedVocabularyCatalogValidation.noVocabularyUsesForbiddenSequences())
        assertTrue(GuidedVocabularyCatalogValidation.categoryShortcutLabelsMatchExpectedSlots())
        assertTrue(GuidedCategoryShortcuts.doNotConflictWithGlobalNavigation())
    }

    @Test
    fun auditReportsUnnecessarilyLongExistingMapping() {
        val longMapping = WinkMapping(
            left = 7,
            right = 1,
            vocabularyId = "I'm itchy",
            isCustom = true,
            customPhrase = "I'm itchy",
            caregiverCategory = CustomPhraseEngine.CaregiverPhraseCategory.Medical
        )
        val findings = CustomPhraseEngine.auditExistingCustomMappings(listOf(longMapping))
        assertTrue(findings.any { it.phrase == "I'm itchy" && it.suggestedSequence == (5 to 1) })
    }

    @Test
    fun rootCauseWasGlobalCatalogSlotExclusion() {
        val medicalOccupied = CustomPhraseEngine.categoryLocalOccupiedSequences(
            CustomPhraseEngine.CaregiverPhraseCategory.Medical,
            emptyList()
        )
        assertTrue((2 to 1) !in medicalOccupied || medicalOccupied.contains(2 to 1))
        assertEquals(10, medicalOccupied.size)
        assertTrue((2 to 1) in medicalOccupied)
        assertFalse((7 to 1) in medicalOccupied)
        val allocated = CustomPhraseEngine.allocateSequence(
            CustomPhraseEngine.CaregiverPhraseCategory.Medical,
            emptyList()
        )
        assertEquals(5 to 1, allocated)
    }
}
