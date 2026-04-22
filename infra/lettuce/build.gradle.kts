plugins {
    kotlin("plugin.allopen")
    id(Plugins.kotlinx_benchmark)
}

allOpen {
    // https://github.com/Kotlin/kotlinx-benchmark
    annotation("org.openjdk.jmh.annotations.State")
}

sourceSets {
    create("benchmark")
}

kotlin {
    target {
        compilations.getByName("benchmark").associateWith(compilations.getByName("main"))
    }
}

// https://github.com/Kotlin/kotlinx-benchmark
benchmark {
    targets {
        register("benchmark") {
            this as kotlinx.benchmark.gradle.JvmBenchmarkTarget
            jmhVersion = Versions.jmh
        }
    }
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
    named("benchmarkImplementation") {
        extendsFrom(
            configurations.getByName("implementation"),
            configurations.getByName("compileOnly"),
            configurations.getByName("testImplementation"),
        )
    }
    named("benchmarkRuntimeOnly") {
        extendsFrom(
            configurations.getByName("runtimeOnly"),
            configurations.getByName("testRuntimeOnly"),
        )
    }
}

dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-io"))
    api(project(":bluetape4k-netty"))

    // Lettuce
    api(Libs.lettuce_core)

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(Libs.kotlinx_coroutines_core)
    compileOnly(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Leader Election
    compileOnly(project(":bluetape4k-leader"))

    // Cache / Memorizer
    compileOnly(project(":bluetape4k-cache-core"))

    // Serializer
    compileOnly(Libs.fory_kotlin)
    compileOnly(Libs.kryo5)

    // Compressor
    compileOnly(Libs.lz4_java)
    compileOnly(Libs.snappy_java)
    compileOnly(Libs.zstd_jni)

    // JSON Codecs (compileOnly - 사용자가 직접 의존성 추가)
    compileOnly(project(":bluetape4k-jackson3"))
    compileOnly(Libs.jackson3_databind)
    compileOnly(Libs.jackson3_module_kotlin)

    compileOnly(project(":bluetape4k-fastjson2"))
    compileOnly(Libs.fastjson2)
    compileOnly(Libs.fastjson2_kotlin)

    testImplementation(testFixtures(project(":bluetape4k-cache-core")))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // Benchmark
    add("benchmarkImplementation", Libs.kotlinx_benchmark_runtime)
    add("benchmarkImplementation", Libs.kotlinx_benchmark_runtime_jvm)
    add("benchmarkImplementation", Libs.jmh_core)
}
