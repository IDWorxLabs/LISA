package com.idworx.lisa.features.personality.engine

import com.idworx.lisa.features.personality.model.DialogueCategory
import com.idworx.lisa.features.personality.model.DialogueContext
import com.idworx.lisa.features.personality.model.LisaDialogue
import com.idworx.lisa.features.personality.model.CaregiverSupportMoment
import com.idworx.lisa.features.personality.model.MilestoneType
import com.idworx.lisa.features.personality.model.PresenceMoment

interface LisaPersonalityEngine {
    val profile: com.idworx.lisa.features.personality.model.LisaPersonalityProfile

    fun generateGreeting(context: DialogueContext): LisaDialogue
    fun generateGreetingSequence(context: DialogueContext): List<LisaDialogue>
    fun generateInstruction(context: DialogueContext): LisaDialogue
    fun generateEncouragement(context: DialogueContext): LisaDialogue
    fun generateComfort(context: DialogueContext): LisaDialogue
    fun generateCelebration(context: DialogueContext): LisaDialogue
    fun generateWaitingMessage(context: DialogueContext): LisaDialogue
    fun generateNavigationGuidance(context: DialogueContext): LisaDialogue
    fun generatePracticeMessage(context: DialogueContext): LisaDialogue
    fun generateMilestoneMessage(context: DialogueContext): LisaDialogue
    fun generateCompletionMessage(context: DialogueContext): List<LisaDialogue>
    fun generateGraduationMessage(context: DialogueContext): List<LisaDialogue>
    fun generateAlmostMessage(context: DialogueContext): LisaDialogue
    fun generateSkipConfirmMessage(context: DialogueContext): LisaDialogue
    fun generatePresenceDialogue(context: DialogueContext, moment: PresenceMoment): LisaDialogue
    fun generatePresenceSequence(context: DialogueContext, moment: PresenceMoment, maxLines: Int): List<LisaDialogue>
    fun generateCaregiverSupportDialogue(context: DialogueContext, moment: CaregiverSupportMoment): LisaDialogue
    fun generateCaregiverSupportSequence(context: DialogueContext, moment: CaregiverSupportMoment, maxLines: Int): List<LisaDialogue>
}
