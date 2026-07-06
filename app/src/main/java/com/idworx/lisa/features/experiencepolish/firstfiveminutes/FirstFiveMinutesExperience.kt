package com.idworx.lisa.features.experiencepolish.firstfiveminutes

import com.idworx.lisa.features.personality.dialogue.DefaultDialogueCatalog

/**
 * Phase A — First Five Minutes experience polish (Personality Engine catalog).
 */
object FirstFiveMinutesExperience {

    const val PHASE_NAME: String = "LISA Experience Polish — Phase A: First Five Minutes V1"

    fun meetLisaDialogues(): List<String> = texts("phase_a_meet_lisa")

    fun gettingReadyDialogues(): List<String> = texts("phase_a_getting_ready")

    fun faceDetectedDialogues(): List<String> = texts("phase_a_face_detected")

    fun faceLostDialogues(): List<String> = texts("phase_a_face_lost")

    fun faceWaitingDialogues(): List<String> = texts("phase_a_face_waiting")

    fun calibrationIntroDialogues(): List<String> = texts("phase_a_cal_intro")

    fun calibrationDotDialogues(): List<String> = texts("phase_a_cal_dot")

    fun calibrationDotCapturedDialogues(): List<String> = texts("phase_a_cal_captured")

    fun calibrationStruggleDialogues(): List<String> = texts("phase_a_cal_struggle")

    fun calibrationExcellentDialogues(): List<String> = texts("phase_a_cal_excellent")

    fun calibrationAcceptableDialogues(): List<String> = texts("phase_a_cal_acceptable")

    fun calibrationPoorDialogues(): List<String> = texts("phase_a_cal_poor")

    fun firstHelloCelebrationDialogues(): List<String> = texts("phase_a_first_success")

    fun firstHelloEncouragementDialogues(): List<String> = texts("phase_a_first_encourage")

    fun nextPhraseIntroDialogues(): List<String> = texts("phase_a_next_phrase")

    fun gentleMissedBlinkDialogues(): List<String> = texts("phase_a_gentle_miss")

    fun patienceDialogues(): List<String> = texts("phase_a_patience")

    private fun texts(tag: String): List<String> =
        DefaultDialogueCatalog.all("en")
            .filter { it.contextTags.contains(tag) }
            .sortedBy { it.id }
            .map { it.text }
}
