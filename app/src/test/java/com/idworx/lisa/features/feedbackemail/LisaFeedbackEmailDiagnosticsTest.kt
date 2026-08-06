package com.idworx.lisa.features.feedbackemail

import com.idworx.lisa.MenuFeedbackDraft
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LisaFeedbackEmailDiagnosticsTest {

    private val sampleDiagnostics =
        LisaFeedbackEmailAuthority.FeedbackDiagnostics(
            build = "Release",
            sensitivity = "5",
            responseTime = "2s",
            normallyUsesGlasses = "YES",
            language = "English",
            camera = "Ready",
            face = "Detected",
            eyes = "Not Detected",
            date = "2026-07-18",
            time = "07:17",
            timeZone = "Africa/Johannesburg"
        )

    @Test
    fun subjectIncludesVersionGlassesAndDevice() {
        val subject = LisaFeedbackEmailAuthority.buildSubject(
            versionName = "1.1",
            glassesLabel = "YES",
            manufacturer = "samsung",
            model = "SM-N986N"
        )
        assertEquals(
            "LISA Feedback | v1.1 | Glasses: YES | samsung SM-N986N",
            subject
        )
    }

    @Test
    fun subjectUsesUnknownGlassesWhenUnset() {
        val subject = LisaFeedbackEmailAuthority.buildSubject(
            versionName = "1.1",
            glassesLabel = "UNKNOWN",
            manufacturer = "samsung",
            model = "SM-N986N"
        )
        assertEquals(
            "LISA Feedback | v1.1 | Glasses: UNKNOWN | samsung SM-N986N",
            subject
        )
    }

    @Test
    fun prepareUsesGroupedLayoutWithCommunicationLanguage() {
        val prepared = LisaFeedbackEmailAuthority.prepare(
            draft = MenuFeedbackDraft(workedWell = "eye tracking"),
            versionName = "1.1",
            androidRelease = "14",
            androidSdk = 34,
            manufacturer = "samsung",
            model = "SM-N986N",
            diagnostics = sampleDiagnostics
        )
        assertEquals(
            "LISA Feedback | v1.1 | Glasses: YES | samsung SM-N986N",
            prepared.subject
        )
        assertTrue(prepared.body.startsWith("LISA Version: 1.1\n"))
        assertTrue(prepared.body.contains("Android Version: 14 (SDK 34)\n"))
        assertTrue(prepared.body.contains("Device Model: samsung SM-N986N\n"))
        assertTrue(prepared.body.contains("TECHNICAL INFORMATION"))
        assertTrue(prepared.body.contains("USER FEEDBACK"))
        assertTrue(prepared.body.contains("END OF REPORT"))
        assertTrue(prepared.body.contains("Build Type: Release\n"))
        assertTrue(prepared.body.contains("Sensitivity: 5\n"))
        assertTrue(prepared.body.contains("Response Time: 2s\n"))
        assertTrue(prepared.body.contains("Normally Uses Glasses: YES\n"))
        assertTrue(prepared.body.contains("Communication Language: English\n"))
        assertFalse(prepared.body.contains("\nLanguage:\n"))
        assertFalse(prepared.body.contains("\nBuild: "))
        assertFalse(prepared.body.contains("Build:\nRelease\n"))
        assertTrue(prepared.body.contains("Camera: Ready\n"))
        assertTrue(prepared.body.contains("Face: Detected\n"))
        assertTrue(prepared.body.contains("Eyes: Not Detected\n"))
        assertTrue(prepared.body.contains("Date: 2026-07-18\n"))
        assertTrue(prepared.body.contains("Time: 07:17\n"))
        assertTrue(prepared.body.contains("Time Zone: Africa/Johannesburg\n"))
        // Blank line after USER FEEDBACK header, then first question.
        assertTrue(
            prepared.body.contains(
                "USER FEEDBACK\n========================================\n\nWhat worked well:\n"
            )
        )
        // One blank line after every feedback response.
        assertTrue(prepared.body.contains("What worked well:\neye tracking\n\nWhat was confusing:\n"))
        assertTrue(
            prepared.body.contains("What was confusing:\n(not provided)\n\nWink detection feedback:\n")
        )
        assertTrue(
            prepared.body.contains(
                "Wink detection feedback:\n(not provided)\n\nSpeech timing feedback:\n"
            )
        )
        assertTrue(
            prepared.body.contains(
                "Speech timing feedback:\n(not provided)\n\nSteps to reproduce:\n"
            )
        )
        assertTrue(
            prepared.body.contains("Steps to reproduce:\n(not provided)\n\nExpected behaviour:\n")
        )
        assertTrue(
            prepared.body.contains("Expected behaviour:\n(not provided)\n\nActual behaviour:\n")
        )
        assertTrue(
            prepared.body.contains("Actual behaviour:\n(not provided)\n\n========================================\nEND OF REPORT\n")
        )
        // Technical block stays single-line with no blank lines between fields.
        assertTrue(
            prepared.body.contains(
                "Build Type: Release\nSensitivity: 5\nResponse Time: 2s\nNormally Uses Glasses: YES\n"
            )
        )
        assertFalse(prepared.body.contains("Android ID"))
        assertFalse(prepared.body.contains("GPS"))
        assertFalse(prepared.body.contains("biometric", ignoreCase = true))
    }

    @Test
    fun missingValuesBecomeUnknownNeverBlank() {
        val diagnostics = LisaFeedbackEmailAuthority.FeedbackDiagnostics.fromOperationalState(
            isDebugBuild = true,
            sensitivityLevel = null,
            responseTimeSec = null,
            normallyUsesGlasses = null,
            languageLabel = "  ",
            cameraReady = null,
            faceDetected = null,
            eyesDetected = null,
            nowMillis = 0L
        )
        assertEquals("Debug", diagnostics.build)
        assertEquals("Unknown", diagnostics.sensitivity)
        assertEquals("Unknown", diagnostics.responseTime)
        assertEquals("UNKNOWN", diagnostics.normallyUsesGlasses)
        assertEquals("Unknown", diagnostics.language)
        assertEquals("Unknown", diagnostics.camera)
        assertEquals("Unknown", diagnostics.face)
        assertEquals("Unknown", diagnostics.eyes)
        assertTrue(diagnostics.date.isNotBlank())
        assertTrue(diagnostics.time.isNotBlank())
        assertTrue(diagnostics.timeZone.isNotBlank())

        val prepared = LisaFeedbackEmailAuthority.prepare(
            draft = MenuFeedbackDraft(),
            versionName = "",
            androidRelease = null,
            androidSdk = null,
            manufacturer = "",
            model = "",
            diagnostics = diagnostics.copy(date = "", time = "", timeZone = "  ")
        )
        assertTrue(prepared.subject.contains("Glasses: UNKNOWN"))
        assertTrue(prepared.body.contains("LISA Version: Unknown\n"))
        assertTrue(prepared.body.contains("Android Version: Unknown (SDK Unknown)\n"))
        assertTrue(prepared.body.contains("Device Model: Unknown\n"))
        assertTrue(prepared.body.contains("Communication Language: Unknown\n"))
        assertTrue(prepared.body.contains("Date: Unknown\n"))
        assertTrue(prepared.body.contains("Time: Unknown\n"))
        assertTrue(prepared.body.contains("Time Zone: Unknown\n"))
    }

    @Test
    fun reviewAndSendWordingReplacesEmailAssumption() {
        val strings = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/LisaUiStrings.kt"
        ) ?: error("missing strings")
        assertTrue(strings.contains("\"Review and Send\""))
        assertFalse(strings.contains("Review and send by email"))
        val privacy = ZeroTouchFileProbe.readProjectFile("PRIVACY.md")
            ?: error("missing PRIVACY.md")
        assertTrue(privacy.contains("Review and Send"))
        assertFalse(privacy.contains("Review and send by email"))
    }
}
