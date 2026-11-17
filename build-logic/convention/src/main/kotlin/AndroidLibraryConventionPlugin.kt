import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("com.android.library")
        extensions.configure<LibraryExtension> {
            namespace = moduleNamespace()
            compileSdk = Sdk.COMPILE
            defaultConfig {
                minSdk = Sdk.MIN
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                consumerProguardFiles("consumer-rules.pro")
            }
            compileOptions {
                sourceCompatibility = Sdk.JAVA
                targetCompatibility = Sdk.JAVA
            }
            buildFeatures {
                buildConfig = false
                aidl = false
                shaders = false
                resValues = false
            }
            testOptions {
                unitTests.isIncludeAndroidResources = true
                unitTests.isReturnDefaultValues = true
            }
        }
        configureKotlin()
        addUnitTestDependencies()
    }
}
