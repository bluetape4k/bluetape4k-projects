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

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-logging"))
    api(project(":bluetape4k-virtualthread-api"))
    runtimeOnly(project(":bluetape4k-virtualthread-jdk25"))
    testImplementation(project(":bluetape4k-junit5"))

    // Apache Commons
    api(bt4k.commons.lang3)
    compileOnly(bt4k.commons.codec)
    compileOnly(bt4k.commons.compress)
    compileOnly(bt4k.commons.io)

    // Hashing (XXHasher용)
    compileOnly(bt4k.at.yawk.lz4.java)

    // Coroutines
    compileOnly(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)

    // Reactor
    compileOnly(libs.reactor.core)
    compileOnly(libs.reactor.kotlin.extensions)
    testImplementation(libs.reactor.test)

    // Collections
    compileOnly(bt4k.commons.collections4)
    compileOnly(bt4k.eclipse.collections)
    compileOnly(bt4k.eclipse.collections.forkjoin)

    testImplementation(bt4k.java.uuid.generator)

    // Benchmark
    testImplementation(bt4k.kotlinx.benchmark.runtime)
    testImplementation(bt4k.kotlinx.benchmark.runtime.jvm)
    testImplementation(bt4k.jmh.core)
}
