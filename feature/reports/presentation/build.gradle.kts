plugins {
    alias(libs.plugins.aijones.feature.presentation)
}

dependencies {
    implementation(projects.feature.reports.domain)
    implementation(projects.core.analytics)
    implementation(libs.androidx.core)
    testImplementation(testFixtures(projects.feature.reports.domain))
    testImplementation(testFixtures(projects.feature.trips.domain))
    testImplementation(testFixtures(projects.feature.perdiem.domain))
    testImplementation(testFixtures(projects.feature.settings.domain))
    testImplementation(testFixtures(projects.core.analytics))
}
