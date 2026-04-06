configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-io"))
    api(project(":bluetape4k-netty"))
    api(project(":bluetape4k-resilience4j"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // Coroutines
    api(project(":bluetape4k-coroutines"))
    compileOnly(Libs.kotlinx_coroutines_core)
    compileOnly(Libs.kotlinx_coroutines_reactive)
    compileOnly(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    // OkHttp3
    compileOnly(Libs.okhttp3)
    compileOnly(Libs.okhttp3_logging_interceptor)

    // OkHttp3 MockWebServer
    compileOnly(Libs.okhttp3_mockwebserver)

    // Apache HttpCompoents HttpClient 5
    api(Libs.httpclient5)
    api(Libs.httpclient5_cache)
    api(Libs.httpclient5_fluent)
    testImplementation(Libs.httpclient5_testing)

    // Apache HttpComponent Core 5
    api(Libs.httpcore5)
    implementation(Libs.httpcore5_h2)
    implementation(Libs.httpcore5_reactive)
    testImplementation(Libs.httpcore5_testing)

    compileOnly(project(":bluetape4k-cache-core"))
    compileOnly(Libs.caffeine)
    compileOnly(Libs.caffeine_jcache)

    // Vertx
    compileOnly(project(":bluetape4k-vertx"))
    compileOnly(Libs.vertx_core)
    compileOnly(Libs.vertx_lang_kotlin)
    compileOnly(Libs.vertx_lang_kotlin_coroutines)

    // Apache AsyncHttpClient
    compileOnly(Libs.async_http_client)

    // Jackson
    compileOnly(project(":bluetape4k-jackson2"))
    compileOnly(Libs.jackson_databind)
    compileOnly(Libs.jackson_module_kotlin)
    compileOnly(Libs.jackson_module_blackbird)

    // Fastjson2
    compileOnly(project(":bluetape4k-fastjson2"))
    compileOnly(Libs.fastjson2)
    compileOnly(Libs.fastjson2_kotlin)

    // Reactor
    testImplementation(Libs.reactor_core)
    testImplementation(Libs.reactor_kotlin_extensions)
}
