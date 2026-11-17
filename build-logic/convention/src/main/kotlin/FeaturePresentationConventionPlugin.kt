import org.gradle.api.Plugin
import org.gradle.api.Project

class FeaturePresentationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("aijones.android.library")
        pluginManager.apply("aijones.android.compose")
        pluginManager.apply("aijones.hilt")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
        dependencies.apply {
            add("implementation", project(":core:model"))
            add("implementation", project(":core:common"))
            add("implementation", project(":core:designsystem"))
            add("implementation", project(":core:ui"))
            add("implementation", project(":core:navigation"))
            add("implementation", catalog.lib("androidx-lifecycle-runtime-compose"))
            add("implementation", catalog.lib("androidx-lifecycle-viewmodel-compose"))
            add("implementation", catalog.lib("androidx-hilt-lifecycle-viewmodel-compose"))
            add("implementation", catalog.lib("androidx-navigation3-runtime"))
            add("implementation", catalog.lib("kotlinx-coroutines-android"))
            add("testImplementation", project(":core:testing"))
            add("testImplementation", catalog.lib("robolectric"))
            add("testImplementation", catalog.lib("androidx-compose-ui-test-junit4"))
            add("debugImplementation", catalog.lib("androidx-compose-ui-test-manifest"))
        }
    }
}
