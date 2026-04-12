plugins {
    kotlin("plugin.allopen")
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
        register("h2Jdbc") {
            include("io.bluetape4k.batch.benchmark.jdbc.H2JdbcBatchBenchmark")
            warmups = 2
            iterations = 5
            iterationTime = 1
            iterationTimeUnit = "s"
            mode = "thrpt"
            outputTimeUnit = "s"
            reportFormat = "json"
        }
        register("h2R2dbc") {
            include("io.bluetape4k.batch.benchmark.r2dbc.H2R2dbcBatchBenchmark")
            warmups = 2
            iterations = 5
            iterationTime = 1
            iterationTimeUnit = "s"
            mode = "thrpt"
            outputTimeUnit = "s"
            reportFormat = "json"
        }
        register("postgresJdbc") {
            include("io.bluetape4k.batch.benchmark.jdbc.PostgreSqlJdbcBatchBenchmark")
            warmups = 2
            iterations = 5
            iterationTime = 1
            iterationTimeUnit = "s"
            mode = "thrpt"
            outputTimeUnit = "s"
            reportFormat = "json"
        }
        register("postgresR2dbc") {
            include("io.bluetape4k.batch.benchmark.r2dbc.PostgreSqlR2dbcBatchBenchmark")
            warmups = 2
            iterations = 5
            iterationTime = 1
            iterationTimeUnit = "s"
            mode = "thrpt"
            outputTimeUnit = "s"
            reportFormat = "json"
        }
        register("mysqlJdbc") {
            include("io.bluetape4k.batch.benchmark.jdbc.MySqlJdbcBatchBenchmark")
            warmups = 2
            iterations = 5
            iterationTime = 1
            iterationTimeUnit = "s"
            mode = "thrpt"
            outputTimeUnit = "s"
            reportFormat = "json"
        }
        register("mysqlR2dbc") {
            include("io.bluetape4k.batch.benchmark.r2dbc.MySqlR2dbcBatchBenchmark")
            warmups = 2
            iterations = 5
            iterationTime = 1
            iterationTimeUnit = "s"
            mode = "thrpt"
            outputTimeUnit = "s"
            reportFormat = "json"
        }
    }
}

tasks.register<JavaExec>("generateBenchmarkDocs") {
    dependsOn("benchmarkClasses")
    classpath = sourceSets["benchmark"].runtimeClasspath
    mainClass.set("io.bluetape4k.batch.benchmark.support.BenchmarkDocsGeneratorKt")
    args(
        projectDir.absolutePath,
        layout.buildDirectory.dir("reports/benchmarks").get().asFile.absolutePath
    )
}

dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-coroutines"))
    api(project(":bluetape4k-logging"))
    api(project(":bluetape4k-workflow"))

    implementation(project(":bluetape4k-virtualthread-api"))
    runtimeOnly(project(":bluetape4k-virtualthread-jdk21"))

    // Exposed JDBC/R2DBC — 선택적 백엔드 (compileOnly)
    compileOnly(project(":bluetape4k-exposed-jdbc"))
    compileOnly(project(":bluetape4k-exposed-r2dbc"))
    compileOnly(Libs.exposed_java_time)

    // Checkpoint JSON 직렬화 — bluetape4k-jackson3 선택 의존
    compileOnly(project(":bluetape4k-jackson3"))

    // Coroutines
    implementation(Libs.kotlinx_coroutines_core)

    // Test
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-jackson3"))
    testImplementation(Libs.kotlinx_coroutines_test)

    // JDBC/R2DBC 통합 테스트 인프라
    testImplementation(project(":bluetape4k-exposed-jdbc-tests"))
    testImplementation(project(":bluetape4k-exposed-r2dbc-tests"))
    testImplementation(project(":bluetape4k-virtualthread-jdk21"))

    // Test DB — H2 (내장)
    testImplementation(Libs.h2_v2)
    testImplementation(Libs.hikaricp)
    testImplementation(Libs.r2dbc_h2)
    testImplementation(Libs.r2dbc_pool)

    // Test DB — PostgreSQL (Testcontainers)
    testImplementation(Libs.testcontainers_postgresql)
    testImplementation(Libs.postgresql_driver)
    testImplementation(Libs.r2dbc_postgresql)

    // Test DB — MySQL (Testcontainers)
    testImplementation(Libs.testcontainers_mysql)
    testImplementation(Libs.mysql_connector_j)
    testImplementation(Libs.r2dbc_mysql)

    // Benchmark
    add("benchmarkImplementation", Libs.kotlinx_benchmark_runtime)
    add("benchmarkImplementation", Libs.kotlinx_benchmark_runtime_jvm)
    add("benchmarkImplementation", Libs.jmh_core)
}
