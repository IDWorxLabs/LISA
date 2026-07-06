package com.idworx.lisa.features.corecommunicationreliability.emergency

import com.idworx.lisa.isEmergencySequence

data class EmergencyConfirmationDecision(
    val allowRealEmergency: Boolean,
    val routeToTrainingVerification: Boolean,
    val requiresConfirmation: Boolean,
    val blockedReason: String? = null
)

object EmergencyConfirmationPolicy {

    fun decide(
        left: Int,
        right: Int,
        navigationTrainingActive: Boolean,
        communicationTrainingActive: Boolean
    ): EmergencyConfirmationDecision {
        if (!isEmergencySequence(left, right)) {
            return EmergencyConfirmationDecision(
                allowRealEmergency = false,
                routeToTrainingVerification = false,
                requiresConfirmation = false
            )
        }
        if (navigationTrainingActive) {
            return EmergencyConfirmationDecision(
                allowRealEmergency = false,
                routeToTrainingVerification = true,
                requiresConfirmation = true,
                blockedReason = null
            )
        }
        if (communicationTrainingActive) {
            return EmergencyConfirmationDecision(
                allowRealEmergency = false,
                routeToTrainingVerification = false,
                requiresConfirmation = true,
                blockedReason = "Emergency blocked during communication training"
            )
        }
        return EmergencyConfirmationDecision(
            allowRealEmergency = true,
            routeToTrainingVerification = false,
            requiresConfirmation = true
        )
    }
}

object EmergencySpeechSafetyGuard {

    private val emergencyDebouncer = com.idworx.lisa.features.corecommunicationreliability.sequence.BlinkSequenceDebouncer(
        debounceWindowMs = 1500L
    )

    fun evaluate(
        left: Int,
        right: Int,
        navigationTrainingActive: Boolean,
        communicationTrainingActive: Boolean,
        practiceMode: Boolean
    ): com.idworx.lisa.features.corecommunicationreliability.model.EmergencySafetyResult {
        val isEmergency = isEmergencySequence(left, right)
        if (!isEmergency) {
            return com.idworx.lisa.features.corecommunicationreliability.model.EmergencySafetyResult(
                allowed = false,
                isEmergencySequence = false,
                trainingMode = navigationTrainingActive || communicationTrainingActive,
                navigationTraining = navigationTrainingActive,
                requiresConfirmation = false
            )
        }
        val policy = EmergencyConfirmationPolicy.decide(
            left, right, navigationTrainingActive, communicationTrainingActive
        )
        val duplicate = !emergencyDebouncer.shouldAllow(left, right)
        val allowed = policy.allowRealEmergency && !duplicate && !practiceMode

        return com.idworx.lisa.features.corecommunicationreliability.model.EmergencySafetyResult(
            allowed = allowed,
            isEmergencySequence = true,
            trainingMode = navigationTrainingActive || communicationTrainingActive,
            navigationTraining = navigationTrainingActive,
            requiresConfirmation = policy.requiresConfirmation,
            blockedReason = when {
                duplicate -> "Emergency duplicate activation prevented"
                communicationTrainingActive -> "Emergency blocked during communication training"
                practiceMode -> "Emergency blocked during practice mode"
                !policy.allowRealEmergency && !policy.routeToTrainingVerification ->
                    "Emergency not allowed in current mode"
                else -> null
            }
        )
    }

    fun resetDebouncer() = emergencyDebouncer.reset()
}
