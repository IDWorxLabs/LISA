package com.idworx.lisa.features.experiencepolish.guidedlearningsimplification

import com.idworx.lisa.features.personality.dialogue.DefaultDialogueCatalog

object GuidedLearningSimplificationExperience {

    const val PHASE_NAME: String = "LISA Guided Learning Simplification V1"

    fun workspaceTransitionDialogues(): List<String> =
        DefaultDialogueCatalog.all("en")
            .filter { it.contextTags.contains("gl_simplify_workspace_transition") }
            .sortedBy { it.id }
            .map { it.text }
}
