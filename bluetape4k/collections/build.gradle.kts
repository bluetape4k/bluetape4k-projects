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
    api(project(":bluetape4k-core"))
    testImplementation(project(":bluetape4k-junit5"))

    // Apache Commons
    api(Libs.commons_collections4)

    // Eclipse Collections
    compileOnly(Libs.eclipse_collections)
    compileOnly(Libs.eclipse_collections_forkjoin)
    testImplementation(Libs.eclipse_collections_testutils)

    // Pods4k
    compileOnly(Libs.pods4k_core)
    compileOnly(Libs.pods4k_transformations_to_standard_collections)

    // Benchmark
    testImplementation(Libs.kotlinx_benchmark_runtime)
    testImplementation(Libs.kotlinx_benchmark_runtime_jvm)
    testImplementation(Libs.jmh_core)
}
