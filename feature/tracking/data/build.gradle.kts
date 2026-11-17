plugins {
    alias(libs.plugins.aijones.feature.data)
}

dependencies {
    implementation(projects.feature.tracking.domain)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.analytics)
}
