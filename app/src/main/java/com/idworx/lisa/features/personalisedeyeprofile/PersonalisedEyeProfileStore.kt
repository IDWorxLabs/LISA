package com.idworx.lisa.features.personalisedeyeprofile

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Debug-only local storage for personalised eye profiles.
 * Separate namespace from production settings — never overwrites Standard Mode.
 */
class PersonalisedEyeProfileStore(
    private val rootDir: File
) {
    companion object {
        const val RELATIVE_DIR = "debug/personalised_eye_profile"
        const val PROFILE_FILE = "prototype_profile.json"

        fun defaultRoot(filesDir: File): File = File(filesDir, RELATIVE_DIR)
    }

    init {
        if (!rootDir.exists()) rootDir.mkdirs()
    }

    fun directory(): File = rootDir

    fun profileFile(): File = File(rootDir, PROFILE_FILE)

    fun load(): PersonalisedEyeProfile? {
        val file = profileFile()
        if (!file.exists()) return null
        return try {
            fromJson(JSONObject(file.readText()))
        } catch (_: Exception) {
            null
        }
    }

    fun save(profile: PersonalisedEyeProfile) {
        if (!rootDir.exists()) rootDir.mkdirs()
        profileFile().writeText(toJson(profile).toString(2))
    }

    fun delete(): Boolean {
        val file = profileFile()
        return if (file.exists()) file.delete() else true
    }

    fun reportsDir(): File {
        val dir = File(rootDir, "reports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Saves an engineering report under filesDir/debug/personalised_eye_profile/reports/.
     * Filename: profile_report_yyyyMMdd_HHmmss_sessionId.txt
     */
    fun saveReport(fullText: String, sessionId: String, reportGeneratedMs: Long): File {
        val stamp = PersonalisedEyeProfileReportAuthority.formatFileStamp(reportGeneratedMs)
        val shortId = sessionId.replace("-", "").take(8)
        val file = File(reportsDir(), "profile_report_${stamp}_$shortId.txt")
        file.writeText(fullText)
        return file
    }

    fun containsImageOrPiiMarkers(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("data:image") ||
            lower.contains("base64") ||
            lower.contains(".jpg") ||
            lower.contains(".png") ||
            lower.contains("@") ||
            lower.contains("password") ||
            lower.contains("keystore")
    }

    private fun toJson(p: PersonalisedEyeProfile): JSONObject = JSONObject().apply {
        put("profileId", p.profileId)
        put("createdAtMs", p.createdAtMs)
        put("updatedAtMs", p.updatedAtMs)
        put("deviceManufacturer", p.deviceManufacturer)
        put("deviceModel", p.deviceModel)
        put("androidVersion", p.androidVersion)
        put("appVersionName", p.appVersionName)
        put("versionCode", p.versionCode)
        put("calibrationConditionLabel", p.calibrationConditionLabel)
        put("leftOpenBaseline", p.leftOpenBaseline.toDouble())
        put("leftClosedBaseline", p.leftClosedBaseline.toDouble())
        put("leftClosedMinimum", p.leftClosedMinimum.toDouble())
        put("leftReopenMaximum", p.leftReopenMaximum.toDouble())
        put("leftClosedThreshold", p.leftClosedThreshold.toDouble())
        put("leftOpenThreshold", p.leftOpenThreshold.toDouble())
        put("leftUncertaintyLower", p.leftUncertaintyLower.toDouble())
        put("leftUncertaintyUpper", p.leftUncertaintyUpper.toDouble())
        put("rightOpenBaseline", p.rightOpenBaseline.toDouble())
        put("rightClosedBaseline", p.rightClosedBaseline.toDouble())
        put("rightClosedMinimum", p.rightClosedMinimum.toDouble())
        put("rightReopenMaximum", p.rightReopenMaximum.toDouble())
        put("rightClosedThreshold", p.rightClosedThreshold.toDouble())
        put("rightOpenThreshold", p.rightOpenThreshold.toDouble())
        put("rightUncertaintyLower", p.rightUncertaintyLower.toDouble())
        put("rightUncertaintyUpper", p.rightUncertaintyUpper.toDouble())
        put("requiredConsecutiveCloseFrames", p.requiredConsecutiveCloseFrames)
        put("requiredConsecutiveReopenFrames", p.requiredConsecutiveReopenFrames)
        put("status", p.status.name)
        put("falsePositiveCount", p.falsePositiveCount)
        put("derivationNotes", p.derivationNotes)
        put("failureReasons", JSONArray(p.failureReasons))
        val counts = JSONObject()
        p.calibrationSampleCounts.forEach { (k, v) -> counts.put(k, v) }
        put("calibrationSampleCounts", counts)
        p.validationRun1?.let { put("validationRun1", runToJson(it)) }
        p.validationRun2?.let { put("validationRun2", runToJson(it)) }
    }

    private fun runToJson(r: PersonalisedValidationRunResult): JSONObject = JSONObject().apply {
        put("runNumber", r.runNumber)
        put("passed", r.passed)
        put("leftWinksDetected", r.leftWinksDetected)
        put("rightWinksDetected", r.rightWinksDetected)
        put("l1r1Success", r.l1r1Success)
        put("l2r2Success", r.l2r2Success)
        put("falsePositiveWinks", r.falsePositiveWinks)
        put("unexpectedSequence", r.unexpectedSequence)
        put("nullProbabilityPercent", r.nullProbabilityPercent.toDouble())
        put("uncertainOccupancyPercent", r.uncertainOccupancyPercent.toDouble())
        put("failureReasons", JSONArray(r.failureReasons))
    }

    private fun fromJson(o: JSONObject): PersonalisedEyeProfile {
        fun run(key: String): PersonalisedValidationRunResult? {
            if (!o.has(key) || o.isNull(key)) return null
            val r = o.getJSONObject(key)
            val reasons = mutableListOf<String>()
            val arr = r.optJSONArray("failureReasons")
            if (arr != null) for (i in 0 until arr.length()) reasons += arr.getString(i)
            return PersonalisedValidationRunResult(
                runNumber = r.getInt("runNumber"),
                passed = r.getBoolean("passed"),
                leftWinksDetected = r.optInt("leftWinksDetected"),
                rightWinksDetected = r.optInt("rightWinksDetected"),
                l1r1Success = r.optBoolean("l1r1Success"),
                l2r2Success = r.optBoolean("l2r2Success"),
                falsePositiveWinks = r.optInt("falsePositiveWinks"),
                unexpectedSequence = r.optBoolean("unexpectedSequence"),
                nullProbabilityPercent = r.optDouble("nullProbabilityPercent").toFloat(),
                uncertainOccupancyPercent = r.optDouble("uncertainOccupancyPercent").toFloat(),
                failureReasons = reasons
            )
        }
        val counts = mutableMapOf<String, Int>()
        val cObj = o.optJSONObject("calibrationSampleCounts")
        if (cObj != null) {
            val keys = cObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                counts[k] = cObj.getInt(k)
            }
        }
        val reasons = mutableListOf<String>()
        val rArr = o.optJSONArray("failureReasons")
        if (rArr != null) for (i in 0 until rArr.length()) reasons += rArr.getString(i)
        return PersonalisedEyeProfile(
            profileId = o.getString("profileId"),
            createdAtMs = o.getLong("createdAtMs"),
            updatedAtMs = o.getLong("updatedAtMs"),
            deviceManufacturer = o.optString("deviceManufacturer"),
            deviceModel = o.optString("deviceModel"),
            androidVersion = o.optString("androidVersion"),
            appVersionName = o.optString("appVersionName"),
            versionCode = o.optInt("versionCode"),
            calibrationConditionLabel = o.optString("calibrationConditionLabel", "glasses_or_user_condition"),
            leftOpenBaseline = o.optDouble("leftOpenBaseline").toFloat(),
            leftClosedBaseline = o.optDouble("leftClosedBaseline").toFloat(),
            leftClosedMinimum = o.optDouble("leftClosedMinimum").toFloat(),
            leftReopenMaximum = o.optDouble("leftReopenMaximum").toFloat(),
            leftClosedThreshold = o.optDouble("leftClosedThreshold").toFloat(),
            leftOpenThreshold = o.optDouble("leftOpenThreshold").toFloat(),
            leftUncertaintyLower = o.optDouble("leftUncertaintyLower").toFloat(),
            leftUncertaintyUpper = o.optDouble("leftUncertaintyUpper").toFloat(),
            rightOpenBaseline = o.optDouble("rightOpenBaseline").toFloat(),
            rightClosedBaseline = o.optDouble("rightClosedBaseline").toFloat(),
            rightClosedMinimum = o.optDouble("rightClosedMinimum").toFloat(),
            rightReopenMaximum = o.optDouble("rightReopenMaximum").toFloat(),
            rightClosedThreshold = o.optDouble("rightClosedThreshold").toFloat(),
            rightOpenThreshold = o.optDouble("rightOpenThreshold").toFloat(),
            rightUncertaintyLower = o.optDouble("rightUncertaintyLower").toFloat(),
            rightUncertaintyUpper = o.optDouble("rightUncertaintyUpper").toFloat(),
            requiredConsecutiveCloseFrames = o.optInt("requiredConsecutiveCloseFrames", 2),
            requiredConsecutiveReopenFrames = o.optInt("requiredConsecutiveReopenFrames", 1),
            calibrationSampleCounts = counts,
            validationRun1 = run("validationRun1"),
            validationRun2 = run("validationRun2"),
            falsePositiveCount = o.optInt("falsePositiveCount"),
            status = PersonalisedEyeProfileStatus.valueOf(o.optString("status", "Draft")),
            failureReasons = reasons,
            derivationNotes = o.optString("derivationNotes")
        )
    }
}
