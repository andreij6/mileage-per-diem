plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.openapi.generator)
    application
}

kotlin {
    jvmToolchain(17)
}

val generatedClient = layout.buildDirectory.dir("generated/openapi")

openApiGenerate {
    generatorName.set("kotlin")
    library.set("jvm-okhttp4")
    inputSpec.set(layout.projectDirectory.file("openapi/perdiem.yaml").asFile.path)
    outputDir.set(generatedClient.get().asFile.path)
    packageName.set("com.aijones.mileage.tools.perdiem.gsa")
    generateApiTests.set(false)
    generateModelTests.set(false)
    generateApiDocumentation.set(false)
    generateModelDocumentation.set(false)
    configOptions.set(mapOf("serializationLibrary" to "kotlinx_serialization", "sourceFolder" to "src/main/kotlin", "omitGradleWrapper" to "true"))
}

sourceSets.main {
    kotlin.srcDir(generatedClient.map { it.dir("src/main/kotlin") })
}

tasks.named("compileKotlin") { dependsOn(tasks.named("openApiGenerate")) }

dependencies {
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.sqlite.jdbc)
    testImplementation(libs.kotlin.test)
}

application {
    mainClass.set("com.aijones.mileage.tools.perdiem.MainKt")
}

tasks.named<JavaExec>("run") {
    workingDir = rootDir
}
