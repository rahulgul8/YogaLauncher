import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "in.tuwi.yogaphone"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tuwi.yogaphone"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        versionName = "1.4.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("boolean", "GUARD", "false")
        // Fake introduction evidence from an adb intent extra (IntroSignalsReader.setPreview). Never in release.
        buildConfigField("boolean", "INTRO_PREVIEW", "false")
    }

    // Yoga guard (accessibility service). Debug and profile builds always have it. Set this false to leave it
    // out of Play bundles (e.g. while the Accessibility API declaration is pending); see docs/launch/release.md.
    val guardInRelease = true

    // The Play upload key lives outside the repo. Point YOGA_UPLOAD_PROPERTIES elsewhere (e.g. in CI) if needed.
    val uploadProps = File(System.getenv("YOGA_UPLOAD_PROPERTIES")
        ?: "${System.getProperty("user.home")}/.secrets/yoga-upload.properties")
    signingConfigs {
        if (uploadProps.exists()) create("upload") {
            val p = Properties().apply { uploadProps.inputStream().use { load(it) } }
            storeFile = file(p.getProperty("storeFile"))
            storePassword = p.getProperty("storePassword")
            keyAlias = p.getProperty("keyAlias")
            keyPassword = p.getProperty("keyPassword")
        }
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "GUARD", "true")
            buildConfigField("boolean", "INTRO_PREVIEW", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfigs.findByName("upload")?.let { signingConfig = it }
            buildConfigField("boolean", "GUARD", guardInRelease.toString())
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        // Release-speed build for on-device checks: not debuggable, signed with the debug key.
        create("profile") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
            buildConfigField("boolean", "GUARD", "true")
            buildConfigField("boolean", "INTRO_PREVIEW", "true")
        }
    }
    sourceSets {
        getByName("debug").manifest.srcFile("src/guard/AndroidManifest.xml")
        getByName("profile").manifest.srcFile("src/guard/AndroidManifest.xml")
        if (guardInRelease) getByName("release").manifest.srcFile("src/guard/AndroidManifest.xml")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        // ScreenshotTest needs the Compose test activity, which only the debug build carries (ui-test-manifest).
        unitTests.all { test -> if (test.name.contains("Release")) test.exclude("**/ScreenshotTest*") }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Room
    implementation(libs.play.billing)
    // In-app updates (Play Core); talks only to the Play Store app on the phone.
    implementation(libs.play.app.update.ktx)
    // Play Core brings in fragment 1.1.0; the Activity Result API needs 1.3+ (lint InvalidFragmentVersionForActivityResult).
    implementation("androidx.fragment:fragment-ktx:1.8.9")
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // WorkManager, Navigation, Icons
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.extended)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    // Real org.json for JVM tests (the Android stub throws).
    testImplementation("org.json:json:20240303")
    // Off-device screenshots of screens (Robolectric native graphics); see ui/ScreenshotTest.kt
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}