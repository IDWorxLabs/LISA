package com.idworx.lisa.features.guidedlessonteaching

import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceHighlightTarget

/**
 * RC8.22 — One instructional method within a guided lesson (primary / secondary / …).
 *
 * Each method can carry its own title, body lines, and optional highlighted sequence box
 * so lessons that teach multiple valid approaches stay consistent for 16–32.
 */
data class GuidedLessonTeachingMethod(
    val title: String,
    val instructionalLines: List<String> = emptyList(),
    /** Sequence shown in the shared highlighted box (e.g. "L0 R2"). Null = no box. */
    val highlightedSequence: String? = null
)

/**
 * RC8.23 / RC8.24 — Production action a teaching phase requires the learner to demonstrate.
 *
 * Always maps onto real Communication workspace gestures — never a training-only sequence.
 */
enum class GuidedLessonPhaseRequiredAction {
    /** Move Down (L0 R2) until [GuidedLessonTeachingPhase.productionTargetCategoryIndex] is selected. */
    MoveDownUntilCategorySelected,
    /**
     * Open the currently selected category via production Select (L1 R1 → openSelectedCategory).
     * Completes only when the target category workspace is actually open via that path.
     */
    OpenSelectedCategory,
    /**
     * Perform the category's labelled shortcut until production opens that category
     * (e.g. Medical L3 R1 → openCategoryDirectly).
     */
    CategoryShortcutJump,
    /** RC8.32 — move Category Menu from Page 1 to Page 2 via production Next Page (L0 R4). */
    MoveToSettingsPage,
    /** RC8.28 — open Settings & Controls via production L5 R5. */
    OpenSettingsAndControls,
    /**
     * RC8.28 / RC8.32 — open Sensitivity from the Settings hub via the labelled card sequence
     * L2 R0 (not generic Select L1 R1).
     */
    OpenSensitivitySetting,
    /**
     * RC8.32 — decrease (L3 R1) or increase (L1 R3) Sensitivity one valid step (immediate-save).
     */
    AdjustSensitivity,
    /** RC8.32 — return from Sensitivity Adjustment to Settings & Controls via Back (L2 R2). */
    ReturnToSettingsAndControls,
    /**
     * RC8.28 / RC8.30 legacy — increase-only phase. Superseded by [AdjustSensitivity].
     * Kept for enum stability only.
     */
    IncreaseSensitivityOnce,
    /**
     * RC8.28 legacy — retired by RC8.30 IMMEDIATE_SAVE. Kept for enum stability only.
     */
    SaveSensitivity
}

/**
 * RC8.23 / RC8.24 — One sequential practical exercise within a single guided lesson.
 *
 * Lessons may contain multiple phases; the engine advances them in order and only completes
 * the catalog lesson after the final phase succeeds.
 */
data class GuidedLessonTeachingPhase(
    val id: String,
    val title: String,
    /**
     * RC8.33 — optional short purpose/context shown once above the instruction.
     * Must not repeat [description] / instructional lines.
     */
    val context: String? = null,
    val description: String? = null,
    val methods: List<GuidedLessonTeachingMethod> = emptyList(),
    val rawGestureLabel: String,
    val navigationControlHighlight: GuidedWorkspaceHighlightTarget? = null,
    val productionTargetCategoryIndex: Int? = null,
    val destinationCategoryIndex: Int? = null,
    val requiredAction: GuidedLessonPhaseRequiredAction,
    /** Primary acknowledgement line (e.g. "Well done!"). Unused when [showCompletionFeedback] is false. */
    val completionFeedbackMessage: String = "",
    /** Optional supporting line under the acknowledgement. */
    val completionFeedbackDetail: String? = null,
    /**
     * When true, after this phase's feedback the Communication workspace is reset to the
     * lesson's initial production start state before the next phase is shown.
     */
    val resetWorkspaceBeforeNextPhase: Boolean = false,
    /**
     * RC8.24 — when false, the engine advances to the next phase immediately without a
     * "Well done" acknowledgement (e.g. scroll-to-Medical → open-selected instruction).
     */
    val showCompletionFeedback: Boolean = true
)

/**
 * RC8.19–RC8.24 — Reusable guided-lesson teaching presentation for lessons 16–32.
 *
 * Separates:
 * - WHAT — [title] / [description]
 * - HOW — [methods] (preferred) or legacy [nextActionHeading] + steps
 * - WHICH control — [navigationControlHighlight]
 * - WHERE to arrive — [productionTargetCategoryIndex] (completion; not a glow)
 * - optional visual destination — [destinationCategoryIndex]
 * - RC8.23/24 sequential phases — [phases] (when non-empty, engine drives one phase at a time)
 */
data class GuidedLessonTeachingPresentation(
    val title: String,
    /**
     * RC8.33 — optional short purpose/context shown once above the instruction.
     * Distinct from [description] (the next-action instruction).
     */
    val context: String? = null,
    /** Body explaining the workspace / goal — or the single next-action instruction. */
    val description: String? = null,
    /**
     * RC8.22 — ordered instructional methods (Method 1, Method 2, …).
     * When non-empty, the card renders these instead of the legacy Next Action block.
     */
    val methods: List<GuidedLessonTeachingMethod> = emptyList(),
    /**
     * Legacy single Next Action heading. Used when [methods] is empty.
     */
    val nextActionHeading: String? = null,
    /** Legacy steps under Next Action. */
    val nextActionSteps: List<String> = emptyList(),
    /** Legacy single sequence box when [methods] is empty. */
    val sequenceEmphasis: String? = null,
    /** Production gesture for the primary taught control (e.g. "L0 R2"). */
    val rawGestureLabel: String,
    val navigationControlHighlight: GuidedWorkspaceHighlightTarget? = null,
    val productionTargetCategoryIndex: Int? = null,
    val destinationCategoryIndex: Int? = null,
    /**
     * RC8.23 — ordered practical exercises within this lesson.
     * Empty = single-step lesson (legacy). Non-empty = multi-phase assessment.
     */
    val phases: List<GuidedLessonTeachingPhase> = emptyList()
) {
    val usesStructuredMethods: Boolean get() = methods.isNotEmpty()

    val usesStructuredNextAction: Boolean
        get() = usesStructuredMethods || !nextActionHeading.isNullOrBlank()

    val isMultiPhase: Boolean get() = phases.size > 1

    /** Presentation for the active phase index (or this presentation if no phases). */
    fun forPhaseIndex(phaseIndex: Int): GuidedLessonTeachingPresentation {
        if (phases.isEmpty()) return this
        val phase = phases.getOrNull(phaseIndex.coerceAtLeast(0)) ?: phases.last()
        return copy(
            title = phase.title,
            context = phase.context,
            description = phase.description,
            methods = phase.methods,
            rawGestureLabel = phase.rawGestureLabel,
            navigationControlHighlight = phase.navigationControlHighlight,
            productionTargetCategoryIndex = phase.productionTargetCategoryIndex,
            destinationCategoryIndex = phase.destinationCategoryIndex,
            nextActionHeading = null,
            nextActionSteps = emptyList(),
            sequenceEmphasis = null
        )
    }

    fun phaseAt(phaseIndex: Int): GuidedLessonTeachingPhase? =
        phases.getOrNull(phaseIndex.coerceAtLeast(0))
}
