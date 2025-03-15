@file:Suppress("UnstableApiUsage")

plugins {
    kotlin("kapt")
    kotlin("plugin.noarg")
    kotlin("plugin.allopen")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {

    // Spring Modulith
    implementation(platform(Libs.spring_modulith_bom))
    api(Libs.spring_modulith_events_core)

    implementation(Libs.springBoot("autoconfigure"))

    // Exposed


}
