plugins {
    kotlin("plugin.allopen")
    id(Plugins.kotlinx_benchmark)
    id(Plugins.kover)
}

kover {
    reports {
        filters {
            excludes {
                packages("io.bluetape4k.text.search.benchmark")
            }
        }
    }
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

// https://github.com/Kotlin/kotlinx-benchmark
benchmark {
    targets {
        register("benchmark") {
            this as kotlinx.benchmark.gradle.JvmBenchmarkTarget
            jmhVersion = Versions.jmh
        }
    }
    configurations {
        register("ahocorasick") {
            include("io.bluetape4k.text.search.benchmark.AhoCorasickBenchmark")
            warmups = 2
            iterations = 5
            iterationTime = 1
            iterationTimeUnit = "s"
            mode = "thrpt"
            outputTimeUnit = "s"
            reportFormat = "json"
        }
    }
}

dependencies {
    api(project(":bluetape4k-core"))
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(Libs.kotlinx_coroutines_core)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-coroutines"))
    testImplementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)

    add("benchmarkImplementation", Libs.kotlinx_benchmark_runtime)
    add("benchmarkImplementation", Libs.kotlinx_benchmark_runtime_jvm)
    add("benchmarkImplementation", Libs.jmh_core)
}
