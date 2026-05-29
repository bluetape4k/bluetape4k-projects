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
    }
}

dependencies {
    add("benchmarkImplementation", project(":bluetape4k-idgenerators"))
    add("benchmarkImplementation", project(":bluetape4k-ktor-core"))

    add("benchmarkImplementation", libs.kotlinx.benchmark.runtime)
    add("benchmarkImplementation", libs.kotlinx.benchmark.runtime.jvm)
    add("benchmarkImplementation", libs.jmh.core)

    add("benchmarkImplementation", libs.kotlinx.serialization.json)
    add("benchmarkImplementation", libs.ktor.server.core)
    add("benchmarkImplementation", libs.ktor.server.cio)
    add("benchmarkImplementation", libs.ktor.server.content.negotiation)
    add("benchmarkImplementation", libs.ktor.serialization.kotlinx.json)

    add("benchmarkImplementation", platform(libs.spring.boot.dependencies))
    add("benchmarkImplementation", "org.springframework.boot:spring-boot-starter-webflux")
    add("benchmarkImplementation", libs.jackson3.module.kotlin)
    add("benchmarkImplementation", libs.jackson3.module.blackbird)

    add("benchmarkRuntimeOnly", libs.logback.classic)
}
