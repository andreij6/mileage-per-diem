plugins {
    alias(libs.plugins.aijones.feature.presentation)
}

dependencies {
    implementation(projects.feature.trips.domain)
    implementation(projects.core.analytics)
    implementation(projects.core.featureflags)
    implementation(libs.androidx.activity.compose)
    testImplementation(testFixtures(projects.feature.trips.domain))
    testImplementation(testFixtures(projects.core.analytics))
    testImplementation(testFixtures(projects.core.featureflags))
}
