package com.idworx.lisa.features.onboardingguide.metadata

object TrainingMetadata {
    const val FEATURE_NAME: String = "LISA Learning Journey"
    const val VERSION: String = "V1"
    const val JOURNEY_STAGE_COUNT: Int = 7
    const val COMMUNICATION_LESSON_COUNT: Int = 20
    /** Beginner phrase curriculum taught before navigation in Guided Learning. */
    const val GUIDED_LEARNING_ESSENTIAL_PHRASE_COUNT: Int = 15
    const val MASTERY_ROUND_COUNT: Int = 10
    const val NAVIGATION_LESSON_COUNT: Int = 8

    val GUIDED_LEARNING_ESSENTIAL_VOCABULARY_IDS: List<String> = listOf(
        "hello",
        "yes",
        "no",
        "please",
        "thank_you",
        "i_need_water",
        "i_need_food",
        "i_need_help",
        "i_am_in_pain",
        "call_my_family",
        "i_want_to_lie_down",
        "i_need_the_toilet",
        "i_am_good",
        "i_am_not_okay",
        "i_am_okay"
    )
}
