pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        kotlin("jvm") version "2.3.10"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
rootProject.name = "mileage-per-diem"

include(":app", ":benchmark")
include(
    ":core:model", ":core:common", ":core:database", ":core:datastore", ":core:designsystem", ":core:ui",
    ":core:navigation", ":core:testing", ":core:analytics", ":core:analytics-impl", ":core:featureflags",
    ":core:featureflags-impl",
)
listOf("onboarding", "trips", "tracking", "perdiem", "reports", "settings", "insights").forEach { feature ->
    include(":feature:$feature:domain", ":feature:$feature:data", ":feature:$feature:presentation")
}
include(":feature:widgets:presentation")
include(":tools:perdiem-import")
