import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure

class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        pluginManager.apply("java-test-fixtures")
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = Sdk.JAVA
            targetCompatibility = Sdk.JAVA
        }
        configureKotlin()
        dependencies.apply {
            add("implementation", catalog.lib("javax-inject"))
            add("implementation", catalog.lib("kotlinx-coroutines-core"))
            add("testFixturesImplementation", catalog.lib("kotlinx-coroutines-core"))
        }
        addUnitTestDependencies()
    }
}
