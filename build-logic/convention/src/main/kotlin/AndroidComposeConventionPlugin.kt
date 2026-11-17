import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        extensions.findByType<LibraryExtension>()?.buildFeatures?.compose = true
        extensions.findByType<ApplicationExtension>()?.buildFeatures?.compose = true
        dependencies.apply {
            val bom = dependencies.platform(catalog.lib("androidx-compose-bom"))
            add("implementation", bom)
            add("testImplementation", bom)
            add("androidTestImplementation", bom)
            add("implementation", catalog.lib("androidx-compose-ui"))
            add("implementation", catalog.lib("androidx-compose-foundation"))
            add("implementation", catalog.lib("androidx-compose-material3"))
            add("implementation", catalog.lib("androidx-compose-ui-tooling-preview"))
            add("debugImplementation", catalog.lib("androidx-compose-ui-tooling"))
        }
    }
}
