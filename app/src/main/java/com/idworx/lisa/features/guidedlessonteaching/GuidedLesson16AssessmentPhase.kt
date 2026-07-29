package com.idworx.lisa.features.guidedlessonteaching

import com.idworx.lisa.features.guidedmedicalcategoryjourney.GuidedMedicalCategoryJourneyAuthority

/**
 * RC8.25 — Explicit Lesson 16 assessment stages (scroll → open selected).
 * Direct Medical open is Lesson 17 ([GuidedMedicalCategoryJourneyAuthority.ID_OPEN_MEDICAL]).
 */
enum class GuidedLesson16AssessmentPhase {
    Part1ScrollToMedical,
    Part1OpenSelectedMedical;

    val phaseIndex: Int get() = ordinal

    companion object {
        fun fromPhaseIndex(index: Int): GuidedLesson16AssessmentPhase? =
            entries.getOrNull(index)

        fun fromPhaseId(id: String): GuidedLesson16AssessmentPhase? = when (id) {
            GuidedMedicalCategoryJourneyAuthority.PHASE_ID_PART1_SCROLL -> Part1ScrollToMedical
            GuidedMedicalCategoryJourneyAuthority.PHASE_ID_PART1_OPEN -> Part1OpenSelectedMedical
            else -> null
        }
    }
}
