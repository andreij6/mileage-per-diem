plugins {
    alias(libs.plugins.aijones.android.library)
    alias(libs.plugins.aijones.hilt)
}

dependencies {
    api(libs.androidx.datastore.preferences)
    implementation(projects.core.common)
    testImplementation(projects.core.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
