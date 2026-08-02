plugins {
    kotlin("plugin.allopen")
    kotlin("plugin.serialization")
    alias(libs.plugins.kotlinx.benchmark)
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
            jmhVersion = libs.versions.jmh.get()
        }
    }
    configurations {
        register("throughput") {
            include("io.bluetape4k.benchmark.webframework.WebFrameworkRequestBenchmark")
            warmups = 2
            iterations = 3
            iterationTime = 1
            iterationTimeUnit = "s"
            mode = "thrpt"
            outputTimeUnit = "s"
            reportFormat = "json"
        }
        register("latency") {
            include("io.bluetape4k.benchmark.webframework.WebFrameworkLatencyBenchmark")
            warmups = 2
            iterations = 3
            iterationTime = 1
            iterationTimeUnit = "s"
            mode = "avgt"
            outputTimeUnit = "us"
            reportFormat = "json"
        }
        register("startup") {
            include("io.bluetape4k.benchmark.webframework.WebFrameworkStartupBenchmark")
            warmups = 1
            iterations = 3
            iterationTime = 1
            iterationTimeUnit = "s"
            mode = "avgt"
            outputTimeUnit = "ms"
            reportFormat = "json"
        }
        register("memory") {
            include("io.bluetape4k.benchmark.webframework.WebFrameworkMemoryBenchmark")
            warmups = 1
            iterations = 3
            iterationTime = 1
            iterationTimeUnit = "s"
            mode = "avgt"
            outputTimeUnit = "ms"
            reportFormat = "json"
        }
    }
}

val normalizeMemoryBenchmarkReport = tasks.register("normalizeMemoryBenchmarkReport") {
    doLast {
        val reportRoot = layout.buildDirectory.dir("reports/benchmarks/memory").get().asFile
        val report = reportRoot
            .walkTopDown()
            .filter { it.isFile && it.name == "benchmark.json" }
            .maxByOrNull { it.lastModified() }
            ?: error("No memory benchmark JSON found under $reportRoot")
        val normalized = report.resolveSibling("memory-metrics.json")

        val exitCode = ProcessBuilder(
            "python3",
            rootProject.file("benchmark/web-framework-benchmark/scripts/normalize-memory-report.py").absolutePath,
            report.absolutePath,
            normalized.absolutePath,
        ).inheritIO().start().waitFor()
        check(exitCode == 0) { "memory report normalization failed with exit code $exitCode" }
        logger.lifecycle("Normalized JVM heap report: ${normalized.relativeTo(rootProject.projectDir)}")
    }
}

tasks
    .matching { it.name == "memoryBenchmark" || it.name == "benchmarkMemoryBenchmark" }
    .configureEach { finalizedBy(normalizeMemoryBenchmarkReport) }

dependencies {
    add("benchmarkImplementation", project(":bluetape4k-idgenerators"))
    add("benchmarkImplementation", project(":bluetape4k-ktor-core"))

    add("benchmarkImplementation", libs.kotlinx.benchmark.runtime)
    add("benchmarkImplementation", libs.kotlinx.benchmark.runtime.jvm)
    add("benchmarkImplementation", libs.jmh.core)

    add("benchmarkImplementation", bt4k.kotlinx.serialization.json)
    add("benchmarkImplementation", libs.ktor.server.core)
    add("benchmarkImplementation", libs.ktor.server.cio)
    add("benchmarkImplementation", libs.ktor.server.content.negotiation)
    add("benchmarkImplementation", libs.ktor.serialization.kotlinx.json)

    add("benchmarkImplementation", platform(bt4k.spring.boot4.dependencies))
    add("benchmarkImplementation", "org.springframework.boot:spring-boot-starter-webflux")
    add("benchmarkImplementation", libs.jackson3.module.kotlin)
    add("benchmarkImplementation", libs.jackson3.module.blackbird)

    add("benchmarkRuntimeOnly", libs.logback.classic)
}
