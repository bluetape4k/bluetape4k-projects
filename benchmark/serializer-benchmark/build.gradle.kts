plugins {
    kotlin("plugin.allopen")
    alias(bt4k.plugins.kotlinx.benchmark)
}

allOpen {
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

benchmark {
    targets {
        register("benchmark") {
            this as kotlinx.benchmark.gradle.JvmBenchmarkTarget
            jmhVersion = bt4k.versions.managed.jmh.core.h350a653f63e5.get()
        }
    }
}

tasks.matching { it.name.endsWith("BenchmarkJar") }.configureEach {
    this as org.gradle.jvm.tasks.Jar
    exclude("META-INF/*.RSA", "META-INF/*.DSA", "META-INF/*.SF")
}

dependencies {
    implementation(project(":bluetape4k-io"))
    implementation(project(":bluetape4k-json"))
    implementation(project(":bluetape4k-jackson2"))
    implementation(project(":bluetape4k-jackson3"))
    implementation(project(":bluetape4k-fastjson2"))
    implementation(project(":bluetape4k-avro"))
    implementation(project(":bluetape4k-kafka4"))
    implementation(bt4k.kryo5)
    implementation(bt4k.fory.kotlin)

    testImplementation(project(":bluetape4k-junit5"))

    add("benchmarkImplementation", bt4k.kotlinx.benchmark.runtime)
    add("benchmarkImplementation", bt4k.kotlinx.benchmark.runtime.jvm)
    add("benchmarkImplementation", bt4k.jmh.core)
    add("benchmarkRuntimeOnly", bt4k.logback)
}
