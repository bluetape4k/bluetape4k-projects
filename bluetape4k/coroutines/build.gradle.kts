plugins {
    kotlin("plugin.allopen")
    alias(bt4k.plugins.kotlinx.benchmark)
}

allOpen {
    // https://github.com/Kotlin/kotlinx-benchmark
    annotation("org.openjdk.jmh.annotations.State")
}

// https://github.com/Kotlin/kotlinx-benchmark
benchmark {
    targets {
        register("test") {
            this as kotlinx.benchmark.gradle.JvmBenchmarkTarget
            jmhVersion = bt4k.versions.managed.jmh.core.h350a653f63e5.get()
        }
    }
    configurations {
        // self-improve 루프용: 빠른 측정 (warmup 2 + measurement 3 x 1s)
        register("coroutinesFlow") {
            include("io.bluetape4k.coroutines.benchmark.CoroutinesFlowBenchmark")
            warmups = 2
            iterations = 3
            iterationTime = 1
            iterationTimeUnit = "s"
            mode = "thrpt"
            outputTimeUnit = "s"
            reportFormat = "json"
        }
    }
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-virtualthread-api"))
    compileOnly(project(":bluetape4k-virtualthread-jdk21"))
    testImplementation(project(":bluetape4k-junit5"))

    // Coroutines
    api(libs.kotlinx.coroutines.core)
    compileOnly(libs.kotlinx.coroutines.reactive)
    compileOnly(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.debug)
    testImplementation(libs.kotlinx.coroutines.test)

    // Collections
    compileOnly(bt4k.commons.collections4)
    compileOnly(bt4k.eclipse.collections)
    compileOnly(bt4k.eclipse.collections.forkjoin)

    // Test Fixture
    compileOnly(project(":bluetape4k-assertions"))
    compileOnly(libs.kotlin.test.junit5)

    testImplementation(bt4k.mockk)

    // Coroutines Flow를 Reactor처럼 테스트 할 수 있도록 해줍니다.
    // 참고: https://github.com/cashapp/turbine/
    testImplementation(bt4k.turbine)

    // Benchmark
    testImplementation(bt4k.kotlinx.benchmark.runtime)
    testImplementation(bt4k.kotlinx.benchmark.runtime.jvm)
    testImplementation(bt4k.jmh.core)
}
