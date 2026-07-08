package com.idworx.lisa.features.onboardingguide.navigation

import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.LisaSystemLanguage
import com.idworx.lisa.OPEN_QUICK_CONTROLS_LEFT_WINKS
import com.idworx.lisa.OPEN_QUICK_CONTROLS_RIGHT_WINKS
import com.idworx.lisa.SystemCommandAction
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.isEmergencySequence

/**
 * Maps blink sequences to navigation lesson actions while Guided Learning navigation
 * training is active. Navigation lessons own gesture interpretation — the workspace
 * phrase resolver must not run in this context.
 */
object NavigationTrainingGestureHandler {

    fun resolveAction(left: Int, right: Int): NavigationAction? = when {
        isEmergencySequence(left, right) -> NavigationAction.TriggerEmergency
        GuidedModeNavigation.isCategoriesSequence(left, right) -> NavigationAction.OpenCategories
        GuidedModeNavigation.isSelectSequence(left, right) -> NavigationAction.SelectCategory
        GuidedModeNavigation.isBackSequence(left, right) -> NavigationAction.CloseMenu
        left == 1 && right == 1 -> NavigationAction.RepeatLastPhrase
        left == OPEN_QUICK_CONTROLS_LEFT_WINKS && right == OPEN_QUICK_CONTROLS_RIGHT_WINKS ->
            NavigationAction.OpenQuickControls
        LisaSystemLanguage.resolveQuickControlCommand(left, right) == SystemCommandAction.RepeatLastPhrase ->
            NavigationAction.RepeatLastPhrase
        else -> null
    }

    fun opensCategories(left: Int, right: Int): Boolean =
        GuidedModeNavigation.isCategoriesSequence(left, right)

    /** Navigation lessons always take priority over the workspace phrase resolver for the
     *  Categories gesture, regardless of which concrete gesture Categories is assigned and even
     *  if a future vocabulary change ever reintroduced a collision with a default-language
     *  phrase — see [com.idworx.lisa.features.gesturesequenceaudit.GestureSequenceAuditEngine]. */
    fun blocksWorkspacePhraseResolver(left: Int, right: Int): Boolean = opensCategories(left, right)
}
