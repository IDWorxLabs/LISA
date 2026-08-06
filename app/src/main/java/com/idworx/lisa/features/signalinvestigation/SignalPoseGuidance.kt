package com.idworx.lisa.features.signalinvestigation

/**
 * Pure pose-target and guidance authority for Signal Investigation.
 *
 * Coordinate convention (front-camera, user-facing):
 * - Raw ML Kit yaw/roll are image-relative.
 * - [userYaw] / [userRoll] apply a front-camera mirror so that:
 *   - positive userYaw  = user turns toward their RIGHT
 *   - positive userRoll = user tilts RIGHT ear toward RIGHT shoulder
 *   - negative userYaw  = user turns toward their LEFT
 *   - negative userRoll = user tilts LEFT ear toward LEFT shoulder
 *
 * Tilt uses roll only. Turn would use yaw (not in the seven-condition set).
 */
object SignalPoseGuidance {
    const val STRAIGHT_YAW_ABS: Float = 5f
    const val STRAIGHT_ROLL_ABS: Float = 5f
    const val TILT_MIN_ABS: Float = 10f
    const val TILT_MAX_ABS: Float = 15f
    const val STABLE_REQUIRED_MS: Long = 2_000L
    const val PREPARE_MS: Long = 4_000L
    const val OPEN_RECORD_MS: Long = 5_000L
    const val CLOSED_RECORD_MS: Long = 3_000L
    const val COMPLETE_MS: Long = 1_000L
    /** Pause between positions: Complete → Next Position (UX timing only). */
    const val POSITION_RESULT_MS: Long = 2_500L
    const val RECOVERY_MIN_MS: Long = 2_500L
    const val MAX_POSE_REJECT_PERCENT: Float = 35f
    /** Longer cooldown so voice stays sparse while visuals lead. */
    const val VOICE_COOLDOWN_MS: Long = 3_500L

    /** Vertical face-centre shift (percent of image height) for phone higher/lower. */
    const val PHONE_VERTICAL_OFFSET_PCT: Float = 8f
    /** Face-width relative change for closer/further. */
    const val DISTANCE_WIDTH_FRAC: Float = 0.18f
    const val DISTANCE_WIDTH_MIN_PP: Float = 4f

    enum class Band { TooLittle, InTarget, TooFar, Unknown }

    data class LivePose(
        val faceDetected: Boolean,
        val userYaw: Float?,
        val userRoll: Float?,
        val faceCenterXPct: Float?,
        val faceCenterYPct: Float?,
        val faceWidthPct: Float?,
        val leftOpen: Float?,
        val rightOpen: Float?
    )

    data class BaselinePose(
        val yaw: Float,
        val roll: Float,
        val faceCenterXPct: Float,
        val faceCenterYPct: Float,
        val faceWidthPct: Float
    )

    data class PoseTarget(
        val position: SignalPosition,
        val description: String,
        /** Inclusive range for the primary controlled metric. */
        val primaryLabel: String,
        val min: Float,
        val max: Float,
        val secondaryConstraints: List<SecondaryConstraint> = emptyList()
    )

    data class SecondaryConstraint(
        val label: String,
        val min: Float,
        val max: Float
    )

    data class GuidanceResult(
        val inTarget: Boolean,
        val band: Band,
        val correction: String,
        val holdPrompt: String,
        val voicePrompt: String
    )

    /** Front-camera mirror: invert image yaw so positive = user-right. */
    fun userYaw(rawYaw: Float): Float = -rawYaw

    /** Front-camera mirror: invert image roll so positive = user-right tilt. */
    fun userRoll(rawRoll: Float): Float = -rawRoll

    fun bothEyesOpen(left: Float?, right: Float?, openThr: Float = 0.70f): Boolean =
        left != null && right != null && left > openThr && right > openThr

    fun bothEyesClosed(left: Float?, right: Float?, closedThr: Float = 0.45f): Boolean =
        left != null && right != null && left < closedThr && right < closedThr

    fun targetsFor(position: SignalPosition, baseline: BaselinePose?): PoseTarget = when (position) {
        SignalPosition.HeadStraight -> PoseTarget(
            position = position,
            description = "Hold your head straight, looking at the phone.",
            primaryLabel = "yaw",
            min = -STRAIGHT_YAW_ABS,
            max = STRAIGHT_YAW_ABS,
            secondaryConstraints = listOf(
                SecondaryConstraint("roll", -STRAIGHT_ROLL_ABS, STRAIGHT_ROLL_ABS)
            )
        )
        SignalPosition.HeadTiltLeft -> PoseTarget(
            position = position,
            description = "Tilt your head slightly left — left ear toward left shoulder. Do not turn.",
            primaryLabel = "roll",
            min = -TILT_MAX_ABS,
            max = -TILT_MIN_ABS,
            secondaryConstraints = listOf(
                SecondaryConstraint("yaw", -STRAIGHT_YAW_ABS * 2f, STRAIGHT_YAW_ABS * 2f)
            )
        )
        SignalPosition.HeadTiltRight -> PoseTarget(
            position = position,
            description = "Tilt your head slightly right — right ear toward right shoulder. Do not turn.",
            primaryLabel = "roll",
            min = TILT_MIN_ABS,
            max = TILT_MAX_ABS,
            secondaryConstraints = listOf(
                SecondaryConstraint("yaw", -STRAIGHT_YAW_ABS * 2f, STRAIGHT_YAW_ABS * 2f)
            )
        )
        SignalPosition.PhoneHigher -> {
            val baseY = baseline?.faceCenterYPct ?: 50f
            // Raising the phone moves the face downward in the frame.
            val min = (baseY + PHONE_VERTICAL_OFFSET_PCT * 0.6f).coerceAtMost(85f)
            val max = (baseY + PHONE_VERTICAL_OFFSET_PCT * 1.6f).coerceAtMost(92f)
            PoseTarget(
                position = position,
                description = "Raise the phone slightly. Keep your head posture the same.",
                primaryLabel = "faceCenterY",
                min = min,
                max = maxOf(min + 1f, max),
                secondaryConstraints = listOf(
                    SecondaryConstraint("yaw", -STRAIGHT_YAW_ABS * 2f, STRAIGHT_YAW_ABS * 2f),
                    SecondaryConstraint("roll", -STRAIGHT_ROLL_ABS * 2f, STRAIGHT_ROLL_ABS * 2f)
                )
            )
        }
        SignalPosition.PhoneLower -> {
            val baseY = baseline?.faceCenterYPct ?: 50f
            val max = (baseY - PHONE_VERTICAL_OFFSET_PCT * 0.6f).coerceAtLeast(8f)
            val min = (baseY - PHONE_VERTICAL_OFFSET_PCT * 1.6f).coerceAtLeast(5f)
            PoseTarget(
                position = position,
                description = "Lower the phone slightly. Keep your head posture the same.",
                primaryLabel = "faceCenterY",
                min = min,
                max = maxOf(min + 1f, max),
                secondaryConstraints = listOf(
                    SecondaryConstraint("yaw", -STRAIGHT_YAW_ABS * 2f, STRAIGHT_YAW_ABS * 2f),
                    SecondaryConstraint("roll", -STRAIGHT_ROLL_ABS * 2f, STRAIGHT_ROLL_ABS * 2f)
                )
            )
        }
        SignalPosition.Closer -> {
            val baseW = baseline?.faceWidthPct ?: 28f
            val delta = maxOf(baseW * DISTANCE_WIDTH_FRAC, DISTANCE_WIDTH_MIN_PP)
            val min = (baseW + delta * 0.7f).coerceAtMost(55f)
            val max = (baseW + delta * 1.8f).coerceAtMost(65f)
            PoseTarget(
                position = position,
                description = "Move closer to the phone. Keep your head straight.",
                primaryLabel = "faceWidth",
                min = min,
                max = maxOf(min + 1f, max),
                secondaryConstraints = listOf(
                    SecondaryConstraint("yaw", -STRAIGHT_YAW_ABS * 2f, STRAIGHT_YAW_ABS * 2f),
                    SecondaryConstraint("roll", -STRAIGHT_ROLL_ABS * 2f, STRAIGHT_ROLL_ABS * 2f)
                )
            )
        }
        SignalPosition.Further -> {
            val baseW = baseline?.faceWidthPct ?: 28f
            val delta = maxOf(baseW * DISTANCE_WIDTH_FRAC, DISTANCE_WIDTH_MIN_PP)
            val max = (baseW - delta * 0.7f).coerceAtLeast(12f)
            val min = (baseW - delta * 1.8f).coerceAtLeast(8f)
            PoseTarget(
                position = position,
                description = "Move farther from the phone. Keep your head straight.",
                primaryLabel = "faceWidth",
                min = min,
                max = maxOf(min + 1f, max),
                secondaryConstraints = listOf(
                    SecondaryConstraint("yaw", -STRAIGHT_YAW_ABS * 2f, STRAIGHT_YAW_ABS * 2f),
                    SecondaryConstraint("roll", -STRAIGHT_ROLL_ABS * 2f, STRAIGHT_ROLL_ABS * 2f)
                )
            )
        }
    }

    fun primaryValue(target: PoseTarget, pose: LivePose): Float? = when (target.primaryLabel) {
        "yaw" -> pose.userYaw
        "roll" -> pose.userRoll
        "faceCenterY" -> pose.faceCenterYPct
        "faceWidth" -> pose.faceWidthPct
        else -> null
    }

    fun evaluate(target: PoseTarget, pose: LivePose): GuidanceResult {
        if (!pose.faceDetected) {
            return GuidanceResult(
                inTarget = false,
                band = Band.Unknown,
                correction = "Centre your face in the camera.",
                holdPrompt = "Waiting for face…",
                voicePrompt = "Centre your face in the camera."
            )
        }
        val primary = primaryValue(target, pose)
        if (primary == null) {
            return GuidanceResult(
                false, Band.Unknown, "Hold still while we measure your position.",
                "Measuring…", "Hold still."
            )
        }
        for (c in target.secondaryConstraints) {
            val v = when (c.label) {
                "yaw" -> pose.userYaw
                "roll" -> pose.userRoll
                "faceCenterY" -> pose.faceCenterYPct
                "faceWidth" -> pose.faceWidthPct
                else -> null
            } ?: continue
            if (v < c.min || v > c.max) {
                val corr = secondaryCorrection(c.label, v, c.min, c.max)
                return GuidanceResult(false, Band.TooFar, corr, corr, corr)
            }
        }
        val band = when {
            primary in target.min..target.max -> Band.InTarget
            target.position == SignalPosition.HeadTiltLeft ->
                if (primary > target.max) Band.TooLittle else Band.TooFar
            target.position == SignalPosition.HeadTiltRight ->
                if (primary < target.min) Band.TooLittle else Band.TooFar
            target.position == SignalPosition.Closer ->
                if (primary < target.min) Band.TooLittle else Band.TooFar
            target.position == SignalPosition.Further ->
                if (primary > target.max) Band.TooLittle else Band.TooFar
            target.position == SignalPosition.PhoneHigher ->
                if (primary < target.min) Band.TooLittle else Band.TooFar
            target.position == SignalPosition.PhoneLower ->
                if (primary > target.max) Band.TooLittle else Band.TooFar
            else -> when {
                primary < target.min -> Band.TooLittle
                primary > target.max -> Band.TooFar
                else -> Band.InTarget
            }
        }
        if (band == Band.InTarget) {
            return GuidanceResult(
                inTarget = true,
                band = Band.InTarget,
                correction = "Position correct",
                holdPrompt = "Hold still",
                voicePrompt = "Position correct. Hold still."
            )
        }
        val corr = primaryCorrection(target, primary, band)
        return GuidanceResult(false, band, corr, corr, corr)
    }

    private fun primaryCorrection(target: PoseTarget, value: Float, band: Band): String =
        when (target.position) {
            SignalPosition.HeadStraight -> when {
                value < target.min -> "Move a little more right"
                value > target.max -> "Move a little more left"
                else -> "Hold still"
            }
            SignalPosition.HeadTiltLeft -> when (band) {
                Band.TooLittle -> "Tilt a little more"
                Band.TooFar -> "Reduce the tilt"
                else -> "Hold still"
            }
            SignalPosition.HeadTiltRight -> when (band) {
                Band.TooLittle -> "Tilt a little more"
                Band.TooFar -> "Reduce the tilt"
                else -> "Hold still"
            }
            SignalPosition.PhoneHigher -> when (band) {
                Band.TooLittle -> "Move the phone slightly higher"
                Band.TooFar -> "Move the phone slightly lower"
                else -> "Hold still"
            }
            SignalPosition.PhoneLower -> when (band) {
                Band.TooLittle -> "Move the phone slightly lower"
                Band.TooFar -> "Move the phone slightly higher"
                else -> "Hold still"
            }
            SignalPosition.Closer -> when (band) {
                Band.TooLittle -> "Move closer"
                Band.TooFar -> "Move farther away"
                else -> "Hold still"
            }
            SignalPosition.Further -> when (band) {
                Band.TooLittle -> "Move farther away"
                Band.TooFar -> "Move closer"
                else -> "Hold still"
            }
        }

    private fun secondaryCorrection(label: String, value: Float, min: Float, max: Float): String =
        when (label) {
            "yaw" -> if (value < min) "Turn a little less left" else "Turn a little less right"
            "roll" -> if (value < min) "Reduce left tilt" else "Reduce right tilt"
            else -> "Return toward the target position"
        }

    /** Prove tilt uses roll ranges (signed) and not yaw as primary. */
    fun tiltUsesRoll(position: SignalPosition): Boolean =
        position == SignalPosition.HeadTiltLeft || position == SignalPosition.HeadTiltRight

    fun tiltPrimaryIsRoll(position: SignalPosition, baseline: BaselinePose?): Boolean =
        targetsFor(position, baseline).primaryLabel == "roll"
}
