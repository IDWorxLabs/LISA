package com.idworx.lisa.features.guidedlessonexecutionauthority.audit

import com.idworx.lisa.features.guidedlessonexecutionauthority.GuidedLessonExecutionAuthority
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe

/**
 * RC8.18 — source-level audit that Guided Learning does not pre-execute taught actions
 * and that lesson cards never scroll.
 */
object GuidedLessonExecutionAuditor {

    private fun read(pathUnderMainJava: String): String? =
        ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/$pathUnderMainJava")

    fun lessonCardHasNoInternalScroll(): Boolean {
        val card = read("features/onboardingguide/ui/TrainingComponents.kt")
            ?.substringAfter("fun GuidedWorkspaceLessonCard(")
            ?.substringBefore("fun GuidedLessonPhraseTitle(")
            ?: return false
        return !card.contains("verticalScroll") &&
            !card.contains("rememberScrollState") &&
            card.contains("wrapContentHeight()")
    }

    fun lessonCardUsesWorkspaceHeightFraction(): Boolean {
        val authority = read("features/guidedworkspacelessoncard/GuidedWorkspaceLessonCardAuthority.kt")
            ?: return false
        val execution = read("features/guidedlessonexecutionauthority/GuidedLessonExecutionAuthority.kt")
            ?: return false
        val ui = read("LisaAccessibilityUi.kt") ?: return false
        return authority.contains("MaxHeightFraction") &&
            execution.contains("LESSON_CARD_MAX_HEIGHT_FRACTION") &&
            execution.contains("0.45") &&
            ui.contains("BoxWithConstraints") &&
            (ui.contains("MaxHeightFraction") || ui.contains("lessonCardMaxHeight"))
    }

    fun workspaceBackLessonIdIsNavBack(): Boolean =
        GuidedLessonExecutionAuthority.ID_WORKSPACE_BACK == "nav_back"

    fun mainActivityGatesWorkspaceBackOnProductionState(): Boolean {
        val main = read("MainActivity.kt") ?: return false
        return main.contains("isWorkspaceBackCompleted") &&
            main.contains("isWorkspaceBackStartState") &&
            main.contains("ID_WORKSPACE_BACK") &&
            main.contains("verifyTrainingNavigation(NavigationAction.CloseMenu)")
    }

    fun mainActivityDoesNotAutoBackOnLesson19Entry(): Boolean {
        val main = read("MainActivity.kt") ?: return false
        if (!main.contains("ID_WORKSPACE_BACK")) return false
        val backPrep = main.substringAfter("execution.ID_WORKSPACE_BACK -> {")
            .substringBefore("else -> Unit")
        return backPrep.contains("isWorkspaceBackStartState") &&
            !backPrep.contains("openCategoryMenu(") &&
            !backPrep.contains("communicationWorkspaceRoot(") &&
            backPrep.contains("openCategoryDirectly")
    }

    fun authorityForbidsPreExecutingTaughtAction(): Boolean {
        val auth = read("features/guidedlessonexecutionauthority/GuidedLessonExecutionAuthority.kt")
            ?: return false
        return auth.contains("must never auto-open") ||
            auth.contains("never auto-open") ||
            auth.contains("must never execute the action the current lesson teaches")
    }
}
