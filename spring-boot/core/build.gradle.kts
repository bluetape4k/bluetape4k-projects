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
    implementation(platform(bt4k.spring.boot4.dependencies))
    // Spring Boot Starters
    compileOnly("org.springframework.boot:spring-boot-starter-webflux")
    compileOnly("org.springframework.boot:spring-boot-starter-web")
    compileOnly("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }
    testImplementation("org.springframework.boot:spring-boot-starter-actuator")

    // Spring core
    compileOnly(project(":bluetape4k-io"))
    compileOnly(project(":bluetape4k-jackson3"))
    compileOnly("org.springframework:spring-context-support")
    compileOnly("org.springframework:spring-messaging")
    compileOnly("org.springframework:spring-web")
    compileOnly("org.springframework.data:spring-data-commons")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    compileOnly("org.springframework.boot:spring-boot-configuration-processor")
    api(bt4k.jakarta.annotation.api)
    compileOnly(bt4k.findbugs)
    compileOnly(project(":bluetape4k-idgenerators"))
    compileOnly(bt4k.java.uuid.generator)
    compileOnly(libs.netty.buffer)

    compileOnly(project(":bluetape4k-netty"))
    compileOnly(project(":bluetape4k-micrometer"))

    compileOnly(bt4k.okhttp3)
    compileOnly(bt4k.okhttp3.logging.interceptor)
    testImplementation(bt4k.okhttp3.mockwebserver)

    compileOnly(bt4k.httpclient5)
    compileOnly(libs.httpclient5.cache)
    compileOnly(libs.httpclient5.fluent)
    testImplementation(libs.httpclient5.testing) {
        exclude(group = "org.apache.httpcomponents.core5", module = "httpcore5-testing")
    }

    // Jackson 3 (Spring Boot 4는 Jackson 3 사용)
    compileOnly(project(":bluetape4k-jackson3"))
    compileOnly(libs.jackson3.module.kotlin)
    compileOnly(libs.jackson3.module.blackbird)

    compileOnly(project(":bluetape4k-resilience4j"))
    compileOnly(bt4k.resilience4j.all)
    compileOnly(bt4k.resilience4j.kotlin)
    compileOnly(bt4k.resilience4j.cache)
    compileOnly(bt4k.resilience4j.retry)
    compileOnly(bt4k.resilience4j.circuitbreaker)
    compileOnly(bt4k.resilience4j.reactor)

    compileOnly(libs.micrometer.core)
    compileOnly(libs.micrometer.observation)
    compileOnly(bt4k.micrometer.context.propagation)
    testImplementation(libs.micrometer.core)
    testImplementation(libs.micrometer.observation)
    testImplementation(libs.micrometer.observation.test)
    testImplementation(libs.micrometer.registry.prometheus)

    compileOnly(bt4k.hibernate.validator)
    compileOnly(bt4k.jakarta.el.api)

    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)
    compileOnly(libs.kotlinx.coroutines.reactive)
    testImplementation(libs.kotlinx.coroutines.test)

    compileOnly(libs.reactor.core)
    compileOnly(libs.reactor.kotlin.extensions)
    testImplementation(libs.reactor.test)

    compileOnly(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(libs.testcontainers)
}
