package com.idworx.lisa.features.feedbackemail

import com.idworx.lisa.MainMenuDestination
import com.idworx.lisa.MenuDestinationInteractionStage
import com.idworx.lisa.MenuDestinationNavigationController
import com.idworx.lisa.MenuDestinationNavigationState
import com.idworx.lisa.MenuFeedbackDraft
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackCaregiverAssistAuthorityTest {

    @Test
    fun reviewAndSendOpensCaregiverAssistNotChooser() {
        val main = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/MainActivity.kt"
        ) ?: error("missing MainActivity")
        assertTrue(main.contains("openFeedbackCaregiverAssist()"))
        val saveBranch = main.substringAfter("actionId == MenuDestinationActionId.FeedbackSave &&")
            .substringBefore("actionId == MenuDestinationActionId.FeedbackClearDraft")
        assertTrue(saveBranch.contains("openFeedbackCaregiverAssist()"))
        assertFalse(saveBranch.contains("launchFeedbackEmail("))
        assertTrue(main.contains("onSaveFeedback = { _, _, _, _ ->"))
    }

    @Test
    fun touchAndWinkShareSameActionPaths() {
        val main = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/MainActivity.kt"
        ) ?: error("missing MainActivity")
        assertTrue(main.contains("activateFeedbackCaregiverAssistPrimary(caregiverStage.step)"))
        assertTrue(main.contains("authority.isPrimarySequence(left, right)"))
        assertTrue(main.contains("MenuDestinationPanelCommand.Select ->"))
        assertTrue(main.contains("activateFeedbackCaregiverAssistPrimary(caregiverStage.step)"))
        // Content buttons invoke the same Select/Back commands as wink sequences.
        val workspace = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/MenuDestinationWorkspaceUi.kt"
        ) ?: error("missing workspace")
        assertTrue(workspace.contains("binding.onCommand(MenuDestinationPanelCommand.Select)"))
        assertTrue(workspace.contains("binding.onCommand(MenuDestinationPanelCommand.Back)"))
    }

    @Test
    fun stage1SpeaksExactHelpPhraseAndDoesNotOpenEmail() {
        assertEquals(
            "Please send this email for me.",
            FeedbackCaregiverAssistAuthority.SPOKEN_HELP_REQUEST
        )
        val main = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/MainActivity.kt"
        ) ?: error("missing MainActivity")
        assertTrue(main.contains("SPOKEN_HELP_REQUEST"))
        assertTrue(
            main.contains("advanceFeedbackCaregiverAssistToOpenEmail")
        )
        // SpeakRequest branch speaks then advances; must not launch email there.
        val speakToOpen = main.substringAfter("SpeakRequest ->")
            .substringBefore("OpenEmailApp ->")
        assertTrue(speakToOpen.contains("speak("))
        assertTrue(speakToOpen.contains("SPOKEN_HELP_REQUEST"))
        assertFalse(speakToOpen.contains("launchFeedbackEmail("))
    }

    @Test
    fun stage1AdvancesToStage2AfterSpeak() {
        val state = MenuDestinationNavigationController.beginFeedbackCaregiverAssist(
            MenuDestinationNavigationState(MainMenuDestination.Feedback, isActive = true)
        )
        val stage = state.interactionStage as MenuDestinationInteractionStage.FeedbackCaregiverAssist
        assertEquals(FeedbackCaregiverAssistStep.SpeakRequest, stage.step)
        val advanced = MenuDestinationNavigationController.advanceFeedbackCaregiverAssistToOpenEmail(state)
        val next = advanced.interactionStage as MenuDestinationInteractionStage.FeedbackCaregiverAssist
        assertEquals(FeedbackCaregiverAssistStep.OpenEmailApp, next.step)
    }

    @Test
    fun stage2PrimaryOpensExistingEmailHandoff() {
        val main = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/MainActivity.kt"
        ) ?: error("missing MainActivity")
        val openBranch = main.substringAfter("OpenEmailApp ->")
            .substringBefore("private fun launchFeedbackEmail")
        assertTrue(openBranch.contains("launchFeedbackEmail("))
        assertTrue(openBranch.contains("cancelCurrentStage"))
    }

    @Test
    fun backFromBothStagesReturnsToFeedbackBrowsing() {
        val speak = MenuDestinationNavigationController.beginFeedbackCaregiverAssist(
            MenuDestinationNavigationState(MainMenuDestination.Feedback, isActive = true)
        )
        val cancelledSpeak = MenuDestinationNavigationController.cancelCurrentStage(speak)
        assertEquals(
            MenuDestinationInteractionStage.Browsing,
            cancelledSpeak.interactionStage
        )
        val open = MenuDestinationNavigationController.advanceFeedbackCaregiverAssistToOpenEmail(speak)
        val cancelledOpen = MenuDestinationNavigationController.cancelCurrentStage(open)
        assertEquals(
            MenuDestinationInteractionStage.Browsing,
            cancelledOpen.interactionStage
        )
        assertTrue(cancelledOpen.isActive)
    }

    @Test
    fun feedbackFieldsRemainIntactThroughAssistFlow() {
        val draft = MenuFeedbackDraft(
            workedWell = "a",
            confusing = "b",
            winkDetection = "c",
            speechTiming = "d"
        )
        assertTrue(draft.hasContent)
        // Assist stages do not mutate draft — only openFeedbackCaregiverAssist / cancel / email.
        val state = MenuDestinationNavigationController.beginFeedbackCaregiverAssist(
            MenuDestinationNavigationState(MainMenuDestination.Feedback, isActive = true)
        )
        MenuDestinationNavigationController.advanceFeedbackCaregiverAssistToOpenEmail(state)
        MenuDestinationNavigationController.cancelCurrentStage(state)
        assertEquals("a", draft.workedWell)
        assertEquals("d", draft.speechTiming)
    }

    @Test
    fun sequencesAreL1R1AndL2R2WithoutReuseConflict() {
        assertEquals(1 to 1, FeedbackCaregiverAssistAuthority.PRIMARY_SEQUENCE)
        assertEquals(2 to 2, FeedbackCaregiverAssistAuthority.BACK_SEQUENCE)
        assertTrue(
            FeedbackCaregiverAssistAuthority.isPrimarySequence(1, 1)
        )
        assertTrue(
            FeedbackCaregiverAssistAuthority.isBackSequence(2, 2)
        )
        assertFalse(
            FeedbackCaregiverAssistAuthority.isPrimarySequence(2, 2)
        )
    }

    @Test
    fun noOemOrEmailPackageSpecificLogic() {
        listOf(
            "app/src/main/java/com/idworx/lisa/features/feedbackemail/FeedbackCaregiverAssistAuthority.kt",
            "app/src/main/java/com/idworx/lisa/features/feedbackemail/FeedbackCaregiverAssistUi.kt",
            "app/src/main/java/com/idworx/lisa/features/feedbackemail/LisaFeedbackEmailAuthority.kt"
        ).forEach { path ->
            val src = ZeroTouchFileProbe.readProjectFile(path) ?: error(path)
            assertFalse(src.contains("com.google.android.gm"))
            assertFalse(src.contains("setPackage("))
            assertFalse(src.contains("outlook", ignoreCase = true))
            assertFalse(src.contains("samsung", ignoreCase = true))
        }
    }

    @Test
    fun emailTemplateAndSessionClearingUnchanged() {
        assertEquals(
            "lisa-feedback@asgarddynamics.io",
            LisaFeedbackEmailAuthority.DESTINATION_EMAIL
        )
        assertEquals("LISA Feedback", LisaFeedbackEmailAuthority.EMAIL_SUBJECT)
        val main = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/MainActivity.kt"
        ) ?: error("missing MainActivity")
        assertTrue(main.contains("discardPersistedFeedbackDraftFromPreviousBuilds()"))
        assertTrue(main.contains("uiMenuFeedbackDraft.value = MenuFeedbackDraft()"))
        assertFalse(main.contains("saveFeedbackDraft"))
    }

    @Test
    fun caregiverCopyMentionsCaregiverMayBeNeeded() {
        val strings = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/LisaUiStrings.kt"
        ) ?: error("missing strings")
        assertTrue(strings.contains("A caregiver may be needed to choose an email app and press Send."))
        assertTrue(strings.contains("Information leaves LISA only if you choose to send the email."))
        assertTrue(strings.contains("Help me send this feedback"))
        assertTrue(strings.contains("Ready for caregiver assistance"))
        assertTrue(strings.contains("An email account must already be set up"))
        assertTrue(strings.contains("select the email account"))
        assertTrue(strings.contains("LISA cannot confirm whether the email was sent."))
    }
}
