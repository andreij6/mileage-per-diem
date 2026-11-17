plugins {
    alias(libs.plugins.aijones.feature.data)
}

dependencies {
    implementation(projects.feature.insights.domain)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.analytics)
}
