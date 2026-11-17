plugins {
    alias(libs.plugins.aijones.feature.data)
}

dependencies {
    implementation(projects.feature.reports.domain)
    implementation(projects.core.designsystem)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core)
}
