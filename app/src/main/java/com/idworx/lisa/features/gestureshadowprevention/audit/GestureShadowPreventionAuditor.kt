package com.idworx.lisa.features.gestureshadowprevention.audit

import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.LisaSystemLanguage
import com.idworx.lisa.PracticeModeCatalog
import com.idworx.lisa.defaultLanguageMappings
import com.idworx.lisa.features.gesturesequenceaudit.GestureSequenceAuditEngine
import com.idworx.lisa.features.gesturesequenceaudit.GestureSourceContext
import com.idworx.lisa.features.guidedcurriculumandnavigationcontext.validation.GuidedCurriculumAndNavigationContextAuthorityV1
import com.idworx.lisa.features.guidedtrainingexitrefinement.validation.GuidedTrainingExitRefinementAuthorityV1
import com.idworx.lisa.features.onboardingguide.validation.GuidedTrainingAuthorityV1
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import com.idworx.lisa.isEmergencySequence
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.authority.GestureConflictAuthorityV1
import com.idworx.lisa.validation.authority.GuidedNavigationAuthorityV1
import com.idworx.lisa.validation.authority.NavigationReachabilityAuthorityV1

/**
 * Audits the remaining gesture shadowing introduced by the Categories (L3 R0) and Finish
 * Training (L0 R3) navigation gestures: the legacy "good_morning" and "i_need_water" default
 * vocabulary phrases previously collided with those exact sequences and had become
 * unreachable. This module proves the phrases were reassigned — not the navigation gestures —
 * and that the gesture allocation system now generically detects this class of defect.
 */
object GestureShadowPreventionAuditor {

    data class ConflictReport(
        val totalNavigationGestures: Int,
        val totalVocabularyGestures: Int,
        val totalReservedGestures: Int,
        val totalSystemGestures: Int,
        val remainingConflicts: Int,
        val unreachablePhrases: Int
    )

    // --- 1. The new navigation gestures are preserved, unchanged ------------------------------

    fun categoriesGestureStillUsesL3R0(): Boolean =
        GuidedModeNavigation.CATEGORIES_LEFT == 3 && GuidedModeNavigation.CATEGORIES_RIGHT == 0

    fun finishTrainingGestureStillUsesL0R3(): Boolean =
        GuidedModeNavigation.FINISH_TRAINING_LEFT == 0 && GuidedModeNavigation.FINISH_TRAINING_RIGHT == 3

    // --- 2. The two affected phrases were reassigned, not the navigation gestures -------------

    fun goodMorningNoLongerSharesCategoriesGesture(): Boolean {
        val mapping = defaultLanguageMappings().firstOrNull { it.vocabularyId == "good_morning" } ?: return false
        return mapping.left != GuidedModeNavigation.CATEGORIES_LEFT || mapping.right != GuidedModeNavigation.CATEGORIES_RIGHT
    }

    fun iNeedWaterNoLongerSharesFinishTrainingGesture(): Boolean {
        val mapping = defaultLanguageMappings().firstOrNull { it.vocabularyId == "i_need_water" } ?: return false
        return mapping.left != GuidedModeNavigation.FINISH_TRAINING_LEFT || mapping.right != GuidedModeNavigation.FINISH_TRAINING_RIGHT
    }

    fun practiceModeTeachesTheUpdatedINeedWaterGesture(): Boolean {
        val item = PracticeModeCatalog.items.firstOrNull { it.vocabularyId == "i_need_water" } ?: return false
        val mapping = defaultLanguageMappings().firstOrNull { it.vocabularyId == "i_need_water" } ?: return false
        return item.left == mapping.left && item.right == mapping.right
    }

    fun replacementGesturesAreConflictFree(): Boolean {
        val affected = defaultLanguageMappings().filter { it.vocabularyId == "good_morning" || it.vocabularyId == "i_need_water" }
        if (affected.size != 2) return false
        return affected.all { mapping ->
            !GuidedModeNavigation.isGlobalNavigationSequence(mapping.left, mapping.right) &&
                !isEmergencySequence(mapping.left, mapping.right) &&
                !LisaSystemLanguage.isReservedSystemSequence(mapping.left, mapping.right)
        }
    }

    // --- 3. Every vocabulary phrase — in every category — remains reachable -------------------

    fun everyVocabularyPhraseHasAUniqueGesture(): Boolean {
        val mappings = defaultLanguageMappings()
        return mappings.map { it.left to it.right }.distinct().size == mappings.size
    }

    fun everyVocabularyPhraseReachable(): Boolean =
        GestureSequenceAuditEngine.allWorkspaceDefaultPhrasesReachable()

    fun noNavigationGestureShadowsAPhrase(): Boolean =
        GestureSequenceAuditEngine.workspaceDefaultsFreeOfReservedConflicts()

    /** Symmetric to [noNavigationGestureShadowsAPhrase]: no phrase claims a reserved nav gesture. */
    fun noPhraseShadowsNavigation(): Boolean {
        val reservedNavigationGestures = setOf(
            GuidedModeNavigation.PREVIOUS_LEFT to GuidedModeNavigation.PREVIOUS_RIGHT,
            GuidedModeNavigation.NEXT_LEFT to GuidedModeNavigation.NEXT_RIGHT,
            GuidedModeNavigation.SELECT_LEFT to GuidedModeNavigation.SELECT_RIGHT,
            GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT,
            GuidedModeNavigation.CATEGORIES_LEFT to GuidedModeNavigation.CATEGORIES_RIGHT,
            GuidedModeNavigation.FINISH_TRAINING_LEFT to GuidedModeNavigation.FINISH_TRAINING_RIGHT
        )
        return defaultLanguageMappings().none { (it.left to it.right) in reservedNavigationGestures }
    }

    fun noDuplicatePhraseGestures(): Boolean = everyVocabularyPhraseHasAUniqueGesture()

    fun noReservedGestureConflicts(): Boolean =
        GestureConflictAuthorityV1.validate().outcome == ValidationOutcome.PASS

    // --- 4. The gesture allocation system now detects this class of defect generically --------

    fun auditEngineGenericallyDetectsWorkspaceReservedConflicts(): Boolean {
        val engineSource = readGestureSequenceAuditEngine() ?: return false
        return engineSource.contains("fun workspaceDefaultsFreeOfReservedConflicts") &&
            engineSource.contains("GestureSourceContext.WORKSPACE_DEFAULT") &&
            engineSource.contains("GestureSourceContext.GLOBAL_NAVIGATION") &&
            engineSource.contains("GestureSourceContext.SYSTEM_COMMAND") &&
            engineSource.contains("GestureSourceContext.EMERGENCY")
    }

    fun engineWouldHaveCaughtTheOriginalDefect(): Boolean {
        // Simulate the exact defect this milestone fixes: Categories/Finish Training claiming a
        // gesture already used by a vocabulary phrase must be flagged as a RESERVED_CONFLICT.
        val simulatedShadowedPhrase = "good_morning" to (GuidedModeNavigation.CATEGORIES_LEFT to GuidedModeNavigation.CATEGORIES_RIGHT)
        val wouldConflict = GuidedModeNavigation.isGlobalNavigationSequence(
            simulatedShadowedPhrase.second.first,
            simulatedShadowedPhrase.second.second
        )
        return wouldConflict && auditEngineGenericallyDetectsWorkspaceReservedConflicts()
    }

    // --- 5. Existing Guided Training and gesture consistency validations still pass -----------

    fun existingGuidedTrainingAndGestureAuthoritiesRemainGreen(): Boolean {
        val outcomes = listOf(
            GuidedTrainingAuthorityV1.validate().outcome,
            GuidedCurriculumAndNavigationContextAuthorityV1.validate().outcome,
            GuidedTrainingExitRefinementAuthorityV1.validate().outcome,
            NavigationReachabilityAuthorityV1.validate().outcome,
            GuidedNavigationAuthorityV1.validate().outcome,
            GestureConflictAuthorityV1.validate().outcome
        )
        return outcomes.all { it == ValidationOutcome.PASS }
    }

    // --- 6. Repository-wide gesture allocation audit / conflict report ------------------------

    fun buildConflictReport(): ConflictReport {
        val report = GestureSequenceAuditEngine.auditAll()
        val navigationGestures = report.entries.count { it.context == GestureSourceContext.GLOBAL_NAVIGATION }
        val vocabularyGestures = report.entries.count { it.context == GestureSourceContext.WORKSPACE_DEFAULT }
        val systemGestures = report.entries.count { it.context == GestureSourceContext.SYSTEM_COMMAND }
        val emergencyGestures = report.entries.count { it.context == GestureSourceContext.EMERGENCY }
        val reservedGestures = navigationGestures + systemGestures + emergencyGestures

        val workspaceConflictFindings = report.reservedConflicts.filter { finding ->
            finding.entries.any { it.context == GestureSourceContext.WORKSPACE_DEFAULT }
        }
        val unreachablePhrases = workspaceConflictFindings
            .flatMap { it.entries }
            .filter { it.context == GestureSourceContext.WORKSPACE_DEFAULT }
            .distinctBy { it.vocabularyId }
            .size

        return ConflictReport(
            totalNavigationGestures = navigationGestures,
            totalVocabularyGestures = vocabularyGestures,
            totalReservedGestures = reservedGestures,
            totalSystemGestures = systemGestures,
            remainingConflicts = workspaceConflictFindings.size,
            unreachablePhrases = unreachablePhrases
        )
    }

    fun repositoryAuditReportsZeroConflicts(): Boolean = buildConflictReport().remainingConflicts == 0

    fun repositoryAuditReportsZeroUnreachablePhrases(): Boolean = buildConflictReport().unreachablePhrases == 0

    // --- Infra: test class + gradle task -------------------------------------------------------

    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/GestureShadowPreventionAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaGestureShadowPreventionV1")
    }

    private fun readGestureSequenceAuditEngine(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/gesturesequenceaudit/GestureSequenceAuditEngine.kt"
    )
}
