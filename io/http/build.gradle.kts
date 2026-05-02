plugins {
    kotlin("plugin.allopen")
    alias(libs.plugins.kotlinx.benchmark)
}

allOpen {
    // https://github.com/Kotlin/kotlinx-benchmark
    annotation("org.openjdk.jmh.annotations.State")
}

// https://github.com/Kotlin/kotlinx-benchmark
benchmark {
    // Allow selecting a single benchmark class via -PbenchmarkInclude=<regex>
    // Default: run both HttpClientBenchmark and HttpClientCompressionCacheBenchmark
    configurations {
        named("main") {
            val includeRegex = (project.findProperty("benchmarkInclude") as String?)
            if (!includeRegex.isNullOrBlank()) {
                include(includeRegex)
            }
        }
    }
    targets {
        register("test") {
            this as kotlinx.benchmark.gradle.JvmBenchmarkTarget
            jmhVersion = libs.versions.jmh.get()
        }
    }
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-io"))
    api(project(":bluetape4k-netty"))
    api(project(":bluetape4k-resilience4j"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(libs.wiremock)

    // Benchmark
    testImplementation(libs.kotlinx.benchmark.runtime)
    testImplementation(libs.kotlinx.benchmark.runtime.jvm)
    testImplementation(libs.jmh.core)

    // Coroutines
    api(project(":bluetape4k-coroutines"))
    compileOnly(libs.kotlinx.coroutines.core)
    compileOnly(libs.kotlinx.coroutines.reactive)
    compileOnly(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)

    // OkHttp3
    compileOnly(libs.okhttp3)
    compileOnly(libs.okhttp3.coroutines)
    compileOnly(libs.okhttp3.logging.interceptor)

    // OkHttp3 MockWebServer
    compileOnly(libs.okhttp3.mockwebserver)

    // Apache HttpCompoents HttpClient 5
    compileOnly(libs.httpclient5)
    compileOnly(libs.httpclient5.cache)
    compileOnly(libs.httpclient5.fluent)
    testImplementation(libs.httpclient5.testing)

    // Apache HttpComponent Core 5
    compileOnly(libs.httpcore5)
    compileOnly(libs.httpcore5.h2)
    compileOnly(libs.httpcore5.reactive)
    testImplementation(libs.httpcore5.testing)

    compileOnly(project(":bluetape4k-cache-core"))
    compileOnly(libs.caffeine)
    compileOnly(libs.caffeine.jcache)

    // Vertx
    compileOnly(project(":bluetape4k-vertx"))
    compileOnly(libs.vertx.core)
    compileOnly(libs.vertx.web.client)
    compileOnly(libs.vertx.lang.kotlin)
    compileOnly(libs.vertx.lang.kotlin.coroutines)

    // Jackson
    compileOnly(project(":bluetape4k-jackson2"))
    compileOnly(libs.jackson.databind)
    compileOnly(libs.jackson.module.kotlin)
    compileOnly(libs.jackson.module.blackbird)

    // Fastjson2
    compileOnly(project(":bluetape4k-fastjson2"))
    compileOnly(libs.fastjson2)
    compileOnly(libs.fastjson2.kotlin)

    // Reactor
    testImplementation(libs.reactor.core)
    testImplementation(libs.reactor.kotlin.extensions)
}
