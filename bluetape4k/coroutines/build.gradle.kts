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
    targets {
        register("test") {
            this as kotlinx.benchmark.gradle.JvmBenchmarkTarget
            jmhVersion = libs.versions.jmh.get()
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

val currentJvmMajor = JavaVersion.current().majorVersion.toInt()

dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-virtualthread-api"))
    if (currentJvmMajor >= 25) {
        compileOnly(project(":bluetape4k-virtualthread-jdk25"))
    } else {
        compileOnly(project(":bluetape4k-virtualthread-jdk21"))
    }
    testImplementation(project(":bluetape4k-junit5"))

    // Coroutines
    api(libs.kotlinx.coroutines.core)
    compileOnly(libs.kotlinx.coroutines.reactive)
    compileOnly(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.debug)
    testImplementation(libs.kotlinx.coroutines.test)

    // Collections
    compileOnly(libs.commons.collections4)
    compileOnly(libs.eclipse.collections)
    compileOnly(libs.eclipse.collections.forkjoin)
    testImplementation(libs.eclipse.collections.testutils)

    // Test Fixture
    compileOnly(libs.kluent)
    compileOnly(libs.kotlin.test.junit5)

    testImplementation(libs.mockk)

    // Coroutines Flow를 Reactor처럼 테스트 할 수 있도록 해줍니다.
    // 참고: https://github.com/cashapp/turbine/
    testImplementation(libs.turbine)

    // Benchmark
    testImplementation(libs.kotlinx.benchmark.runtime)
    testImplementation(libs.kotlinx.benchmark.runtime.jvm)
    testImplementation(libs.jmh.core)
}
