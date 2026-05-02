plugins {
    kotlin("plugin.spring")
    kotlin("plugin.noarg")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(libs.spring.boot3.dependencies))
    // Spring Boot Starters
    compileOnly("org.springframework.boot:spring-boot-starter-webflux")
    compileOnly("org.springframework.boot:spring-boot-starter-web")
    // spring-test for WebTestClient in test support sources (main)
    compileOnly("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }
    testImplementation("org.springframework.boot:spring-boot-starter-actuator")

    // Spring core (from spring/core)
    compileOnly(project(":bluetape4k-io"))
    compileOnly(project(":bluetape4k-jackson2"))
    compileOnly("org.springframework:spring-context-support")
    compileOnly("org.springframework:spring-messaging")
    compileOnly("org.springframework:spring-web")
    compileOnly("org.springframework.data:spring-data-commons")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    compileOnly("org.springframework.boot:spring-boot-configuration-processor")
    api(libs.jakarta.annotation.api)
    compileOnly(libs.findbugs)
    compileOnly(project(":bluetape4k-idgenerators"))
    compileOnly(libs.java.uuid.generator)
    compileOnly(libs.netty.buffer)

    // Netty (from spring/webflux)
    compileOnly(project(":bluetape4k-netty"))

    compileOnly(project(":bluetape4k-micrometer"))

    // OkHttp3
    compileOnly(libs.okhttp3)
    compileOnly(libs.okhttp3.logging.interceptor)
    testImplementation(libs.okhttp3.mockwebserver)

    // Apache HttpComponents HttpClient 5
    compileOnly(libs.httpclient5)
    compileOnly(libs.httpclient5.cache)
    compileOnly(libs.httpclient5.fluent)
    testImplementation(libs.httpclient5.testing)

    // Jackson
    compileOnly(project(":bluetape4k-jackson2"))
    compileOnly(libs.jackson.core)
    compileOnly(libs.jackson.databind)
    compileOnly(libs.jackson.module.kotlin)
    compileOnly(libs.jackson.module.blackbird)

    // Resilience4j
    compileOnly(project(":bluetape4k-resilience4j"))
    compileOnly(libs.resilience4j.all)
    compileOnly(libs.resilience4j.kotlin)
    compileOnly(libs.resilience4j.cache)
    compileOnly(libs.resilience4j.retry)
    compileOnly(libs.resilience4j.circuitbreaker)
    compileOnly(libs.resilience4j.reactor)

    // Micrometer
    compileOnly(libs.micrometer.core)
    testImplementation(libs.micrometer.core)
    testImplementation(libs.micrometer.registry.prometheus)

    compileOnly(libs.hibernate.validator)
    compileOnly(libs.jakarta.el.api)

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(libs.kotlinx.coroutines.core)
    compileOnly(libs.kotlinx.coroutines.reactor)
    compileOnly(libs.kotlinx.coroutines.reactive)
    testImplementation(libs.kotlinx.coroutines.test)

    // Reactor
    compileOnly(libs.reactor.core)
    compileOnly(libs.reactor.kotlin.extensions)
    testImplementation(libs.reactor.test)

    // Test infra
    compileOnly(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(libs.testcontainers)
}
