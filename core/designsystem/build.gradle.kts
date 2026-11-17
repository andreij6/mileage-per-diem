plugins {
    alias(libs.plugins.aijones.android.library)
    alias(libs.plugins.aijones.android.compose)
}

dependencies {
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.animation)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
