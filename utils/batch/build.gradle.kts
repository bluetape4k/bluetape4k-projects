plugins {
    kotlin("plugin.allopen")
    alias(libs.plugins.kotlinx.benchmark)
    alias(libs.plugins.kover)
}

kover {
    reports {
        filters {
            excludes {
                // JMH 벤치마크 코드는 커버리지 측정 대상에서 제외
                packages("io.bluetape4k.batch.benchmark")
            }
        }
    }
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
    compileOnly(libs.exposed.java.time)

    // Checkpoint JSON 직렬화 — bluetape4k-jackson3 선택 의존
    compileOnly(project(":bluetape4k-jackson3"))

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Test
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-jackson3"))
    testImplementation(libs.kotlinx.coroutines.test)

    // JDBC/R2DBC 통합 테스트 인프라
    testImplementation(project(":bluetape4k-exposed-jdbc-tests"))
    testImplementation(project(":bluetape4k-exposed-r2dbc-tests"))
    testImplementation(project(":bluetape4k-virtualthread-jdk21"))

    // Test DB — H2 (내장)
    testImplementation(libs.h2.v2)
    testImplementation(libs.hikaricp)
    testImplementation(libs.r2dbc.h2)
    testImplementation(libs.r2dbc.pool)

    // Test DB — PostgreSQL (Testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.postgresql.driver)
    testImplementation(libs.r2dbc.postgresql)

    // Test DB — MySQL (Testcontainers)
    testImplementation(libs.testcontainers.mysql)
    testImplementation(libs.mysql.connector.j)
    testImplementation(libs.r2dbc.mysql)

    // Benchmark
    add("benchmarkImplementation", libs.kotlinx.benchmark.runtime)
    add("benchmarkImplementation", libs.kotlinx.benchmark.runtime.jvm)
    add("benchmarkImplementation", libs.jmh.core)
}
