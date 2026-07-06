package com.idworx.lisa.features.accessibilityconsistency.engine

import com.idworx.lisa.features.accessibilityconsistency.model.AccessibilityAudit
import com.idworx.lisa.features.accessibilityconsistency.model.AccessibilityReport
import com.idworx.lisa.features.accessibilityconsistency.model.AccessibilityScore

interface AccessibilityConsistencyEngine {
    fun runAudit(): AccessibilityAudit
    fun generateReport(): AccessibilityReport
    fun lastReport(): AccessibilityReport?
    fun lastScore(): AccessibilityScore?
    fun guidanceMessage(): String
}
