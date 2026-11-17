plugins {
    alias(libs.plugins.aijones.feature.presentation)
}

dependencies {
    implementation(projects.feature.insights.domain)
    implementation(projects.core.analytics)
    implementation(projects.core.featureflags)
    testImplementation(testFixtures(projects.feature.insights.domain))
    testImplementation(testFixtures(projects.core.analytics))
    testImplementation(testFixtures(projects.core.featureflags))
}
