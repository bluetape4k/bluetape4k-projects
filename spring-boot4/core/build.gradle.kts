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
    compileOnly(Libs.springBootStarter("webflux"))
    compileOnly(Libs.springBootStarter("web"))
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

    compileOnly(project(":bluetape4k-netty"))
    compileOnly(project(":bluetape4k-micrometer"))

    compileOnly(Libs.okhttp3)
    compileOnly(Libs.okhttp3_logging_interceptor)
    testImplementation(Libs.okhttp3_mockwebserver)

    compileOnly(Libs.httpclient5)
    compileOnly(Libs.httpclient5_cache)
    compileOnly(Libs.httpclient5_fluent)
    testImplementation(Libs.httpclient5_testing)

    // Jackson 3 (Spring Boot 4는 Jackson 3 사용)
    compileOnly(project(":bluetape4k-jackson3"))
    compileOnly(Libs.jackson3_module_kotlin)
    compileOnly(Libs.jackson3_module_blackbird)

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

    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_reactor)
    compileOnly(Libs.kotlinx_coroutines_reactive)
    testImplementation(Libs.kotlinx_coroutines_test)

    compileOnly(Libs.reactor_core)
    compileOnly(Libs.reactor_kotlin_extensions)
    testImplementation(Libs.reactor_test)

    compileOnly(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers)
}
