package com.idworx.lisa.features.experiencepolish.communicationworkspace

import com.idworx.lisa.LisaReleaseStore

object CommunicationWorkspaceEntryHandler {

    fun shouldPlayEntryIntro(store: LisaReleaseStore, onboardingCompleted: Boolean, cameraGranted: Boolean): Boolean =
        onboardingCompleted && cameraGranted && !store.isWorkspaceEntryIntroCompleted()

    fun markEntryIntroComplete(store: LisaReleaseStore) {
        store.setWorkspaceEntryIntroCompleted(completed = true)
    }

    fun entryDialogues(): List<String> = CommunicationWorkspaceExperience.entryDialogues()
}
