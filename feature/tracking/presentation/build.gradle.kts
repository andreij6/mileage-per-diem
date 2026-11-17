plugins {
    alias(libs.plugins.aijones.feature.presentation)
}

dependencies {
    implementation(projects.feature.tracking.domain)
    implementation(projects.core.analytics)
    implementation(projects.core.featureflags)
    testImplementation(testFixtures(projects.feature.tracking.domain))
    testImplementation(testFixtures(projects.core.analytics))
    testImplementation(testFixtures(projects.core.featureflags))
}
