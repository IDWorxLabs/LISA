package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** RC6 — production menu cleanup and device check relocation. */
class Rc6ProductionMenuCleanupTest {

    private val english = LisaUiStrings(PreferredLanguage.English)

    @Test
    fun productionMenuExcludesRepeatLastPhrase() {
        val source = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        val menuPanel = extractFunction(source, "private fun MenuPanel")
        assertFalse(menuPanel.contains("repeatLastPhrase"))
        assertFalse(menuPanel.contains("canRepeat"))
    }

    @Test
    fun productionMenuExcludesDeviceChecklistAndDeveloperTools() {
        val source = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        val menuPanel = extractFunction(source, "private fun MenuPanel")
        assertFalse(menuPanel.contains("LisaPanel.TestingChecklist"))
        assertFalse(menuPanel.contains("LisaPanel.DeveloperTools"))
        assertFalse(menuPanel.contains("menuSectionDeveloper"))
    }

    @Test
    fun productionMenuHasOnlyEverydaySections() {
        val source = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        val menuPanel = extractFunction(source, "private fun MenuPanel")
        assertTrue(menuPanel.contains("menuSectionCommunication"))
        assertTrue(menuPanel.contains("menuSectionApplication"))
        assertTrue(menuPanel.contains("menuSectionSupport"))
        assertTrue(menuPanel.contains("LisaPanel.ReleaseNotes"))
    }

    @Test
    fun settingsContainsRunDeviceCheck() {
        val source = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertTrue(source.contains("private fun SettingsPanel"))
        assertTrue(source.contains("settingsSectionSupportDiagnostics"))
        assertTrue(source.contains("runDeviceCheckTitle"))
        assertTrue(source.contains("runDeviceCheckSubtitle"))
        assertTrue(source.contains("onOpenDeviceCheck"))
    }

    @Test
    fun settingsOpensExistingDeviceChecklistPanel() {
        val mainActivity = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(mainActivity.contains("onOpenDeviceCheck = { openPanel(LisaPanel.TestingChecklist, LisaPanel.Settings) }"))
        val uiSource = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertTrue(uiSource.contains("LisaPanel.TestingChecklist -> TestingChecklistPanel"))
    }

    @Test
    fun deviceChecklistItemsUnchanged() {
        assertEquals(9, TestingChecklistItem.entries.size)
        assertTrue(TestingChecklistItem.entries.any { it.key == "camera_opens" })
        assertTrue(TestingChecklistItem.entries.any { it.key == "emergency_alarm_tested" })
    }

    @Test
    fun mainCommunicationRepeatButtonRemains() {
        val source = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertTrue(source.contains("LisaActionButton"))
        assertTrue(source.contains("uiStrings.repeat"))
    }

    @Test
    fun developerToolsGatedForDebugBuildsInSettings() {
        val source = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        val settingsStart = source.indexOf("private fun SettingsPanel")
        assertTrue(settingsStart >= 0)
        val settingsPanel = source.substring(settingsStart)
        assertTrue(settingsPanel.contains("BuildConfig.DEBUG"))
        assertTrue(settingsPanel.contains("onOpenDeveloperTools"))
        assertTrue(settingsPanel.contains("developerTools"))
    }

    @Test
    fun deviceCheckReturnsToSettings() {
        val mainActivity = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(mainActivity.contains("uiPanelReturnTarget"))
        assertTrue(mainActivity.contains("navigateBackFromPanel"))
        assertTrue(mainActivity.contains("openPanel(LisaPanel.TestingChecklist, LisaPanel.Settings)"))
    }

    @Test
    fun removedRc4Rc5MenuEntriesDoNotReappear() {
        val source = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        val menuPanel = extractFunction(source, "private fun MenuPanel")
        assertFalse(menuPanel.contains("CaregiverLinking"))
        assertFalse(menuPanel.contains("EmergencySetup"))
        assertFalse(menuPanel.contains("CommunicationSetup"))
        assertFalse(menuPanel.contains("Assistance"))
    }

    @Test
    fun supportDiagnosticsStringsLocalized() {
        assertEquals("Support & Diagnostics", english.settingsSectionSupportDiagnostics)
        assertEquals("Run Device Check", english.runDeviceCheckTitle)
        assertTrue(english.runDeviceCheckSubtitle.contains("camera", ignoreCase = true))
        assertTrue(english.runDeviceCheckSubtitle.contains("emergency alarm", ignoreCase = true))
    }

    private fun readSource(relativePath: String): String {
        val normalized = relativePath.replace('/', File.separatorChar)
        val roots = listOfNotNull(
            File(System.getProperty("user.dir")),
            File(System.getProperty("user.dir")).parentFile
        )
        for (root in roots) {
            val direct = root.resolve(normalized)
            if (direct.isFile) return direct.readText()
            if (normalized.startsWith("app${File.separatorChar}")) {
                val withoutApp = root.resolve(normalized.removePrefix("app${File.separatorChar}"))
                if (withoutApp.isFile) return withoutApp.readText()
            }
        }
        error("Missing source: $relativePath")
    }

    private fun extractFunction(source: String, signature: String): String {
        val start = source.indexOf(signature)
        assertTrue("Expected $signature in LisaAccessibilityUi.kt", start >= 0)
        val openBrace = source.indexOf('{', start)
        var depth = 0
        for (index in openBrace until source.length) {
            when (source[index]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return source.substring(start, index + 1)
                    }
                }
            }
        }
        error("Unterminated function starting at $signature")
    }
}
