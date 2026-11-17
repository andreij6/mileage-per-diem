plugins {
    alias(libs.plugins.aijones.android.application)
    alias(libs.plugins.aijones.android.compose)
    alias(libs.plugins.aijones.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.aijones.mileage"
    defaultConfig {
        applicationId = "com.aijones.mileage"
        versionCode = 1
        versionName = "1.0.0"
    }
    androidResources {
        localeFilters += "en"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = true
    }
}

baselineProfile {
    dexLayoutOptimization = true
    saveInSrc = true
    automaticGenerationDuringBuild = false
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.designsystem)
    implementation(projects.core.ui)
    implementation(projects.core.navigation)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.analyticsImpl)
    implementation(projects.core.featureflagsImpl)
    implementation(projects.feature.onboarding.domain)
    implementation(projects.feature.onboarding.data)
    implementation(projects.feature.onboarding.presentation)
    implementation(projects.feature.trips.domain)
    implementation(projects.feature.trips.data)
    implementation(projects.feature.trips.presentation)
    implementation(projects.feature.tracking.domain)
    implementation(projects.feature.tracking.data)
    implementation(projects.feature.tracking.presentation)
    implementation(projects.feature.perdiem.domain)
    implementation(projects.feature.perdiem.data)
    implementation(projects.feature.perdiem.presentation)
    implementation(projects.feature.reports.domain)
    implementation(projects.feature.reports.data)
    implementation(projects.feature.reports.presentation)
    implementation(projects.feature.settings.domain)
    implementation(projects.feature.settings.data)
    implementation(projects.feature.settings.presentation)
    implementation(projects.feature.insights.domain)
    implementation(projects.feature.insights.data)
    implementation(projects.feature.insights.presentation)
    implementation(projects.feature.widgets.presentation)

    implementation(libs.androidx.core)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.kotlinx.coroutines.android)
    baselineProfile(projects.benchmark)
    testImplementation(projects.core.testing)
}

val verifyPermissions = tasks.register("verifyNoForbiddenPermissions") {
    group = "verification"
    description = "Fails if the merged release manifest requests background location or internet access."
    val manifest = layout.buildDirectory.file("intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml")
    dependsOn("processReleaseManifest")
    inputs.file(manifest)
    doLast {
        val text = manifest.get().asFile.readText()
        val forbidden = listOf("android.permission.ACCESS_BACKGROUND_LOCATION", "android.permission.INTERNET")
            .filter { text.contains("\"$it\"") }
        check(forbidden.isEmpty()) { "Forbidden permissions in merged manifest: $forbidden" }
    }
}

tasks.named("check") { dependsOn(verifyPermissions) }
