package com.idworx.lisa.features.eyetrackingstatus

import com.idworx.lisa.ComposerEyeFeedback
import com.idworx.lisa.DEFAULT_SENSITIVITY_LEVEL
import com.idworx.lisa.EyeTrackingBannerContext
import com.idworx.lisa.LisaUiStrings
import com.idworx.lisa.SequenceProcessingDelay
import com.idworx.lisa.bannerMessage
import com.idworx.lisa.features.onboardingguide.ui.TrainingEyeTrackingState

/**
 * Stable UI projection of the single authoritative eye-tracking session.
 * Screens render this; they must not own a competing detector or counter.
 */
data class EyeTrackingStatusUiState(
    val trackingActive: Boolean = false,
    val cameraActive: Boolean = false,
    val faceDetected: Boolean = false,
    val eyesDetected: Boolean = false,
    val leftBlinkCount: Int = 0,
    val rightBlinkCount: Int = 0,
    val sensitivity: Int = DEFAULT_SENSITIVITY_LEVEL,
    val responseTimeSeconds: Int = SequenceProcessingDelay.DEFAULT_SECONDS,
    val calibrationInProgress: Boolean = false,
    val statusText: String = "",
    val controlsEnabled: Boolean = true
)

/**
 * Builds [EyeTrackingStatusUiState] only from existing session / banner / count sources.
 * Does not create a second tracking authority.
 */
object EyeTrackingStatusUiMapper {

    fun fromAuthoritative(
        uiStrings: LisaUiStrings,
        banner: EyeTrackingBannerContext,
        cameraActive: Boolean,
        leftBlinkCount: Int,
        rightBlinkCount: Int,
        sensitivity: Int,
        responseTimeSeconds: Int,
        calibrationInProgress: Boolean = banner.calibrationActive,
        controlsEnabled: Boolean = true
    ): EyeTrackingStatusUiState {
        val effectiveBanner = if (calibrationInProgress && !banner.calibrationActive) {
            banner.copy(calibrationActive = true)
        } else {
            banner
        }
        return EyeTrackingStatusUiState(
            trackingActive = cameraActive && effectiveBanner.faceDetected && effectiveBanner.eyesDetected,
            cameraActive = cameraActive,
            faceDetected = effectiveBanner.faceDetected,
            eyesDetected = effectiveBanner.eyesDetected,
            leftBlinkCount = leftBlinkCount.coerceAtLeast(0),
            rightBlinkCount = rightBlinkCount.coerceAtLeast(0),
            sensitivity = sensitivity,
            responseTimeSeconds = responseTimeSeconds,
            calibrationInProgress = calibrationInProgress || effectiveBanner.calibrationActive,
            statusText = effectiveBanner.bannerMessage(uiStrings),
            controlsEnabled = controlsEnabled
        )
    }

    fun fromComposerFeedback(
        uiStrings: LisaUiStrings,
        feedback: ComposerEyeFeedback,
        cameraActive: Boolean,
        calibrationInProgress: Boolean = feedback.eyeTrackingBanner.calibrationActive,
        controlsEnabled: Boolean = true
    ): EyeTrackingStatusUiState = fromAuthoritative(
        uiStrings = uiStrings,
        banner = feedback.eyeTrackingBanner,
        cameraActive = cameraActive,
        leftBlinkCount = feedback.leftWinkCount,
        rightBlinkCount = feedback.rightWinkCount,
        sensitivity = feedback.sensitivityLevel,
        responseTimeSeconds = feedback.responseTimeSec,
        calibrationInProgress = calibrationInProgress,
        controlsEnabled = controlsEnabled
    )

    fun fromTraining(
        uiStrings: LisaUiStrings,
        eyeTracking: TrainingEyeTrackingState,
        sensitivity: Int,
        responseTimeSeconds: Int,
        calibrationInProgress: Boolean = false,
        controlsEnabled: Boolean = true,
        leftBlinkCount: Int = eyeTracking.leftBlinkCount,
        rightBlinkCount: Int = eyeTracking.rightBlinkCount
    ): EyeTrackingStatusUiState = fromAuthoritative(
        uiStrings = uiStrings,
        banner = EyeTrackingBannerContext(
            calibrationActive = calibrationInProgress,
            trackingLost = false,
            faceDetected = eyeTracking.faceDetected,
            eyesDetected = eyeTracking.eyesDetected
        ),
        cameraActive = eyeTracking.cameraActive,
        leftBlinkCount = leftBlinkCount,
        rightBlinkCount = rightBlinkCount,
        sensitivity = sensitivity,
        responseTimeSeconds = responseTimeSeconds,
        calibrationInProgress = calibrationInProgress,
        controlsEnabled = controlsEnabled
    )

    fun toTrainingEyeTracking(state: EyeTrackingStatusUiState): TrainingEyeTrackingState =
        TrainingEyeTrackingState(
            cameraActive = state.cameraActive,
            faceDetected = state.faceDetected,
            eyesDetected = state.eyesDetected,
            leftBlinkCount = state.leftBlinkCount,
            rightBlinkCount = state.rightBlinkCount
        )
}
