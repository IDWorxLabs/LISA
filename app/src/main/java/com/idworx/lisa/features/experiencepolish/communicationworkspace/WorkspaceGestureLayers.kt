package com.idworx.lisa.features.experiencepolish.communicationworkspace

import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.features.brain1interactionstandard.model.UniversalInteractionGestures
import com.idworx.lisa.features.experiencepolish.communicationworkspace.metadata.CommunicationWorkspaceMetadata

/**
 * Documents separation between Brain 1 decision gestures and workspace navigation gestures.
 * L1 R1 means Confirm in Brain 1 decisions and Select in workspace navigation — context resolves meaning.
 */
object WorkspaceGestureLayers {

    fun isWorkspaceNavigationGesture(left: Int, right: Int): Boolean =
        GuidedModeNavigation.isGlobalNavigationSequence(left, right) ||
            GuidedModeNavigation.isDecreaseValueSequence(left, right) ||
            GuidedModeNavigation.isIncreaseValueSequence(left, right)

    fun isBrain1DecisionGesture(left: Int, right: Int): Boolean =
        UniversalInteractionGestures.isOptionA(left, right) ||
            UniversalInteractionGestures.isOptionB(left, right)

    val layerDocumentation: String = CommunicationWorkspaceMetadata.GESTURE_LAYER_RULE

    val workspaceGestures: Map<String, String> = CommunicationWorkspaceMetadata.WORKSPACE_NAVIGATION_GESTURES
}
