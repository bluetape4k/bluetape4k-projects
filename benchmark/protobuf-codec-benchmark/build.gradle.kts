import com.google.protobuf.gradle.id

plugins {
    kotlin("plugin.allopen")
    alias(bt4k.plugins.protobuf.plugin)
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

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${bt4k.versions.protobuf.get()}"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                id("kotlin")
            }
        }
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

dependencies {
    implementation(project(":bluetape4k-protobuf"))
    implementation(project(":bluetape4k-redisson"))
    implementation(project(":bluetape4k-lettuce"))
    testImplementation(project(":bluetape4k-junit5"))

    add("benchmarkImplementation", bt4k.kotlinx.benchmark.runtime)
    add("benchmarkImplementation", bt4k.kotlinx.benchmark.runtime.jvm)
    add("benchmarkImplementation", bt4k.jmh.core)
    add("benchmarkRuntimeOnly", bt4k.logback)
}

tasks.matching { it.name.endsWith("BenchmarkJar") }.configureEach {
    this as org.gradle.jvm.tasks.Jar
    exclude("META-INF/*.RSA", "META-INF/*.DSA", "META-INF/*.SF")
}
