plugins {
    kotlin("plugin.allopen")           // allOpen 필수
    alias(libs.plugins.kotlinx.benchmark)      // kotlinx-benchmark 플러그인
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
    annotation("kotlinx.benchmark.State")
}

sourceSets {
    create("benchmark")
}

val vipsImpl = project.findProperty("vips.impl")?.toString() ?: "java25"
val javaVersion = if (vipsImpl == "java21") 21 else 25

kotlin {
    jvmToolchain(javaVersion)
    target {
        compilations.getByName("benchmark").associateWith(compilations.getByName("main"))
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

benchmark {
    targets {
        register("benchmark") {
            this as kotlinx.benchmark.gradle.JvmBenchmarkTarget
            jmhVersion = libs.versions.jmh.get()
        }
    }
}

dependencies {
    // core
    implementation(project(":bluetape4k-core"))
    implementation(project(":bluetape4k-logging"))
    testImplementation(project(":bluetape4k-junit5"))

    // scrimage (images)
    implementation(project(":bluetape4k-images"))

    // vips — API 인터페이스는 컴파일 타임에 필요, 구현체는 런타임에만 필요
    add("benchmarkImplementation", project(":bluetape4k-images-vips-api"))
    if (vipsImpl == "java21") {
        add("benchmarkRuntimeOnly", project(":bluetape4k-images-vips-java21"))
    } else {
        add("benchmarkRuntimeOnly", project(":bluetape4k-images-vips-java25"))
    }

    // Benchmark
    add("benchmarkImplementation", libs.kotlinx.benchmark.runtime)
    add("benchmarkImplementation", libs.kotlinx.benchmark.runtime.jvm)
    add("benchmarkImplementation", libs.jmh.core)
    add("benchmarkImplementation", libs.jmh.generator.annprocess)
}
