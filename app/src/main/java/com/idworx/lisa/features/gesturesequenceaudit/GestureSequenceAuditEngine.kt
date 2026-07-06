package com.idworx.lisa.features.gesturesequenceaudit

import com.idworx.lisa.EMERGENCY_LEFT_WINKS
import com.idworx.lisa.EMERGENCY_RIGHT_WINKS
import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.LisaSystemLanguage
import com.idworx.lisa.PracticeModeCatalog
import com.idworx.lisa.defaultLanguageMappings
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.metadata.TrainingMetadata

object GestureSequenceAuditEngine {

    fun auditAll(): GestureSequenceAuditReport {
        val entries = collectAllEntries()
        val findings = classifyDuplicates(entries)
        val essentials = essentialGuidedLearningEntries(entries)
        val essentialsUnique = essentials.map { it.sequenceKey }.distinct().size == essentials.size
        val noPleaseDistinct = essentials
            .filter { it.vocabularyId in setOf("no", "please") }
            .map { it.sequenceKey }
            .distinct()
            .size == 2
        return GestureSequenceAuditReport(
            entries = entries,
            findings = findings,
            guidedEssentialsUnique = essentialsUnique,
            noAndPleaseDistinct = noPleaseDistinct
        )
    }

    fun essentialGuidedLearningEntries(all: List<GestureSequenceEntry> = collectAllEntries()): List<GestureSequenceEntry> =
        all.filter { it.context == GestureSourceContext.GUIDED_LEARNING_ESSENTIAL }

    fun guidedLearningEssentialsHaveUniqueSequences(): Boolean =
        auditAll().guidedEssentialsUnique

    fun noAndPleaseAreDistinct(): Boolean = auditAll().noAndPleaseDistinct

    private fun collectAllEntries(): List<GestureSequenceEntry> = buildList {
        TrainingMetadata.GUIDED_LEARNING_ESSENTIAL_VOCABULARY_IDS.forEach { vocabId ->
            val lesson = TrainingLessonCatalog.communicationFundamentals
                .firstOrNull { it.vocabularyId == vocabId } ?: return@forEach
            add(
                GestureSequenceEntry(
                    source = "TrainingLessonCatalog.essential",
                    context = GestureSourceContext.GUIDED_LEARNING_ESSENTIAL,
                    left = lesson.left,
                    right = lesson.right,
                    blinkOrder = lesson.blinkOrder,
                    label = lesson.id,
                    vocabularyId = lesson.vocabularyId
                )
            )
        }
        TrainingLessonCatalog.communicationFundamentals.forEach { lesson ->
            add(
                GestureSequenceEntry(
                    source = "TrainingLessonCatalog.communicationFundamentals",
                    context = GestureSourceContext.GUIDED_LEARNING_FULL,
                    left = lesson.left,
                    right = lesson.right,
                    blinkOrder = lesson.blinkOrder,
                    label = lesson.id,
                    vocabularyId = lesson.vocabularyId
                )
            )
        }
        defaultLanguageMappings().forEach { mapping ->
            add(
                GestureSequenceEntry(
                    source = "LisaDefaultLanguage.defaultLanguageMappings",
                    context = GestureSourceContext.WORKSPACE_DEFAULT,
                    left = mapping.left,
                    right = mapping.right,
                    label = mapping.vocabularyId,
                    vocabularyId = mapping.vocabularyId
                )
            )
        }
        (LisaSystemLanguage.globalCommands + LisaSystemLanguage.quickControlCommands).forEach { cmd ->
            add(
                GestureSequenceEntry(
                    source = "LisaSystemLanguage.${cmd.labelKey}",
                    context = GestureSourceContext.SYSTEM_COMMAND,
                    left = cmd.left,
                    right = cmd.right,
                    label = cmd.labelKey
                )
            )
        }
        listOf(
            "PREVIOUS" to (GuidedModeNavigation.PREVIOUS_LEFT to GuidedModeNavigation.PREVIOUS_RIGHT),
            "NEXT" to (GuidedModeNavigation.NEXT_LEFT to GuidedModeNavigation.NEXT_RIGHT),
            "SELECT" to (GuidedModeNavigation.SELECT_LEFT to GuidedModeNavigation.SELECT_RIGHT),
            "BACK" to (GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT),
            "CATEGORIES" to (GuidedModeNavigation.CATEGORIES_LEFT to GuidedModeNavigation.CATEGORIES_RIGHT),
            "DECREASE_VALUE" to (GuidedModeNavigation.DECREASE_VALUE_LEFT to GuidedModeNavigation.DECREASE_VALUE_RIGHT),
            "INCREASE_VALUE" to (GuidedModeNavigation.INCREASE_VALUE_LEFT to GuidedModeNavigation.INCREASE_VALUE_RIGHT)
        ).forEach { (name, seq) ->
            add(
                GestureSequenceEntry(
                    source = "GuidedModeNavigation.$name",
                    context = GestureSourceContext.GLOBAL_NAVIGATION,
                    left = seq.first,
                    right = seq.second,
                    label = name
                )
            )
        }
        PracticeModeCatalog.items.forEach { item ->
            add(
                GestureSequenceEntry(
                    source = "PracticeModeCatalog",
                    context = GestureSourceContext.PRACTICE_MODE,
                    left = item.left,
                    right = item.right,
                    label = item.vocabularyId,
                    vocabularyId = item.vocabularyId
                )
            )
        }
        add(
            GestureSequenceEntry(
                source = "EmergencyArchitecture",
                context = GestureSourceContext.EMERGENCY,
                left = EMERGENCY_LEFT_WINKS,
                right = EMERGENCY_RIGHT_WINKS,
                label = "emergency"
            )
        )
        add(
            GestureSequenceEntry(
                source = "UniversalInteractionGestures.CONFIRM",
                context = GestureSourceContext.BRAIN1_DECISION,
                left = 1,
                right = 1,
                blinkOrder = "LR",
                label = "confirm_L1_R1"
            )
        )
        add(
            GestureSequenceEntry(
                source = "UniversalInteractionGestures.CANCEL",
                context = GestureSourceContext.BRAIN1_DECISION,
                left = 1,
                right = 1,
                blinkOrder = "RL",
                label = "cancel_R1_L1"
            )
        )
    }

    private fun classifyDuplicates(entries: List<GestureSequenceEntry>): List<GestureDuplicateFinding> {
        val findings = mutableListOf<GestureDuplicateFinding>()

        val essentials = entries.filter { it.context == GestureSourceContext.GUIDED_LEARNING_ESSENTIAL }
        essentials.groupBy { it.sequenceKey }.filter { it.value.size > 1 }.forEach { (key, group) ->
            findings.add(
                GestureDuplicateFinding(
                    classification = GestureDuplicateClass.INVALID_DUPLICATE,
                    sequenceKey = key,
                    entries = group,
                    note = "Guided Learning essential phrases must have unique L/R counts."
                )
            )
        }

        val guidedFull = entries.filter { it.context == GestureSourceContext.GUIDED_LEARNING_FULL }
        guidedFull.groupBy { it.sequenceKey }.filter { it.value.size > 1 }.forEach { (key, group) ->
            if (findings.any { it.sequenceKey == key && it.classification == GestureDuplicateClass.INVALID_DUPLICATE }) return@forEach
            findings.add(
                GestureDuplicateFinding(
                    classification = GestureDuplicateClass.INVALID_DUPLICATE,
                    sequenceKey = key,
                    entries = group,
                    note = "Duplicate sequence in full Guided Learning catalog."
                )
            )
        }

        val workspace = entries.filter { it.context == GestureSourceContext.WORKSPACE_DEFAULT }
        workspace.groupBy { it.sequenceKey }.filter { it.value.size > 1 }.forEach { (key, group) ->
            findings.add(
                GestureDuplicateFinding(
                    classification = GestureDuplicateClass.INVALID_DUPLICATE,
                    sequenceKey = key,
                    entries = group,
                    note = "Duplicate workspace default mapping."
                )
            )
        }

        essentials.forEach { essential ->
            val reserved = LisaSystemLanguage.isReservedSystemSequence(essential.left, essential.right)
            val navMatch = GuidedModeNavigation.isGlobalNavigationSequence(essential.left, essential.right)
            if (reserved || navMatch) {
                val related = entries.filter {
                    (it.context == GestureSourceContext.SYSTEM_COMMAND ||
                        it.context == GestureSourceContext.GLOBAL_NAVIGATION) &&
                        it.sequenceKey == essential.sequenceKey
                } + essential
                findings.add(
                    GestureDuplicateFinding(
                        classification = GestureDuplicateClass.RESERVED_CONFLICT,
                        sequenceKey = essential.sequenceKey,
                        entries = related.distinctBy { it.source + it.label },
                        note = if (navMatch) {
                            "Guided essential ${essential.vocabularyId} shares navigation gesture; isolated while training UI blocks workspace."
                        } else {
                            "Guided essential ${essential.vocabularyId} shares reserved system gesture; isolated during Guided Learning."
                        }
                    )
                )
            }
        }

        val contextualPairs = listOf(
            "L1 R1" to listOf(
                GestureSourceContext.BRAIN1_DECISION,
                GestureSourceContext.GLOBAL_NAVIGATION,
                GestureSourceContext.SYSTEM_COMMAND
            )
        )
        contextualPairs.forEach { (key, contexts) ->
            val related = entries.filter { it.sequenceKey == key && it.context in contexts }
            if (related.size >= 2 && findings.none { it.sequenceKey == key && it.classification == GestureDuplicateClass.VALID_CONTEXTUAL_REUSE }) {
                findings.add(
                    GestureDuplicateFinding(
                        classification = GestureDuplicateClass.VALID_CONTEXTUAL_REUSE,
                        sequenceKey = key,
                        entries = related,
                        note = "L1 R1 reused for confirm/select/repeat with order or visible-only resolver isolation."
                    )
                )
            }
        }

        return findings
    }
}
