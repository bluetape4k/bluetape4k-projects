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
    implementation(project(":bluetape4k-crypto"))
    testImplementation(project(":bluetape4k-junit5"))

    compileOnly(Libs.kotlinx_atomicfu)

    // Apache Commons
    api(Libs.commons_lang3)
    compileOnly(Libs.commons_codec)
    compileOnly(Libs.commons_compress)
    compileOnly(Libs.commons_io)

    // Okio
    api(Libs.okio)

    // Coroutines
    api(project(":bluetape4k-coroutines"))
    api(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Reactor
    compileOnly(Libs.reactor_core)
    compileOnly(Libs.reactor_kotlin_extensions)
    testImplementation(Libs.reactor_test)

    // Eclipse Collections
    compileOnly(Libs.eclipse_collections)
    compileOnly(Libs.eclipse_collections_forkjoin)
    testImplementation(Libs.eclipse_collections_testutils)

    // Cache
    compileOnly(Libs.caffeine)
    compileOnly(Libs.caffeine_jcache)

    // Compression
    compileOnly(Libs.snappy_java)
    compileOnly(Libs.lz4_java)
    compileOnly(Libs.xz)
    compileOnly(Libs.zstd_jni)

    compileOnly(Libs.brotli4j)
    compileOnly(Libs.brotli4j_native)

    // Binary Serializers
    compileOnly(Libs.kryo5)
    compileOnly(Libs.fory_kotlin)  // new Apache Fory

    // Benchmark
    testImplementation(Libs.kotlinx_benchmark_runtime)
    testImplementation(Libs.kotlinx_benchmark_runtime_jvm)
    testImplementation(Libs.jmh_core)

    // Binary Serializer 와 비교하기 하기 위해 Benchmark 에서 사용합니다.
    testImplementation(Libs.jackson_datatype_jsr310)
    testImplementation(Libs.jackson_module_kotlin)
    testImplementation(Libs.jackson_module_blackbird)
}
