package com.idworx.lisa

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** RC5A — Version 1 emergency path has no caregiver placeholder execution. */
class Rc5AProductionFinalisationTest {

    @Test
    fun mainActivityEmergencyFlowHasNoCaregiverPlaceholder() {
        val source = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertFalse(source.contains("EmergencyNotificationService"))
        assertFalse(source.contains("LisaCaregiverStore"))
        assertFalse(source.contains("uiEmergencyNotifyNames"))
        assertFalse(source.contains("notifyCaregiverPlaceholder"))
    }

    @Test
    fun emergencyOverlayHasNoNotifyNamesUi() {
        val source = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertFalse(source.contains("notifyNames"))
        assertFalse(source.contains("wouldNotify"))
        assertTrue(source.contains("EmergencyOverlay"))
        assertTrue(source.contains("callingForHelp"))
    }

    @Test
    fun emergencyArchitectureHasNoPlaceholderService() {
        val source = readSource("app/src/main/java/com/idworx/lisa/EmergencyArchitecture.kt")
        assertFalse(source.contains("EmergencyNotificationService"))
        assertFalse(source.contains("notifyCaregiverPlaceholder"))
        assertFalse(source.contains("android.util.Log"))
        assertTrue(source.contains("EMERGENCY_LEFT_WINKS"))
    }

    @Test
    fun version2CaregiverFoundationIsIsolated() {
        val source = readSource(
            "app/src/main/java/com/idworx/lisa/v2/foundation/CaregiverLinkingFoundation.kt"
        )
        assertTrue(source.contains("Version 2 foundation"))
        assertTrue(source.contains("class LisaCaregiverStore"))
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
}
