plugins {
    alias(libs.plugins.aijones.jvm.library)
}

dependencies {
    api(projects.core.model)
    api(projects.feature.trips.domain)
    implementation(projects.core.common)
    api(projects.core.analytics)
    api(projects.core.featureflags)
    testFixturesImplementation(projects.core.analytics)
    testImplementation(projects.core.testing)
    testImplementation(testFixtures(projects.core.analytics))
    testImplementation(testFixtures(projects.core.featureflags))
    testImplementation(testFixtures(projects.feature.trips.domain))
}
