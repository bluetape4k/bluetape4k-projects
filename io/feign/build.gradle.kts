plugins {
    // Spring 관련 Plugin 은 spring-cloud-openfeign 예제를 위한 것입니다.
    kotlin("plugin.spring")
    // alias(bt4k.plugins.spring.boot)
}

//tasks.bootJar {
//    enabled = false
//}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(bt4k.spring.boot4.dependencies))
    testImplementation(platform(bt4k.spring.cloud.dependencies))

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
    // api(bt4k.jakarta.ws.rs.api)

    // Feign
    api(bt4k.feign.core)
    api(bt4k.feign.hc5)
    api(bt4k.feign.kotlin)
    api(bt4k.feign.slf4j)
    api(bt4k.feign.jackson)
    compileOnly(bt4k.feign.reactive.wrappers)
    compileOnly(bt4k.feign.micrometer)
    compileOnly(bt4k.feign.jaxrs)
    compileOnly(bt4k.feign.jaxrs2)
    testImplementation(bt4k.feign.mock)

    // OkHttp3
    compileOnly(bt4k.okhttp3)
    compileOnly(bt4k.okhttp3.logging.interceptor)

    // OkHttp3 MockWebServer
    testImplementation(bt4k.okhttp3.mockwebserver)

    // Apache HttpCompoents HttpClient 5
    // feign_hc5 를 사용하려면, httpcore5, httpcore5-h2 도 버전을 맞춰줘야 한다 
    api(bt4k.httpclient5)
    api(libs.httpclient5.cache)
    api(bt4k.httpcore5.lib)
    api(bt4k.httpcore5.h2)

    // Vertx
    compileOnly(project(":bluetape4k-vertx"))
    compileOnly(bt4k.vertx.core)
    compileOnly(libs.vertx.lang.kotlin)
    compileOnly(libs.vertx.lang.kotlin.coroutines)

    // Jackson
    api(project(":bluetape4k-jackson3"))
    api(libs.jackson3.core)
    api(libs.jackson3.databind)
    api(libs.jackson3.module.kotlin)
    compileOnly(libs.jackson3.module.blackbird)

    // Fastjson2
    compileOnly(project(":bluetape4k-fastjson2"))
    compileOnly(bt4k.fastjson2)
    compileOnly(bt4k.fastjson2.kotlin)

    // Resilience4j
    compileOnly(project(":bluetape4k-resilience4j"))
    compileOnly(bt4k.resilience4j.all)
    compileOnly(bt4k.resilience4j.kotlin)
    compileOnly(bt4k.resilience4j.feign)
    compileOnly(bt4k.resilience4j.cache)

    //
    // Spring Cloud OpenFeign 사용
    //
    testImplementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    testImplementation(bt4k.spring.boot.http.converter)
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }
}
