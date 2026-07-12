package com.idworx.lisa

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat

/** User-facing app version details sourced from the installed package. */
data class LisaAppVersionInfo(
    val versionName: String,
    val versionCode: Long
) {
    companion object {
        fun from(context: Context): LisaAppVersionInfo {
            val packageManager = context.packageManager
            val packageName = context.packageName
            val info = packageManager.getPackageInfo(packageName, 0)
            return LisaAppVersionInfo(
                versionName = info.versionName ?: "—",
                versionCode = PackageInfoCompat.getLongVersionCode(info)
            )
        }
    }
}
