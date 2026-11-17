plugins {
    `kotlin-dsl`
}

group = "com.aijones.mileage.buildlogic"

kotlin {
    jvmToolchain(17)
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
    compileOnly(libs.hilt.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "aijones.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "aijones.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "aijones.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("hilt") {
            id = "aijones.hilt"
            implementationClass = "HiltConventionPlugin"
        }
        register("jvmLibrary") {
            id = "aijones.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("featurePresentation") {
            id = "aijones.feature.presentation"
            implementationClass = "FeaturePresentationConventionPlugin"
        }
        register("featureData") {
            id = "aijones.feature.data"
            implementationClass = "FeatureDataConventionPlugin"
        }
        register("room") {
            id = "aijones.room"
            implementationClass = "RoomConventionPlugin"
        }
    }
}
