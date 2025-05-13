plugins {
    kotlin("plugin.spring")
    kotlin("plugin.noarg")
}

@Suppress("UnstableApiUsage")
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.spring_modulith_bom))
    implementation(platform(Libs.exposed_bom))

    // Spring Modulith
    api(Libs.spring_modulith_events_core)
    api(Libs.spring_modulith_events_jackson)
    testImplementation(Libs.spring_modulith_junit)

    // Spring Boot
    compileOnly(Libs.springBoot("autoconfigure"))
    compileOnly(Libs.springBoot("configuration-processor"))
    annotationProcessor(Libs.springBoot("configuration-processor"))

    testImplementation(Libs.springBootStarter("web"))
    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }
    testImplementation(Libs.jakarta_servlet_api)

    // Exposed
    api(Libs.exposed_dao)
    api(Libs.exposed_java_time)
    api(Libs.exposed_spring_boot_starter)
    api(project(":bluetape4k-exposed"))
    testImplementation(project(":bluetape4k-exposed-tests"))
    testImplementation(project(":bluetape4k-junit5"))

    // Global Unique Identifier for Timebased UUID Table
    api(project(":bluetape4k-idgenerators"))
    api(Libs.java_uuid_generator)

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
