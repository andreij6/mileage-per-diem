plugins {
    alias(libs.plugins.aijones.feature.presentation)
}

dependencies {
    implementation(projects.feature.tracking.domain)
    implementation(projects.core.analytics)
    implementation(projects.core.featureflags)
    implementation(libs.androidx.activity.compose)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)
    testImplementation(testFixtures(projects.feature.tracking.domain))
    testImplementation(testFixtures(projects.feature.trips.domain))
    testImplementation(testFixtures(projects.feature.settings.domain))
    testImplementation(testFixtures(projects.core.analytics))
    testImplementation(testFixtures(projects.core.featureflags))
}
