package com.idworx.lisa

/**
 * V1.2 Voice Platform foundation — architecture only.
 * No billing, networking, or AI integration.
 */
enum class VoiceCategory {
    Device,
    Premium,
    MyVoice,
    Family
}

enum class VoiceType {
    Standard,
    Premium,
    Cloned,
    Donated
}

enum class VoiceProvider {
    AndroidTts,
    LisaPremium,
    LisaClone,
    LisaFamily
}

enum class VoiceOwnership {
    System,
    User,
    FamilyMember,
    Premium
}

enum class VoiceStatus {
    Active,
    ComingSoon,
    Unavailable,
    Installed
}

enum class VoiceSource {
    OnDevice,
    Cloud,
    Recorded
}

enum class VoiceCapability {
    OfflineSpeech,
    NaturalAccent,
    ExpressiveSpeech,
    FastGeneration,
    VoiceCloning,
    FamilyDonation,
    SouthAfricanAccent
}

data class VoicePreview(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val genderLabel: String? = null,
    val language: PreferredLanguage? = null,
    val status: VoiceStatus,
    val type: VoiceType,
    val isPremium: Boolean = false
)

data class VoicePack(
    val id: String,
    val category: VoiceCategory,
    val title: String,
    val description: String,
    val status: VoiceStatus,
    val ownership: VoiceOwnership,
    val provider: VoiceProvider,
    val source: VoiceSource,
    val capabilities: Set<VoiceCapability>,
    val previews: List<VoicePreview> = emptyList(),
    val actionLabel: String? = null,
    val highlights: List<String> = emptyList()
)

object VoicePlatformCatalog {

    fun homePacks(ui: LisaUiStrings): List<VoicePack> = listOf(
        VoicePack(
            id = "device",
            category = VoiceCategory.Device,
            title = ui.deviceVoiceTitle,
            description = ui.deviceVoiceDescription,
            status = VoiceStatus.Active,
            ownership = VoiceOwnership.System,
            provider = VoiceProvider.AndroidTts,
            source = VoiceSource.OnDevice,
            capabilities = setOf(VoiceCapability.OfflineSpeech),
            actionLabel = ui.manage
        ),
        VoicePack(
            id = "premium",
            category = VoiceCategory.Premium,
            title = ui.premiumVoicesTitle,
            description = ui.premiumVoicesHomeDescription,
            status = VoiceStatus.ComingSoon,
            ownership = VoiceOwnership.Premium,
            provider = VoiceProvider.LisaPremium,
            source = VoiceSource.Cloud,
            capabilities = setOf(
                VoiceCapability.NaturalAccent,
                VoiceCapability.ExpressiveSpeech,
                VoiceCapability.FastGeneration,
                VoiceCapability.SouthAfricanAccent
            ),
            previews = premiumPreviews(ui),
            highlights = listOf(
                ui.naturalEnglish,
                ui.naturalAfrikaans,
                ui.naturalIsiZulu
            )
        ),
        VoicePack(
            id = "my_voice",
            category = VoiceCategory.MyVoice,
            title = ui.myVoiceTitle,
            description = ui.myVoiceHomeDescription,
            status = VoiceStatus.ComingSoon,
            ownership = VoiceOwnership.User,
            provider = VoiceProvider.LisaClone,
            source = VoiceSource.Recorded,
            capabilities = setOf(VoiceCapability.VoiceCloning)
        ),
        VoicePack(
            id = "family_voice",
            category = VoiceCategory.Family,
            title = ui.familyVoiceTitle,
            description = ui.familyVoiceHomeDescription,
            status = VoiceStatus.ComingSoon,
            ownership = VoiceOwnership.FamilyMember,
            provider = VoiceProvider.LisaFamily,
            source = VoiceSource.Recorded,
            capabilities = setOf(VoiceCapability.FamilyDonation, VoiceCapability.VoiceCloning)
        )
    )

    fun premiumPreviews(ui: LisaUiStrings): List<VoicePreview> = listOf(
        VoicePreview(
            id = "premium_sa_female",
            displayName = ui.southAfricanFemale,
            subtitle = ui.premium,
            genderLabel = ui.female,
            language = PreferredLanguage.English,
            status = VoiceStatus.ComingSoon,
            type = VoiceType.Premium,
            isPremium = true
        ),
        VoicePreview(
            id = "premium_sa_male",
            displayName = ui.southAfricanMale,
            subtitle = ui.premium,
            genderLabel = ui.male,
            language = PreferredLanguage.English,
            status = VoiceStatus.ComingSoon,
            type = VoiceType.Premium,
            isPremium = true
        ),
        VoicePreview(
            id = "premium_af",
            displayName = ui.naturalAfrikaans,
            subtitle = ui.premium,
            language = PreferredLanguage.Afrikaans,
            status = VoiceStatus.ComingSoon,
            type = VoiceType.Premium,
            isPremium = true
        ),
        VoicePreview(
            id = "premium_zu",
            displayName = ui.naturalIsiZulu,
            subtitle = ui.premium,
            language = PreferredLanguage.IsiZulu,
            status = VoiceStatus.ComingSoon,
            type = VoiceType.Premium,
            isPremium = true
        )
    )

    fun premiumBenefits(ui: LisaUiStrings): List<String> = listOf(
        ui.premiumBenefitPronunciation,
        ui.premiumBenefitAccent,
        ui.premiumBenefitExpressive,
        ui.premiumBenefitFast,
        ui.premiumBenefitMoreVoices
    )

    fun myVoiceSteps(ui: LisaUiStrings): List<String> = listOf(
        ui.myVoiceStepRecord,
        ui.myVoiceStepLearn,
        ui.myVoiceStepSpeak
    )

    fun familyVoiceSteps(ui: LisaUiStrings): List<String> = listOf(
        ui.familyVoiceStepRecord,
        ui.familyVoiceStepProcess,
        ui.familyVoiceStepSpeak
    )

    fun packForCategory(category: VoiceCategory, ui: LisaUiStrings): VoicePack? =
        homePacks(ui).find { it.category == category }
}
