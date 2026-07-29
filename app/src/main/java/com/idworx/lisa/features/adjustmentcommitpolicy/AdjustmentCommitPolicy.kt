package com.idworx.lisa.features.adjustmentcommitpolicy

import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.GuidedPreferencesAdjustMode
import com.idworx.lisa.GuidedSequenceResult
import com.idworx.lisa.MAX_SENSITIVITY_LEVEL
import com.idworx.lisa.MIN_SENSITIVITY_LEVEL
import com.idworx.lisa.SequenceProcessingDelay
import com.idworx.lisa.SpeechSpeedAuthority
import com.idworx.lisa.SpeechVolumeAuthority
import com.idworx.lisa.formatWinkSequenceShort

/**
 * RC8.30 — Shared production commit policy for Settings & Controls numeric adjustments.
 *
 * All four adjustable settings use [IMMEDIATE_SAVE]:
 * each valid Increase / Decrease updates the visible value and persists immediately,
 * remaining on the adjustment screen. There is no separate Save or confirmation step.
 */
enum class AdjustmentCommitPolicy {
    IMMEDIATE_SAVE
}

enum class AdjustableSettingKind {
    Sensitivity,
    ResponseTime,
    SpeechVolume,
    SpeechSpeed
}

enum class AdjustmentDirection {
    Increase,
    Decrease
}

/**
 * Production evidence for one adjustment attempt (blink or touch).
 */
data class AdjustmentCommitResult(
    val setting: AdjustableSettingKind,
    val previousValue: Int,
    val resultingValue: Int,
    val direction: AdjustmentDirection,
    val sequenceLabel: String,
    val persistedValue: Int?,
    val executionId: String,
    val boundaryPreventedChange: Boolean,
    val policy: AdjustmentCommitPolicy = AdjustmentCommitPolicy.IMMEDIATE_SAVE
) {
    val valueChanged: Boolean get() = !boundaryPreventedChange && previousValue != resultingValue
    val didPersist: Boolean get() = persistedValue != null && valueChanged
}

object AdjustmentCommitPolicyAuthority {

    const val HELPER_CHANGES_SAVE_AUTOMATICALLY: String = "Changes save automatically."

    fun policyFor(mode: GuidedPreferencesAdjustMode): AdjustmentCommitPolicy? =
        when (mode) {
            GuidedPreferencesAdjustMode.Sensitivity,
            GuidedPreferencesAdjustMode.ResponseTime,
            GuidedPreferencesAdjustMode.SpeechVolume,
            GuidedPreferencesAdjustMode.SpeechSpeed -> AdjustmentCommitPolicy.IMMEDIATE_SAVE
            else -> null
        }

    fun usesImmediateSave(mode: GuidedPreferencesAdjustMode): Boolean =
        policyFor(mode) == AdjustmentCommitPolicy.IMMEDIATE_SAVE

    fun settingKindFor(mode: GuidedPreferencesAdjustMode): AdjustableSettingKind? =
        when (mode) {
            GuidedPreferencesAdjustMode.Sensitivity -> AdjustableSettingKind.Sensitivity
            GuidedPreferencesAdjustMode.ResponseTime -> AdjustableSettingKind.ResponseTime
            GuidedPreferencesAdjustMode.SpeechVolume -> AdjustableSettingKind.SpeechVolume
            GuidedPreferencesAdjustMode.SpeechSpeed -> AdjustableSettingKind.SpeechSpeed
            else -> null
        }

    fun allAdjustableSettingsUseImmediateSave(): Boolean =
        listOf(
            GuidedPreferencesAdjustMode.Sensitivity,
            GuidedPreferencesAdjustMode.ResponseTime,
            GuidedPreferencesAdjustMode.SpeechVolume,
            GuidedPreferencesAdjustMode.SpeechSpeed
        ).all { usesImmediateSave(it) }

    fun increaseSequenceLabel(): String = formatWinkSequenceShort(
        GuidedModeNavigation.INCREASE_VALUE_LEFT,
        GuidedModeNavigation.INCREASE_VALUE_RIGHT
    )

    fun decreaseSequenceLabel(): String = formatWinkSequenceShort(
        GuidedModeNavigation.DECREASE_VALUE_LEFT,
        GuidedModeNavigation.DECREASE_VALUE_RIGHT
    )

    fun backSequenceLabel(): String = formatWinkSequenceShort(
        GuidedModeNavigation.BACK_LEFT,
        GuidedModeNavigation.BACK_RIGHT
    )

    fun nextExecutionId(): String =
        "adj_${System.nanoTime()}"

    fun coerceValue(setting: AdjustableSettingKind, value: Int): Int = when (setting) {
        AdjustableSettingKind.Sensitivity ->
            value.coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL)
        AdjustableSettingKind.ResponseTime ->
            SequenceProcessingDelay.coerce(value)
        AdjustableSettingKind.SpeechVolume ->
            SpeechVolumeAuthority.coerce(value)
        AdjustableSettingKind.SpeechSpeed ->
            SpeechSpeedAuthority.coerce(value)
    }

    fun toSaveResult(
        newState: com.idworx.lisa.GuidedNavigationState,
        commit: AdjustmentCommitResult
    ): GuidedSequenceResult {
        if (!commit.didPersist) {
            return GuidedSequenceResult.Navigate(newState)
        }
        return when (commit.setting) {
            AdjustableSettingKind.Sensitivity -> GuidedSequenceResult.SavePreferencesAdjustment(
                newState = newState,
                sensitivityLevel = commit.persistedValue
            )
            AdjustableSettingKind.ResponseTime -> GuidedSequenceResult.SavePreferencesAdjustment(
                newState = newState,
                responseTimeSec = commit.persistedValue
            )
            AdjustableSettingKind.SpeechVolume -> GuidedSequenceResult.SavePreferencesAdjustment(
                newState = newState,
                speechVolumeLevel = commit.persistedValue
            )
            AdjustableSettingKind.SpeechSpeed -> GuidedSequenceResult.SavePreferencesAdjustment(
                newState = newState,
                speechSpeedLevel = commit.persistedValue
            )
        }
    }
}
