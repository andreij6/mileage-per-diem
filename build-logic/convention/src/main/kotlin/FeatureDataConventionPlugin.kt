import org.gradle.api.Plugin
import org.gradle.api.Project

class FeatureDataConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("aijones.android.library")
        pluginManager.apply("aijones.hilt")
        dependencies.apply {
            add("implementation", project(":core:model"))
            add("implementation", project(":core:common"))
            add("implementation", catalog.lib("kotlinx-coroutines-android"))
            add("testImplementation", project(":core:testing"))
            add("testImplementation", catalog.lib("robolectric"))
            add("testImplementation", catalog.lib("androidx-test-core"))
        }
    }
}
