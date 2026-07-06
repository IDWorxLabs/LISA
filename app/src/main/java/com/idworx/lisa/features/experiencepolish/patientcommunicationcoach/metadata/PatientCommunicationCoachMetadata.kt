package com.idworx.lisa.features.experiencepolish.patientcommunicationcoach.metadata

object PatientCommunicationCoachMetadata {

    const val PASS_TOKEN: String = "LISA_PATIENT_COMMUNICATION_COACH_V1_PASS"

    const val COACHING_PHILOSOPHY: String =
        "Lisa teaches like a patient human coach: one phrase at a time, gradual gestures, " +
            "repeat when needed, celebrate warmly, slow down on struggle, rest on fatigue."

    const val PACING_RULE: String =
        "Base pacing delay ${PatientCommunicationCoachEngineRef.BASE_MS}ms, scaled by struggle multiplier."

    const val GESTURE_PROGRESSION_RULE: String =
        "Adjacent lessons may increase difficulty by at most ${PatientCommunicationCoachEngineRef.MAX_JUMP} level."

    private object PatientCommunicationCoachEngineRef {
        const val BASE_MS: Long = 1_200L
        const val MAX_JUMP: Int = 1
    }
}
