plugins {
    alias(libs.plugins.aijones.feature.data)
}

dependencies {
    implementation(projects.feature.settings.domain)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.analytics)
    testImplementation(libs.androidx.room.runtime)
}
