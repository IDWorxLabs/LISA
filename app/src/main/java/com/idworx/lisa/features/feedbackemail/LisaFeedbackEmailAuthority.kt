package com.idworx.lisa.features.feedbackemail

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.content.Context
import com.idworx.lisa.LisaAppVersionInfo
import com.idworx.lisa.MenuFeedbackDraft
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * LISA V1 feedback handoff — opens the user’s email app with a pre-filled message.
 * LISA never sends email itself and cannot confirm delivery.
 *
 * Uses a standards-based [Intent.ACTION_SENDTO] `mailto:` URI. Launch is attempted
 * directly; [ActivityNotFoundException] is the only reliable “no email app” signal.
 * Package-visibility [android.content.pm.PackageManager.resolveActivity] pre-checks are
 * intentionally not used — they can return null even when a handler can open the intent.
 */
object LisaFeedbackEmailAuthority {
    const val DESTINATION_EMAIL: String = "lisa-feedback@asgarddynamics.io"
    const val EMAIL_SUBJECT: String = "LISA Feedback"
    private const val UNKNOWN: String = "Unknown"
    private const val SECTION_RULE: String = "========================================"

    /**
     * Operational diagnostics only — no photos, frames, biometrics, phrases, GPS, or contacts.
     * Every field is non-blank; use [UNKNOWN] when a value cannot be determined.
     */
    data class FeedbackDiagnostics(
        val build: String,
        val sensitivity: String,
        val responseTime: String,
        val normallyUsesGlasses: String,
        val language: String,
        val camera: String,
        val face: String,
        val eyes: String,
        val date: String,
        val time: String,
        val timeZone: String
    ) {
        companion object {
            fun glassesLabel(normallyUsesGlasses: Boolean?): String = when (normallyUsesGlasses) {
                true -> "YES"
                false -> "NO"
                null -> "UNKNOWN"
            }

            fun buildTypeLabel(isDebugBuild: Boolean): String =
                if (isDebugBuild) "Debug" else "Release"

            fun cameraLabel(cameraReady: Boolean?): String = when (cameraReady) {
                true -> "Ready"
                false -> "Not Ready"
                null -> UNKNOWN
            }

            fun detectionLabel(detected: Boolean?): String = when (detected) {
                true -> "Detected"
                false -> "Not Detected"
                null -> UNKNOWN
            }

            fun nonBlank(value: String?): String {
                val trimmed = value?.trim().orEmpty()
                return trimmed.ifEmpty { UNKNOWN }
            }

            fun captureClock(nowMillis: Long = System.currentTimeMillis()): Triple<String, String, String> {
                val zone = TimeZone.getDefault()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = zone }
                val timeFormat = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = zone }
                val date = dateFormat.format(Date(nowMillis)).ifBlank { UNKNOWN }
                val time = timeFormat.format(Date(nowMillis)).ifBlank { UNKNOWN }
                val zoneId = zone.id?.trim().orEmpty().ifEmpty { UNKNOWN }
                return Triple(date, time, zoneId)
            }

            fun fromOperationalState(
                isDebugBuild: Boolean,
                sensitivityLevel: Int?,
                responseTimeSec: Int?,
                normallyUsesGlasses: Boolean?,
                languageLabel: String?,
                cameraReady: Boolean?,
                faceDetected: Boolean?,
                eyesDetected: Boolean?,
                nowMillis: Long = System.currentTimeMillis()
            ): FeedbackDiagnostics {
                val (date, time, timeZone) = captureClock(nowMillis)
                return FeedbackDiagnostics(
                    build = buildTypeLabel(isDebugBuild),
                    sensitivity = sensitivityLevel?.toString()?.ifBlank { UNKNOWN } ?: UNKNOWN,
                    responseTime = responseTimeSec?.let { "${it}s" } ?: UNKNOWN,
                    normallyUsesGlasses = glassesLabel(normallyUsesGlasses),
                    language = nonBlank(languageLabel),
                    camera = cameraLabel(cameraReady),
                    face = detectionLabel(faceDetected),
                    eyes = detectionLabel(eyesDetected),
                    date = date,
                    time = time,
                    timeZone = timeZone
                )
            }
        }
    }

    data class PreparedEmail(
        val to: String,
        val subject: String,
        val body: String
    )

    sealed class LaunchResult {
        data object Opened : LaunchResult()
        data object NoCompatibleEmailApp : LaunchResult()
    }

    fun buildSubject(
        versionName: String,
        glassesLabel: String,
        manufacturer: String,
        model: String
    ): String {
        val version = FeedbackDiagnostics.nonBlank(versionName).let { raw ->
            if (raw == UNKNOWN) UNKNOWN
            else if (raw.startsWith("v", ignoreCase = true)) raw
            else "v$raw"
        }
        val glasses = FeedbackDiagnostics.nonBlank(glassesLabel).let {
            if (it == UNKNOWN) "UNKNOWN" else it
        }
        val device = listOf(manufacturer, model)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .ifBlank { UNKNOWN }
        return "$EMAIL_SUBJECT | $version | Glasses: $glasses | $device"
    }

    fun prepare(
        context: Context,
        draft: MenuFeedbackDraft,
        diagnostics: FeedbackDiagnostics
    ): PreparedEmail {
        val version = LisaAppVersionInfo.from(context)
        return prepare(
            draft = draft,
            versionName = version.versionName,
            androidRelease = Build.VERSION.RELEASE,
            androidSdk = Build.VERSION.SDK_INT,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            diagnostics = diagnostics
        )
    }

    /**
     * Pure builder used by production [prepare] and unit tests.
     * Keeps the existing device header and feedback questions; groups diagnostics for readability.
     */
    fun prepare(
        draft: MenuFeedbackDraft,
        versionName: String,
        androidRelease: String?,
        androidSdk: Int?,
        manufacturer: String?,
        model: String?,
        diagnostics: FeedbackDiagnostics
    ): PreparedEmail {
        val safeVersion = FeedbackDiagnostics.nonBlank(versionName)
        val safeRelease = FeedbackDiagnostics.nonBlank(androidRelease)
        val safeSdk = androidSdk?.toString()?.ifBlank { UNKNOWN } ?: UNKNOWN
        val safeManufacturer = manufacturer?.trim().orEmpty()
        val safeModel = model?.trim().orEmpty()
        val deviceModel = listOf(safeManufacturer, safeModel)
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .ifBlank { UNKNOWN }

        val body = buildString {
            appendLine("LISA Version: $safeVersion")
            appendLine("Android Version: $safeRelease (SDK $safeSdk)")
            appendLine("Device Model: $deviceModel")
            appendLine()
            appendSectionHeader("TECHNICAL INFORMATION")
            appendTechnicalField("Build Type", diagnostics.build)
            appendTechnicalField("Sensitivity", diagnostics.sensitivity)
            appendTechnicalField("Response Time", diagnostics.responseTime)
            appendTechnicalField("Normally Uses Glasses", diagnostics.normallyUsesGlasses)
            appendTechnicalField("Communication Language", diagnostics.language)
            appendTechnicalField("Camera", diagnostics.camera)
            appendTechnicalField("Face", diagnostics.face)
            appendTechnicalField("Eyes", diagnostics.eyes)
            appendTechnicalField("Date", diagnostics.date)
            appendTechnicalField("Time", diagnostics.time)
            appendTechnicalField("Time Zone", diagnostics.timeZone)
            appendLine()
            appendSectionHeader("USER FEEDBACK")
            appendFeedbackField("What worked well", draft.workedWell)
            appendFeedbackField("What was confusing", draft.confusing)
            appendFeedbackField("Wink detection feedback", draft.winkDetection)
            appendFeedbackField("Speech timing feedback", draft.speechTiming)
            appendFeedbackField("Steps to reproduce", "")
            appendFeedbackField("Expected behaviour", "")
            appendFeedbackField("Actual behaviour", "")
            appendLine(SECTION_RULE)
            appendLine("END OF REPORT")
            appendLine(SECTION_RULE)
        }
        return PreparedEmail(
            to = DESTINATION_EMAIL,
            subject = buildSubject(
                versionName = safeVersion,
                glassesLabel = diagnostics.normallyUsesGlasses,
                manufacturer = safeManufacturer,
                model = safeModel
            ),
            body = body.trimEnd() + "\n"
        )
    }

    private fun StringBuilder.appendSectionHeader(title: String) {
        appendLine(SECTION_RULE)
        appendLine(title)
        appendLine(SECTION_RULE)
        appendLine()
    }

    private fun StringBuilder.appendTechnicalField(label: String, value: String) {
        appendLine("$label: ${FeedbackDiagnostics.nonBlank(value)}")
    }

    private fun StringBuilder.appendFeedbackField(label: String, value: String) {
        appendLine("$label:")
        appendLine(value.ifBlank { "(not provided)" })
        // Blank line after every response so multi-paragraph answers stay readable.
        appendLine()
    }

    /**
     * Percent-encodes a mailto query value. Spaces become `%20` (not `+`) for broad
     * mailto client compatibility. JVM-safe so unit tests do not require Android Uri mocks.
     */
    fun encodeMailtoQueryValue(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    fun decodeMailtoQueryValue(value: String): String =
        URLDecoder.decode(value, "UTF-8")

    /**
     * Builds a standards-compliant mailto URI string.
     * Recipient is not encoded (avoids double-encoding `@`).
     * Subject and body are percent-encoded query parameters.
     */
    fun buildMailtoUriString(prepared: PreparedEmail): String =
        "mailto:${prepared.to}" +
            "?subject=${encodeMailtoQueryValue(prepared.subject)}" +
            "&body=${encodeMailtoQueryValue(prepared.body)}"

    fun buildMailtoUri(prepared: PreparedEmail): Uri =
        Uri.parse(buildMailtoUriString(prepared))

    fun createSendIntent(prepared: PreparedEmail): Intent {
        return Intent(Intent.ACTION_SENDTO, buildMailtoUri(prepared)).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
        }
    }

    /**
     * Attempts to open a compatible email client. Does not pre-check resolvers.
     * Does not restrict packages, manufacturers, or require a default email app.
     */
    fun launch(activity: Activity, prepared: PreparedEmail): LaunchResult {
        val intent = createSendIntent(prepared)
        return try {
            activity.startActivity(intent)
            LaunchResult.Opened
        } catch (_: ActivityNotFoundException) {
            LaunchResult.NoCompatibleEmailApp
        }
    }

    fun mailtoRecipientFromUriString(mailto: String): String {
        require(mailto.startsWith("mailto:")) { "Not a mailto URI" }
        return mailto.removePrefix("mailto:").substringBefore('?')
    }

    fun mailtoQueryValue(mailto: String, key: String): String? {
        val query = mailto.substringAfter('?', missingDelimiterValue = "")
        if (query.isEmpty()) return null
        val prefix = "$key="
        val raw = query.split('&')
            .firstOrNull { it.startsWith(prefix) }
            ?.removePrefix(prefix)
            ?: return null
        return decodeMailtoQueryValue(raw)
    }
}
