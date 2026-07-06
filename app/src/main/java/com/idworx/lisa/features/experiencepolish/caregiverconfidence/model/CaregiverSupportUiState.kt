package com.idworx.lisa.features.experiencepolish.caregiverconfidence.model

import com.idworx.lisa.features.personality.model.CaregiverSupportMoment

data class CaregiverSupportUiState(
    val primaryHint: String? = null,
    val whatToDoNow: String? = null,
    val progressLine: String? = null,
    val moment: CaregiverSupportMoment? = null
)
