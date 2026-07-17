configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(bt4k.spring.boot4.dependencies))

    api(project(":bluetape4k-http"))
    api(project(":bluetape4k-okio"))
    api(project(":bluetape4k-netty"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // Coroutines
    api(project(":bluetape4k-coroutines"))
    compileOnly(libs.kotlinx.coroutines.core)
    compileOnly(libs.kotlinx.coroutines.reactive)
    compileOnly(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)

    // Retrofit2
    api(libs.retrofit2)
    api(libs.retrofit2.converter.jackson)
    api(libs.retrofit2.converter.scalars)
    api(libs.retrofit2.adapter.java8)
    compileOnly(libs.retrofit2.adapter.reactor)
    compileOnly(libs.retrofit2.adapter.rxjava2)
    compileOnly(libs.retrofit2.adapter.rxjava3)
    testImplementation(libs.retrofit2.mock)

    // OkHttp3
    api(libs.okhttp3)
    api(libs.okhttp3.logging.interceptor)

    // OkHttp3 MockWebServer
    testImplementation(libs.okhttp3.mockwebserver)

    // Apache HttpCompoents HttpClient 5
    // feign_hc5 를 사용하려면, httpcore5, httpcore5-h2 도 버전을 맞춰줘야 한다
    compileOnly(bt4k.httpclient5)
    compileOnly(libs.httpclient5.cache)
    compileOnly(bt4k.httpcore5.lib)
    compileOnly(bt4k.httpcore5.h2)

    // Vertx
    compileOnly(project(":bluetape4k-vertx"))
    compileOnly(bt4k.vertx.core)
    compileOnly(libs.vertx.lang.kotlin)
    compileOnly(libs.vertx.lang.kotlin.coroutines)

    // Jackson
    api(project(":bluetape4k-jackson2"))
    api(libs.jackson.core)
    api(libs.jackson.databind)
    api(libs.jackson.module.kotlin)
    api(libs.jackson.module.blackbird)

    // Fastjson2
    compileOnly(libs.fastjson2)
    compileOnly(libs.fastjson2.kotlin)

    // Collections
    compileOnly(libs.commons.collections4)
    compileOnly(libs.eclipse.collections)
    compileOnly(libs.eclipse.collections.forkjoin)

    // Resilience4j
    compileOnly(project(":bluetape4k-resilience4j"))
    compileOnly(libs.resilience4j.all)
    compileOnly(libs.resilience4j.kotlin)
    compileOnly(libs.resilience4j.cache)
    compileOnly(libs.resilience4j.retry)
    compileOnly(libs.resilience4j.circuitbreaker)
    compileOnly(libs.resilience4j.reactor)
}
