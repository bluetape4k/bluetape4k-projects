plugins {
    kotlin("plugin.allopen")
    kotlin("plugin.spring")
    id(Plugins.kotlinx_benchmark)
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
            jmhVersion = Versions.jmh
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

dependencies {
    implementation(platform(Libs.spring_boot3_dependencies))
    api(project(":bluetape4k-core"))
    testImplementation(project(":bluetape4k-junit5"))

    // Jackson
    compileOnly(project(":bluetape4k-jackson2"))
    compileOnly(Libs.jackson_module_kotlin)

    // Coroutines
    api(project(":bluetape4k-coroutines"))
    api(Libs.kotlinx_coroutines_core)
    api(Libs.kotlinx_coroutines_reactive)
    api(Libs.kotlinx_coroutines_reactor)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Reactor
    compileOnly(Libs.reactor_core)
    compileOnly(Libs.reactor_kotlin_extensions)
    testImplementation(Libs.reactor_test)

    // R2DBC
    api(Libs.r2dbc_pool)
    compileOnly(Libs.springBootStarter("data-r2dbc"))
    compileOnly(Libs.h2_v2)
    compileOnly(Libs.r2dbc_h2)
    compileOnly(Libs.r2dbc_mysql)
    compileOnly(Libs.r2dbc_postgresql)


    // Spring Boot
    compileOnly(Libs.springBoot("autoconfigure"))

    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }

    // Benchmark
    add("benchmarkImplementation", Libs.kotlinx_benchmark_runtime)
    add("benchmarkImplementation", Libs.kotlinx_benchmark_runtime_jvm)
    add("benchmarkImplementation", Libs.jmh_core)
    add("benchmarkImplementation", project(":bluetape4k-testcontainers"))
    add("benchmarkImplementation", Libs.testcontainers_postgresql)
    add("benchmarkImplementation", Libs.testcontainers_mysql)
    add("benchmarkImplementation", Libs.testcontainers_r2dbc)
    add("benchmarkImplementation", Libs.r2dbc_postgresql)
    add("benchmarkImplementation", Libs.r2dbc_mysql)
    add("benchmarkImplementation", Libs.postgresql_driver)
    add("benchmarkImplementation", Libs.mysql_connector_j)
    add("benchmarkRuntimeOnly", Libs.h2_v2)
    add("benchmarkRuntimeOnly", Libs.r2dbc_h2)
    add("benchmarkRuntimeOnly", Libs.r2dbc_postgresql)
    add("benchmarkRuntimeOnly", Libs.r2dbc_mysql)
}
