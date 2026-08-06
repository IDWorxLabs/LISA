package com.idworx.lisa.features.glassescharacterisation

/**
 * Cross-condition comparison and decision support (engineering guidance only).
 */
object GlassesCharacterisationComparison {
    const val MEANINGFUL_SEP_DELTA: Float = 0.05f

    fun bestForEye(
        conditions: List<ConditionResult>,
        leftEye: Boolean
    ): LightingConditionKind? {
        val scored = conditions.filter { it.completed || it.left.open.count > 0 }.map { c ->
            val m = if (leftEye) c.left else c.right
            val sep = m.openP25MinusClosedP75 ?: -1f
            c.condition to sep
        }.filter { it.second >= 0f }
        return scored.maxByOrNull { it.second }?.first
    }

    fun bestOverall(conditions: List<ConditionResult>): LightingConditionKind? {
        val scored = conditions.map { c ->
            val l = c.left.openP25MinusClosedP75 ?: 0f
            val r = c.right.openP25MinusClosedP75 ?: 0f
            val qualityBonus = when (c.quality) {
                SignalQualityClass.Strong -> 0.10f
                SignalQualityClass.Moderate -> 0.04f
                SignalQualityClass.Weak -> 0f
                SignalQualityClass.Unusable -> -0.20f
            }
            c.condition to (l + r) / 2f + qualityBonus
        }
        return scored.maxByOrNull { it.second }?.first
    }

    fun improvementConsistentBothEyes(conditions: List<ConditionResult>): Boolean {
        val normal = conditions.firstOrNull { it.condition == LightingConditionKind.Normal }
            ?: return false
        val bestL = bestForEye(conditions, leftEye = true) ?: return false
        val bestR = bestForEye(conditions, leftEye = false) ?: return false
        if (bestL == LightingConditionKind.Normal && bestR == LightingConditionKind.Normal) {
            return false
        }
        val nL = normal.left.openP25MinusClosedP75 ?: 0f
        val nR = normal.right.openP25MinusClosedP75 ?: 0f
        val bL = conditions.firstOrNull { it.condition == bestL }
            ?.left?.openP25MinusClosedP75 ?: 0f
        val bR = conditions.firstOrNull { it.condition == bestR }
            ?.right?.openP25MinusClosedP75 ?: 0f
        return (bL - nL) >= MEANINGFUL_SEP_DELTA && (bR - nR) >= MEANINGFUL_SEP_DELTA
    }

    fun noMeaningfulImprovement(conditions: List<ConditionResult>): Boolean {
        val normal = conditions.firstOrNull { it.condition == LightingConditionKind.Normal }
            ?: return true
        val nL = normal.left.openP25MinusClosedP75 ?: 0f
        val nR = normal.right.openP25MinusClosedP75 ?: 0f
        return conditions.filter { it.condition != LightingConditionKind.Normal }.all { c ->
            val dL = (c.left.openP25MinusClosedP75 ?: 0f) - nL
            val dR = (c.right.openP25MinusClosedP75 ?: 0f) - nR
            dL < MEANINGFUL_SEP_DELTA && dR < MEANINGFUL_SEP_DELTA
        }
    }

    fun decide(conditions: List<ConditionResult>): Pair<DecisionSupportCategory, String> {
        if (conditions.isEmpty()) {
            return DecisionSupportCategory.INSUFFICIENT_EVIDENCE to
                "No lighting conditions were measured."
        }
        val anyStrongBoth = conditions.any {
            GlassesCharacterisationQuality.eyeClass(it.left) == SignalQualityClass.Strong &&
                GlassesCharacterisationQuality.eyeClass(it.right) == SignalQualityClass.Strong
        }
        if (anyStrongBoth) {
            return DecisionSupportCategory.CONTINUE_PERSONALISED_PROFILE_RESEARCH to
                "At least one lighting condition produced usable separation for both eyes. " +
                    "Engineering guidance: Personalised Eye Profile research may continue under " +
                    "the best lighting condition. This is not a production decision."
        }
        val anyModerate = conditions.any {
            it.quality == SignalQualityClass.Moderate || it.quality == SignalQualityClass.Strong
        }
        if (anyModerate && !noMeaningfulImprovement(conditions)) {
            return DecisionSupportCategory.CONTINUE_WITH_ENVIRONMENT_GUIDANCE to
                "Lighting changed measured separation. Engineering guidance: continue with " +
                    "caregiver lighting guidance and re-test before abandoning glasses research."
        }
        val allUnusable = conditions.all {
            it.quality == SignalQualityClass.Unusable ||
                ((it.left.openP25MinusClosedP75 ?: 0f) < 0.05f &&
                    (it.right.openP25MinusClosedP75 ?: 0f) < 0.05f)
        }
        if (allUnusable) {
            return DecisionSupportCategory.CONSIDER_ALTERNATIVE_EYE_DETECTOR to
                "No lighting condition produced meaningful open/closed separation with glasses. " +
                    "Engineering guidance: consider alternative eye-detection approaches for " +
                    "this device/setup. This is not a production decision."
        }
        if (noMeaningfulImprovement(conditions)) {
            return DecisionSupportCategory.SIGNAL_REMAINS_UNRELIABLE to
                "Lighting changes did not materially improve the measured signal. " +
                    "Engineering guidance: the glasses signal remains unreliable under tested " +
                    "conditions."
        }
        return DecisionSupportCategory.INSUFFICIENT_EVIDENCE to
            "Evidence is incomplete or mixed. Engineering guidance: gather more controlled " +
                "conditions before deciding."
    }

    fun findings(conditions: List<ConditionResult>): List<String> {
        val out = mutableListOf<String>()
        out += "Glasses Characterisation compared Normal, Brighter, and Dimmer lighting " +
            "with the patient remaining in a natural position."
        val normal = conditions.firstOrNull { it.condition == LightingConditionKind.Normal }
        for (c in conditions) {
            val l = c.left.openP25MinusClosedP75
            val r = c.right.openP25MinusClosedP75
            out += "${c.condition.displayName}: left separation ${fmt(l)}, " +
                "right separation ${fmt(r)}, quality ${c.quality}."
            if (normal != null && c.condition != LightingConditionKind.Normal) {
                val nL = normal.left.openP25MinusClosedP75
                val nR = normal.right.openP25MinusClosedP75
                if (l != null && nL != null) {
                    out += "${c.condition.displayName} changed left-eye separation " +
                        "from ${fmt(nL)} to ${fmt(l)}."
                }
                if (r != null && nR != null) {
                    out += "${c.condition.displayName} changed right-eye separation " +
                        "from ${fmt(nR)} to ${fmt(r)}."
                }
            }
        }
        val bestL = bestForEye(conditions, true)
        val bestR = bestForEye(conditions, false)
        val bestO = bestOverall(conditions)
        out += "Best lighting for left eye: ${bestL?.displayName ?: "n/a"}."
        out += "Best lighting for right eye: ${bestR?.displayName ?: "n/a"}."
        out += "Best overall lighting: ${bestO?.displayName ?: "n/a"}."
        if (bestL != null && bestR != null && bestL != bestR) {
            out += "The best condition for the right eye differed from the left eye."
        }
        if (noMeaningfulImprovement(conditions)) {
            out += "Lighting changes did not materially improve the measured signal."
        }
        val leftWeakAll = conditions.all {
            GlassesCharacterisationQuality.eyeClass(it.left) == SignalQualityClass.Unusable ||
                GlassesCharacterisationQuality.eyeClass(it.left) == SignalQualityClass.Weak
        }
        if (leftWeakAll) {
            out += "No lighting condition produced reliable left-eye separation."
        }
        return out.distinct()
    }

    fun caregiverRecommendations(
        decision: DecisionSupportCategory,
        bestOverall: LightingConditionKind?
    ): List<String> {
        val out = mutableListOf<String>()
        out += "Keep the patient comfortably still. Do not ask them to tilt, lean, or turn."
        out += "Never shine a torch or the phone flash into the user’s eyes."
        when (decision) {
            DecisionSupportCategory.CONTINUE_WITH_ENVIRONMENT_GUIDANCE,
            DecisionSupportCategory.CONTINUE_PERSONALISED_PROFILE_RESEARCH -> {
                bestOverall?.let {
                    out += "Prefer ${it.displayName} when continuing glasses-related engineering tests."
                }
                out += "A caregiver may adjust room lighting gently between sessions."
            }
            DecisionSupportCategory.SIGNAL_REMAINS_UNRELIABLE,
            DecisionSupportCategory.CONSIDER_ALTERNATIVE_EYE_DETECTOR -> {
                out += "Further lighting changes alone are unlikely to fix this setup."
            }
            DecisionSupportCategory.INSUFFICIENT_EVIDENCE -> {
                out += "Repeat the characterisation when lighting can be changed more clearly."
            }
        }
        return out
    }

    private fun fmt(v: Float?): String = v?.let { "%.3f".format(it) } ?: "n/a"
}
