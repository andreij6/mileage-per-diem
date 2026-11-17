plugins {
    alias(libs.plugins.aijones.android.library)
    alias(libs.plugins.aijones.hilt)
    id("aijones.room")
}

dependencies {
    implementation(projects.core.model)
    testImplementation(projects.core.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
