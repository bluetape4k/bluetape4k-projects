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
}

tasks.matching { it.name.endsWith("BenchmarkJar") }.configureEach {
    this as org.gradle.jvm.tasks.Jar
    exclude("META-INF/*.RSA", "META-INF/*.DSA", "META-INF/*.SF")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))

    compileOnly(project(":bluetape4k-tink"))
    testImplementation(project(":bluetape4k-junit5"))

    // Apache Commons
    compileOnly(bt4k.commons.io)
    compileOnly(bt4k.commons.lang3)
    compileOnly(bt4k.commons.codec)
    compileOnly(bt4k.commons.compress)

    // Okio (compressor 내부용)
    api(bt4k.okio)

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)

    // Reactor
    compileOnly(libs.reactor.core)
    compileOnly(libs.reactor.kotlin.extensions)
    testImplementation(libs.reactor.test)

    // Eclipse Collections
    compileOnly(bt4k.eclipse.collections)
    compileOnly(bt4k.eclipse.collections.forkjoin)

    // Cache
    compileOnly(bt4k.caffeine)
    compileOnly(bt4k.caffeine.jcache)

    // Compression
    compileOnly(bt4k.at.yawk.lz4.java)
    compileOnly(bt4k.snappy.java)
    compileOnly(bt4k.xz)
    compileOnly(bt4k.zstd.jni)
    compileOnly(bt4k.brotli4j)
    compileOnly(bt4k.brotli4j.native)

    // Binary Serializers
    compileOnly(bt4k.kryo5)
    compileOnly(bt4k.fory.kotlin)  // new Apache Fory

    // Benchmark
    testImplementation(bt4k.kotlinx.benchmark.runtime)
    testImplementation(bt4k.kotlinx.benchmark.runtime.jvm)
    testImplementation(bt4k.jmh.core)

    // Binary Serializer 와 비교하기 하기 위해 Benchmark 에서 사용합니다.
    testImplementation(libs.jackson.datatype.jsr310)
    testImplementation(libs.jackson.module.kotlin)
    testImplementation(libs.jackson.module.blackbird)
}
