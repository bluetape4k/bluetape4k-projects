plugins {
    kotlin("plugin.allopen")
    alias(bt4k.plugins.kotlinx.benchmark)
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

// https://github.com/Kotlin/kotlinx-benchmark
benchmark {
    targets {
        register("benchmark") {
            this as kotlinx.benchmark.gradle.JvmBenchmarkTarget
            jmhVersion = bt4k.versions.managed.jmh.core.h350a653f63e5.get()
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
    implementation(platform(bt4k.spring.boot4.dependencies))
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-io"))
    api(project(":bluetape4k-netty"))

    // Redisson
    api(bt4k.redisson)
    compileOnly(libs.redisson.spring.boot.starter)

    // Dependencies
    compileOnly(project(":bluetape4k-cache-core"))
    compileOnly(project(":bluetape4k-idgenerators"))

    // Coroutines
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(libs.kotlinx.coroutines.core)
    compileOnly(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)

    // Codecs
    compileOnly(bt4k.fory.kotlin)
    compileOnly(bt4k.kryo5)

    // Compressor
    compileOnly(bt4k.commons.compress)
    compileOnly(bt4k.snappy.java)
    compileOnly(bt4k.at.yawk.lz4.java)
    compileOnly(bt4k.zstd.jni)

    // Jackson 2
    compileOnly(project(":bluetape4k-jackson2"))
    compileOnly(libs.jackson.module.kotlin)
    compileOnly(libs.jackson.module.blackbird)
    compileOnly(libs.jackson.dataformat.protobuf)

    // JSON Codecs (compileOnly - 사용자가 직접 의존성 추가)
    compileOnly(project(":bluetape4k-jackson3"))
    compileOnly(libs.jackson3.databind)
    compileOnly(libs.jackson3.module.kotlin)

    compileOnly(project(":bluetape4k-fastjson2"))
    compileOnly(bt4k.fastjson2)
    compileOnly(bt4k.fastjson2.kotlin)

    // Cache
    compileOnly(bt4k.caffeine)
    compileOnly(bt4k.caffeine.jcache)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    // Redisson Map Read/Write Through test
    testImplementation(project(":bluetape4k-jdbc"))
    testRuntimeOnly(bt4k.h2.v2)
    testImplementation(bt4k.hikaricp)
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc")

    // Benchmark
    add("benchmarkImplementation", bt4k.kotlinx.benchmark.runtime)
    add("benchmarkImplementation", bt4k.kotlinx.benchmark.runtime.jvm)
    add("benchmarkImplementation", bt4k.jmh.core)
}
