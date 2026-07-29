package com.idworx.lisa.features.onboardingguide.lessons

import com.idworx.lisa.features.brain1interactionstandard.model.BlinkSequenceOrder
import com.idworx.lisa.features.brain1interactionstandard.model.UniversalInteractionGestures
import com.idworx.lisa.features.onboardingguide.metadata.TrainingMetadata
import com.idworx.lisa.features.onboardingguide.model.CommunicationLesson
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.features.onboardingguide.model.NavigationLesson
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.features.silentwelcome.LisaSpeechPolicy

object TrainingLessonCatalog {

    /**
     * Stage 4 — progressive difficulty (Level 1 = 2 blinks → Level 5 = 6+ blinks).
     * First [TrainingMetadata.GUIDED_LEARNING_ESSENTIAL_PHRASE_COUNT] lessons form the
     * beginner curriculum; navigation begins only after those are complete.
     */
    val communicationFundamentals: List<CommunicationLesson> = listOf(
        // Level 1 — Very Easy (2 blinks)
        CommunicationLesson("comm_hello", "hello", 2, 0, 1, difficultyLevel = 1),
        CommunicationLesson("comm_yes", "yes", 0, 2, 2, difficultyLevel = 1),
        CommunicationLesson("comm_no", "no", 1, 1, 3, difficultyLevel = 1),
        // Level 2 — Easy (3–4 blinks)
        CommunicationLesson("comm_please", "please", 1, 2, 4, difficultyLevel = 2),
        CommunicationLesson("comm_thank_you", "thank_you", 3, 1, 5, difficultyLevel = 2),
        CommunicationLesson("comm_water", "i_need_water", 1, 3, 6, difficultyLevel = 2),
        CommunicationLesson("comm_food", "i_need_food", 2, 1, 7, difficultyLevel = 2),
        // Level 3 — Intermediate (3–5 blinks)
        CommunicationLesson("comm_help", "i_need_help", 3, 0, 8, difficultyLevel = 3),
        CommunicationLesson("comm_pain", "i_am_in_pain", 2, 3, 9, difficultyLevel = 3),
        CommunicationLesson("comm_family", "call_my_family", 0, 4, 10, difficultyLevel = 3),
        CommunicationLesson("comm_tired", "i_want_to_lie_down", 3, 2, 11, difficultyLevel = 3),
        CommunicationLesson("comm_bathroom", "i_need_the_toilet", 2, 4, 12, difficultyLevel = 3),
        // Level 4 — Advanced (4–5 blinks)
        CommunicationLesson("comm_good", "i_am_good", 4, 0, 13, difficultyLevel = 4),
        CommunicationLesson("comm_bad", "i_am_not_okay", 0, 5, 14, difficultyLevel = 4),
        CommunicationLesson("comm_im_ok", "i_am_okay", 5, 0, 15, difficultyLevel = 4),
        // Extended curriculum (full 20-lesson path when narration is enabled)
        CommunicationLesson("comm_goodbye", "goodbye", 1, 0, 16, difficultyLevel = 4),
        CommunicationLesson("comm_love", "i_love_you", 4, 1, 17, difficultyLevel = 4),
        CommunicationLesson("comm_how_are_you", "how_are_you", 4, 2, 18, difficultyLevel = 4),
        CommunicationLesson("comm_caregiver", "call_my_caregiver", 4, 3, 19, difficultyLevel = 5),
        CommunicationLesson("comm_emergency", "emergency", 6, 0, 20, difficultyLevel = 5)
    )

    val communicationLessons: List<CommunicationLesson> get() = communicationFundamentals

    /**
     * Real Communication Workspace training — every lesson below is taught inside the actual
     * workspace (Guided Training Mode), never a standalone fake screen. RC8.25 — lessons 16–19
     * form the Medical journey (scroll+open → direct open → speak phrase → back), then remaining
     * real controls and Explore LISA.
     */
    val navigationLessons: List<NavigationLesson> = listOf(
        NavigationLesson(
            com.idworx.lisa.features.guidedmedicalcategoryjourney.GuidedMedicalCategoryJourneyAuthority
                .ID_MOVE_TO_MEDICAL,
            NavigationAction.MoveToMedicalCategory,
            1
        ),
        NavigationLesson(
            com.idworx.lisa.features.guidedmedicalcategoryjourney.GuidedMedicalCategoryJourneyAuthority
                .ID_OPEN_MEDICAL,
            NavigationAction.SelectCategory,
            2
        ),
        NavigationLesson(
            com.idworx.lisa.features.guidedmedicalcategoryjourney.GuidedMedicalCategoryJourneyAuthority
                .ID_USE_MEDICAL_PHRASE,
            NavigationAction.SelectPhrase,
            3
        ),
        NavigationLesson("nav_back", NavigationAction.CloseMenu, 4),
        NavigationLesson("nav_next_page", NavigationAction.NextPage, 5),
        NavigationLesson("nav_previous_page", NavigationAction.PreviousPage, 6),
        NavigationLesson("nav_emergency", NavigationAction.TriggerEmergency, 7),
        NavigationLesson("nav_reset", NavigationAction.ResetSequence, 8),
        // RC8.13 — Explore LISA (final confidence tour; production Menu / Voice / Settings only)
        NavigationLesson(
            com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.ID_OPEN_MENU,
            NavigationAction.OpenMenu,
            9
        ),
        NavigationLesson(
            com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.ID_SELECT_VOICE,
            NavigationAction.MenuSelectVoice,
            10
        ),
        NavigationLesson(
            com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.ID_OPEN_VOICE,
            NavigationAction.OpenVoice,
            11
        ),
        NavigationLesson(
            com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.ID_BACK_VOICE,
            NavigationAction.BackFromDestination,
            12
        ),
        NavigationLesson(
            com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.ID_SELECT_SETTINGS,
            NavigationAction.MenuSelectSettings,
            13
        ),
        NavigationLesson(
            com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.ID_OPEN_SETTINGS,
            NavigationAction.OpenSettings,
            14
        ),
        NavigationLesson(
            com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.ID_BACK_SETTINGS,
            NavigationAction.BackFromDestination,
            15
        ),
        NavigationLesson(
            com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.ID_CLOSE_MENU,
            NavigationAction.CloseMenu,
            16
        ),
        NavigationLesson(
            com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.ID_FINISH,
            NavigationAction.FinishGuidedLearning,
            17
        )
    )

    val totalLessons: Int get() =
        communicationFundamentals.size + TrainingMetadata.MASTERY_ROUND_COUNT + navigationLessons.size

    fun communicationLessonAt(index: Int): CommunicationLesson? =
        communicationFundamentals.getOrNull(index)

    fun lessonMatchesGesture(lesson: CommunicationLesson, left: Int, right: Int, blinkOrder: List<Boolean>): Boolean =
        lesson.left == left && lesson.right == right && BlinkSequenceOrder.matches(blinkOrder, lesson.blinkOrder)

    fun earliestLessonsUseSimpleGestures(): Boolean {
        val early = communicationFundamentals.take(3)
        return early.all { UniversalInteractionGestures.difficultyLevel(it.left, it.right) == 1 }
    }

    fun previousCommunicationLesson(index: Int): CommunicationLesson? =
        communicationFundamentals.getOrNull(index - 1)

    fun adjacentDifficultyJumpsWithin(maxJump: Int): Boolean =
        communicationFundamentals.zipWithNext().all { (current, next) ->
            next.difficultyLevel - current.difficultyLevel <= maxJump
        }

    fun dailyPhraseCount(): Int =
        communicationFundamentals.count { it.difficultyLevel <= 2 }

    fun masteryLessonAt(roundIndex: Int, phraseOrder: String): CommunicationLesson? {
        val order = parseMasteryOrder(phraseOrder)
        val fundamentalsIndex = order.getOrNull(roundIndex) ?: return null
        return communicationFundamentals.getOrNull(fundamentalsIndex)
    }

    fun navigationLessonAt(index: Int): NavigationLesson? =
        navigationLessons.getOrNull(index)

    fun buildMasteryOrder(seed: Int = System.currentTimeMillis().toInt()): String {
        val indices = communicationFundamentals.indices.shuffled(kotlin.random.Random(seed))
        return indices.take(TrainingMetadata.MASTERY_ROUND_COUNT).joinToString(",")
    }

    fun parseMasteryOrder(raw: String): List<Int> =
        if (raw.isBlank()) emptyList()
        else raw.split(",").mapNotNull { it.trim().toIntOrNull() }

    fun lessonNumber(phase: TrainingPhase, commIndex: Int, navIndex: Int, masteryIndex: Int = 0): Int =
        when (phase) {
            TrainingPhase.CommunicationLesson -> commIndex + 1
            TrainingPhase.CommunicationMastery ->
                communicationFundamentals.size + masteryIndex + 1
            TrainingPhase.NavigationLesson ->
                communicationFundamentals.size + TrainingMetadata.MASTERY_ROUND_COUNT + navIndex + 1
            else -> 0
        }

    /**
     * Current lesson number and total lesson count for the simple "Lesson X of Y" label shown
     * on every Guided Learning lesson screen. Mirrors the path actually taken given
     * [LisaSpeechPolicy.PHRASE_TRANSLATION_ONLY]: Communication Mastery rounds are only counted
     * when that policy allows the full curriculum (mastery is otherwise skipped), so navigation
     * lessons continue the numbering straight after the essential phrases. Returns null outside
     * the three lesson phases (nothing to display on Welcome/Setup/Calibration/Completion).
     */
    fun guidedLessonProgress(progress: TrainingProgress): Pair<Int, Int>? {
        val phraseCount = if (LisaSpeechPolicy.PHRASE_TRANSLATION_ONLY) {
            TrainingMetadata.GUIDED_LEARNING_ESSENTIAL_PHRASE_COUNT
        } else {
            TrainingMetadata.COMMUNICATION_LESSON_COUNT
        }
        val masteryCount = if (LisaSpeechPolicy.PHRASE_TRANSLATION_ONLY) 0 else TrainingMetadata.MASTERY_ROUND_COUNT
        val total = phraseCount + masteryCount + TrainingMetadata.NAVIGATION_LESSON_COUNT
        val current = when (progress.currentPhase) {
            TrainingPhase.CommunicationLesson -> progress.communicationLessonIndex + 1
            TrainingPhase.CommunicationMastery -> phraseCount + progress.masteryRoundIndex + 1
            TrainingPhase.NavigationLesson -> phraseCount + masteryCount + progress.navigationLessonIndex + 1
            else -> return null
        }
        return current to total
    }

    fun stageLabel(phase: TrainingPhase): String = when (phase) {
        TrainingPhase.FirstLaunchChoice -> "Getting Started"
        TrainingPhase.Welcome -> "Meet Lisa"
        TrainingPhase.Setup -> "Getting Ready"
        TrainingPhase.Calibration -> "Calibration"
        TrainingPhase.CommunicationLesson -> "Communication Fundamentals"
        TrainingPhase.CommunicationMastery -> "Communication Mastery"
        TrainingPhase.NavigationLesson -> "Workspace Navigation"
        TrainingPhase.Completion -> "LISA Certified Communicator"
        TrainingPhase.SkipConfirm -> "Confirm"
    }
}
