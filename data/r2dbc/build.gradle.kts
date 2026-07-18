import java.time.Duration

plugins {
    kotlin("plugin.allopen")
    kotlin("plugin.spring")
    alias(libs.plugins.kotlinx.benchmark)
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

// https://github.com/Kotlin/kotlinx-benchmark
benchmark {
    targets {
        register("benchmark") {
            this as kotlinx.benchmark.gradle.JvmBenchmarkTarget
            jmhVersion = libs.versions.jmh.get()
        }
    }
    configurations {
        register("poolConfig") {
            include("io.bluetape4k.r2dbc.benchmark.R2dbcPoolConfigBenchmark")
            warmups = 1
            iterations = 3
            iterationTime = 1
            iterationTimeUnit = "s"
            mode = "thrpt"
            outputTimeUnit = "s"
            reportFormat = "json"
        }
        register("h2PoolAcquire") {
            include("io.bluetape4k.r2dbc.benchmark.H2R2dbcPoolAcquireBenchmark")
            warmups = 1
            iterations = 3
            iterationTime = 1
            iterationTimeUnit = "s"
            mode = "thrpt"
            outputTimeUnit = "s"
            reportFormat = "json"
        }
        register("postgresPoolAcquire") {
            include("io.bluetape4k.r2dbc.benchmark.PostgreSqlR2dbcPoolAcquireBenchmark")
            warmups = 1
            iterations = 3
            iterationTime = 1
            iterationTimeUnit = "s"
            mode = "thrpt"
            outputTimeUnit = "s"
            reportFormat = "json"
        }
        register("mysql8PoolAcquire") {
            include("io.bluetape4k.r2dbc.benchmark.MySql8R2dbcPoolAcquireBenchmark")
            warmups = 1
            iterations = 3
            iterationTime = 1
            iterationTimeUnit = "s"
            mode = "thrpt"
            outputTimeUnit = "s"
            reportFormat = "json"
        }
        register("h2PoolContention") {
            include("io.bluetape4k.r2dbc.benchmark.H2R2dbcPoolContentionBenchmark")
            warmups = 1
            iterations = 3
            iterationTime = 1
            iterationTimeUnit = "s"
            mode = "thrpt"
            outputTimeUnit = "s"
            reportFormat = "json"
        }
    }
}

val poolAcquireBenchmarkTaskNames = setOf(
    "benchmarkH2PoolAcquireBenchmark",
    "benchmarkPostgresPoolAcquireBenchmark",
    "benchmarkMysql8PoolAcquireBenchmark",
)
val poolAcquireBenchmarkTaskTimeout = providers
    .gradleProperty("r2dbcPoolAcquireBenchmarkTaskTimeoutSeconds")
    .map { Duration.ofSeconds(it.toLong()) }
    .orElse(Duration.ofMinutes(5))

tasks.matching { it.name in poolAcquireBenchmarkTaskNames }.configureEach {
    timeout.set(poolAcquireBenchmarkTaskTimeout)
}

dependencies {
    implementation(platform(bt4k.spring.boot4.dependencies))
    api(project(":bluetape4k-core"))
    testImplementation(project(":bluetape4k-junit5"))

    // Jackson
    compileOnly(project(":bluetape4k-jackson3"))
    compileOnly(libs.jackson3.module.kotlin)

    // Coroutines
    api(project(":bluetape4k-coroutines"))
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.reactive)
    api(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlinx.coroutines.test)

    // Reactor
    compileOnly(libs.reactor.core)
    compileOnly(libs.reactor.kotlin.extensions)
    testImplementation(libs.reactor.test)

    // R2DBC
    api(libs.r2dbc.pool)
    compileOnly("org.springframework.boot:spring-boot-starter-data-r2dbc")
    compileOnly(libs.h2.v2)
    compileOnly(bt4k.r2dbc.h2)
    compileOnly(libs.r2dbc.mysql)
    compileOnly(libs.r2dbc.postgresql)


    // Spring Boot
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }

    // Benchmark
    add("benchmarkImplementation", libs.kotlinx.benchmark.runtime)
    add("benchmarkImplementation", libs.kotlinx.benchmark.runtime.jvm)
    add("benchmarkImplementation", libs.jmh.core)
    add("benchmarkImplementation", project(":bluetape4k-testcontainers"))
    add("benchmarkImplementation", libs.testcontainers.postgresql)
    add("benchmarkImplementation", libs.testcontainers.mysql)
    add("benchmarkImplementation", libs.testcontainers.r2dbc)
    add("benchmarkImplementation", libs.r2dbc.postgresql)
    add("benchmarkImplementation", libs.r2dbc.mysql)
    add("benchmarkImplementation", bt4k.postgresql)
    add("benchmarkImplementation", bt4k.mysql.connector.j)
    add("benchmarkRuntimeOnly", libs.h2.v2)
    add("benchmarkRuntimeOnly", bt4k.r2dbc.h2)
    add("benchmarkRuntimeOnly", libs.r2dbc.postgresql)
    add("benchmarkRuntimeOnly", libs.r2dbc.mysql)
}
