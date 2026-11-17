plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.aijones.mileage.benchmark"
    compileSdk = 37
    defaultConfig {
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.uiautomator)
}

val startupBudgetMs = (findProperty("startupBudgetMs") as String?)?.toDouble() ?: 600.0

tasks.register("verifyStartupBudget") {
    val budget = startupBudgetMs
    mustRunAfter("connectedBenchmarkReleaseAndroidTest")
    group = "verification"
    description = "Fails when the median cold startup with a baseline profile is above the budget."
    val results = layout.buildDirectory.dir("outputs/connected_android_test_additional_output")
    inputs.dir(results).optional()
    doLast {
        val files = results.get().asFile.walkTopDown().filter { it.name.endsWith("benchmarkData.json") }.toList()
        check(files.isNotEmpty()) { "No benchmark results found. Run connectedBenchmarkReleaseAndroidTest first." }
        @Suppress("UNCHECKED_CAST")
        val medians = files.flatMap { f ->
            val json = groovy.json.JsonSlurper().parse(f) as Map<String, Any?>
            (json["benchmarks"] as List<Map<String, Any?>>).filter { (it["name"] as String).contains("BaselineProfile") }.mapNotNull { b ->
                val metrics = b["metrics"] as Map<String, Map<String, Any?>>
                (metrics["timeToInitialDisplayMs"]?.get("median") as Number?)?.toDouble()
            }
        }
        check(medians.isNotEmpty()) { "No baseline-profile startup result in $files" }
        val worst = medians.max()
        println("Cold startup median (baseline profile): ${"%.1f".format(worst)} ms, budget $budget ms")
        check(worst <= budget) { "Startup regression: median TTID $worst ms > budget $budget ms" }
    }
}
