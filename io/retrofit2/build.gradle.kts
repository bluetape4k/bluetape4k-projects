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
    api(bt4k.retrofit2)
    api(bt4k.retrofit2.converter.jackson)
    api(bt4k.retrofit2.converter.scalars)
    api(bt4k.retrofit2.adapter.java8)
    compileOnly(bt4k.retrofit2.adapter.reactor)
    compileOnly(bt4k.retrofit2.adapter.rxjava2)
    compileOnly(bt4k.retrofit2.adapter.rxjava3)
    testImplementation(bt4k.retrofit2.mock)

    // OkHttp3
    api(bt4k.okhttp3)
    api(bt4k.okhttp3.logging.interceptor)

    // OkHttp3 MockWebServer
    testImplementation(bt4k.okhttp3.mockwebserver)

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
    compileOnly(bt4k.fastjson2)
    compileOnly(bt4k.fastjson2.kotlin)

    // Collections
    compileOnly(bt4k.commons.collections4)
    compileOnly(bt4k.eclipse.collections)
    compileOnly(bt4k.eclipse.collections.forkjoin)

    // Resilience4j
    compileOnly(project(":bluetape4k-resilience4j"))
    compileOnly(bt4k.resilience4j.all)
    compileOnly(bt4k.resilience4j.kotlin)
    compileOnly(bt4k.resilience4j.cache)
    compileOnly(bt4k.resilience4j.retry)
    compileOnly(bt4k.resilience4j.circuitbreaker)
    compileOnly(bt4k.resilience4j.reactor)
}
