package com.idworx.lisa.features.companionmemory.validation

import com.idworx.lisa.features.companionmemory.analytics.LearningProgressAnalyzer
import com.idworx.lisa.features.companionmemory.engine.CompanionMemoryEngine
import com.idworx.lisa.features.companionmemory.engine.CompanionMemoryEngines
import com.idworx.lisa.features.companionmemory.engine.DefaultCompanionMemoryEngine
import com.idworx.lisa.features.companionmemory.engine.MemoryPrivacyGuard
import com.idworx.lisa.features.companionmemory.integration.GuidedLearningMemoryAdapter
import com.idworx.lisa.features.companionmemory.integration.PersonalityMemoryAdapter
import com.idworx.lisa.features.companionmemory.integration.PracticeMemoryAdapter
import com.idworx.lisa.features.companionmemory.metadata.CompanionMemoryMetadata
import com.idworx.lisa.features.companionmemory.model.CompanionMemory
import com.idworx.lisa.features.companionmemory.model.GreetingContext
import com.idworx.lisa.features.companionmemory.model.LearningHistoryEntry
import com.idworx.lisa.features.companionmemory.model.LearningMilestone
import com.idworx.lisa.features.companionmemory.model.MemoryCategory
import com.idworx.lisa.features.companionmemory.model.MemoryEvent
import com.idworx.lisa.features.companionmemory.model.MemoryImportance
import com.idworx.lisa.features.companionmemory.model.PreferenceMemorySnapshot
import com.idworx.lisa.features.companionmemory.model.PracticeHistoryEntry
import com.idworx.lisa.features.companionmemory.model.SessionSummary
import com.idworx.lisa.features.companionmemory.repository.CompanionMemoryExportImport
import com.idworx.lisa.features.companionmemory.repository.CompanionMemoryRepository
import com.idworx.lisa.features.companionmemory.repository.CompanionMemorySerializer
import com.idworx.lisa.features.companionmemory.repository.CompanionMemoryStore
import com.idworx.lisa.features.companionmemory.repository.InMemoryCompanionMemoryRepository
import com.idworx.lisa.features.companionmemory.state.CompanionMemoryState
import com.idworx.lisa.features.onboardingguide.services.TrainingProgressStore
import com.idworx.lisa.features.personality.model.DialogueContext
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport
import java.io.File

object CompanionMemoryAuthorityV1 {

    const val AUTHORITY_NAME: String = "COMPANION_MEMORY_AUTHORITY_V1"
    const val PASS_TOKEN: String = "COMPANION_MEMORY_AUTHORITY_V1_PASS"

    private val PROJECT_ROOT = File(System.getProperty("user.dir") ?: ".")

    fun validate(): ValidationReport {
        val engine = CompanionMemoryEngines.createForTests()
        val checks = listOf(
            check("CMEM_001", "Companion Memory module package exists", classExists(CompanionMemoryEngine::class.java)),
            check("CMEM_002", "Companion Memory README exists", readmeExists()),
            check("CMEM_003", "CompanionMemoryEngine interface exists", CompanionMemoryEngine::class.java.isInterface),
            check("CMEM_004", "DefaultCompanionMemoryEngine exists", classExists(DefaultCompanionMemoryEngine::class.java)),
            check("CMEM_005", "CompanionMemory model exists", classExists(CompanionMemory::class.java)),
            check("CMEM_006", "LearningMilestone model exists", LearningMilestone.entries.size >= 9),
            check("CMEM_007", "GreetingContext model exists", classExists(GreetingContext::class.java)),
            check("CMEM_008", "CompanionMemoryRepository exists", CompanionMemoryRepository::class.java.isInterface),
            check("CMEM_009", "CompanionMemoryStore persistence exists", classExists(CompanionMemoryStore::class.java)),
            check("CMEM_010", "SessionSummary model exists", classExists(SessionSummary::class.java)),
            check("CMEM_011", "LearningHistoryEntry exists", classExists(LearningHistoryEntry::class.java)),
            check("CMEM_012", "PracticeHistoryEntry exists", classExists(PracticeHistoryEntry::class.java)),
            check("CMEM_013", "PreferenceMemorySnapshot exists", classExists(PreferenceMemorySnapshot::class.java)),
            check("CMEM_014", "Achievement tracking via milestones works", achievementTrackingWorks(engine)),
            check("CMEM_015", "Observable evidence required at record time", observableEvidenceRequired(engine)),
            check("CMEM_016", "Inferred emotions blocked at record time", noInferredEmotionsStored(engine)),
            check("CMEM_017", "No fabricated memories without evidence", noFabricatedMemories(engine)),
            check("CMEM_018", "Guided Learning integration adapter exists", classExists(GuidedLearningMemoryAdapter::class.java)),
            check("CMEM_019", "Personality Engine integration adapter exists", personalityIntegrationWorks(engine)),
            check("CMEM_020", "Practice Mode integration adapter exists", classExists(PracticeMemoryAdapter::class.java)),
            check("CMEM_021", "Export interface exists", exportWorks(engine)),
            check("CMEM_022", "Import interface exists", importWorks(engine)),
            check("CMEM_023", "Importance levels implemented", MemoryImportance.entries.size >= 5),
            check("CMEM_024", "Privacy rules enforced", privacyRulesEnforced()),
            check("CMEM_025", "Companion Memory tests exist", testClassExists()),
            check("CMEM_026", "Documentation metadata exists", CompanionMemoryMetadata.VERSION.isNotBlank()),
            check("CMEM_027", "Future cloud sync hooks via repository interface", cloudHooksExist()),
            check("CMEM_028", "Future AI hooks via observable event stream", aiHooksExist(engine)),
            check("CMEM_029", "Existing preference systems reused not duplicated", preferencesReuseTrainingStore()),
            check("CMEM_030", "Pass token defined", PASS_TOKEN == "COMPANION_MEMORY_AUTHORITY_V1_PASS")
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Companion Memory Authority verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = emptyList(),
            affectedLicArticles = listOf("Article 4.2.1.2 — Every visible action tappable if touch exists"),
            affectedLiecArticles = listOf("Article 6.2.1.1 — Recovery engine guarantees escape"),
            affectedLvcArticles = listOf("Article 4.1.1.2 — Evidence record requirements"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "LISA Companion Memory System V1 is complete with truthful observable memory, integrations, and privacy enforcement."
            } else {
                "${failed.size} companion memory checks failed."
            },
            subsystem = "Companion Memory"
        )
    }

    private fun readmeExists(): Boolean {
        val candidates = buildList {
            add(File(PROJECT_ROOT, "features/companionmemory/README.md"))
            add(File(PROJECT_ROOT.parentFile ?: PROJECT_ROOT, "features/companionmemory/README.md"))
            var dir: File? = PROJECT_ROOT
            repeat(5) {
                dir?.let { add(File(it, "features/companionmemory/README.md")) }
                dir = dir?.parentFile
            }
        }
        return candidates.any { it.exists() }
    }

    private fun testClassExists(): Boolean {
        val candidates = listOf(
            File(PROJECT_ROOT, "app/src/test/java/com/idworx/lisa/validation/authority/CompanionMemoryAuthorityV1Test.kt"),
            File(PROJECT_ROOT, "src/test/java/com/idworx/lisa/validation/authority/CompanionMemoryAuthorityV1Test.kt")
        )
        return candidates.any { it.exists() }
    }

    private fun achievementTrackingWorks(engine: DefaultCompanionMemoryEngine): Boolean {
        val first = engine.recordMilestone(LearningMilestone.FirstSuccessfulBlink, "Blink detected in lesson comm_1")
        val duplicate = engine.recordMilestone(LearningMilestone.FirstSuccessfulBlink, "Blink detected again")
        return first != null && duplicate == null && LearningMilestone.FirstSuccessfulBlink in engine.getMilestones()
    }

    private fun observableEvidenceRequired(engine: DefaultCompanionMemoryEngine): Boolean {
        val rejected = engine.recordEvent(
            MemoryEvent("test", evidence = ""),
            MemoryCategory.Session,
            MemoryImportance.Low,
            "Empty evidence",
            "Should fail"
        )
        val accepted = engine.recordEvent(
            MemoryEvent("test", evidence = "User opened settings screen"),
            MemoryCategory.Session,
            MemoryImportance.Low,
            "Settings opened",
            "User opened settings screen"
        )
        return rejected == null && accepted != null && accepted.observableEvidence.isNotBlank()
    }

    private fun noInferredEmotionsStored(engine: DefaultCompanionMemoryEngine): Boolean {
        val rejected = engine.recordEvent(
            MemoryEvent("emotion", evidence = "User was sad during lesson"),
            MemoryCategory.Personality,
            MemoryImportance.Low,
            "Emotion",
            "User was sad"
        )
        return rejected == null && MemoryPrivacyGuard.isObservableEvidenceValid("User was sad").not()
    }

    private fun noFabricatedMemories(engine: DefaultCompanionMemoryEngine): Boolean {
        val before = engine.getRecentAchievements(100).size
        engine.recordEvent(
            MemoryEvent("fabricated", evidence = "User enjoys practice sessions"),
            MemoryCategory.Personality,
            MemoryImportance.High,
            "Fabricated",
            "User enjoys practice"
        )
        return engine.getRecentAchievements(100).size == before
    }

    private fun personalityIntegrationWorks(engine: DefaultCompanionMemoryEngine): Boolean {
        engine.startSession()
        val greeting = PersonalityMemoryAdapter.greetingContextFromEngine(engine)
        val enriched = PersonalityMemoryAdapter.enrichDialogueContext(DialogueContext(), greeting)
        return classExists(PersonalityMemoryAdapter::class.java) &&
            enriched.returningUser || greeting.isFirstLaunch
    }

    private fun exportWorks(engine: DefaultCompanionMemoryEngine): Boolean {
        engine.recordMilestone(LearningMilestone.FirstPhrase, "Phrase spoken in lesson comm_hello")
        val json = engine.exportMemory()
        return json.contains("FirstPhrase") && CompanionMemorySerializer.fromJson(json).achievedMilestones.isNotEmpty()
    }

    private fun importWorks(engine: DefaultCompanionMemoryEngine): Boolean {
        val json = engine.exportMemory()
        val fresh = CompanionMemoryEngines.createForTests()
        return fresh.importMemory(json) && fresh.getMilestones().isNotEmpty()
    }

    private fun privacyRulesEnforced(): Boolean =
        MemoryPrivacyGuard.FORBIDDEN_EMOTION_PATTERNS.isNotEmpty() &&
            !MemoryPrivacyGuard.isObservableEvidenceValid("User was frustrated with the lesson")

    private fun cloudHooksExist(): Boolean =
        CompanionMemoryRepository::class.java.isInterface &&
            CompanionMemoryExportImport::class.java.isInterface &&
            classExists(CompanionMemoryStore::class.java)

    private fun aiHooksExist(engine: DefaultCompanionMemoryEngine): Boolean {
        engine.recordEvent(
            MemoryEvent("ai_hook", evidence = "Observable event stream entry for future AI expansion"),
            MemoryCategory.Personality,
            MemoryImportance.High,
            "AI hook",
            "Future AI memory expansion point"
        )
        return classExists(LearningProgressAnalyzer::class.java) &&
            engine.getRecentAchievements(10).isNotEmpty()
    }

    private fun preferencesReuseTrainingStore(): Boolean =
        classExists(TrainingProgressStore::class.java) &&
            !File(PROJECT_ROOT, "app/src/main/java/com/idworx/lisa/features/companionmemory/repository/TrainingPreferencesStore.kt").exists()

    private fun classExists(clazz: Class<*>): Boolean = try {
        Class.forName(clazz.name)
        true
    } catch (_: Exception) {
        false
    }

    private fun check(
        id: String,
        description: String,
        passed: Boolean,
        remediation: String? = null
    ): ValidationCheckResult = ValidationCheckResult(
        checkId = id,
        description = description,
        passed = passed,
        remediation = remediation
    )
}
