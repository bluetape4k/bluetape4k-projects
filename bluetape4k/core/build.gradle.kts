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
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-logging"))
    testImplementation(project(":bluetape4k-junit5"))

    api(Libs.kotlinx_atomicfu)

    // Apache Commons
    api(Libs.commons_lang3)
    compileOnly(Libs.commons_codec)
    compileOnly(Libs.commons_compress)
    compileOnly(Libs.commons_io)

    // Coroutines
    compileOnly(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Reactor
    compileOnly(Libs.reactor_core)
    compileOnly(Libs.reactor_kotlin_extensions)
    testImplementation(Libs.reactor_test)

    // Collections
    api(Libs.eclipse_collections)
    api(Libs.eclipse_collections_forkjoin)
    testImplementation(Libs.eclipse_collections_testutils)
    compileOnly(Libs.pods4k_core)
    compileOnly(Libs.pods4k_transformations_to_standard_collections)
    compileOnly(Libs.commons_collections4)

    // Benchmark
    testImplementation(Libs.kotlinx_benchmark_runtime)
    testImplementation(Libs.kotlinx_benchmark_runtime_jvm)
    testImplementation(Libs.jmh_core)
}
