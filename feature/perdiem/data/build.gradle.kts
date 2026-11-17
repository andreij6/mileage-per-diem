plugins {
    alias(libs.plugins.aijones.feature.data)
    id("aijones.room")
}

dependencies {
    implementation(projects.feature.perdiem.domain)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.analytics)
}
