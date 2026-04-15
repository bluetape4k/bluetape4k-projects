plugins {
    kotlin("plugin.spring")
    kotlin("plugin.noarg")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Spring Boot 4 BOM: platform()을 사용하면 compileClasspath/runtimeClasspath에만 적용되고
    // kotlinBuildToolsApiClasspath 같은 내부 Gradle 설정에는 영향을 주지 않음
    // (dependencyManagement 플러그인은 ALL configurations에 적용되어 kotlin-stdlib 버전 충돌 유발)
    implementation(platform(Libs.spring_boot4_dependencies))
    // Spring Boot Starters
    api(Libs.springBootStarter("webflux"))
    implementation(Libs.springBootStarter("web"))
    compileOnly(Libs.springBootStarter("test"))
    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }
    testImplementation(Libs.springBootStarter("actuator"))

    // Spring core
    compileOnly(project(":bluetape4k-io"))
    compileOnly(project(":bluetape4k-jackson3"))
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

    api(project(":bluetape4k-netty"))
    api(project(":bluetape4k-micrometer"))
    api(project(":bluetape4k-retrofit2"))
    api(Libs.retrofit2)
    compileOnly(Libs.retrofit2_converter_jackson)
    api(Libs.retrofit2_converter_scalars)
    api(Libs.retrofit2_adapter_java8)
    compileOnly(Libs.retrofit2_adapter_reactor)
    testImplementation(Libs.retrofit2_mock)

    api(Libs.okhttp3)
    compileOnly(Libs.okhttp3_logging_interceptor)
    testImplementation(Libs.okhttp3_mockwebserver)

    implementation(Libs.httpclient5)
    implementation(Libs.httpclient5_cache)
    implementation(Libs.httpclient5_fluent)
    testImplementation(Libs.httpclient5_testing)

    compileOnly(project(":bluetape4k-vertx"))
    compileOnly(Libs.vertx_core)
    compileOnly(Libs.vertx_lang_kotlin)
    compileOnly(Libs.vertx_lang_kotlin_coroutines)

    compileOnly(Libs.async_http_client)
    compileOnly(Libs.async_http_client_extras_retrofit2)
    compileOnly(Libs.async_http_client_extras_rxjava2)

    // Jackson 2 (Spring Boot 4는 여전히 Jackson 2 사용)
    implementation(project(":bluetape4k-jackson2"))
    implementation(Libs.jackson_core)
    implementation(Libs.jackson_databind)
    implementation(Libs.jackson_module_kotlin)
    compileOnly(Libs.jackson_module_blackbird)

    compileOnly(project(":bluetape4k-resilience4j"))
    compileOnly(Libs.resilience4j_all)
    compileOnly(Libs.resilience4j_kotlin)
    compileOnly(Libs.resilience4j_cache)
    compileOnly(Libs.resilience4j_retry)
    compileOnly(Libs.resilience4j_circuitbreaker)
    compileOnly(Libs.resilience4j_reactor)

    compileOnly(Libs.micrometer_core)
    testImplementation(Libs.micrometer_core)
    testImplementation(Libs.micrometer_registry_prometheus)

    compileOnly(Libs.hibernate_validator)
    compileOnly(Libs.jakarta_el_api)

    api(project(":bluetape4k-coroutines"))
    api(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_reactor)
    compileOnly(Libs.kotlinx_coroutines_reactive)
    testImplementation(Libs.kotlinx_coroutines_test)

    implementation(Libs.reactor_core)
    implementation(Libs.reactor_kotlin_extensions)
    testImplementation(Libs.reactor_test)

    implementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers)
}
