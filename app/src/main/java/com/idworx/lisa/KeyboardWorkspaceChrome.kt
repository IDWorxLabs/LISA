package com.idworx.lisa

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.idworx.lisa.ui.theme.SharedKeyboardTheme

/**
 * RC8.2 — reusable keyboard-workspace chrome shared by Feedback and Phrase Management.
 * Visual standardization only; callers own behaviour and navigation.
 */

@Composable
fun KeyboardWorkspaceSurface(
    modifier: Modifier = Modifier,
    scrimmed: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = SharedKeyboardTheme
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (scrimmed) {
                    Modifier
                        .background(theme.OverlayScrim)
                        .padding(horizontal = theme.HorizontalInset, vertical = theme.VerticalInset)
                } else {
                    Modifier
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(theme.SurfaceCornerRadius))
                .background(theme.SurfaceBackground)
                .padding(theme.SurfaceContentPadding),
            content = content
        )
    }
}

/**
 * Neutral status strip: small ready indicator only — never a full-width green overlay.
 */
@Composable
fun KeyboardWorkspaceStatus(
    statusText: String,
    trackingReady: Boolean,
    modifier: Modifier = Modifier,
    secondaryContent: @Composable (ColumnScope.() -> Unit)? = null
) {
    val theme = SharedKeyboardTheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(theme.StatusCornerRadius))
            .background(theme.StatusBackground)
            .padding(
                horizontal = theme.StatusHorizontalPadding,
                vertical = theme.StatusVerticalPadding
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(theme.StatusIndicatorSize)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (trackingReady) theme.StatusReadyIndicator else theme.StatusIdleIndicator
                    )
            )
            Text(
                text = statusText,
                color = theme.StatusLabelColor,
                fontWeight = FontWeight.Medium,
                fontSize = theme.StatusLabelSize,
                maxLines = 2,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start
            )
        }
        secondaryContent?.invoke(this)
    }
}

@Composable
fun KeyboardWorkspaceInputCard(
    title: String?,
    body: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 3
) {
    val theme = SharedKeyboardTheme
    val display = body.ifBlank { placeholder }
    val isPlaceholder = body.isBlank()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(theme.InputCardCornerRadius))
            .background(theme.InputCardBackground)
            .padding(theme.InputCardPadding)
    ) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                fontSize = theme.InputTitleSize,
                fontWeight = FontWeight.Medium,
                color = theme.InputTitleColor
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Text(
            text = display,
            fontWeight = if (isPlaceholder) FontWeight.Medium else FontWeight.Bold,
            fontSize = theme.InputBodySize,
            color = if (isPlaceholder) theme.InputPlaceholderColor else theme.InputBodyColor,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun KeyboardWorkspaceOutlinedAction(
    title: String,
    sequenceLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    icon: String? = null
) {
    val theme = SharedKeyboardTheme
    val shape = RoundedCornerShape(theme.ActionCornerRadius)
    val borderColor = when {
        !enabled -> theme.ActionDisabledBorder
        selected -> theme.ActionSelectedBorder
        else -> theme.ActionBorder
    }
    val background = when {
        !enabled -> theme.ActionDisabledBackground
        selected -> theme.ActionSelectedBackground
        else -> theme.ActionBackground
    }
    val contentColor = if (enabled) theme.ActionContent else theme.ActionDisabledContent
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = theme.ActionMinHeight),
        shape = shape,
        border = BorderStroke(theme.ActionBorderWidth, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = background,
            contentColor = contentColor,
            disabledContainerColor = theme.ActionDisabledBackground,
            disabledContentColor = theme.ActionDisabledContent
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (icon != null) {
                Text(
                    text = icon,
                    fontSize = theme.ActionIconSize,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1
                )
            }
            Text(
                text = title,
                fontSize = theme.ActionTitleSize,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = sequenceLabel,
                fontSize = theme.ActionSequenceSize,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Compact outlined chip used for direction legends (Feedback reference). */
@Composable
fun KeyboardWorkspaceOutlinedChip(
    title: String,
    sequenceLabel: String,
    modifier: Modifier = Modifier
) {
    val theme = SharedKeyboardTheme
    val shape = RoundedCornerShape(theme.ActionCornerRadius)
    Column(
        modifier = modifier
            .clip(shape)
            .background(theme.ActionBackground)
            .border(theme.ActionBorderWidth, theme.ActionBorder, shape)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = theme.ActionContent,
            fontSize = theme.ActionTitleSize,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = sequenceLabel,
            color = theme.ActionContent,
            fontSize = theme.ActionSequenceSize,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun KeyboardWorkspaceOutlinedActionRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = SharedKeyboardTheme.TightSpacing),
        horizontalArrangement = Arrangement.spacedBy(SharedKeyboardTheme.ActionRowSpacing),
        content = content
    )
}

@Composable
fun KeyboardWorkspaceClickableActionCard(
    title: String,
    sequenceLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    icon: String? = null
) {
    val theme = SharedKeyboardTheme
    val shape = RoundedCornerShape(theme.ActionCornerRadius)
    val borderColor = when {
        !enabled -> theme.ActionDisabledBorder
        selected -> theme.ActionSelectedBorder
        else -> theme.ActionBorder
    }
    val background = when {
        !enabled -> theme.ActionDisabledBackground
        selected -> theme.ActionSelectedBackground
        else -> theme.ActionBackground
    }
    val contentColor = if (enabled) theme.ActionContent else theme.ActionDisabledContent
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = theme.ActionMinHeight)
            .clip(shape)
            .border(theme.ActionBorderWidth, borderColor, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .background(background)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Text(
                text = icon,
                fontWeight = FontWeight.Bold,
                fontSize = theme.ActionIconSize,
                color = contentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
        }
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            fontSize = theme.ActionTitleSize,
            color = contentColor,
            lineHeight = theme.ActionTitleSize * 1.15f,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = sequenceLabel,
            fontWeight = FontWeight.Bold,
            fontSize = theme.ActionSequenceSize,
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * RC8.3 / RC8.4 — bottom keyboard-workspace actions (Feedback Done/Back row).
 * Matches Phrase Management outlined action chrome.
 */
data class KeyboardWorkspaceBottomAction(
    val title: String,
    val sequenceLabel: String,
    val onClick: () -> Unit
)

@Composable
fun KeyboardWorkspaceBottomActionRow(
    actions: List<KeyboardWorkspaceBottomAction>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SharedKeyboardTheme.ActionRowSpacing)
    ) {
        for (action in actions) {
            KeyboardWorkspaceClickableActionCard(
                title = action.title,
                sequenceLabel = action.sequenceLabel,
                onClick = action.onClick,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = SharedKeyboardTheme.ActionMinHeight),
                icon = null
            )
        }
    }
}

/** Single full-width Done control — prefer [KeyboardWorkspaceBottomActionRow] for Done|Back. */
@Composable
fun KeyboardWorkspaceBottomDoneButton(
    title: String,
    sequenceLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    KeyboardWorkspaceBottomActionRow(
        actions = listOf(
            KeyboardWorkspaceBottomAction(
                title = title,
                sequenceLabel = sequenceLabel,
                onClick = onClick
            )
        ),
        modifier = modifier
    )
}
