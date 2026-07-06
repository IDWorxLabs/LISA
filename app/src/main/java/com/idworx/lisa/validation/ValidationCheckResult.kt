package com.idworx.lisa.validation

/** Result of a single deterministic validation check within an authority run. */
data class ValidationCheckResult(
    val checkId: String,
    val description: String,
    val passed: Boolean,
    val observation: String? = null,
    val remediation: String? = null
)
