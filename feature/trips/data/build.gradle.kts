plugins {
    alias(libs.plugins.aijones.feature.data)
}

dependencies {
    implementation(projects.feature.trips.domain)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.analytics)
}
