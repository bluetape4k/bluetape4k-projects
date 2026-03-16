plugins {
    kotlin("plugin.spring")
    kotlin("plugin.noarg")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Spring Boot Starters
    api(Libs.springBootStarter("webflux"))
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
    compileOnly(project(":bluetape4k-jackson"))
    compileOnly(Libs.spring("context-support"))
    compileOnly(Libs.spring("messaging"))
    compileOnly(Libs.spring("web"))
    compileOnly(Libs.springData("commons"))
    compileOnly(Libs.springBoot("autoconfigure"))
    compileOnly(Libs.springBoot("configuration-processor"))
    annotationProcessor(Libs.springBoot("configuration-processor"))
    api(Libs.jakarta_annotation_api)
    compileOnly(Libs.findbugs)
    compileOnly(project(":bluetape4k-idgenerators"))
    compileOnly(Libs.java_uuid_generator)
    compileOnly(Libs.netty_buffer)

    // Netty (from spring/webflux)
    api(project(":bluetape4k-netty"))

    // Retrofit2 (from spring/retrofit2)
    api(project(":bluetape4k-micrometer"))
    api(project(":bluetape4k-retrofit2"))
    api(Libs.retrofit2)
    api(Libs.retrofit2_converter_jackson)
    api(Libs.retrofit2_converter_scalars)
    api(Libs.retrofit2_adapter_java8)
    compileOnly(Libs.retrofit2_adapter_reactor)
    testImplementation(Libs.retrofit2_mock)

    // OkHttp3
    api(Libs.okhttp3)
    compileOnly(Libs.okhttp3_logging_interceptor)
    testImplementation(Libs.okhttp3_mockwebserver)

    // Apache HttpComponents HttpClient 5
    implementation(Libs.httpclient5)
    implementation(Libs.httpclient5_cache)
    implementation(Libs.httpclient5_fluent)
    testImplementation(Libs.httpclient5_testing)

    // Vertx (optional)
    compileOnly(project(":bluetape4k-vertx-core"))
    compileOnly(Libs.vertx_core)
    compileOnly(Libs.vertx_lang_kotlin)
    compileOnly(Libs.vertx_lang_kotlin_coroutines)

    // Apache AsyncHttpClient
    compileOnly(Libs.async_http_client)
    compileOnly(Libs.async_http_client_extras_retrofit2)
    compileOnly(Libs.async_http_client_extras_rxjava2)

    // Jackson
    implementation(project(":bluetape4k-jackson"))
    implementation(Libs.jackson_core)
    implementation(Libs.jackson_databind)
    implementation(Libs.jackson_module_kotlin)
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

    // Spring Cloud
    compileOnly(Libs.spring_cloud_starter_bootstrap)

    compileOnly(Libs.hibernate_validator)
    compileOnly(Libs.jakarta_el_api)

    // Coroutines
    api(project(":bluetape4k-coroutines"))
    api(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_reactor)
    compileOnly(Libs.kotlinx_coroutines_reactive)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Reactor
    implementation(Libs.reactor_core)
    implementation(Libs.reactor_kotlin_extensions)
    testImplementation(Libs.reactor_test)

    // Test infra
    implementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers)
}
