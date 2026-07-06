package com.idworx.lisa.features.guidedlearningsetupbeforehello.metadata

object GuidedLearningSetupBeforeHelloMetadata {

    const val PASS_TOKEN: String = "LISA_GUIDED_LEARNING_SETUP_BEFORE_HELLO_V1_PASS"

    const val FLOW_RULE: String =
        "Start Guided Learning → eye detection setup → ready check → Continue → HELLO lesson."

    const val NO_AUTO_HELLO: String =
        "Face/eye detection must not skip setup or open HELLO without Continue."
}
