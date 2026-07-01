package com.idworx.lisa

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.ui.theme.LisaBlue
import com.idworx.lisa.ui.theme.LisaBlueDark
import com.idworx.lisa.ui.theme.LisaEmergencyRed
import com.idworx.lisa.ui.theme.LisaGray
import com.idworx.lisa.ui.theme.LisaWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaregiverLinkingPanel(
    caregivers: List<LisaCaregiver>,
    activeProfileId: String,
    activeProfileName: String,
    onAddCaregiver: (LisaCaregiver) -> Unit,
    onUpdateCaregiver: (LisaCaregiver) -> Unit,
    onDeleteCaregiver: (String) -> Unit,
    onBack: () -> Unit
) {
    var editingCaregiver by remember { mutableStateOf<LisaCaregiver?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newCaregiverName by remember { mutableStateOf("") }

    LisaPanelShell(title = "Caregiver Linking", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Linked to: $activeProfileName",
                fontSize = 12.sp,
                color = LisaBlueDark.copy(alpha = 0.7f)
            )

            if (caregivers.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LisaWhite)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No caregivers linked yet.",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LisaBlueDark
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Add someone who should be contacted in an emergency.",
                        fontSize = 13.sp,
                        color = LisaBlueDark.copy(alpha = 0.75f),
                        lineHeight = 18.sp
                    )
                }
            } else {
                caregivers.forEach { caregiver ->
                    CaregiverListRow(
                        caregiver = caregiver,
                        isSelected = editingCaregiver?.id == caregiver.id,
                        onClick = { editingCaregiver = caregiver }
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    newCaregiverName = ""
                    showAddDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add caregiver")
            }

            editingCaregiver?.let { caregiver ->
                HorizontalDivider(color = LisaBlue.copy(alpha = 0.25f))
                CaregiverEditForm(
                    caregiver = caregiver,
                    onSave = { updated ->
                        onUpdateCaregiver(updated.copy(updatedAt = System.currentTimeMillis()))
                        editingCaregiver = updated
                    },
                    onDelete = {
                        onDeleteCaregiver(caregiver.id)
                        editingCaregiver = null
                    },
                    canDelete = true
                )
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add caregiver") },
            text = {
                OutlinedTextField(
                    value = newCaregiverName,
                    onValueChange = { newCaregiverName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val caregiver = LisaCaregiver.createNew(activeProfileId, newCaregiverName)
                        onAddCaregiver(caregiver)
                        editingCaregiver = caregiver
                        showAddDialog = false
                    },
                    enabled = newCaregiverName.isNotBlank()
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CaregiverListRow(
    caregiver: LisaCaregiver,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) LisaBlue.copy(alpha = 0.18f) else LisaWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = caregiver.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = LisaBlueDark
            )
            Text(
                text = caregiver.relationship.label,
                fontSize = 12.sp,
                color = LisaGray
            )
            if (caregiver.shouldNotifyOnEmergency()) {
                Text(
                    text = "Emergency contact",
                    fontSize = 11.sp,
                    color = LisaBlue,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Text(text = "›", fontSize = 20.sp, color = LisaGray)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaregiverEditForm(
    caregiver: LisaCaregiver,
    onSave: (LisaCaregiver) -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    var draft by remember(caregiver.id) { mutableStateOf(caregiver) }
    var relationshipExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(caregiver) {
        draft = caregiver
    }

    Text(
        text = "Edit caregiver",
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        color = LisaBlueDark
    )

    OutlinedTextField(
        value = draft.name,
        onValueChange = { draft = draft.copy(name = it) },
        label = { Text("Name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    )

    Text("Relationship", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = LisaBlueDark)
    ExposedDropdownMenuBox(
        expanded = relationshipExpanded,
        onExpandedChange = { relationshipExpanded = it }
    ) {
        OutlinedTextField(
            value = draft.relationship.label,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = relationshipExpanded) },
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(
            expanded = relationshipExpanded,
            onDismissRequest = { relationshipExpanded = false }
        ) {
            CaregiverRelationship.entries.forEach { relationship ->
                DropdownMenuItem(
                    text = { Text(relationship.label) },
                    onClick = {
                        draft = draft.copy(relationship = relationship)
                        relationshipExpanded = false
                    }
                )
            }
        }
    }

    OutlinedTextField(
        value = draft.phoneNumber,
        onValueChange = { draft = draft.copy(phoneNumber = it) },
        label = { Text("Phone number") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    )

    OutlinedTextField(
        value = draft.email,
        onValueChange = { draft = draft.copy(email = it) },
        label = { Text("Email") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    )

    Text(
        text = "PERMISSIONS",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = LisaBlueDark.copy(alpha = 0.55f),
        letterSpacing = 0.5.sp
    )

    CaregiverPermissionToggle(
        title = "Receive emergency alerts",
        checked = draft.canReceiveEmergencyAlerts,
        onCheckedChange = { draft = draft.copy(canReceiveEmergencyAlerts = it) }
    )
    CaregiverPermissionToggle(
        title = "Edit vocabulary",
        checked = draft.canEditVocabulary,
        onCheckedChange = { draft = draft.copy(canEditVocabulary = it) }
    )
    CaregiverPermissionToggle(
        title = "Change settings",
        checked = draft.canChangeSettings,
        onCheckedChange = { draft = draft.copy(canChangeSettings = it) }
    )
    CaregiverPermissionToggle(
        title = "Emergency contact",
        checked = draft.emergencyContactEnabled,
        onCheckedChange = { draft = draft.copy(emergencyContactEnabled = it) }
    )

    Button(
        onClick = { onSave(draft) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = LisaBlue)
    ) {
        Text("Save caregiver")
    }

    if (canDelete) {
        OutlinedButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = LisaEmergencyRed)
        ) {
            Text("Delete caregiver")
        }
    }
}

@Composable
private fun CaregiverPermissionToggle(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LisaWhite)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = LisaBlueDark)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
