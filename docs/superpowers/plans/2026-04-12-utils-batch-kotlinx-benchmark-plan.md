# bluetape4k-batch benchmark 재구성 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `utils/batch`의 legacy JUnit benchmark를 `kotlinx-benchmark` 기반 benchmark source set으로 교체하고, DB별 JDBC/R2DBC 비교 문서를 자동 생성한다.

**Architecture:** `src/benchmark/kotlin` 아래에 DB/driver별 benchmark 클래스를 6개 두고, 공통 시나리오/환경/문서 생성 로직은 `benchmark.support` 패키지로 모은다. benchmark 실행은 `kotlinx-benchmark` custom configuration 6개(`h2Jdbc`, `h2R2dbc`, `postgresJdbc`, `postgresR2dbc`, `mysqlJdbc`, `mysqlR2dbc`)로 분리하고, JSON 결과를 `generateBenchmarkDocs` task가 읽어 `utils/batch/docs/benchmark/*.md` 와 `README.md`/`README.ko.md` 요약 링크를 갱신한다.

**Tech Stack:** Kotlin 2.3, kotlinx-benchmark(JMH), Exposed JDBC/R2DBC, HikariCP, r2dbc-pool, Testcontainers(PostgreSQL/MySQL), Mermaid, Gradle Kotlin DSL

> **Commit policy:** 이 저장소는 사용자가 명시적으로 요청할 때만 git commit 한다. 아래 계획에는 commit step을 넣지 않는다.

---

## File Structure

```text
utils/batch/
├── build.gradle.kts
├── README.md
├── README.ko.md
├── docs/
│   └── benchmark/
│       ├── README.md
│       ├── README.ko.md
│       ├── h2.md
│       ├── postgresql.md
│       └── mysql.md
└── src/
    ├── benchmark/kotlin/io/bluetape4k/batch/benchmark/
    │   ├── support/
    │   │   ├── BenchmarkDatabase.kt
    │   │   ├── BenchmarkScenarioParams.kt
    │   │   ├── BenchmarkSchema.kt
    │   │   ├── BenchmarkModels.kt
    │   │   ├── BenchmarkEnvironment.kt
    │   │   ├── SeedBenchmarkSupport.kt
    │   │   ├── BatchJobBenchmarkSupport.kt
    │   │   ├── BenchmarkMarkdownExporter.kt
    │   │   └── BenchmarkDocsGenerator.kt
    │   ├── jdbc/
    │   │   ├── AbstractJdbcBatchBenchmark.kt
    │   │   ├── H2JdbcBatchBenchmark.kt
    │   │   ├── PostgreSqlJdbcBatchBenchmark.kt
    │   │   └── MySqlJdbcBatchBenchmark.kt
    │   └── r2dbc/
    │       ├── AbstractR2dbcBatchBenchmark.kt
    │       ├── H2R2dbcBatchBenchmark.kt
    │       ├── PostgreSqlR2dbcBatchBenchmark.kt
    │       └── MySqlR2dbcBatchBenchmark.kt
    └── test/kotlin/io/bluetape4k/batch/
        ├── jdbc/BatchJdbcBenchmarkTest.kt
        └── r2dbc/BatchR2dbcBenchmarkTest.kt
```

---

### Task 1: Gradle benchmark wiring 추가

**Files:**
- Modify: `utils/batch/build.gradle.kts`

- [ ] **Step 1: benchmark plugin, source set, allOpen 설정 추가**

```kotlin
plugins {
    kotlin("plugin.allopen")
    id(Plugins.kotlinx_benchmark)
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

sourceSets {
    create("benchmark")
}

kotlin {
    target {
        compilations.getByName("benchmark")
            .associateWith(compilations.getByName("main"))
    }
}
```

- [ ] **Step 2: benchmark source set이 기존 test 의존성을 재사용하도록 설정**

```kotlin
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())

    named("benchmarkImplementation") {
        extendsFrom(
            implementation.get(),
            compileOnly.get(),
            testImplementation.get(),
        )
    }
    named("benchmarkRuntimeOnly") {
        extendsFrom(
            runtimeOnly.get(),
            testRuntimeOnly.get(),
        )
    }
}

dependencies {
    add("benchmarkImplementation", Libs.kotlinx_benchmark_runtime)
    add("benchmarkImplementation", Libs.kotlinx_benchmark_runtime_jvm)
    add("benchmarkImplementation", Libs.jmh_core)
}
```

- [ ] **Step 3: 6개 benchmark profile과 문서 생성 task를 등록**

```kotlin
benchmark {
    targets {
        register("jvm") {
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
        project.projectDir.absolutePath,
        layout.buildDirectory.dir("reports/benchmarks").get().asFile.absolutePath,
    )
}
```

- [ ] **Step 4: benchmark task 생성 여부를 검증**

Run: `./gradlew :bluetape4k-batch:tasks --all | grep -E 'jvm(H2|Postgres|Mysql)(Jdbc|R2dbc)Benchmark|generateBenchmarkDocs'`
Expected: `jvmH2JdbcBenchmark`, `jvmPostgresR2dbcBenchmark`, `jvmMysqlJdbcBenchmark`, `generateBenchmarkDocs` 가 출력된다.

- [ ] **Step 5: benchmark source set 컴파일을 검증**

Run: `./gradlew :bluetape4k-batch:benchmarkCompileKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 2: benchmark 공통 support 계층 추가

**Files:**
- Create: `utils/batch/src/benchmark/kotlin/io/bluetape4k/batch/benchmark/support/BenchmarkDatabase.kt`
- Create: `utils/batch/src/benchmark/kotlin/io/bluetape4k/batch/benchmark/support/BenchmarkScenarioParams.kt`
- Create: `utils/batch/src/benchmark/kotlin/io/bluetape4k/batch/benchmark/support/BenchmarkSchema.kt`
- Create: `utils/batch/src/benchmark/kotlin/io/bluetape4k/batch/benchmark/support/BenchmarkModels.kt`
- Create: `utils/batch/src/benchmark/kotlin/io/bluetape4k/batch/benchmark/support/BenchmarkEnvironment.kt`
- Create: `utils/batch/src/benchmark/kotlin/io/bluetape4k/batch/benchmark/support/SeedBenchmarkSupport.kt`
- Create: `utils/batch/src/benchmark/kotlin/io/bluetape4k/batch/benchmark/support/BatchJobBenchmarkSupport.kt`

- [ ] **Step 1: DB/driver/kind/params 모델을 만든다**

```kotlin
package io.bluetape4k.batch.benchmark.support

import kotlinx.benchmark.Param
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State

enum class BenchmarkDatabase(val slug: String, val displayName: String) {
    H2("h2", "H2"),
    POSTGRESQL("postgresql", "PostgreSQL"),
    MYSQL("mysql", "MySQL"),
}

enum class BenchmarkDriver(val displayName: String) {
    JDBC("JDBC + Virtual Threads"),
    R2DBC("R2DBC"),
}

enum class BenchmarkKind(val displayName: String) {
    SEED("Seed"),
    END_TO_END("End-to-End Batch Job"),
}

@State(Scope.Benchmark)
open class SeedScenarioParams {
    @Param("1000", "10000", "100000")
    var dataSize: Int = 1000

    @Param("10", "30", "60")
    var poolSize: Int = 10
}

@State(Scope.Benchmark)
open class JobScenarioParams {
    @Param("1000", "10000", "100000")
    var dataSize: Int = 1000

    @Param("10", "30", "60")
    var poolSize: Int = 10

    @Param("1", "4", "8")
    var parallelism: Int = 1
}
```

- [ ] **Step 2: benchmark source set 전용 schema와 record를 만든다**

```kotlin
package io.bluetape4k.batch.benchmark.support

import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import java.io.Serializable

object BenchmarkSourceTable : Table("benchmark_source") {
    val id = long("id").autoIncrement()
    val name = varchar("name", 255)
    val value = integer("value")
    override val primaryKey = PrimaryKey(id)
}

object BenchmarkTargetTable : LongIdTable("benchmark_target") {
    val sourceName = varchar("source_name", 255).uniqueIndex()
    val transformedValue = integer("transformed_value")
}

data class BenchmarkSourceRecord(
    val id: Long,
    val name: String,
    val value: Int,
) : Serializable {
    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }
}

data class BenchmarkTargetRecord(
    val sourceName: String,
    val transformedValue: Int,
) : Serializable {
    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }
}
```

- [ ] **Step 3: 환경 준비와 partition 계산을 공통화한다**

```kotlin
package io.bluetape4k.batch.benchmark.support

import io.bluetape4k.support.*

internal data class KeyRange(val minKeyExclusive: Long?, val maxKeyInclusive: Long?)

internal interface BenchmarkEnvironment : AutoCloseable {
    val database: BenchmarkDatabase
    fun resetSchema()
    fun truncateWorkingTables()
    fun seedSourceRows(dataSize: Int)
    fun minMaxKey(): Pair<Long, Long>

    fun partitionRanges(parallelism: Int): List<KeyRange> {
        parallelism.requirePositiveNumber("parallelism")
        val (minKey, maxKey) = minMaxKey()
        val width = ((maxKey - minKey + 1) + parallelism - 1) / parallelism
        return (0 until parallelism).map { index ->
            val start = minKey + (index * width)
            val end = minOf(maxKey, start + width - 1)
            KeyRange(
                minKeyExclusive = if (index == 0) null else start - 1,
                maxKeyInclusive = end,
            )
        }.filter { it.maxKeyInclusive != null }
    }
}
```

- [ ] **Step 4: seed/job benchmark support를 만든다**

```kotlin
internal object SeedBenchmarkSupport {
    fun runJdbc(environment: JdbcBenchmarkEnvironment, params: SeedScenarioParams): Int {
        environment.resetSchema()
        environment.seedSourceRows(params.dataSize)
        return params.dataSize
    }

    suspend fun runR2dbc(environment: R2dbcBenchmarkEnvironment, params: SeedScenarioParams): Int {
        environment.resetSchema()
        environment.seedSourceRows(params.dataSize)
        return params.dataSize
    }
}

internal object BatchJobBenchmarkSupport {
    fun runJdbc(environment: JdbcBenchmarkEnvironment, params: JobScenarioParams): Int =
        environment.runEndToEnd(params.dataSize, params.poolSize, params.parallelism)

    suspend fun runR2dbc(environment: R2dbcBenchmarkEnvironment, params: JobScenarioParams): Int =
        environment.runEndToEnd(params.dataSize, params.poolSize, params.parallelism)
}
```

- [ ] **Step 5: support 계층이 컴파일되는지 검증**

Run: `./gradlew :bluetape4k-batch:benchmarkCompileKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 3: JDBC benchmark 3종 구현

**Files:**
- Create: `utils/batch/src/benchmark/kotlin/io/bluetape4k/batch/benchmark/jdbc/AbstractJdbcBatchBenchmark.kt`
- Create: `utils/batch/src/benchmark/kotlin/io/bluetape4k/batch/benchmark/jdbc/H2JdbcBatchBenchmark.kt`
- Create: `utils/batch/src/benchmark/kotlin/io/bluetape4k/batch/benchmark/jdbc/PostgreSqlJdbcBatchBenchmark.kt`
- Create: `utils/batch/src/benchmark/kotlin/io/bluetape4k/batch/benchmark/jdbc/MySqlJdbcBatchBenchmark.kt`

- [ ] **Step 1: JDBC 공통 benchmark 베이스를 작성한다**

```kotlin
package io.bluetape4k.batch.benchmark.jdbc

import io.bluetape4k.batch.benchmark.support.*
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.Measurement
import kotlinx.benchmark.Mode
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlinx.benchmark.TearDown
import kotlinx.benchmark.Warmup
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 2)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
abstract class AbstractJdbcBatchBenchmark {
    protected abstract val database: BenchmarkDatabase
    private lateinit var environment: JdbcBenchmarkEnvironment

    @Setup
    fun setup() {
        environment = JdbcBenchmarkEnvironment(database)
    }

    @TearDown
    fun teardown() {
        environment.close()
    }

    @Benchmark
    fun seedBenchmark(params: SeedScenarioParams): Int =
        SeedBenchmarkSupport.runJdbc(environment, params)

    @Benchmark
    fun endToEndBatchJobBenchmark(params: JobScenarioParams): Int =
        BatchJobBenchmarkSupport.runJdbc(environment, params)
}
```

- [ ] **Step 2: DB별 concrete class 3개를 추가한다**

```kotlin
class H2JdbcBatchBenchmark : AbstractJdbcBatchBenchmark() {
    override val database: BenchmarkDatabase = BenchmarkDatabase.H2
}

class PostgreSqlJdbcBatchBenchmark : AbstractJdbcBatchBenchmark() {
    override val database: BenchmarkDatabase = BenchmarkDatabase.POSTGRESQL
}

class MySqlJdbcBatchBenchmark : AbstractJdbcBatchBenchmark() {
    override val database: BenchmarkDatabase = BenchmarkDatabase.MYSQL
}
```

- [ ] **Step 3: JDBC 환경이 HikariCP/Testcontainers를 올바르게 쓰도록 채운다**

```kotlin
internal class JdbcBenchmarkEnvironment(
    override val database: BenchmarkDatabase,
) : BenchmarkEnvironment {
    private val dataSource = when (database) {
        BenchmarkDatabase.H2 -> createH2DataSource(poolSize = 10)
        BenchmarkDatabase.POSTGRESQL -> createPostgresDataSource(poolSize = 10)
        BenchmarkDatabase.MYSQL -> createMySqlDataSource(poolSize = 10)
    }

    override fun resetSchema() {
        // SchemaUtils.drop/create + job/source/target table 초기화
    }

    override fun truncateWorkingTables() {
        // source/target/job execution table truncate
    }

    override fun seedSourceRows(dataSize: Int) {
        // batchInsert로 dataSize 만큼 적재
    }

    fun runEndToEnd(dataSize: Int, poolSize: Int, parallelism: Int): Int {
        // seed 완료 상태에서 sequential or partitioned batch job 실행
        return dataSize
    }

    override fun close() {
        runCatching { dataSource.close() }
    }
}
```

- [ ] **Step 4: H2 JDBC profile을 실행한다**

Run: `./gradlew :bluetape4k-batch:jvmH2JdbcBenchmark`
Expected: BUILD SUCCESSFUL, `seedBenchmark` 와 `endToEndBatchJobBenchmark` 결과 JSON이 생성된다.

- [ ] **Step 5: PostgreSQL JDBC profile을 실행한다**

Run: `./gradlew :bluetape4k-batch:jvmPostgresJdbcBenchmark`
Expected: BUILD SUCCESSFUL, PostgreSQL Testcontainers가 benchmark 측정 전 단계에서 자동 기동된다.

---

### Task 4: R2DBC benchmark 3종 구현

**Files:**
- Create: `utils/batch/src/benchmark/kotlin/io/bluetape4k/batch/benchmark/r2dbc/AbstractR2dbcBatchBenchmark.kt`
- Create: `utils/batch/src/benchmark/kotlin/io/bluetape4k/batch/benchmark/r2dbc/H2R2dbcBatchBenchmark.kt`
- Create: `utils/batch/src/benchmark/kotlin/io/bluetape4k/batch/benchmark/r2dbc/PostgreSqlR2dbcBatchBenchmark.kt`
- Create: `utils/batch/src/benchmark/kotlin/io/bluetape4k/batch/benchmark/r2dbc/MySqlR2dbcBatchBenchmark.kt`

- [ ] **Step 1: R2DBC 공통 benchmark 베이스를 작성한다**

```kotlin
package io.bluetape4k.batch.benchmark.r2dbc

import io.bluetape4k.batch.benchmark.support.*
import io.bluetape4k.junit5.coroutines.runSuspendIO
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.Measurement
import kotlinx.benchmark.Mode
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlinx.benchmark.TearDown
import kotlinx.benchmark.Warmup
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 2)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
abstract class AbstractR2dbcBatchBenchmark {
    protected abstract val database: BenchmarkDatabase
    private lateinit var environment: R2dbcBenchmarkEnvironment

    @Setup
    fun setup() = runSuspendIO {
        environment = R2dbcBenchmarkEnvironment(database)
    }

    @TearDown
    fun teardown() = runSuspendIO {
        environment.close()
    }

    @Benchmark
    fun seedBenchmark(params: SeedScenarioParams): Int = runSuspendIO {
        SeedBenchmarkSupport.runR2dbc(environment, params)
    }

    @Benchmark
    fun endToEndBatchJobBenchmark(params: JobScenarioParams): Int = runSuspendIO {
        BatchJobBenchmarkSupport.runR2dbc(environment, params)
    }
}
```

- [ ] **Step 2: DB별 concrete class 3개를 추가한다**

```kotlin
class H2R2dbcBatchBenchmark : AbstractR2dbcBatchBenchmark() {
    override val database: BenchmarkDatabase = BenchmarkDatabase.H2
}

class PostgreSqlR2dbcBatchBenchmark : AbstractR2dbcBatchBenchmark() {
    override val database: BenchmarkDatabase = BenchmarkDatabase.POSTGRESQL
}

class MySqlR2dbcBatchBenchmark : AbstractR2dbcBatchBenchmark() {
    override val database: BenchmarkDatabase = BenchmarkDatabase.MYSQL
}
```

- [ ] **Step 3: R2DBC 환경이 r2dbc-pool/Testcontainers를 올바르게 쓰도록 채운다**

```kotlin
internal class R2dbcBenchmarkEnvironment(
    override val database: BenchmarkDatabase,
) : BenchmarkEnvironment {
    suspend fun resetSchema() {
        // suspendTransaction + SchemaUtils.drop/create
    }

    suspend fun seedSourceRows(dataSize: Int) {
        // batchInsert
    }

    suspend fun runEndToEnd(dataSize: Int, poolSize: Int, parallelism: Int): Int {
        // seed 완료 후 sequential or partitioned batch job 실행
        return dataSize
    }

    override fun close() {
        // pool.close()
    }
}
```

- [ ] **Step 4: H2 R2DBC profile을 실행한다**

Run: `./gradlew :bluetape4k-batch:jvmH2R2dbcBenchmark`
Expected: BUILD SUCCESSFUL, H2 R2DBC JSON 결과가 생성된다.

- [ ] **Step 5: PostgreSQL R2DBC profile을 실행한다**

Run: `./gradlew :bluetape4k-batch:jvmPostgresR2dbcBenchmark`
Expected: BUILD SUCCESSFUL, PostgreSQL Testcontainers가 benchmark 측정 전 단계에서 자동 기동된다.

---

### Task 5: Markdown exporter와 README 링크를 구현한다

**Files:**
- Create: `utils/batch/src/benchmark/kotlin/io/bluetape4k/batch/benchmark/support/BenchmarkMarkdownExporter.kt`
- Create: `utils/batch/src/benchmark/kotlin/io/bluetape4k/batch/benchmark/support/BenchmarkDocsGenerator.kt`
- Create: `utils/batch/docs/benchmark/README.md`
- Create: `utils/batch/docs/benchmark/README.ko.md`
- Create: `utils/batch/docs/benchmark/h2.md`
- Create: `utils/batch/docs/benchmark/postgresql.md`
- Create: `utils/batch/docs/benchmark/mysql.md`
- Modify: `utils/batch/README.md`
- Modify: `utils/batch/README.ko.md`

- [ ] **Step 1: JMH JSON을 파싱하는 모델과 exporter를 만든다**

```kotlin
package io.bluetape4k.batch.benchmark.support

import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.nio.file.Path

internal data class BenchmarkRow(
    val database: BenchmarkDatabase,
    val driver: BenchmarkDriver,
    val benchmarkKind: BenchmarkKind,
    val dataSize: Int,
    val poolSize: Int,
    val parallelism: Int?,
    val samples: Int,
    val score: Double,
    val scoreError: Double,
    val unit: String,
) {
    val throughputRowsPerSecond: Double
        get() = score * dataSize
}

internal object BenchmarkMarkdownExporter {
    private val mapper = jacksonObjectMapper().findAndRegisterModules()

    fun readRows(reportDir: Path): List<BenchmarkRow> {
        // profile별 JSON 파일을 읽어 params/dataSize/poolSize/parallelism 및 score/samples 추출
        return emptyList()
    }

    fun writeAll(projectDir: Path, rows: List<BenchmarkRow>) {
        // README.md / README.ko.md / h2.md / postgresql.md / mysql.md 생성
    }
}
```

- [ ] **Step 2: 문서 생성 entry point를 만든다**

```kotlin
package io.bluetape4k.batch.benchmark.support

import java.nio.file.Paths

fun main(args: Array<String>) {
    val projectDir = Paths.get(args[0])
    val reportDir = Paths.get(args[1])
    val rows = BenchmarkMarkdownExporter.readRows(reportDir)
    BenchmarkMarkdownExporter.writeAll(projectDir, rows)
}
```

- [ ] **Step 3: H2 + PostgreSQL 조합으로 문서 생성 흐름을 검증한다**

Run: `./gradlew :bluetape4k-batch:jvmH2JdbcBenchmark :bluetape4k-batch:jvmH2R2dbcBenchmark :bluetape4k-batch:jvmPostgresJdbcBenchmark :bluetape4k-batch:jvmPostgresR2dbcBenchmark :bluetape4k-batch:generateBenchmarkDocs`
Expected: `utils/batch/docs/benchmark/README.md`, `README.ko.md`, `h2.md`, `postgresql.md`, `mysql.md` 가 생성 또는 갱신된다.

- [ ] **Step 4: module README에 요약표와 direct link를 추가한다**

```markdown
## Benchmarks

| DB | Summary | Details |
|----|---------|---------|
| H2 | Compare JDBC vs R2DBC across seed and end-to-end scenarios | [H2 benchmark details](docs/benchmark/h2.md) |
| PostgreSQL | Compare JDBC vs R2DBC across seed and end-to-end scenarios | [PostgreSQL benchmark details](docs/benchmark/postgresql.md) |
| MySQL | Compare JDBC vs R2DBC across seed and end-to-end scenarios | [MySQL benchmark details](docs/benchmark/mysql.md) |

- [Benchmark hub](docs/benchmark/README.md)
- Tasks: `./gradlew :bluetape4k-batch:jvmH2JdbcBenchmark`, `./gradlew :bluetape4k-batch:jvmPostgresR2dbcBenchmark`
```

- [ ] **Step 5: 한국어 README도 같은 링크 구조로 맞춘다**

```markdown
## Benchmarks

| DB | 요약 | 상세 문서 |
|----|------|-----------|
| H2 | Seed / End-to-End 기준 JDBC vs R2DBC 비교 | [H2 상세 결과](docs/benchmark/h2.md) |
| PostgreSQL | Seed / End-to-End 기준 JDBC vs R2DBC 비교 | [PostgreSQL 상세 결과](docs/benchmark/postgresql.md) |
| MySQL | Seed / End-to-End 기준 JDBC vs R2DBC 비교 | [MySQL 상세 결과](docs/benchmark/mysql.md) |

- [Benchmark 문서 허브](docs/benchmark/README.ko.md)
- 실행 예시: `./gradlew :bluetape4k-batch:jvmH2JdbcBenchmark`, `./gradlew :bluetape4k-batch:jvmMysqlR2dbcBenchmark`
```

---

### Task 6: legacy benchmark를 reference로 남기고 최종 검증/로그를 마무리한다

**Files:**
- Modify: `utils/batch/src/test/kotlin/io/bluetape4k/batch/jdbc/BatchJdbcBenchmarkTest.kt`
- Modify: `utils/batch/src/test/kotlin/io/bluetape4k/batch/r2dbc/BatchR2dbcBenchmarkTest.kt`
- Modify: `docs/testlogs/2026-04.md`
- Modify: `docs/superpowers/index/2026-04.md`
- Modify: `docs/superpowers/INDEX.md`

- [ ] **Step 1: legacy benchmark 클래스 설명을 새 체계 기준으로 바꾼다**

```kotlin
/**
 * Legacy JDBC benchmark reference.
 *
 * 공식 benchmark 엔트리는 `src/benchmark/kotlin/io/bluetape4k/batch/benchmark/jdbc/*` 이며,
 * 이 테스트 클래스는 이전 `measureTimeMillis` 수치를 비교할 때만 사용한다.
 */
class BatchJdbcBenchmarkTest : AbstractBatchJdbcTest()
```

```kotlin
/**
 * Legacy R2DBC benchmark reference.
 *
 * 공식 benchmark 엔트리는 `src/benchmark/kotlin/io/bluetape4k/batch/benchmark/r2dbc/*` 이며,
 * 이 테스트 클래스는 이전 `measureTimeMillis` 수치를 비교할 때만 사용한다.
 */
class BatchR2dbcBenchmarkTest : AbstractBatchR2dbcTest()
```

- [ ] **Step 2: 회귀 테스트와 benchmark 실행을 모두 확인한다**

Run: `./gradlew :bluetape4k-batch:test`
Expected: legacy benchmark를 포함한 기존 테스트가 통과한다.

Run: `./gradlew :bluetape4k-batch:jvmH2JdbcBenchmark :bluetape4k-batch:jvmH2R2dbcBenchmark`
Expected: H2 benchmark JSON 결과가 생성된다.

Run: `./gradlew :bluetape4k-batch:jvmPostgresJdbcBenchmark :bluetape4k-batch:jvmPostgresR2dbcBenchmark`
Expected: PostgreSQL benchmark JSON 결과가 생성된다.

Run: `./gradlew :bluetape4k-batch:generateBenchmarkDocs`
Expected: `docs/benchmark/*.md` 와 `README.md`/`README.ko.md` 링크가 최신 상태가 된다.

- [ ] **Step 3: testlog와 superpowers 인덱스를 갱신한다**

- `docs/testlogs/2026-04.md` 맨 위에 benchmark source set 도입, 실행한 benchmark task, 문서 생성 결과를 새 행으로 추가한다.
- `docs/superpowers/index/2026-04.md` 에 이 작업 행을 추가하고 상태를 실제 구현 결과에 맞게 `✅` 또는 `🔄` 로 기록한다.
- `docs/superpowers/INDEX.md` 의 요약 건수와 `2026-04` 항목 수를 실제 행 개수로 맞춘다.

- [ ] **Step 4: 완료 기준을 다시 체크한다**

- `kotlinx-benchmark` source set 컴파일 성공
- H2 1개 + 네트워크 DB 1개 benchmark 실행 성공
- `utils/batch/docs/benchmark/*.md` 에 표와 Mermaid graph 포함
- `utils/batch/README.md`, `utils/batch/README.ko.md` 에 상세 링크 연결
- legacy benchmark가 reference로만 남아 있고 새 체계가 공식 경로가 됨

---

## Self-Review

- **Spec coverage:** benchmark source set, 6개 profile, Testcontainers, seed/end-to-end 분리, DB별 문서, Mermaid graph, README 링크, legacy benchmark 유지, testlog/index 갱신까지 모두 작업으로 매핑했다.
- **Placeholder scan:** `TBD`, `TODO`, `implement later`, 미정 파일명, 미정 task 이름을 쓰지 않았다. build task와 파일 경로는 모두 구체적으로 적었다.
- **Type consistency:** `BenchmarkDatabase`, `SeedScenarioParams`, `JobScenarioParams`, `JdbcBenchmarkEnvironment`, `R2dbcBenchmarkEnvironment`, `BenchmarkMarkdownExporter`, `BenchmarkDocsGeneratorKt` 이름을 전 구간에서 일관되게 사용했다.
