plugins {
    // Spring 관련 Plugin 은 spring-cloud-openfeign 예제를 위한 것입니다.
    kotlin("plugin.spring")
    // alias(libs.plugins.spring.boot3)
}

//tasks.bootJar {
//    enabled = false
//}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(libs.spring.boot3.dependencies))
    testImplementation(platform(libs.spring.cloud3.dependencies))

    api(project(":bluetape4k-http"))
    api(project(":bluetape4k-netty"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(libs.kotlinx.coroutines.core)
    compileOnly(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)

    // https://mvnrepository.com/artifact/javax.ws.rs/javax.ws.rs-api
    // feign 12.3 에서는 아직 javax.ws.rs-api 를 사용합니다.
    // api(libs.jakarta.ws.rs.api)

    // Feign
    api(libs.feign.core)
    api(libs.feign.hc5)
    api(libs.feign.kotlin)
    api(libs.feign.slf4j)
    api(libs.feign.jackson)
    compileOnly(libs.feign.reactive.wrappers)
    compileOnly(libs.feign.micrometer)
    compileOnly(libs.feign.jaxrs)
    compileOnly(libs.feign.jaxrs2)
    testImplementation(libs.feign.mock)

    // OkHttp3
    compileOnly(libs.okhttp3)
    compileOnly(libs.okhttp3.logging.interceptor)

    // OkHttp3 MockWebServer
    testImplementation(libs.okhttp3.mockwebserver)

    // Apache HttpCompoents HttpClient 5
    // feign_hc5 를 사용하려면, httpcore5, httpcore5-h2 도 버전을 맞춰줘야 한다 
    api(libs.httpclient5)
    api(libs.httpclient5.cache)
    api(libs.httpcore5)
    api(libs.httpcore5.h2)

    // Vertx
    compileOnly(project(":bluetape4k-vertx"))
    compileOnly(libs.vertx.core)
    compileOnly(libs.vertx.lang.kotlin)
    compileOnly(libs.vertx.lang.kotlin.coroutines)

    // Jackson (2.14 와 2.13 이 혼용되어서 jackson-core, jackson-databind 를 모두 지정해주어야 한다)
    api(project(":bluetape4k-jackson2"))
    api(libs.jackson.core)
    api(libs.jackson.databind)
    api(libs.jackson.module.kotlin)
    compileOnly(libs.jackson.module.blackbird)

    // Fastjson2
    compileOnly(project(":bluetape4k-fastjson2"))
    compileOnly(libs.fastjson2)
    compileOnly(libs.fastjson2.kotlin)

    // Resilience4j
    compileOnly(project(":bluetape4k-resilience4j"))
    compileOnly(libs.resilience4j.all)
    compileOnly(libs.resilience4j.kotlin)
    compileOnly(libs.resilience4j.feign)
    compileOnly(libs.resilience4j.cache)

    //
    // Spring Cloud OpenFeign 사용
    //
    testImplementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }
}
