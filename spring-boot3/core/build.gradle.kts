plugins {
    kotlin("plugin.spring")
    kotlin("plugin.noarg")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.spring_boot3_dependencies))
    // Spring Boot Starters
    compileOnly(Libs.springBootStarter("webflux"))
    compileOnly(Libs.springBootStarter("web"))
    // spring-test for WebTestClient in test support sources (main)
    compileOnly(Libs.springBootStarter("test"))
    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }
    testImplementation(Libs.springBootStarter("actuator"))

    // Spring core (from spring/core)
    compileOnly(project(":bluetape4k-io"))
    compileOnly(project(":bluetape4k-jackson2"))
    compileOnly(Libs.spring("context-support"))
    compileOnly(Libs.spring("messaging"))
    compileOnly(Libs.spring("web"))
    compileOnly(Libs.springData("commons"))
    compileOnly(Libs.springBoot("autoconfigure"))
    compileOnly(Libs.springBoot("configuration-processor"))
    api(Libs.jakarta_annotation_api)
    compileOnly(Libs.findbugs)
    compileOnly(project(":bluetape4k-idgenerators"))
    compileOnly(Libs.java_uuid_generator)
    compileOnly(Libs.netty_buffer)

    // Netty (from spring/webflux)
    compileOnly(project(":bluetape4k-netty"))

    compileOnly(project(":bluetape4k-micrometer"))

    // OkHttp3
    compileOnly(Libs.okhttp3)
    compileOnly(Libs.okhttp3_logging_interceptor)
    testImplementation(Libs.okhttp3_mockwebserver)

    // Apache HttpComponents HttpClient 5
    compileOnly(Libs.httpclient5)
    compileOnly(Libs.httpclient5_cache)
    compileOnly(Libs.httpclient5_fluent)
    testImplementation(Libs.httpclient5_testing)

    // Jackson
    compileOnly(project(":bluetape4k-jackson2"))
    compileOnly(Libs.jackson_core)
    compileOnly(Libs.jackson_databind)
    compileOnly(Libs.jackson_module_kotlin)
    compileOnly(Libs.jackson_module_blackbird)

    // Resilience4j
    compileOnly(project(":bluetape4k-resilience4j"))
    compileOnly(Libs.resilience4j_all)
    compileOnly(Libs.resilience4j_kotlin)
    compileOnly(Libs.resilience4j_cache)
    compileOnly(Libs.resilience4j_retry)
    compileOnly(Libs.resilience4j_circuitbreaker)
    compileOnly(Libs.resilience4j_reactor)

    // Micrometer
    compileOnly(Libs.micrometer_core)
    testImplementation(Libs.micrometer_core)
    testImplementation(Libs.micrometer_registry_prometheus)

    compileOnly(Libs.hibernate_validator)
    compileOnly(Libs.jakarta_el_api)

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(Libs.kotlinx_coroutines_core)
    compileOnly(Libs.kotlinx_coroutines_reactor)
    compileOnly(Libs.kotlinx_coroutines_reactive)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Reactor
    compileOnly(Libs.reactor_core)
    compileOnly(Libs.reactor_kotlin_extensions)
    testImplementation(Libs.reactor_test)

    // Test infra
    compileOnly(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers)
}
