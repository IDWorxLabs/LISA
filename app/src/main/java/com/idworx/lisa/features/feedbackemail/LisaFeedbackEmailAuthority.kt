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

    data class PreparedEmail(
        val to: String,
        val subject: String,
        val body: String
    )

    sealed class LaunchResult {
        data object Opened : LaunchResult()
        data object NoCompatibleEmailApp : LaunchResult()
    }

    fun prepare(
        context: Context,
        draft: MenuFeedbackDraft
    ): PreparedEmail {
        val version = LisaAppVersionInfo.from(context)
        val body = buildString {
            appendLine("LISA Version: ${version.versionName}")
            appendLine("Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("Device Model: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine()
            appendLine("Feedback:")
            appendLine()
            appendLine("What worked well:")
            appendLine(draft.workedWell.ifBlank { "(not provided)" })
            appendLine()
            appendLine("What was confusing:")
            appendLine(draft.confusing.ifBlank { "(not provided)" })
            appendLine()
            appendLine("Wink detection feedback:")
            appendLine(draft.winkDetection.ifBlank { "(not provided)" })
            appendLine()
            appendLine("Speech timing feedback:")
            appendLine(draft.speechTiming.ifBlank { "(not provided)" })
            appendLine()
            appendLine("Steps to reproduce:")
            appendLine("(not provided)")
            appendLine()
            appendLine("Expected behaviour:")
            appendLine("(not provided)")
            appendLine()
            appendLine("Actual behaviour:")
            appendLine("(not provided)")
        }
        return PreparedEmail(
            to = DESTINATION_EMAIL,
            subject = EMAIL_SUBJECT,
            body = body.trimEnd() + "\n"
        )
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
