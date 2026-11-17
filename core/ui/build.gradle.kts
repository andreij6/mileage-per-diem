plugins {
    alias(libs.plugins.aijones.android.library)
    alias(libs.plugins.aijones.android.compose)
}

dependencies {
    api(projects.core.designsystem)
    implementation(projects.core.model)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
