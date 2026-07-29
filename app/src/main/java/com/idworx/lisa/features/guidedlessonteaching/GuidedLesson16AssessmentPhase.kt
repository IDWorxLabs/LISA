package com.idworx.lisa.features.guidedlessonteaching

import com.idworx.lisa.features.guidedmedicalcategoryjourney.GuidedMedicalCategoryJourneyAuthority

/**
 * RC8.34 — Explicit Lesson 16 teaching phases (indices 0–2) plus named lifecycle stages.
 *
 * Teaching phases (catalogue card / gesture authority):
 * - [Method1ScrollToMedical] → [Method1OpenSelectedMedical] → [Method2DirectOpenMedical]
 *
 * Lifecycle after Method 1 open succeeds (not separate teaching cards):
 * Method1Success (Well done while Medical remains open) → ResetForMethod2 → Method 2 card.
 * After Method 2: Method2Success → Completed → Lesson 17 via catalogue progression.
 *
 * Lesson 17 ([GuidedMedicalCategoryJourneyAuthority.ID_OPEN_MEDICAL]) still re-teaches direct
 * open in isolation; Lesson 16 remains one catalogue lesson with both methods.
 */
enum class GuidedLesson16AssessmentPhase {
    Method1ScrollToMedical,
    Method1OpenSelectedMedical,
    Method2DirectOpenMedical;

    val phaseIndex: Int get() = ordinal

    companion object {
        const val LIFECYCLE_METHOD1_SUCCESS: String = "Method1Success"
        const val LIFECYCLE_RESET_FOR_METHOD2: String = "ResetForMethod2"
        const val LIFECYCLE_METHOD2_SUCCESS: String = "Method2Success"
        const val LIFECYCLE_COMPLETED: String = "Completed"

        fun fromPhaseIndex(index: Int): GuidedLesson16AssessmentPhase? =
            entries.getOrNull(index)

        fun fromPhaseId(id: String): GuidedLesson16AssessmentPhase? = when (id) {
            GuidedMedicalCategoryJourneyAuthority.PHASE_ID_METHOD1_SCROLL ->
                Method1ScrollToMedical
            GuidedMedicalCategoryJourneyAuthority.PHASE_ID_METHOD1_OPEN ->
                Method1OpenSelectedMedical
            GuidedMedicalCategoryJourneyAuthority.PHASE_ID_METHOD2_DIRECT ->
                Method2DirectOpenMedical
            else -> null
        }

        /** @deprecated RC8.34 — use [Method1ScrollToMedical]. */
        val Part1ScrollToMedical: GuidedLesson16AssessmentPhase get() = Method1ScrollToMedical

        /** @deprecated RC8.34 — use [Method1OpenSelectedMedical]. */
        val Part1OpenSelectedMedical: GuidedLesson16AssessmentPhase get() = Method1OpenSelectedMedical
    }
}
