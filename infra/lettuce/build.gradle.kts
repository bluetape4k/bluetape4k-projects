import java.nio.file.Files
import java.nio.file.Path

plugins {
    kotlin("plugin.allopen")
    alias(libs.plugins.kotlinx.benchmark)
}

allOpen {
    // https://github.com/Kotlin/kotlinx-benchmark
    annotation("org.openjdk.jmh.annotations.State")
}

sourceSets {
    create("benchmark")
}

kover {
    currentProject {
        sources {
            excludedSourceSets.add("benchmark")
        }
    }
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
            jmhVersion = libs.versions.jmh.get()
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
    api(libs.lettuce.core)

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(libs.kotlinx.coroutines.core)
    compileOnly(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)

    // Cache / Memorizer
    compileOnly(project(":bluetape4k-cache-core"))

    // Serializer
    compileOnly(bt4k.fory.kotlin)
    compileOnly(libs.kryo5)

    // Compressor
    compileOnly(libs.lz4.java)
    compileOnly(libs.snappy.java)
    compileOnly(bt4k.zstd.jni)

    // JSON Codecs (compileOnly - 사용자가 직접 의존성 추가)
    compileOnly(project(":bluetape4k-jackson3"))
    compileOnly(libs.jackson3.databind)
    compileOnly(libs.jackson3.module.kotlin)

    compileOnly(project(":bluetape4k-fastjson2"))
    compileOnly(libs.fastjson2)
    compileOnly(libs.fastjson2.kotlin)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-resilience4j"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // Benchmark
    add("benchmarkImplementation", libs.kotlinx.benchmark.runtime)
    add("benchmarkImplementation", libs.kotlinx.benchmark.runtime.jvm)
    add("benchmarkImplementation", libs.jmh.core)
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("performance", "fencing-topology")
    }
}

tasks.register<Test>("fencingLeaseTopologyRecoveryTest") {
    description = "Runs fencing lease Redis promotion and restore recovery tests."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("fencing-topology")
    }
    outputs.upToDateWhen { false }
    outputs.cacheIf { false }
    shouldRunAfter(tasks.test)
}

tasks.register<Test>("multiKeyLeasePerformanceTest") {
    description = "Runs multi-key lease Redis characterization tests."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("performance")
    }
    val reportPath = layout.buildDirectory.file("reports/multi-key-lease-performance/results.json")
        .get()
        .asFile
        .absolutePath
    outputs.file(reportPath)
    outputs.upToDateWhen { false }
    outputs.cacheIf { false }
    doFirst {
        Files.deleteIfExists(Path.of(reportPath))
    }
    shouldRunAfter(tasks.test)
}
