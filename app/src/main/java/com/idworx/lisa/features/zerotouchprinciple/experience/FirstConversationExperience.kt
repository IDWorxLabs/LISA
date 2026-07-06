package com.idworx.lisa.features.zerotouchprinciple.experience

import com.idworx.lisa.features.personality.dialogue.DefaultDialogueCatalog
import com.idworx.lisa.features.personality.model.DialogueCategory
import com.idworx.lisa.features.personality.model.LisaDialogue

/**
 * First Conversation Experience — Lisa introduces herself and guides without touch.
 */
object FirstConversationExperience {

    enum class Stage {
        MeetLisa,
        GettingReady,
        Calibration,
        CommunicationFundamentals,
        CommunicationMastery,
        WorkspaceNavigation,
        LisaCertifiedCommunicator
    }

    fun meetLisaDialogues(): List<LisaDialogue> =
        dialoguesWithTag("first_launch")

    fun returningUserDialogues(): List<LisaDialogue> =
        DefaultDialogueCatalog.forCategory(DialogueCategory.Greeting, "en")
            .filter { it.contextTags.contains("returning") }
            .sortedBy { it.id }

    fun gettingReadyDialogues(): List<String> = listOf(
        "Before we begin, I'd like to make sure I can see you clearly.",
        "I'm looking for your face.",
        "When you're ready, simply look back at me."
    )

    fun faceDetectedDialogues(): List<String> = listOf(
        "Wonderful.",
        "I can see you now."
    )

    fun faceLostDialogues(): List<String> = listOf(
        "I've lost sight of you.",
        "When you're ready, simply look back at me."
    )

    fun patienceDialogues(): List<String> = listOf(
        "I'll wait until you're ready.",
        "There is no rush.",
        "Take your time."
    )

    fun gentleMissedBlinkDialogues(): List<String> = listOf(
        "I think I missed that blink.",
        "Let's try once more together.",
        "That's alright.",
        "We'll keep trying together."
    )

    fun firstPhraseSuccessDialogues(): List<String> = listOf(
        "Wonderful!",
        "You did it!",
        "You've just spoken your very first word using Lisa.",
        "That was excellent.",
        "Let's learn another one."
    )

    private fun dialoguesWithTag(tag: String): List<LisaDialogue> =
        DefaultDialogueCatalog.forCategory(DialogueCategory.Greeting, "en")
            .filter { it.contextTags.contains(tag) }
            .sortedBy { it.id }
}
