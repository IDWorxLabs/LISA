package com.idworx.lisa.features.glassessetup

/**
 * Production glasses setup preference — guidance only.
 * Does not alter Standard Mode thresholds or detection behaviour.
 */
object GlassesSetupAuthority {

    const val QUESTION_TITLE = "Do you normally wear glasses while using LISA?"
    const val QUESTION_SUPPORTING = "This helps LISA provide the correct setup guidance."
    const val ANSWER_YES = "Yes, I wear glasses"
    const val ANSWER_NO = "No, I do not wear glasses"

    const val GUIDANCE_TITLE = "Using LISA with glasses"
    const val GUIDANCE_INTRO =
        "LISA works best when the camera can clearly distinguish between open and closed eyes."
    const val GUIDANCE_RISK =
        "Some glasses may reduce eye-tracking accuracy because of reflections, lens coatings, or frame design."
    const val GUIDANCE_BEST_RESULTS_HEADER = "For the best possible results:"
    val GUIDANCE_BULLETS = listOf(
        "Use bright, even room lighting.",
        "Avoid strong reflections on the lenses.",
        "Keep the phone stable and directly in front of the user.",
        "Do not shine a light or the phone flash directly into the user’s eyes."
    )
    const val GUIDANCE_NOTICE =
        "Even under ideal conditions, some glasses may reduce eye-tracking reliability."
    const val GUIDANCE_CONTINUE = "Continue to Eye Tracking Setup"
    const val GUIDANCE_BACK = "Back"

    const val PREPARING_REMINDER =
        "For glasses users:\nUse bright, even lighting and avoid lens reflections."

    const val SETTINGS_TITLE = "Glasses used with LISA"
    const val SETTINGS_CHANGE = "Change answer"
    const val SETTINGS_VIEW_GUIDANCE = "View glasses guidance"
    const val SETTINGS_VALUE_YES = "Yes"
    const val SETTINGS_VALUE_NO = "No"
    const val SETTINGS_VALUE_UNSET = "Not answered"

    /** Ask once when the preference has never been answered. */
    fun requiresFirstLaunchQuestion(normallyUsesGlasses: Boolean?): Boolean =
        normallyUsesGlasses == null

    fun showPreparingReminder(normallyUsesGlasses: Boolean?): Boolean =
        normallyUsesGlasses == true

    fun statusLabel(normallyUsesGlasses: Boolean?): String = when (normallyUsesGlasses) {
        true -> SETTINGS_VALUE_YES
        false -> SETTINGS_VALUE_NO
        null -> SETTINGS_VALUE_UNSET
    }

    /**
     * Changing this preference must never be treated as a detection-mode or threshold change.
     * Callers persist guidance state only.
     */
    fun affectsEyeThresholds(): Boolean = false
}
