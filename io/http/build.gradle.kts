plugins {
    kotlin("plugin.allopen")
    kotlin("plugin.serialization")
    alias(bt4k.plugins.kotlinx.benchmark)
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
            jmhVersion = bt4k.versions.managed.jmh.core.h350a653f63e5.get()
        }
    }
}

// ---------------------------------------------------------------------------
// CPU / GC profiling support
//
// Add -PbenchmarkProfile=<profiler> to the Gradle command to enable profiling.
//
//   gc    → JVM GC logging  (-Xlog:gc*)
//             Output: build/benchmark-profiling/gc.log
//
//   jfr   → Java Flight Recorder  (-XX:StartFlightRecording)
//             Output: build/benchmark-profiling/benchmark.jfr
//             Open with JDK Mission Control (jmc) or IntelliJ IDEA JFR viewer.
//
//   async → async-profiler flame graph  (-agentpath:libasyncProfiler.so)
//             Also requires: -PasyncProfilerLib=/path/to/libasyncProfiler.so
//             Output: build/benchmark-profiling/async-cpu.html
//             kotlinx-benchmark runtime auto-detects the agent and sets forks=0.
//
// Examples:
//   ./gradlew :bluetape4k-http:testBenchmark -PbenchmarkProfile=gc
//   ./gradlew :bluetape4k-http:testBenchmark -PbenchmarkProfile=jfr \
//       -PbenchmarkInclude="HttpClientBenchmark"
//   ./gradlew :bluetape4k-http:testBenchmark -PbenchmarkProfile=async \
//       -PasyncProfilerLib=/path/to/libasyncProfiler.so \
//       -PbenchmarkInclude="HttpClientBenchmark"
// ---------------------------------------------------------------------------
val benchmarkProfiler = (project.findProperty("benchmarkProfile") as String?)
if (!benchmarkProfiler.isNullOrBlank()) {
    tasks.withType<JavaExec>().configureEach {
        // kotlinx-benchmark generates a JavaExec task named "{target}{capitalizedConfig}Benchmark".
        // For target="test" + config="main", capitalizedName() returns "" → task is "testBenchmark".
        if (name.endsWith("Benchmark")) {
            val profilingDir = layout.buildDirectory.dir("benchmark-profiling").get().asFile
            doFirst { profilingDir.mkdirs() }
            when (benchmarkProfiler.lowercase()) {
                "gc"    -> jvmArgs(
                    "-Xlog:gc*,safepoint:file=${profilingDir}/gc.log:time,uptime,level,tags"
                )
                "jfr"   -> jvmArgs(
                    "-XX:StartFlightRecording=" +
                            "filename=${profilingDir}/benchmark.jfr," +
                            "dumponexit=true,settings=profile,duration=600s"
                )
                "async" -> {
                    val profilerLib = (project.findProperty("asyncProfilerLib") as String?)
                        ?: error(
                            "async-profiler requires -PasyncProfilerLib=/path/to/libasyncProfiler.so"
                        )
                    jvmArgs(
                        "-agentpath:$profilerLib=start,event=cpu," +
                                "file=${profilingDir}/async-cpu.html,flamegraph,interval=1ms"
                    )
                }
                else    -> logger.warn(
                    "[bluetape4k-http] Unknown benchmarkProfile='$benchmarkProfiler'. " +
                            "Valid values: gc, jfr, async"
                )
            }
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
    testImplementation(bt4k.wiremock) {
        // 이 모듈은 Docker WireMock의 client API만 사용하므로 embedded server 전이는 필요하지 않다.
        exclude(group = "com.github.jknack")
        exclude(group = "org.eclipse.jetty")
        exclude(group = "org.eclipse.jetty.http2")
    }

    // Benchmark
    testImplementation(bt4k.kotlinx.benchmark.runtime)
    testImplementation(bt4k.kotlinx.benchmark.runtime.jvm)
    testImplementation(bt4k.jmh.core)

    // Coroutines
    api(project(":bluetape4k-coroutines"))
    compileOnly(libs.kotlinx.coroutines.core)
    compileOnly(libs.kotlinx.coroutines.reactive)
    compileOnly(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)

    // OkHttp3
    compileOnly(bt4k.okhttp3)
    compileOnly(bt4k.okhttp3.coroutines)
    compileOnly(bt4k.okhttp3.logging.interceptor)

    // OkHttp3 MockWebServer
    compileOnly(bt4k.okhttp3.mockwebserver)

    // Apache HttpCompoents HttpClient 5
    compileOnly(bt4k.httpclient5)
    compileOnly(libs.httpclient5.cache)
    compileOnly(libs.httpclient5.fluent)
    testImplementation(libs.httpclient5.testing) {
        exclude(group = "org.apache.httpcomponents.core5", module = "httpcore5-testing")
    }

    // Apache HttpComponent Core 5
    compileOnly(bt4k.httpcore5.lib)
    compileOnly(bt4k.httpcore5.h2)
    compileOnly(bt4k.httpcore5.reactive)
    testImplementation(libs.httpcore5.testing) {
        exclude(group = "org.apache.httpcomponents.core5", module = "httpcore5")
    }

    compileOnly(project(":bluetape4k-cache-core"))
    compileOnly(bt4k.caffeine)
    compileOnly(bt4k.caffeine.jcache)

    // Ktor Client
    compileOnly(libs.ktor.client.core)
    compileOnly(libs.ktor.client.cio)
    compileOnly(libs.ktor.client.content.negotiation)
    compileOnly(libs.ktor.serialization.kotlinx.json)
    compileOnly(bt4k.kotlinx.serialization.json)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.serialization.kotlinx.json)
    testImplementation(bt4k.kotlinx.serialization.json)

    // Vertx
    compileOnly(project(":bluetape4k-vertx"))
    compileOnly(bt4k.vertx.core)
    compileOnly(libs.vertx.web.client)
    compileOnly(libs.vertx.lang.kotlin)
    compileOnly(libs.vertx.lang.kotlin.coroutines)

    // Jackson
    compileOnly(project(":bluetape4k-jackson3"))
    compileOnly(libs.jackson3.databind)
    compileOnly(libs.jackson3.module.kotlin)
    compileOnly(libs.jackson3.module.blackbird)

    // Fastjson2
    compileOnly(project(":bluetape4k-fastjson2"))
    compileOnly(bt4k.fastjson2)
    compileOnly(bt4k.fastjson2.kotlin)

    // Reactor
    testImplementation(libs.reactor.core)
    testImplementation(libs.reactor.kotlin.extensions)
}
