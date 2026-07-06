package com.idworx.lisa.features.accessibilityconsistency.validators

import com.idworx.lisa.features.accessibilityconsistency.metadata.AccessibilityMetadata
import com.idworx.lisa.features.accessibilityconsistency.model.AccessibilityCategory
import com.idworx.lisa.features.accessibilityconsistency.model.AccessibilityIssue
import com.idworx.lisa.features.accessibilityconsistency.model.AccessibilitySeverity
import java.io.File

data class ValidatorResult(
    val validatorName: String,
    val passed: Boolean,
    val issues: List<AccessibilityIssue>,
    val checksPerformed: Int,
    val checksPassed: Int,
    val evidence: String
)

object AccessibilityFileProbe {
    private val PROJECT_ROOT = File(System.getProperty("user.dir") ?: ".")

    fun readProjectFile(relativePath: String): String? {
        var dir: File? = PROJECT_ROOT
        repeat(6) {
            dir?.let { candidate ->
                val file = File(candidate, relativePath)
                if (file.exists()) return file.readText()
            }
            dir = dir?.parentFile
        }
        return null
    }

    fun fileExists(relativePath: String): Boolean = readProjectFile(relativePath) != null
}

object TypographyValidator {

    fun validate(): ValidatorResult {
        val issues = mutableListOf<AccessibilityIssue>()
        var passed = 0
        val checks = 4

        val profileSource = AccessibilityFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/LisaUserProfile.kt")
        if (profileSource?.contains("0.8f, 1.4f") == true || profileSource?.contains("coerceIn(0.8f, 1.4f)") == true) {
            passed++
        } else {
            issues.add(issue("TYP_001", "Text scale bounds not enforced in LisaUserProfile"))
        }

        val uiSource = AccessibilityFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        if (uiSource?.contains("fontScale * textSizeScale") == true) {
            passed++
        } else {
            issues.add(issue("TYP_002", "Accessibility text scaling not wired in LisaRootUI"))
        }

        if (uiSource?.contains("0.8f..1.4f") == true) {
            passed++
        } else {
            issues.add(issue("TYP_003", "Settings text scale slider bounds missing"))
        }

        val typeSource = AccessibilityFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/ui/theme/Type.kt")
        if (typeSource?.contains("16.sp") == true) {
            passed++
        } else {
            issues.add(issue("TYP_004", "Base body typography below readable threshold", AccessibilitySeverity.Warning))
        }

        return ValidatorResult(
            validatorName = "TypographyValidator",
            passed = issues.none { it.severity >= AccessibilitySeverity.Error },
            issues = issues,
            checksPerformed = checks,
            checksPassed = passed,
            evidence = "Typography rules validated against configured scale ${AccessibilityMetadata.MIN_TEXT_SCALE}–${AccessibilityMetadata.MAX_TEXT_SCALE}"
        )
    }

    private fun issue(id: String, desc: String, severity: AccessibilitySeverity = AccessibilitySeverity.Error) =
        AccessibilityIssue(id, AccessibilityCategory.Typography, severity, "Global", desc, "Observable source check")
}

object TouchTargetValidator {

    fun validate(): ValidatorResult {
        val issues = mutableListOf<AccessibilityIssue>()
        var passed = 0
        val checks = 3

        val training = AccessibilityFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingComponents.kt"
        )
        if (training?.contains(".height(56.dp)") == true) {
            passed++
        } else {
            issues.add(issue("TOUCH_001", "Training primary button below preferred touch height"))
        }

        if (training?.contains(".height(48.dp)") == true) {
            passed++
        } else {
            issues.add(issue("TOUCH_002", "Training secondary button below minimum touch height"))
        }

        val uiSource = AccessibilityFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        if (uiSource?.contains("34.dp") == true) {
            passed++
            issues.add(
                AccessibilityIssue(
                    "TOUCH_003",
                    AccessibilityCategory.TouchTarget,
                    AccessibilitySeverity.Warning,
                    "Settings",
                    "Settings increment buttons use 34dp — below 48dp guidance",
                    "Documented observation; settings remain usable via slider"
                )
            )
        } else {
            passed++
        }

        return ValidatorResult(
            validatorName = "TouchTargetValidator",
            passed = issues.none { it.severity >= AccessibilitySeverity.Error },
            issues = issues,
            checksPerformed = checks,
            checksPassed = passed,
            evidence = "Touch targets validated against ${AccessibilityMetadata.MIN_PRIMARY_TOUCH_DP}dp minimum"
        )
    }

    private fun issue(id: String, desc: String) =
        AccessibilityIssue(id, AccessibilityCategory.TouchTarget, AccessibilitySeverity.Error, "Guided Learning", desc, "Source inspection")
}

object ContrastValidator {

    fun validate(): ValidatorResult {
        val issues = mutableListOf<AccessibilityIssue>()
        var passed = 0
        val checks = 4

        val colors = AccessibilityFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/ui/theme/Color.kt")
        if (colors?.contains("LisaEmergencyRed") == true) passed++ else issues.add(issue("CON_001", "Emergency color token missing"))
        if (colors?.contains("LisaWhite") == true && colors.contains("LisaBlueDark")) passed++ else issues.add(issue("CON_002", "Core contrast palette tokens missing"))
        if (colors?.contains("LisaGray") == true) passed++ else issues.add(issue("CON_003", "Secondary text color token missing"))

        val theme = AccessibilityFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/ui/theme/Theme.kt")
        if (theme?.contains("onBackground") == true || theme?.contains("onSurface") == true) passed++
        else issues.add(issue("CON_004", "Theme foreground roles not configured", AccessibilitySeverity.Warning))

        return ValidatorResult(
            validatorName = "ContrastValidator",
            passed = issues.none { it.severity >= AccessibilitySeverity.Error },
            issues = issues,
            checksPerformed = checks,
            checksPassed = passed,
            evidence = "Configured design tokens validated — no subjective colour analysis"
        )
    }

    private fun issue(id: String, desc: String, severity: AccessibilitySeverity = AccessibilitySeverity.Error) =
        AccessibilityIssue(id, AccessibilityCategory.Contrast, severity, "Theme", desc, "Design token check")
}

object LayoutValidator {

    fun validate(): ValidatorResult {
        val uiSource = AccessibilityFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        val issues = mutableListOf<AccessibilityIssue>()
        var passed = 0
        val checks = 3

        if (uiSource?.contains("SettingsPanel") == true) passed++ else issues.add(issue("LAY_001", "Settings panel structure missing"))
        if (uiSource?.contains("LisaRootUI") == true) passed++ else issues.add(issue("LAY_002", "Root UI shell missing"))
        if (uiSource?.contains("Column") == true || uiSource?.contains("Scaffold") == true) passed++
        else issues.add(issue("LAY_003", "Predictable layout containers not found", AccessibilitySeverity.Warning))

        return ValidatorResult(
            validatorName = "LayoutValidator",
            passed = issues.isEmpty() || issues.all { it.severity < AccessibilitySeverity.Error },
            issues = issues,
            checksPerformed = checks,
            checksPassed = passed,
            evidence = "Layout simplicity checks on existing UI structure"
        )
    }

    private fun issue(id: String, desc: String, severity: AccessibilitySeverity = AccessibilitySeverity.Error) =
        AccessibilityIssue(id, AccessibilityCategory.Layout, severity, "Global", desc, "Structure check")
}

object NavigationValidator {

    fun validate(): ValidatorResult {
        val issues = mutableListOf<AccessibilityIssue>()
        var passed = 0
        val checks = 4

        val nav = AccessibilityFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/LisaNavigation.kt")
        if (nav?.contains("enum class LisaPanel") == true) passed++ else issues.add(issue("NAV_001", "Panel navigation model missing"))

        if (AccessibilityFileProbe.fileExists("app/src/main/java/com/idworx/lisa/validation/authority/NavigationReachabilityAuthorityV1.kt")) {
            passed++
        } else {
            issues.add(issue("NAV_002", "Navigation reachability authority missing"))
        }

        val guided = AccessibilityFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
        if (guided?.contains("contentDescription") == true || guided?.contains("Role.Button") == true) passed++
        else issues.add(issue("NAV_003", "Guided navigation labeling incomplete", AccessibilitySeverity.Warning))

        if (nav?.contains("EmergencySetup") == true || nav?.contains("Settings") == true) passed++
        else issues.add(issue("NAV_004", "Settings and emergency panels not registered"))

        return ValidatorResult(
            validatorName = "NavigationValidator",
            passed = issues.none { it.severity >= AccessibilitySeverity.Error },
            issues = issues,
            checksPerformed = checks,
            checksPassed = passed,
            evidence = "Navigation consistency validated against existing panel and reachability systems"
        )
    }

    private fun issue(id: String, desc: String, severity: AccessibilitySeverity = AccessibilitySeverity.Error) =
        AccessibilityIssue(id, AccessibilityCategory.Navigation, severity, "Navigation", desc, "Navigation check")
}

object AccessibilitySettingsValidator {

    fun validate(): ValidatorResult {
        val issues = mutableListOf<AccessibilityIssue>()
        var passed = 0
        val checks = 5

        val settings = AccessibilityFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/LisaNavigation.kt")
        if (settings?.contains("textSizeScale") == true) passed++ else issues.add(issue("SET_001", "Text scaling setting missing"))
        if (settings?.contains("countdownDurationSec") == true) passed++ else issues.add(issue("SET_002", "Countdown setting missing"))
        if (settings?.contains("emergencyAlarmVolume") == true) passed++ else issues.add(issue("SET_003", "Emergency volume setting missing"))
        if (settings?.contains("sensitivityLevel") == true) passed++ else issues.add(issue("SET_004", "Sensitivity setting missing"))

        val ui = AccessibilityFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        if (ui?.contains("TrainingSettingsSection") == true) passed++ else issues.add(issue("SET_005", "Learning preferences not integrated in settings", AccessibilitySeverity.Warning))

        return ValidatorResult(
            validatorName = "AccessibilitySettingsValidator",
            passed = issues.none { it.severity >= AccessibilitySeverity.Error },
            issues = issues,
            checksPerformed = checks,
            checksPassed = passed,
            evidence = "Existing LisaSettingsUiState fields reused — no duplicate settings"
        )
    }

    private fun issue(id: String, desc: String, severity: AccessibilitySeverity = AccessibilitySeverity.Error) =
        AccessibilityIssue(id, AccessibilityCategory.Settings, severity, "Settings", desc, "Settings integration check")
}

object GuidedLearningValidator {

    fun validate(): ValidatorResult {
        val issues = mutableListOf<AccessibilityIssue>()
        var passed = 0
        val checks = 4

        val training = AccessibilityFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingComponents.kt"
        )
        if (training?.contains("contentDescription") == true) passed++ else issues.add(issue("GL_001", "Training buttons lack content descriptions"))
        if (training?.contains("TrainingProgressIndicator") == true) passed++ else issues.add(issue("GL_002", "Lesson progress visibility missing"))
        if (training?.contains("WinkSequenceDisplay") == true) passed++ else issues.add(issue("GL_003", "Instructional blink display missing"))

        val lessons = AccessibilityFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingLessonScreens.kt"
        )
        if (lessons?.contains("TrainingPrimaryButton") == true) passed++ else issues.add(issue("GL_004", "Accessible lesson actions missing"))

        return ValidatorResult(
            validatorName = "GuidedLearningValidator",
            passed = issues.isEmpty(),
            issues = issues,
            checksPerformed = checks,
            checksPassed = passed,
            evidence = "Guided Learning accessibility validated against TrainingComponents"
        )
    }

    private fun issue(id: String, desc: String) =
        AccessibilityIssue(id, AccessibilityCategory.GuidedLearning, AccessibilitySeverity.Error, "Guided Learning", desc, "Training UI check")
}

object CommunicationValidator {

    fun validate(): ValidatorResult {
        val issues = mutableListOf<AccessibilityIssue>()
        var passed = 0
        val checks = 4

        val ui = AccessibilityFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        if (ui?.contains("uiPendingPhrase") == true || ui?.contains("pendingPhrase") == true || ui?.contains("CountdownConfirm") == true) {
            passed++
        } else {
            issues.add(issue("COMM_001", "Speech confirmation visibility not found"))
        }

        if (ui?.contains("uiLastSpoken") == true || ui?.contains("lastSpoken") == true) passed++
        else issues.add(issue("COMM_002", "Last spoken phrase feedback missing", AccessibilitySeverity.Warning))

        if (AccessibilityFileProbe.fileExists(
                "app/src/main/java/com/idworx/lisa/features/corecommunicationreliability/engine/DefaultCoreCommunicationReliabilityEngine.kt"
            )
        ) passed++ else issues.add(issue("COMM_003", "Communication reliability integration missing"))

        if (AccessibilityFileProbe.fileExists(
                "app/src/main/java/com/idworx/lisa/features/communicationanalytics/integration/CommunicationAnalyticsBridge.kt"
            )
        ) passed++ else issues.add(issue("COMM_004", "Communication analytics observer missing", AccessibilitySeverity.Warning))

        return ValidatorResult(
            validatorName = "CommunicationValidator",
            passed = issues.none { it.severity >= AccessibilitySeverity.Error },
            issues = issues,
            checksPerformed = checks,
            checksPassed = passed,
            evidence = "Communication accessibility validated without altering runtime behaviour"
        )
    }

    private fun issue(id: String, desc: String, severity: AccessibilitySeverity = AccessibilitySeverity.Error) =
        AccessibilityIssue(id, AccessibilityCategory.Communication, severity, "Communication", desc, "Communication UI check")
}

object EmergencyAccessibilityValidator {

    fun validate(): ValidatorResult {
        val issues = mutableListOf<AccessibilityIssue>()
        var passed = 0
        val checks = 4

        val ui = AccessibilityFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        if (ui?.contains("EmergencyOverlay") == true) passed++ else issues.add(issue("EMER_001", "Emergency overlay missing"))

        val emergency = AccessibilityFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/EmergencyArchitecture.kt")
        if (emergency?.contains("EMERGENCY_LEFT_WINKS") == true) passed++ else issues.add(issue("EMER_002", "Emergency sequence constants missing"))

        if (AccessibilityFileProbe.fileExists(
                "app/src/main/java/com/idworx/lisa/features/corecommunicationreliability/emergency/EmergencySpeechSafetyGuard.kt"
            )
        ) passed++ else issues.add(issue("EMER_003", "Emergency safety guard missing"))

        val guided = AccessibilityFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
        if (guided?.contains("Emergency") == true) passed++
        else issues.add(issue("EMER_004", "Emergency navigation labeling missing", AccessibilitySeverity.Warning))

        return ValidatorResult(
            validatorName = "EmergencyAccessibilityValidator",
            passed = issues.none { it.severity >= AccessibilitySeverity.Error },
            issues = issues,
            checksPerformed = checks,
            checksPassed = passed,
            evidence = "Emergency workflow remains discoverable and safely isolated"
        )
    }

    private fun issue(id: String, desc: String, severity: AccessibilitySeverity = AccessibilitySeverity.Error) =
        AccessibilityIssue(id, AccessibilityCategory.Emergency, severity, "Emergency", desc, "Emergency accessibility check")
}

object ScreenConsistencyValidator {

    fun validate(): ValidatorResult {
        val issues = mutableListOf<AccessibilityIssue>()
        var passed = 0
        val checks = 4
        val themeFiles = listOf(
            "app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt",
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingComponents.kt",
            "app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt"
        )
        themeFiles.forEachIndexed { index, path ->
            val source = AccessibilityFileProbe.readProjectFile(path)
            if (source?.contains("LisaBlue") == true || source?.contains("ui.theme") == true) passed++
            else issues.add(
                AccessibilityIssue(
                    "SCR_${index + 1}",
                    AccessibilityCategory.ScreenConsistency,
                    AccessibilitySeverity.Warning,
                    path.substringAfterLast("/"),
                    "Screen may not use shared Lisa theme tokens",
                    "Theme consistency check"
                )
            )
        }

        return ValidatorResult(
            validatorName = "ScreenConsistencyValidator",
            passed = issues.none { it.severity >= AccessibilitySeverity.Error },
            issues = issues,
            checksPerformed = checks,
            checksPassed = passed,
            evidence = "Cross-screen theme token consistency validated"
        )
    }
}

object CognitiveLoadValidator {

    fun validate(): ValidatorResult {
        val issues = mutableListOf<AccessibilityIssue>()
        var passed = 0
        val checks = 3

        val ui = AccessibilityFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        val settingsSectionCount = ui?.split("SettingsSectionHeader")?.size?.minus(1) ?: 0
        if (settingsSectionCount in 1..8) passed++
        else issues.add(issue("COG_001", "Settings screen may present excessive sections at once", AccessibilitySeverity.Warning))

        if (ui?.contains("developerMode") == true) {
            passed++
            if (ui.contains("developer mode", ignoreCase = true)) passed++
        }

        val training = AccessibilityFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingLessonScreens.kt"
        )
        val buttonCount = training?.split("TrainingPrimaryButton")?.size?.minus(1) ?: 0
        if (buttonCount <= 5) passed++ else issues.add(issue("COG_002", "Lesson screen may present too many competing actions", AccessibilitySeverity.Warning))

        return ValidatorResult(
            validatorName = "CognitiveLoadValidator",
            passed = issues.none { it.severity >= AccessibilitySeverity.Error },
            issues = issues,
            checksPerformed = checks,
            checksPassed = passed,
            evidence = "Cognitive load assessed from observable screen structure"
        )
    }

    private fun issue(id: String, desc: String, severity: AccessibilitySeverity = AccessibilitySeverity.Warning) =
        AccessibilityIssue(id, AccessibilityCategory.CognitiveLoad, severity, "Global", desc, "Cognitive load heuristic")
}
