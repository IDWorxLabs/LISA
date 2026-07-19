package com.idworx.lisa

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.idworx.lisa.ui.theme.LisaBlue
import com.idworx.lisa.ui.theme.LisaBlueLight
import com.idworx.lisa.ui.theme.LisaWhite
import com.idworx.lisa.ui.theme.LisaWorkspaceVisualStyle

data class MenuDestinationSelectionVisualState(
    val selected: Boolean,
    val active: Boolean,
    val enabled: Boolean
)

object MenuDestinationSelectedSurfaceAuthority {
    fun visualState(
        actionId: MenuDestinationActionId,
        selectedActionId: MenuDestinationActionId?,
        active: Boolean,
        enabled: Boolean
    ) = MenuDestinationSelectionVisualState(
        selected = enabled && actionId == selectedActionId,
        active = active,
        enabled = enabled
    )

    fun background(state: MenuDestinationSelectionVisualState): Color = when {
        state.selected -> LisaWorkspaceVisualStyle.CardSelectedBackground
        state.active -> LisaBlueLight.copy(alpha = 0.35f)
        else -> LisaWhite
    }
}

/**
 * Canonical persistent navigation-cursor surface. [active] represents saved product state and is
 * intentionally independent from the eye-navigation [selected] state.
 */
@Composable
fun MenuDestinationSelectableSurface(
    actionId: MenuDestinationActionId,
    enabled: Boolean = true,
    active: Boolean = false,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val selectedActionId = LocalMenuDestinationSelectedAction.current
    val activate = LocalMenuDestinationActivateAction.current
    val visual = MenuDestinationSelectedSurfaceAuthority.visualState(
        actionId,
        selectedActionId,
        active,
        enabled
    )
    val shape = RoundedCornerShape(LisaWorkspaceVisualStyle.CardCornerRadius)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MenuDestinationSelectedSurfaceAuthority.background(visual))
            .then(
                if (visual.selected) {
                    Modifier.border(2.dp, LisaBlue, shape)
                } else {
                    Modifier
                }
            )
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick ?: { activate(actionId) }
            )
            .semantics {
                selected = visual.selected
                role = Role.Button
                stateDescription = when {
                    visual.selected && visual.active -> "Selected, active"
                    visual.selected -> "Selected"
                    visual.active -> "Active"
                    else -> "Not selected"
                }
                if (!enabled) disabled()
            }
            .padding(contentPadding),
        content = content
    )
}
