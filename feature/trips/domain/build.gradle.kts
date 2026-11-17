plugins {
    alias(libs.plugins.aijones.jvm.library)
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)
    api(projects.core.analytics)
    api(projects.core.featureflags)
    testImplementation(projects.core.testing)
    testImplementation(testFixtures(projects.core.analytics))
    testImplementation(testFixtures(projects.core.featureflags))
}
