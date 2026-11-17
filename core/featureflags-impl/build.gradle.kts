plugins {
    alias(libs.plugins.aijones.android.library)
    alias(libs.plugins.aijones.hilt)
}

dependencies {
    api(projects.core.featureflags)
    implementation(projects.core.common)
    implementation(projects.core.datastore)
    implementation(projects.core.analytics)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(projects.core.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(testFixtures(projects.core.analytics))
}
