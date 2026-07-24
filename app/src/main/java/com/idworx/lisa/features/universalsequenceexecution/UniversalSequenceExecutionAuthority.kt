package com.idworx.lisa.features.universalsequenceexecution

import com.idworx.lisa.formatWinkSequenceShort

/**
 * RC8.12 — Universal Sequence Execution Authority.
 *
 * Constitutional rule: every control that displays a blink sequence must execute the
 * same action for touch and for that blink sequence. Touch and blink never diverge.
 *
 * This authority does not own gesture recognition, timing, or destinations — it owns
 * parity between the advertised sequence and the executable action.
 */
object UniversalSequenceExecutionAuthority {

    data class BoundControl(
        val controlId: String,
        val surface: String,
        val left: Int,
        val right: Int,
        val sequenceLabel: String = formatWinkSequenceShort(left, right),
        /** True when a touch path invokes [execute]. */
        val touchWired: Boolean,
        /** True when a blink path invokes [execute] for the same sequence. */
        val blinkWired: Boolean,
        val execute: () -> Unit = {}
    )

    data class ParityViolation(
        val controlId: String,
        val surface: String,
        val sequenceLabel: String,
        val reason: String
    )

    /** Both input modalities must call this with the identical action closure. */
    fun runShared(action: () -> Unit) {
        action()
    }

    fun matches(left: Int, right: Int, expectedLeft: Int, expectedRight: Int): Boolean =
        left == expectedLeft && right == expectedRight

    fun sequenceLabel(left: Int, right: Int): String =
        formatWinkSequenceShort(left, right)

    /**
     * Static parity check over declared bindings. Does not inspect live Compose trees.
     * Returns violations when a sequence is advertised without blink wiring, or blink is
     * wired without a touch owner.
     */
    fun validateParity(controls: List<BoundControl>): List<ParityViolation> {
        val violations = mutableListOf<ParityViolation>()
        controls.forEach { control ->
            if (control.sequenceLabel.isNotBlank() && control.touchWired && !control.blinkWired) {
                violations += ParityViolation(
                    controlId = control.controlId,
                    surface = control.surface,
                    sequenceLabel = control.sequenceLabel,
                    reason = "sequence displayed / touch wired but blink execution missing"
                )
            }
            if (control.blinkWired && !control.touchWired) {
                violations += ParityViolation(
                    controlId = control.controlId,
                    surface = control.surface,
                    sequenceLabel = control.sequenceLabel,
                    reason = "blink handler exists without a touch owner"
                )
            }
            if (control.left < 0 || control.right < 0 || control.left + control.right < 2) {
                violations += ParityViolation(
                    controlId = control.controlId,
                    surface = control.surface,
                    sequenceLabel = control.sequenceLabel,
                    reason = "assigned sequence must be a deliberate multi-blink gesture"
                )
            }
        }
        return violations
    }

    /**
     * Canonical catalog of sequence-labelled surfaces audited for RC8.12.
     * Wiring flags describe intended production ownership after the Setup / Retry fixes.
     */
    fun auditedCatalog(): List<BoundControl> = listOf(
        BoundControl(
            controlId = "guided_readiness_back",
            surface = "GuidedLearning.Ready",
            left = GuidedReadinessSequenceAuthority.BACK_LEFT,
            right = GuidedReadinessSequenceAuthority.BACK_RIGHT,
            touchWired = true,
            blinkWired = true
        ),
        BoundControl(
            controlId = "guided_readiness_continue",
            surface = "GuidedLearning.Ready",
            left = GuidedReadinessSequenceAuthority.CONTINUE_LEFT,
            right = GuidedReadinessSequenceAuthority.CONTINUE_RIGHT,
            touchWired = true,
            blinkWired = true
        ),
        BoundControl(
            controlId = "welcome_continue_to_destination",
            surface = "Welcome.Introduction",
            left = WelcomeSequenceCatalog.CONTINUE_LEFT,
            right = WelcomeSequenceCatalog.CONTINUE_RIGHT,
            touchWired = true,
            blinkWired = true
        ),
        BoundControl(
            controlId = "welcome_start_guided",
            surface = "Welcome.DestinationSelection",
            left = WelcomeSequenceCatalog.START_GUIDED_LEFT,
            right = WelcomeSequenceCatalog.START_GUIDED_RIGHT,
            touchWired = true,
            blinkWired = true
        ),
        BoundControl(
            controlId = "welcome_skip_communication",
            surface = "Welcome.DestinationSelection",
            left = WelcomeSequenceCatalog.SKIP_LEFT,
            right = WelcomeSequenceCatalog.SKIP_RIGHT,
            touchWired = true,
            blinkWired = true
        ),
        BoundControl(
            controlId = "welcome_back",
            surface = "Welcome.DestinationSelection",
            left = WelcomeSequenceCatalog.BACK_LEFT,
            right = WelcomeSequenceCatalog.BACK_RIGHT,
            touchWired = true,
            blinkWired = true
        ),
        BoundControl(
            controlId = "settings_recalibration_retry",
            surface = "Settings.Recalibration.Failed",
            left = SettingsRecalibrationRetrySequenceAuthority.RETRY_LEFT,
            right = SettingsRecalibrationRetrySequenceAuthority.RETRY_RIGHT,
            touchWired = true,
            blinkWired = true
        ),
        BoundControl(
            controlId = "main_menu_open_selected",
            surface = "Menu",
            left = MenuSequenceCatalog.SELECT_LEFT,
            right = MenuSequenceCatalog.SELECT_RIGHT,
            touchWired = true,
            blinkWired = true
        ),
        BoundControl(
            controlId = "main_menu_back_close",
            surface = "Menu",
            left = MenuSequenceCatalog.BACK_LEFT,
            right = MenuSequenceCatalog.BACK_RIGHT,
            touchWired = true,
            blinkWired = true
        ),
        BoundControl(
            controlId = "guided_category_select",
            surface = "Communication.Category",
            left = CategorySequenceCatalog.SELECT_LEFT,
            right = CategorySequenceCatalog.SELECT_RIGHT,
            touchWired = true,
            blinkWired = true
        ),
        BoundControl(
            controlId = "guided_nav_back",
            surface = "Communication.Navigation",
            left = CategorySequenceCatalog.BACK_LEFT,
            right = CategorySequenceCatalog.BACK_RIGHT,
            touchWired = true,
            blinkWired = true
        )
    )

    fun debugValidateAuditedCatalog(): List<ParityViolation> =
        validateParity(auditedCatalog())
}

/**
 * Debug-only startup check. Never invoke from release production paths.
 */
object UniversalSequenceExecutionDebugValidator {
    fun runIfDebug(isDebugBuild: Boolean, log: (String) -> Unit) {
        if (!isDebugBuild) return
        val violations = UniversalSequenceExecutionAuthority.debugValidateAuditedCatalog()
        if (violations.isEmpty()) {
            log("RC8.12 UniversalSequenceExecutionAuthority: parity OK (${UniversalSequenceExecutionAuthority.auditedCatalog().size} controls)")
            return
        }
        violations.forEach { v ->
            log("RC8.12 SEQUENCE PARITY WARNING [${v.surface}/${v.controlId}] ${v.sequenceLabel}: ${v.reason}")
        }
    }
}
