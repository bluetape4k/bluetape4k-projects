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
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-logging"))
    api(project(":bluetape4k-virtualthread-api"))
    runtimeOnly(project(":bluetape4k-virtualthread-jdk21"))
    // runtimeOnly(project(":bluetape4k-virtualthread-jdk25"))
    testImplementation(project(":bluetape4k-junit5"))

    // Apache Commons
    api(libs.commons.lang3)
    compileOnly(libs.commons.codec)
    compileOnly(libs.commons.compress)
    compileOnly(libs.commons.io)

    // Hashing (XXHasher용)
    compileOnly(libs.lz4.java)

    // Coroutines
    compileOnly(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)

    // Reactor
    compileOnly(libs.reactor.core)
    compileOnly(libs.reactor.kotlin.extensions)
    testImplementation(libs.reactor.test)

    // Collections
    compileOnly(libs.commons.collections4)
    compileOnly(libs.eclipse.collections)
    compileOnly(libs.eclipse.collections.forkjoin)
    testImplementation(libs.eclipse.collections.testutils)

    testImplementation(libs.java.uuid.generator)

    // Benchmark
    testImplementation(libs.kotlinx.benchmark.runtime)
    testImplementation(libs.kotlinx.benchmark.runtime.jvm)
    testImplementation(libs.jmh.core)
}
