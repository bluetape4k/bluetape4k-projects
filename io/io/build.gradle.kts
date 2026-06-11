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
    configurations {
        named("main") {
            val includeRegex = project.findProperty("benchmarkInclude") as String?
            if (!includeRegex.isNullOrBlank()) {
                include(includeRegex)
            }
        }
        register("selfImprove") {
            val includeRegex = project.findProperty("benchmarkInclude") as String?
            include(includeRegex ?: ".*SameConditionCompressorBenchmark.compress.*")
            warmups = 1
            iterations = 2
            iterationTime = 500
            iterationTimeUnit = "ms"
        }
    }
    targets {
        register("test") {
            this as kotlinx.benchmark.gradle.JvmBenchmarkTarget
            jmhVersion = libs.versions.jmh.get()
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
    compileOnly(libs.commons.io)
    compileOnly(libs.commons.lang3)
    compileOnly(libs.commons.codec)
    compileOnly(libs.commons.compress)

    // Okio (compressor 내부용)
    api(libs.okio)

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)

    // Reactor
    compileOnly(libs.reactor.core)
    compileOnly(libs.reactor.kotlin.extensions)
    testImplementation(libs.reactor.test)

    // Eclipse Collections
    compileOnly(libs.eclipse.collections)
    compileOnly(libs.eclipse.collections.forkjoin)

    // Cache
    compileOnly(libs.caffeine)
    compileOnly(libs.caffeine.jcache)

    // Compression
    compileOnly(libs.lz4.java)
    compileOnly(libs.snappy.java)
    compileOnly(libs.xz)
    compileOnly(libs.zstd.jni)
    compileOnly(libs.brotli4j)
    compileOnly(libs.brotli4j.native)

    // Binary Serializers
    compileOnly(libs.kryo5)
    compileOnly(libs.fory.kotlin)  // new Apache Fory

    // Benchmark
    testImplementation(libs.kotlinx.benchmark.runtime)
    testImplementation(libs.kotlinx.benchmark.runtime.jvm)
    testImplementation(libs.jmh.core)

    // Binary Serializer 와 비교하기 하기 위해 Benchmark 에서 사용합니다.
    testImplementation(libs.jackson.datatype.jsr310)
    testImplementation(libs.jackson.module.kotlin)
    testImplementation(libs.jackson.module.blackbird)
}
