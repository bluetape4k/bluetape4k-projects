plugins {
    kotlin("plugin.allopen")
    id(Plugins.kotlinx_benchmark)
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
            jmhVersion = Versions.jmh
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
    testImplementation(project(":bluetape4k-junit5"))

    // Coroutines
    api(Libs.kotlinx_coroutines_core)
    compileOnly(Libs.kotlinx_coroutines_reactive)
    compileOnly(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_debug)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Collections
    compileOnly(Libs.commons_collections4)
    compileOnly(Libs.eclipse_collections)
    compileOnly(Libs.eclipse_collections_forkjoin)
    testImplementation(Libs.eclipse_collections_testutils)

    // Test Fixture
    compileOnly(Libs.kluent)
    compileOnly(Libs.kotlin_test_junit5)

    testImplementation(Libs.mockk)

    // Coroutines Flow를 Reactor처럼 테스트 할 수 있도록 해줍니다.
    // 참고: https://github.com/cashapp/turbine/
    testImplementation(Libs.turbine)

    // Benchmark
    testImplementation(Libs.kotlinx_benchmark_runtime)
    testImplementation(Libs.kotlinx_benchmark_runtime_jvm)
    testImplementation(Libs.jmh_core)
}
