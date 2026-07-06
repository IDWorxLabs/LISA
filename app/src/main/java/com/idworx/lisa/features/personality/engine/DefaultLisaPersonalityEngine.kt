package com.idworx.lisa.features.personality.engine

import com.idworx.lisa.features.personality.celebration.CelebrationDialogueProvider
import com.idworx.lisa.features.personality.comfort.ComfortDialogueProvider
import com.idworx.lisa.features.personality.dialogue.DefaultDialogueCatalog
import com.idworx.lisa.features.personality.encouragement.EncouragementDialogueProvider
import com.idworx.lisa.features.personality.greetings.GreetingDialogueProvider
import com.idworx.lisa.features.personality.instruction.InstructionDialogueProvider
import com.idworx.lisa.features.personality.model.DialogueCategory
import com.idworx.lisa.features.personality.model.DialogueContext
import com.idworx.lisa.features.personality.model.LisaDialogue
import com.idworx.lisa.features.personality.model.LisaPersonalityProfile
import com.idworx.lisa.features.personality.model.CaregiverSupportMoment
import com.idworx.lisa.features.personality.model.MilestoneType
import com.idworx.lisa.features.personality.model.PresenceMoment
import com.idworx.lisa.features.personality.navigation.NavigationDialogueProvider
import com.idworx.lisa.features.personality.practice.PracticeDialogueProvider
import com.idworx.lisa.features.personality.state.DialogueHistoryTracker
import com.idworx.lisa.features.personality.presence.EmotionalPresenceDialogueProvider
import com.idworx.lisa.features.personality.caregiver.CaregiverSupportDialogueProvider
import com.idworx.lisa.features.personality.waiting.WaitingDialogueProvider

class DefaultLisaPersonalityEngine(
    override val profile: LisaPersonalityProfile = LisaPersonalityProfile.DEFAULT,
    private val history: DialogueHistoryTracker = DialogueHistoryTracker(),
    private val selector: LisaDialogueSelector = LisaDialogueSelector(DefaultDialogueCatalog, history, profile),
    private val greetings: GreetingDialogueProvider = GreetingDialogueProvider(selector),
    private val encouragement: EncouragementDialogueProvider = EncouragementDialogueProvider(DefaultDialogueCatalog, selector),
    private val comfort: ComfortDialogueProvider = ComfortDialogueProvider(selector),
    private val celebration: CelebrationDialogueProvider = CelebrationDialogueProvider(selector),
    private val waiting: WaitingDialogueProvider = WaitingDialogueProvider(selector),
    private val instruction: InstructionDialogueProvider = InstructionDialogueProvider(selector),
    private val navigation: NavigationDialogueProvider = NavigationDialogueProvider(selector),
    private val practice: PracticeDialogueProvider = PracticeDialogueProvider(selector),
    private val presence: EmotionalPresenceDialogueProvider = EmotionalPresenceDialogueProvider(selector),
    private val caregiverSupport: CaregiverSupportDialogueProvider = CaregiverSupportDialogueProvider(selector)
) : LisaPersonalityEngine {

    fun historyTracker(): DialogueHistoryTracker = history

    override fun generateGreeting(context: DialogueContext): LisaDialogue =
        greetings.generate(context)

    override fun generateGreetingSequence(context: DialogueContext): List<LisaDialogue> =
        if (context.returningUser || context.daysSinceLastSession > 0) {
            greetings.generateReturningSequence(context)
        } else {
            greetings.generateFirstLaunchSequence(context)
        }

    override fun generateInstruction(context: DialogueContext): LisaDialogue =
        instruction.generate(context)

    override fun generateEncouragement(context: DialogueContext): LisaDialogue =
        encouragement.generate(context)

    override fun generateComfort(context: DialogueContext): LisaDialogue =
        comfort.generate(context)

    override fun generateCelebration(context: DialogueContext): LisaDialogue =
        celebration.generate(context)

    override fun generateWaitingMessage(context: DialogueContext): LisaDialogue =
        waiting.generate(context)

    override fun generateNavigationGuidance(context: DialogueContext): LisaDialogue =
        navigation.generate(context)

    override fun generatePracticeMessage(context: DialogueContext): LisaDialogue =
        practice.generate(context)

    override fun generateMilestoneMessage(context: DialogueContext): LisaDialogue {
        val type = context.milestoneType ?: MilestoneType.LessonComplete
        return selector.selectMilestone(type, context)
    }

    override fun generateCompletionMessage(context: DialogueContext): List<LisaDialogue> =
        DefaultDialogueCatalog.forCategory(DialogueCategory.SessionCompletion, context.locale)

    override fun generateGraduationMessage(context: DialogueContext): List<LisaDialogue> =
        DefaultDialogueCatalog.forCategory(DialogueCategory.Graduation, context.locale)
            .sortedBy { it.id }

    override fun generateAlmostMessage(context: DialogueContext): LisaDialogue =
        encouragement.generateAlmost(context)

    override fun generateSkipConfirmMessage(context: DialogueContext): LisaDialogue =
        LisaDialogue(
            id = "skip_confirm",
            text = "Would you like to begin using Lisa now?",
            category = DialogueCategory.Instruction,
            locale = context.locale
        )

    override fun generatePresenceDialogue(context: DialogueContext, moment: PresenceMoment): LisaDialogue =
        presence.generate(context, moment)

    override fun generatePresenceSequence(
        context: DialogueContext,
        moment: PresenceMoment,
        maxLines: Int
    ): List<LisaDialogue> = presence.generateSequence(context, moment, maxLines)

    override fun generateCaregiverSupportDialogue(
        context: DialogueContext,
        moment: CaregiverSupportMoment
    ): LisaDialogue = caregiverSupport.generate(context, moment)

    override fun generateCaregiverSupportSequence(
        context: DialogueContext,
        moment: CaregiverSupportMoment,
        maxLines: Int
    ): List<LisaDialogue> = caregiverSupport.generateSequence(context, moment, maxLines)
}

object LisaPersonalityEngines {
    val default: LisaPersonalityEngine by lazy { DefaultLisaPersonalityEngine() }

    fun createDeterministic(seed: Int = 0): DefaultLisaPersonalityEngine =
        DefaultLisaPersonalityEngine(history = DialogueHistoryTracker())
}
