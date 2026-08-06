package com.idworx.lisa.features.eyediagnostic

/**
 * Protocol metadata for the guided 8-step Eye Test wizard.
 */
object EyeTestProtocol {
    const val GUIDED_STEPS: Int = EyeTestFlowAuthority.TOTAL_GUIDED_STEPS
}

enum class EyeTestSessionKind(val fileToken: String, val displayName: String) {
    WITHOUT_GLASSES("without_glasses", "Without Glasses"),
    WITH_GLASSES("with_glasses", "With Glasses")
}
