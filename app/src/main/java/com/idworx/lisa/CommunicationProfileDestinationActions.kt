package com.idworx.lisa

/** Canonical visual-order catalog for the interactive Communication Profile controls. */
object CommunicationProfileDestinationActionAuthority {
    fun actions(
        profiles: List<LisaUserProfile>,
        activeProfileId: String,
        uiStrings: LisaUiStrings
    ): List<MenuDestinationAction> = buildList {
        val active = profiles.find { it.id == activeProfileId } ?: profiles.firstOrNull()
        active?.let {
            add(
                MenuDestinationAction(
                    MenuDestinationActionId.ProfileActive,
                    it.name,
                    MenuDestinationActionType.Choice,
                    selected = true,
                    sectionId = "active_profile"
                )
            )
        }
        add(
            MenuDestinationAction(
                MenuDestinationActionId.ProfileName,
                uiStrings.nameLabel,
                MenuDestinationActionType.TextField,
                sectionId = "profile_name"
            )
        )
        PreferredLanguage.selectable.forEach { language ->
            add(
                MenuDestinationAction(
                    MenuDestinationActionId.language(language.label),
                    language.label,
                    MenuDestinationActionType.Choice,
                    selected = active?.preferredLanguage == language,
                    sectionId = "language"
                )
            )
        }
        CommunicationLevel.entries.forEach { level ->
            add(
                MenuDestinationAction(
                    MenuDestinationActionId.communicationLevel(level.label),
                    level.label,
                    MenuDestinationActionType.Choice,
                    selected = active?.communicationLevel == level,
                    sectionId = "communication_level"
                )
            )
        }
        profiles.forEach { profile ->
            add(
                MenuDestinationAction(
                    MenuDestinationActionId.savedProfile(profile.id),
                    profile.name,
                    MenuDestinationActionType.Choice,
                    selected = profile.id == activeProfileId,
                    sectionId = "saved_profiles"
                )
            )
        }
        add(
            MenuDestinationAction(
                MenuDestinationActionId.ProfileNew,
                uiStrings.createNewProfile,
                MenuDestinationActionType.Navigation,
                sectionId = "saved_profiles"
            )
        )
        if (profiles.size > 1) {
            add(
                MenuDestinationAction(
                    MenuDestinationActionId.ProfileDelete,
                    uiStrings.deleteActiveProfile,
                    MenuDestinationActionType.Button,
                    sectionId = "saved_profiles"
                )
            )
        }
    }
}
