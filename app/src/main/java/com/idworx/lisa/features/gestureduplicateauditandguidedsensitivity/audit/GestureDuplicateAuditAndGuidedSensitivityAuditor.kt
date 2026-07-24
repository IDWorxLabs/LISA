package com.idworx.lisa.features.gestureduplicateauditandguidedsensitivity.audit

import com.idworx.lisa.features.gesturesequenceaudit.GestureDuplicateClass
import com.idworx.lisa.features.gesturesequenceaudit.GestureSequenceAuditEngine
import com.idworx.lisa.features.gesturesequenceaudit.GestureSourceContext
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.metadata.TrainingMetadata
import com.idworx.lisa.features.silentwelcome.LisaSpeechPolicy
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe

object GestureDuplicateAuditAndGuidedSensitivityAuditor {

    fun auditEngineScansAllCatalogs(): Boolean {
        val report = GestureSequenceAuditEngine.auditAll()
        val contexts = report.entries.map { it.context }.toSet()
        return contexts.contains(GestureSourceContext.GUIDED_LEARNING_ESSENTIAL) &&
            contexts.contains(GestureSourceContext.WORKSPACE_DEFAULT) &&
            contexts.contains(GestureSourceContext.SYSTEM_COMMAND) &&
            contexts.contains(GestureSourceContext.GLOBAL_NAVIGATION) &&
            report.entries.size >= 40
    }

    fun guidedEssentialsHaveUniqueSequences(): Boolean =
        GestureSequenceAuditEngine.guidedLearningEssentialsHaveUniqueSequences()

    fun noAndPleaseAreDistinct(): Boolean {
        val no = TrainingLessonCatalog.communicationFundamentals.first { it.vocabularyId == "no" }
        val please = TrainingLessonCatalog.communicationFundamentals.first { it.vocabularyId == "please" }
        return no.left != please.left || no.right != please.right
    }

    fun noInvalidEssentialDuplicates(): Boolean {
        val report = GestureSequenceAuditEngine.auditAll()
        return report.invalidDuplicates.none { finding ->
            finding.entries.any { it.context == GestureSourceContext.GUIDED_LEARNING_ESSENTIAL }
        }
    }

    fun validContextualReuseDocumented(): Boolean {
        val report = GestureSequenceAuditEngine.auditAll()
        return report.validReuses.isNotEmpty() &&
            report.validReuses.any { it.note.contains("isolation") || it.note.contains("resolver") }
    }

    fun reservedConflictsDetected(): Boolean {
        val report = GestureSequenceAuditEngine.auditAll()
        return report.reservedConflicts.isNotEmpty()
    }

    fun workspaceVisibleOnlyResolverIntact(): Boolean {
        val main = readMainActivity() ?: return false
        return main.contains("findMatchOnVisiblePage") ||
            ZeroTouchFileProbe.fileExists(
                "app/src/main/java/com/idworx/lisa/LisaGuidedMode.kt"
            ) && readGuidedMode()?.contains("findMatchOnVisiblePage") == true
    }

    fun setupScreenHasSensitivityControl(): Boolean {
        val setup = readSetupScreen() ?: return false
        return (setup.contains("UniversalEyeTrackingHeader") ||
            setup.contains("ExpandedEyeTrackingStatusPanel")) &&
            setup.contains("onDecreaseSensitivity") &&
            setup.contains("onIncreaseSensitivity")
    }

    fun lessonScreenHasSensitivityControl(): Boolean {
        val lessons = readLessonScreens() ?: return false
        val components = readTrainingComponents() ?: return false
        return lessons.contains("UniversalEyeTrackingHeader") &&
            lessons.contains("onDecreaseSensitivity") &&
            (lessons.contains("showSensitivityControls") ||
                components.contains("Sensitivity:") ||
                components.contains("uiStrings.sensitivity"))
    }

    fun sensitivityWiredToChangeSensitivity(): Boolean {
        val main = readMainActivity() ?: return false
        return main.contains("onTrainingReduceSensitivity = { changeSensitivity(-1) }") &&
            main.contains("onTrainingIncreaseSensitivity = { changeSensitivity(1) }") &&
            main.contains("blinkProcessor.tuning")
    }

    fun sensitivityDoesNotResetLesson(): Boolean {
        val main = readMainActivity() ?: return false
        val changeBlock = main.indexOf("private fun changeSensitivity")
        if (changeBlock < 0) return false
        val block = main.substring(changeBlock, changeBlock + 400)
        return block.contains("blinkProcessor.resetGestureFlags()") &&
            !block.contains("dispatch(TrainingEvent") &&
            !block.contains("communicationLessonIndex")
    }

    fun phraseSpeechOnly(): Boolean =
        LisaSpeechPolicy.PHRASE_TRANSLATION_ONLY && !LisaSpeechPolicy.allowsNarration()

    fun essentialMappingMatchesSpec(): Boolean {
        val expected = mapOf(
            "hello" to (2 to 0),
            "yes" to (0 to 2),
            "no" to (1 to 1),
            "please" to (1 to 2),
            "thank_you" to (3 to 1),
            "i_need_water" to (1 to 3)
        )
        return expected.all { (id, seq) ->
            val lesson = TrainingLessonCatalog.communicationFundamentals.firstOrNull { it.vocabularyId == id }
            lesson != null && (lesson.left to lesson.right) == seq
        }
    }

    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/GestureDuplicateAuditAndGuidedSensitivityAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaGestureDuplicateAuditAndGuidedSensitivityV1")
    }

    private fun readMainActivity(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/MainActivity.kt"
    )

    private fun readSetupScreen(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingSetupScreen.kt"
    )

    private fun readLessonScreens(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingLessonScreens.kt"
    )

    private fun readTrainingComponents(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingComponents.kt"
    )

    private fun readGuidedMode(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/LisaGuidedMode.kt"
    )
}
