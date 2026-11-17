plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.aijones.mileage.tools.perdiem.MainKt")
}
