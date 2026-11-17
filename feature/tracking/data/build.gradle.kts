plugins {
    alias(libs.plugins.aijones.feature.data)
}

dependencies {
    implementation(projects.feature.tracking.domain)
    implementation(projects.core.analytics)
    implementation(projects.core.navigation)
    implementation(libs.androidx.core)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    testImplementation(testFixtures(projects.feature.tracking.domain))
    testImplementation(testFixtures(projects.feature.trips.domain))
    testImplementation(testFixtures(projects.feature.settings.domain))
    testImplementation(testFixtures(projects.core.analytics))
}
