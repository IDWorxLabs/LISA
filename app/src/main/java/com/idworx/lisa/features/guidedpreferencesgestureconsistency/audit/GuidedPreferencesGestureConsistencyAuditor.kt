package com.idworx.lisa.features.guidedpreferencesgestureconsistency.audit

import com.idworx.lisa.EMERGENCY_LEFT_WINKS
import com.idworx.lisa.EMERGENCY_RIGHT_WINKS
import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.GuidedNavigationController
import com.idworx.lisa.GuidedNavigationPanelSpec
import com.idworx.lisa.GuidedNavigationState
import com.idworx.lisa.GuidedOverlayScreenMode
import com.idworx.lisa.GuidedPreferencesAdjustMode
import com.idworx.lisa.GuidedSequenceResult
import com.idworx.lisa.LisaSystemLanguage
import com.idworx.lisa.LisaUiStrings
import com.idworx.lisa.PreferredLanguage
import com.idworx.lisa.SystemCommandAction
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import com.idworx.lisa.formatWinkSequenceShort

object GuidedPreferencesGestureConsistencyAuditor {

    private val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)

    /** Standalone quoted gesture-badge literal, e.g. "L4 R4" — the exact anti-pattern being removed. */
    private val hardcodedGestureLiteral = Regex("\"L-?\\d+ R-?\\d+\"")

    // --- 1. Every displayed Preferences gesture matches the executable gesture ---------------------
    fun preferencesGesturesMatchExecutableActions(): Boolean {
        val draftState = GuidedNavigationState(
            preferencesAdjustMode = GuidedPreferencesAdjustMode.ResponseTime,
            draftResponseTimeSec = 4
        )

        val decreaseResult = GuidedNavigationController.processSequence(
            GuidedModeNavigation.DECREASE_VALUE_LEFT, GuidedModeNavigation.DECREASE_VALUE_RIGHT,
            draftState, PreferredLanguage.English, uiStrings
        )
        val decreaseWorks = decreaseResult is GuidedSequenceResult.Navigate &&
            decreaseResult.newState.draftResponseTimeSec == 3

        val increaseResult = GuidedNavigationController.processSequence(
            GuidedModeNavigation.INCREASE_VALUE_LEFT, GuidedModeNavigation.INCREASE_VALUE_RIGHT,
            draftState, PreferredLanguage.English, uiStrings
        )
        val increaseWorks = increaseResult is GuidedSequenceResult.Navigate &&
            increaseResult.newState.draftResponseTimeSec == 5

        val saveResult = GuidedNavigationController.processSequence(
            GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT,
            draftState, PreferredLanguage.English, uiStrings
        )
        // RC7D.27 — first L1 R1 enters save confirmation; second L1 R1 persists.
        val saveEntersConfirmation = saveResult is GuidedSequenceResult.Navigate &&
            saveResult.newState.preferencesAdjustMode == GuidedPreferencesAdjustMode.ConfirmSaveResponseTime
        val confirmPersist = if (saveEntersConfirmation) {
            GuidedNavigationController.processSequence(
                GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT,
                (saveResult as GuidedSequenceResult.Navigate).newState,
                PreferredLanguage.English,
                uiStrings
            )
        } else {
            null
        }
        val saveWorks = confirmPersist is GuidedSequenceResult.SavePreferencesAdjustment

        val cancelResult = GuidedNavigationController.processSequence(
            GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT,
            draftState, PreferredLanguage.English, uiStrings
        )
        // RC7D.27 — Cancel / Back returns to Adjust Settings menu (not full exit).
        val cancelWorks = cancelResult is GuidedSequenceResult.Navigate &&
            cancelResult.newState.preferencesAdjustMode == GuidedPreferencesAdjustMode.SettingsMenu

        val categoriesResult = GuidedNavigationController.processSequence(
            GuidedModeNavigation.CATEGORIES_LEFT, GuidedModeNavigation.CATEGORIES_RIGHT,
            draftState, PreferredLanguage.English, uiStrings
        )
        val categoriesWorks = categoriesResult is GuidedSequenceResult.Navigate &&
            categoriesResult.newState.screenMode == GuidedOverlayScreenMode.CategoryMenu

        return decreaseWorks && increaseWorks && saveWorks && cancelWorks && categoriesWorks
    }

    // --- 2. No hardcoded gesture literals remain in the Preferences panel --------------------------
    fun noHardcodedGestureLiteralsRemainInPreferencesPanel(): Boolean {
        val body = preferencesAdjustmentPanelBody() ?: return false
        return hardcodedGestureLiteral.containsMatchIn(body).not()
    }

    // --- 3. Preferences gesture labels are derived from the shared gesture authority ---------------
    fun preferencesLabelsDeriveFromSharedGestureAuthority(): Boolean {
        val body = preferencesAdjustmentPanelBody() ?: return false
        val requiredConstantRefs = listOf(
            "GuidedModeNavigation.DECREASE_VALUE_LEFT",
            "GuidedModeNavigation.DECREASE_VALUE_RIGHT",
            "GuidedModeNavigation.INCREASE_VALUE_LEFT",
            "GuidedModeNavigation.INCREASE_VALUE_RIGHT",
            "GuidedModeNavigation.SELECT_LEFT",
            "GuidedModeNavigation.SELECT_RIGHT",
            "GuidedModeNavigation.BACK_LEFT",
            "GuidedModeNavigation.BACK_RIGHT",
            "EMERGENCY_LEFT_WINKS",
            "EMERGENCY_RIGHT_WINKS"
        )
        val allConstantsReferenced = requiredConstantRefs.all { body.contains(it) }
        // RC7D.27 — Categories card removed from the adjustment content; right-panel Categories remains.
        // At least Decrease, Increase, Save, Cancel, Emergency formatWinkSequenceShort calls.
        val formatCallCount = Regex("formatWinkSequenceShort\\(").findAll(body).count()
        return allConstantsReferenced && formatCallCount >= 5
    }

    // --- 4. Changing a gesture definition automatically changes the displayed label ------------------
    fun preferencesLabelIsAPureFunctionOfTheSharedConstant(): Boolean {
        val panelActions = GuidedNavigationPanelSpec.panelActions(uiStrings, GuidedNavigationPanelSpec.PanelContext.Vocabulary)
        val preferencesEmergencyLabel = formatWinkSequenceShort(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS)
        val mainPanelEmergencyLabel = panelActions.first { it.symbol == "🚨" }.sequenceLabel
        // Categories remains on the right navigation panel (not duplicated in adjustment content).
        val categoriesAuthority = formatWinkSequenceShort(
            GuidedModeNavigation.CATEGORIES_LEFT,
            GuidedModeNavigation.CATEGORIES_RIGHT
        )
        val mainPanelCategoriesLabel = panelActions.first { it.title == uiStrings.guidedCategoriesNavTitle }.sequenceLabel
        return preferencesEmergencyLabel == mainPanelEmergencyLabel &&
            mainPanelCategoriesLabel == categoriesAuthority
    }

    // --- 5. Quick Controls gesture labels match the shared LisaSystemLanguage authority --------------
    fun quickControlsGestureLabelsMatchSharedAuthority(): Boolean {
        val actionsWithBadges = listOf(
            SystemCommandAction.DecreaseSensitivity,
            SystemCommandAction.IncreaseSensitivity,
            SystemCommandAction.RepeatLastPhrase,
            SystemCommandAction.TogglePauseListening,
            SystemCommandAction.OpenPracticeMode,
            SystemCommandAction.CloseQuickControls
        )
        val everyBadgeRoundTrips = actionsWithBadges.all { action ->
            val command = LisaSystemLanguage.quickControlCommands.first { it.action == action }
            val resolved = LisaSystemLanguage.resolveQuickControlCommand(command.left, command.right)
            resolved == action && command.sequenceLabel == formatWinkSequenceShort(command.left, command.right)
        }
        val overlayBody = quickControlsOverlayBody() ?: return false
        val overlayFreeOfLiterals = hardcodedGestureLiteral.containsMatchIn(overlayBody).not()
        val overlayUsesSharedHelper = overlayBody.contains("quickControlGesture(SystemCommandAction.")
        return everyBadgeRoundTrips && overlayFreeOfLiterals && overlayUsesSharedHelper
    }

    // --- 6. Repository-wide production UI sweep for hardcoded gesture literals -----------------------
    fun productionUiFilesFreeOfHardcodedGestureLiterals(): Boolean {
        val productionUiFiles = listOf(
            "app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt",
            "app/src/main/java/com/idworx/lisa/LisaCommunicationAssistUi.kt",
            "app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt"
        )
        return productionUiFiles.all { path ->
            val content = ZeroTouchFileProbe.readProjectFile(path) ?: return false
            hardcodedGestureLiteral.containsMatchIn(content).not()
        }
    }

    // --- Infra: test class + gradle task --------------------------------------------------------------
    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/GuidedPreferencesGestureConsistencyAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaGuidedPreferencesGestureConsistencyV1")
    }

    private fun preferencesAdjustmentPanelBody(): String? {
        val file = ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt") ?: return null
        val start = file.indexOf("private fun PreferencesAdjustmentPanel(")
        if (start < 0) return null
        val end = file.indexOf("\n@Composable", start)
        return if (end > start) file.substring(start, end) else file.substring(start)
    }

    private fun quickControlsOverlayBody(): String? {
        val file = ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/LisaCommunicationAssistUi.kt") ?: return null
        val start = file.indexOf("fun QuickControlsOverlay(")
        if (start < 0) return null
        val end = file.indexOf("\n@Composable", start)
        return if (end > start) file.substring(start, end) else file.substring(start)
    }
}
