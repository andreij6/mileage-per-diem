plugins {
    alias(libs.plugins.aijones.jvm.library)
}

dependencies {
    api(libs.junit)
    api(libs.kotlin.test)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
}
