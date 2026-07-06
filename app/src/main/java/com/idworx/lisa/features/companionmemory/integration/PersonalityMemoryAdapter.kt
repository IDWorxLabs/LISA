package com.idworx.lisa.features.companionmemory.integration

import com.idworx.lisa.features.companionmemory.engine.CompanionMemoryEngine
import com.idworx.lisa.features.companionmemory.model.GreetingContext
import com.idworx.lisa.features.companionmemory.model.GreetingScenario
import com.idworx.lisa.features.companionmemory.model.LearningMilestone
import com.idworx.lisa.features.personality.model.DialogueContext
import com.idworx.lisa.features.personality.model.MilestoneType

object PersonalityMemoryAdapter {

    fun enrichDialogueContext(base: DialogueContext, greeting: GreetingContext): DialogueContext =
        base.copy(
            returningUser = greeting.returningUser ||
                greeting.scenario != GreetingScenario.FirstLaunch,
            daysSinceLastSession = greeting.daysSinceLastSession,
            guidedLearningComplete = greeting.guidedLearningComplete,
            tutorialSkipped = greeting.tutorialSkipped,
            practiceMode = greeting.practiceStreakDays > 0 || base.practiceMode,
            milestoneType = resolveMilestoneType(greeting, base.milestoneType)
        )

    fun greetingContextFromEngine(engine: CompanionMemoryEngine): GreetingContext =
        engine.getGreetingContext()

    fun isFirstLaunch(engine: CompanionMemoryEngine): Boolean =
        engine.getGreetingContext().isFirstLaunch

    private fun resolveMilestoneType(
        greeting: GreetingContext,
        fallback: MilestoneType?
    ): MilestoneType? = when {
        greeting.graduationComplete -> MilestoneType.GuidedLearningComplete
        greeting.completedCommunicationTraining && !greeting.completedNavigationTraining ->
            MilestoneType.CommunicationTrainingComplete
        greeting.completedNavigationTraining -> MilestoneType.NavigationTrainingComplete
        greeting.unfinishedLessonId != null -> MilestoneType.LessonComplete
        greeting.scenario == GreetingScenario.ReturningUser ||
            greeting.scenario == GreetingScenario.ReturningToday -> MilestoneType.ReturningUser
        else -> fallback
    }

    fun hasMilestone(engine: CompanionMemoryEngine, milestone: LearningMilestone): Boolean =
        milestone in engine.getMilestones()
}
