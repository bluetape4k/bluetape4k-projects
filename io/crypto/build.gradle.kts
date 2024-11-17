plugins {
    idea
    kotlin("plugin.allopen")
    id(Plugins.kotlinx_benchmark) version Plugins.Versions.kotlinx_benchmark
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

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

    // Cryptography
    api(Libs.jasypt)
    api(Libs.bouncycastle_bcprov)
    api(Libs.bouncycastle_bcpkix)
    api(Libs.commons_codec)

    // Compression
    compileOnly(Libs.lz4_java)
    compileOnly(Libs.snappy_java)
    compileOnly(Libs.zstd_jni)
    compileOnly(Libs.commons_compress)

    // Binary Serializers
    compileOnly(Libs.fury)
    compileOnly(Libs.kryo)

    // Apple Silicon
    compileOnly(Libs.jna_platform)

    // Coroutines
    compileOnly(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Benchmark
    testImplementation(Libs.kotlinx_benchmark_runtime)
    testImplementation(Libs.kotlinx_benchmark_runtime_jvm)
    testImplementation(Libs.jmh_core)
}
