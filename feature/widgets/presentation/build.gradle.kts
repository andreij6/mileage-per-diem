plugins {
    alias(libs.plugins.aijones.feature.presentation)
}

dependencies {
    implementation(projects.feature.trips.domain)
    implementation(projects.feature.tracking.domain)
    implementation(projects.feature.perdiem.domain)
    implementation(projects.core.analytics)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    testImplementation(testFixtures(projects.feature.trips.domain))
    testImplementation(testFixtures(projects.feature.tracking.domain))
    testImplementation(testFixtures(projects.feature.perdiem.domain))
}
