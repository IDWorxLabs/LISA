plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.idworx.lisa"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.idworx.lisa"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

// ✅ THIS is the important fix: ONLY ONE android{} and compileOptions is OUTSIDE composeOptions
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
// Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.0")

// Compose (BOM)
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")

// CameraX
    val cameraxVersion = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

// ML Kit (Face Detection)
    implementation("com.google.mlkit:face-detection:16.1.6")
    implementation("com.google.mlkit:vision-common:17.3.0")

// JSON (JVM-compatible for unit tests and export/import)
    implementation("org.json:json:20240303")

// Tests (keep these if Android Studio added them)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

afterEvaluate {
    tasks.register<Test>("validateGuidedNavigationAuthorityV1") {
        group = "verification"
        description = "Run GUIDED_NAVIGATION_AUTHORITY_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.GuidedNavigationAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateGestureConflictAuthorityV1") {
        group = "verification"
        description = "Run GESTURE_CONFLICT_AUTHORITY_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.GestureConflictAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateNavigationReachabilityAuthorityV1") {
        group = "verification"
        description = "Run NAVIGATION_REACHABILITY_AUTHORITY_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.NavigationReachabilityAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateGuidedTrainingAuthorityV1") {
        group = "verification"
        description = "Run GUIDED_TRAINING_AUTHORITY_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.GuidedTrainingAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validatePersonalityEngineAuthorityV1") {
        group = "verification"
        description = "Run PERSONALITY_ENGINE_AUTHORITY_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.PersonalityEngineAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateCompanionMemoryAuthorityV1") {
        group = "verification"
        description = "Run COMPANION_MEMORY_AUTHORITY_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.CompanionMemoryAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateCoreCommunicationReliabilityAuthorityV1") {
        group = "verification"
        description = "Run CORE_COMMUNICATION_RELIABILITY_AUTHORITY_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.CoreCommunicationReliabilityAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateCalibrationReliabilityAuthorityV1") {
        group = "verification"
        description = "Run CALIBRATION_RELIABILITY_AUTHORITY_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.CalibrationReliabilityAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateCommunicationAccuracyAnalyticsAuthorityV1") {
        group = "verification"
        description = "Run COMMUNICATION_ACCURACY_ANALYTICS_AUTHORITY_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.CommunicationAccuracyAnalyticsAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateAccessibilityConsistencyAuthorityV1") {
        group = "verification"
        description = "Run ACCESSIBILITY_CONSISTENCY_AUTHORITY_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.AccessibilityConsistencyAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateOfflineReliabilityAuthorityV1") {
        group = "verification"
        description = "Run OFFLINE_RELIABILITY_AUTHORITY_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.OfflineReliabilityAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateBrain1ReadinessAuthorityV1") {
        group = "verification"
        description = "Run BRAIN_1_READINESS_AUTHORITY_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.Brain1ReadinessAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateAndroidDeviceTestingProtocolAuthorityV1") {
        group = "verification"
        description = "Run ANDROID_DEVICE_TESTING_PROTOCOL_AUTHORITY_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.AndroidDeviceTestingProtocolAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateZeroTouchPrincipleAuthorityV1") {
        group = "verification"
        description = "Run ZERO_TOUCH_PRINCIPLE_AUTHORITY_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.ZeroTouchPrincipleAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateBrain1InteractionStandardAuthorityV1") {
        group = "verification"
        description = "Run BRAIN1_INTERACTION_STANDARD_AUTHORITY_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.Brain1InteractionStandardAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateLisaExperiencePhaseAFirstFiveMinutesV1") {
        group = "verification"
        description = "Run LISA_EXPERIENCE_PHASE_A_FIRST_FIVE_MINUTES_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.FirstFiveMinutesAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateLisaExperiencePhaseBCommunicationWorkspaceV1") {
        group = "verification"
        description = "Run LISA_EXPERIENCE_PHASE_B_COMMUNICATION_WORKSPACE_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.CommunicationWorkspaceAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateLisaPatientCommunicationCoachV1") {
        group = "verification"
        description = "Run LISA_PATIENT_COMMUNICATION_COACH_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.PatientCommunicationCoachAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateLisaEmotionalPresenceV1") {
        group = "verification"
        description = "Run LISA_EMOTIONAL_PRESENCE_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.EmotionalPresenceAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateLisaCaregiverConfidenceV1") {
        group = "verification"
        description = "Run LISA_CAREGIVER_CONFIDENCE_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.CaregiverConfidenceAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateLisaGuidedLearningSimplificationV1") {
        group = "verification"
        description = "Run LISA_GUIDED_LEARNING_SIMPLIFICATION_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.GuidedLearningSimplificationAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateLisaLaunchScreenExactSimpleV1") {
        group = "verification"
        description = "Run LISA_LAUNCH_SCREEN_EXACT_SIMPLE_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.LaunchScreenExactSimpleAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateLisaSilentWelcomeLaunchFlowV1") {
        group = "verification"
        description = "Run LISA_SILENT_WELCOME_LAUNCH_FLOW_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.SilentWelcomeLaunchFlowAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateLisaLaunchWelcomeStatePriorityV1") {
        group = "verification"
        description = "Run LISA_LAUNCH_WELCOME_STATE_PRIORITY_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.LaunchWelcomeStatePriorityAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateLisaRuntimeCameraAndContextualSpeechV1") {
        group = "verification"
        description = "Run LISA_RUNTIME_CAMERA_AND_CONTEXTUAL_SPEECH_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.RuntimeCameraContextualSpeechAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateLisaCoreRuntimeAlwaysWelcomeVisibleSpeechV1") {
        group = "verification"
        description = "Run LISA_CORE_RUNTIME_ALWAYS_WELCOME_VISIBLE_SPEECH_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.CoreRuntimeAlwaysWelcomeVisibleSpeechAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateLisaGuidedLearningSetupBeforeHelloV1") {
        group = "verification"
        description = "Run LISA_GUIDED_LEARNING_SETUP_BEFORE_HELLO_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.GuidedLearningSetupBeforeHelloAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateLisaGuidedLearningInteractiveLessonsV1") {
        group = "verification"
        description = "Run LISA_GUIDED_LEARNING_INTERACTIVE_LESSONS_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.GuidedLearningInteractiveLessonsAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateLisaBlinkDetectionReliabilityTuningV1") {
        group = "verification"
        description = "Run LISA_BLINK_DETECTION_RELIABILITY_TUNING_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.BlinkDetectionReliabilityTuningAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateLisaGestureDuplicateAuditAndGuidedSensitivityV1") {
        group = "verification"
        description = "Run LISA_GESTURE_DUPLICATE_AUDIT_AND_GUIDED_SENSITIVITY_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.GestureDuplicateAuditAndGuidedSensitivityAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateLisaGuidedCurriculumAndNavigationContextV1") {
        group = "verification"
        description = "Run LISA_GUIDED_CURRICULUM_AND_NAVIGATION_CONTEXT_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.GuidedCurriculumAndNavigationContextAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateLisaGuidedUiOverlapAndFalseBlinkFixV1") {
        group = "verification"
        description = "Run LISA_GUIDED_UI_OVERLAP_AND_FALSE_BLINK_FIX_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.GuidedUiOverlapAndFalseBlinkFixAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateLisaGuidedPartialTimeoutAndWrongEyeFeedbackV1") {
        group = "verification"
        description = "Run LISA_GUIDED_PARTIAL_TIMEOUT_AND_WRONG_EYE_FEEDBACK_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.GuidedPartialTimeoutAndWrongEyeFeedbackAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateLisaGuidedWrongBlinkRestartsSequenceV1") {
        group = "verification"
        description = "Run LISA_GUIDED_WRONG_BLINK_RESTARTS_SEQUENCE_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.GuidedWrongBlinkRestartsSequenceAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateLisaGuidedSuccessTimingFixV1") {
        group = "verification"
        description = "Run LISA_GUIDED_SUCCESS_TIMING_FIX_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.GuidedSuccessTimingFixAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateLisaGuidedLessonProgressLabelV1") {
        group = "verification"
        description = "Run LISA_GUIDED_LESSON_PROGRESS_LABEL_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.GuidedLessonProgressLabelAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateLisaGuidedBlinkAcceptanceVisualFeedbackV1") {
        group = "verification"
        description = "Run LISA_GUIDED_BLINK_ACCEPTANCE_VISUAL_FEEDBACK_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.GuidedBlinkAcceptanceVisualFeedbackAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateLisaGuidedRemoveRedundantHelperTextV1") {
        group = "verification"
        description = "Run LISA_GUIDED_REMOVE_REDUNDANT_HELPER_TEXT_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.GuidedRemoveRedundantHelperTextAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateLisaGuidedTotalSequenceProgressV1") {
        group = "verification"
        description = "Run LISA_GUIDED_TOTAL_SEQUENCE_PROGRESS_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.GuidedTotalSequenceProgressAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateLisaGuidedProgressWordingPolishV1") {
        group = "verification"
        description = "Run LISA_GUIDED_PROGRESS_WORDING_POLISH_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.GuidedProgressWordingPolishAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateLisaRealWorkspaceGuidedNavigationTrainingV1") {
        group = "verification"
        description = "Run LISA_REAL_WORKSPACE_GUIDED_NAVIGATION_TRAINING_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.RealWorkspaceGuidedNavigationTrainingAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }

    tasks.register<Test>("validateLisaGuidedTrainingClarityAndTimingV1") {
        group = "verification"
        description = "Run LISA_GUIDED_TRAINING_CLARITY_AND_TIMING_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.GuidedTrainingClarityAndTimingAuthorityV1Test")
        }
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
        }
    }

    tasks.register<Test>("validateLisaGuidedTrainingLessonFocusV1") {
        group = "verification"
        description = "Run LISA_GUIDED_TRAINING_LESSON_FOCUS_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.GuidedTrainingLessonFocusAuthorityV1Test")
        }
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
        }
    }

    tasks.register<Test>("validateLisaGuidedNavigationAccessAndFloatingCardV1") {
        group = "verification"
        description = "Run LISA_GUIDED_NAVIGATION_ACCESS_AND_FLOATING_CARD_V1 validation and emit pass token on success"
        val unitTestTask = tasks.named<Test>("testDebugUnitTest")
        dependsOn(unitTestTask)
        testClassesDirs = unitTestTask.get().testClassesDirs
        classpath = unitTestTask.get().classpath
        filter {
            includeTestsMatching("com.idworx.lisa.validation.authority.GuidedNavigationAccessFloatingCardAuthorityV1Test")
        }
        testLogging {
            events("passed", "failed", "skipped", "standardOut")
        }
    }
}