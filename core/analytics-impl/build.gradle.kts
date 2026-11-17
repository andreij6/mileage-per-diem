plugins {
    alias(libs.plugins.aijones.android.library)
    alias(libs.plugins.aijones.hilt)
}

dependencies {
    api(projects.core.analytics)
    implementation(projects.core.common)
    implementation(projects.core.database)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(projects.core.testing)
    testImplementation(testFixtures(projects.core.analytics))
}
