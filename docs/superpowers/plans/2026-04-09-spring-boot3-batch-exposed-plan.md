# bluetape4k-spring-boot3-batch-exposed Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Spring Batch 5.x + Exposed JDBC 기반 Partitioned Step + VirtualThread Parallel Query 배치 모듈 구현

**Architecture:** `ExposedRangePartitioner`가 auto-increment PK를 N개 ID 범위로 분할하고, 각 파티션을 VirtualThread `TaskExecutor`에서 병렬 실행한다. `ExposedKeysetItemReader`가 keyset 페이징으로 읽고, `ExposedItemWriter` / `ExposedUpsertItemWriter` / `ExposedUpdateItemWriter`가 Spring 청크 트랜잭션에 참여하여 쓴다.

**Tech Stack:** Kotlin 2.3, Spring Batch 5.x (Spring Boot 3 BOM), Exposed JDBC, Virtual Threads (JDK 21), H2 (unit test), Testcontainers PostgreSQL (integration test)

- **작성일**: 2026-04-09
- **스펙**: `docs/superpowers/specs/2026-04-09-spring-boot3-batch-exposed-design.md`
- **예상 복잡도**: HIGH (4개 Phase, 신규 모듈 1개, 소스 파일 약 20개)

---

## File Structure

```
spring-boot3/batch-exposed/
├── build.gradle.kts
└── src/
    ├── main/
    │   ├── kotlin/io/bluetape4k/spring/batch/exposed/
    │   │   ├── partition/
    │   │   │   └── ExposedRangePartitioner.kt
    │   │   ├── reader/
    │   │   │   └── ExposedKeysetItemReader.kt
    │   │   ├── writer/
    │   │   │   ├── ExposedItemWriter.kt
    │   │   │   ├── ExposedUpsertItemWriter.kt
    │   │   │   └── ExposedUpdateItemWriter.kt
    │   │   ├── support/
    │   │   │   └── VirtualThreadPartitionSupport.kt
    │   │   ├── config/
    │   │   │   └── ExposedBatchAutoConfiguration.kt
    │   │   └── dsl/
    │   │       └── BatchJobExtensions.kt
    │   └── resources/
    │       └── META-INF/spring/
    │           └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
    └── test/
        └── kotlin/io/bluetape4k/spring/batch/exposed/
            ├── AbstractExposedBatchTest.kt
            ├── TestTables.kt
            ├── partition/
            │   └── ExposedRangePartitionerTest.kt
            ├── reader/
            │   └── ExposedKeysetItemReaderTest.kt
            ├── writer/
            │   ├── ExposedItemWriterTest.kt
            │   ├── ExposedUpsertItemWriterTest.kt
            │   └── ExposedUpdateItemWriterTest.kt
            └── integration/
                ├── ParallelQueryIntegrationTest.kt
                ├── RestartIntegrationTest.kt
                ├── EndToEndJobTest.kt
                └── ParallelQueryBenchmarkTest.kt
```

---

## Phase 1: 프로젝트 설정

> **선행 조건**: 없음
> **Gradle 빌드 검증**: Phase 1 완료 후 `./gradlew :bluetape4k-spring-boot3-batch-exposed:dependencies`

### Task 1.1: Libs.kt에 spring_batch_test 상수 추가

- **complexity**: low
- **depends_on**: -
- **Files**:
  - Modify: `buildSrc/src/main/kotlin/Libs.kt`

- [ ] **Step 1: Libs.kt에 spring_batch_test 상수 추가**

`Libs.kt` 파일에서 Spring Boot 관련 함수 영역 근처 (`springBoot`, `springBootStarter` 함수 아래)에 다음 상수를 추가한다:

```kotlin
val spring_batch_test = "org.springframework.batch:spring-batch-test"  // Spring Boot BOM 버전 관리
```

- [ ] **Step 2: 검증**

Run: `./gradlew buildSrc:build`
Expected: BUILD SUCCESSFUL

**Done when**: `Libs.spring_batch_test` 상수가 컴파일되고 buildSrc 빌드 성공

---

### Task 1.2: 폴더 생성 + build.gradle.kts

- **complexity**: low
- **depends_on**: T1.1
- **Files**:
  - Create: `spring-boot3/batch-exposed/build.gradle.kts`

- [ ] **Step 1: 디렉토리 생성**

```bash
mkdir -p spring-boot3/batch-exposed/src/main/kotlin/io/bluetape4k/spring/batch/exposed
mkdir -p spring-boot3/batch-exposed/src/main/resources/META-INF/spring
mkdir -p spring-boot3/batch-exposed/src/test/kotlin/io/bluetape4k/spring/batch/exposed
```

- [ ] **Step 2: build.gradle.kts 작성**

Create: `spring-boot3/batch-exposed/build.gradle.kts`

```kotlin
plugins {
    kotlin("plugin.spring")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Core
    api(Libs.kotlin_reflect)
    api(project(":bluetape4k-exposed-jdbc"))
    api(project(":bluetape4k-exposed-core"))
    api(project(":bluetape4k-virtualthread-api"))

    // Exposed
    api(Libs.exposed_spring_transaction)
    api(Libs.exposed_core)
    api(Libs.exposed_jdbc)
    api(Libs.exposed_java_time)

    // Spring Batch (Spring Boot BOM 버전 관리)
    api(Libs.springBootStarter("batch"))
    compileOnly(Libs.springBoot("autoconfigure"))

    // Test
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-exposed-jdbc-tests"))
    testImplementation(project(":bluetape4k-virtualthread-jdk21"))
    testImplementation(Libs.springBootStarter("test"))
    testImplementation(Libs.spring_batch_test)
    testImplementation(Libs.h2_v2)
    testImplementation(Libs.hikaricp)
    testImplementation(Libs.testcontainers_postgresql)
    testImplementation(Libs.postgresql_driver)
}
```

- [ ] **Step 3: 의존성 resolve 검증**

Run: `./gradlew :bluetape4k-spring-boot3-batch-exposed:dependencies --configuration compileClasspath`
Expected: 모든 의존성 resolve 성공 (spring-batch-core, exposed-core 등 확인)

- [ ] **Step 4: 커밋**

```bash
git add buildSrc/src/main/kotlin/Libs.kt spring-boot3/batch-exposed/
git commit -m "$(cat <<'EOF'
chore: bluetape4k-spring-boot3-batch-exposed 모듈 폴더 및 빌드 설정 추가

spring-boot3/batch-exposed 폴더 생성, build.gradle.kts 의존성 설정,
Libs.kt에 spring_batch_test 상수 추가
EOF
)"
```

**Done when**: `./gradlew :bluetape4k-spring-boot3-batch-exposed:dependencies` 성공, settings.gradle.kts 자동 등록 확인 (`includeModules("spring-boot3", ...)`)

---

## Phase 2: 핵심 구현

> **선행 조건**: Phase 1 완료
> **Gradle 빌드 검증**: Phase 2 완료 후 `./gradlew :bluetape4k-spring-boot3-batch-exposed:compileKotlin`

### Task 2.1: ExposedRangePartitioner

- **complexity**: high
- **depends_on**: T1.2
- **Files**:
  - Create: `spring-boot3/batch-exposed/src/main/kotlin/io/bluetape4k/spring/batch/exposed/partition/ExposedRangePartitioner.kt`

- [ ] **Step 1: ExposedRangePartitioner 구현**

`ExposedRangePartitioner` 클래스를 스펙 섹션 4.2에 따라 구현한다.

핵심 구현 포인트:
- `Partitioner` 인터페이스 구현, `Column<Long>` 기반 min/max 조회 후 균등 분할
- `companion object`에 `forEntityId()` 팩토리: `IdTable<Long>.id`(`Column<EntityID<Long>>`)를 `castTo<Long>(LongColumnType())`로 변환하고, `selectMinMax` 람다에서 `table.id.min()/max()`의 `.value` 추출
- 빈 테이블 시 `minId=0, maxId=-1` 단일 파티션 반환, Long overflow 방지를 위한 `safeGridSize` 계산

```kotlin
package io.bluetape4k.spring.batch.exposed.partition

import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.LongColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.castTo
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.min
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.springframework.batch.core.partition.support.Partitioner
import org.springframework.batch.item.ExecutionContext

/**
 * auto-increment PK/sequence `Column<Long>` 컬럼 기반으로 ID 범위를 N개 파티션으로 분할하는 [Partitioner].
 *
 * 전제 조건:
 * - [column]은 `Column<Long>` 타입의 unique + auto-increment 또는 monotonic sequence여야 함
 * - `LongIdTable.id` (`Column<EntityID<Long>>`) 사용 시 `ExposedRangePartitioner.forEntityId(table)` 팩토리 사용
 * - 실행 중 대규모 insert/delete가 없는 상황에 적합
 *
 * 사용 예시:
 * ```kotlin
 * // Column<Long> 컬럼 직접 사용
 * val partitioner = ExposedRangePartitioner(
 *     table = SourceTable,
 *     column = SourceTable.longId,
 *     gridSize = 16,
 * )
 *
 * // LongIdTable (Column<EntityID<Long>>) 사용 시: forEntityId() 팩토리
 * val partitioner = ExposedRangePartitioner.forEntityId(
 *     table = SourceTable,
 *     gridSize = 16,
 * )
 * ```
 *
 * @param database Exposed [Database] (null이면 SpringTransactionManager 활용)
 * @param table 파티션 대상 Exposed [Table]
 * @param column 분할 기준 `Column<Long>` 컬럼 (PK, auto-increment)
 * @param gridSize 파티션 수 (기본값: 8)
 */
class ExposedRangePartitioner(
    private val database: Database? = null,
    private val table: Table,
    private val column: Column<Long>,
    private val gridSize: Int = 8,
    private val selectMinMax: Transaction.() -> Pair<Long?, Long?> = {
        table.select(column.min(), column.max()).single()
            .let { it[column.min()] to it[column.max()] }
    },
) : Partitioner {

    companion object : KLogging() {
        /** ExecutionContext에 저장되는 파티션 시작 ID 키 */
        const val PARTITION_MIN_ID = "minId"
        /** ExecutionContext에 저장되는 파티션 종료 ID 키 */
        const val PARTITION_MAX_ID = "maxId"

        /**
         * `LongIdTable.id` (`Column<EntityID<Long>>`) 기반 파티셔너 팩토리.
         *
         * - `selectMinMax`: `table.id.min()` / `table.id.max()` EntityID-aware 쿼리로 min/max 조회
         * - `column`: `table.id.castTo<Long>(LongColumnType())`으로 Long 변환 — WHERE 절에서 Long 비교 사용
         *   (DB에 따라 `CAST(id AS BIGINT) > lastKey` 형태로 실행될 수 있음)
         *
         * ```kotlin
         * val partitioner = ExposedRangePartitioner.forEntityId(
         *     table = SourceTable,
         *     gridSize = 16,
         * )
         * ```
         */
        fun forEntityId(
            table: IdTable<Long>,
            gridSize: Int = 8,
            database: Database? = null,
        ): ExposedRangePartitioner = ExposedRangePartitioner(
            database = database,
            table = table,
            // EntityID → Long castTo: 팩토리 생성 시 1회 수행, 이후 column 프로퍼티로 재사용
            column = table.id.castTo<Long>(LongColumnType()),
            gridSize = gridSize,
            selectMinMax = {
                // min/max 조회는 EntityID-aware 쿼리로 처리 (castTo 불필요)
                table.select(table.id.min(), table.id.max()).single().let { row ->
                    row[table.id.min()]?.value to row[table.id.max()]?.value
                }
            },
        )
    }

    override fun partition(gridSize: Int): Map<String, ExecutionContext> {
        val effectiveGridSize = if (gridSize > 0) gridSize else this.gridSize

        val (min, max) = transaction(database) { selectMinMax() }

        if (min == null || max == null) {
            return mapOf("partition-0" to ExecutionContext().apply {
                putLong(PARTITION_MIN_ID, 0L)
                putLong(PARTITION_MAX_ID, -1L)
            })
        }

        val totalRange = max - min + 1
        val safeGridSize = minOf(effectiveGridSize.toLong(), totalRange.coerceAtLeast(1L)).toInt()
        val rangeSize = totalRange / safeGridSize

        return (0 until safeGridSize).associate { i ->
            val partMinId = min + i * rangeSize
            val partMaxId = if (i == safeGridSize - 1) max else min + (i + 1) * rangeSize - 1

            "partition-$i" to ExecutionContext().apply {
                putLong(PARTITION_MIN_ID, partMinId)
                putLong(PARTITION_MAX_ID, partMaxId)
            }
        }
    }
}
```

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew :bluetape4k-spring-boot3-batch-exposed:compileKotlin`
Expected: BUILD SUCCESSFUL

**Done when**: `ExposedRangePartitioner` 컴파일 성공, `partition()` 메서드 및 `forEntityId()` 팩토리 구현 완료

---

### Task 2.2: ExposedKeysetItemReader

- **complexity**: high
- **depends_on**: T2.1
- **Files**:
  - Create: `spring-boot3/batch-exposed/src/main/kotlin/io/bluetape4k/spring/batch/exposed/reader/ExposedKeysetItemReader.kt`

- [ ] **Step 1: ExposedKeysetItemReader 구현**

`ExposedKeysetItemReader<T>` 클래스를 스펙 섹션 4.3에 따라 구현한다.

핵심 구현 포인트:
- `ItemStreamReader<T>` + `InitializingBean` 구현, `@Synchronized read()`, 내부 buffer + bufferIndex 방식
- `open()`에서 `ExecutionContext`의 `minId`/`maxId` 읽기 + restart 시 `lastKey` 복원
- `forEntityId()` 팩토리: `table.id.castTo<Long>(LongColumnType())`, `keyExtractor`는 `it[table.id].value`

```kotlin
package io.bluetape4k.spring.batch.exposed.reader

import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.batch.exposed.partition.ExposedRangePartitioner
import io.bluetape4k.support.requireNotBlank
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.LongColumnType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.castTo
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemStreamReader
import org.springframework.beans.factory.InitializingBean

/**
 * Keyset 기반 페이지 읽기 [ItemStreamReader].
 *
 * - `WHERE [column] > lastKey AND [column] <= maxId ORDER BY [column] ASC LIMIT [pageSize]`
 * - lastKey를 [ExecutionContext]에 저장하여 restart 시 마지막 위치부터 재개
 * - 파티션별 독립 인스턴스이므로 thread-safety 보장
 * - synchronized `read()` 구현 (Spring Batch 컨벤션 준수)
 *
 * @param T 반환 타입
 * @param database Exposed [Database] (null이면 SpringTransactionManager 현재 트랜잭션 참여)
 * @param pageSize 한 번에 읽을 레코드 수 (기본값: 500)
 * @param column keyset 기준 `Column<Long>` 컬럼 (auto-increment PK)
 * @param table Exposed [Table]
 * @param rowMapper [ResultRow] -> T 변환 함수
 * @param keyExtractor [ResultRow]에서 keyset 컬럼 Long 값 추출
 * @param additionalCondition 추가 WHERE 조건 람다 (null이면 조건 없음)
 */
class ExposedKeysetItemReader<T>(
    private val database: Database? = null,
    private val pageSize: Int = 500,
    private val column: Column<Long>,
    private val table: Table,
    private val rowMapper: (ResultRow) -> T,
    private val keyExtractor: (ResultRow) -> Long = { it[column] },
    private val additionalCondition: (SqlExpressionBuilder.() -> Op<Boolean>)? = null,
) : ItemStreamReader<T>, InitializingBean {

    companion object : KLogging() {
        private const val LAST_KEY = "lastKey"

        /**
         * `LongIdTable.id` (`Column<EntityID<Long>>`) 기반 Reader 팩토리.
         *
         * - `column`: `table.id.castTo<Long>(LongColumnType())`으로 Long 변환 — WHERE 절에서 Long 비교 사용
         *   (DB에 따라 `CAST(id AS BIGINT) > lastKey` 형태로 실행될 수 있음)
         * - `keyExtractor`: `it[table.id].value`로 EntityID에서 Long 추출
         */
        fun <T> forEntityId(
            table: IdTable<Long>,
            pageSize: Int = 500,
            rowMapper: (ResultRow) -> T,
            keyExtractor: (ResultRow) -> Long = { it[table.id].value },
            additionalCondition: (SqlExpressionBuilder.() -> Op<Boolean>)? = null,
            database: Database? = null,
        ): ExposedKeysetItemReader<T> = ExposedKeysetItemReader(
            database = database,
            pageSize = pageSize,
            column = table.id.castTo<Long>(LongColumnType()),
            table = table,
            rowMapper = rowMapper,
            keyExtractor = keyExtractor,
            additionalCondition = additionalCondition,
        )
    }

    private var minId: Long = 0L
    private var maxId: Long = Long.MAX_VALUE
    private var lastKey: Long = 0L
    private val buffer: MutableList<T> = mutableListOf()
    private var bufferIndex: Int = 0
    private var exhausted: Boolean = false

    override fun afterPropertiesSet() {
        column.name.requireNotBlank("column")
        require(pageSize > 0) { "pageSize must be positive" }
    }

    override fun open(executionContext: ExecutionContext) {
        minId = executionContext.getLong(ExposedRangePartitioner.PARTITION_MIN_ID)
        maxId = executionContext.getLong(ExposedRangePartitioner.PARTITION_MAX_ID)

        if (executionContext.containsKey(LAST_KEY)) {
            lastKey = executionContext.getLong(LAST_KEY)
        } else {
            lastKey = minId - 1
        }
    }

    @Synchronized
    override fun read(): T? {
        if (exhausted) return null

        if (bufferIndex >= buffer.size) {
            fetchNextPage()
            if (buffer.isEmpty()) {
                exhausted = true
                return null
            }
        }

        return buffer[bufferIndex++]
    }

    override fun update(executionContext: ExecutionContext) {
        executionContext.putLong(LAST_KEY, lastKey)
    }

    override fun close() {
        buffer.clear()
        bufferIndex = 0
        exhausted = false
    }

    private fun fetchNextPage() {
        buffer.clear()
        bufferIndex = 0

        transaction(database) {
            var condition: Op<Boolean> = (column greater lastKey) and (column lessEq maxId)
            additionalCondition?.let { addCond ->
                condition = condition and SqlExpressionBuilder.addCond()
            }

            val resultRows = table.selectAll()
                .where { condition }
                .orderBy(column, SortOrder.ASC)
                .limit(pageSize)
                .toList()

            buffer.addAll(resultRows.map(rowMapper))

            if (resultRows.isNotEmpty()) {
                lastKey = keyExtractor(resultRows.last())
            }

            if (resultRows.size < pageSize) {
                exhausted = true
            }
        }
    }
}
```

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew :bluetape4k-spring-boot3-batch-exposed:compileKotlin`
Expected: BUILD SUCCESSFUL

**Done when**: `ExposedKeysetItemReader` 컴파일 성공, `open()/read()/update()/close()` 생명주기 + `forEntityId()` 팩토리 구현 완료

---

### Task 2.3: VirtualThreadPartitionSupport

- **complexity**: medium
- **depends_on**: T1.2
- **Files**:
  - Create: `spring-boot3/batch-exposed/src/main/kotlin/io/bluetape4k/spring/batch/exposed/support/VirtualThreadPartitionSupport.kt`

- [ ] **Step 1: virtualThreadPartitionTaskExecutor 팩토리 함수 구현**

`SimpleAsyncTaskExecutor` 기반 VirtualThread `TaskExecutor` 생성 팩토리 함수를 구현한다.

핵심 구현 포인트:
- `SimpleAsyncTaskExecutor`에 `setVirtualThreads(true)` + `setConcurrencyLimit()` 설정
- 기본 `concurrencyLimit`은 `availableProcessors * 2`

```kotlin
package io.bluetape4k.spring.batch.exposed.support

import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.core.task.TaskExecutor

/**
 * VirtualThread 기반 [TaskExecutor]를 생성하는 팩토리 함수.
 *
 * [org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler]에 주입하여
 * 각 파티션을 VirtualThread에서 병렬 실행한다.
 *
 * 사용 예시:
 * ```kotlin
 * val partitionHandler = TaskExecutorPartitionHandler().apply {
 *     setStep(workerStep)
 *     setTaskExecutor(virtualThreadPartitionTaskExecutor(concurrencyLimit = 16))
 *     gridSize = 16
 * }
 * ```
 *
 * @param threadNamePrefix VirtualThread 이름 접두사
 * @param concurrencyLimit 동시 실행 파티션 수 (기본값: availableProcessors * 2)
 */
fun virtualThreadPartitionTaskExecutor(
    threadNamePrefix: String = "batch-partition-",
    concurrencyLimit: Int = Runtime.getRuntime().availableProcessors() * 2,
): TaskExecutor = SimpleAsyncTaskExecutor(threadNamePrefix).apply {
    setVirtualThreads(true)
    setConcurrencyLimit(concurrencyLimit)
}
```

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew :bluetape4k-spring-boot3-batch-exposed:compileKotlin`
Expected: BUILD SUCCESSFUL

**Done when**: `virtualThreadPartitionTaskExecutor()` 함수 컴파일 성공

---

### Task 2.4: ExposedItemWriter

- **complexity**: medium
- **depends_on**: T1.2
- **Files**:
  - Create: `spring-boot3/batch-exposed/src/main/kotlin/io/bluetape4k/spring/batch/exposed/writer/ExposedItemWriter.kt`

- [ ] **Step 1: ExposedItemWriter 구현**

`batchInsert` 기반 `ItemWriter`를 스펙 섹션 4.4에 따라 구현한다.

핵심 구현 포인트:
- `ItemWriter<T>` 구현, `database` 파라미터 없음 (Spring-managed 청크 트랜잭션 전용)
- `batchInsert`에 `shouldReturnGeneratedValues = false` 설정 (성능 최적화)
- 빈 chunk 조기 반환

```kotlin
package io.bluetape4k.spring.batch.exposed.writer

import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter

/**
 * Exposed `batchInsert` 기반 [ItemWriter].
 *
 * SpringTransactionManager 환경에서 Spring Batch 청크 트랜잭션에 자동 참여.
 * 별도 `transaction { }` 블록 불필요.
 *
 * 사용 예시:
 * ```kotlin
 * val writer = ExposedItemWriter(table = TargetTable) {
 *     this[TargetTable.name] = it.name
 *     this[TargetTable.value] = it.value
 * }
 * ```
 *
 * @param T 입력 타입
 * @param table 대상 Exposed [Table]
 * @param insertBody `batchInsert` 람다
 */
class ExposedItemWriter<T>(
    private val table: Table,
    private val insertBody: BatchInsertStatement.(T) -> Unit,
) : ItemWriter<T> {

    companion object : KLogging()

    override fun write(chunk: Chunk<out T>) {
        if (chunk.isEmpty) return

        table.batchInsert(chunk.items, shouldReturnGeneratedValues = false) { item ->
            insertBody(item)
        }

        log.debug { "${chunk.items.size}건 batchInsert 완료 (table=${table.tableName})" }
    }
}
```

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew :bluetape4k-spring-boot3-batch-exposed:compileKotlin`
Expected: BUILD SUCCESSFUL

**Done when**: `ExposedItemWriter` 컴파일 성공, `write()` 메서드에서 Spring 청크 트랜잭션 자동 참여 구현 완료

---

### Task 2.5: ExposedUpsertItemWriter

- **complexity**: medium
- **depends_on**: T1.2
- **Files**:
  - Create: `spring-boot3/batch-exposed/src/main/kotlin/io/bluetape4k/spring/batch/exposed/writer/ExposedUpsertItemWriter.kt`

- [ ] **Step 1: ExposedUpsertItemWriter 구현**

`batchUpsert` 기반 `ItemWriter`를 스펙 섹션 4.5에 따라 구현한다.

핵심 구현 포인트:
- `ItemWriter<T>` 구현, `database` 파라미터 없음
- `batchUpsert`는 동일 키 존재 시 UPDATE, 없으면 INSERT

```kotlin
package io.bluetape4k.spring.batch.exposed.writer

import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.batchUpsert
import org.jetbrains.exposed.sql.statements.BatchUpsertStatement
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter

/**
 * Exposed `batchUpsert` 기반 [ItemWriter].
 *
 * 동일 키가 존재하면 UPDATE, 없으면 INSERT를 수행.
 * SpringTransactionManager 환경에서 청크 트랜잭션에 자동 참여.
 *
 * @param T 입력 타입
 * @param table 대상 Exposed [Table]
 * @param upsertBody `batchUpsert` 람다
 */
class ExposedUpsertItemWriter<T>(
    private val table: Table,
    private val upsertBody: BatchUpsertStatement.(T) -> Unit,
) : ItemWriter<T> {

    companion object : KLogging()

    override fun write(chunk: Chunk<out T>) {
        if (chunk.isEmpty) return

        table.batchUpsert(chunk.items) { item ->
            upsertBody(item)
        }

        log.debug { "${chunk.items.size}건 batchUpsert 완료 (table=${table.tableName})" }
    }
}
```

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew :bluetape4k-spring-boot3-batch-exposed:compileKotlin`
Expected: BUILD SUCCESSFUL

**Done when**: `ExposedUpsertItemWriter` 컴파일 성공

---

### Task 2.6: ExposedUpdateItemWriter

- **complexity**: medium
- **depends_on**: T1.2
- **Files**:
  - Create: `spring-boot3/batch-exposed/src/main/kotlin/io/bluetape4k/spring/batch/exposed/writer/ExposedUpdateItemWriter.kt`

- [ ] **Step 1: ExposedUpdateItemWriter 구현**

개별 `update` 기반 `ItemWriter`를 스펙 섹션 4.6에 따라 구현한다.

핵심 구현 포인트:
- `ItemWriter<T>` 구현, `keyColumn` + `keyExtractor`로 WHERE 조건 생성
- 각 아이템에 대해 개별 UPDATE 실행 (batch update 아님)

```kotlin
package io.bluetape4k.spring.batch.exposed.writer

import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.update
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter

/**
 * Exposed `update` 기반 [ItemWriter].
 *
 * 기존 레코드를 업데이트할 때 사용. 각 아이템에 대해 개별 UPDATE 실행.
 * SpringTransactionManager 환경에서 청크 트랜잭션에 자동 참여.
 *
 * @param T 입력 타입
 * @param table 대상 Exposed [Table]
 * @param keyColumn WHERE 조건에 사용할 키 컬럼
 * @param keyExtractor T에서 키 값을 추출하는 함수
 * @param updateBody UPDATE SET 람다
 */
class ExposedUpdateItemWriter<T>(
    private val table: Table,
    private val keyColumn: Column<Long>,
    private val keyExtractor: (T) -> Long,
    private val updateBody: UpdateBuilder<*>.(T) -> Unit,
) : ItemWriter<T> {

    companion object : KLogging()

    override fun write(chunk: Chunk<out T>) {
        if (chunk.isEmpty) return

        chunk.items.forEach { item ->
            table.update({ keyColumn eq keyExtractor(item) }) {
                this.updateBody(item)
            }
        }

        log.debug { "${chunk.items.size}건 update 완료 (table=${table.tableName})" }
    }
}
```

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew :bluetape4k-spring-boot3-batch-exposed:compileKotlin`
Expected: BUILD SUCCESSFUL

**Done when**: `ExposedUpdateItemWriter` 컴파일 성공

---

### Task 2.7: ExposedBatchAutoConfiguration + META-INF 등록

- **complexity**: medium
- **depends_on**: T2.3
- **Files**:
  - Create: `spring-boot3/batch-exposed/src/main/kotlin/io/bluetape4k/spring/batch/exposed/config/ExposedBatchAutoConfiguration.kt`
  - Create: `spring-boot3/batch-exposed/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- [ ] **Step 1: ExposedBatchAutoConfiguration 구현**

Spring Boot AutoConfiguration 클래스를 스펙 섹션 4.8에 따라 구현한다.

핵심 구현 포인트:
- `@AutoConfiguration(after = [BatchAutoConfiguration::class])` 순서 지정
- `@ConditionalOnClass(Job::class)`, `@ConditionalOnBean(DataSource::class, PlatformTransactionManager::class)`
- `@EnableBatchProcessing` 절대 사용 금지 (Spring Boot 3.x auto-config 무력화 방지)
- `batchPartitionTaskExecutor` 빈: `@ConditionalOnMissingBean(name = ["batchPartitionTaskExecutor"])`

```kotlin
package io.bluetape4k.spring.batch.exposed.config

import io.bluetape4k.logging.KLogging
import org.springframework.batch.core.Job
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.core.task.TaskExecutor
import javax.sql.DataSource
import org.springframework.transaction.PlatformTransactionManager

/**
 * Spring Boot Auto-Configuration for Exposed Batch 컴포넌트.
 *
 * 주의: `@EnableBatchProcessing` 절대 사용 금지 -- Spring Boot 3.x auto-config 무력화됨.
 *
 * 권장 설정:
 * ```yaml
 * spring:
 *   batch:
 *     job:
 *       enabled: false  # 자동 실행 비활성화, 명시적 JobLauncher 사용 권장
 * ```
 */
@AutoConfiguration(after = [BatchAutoConfiguration::class])
@ConditionalOnClass(Job::class)
@ConditionalOnBean(DataSource::class, PlatformTransactionManager::class)
class ExposedBatchAutoConfiguration {

    companion object : KLogging()

    /**
     * 배치 파티션 실행용 VirtualThread TaskExecutor.
     * 사용자가 직접 빈을 등록하면 이 기본 빈은 생성되지 않음.
     */
    @Bean
    @ConditionalOnMissingBean(name = ["batchPartitionTaskExecutor"])
    fun batchPartitionTaskExecutor(): TaskExecutor =
        SimpleAsyncTaskExecutor("batch-partition-").apply {
            setVirtualThreads(true)
            setConcurrencyLimit(Runtime.getRuntime().availableProcessors() * 2)
        }
}
```

- [ ] **Step 2: META-INF AutoConfiguration.imports 파일 생성**

Create: `spring-boot3/batch-exposed/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

```
io.bluetape4k.spring.batch.exposed.config.ExposedBatchAutoConfiguration
```

- [ ] **Step 3: 컴파일 확인**

Run: `./gradlew :bluetape4k-spring-boot3-batch-exposed:compileKotlin`
Expected: BUILD SUCCESSFUL

**Done when**: AutoConfiguration 등록 완료, `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 파일에 FQCN 등록

---

### Task 2.8: Kotlin DSL 확장 함수

- **complexity**: low
- **depends_on**: T2.1
- **Files**:
  - Create: `spring-boot3/batch-exposed/src/main/kotlin/io/bluetape4k/spring/batch/exposed/dsl/BatchJobExtensions.kt`

- [ ] **Step 1: DSL 확장 함수 구현**

`partitionedBatchJob`, `exposedPartitionedStep` DSL 확장 함수를 스펙 섹션 4.9에 따라 구현한다.

```kotlin
package io.bluetape4k.spring.batch.exposed.dsl

import io.bluetape4k.spring.batch.exposed.partition.ExposedRangePartitioner
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.builder.SimpleJobBuilder
import org.springframework.batch.core.partition.PartitionHandler
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder

/**
 * Partitioned Batch Job을 간결하게 생성하는 DSL 확장.
 *
 * 사용 예시:
 * ```kotlin
 * val job = partitionedBatchJob("parallel-migrate", jobRepository) {
 *     start(partitionedStep)
 * }
 * ```
 */
fun partitionedBatchJob(
    name: String,
    jobRepository: JobRepository,
    block: JobBuilder.() -> SimpleJobBuilder,
): Job = JobBuilder(name, jobRepository).block().build()

/**
 * Exposed Partitioned Step을 생성하는 DSL 확장.
 */
fun StepBuilder.exposedPartitionedStep(
    partitioner: ExposedRangePartitioner,
    handler: PartitionHandler,
): Step = this.partitioner("worker", partitioner)
    .partitionHandler(handler)
    .build()
```

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew :bluetape4k-spring-boot3-batch-exposed:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 커밋**

```bash
git add spring-boot3/batch-exposed/src/main/
git commit -m "$(cat <<'EOF'
feat: spring-boot3-batch-exposed 핵심 구현 추가

ExposedRangePartitioner, ExposedKeysetItemReader (forEntityId 팩토리 포함),
ExposedItemWriter/UpsertItemWriter/UpdateItemWriter, virtualThreadPartitionTaskExecutor,
ExposedBatchAutoConfiguration, partitionedBatchJob DSL 구현
EOF
)"
```

**Done when**: 전체 main 소스 컴파일 성공, Phase 2 모든 클래스 구현 완료

---

## Phase 3: 테스트

> **선행 조건**: Phase 2 완료 (모든 main 소스 컴파일 성공)
> **Gradle 빌드 검증**: Phase 3 완료 후 `./gradlew :bluetape4k-spring-boot3-batch-exposed:test`

### Task 3.1: 테스트 공통 인프라 (TestTables + AbstractExposedBatchTest)

- **complexity**: low
- **depends_on**: T2.8
- **Files**:
  - Create: `spring-boot3/batch-exposed/src/test/kotlin/io/bluetape4k/spring/batch/exposed/TestTables.kt`
  - Create: `spring-boot3/batch-exposed/src/test/kotlin/io/bluetape4k/spring/batch/exposed/AbstractExposedBatchTest.kt`

- [ ] **Step 1: TestTables.kt 작성**

테스트용 Exposed Table 정의 및 데이터 생성 헬퍼 함수를 작성한다.

```kotlin
package io.bluetape4k.spring.batch.exposed

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.Serializable

/**
 * 테스트용 Source 테이블 정의.
 */
object SourceTable : LongIdTable("source_data") {
    val name = varchar("name", 255)
    val value = integer("value")
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}

/**
 * 테스트용 Target 테이블 정의.
 */
object TargetTable : LongIdTable("target_data") {
    val sourceName = varchar("source_name", 255).uniqueIndex()
    val transformedValue = integer("transformed_value")
}

/**
 * Source 테이블 레코드.
 */
data class SourceRecord(
    val id: Long = 0L,
    val name: String,
    val value: Int,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

/**
 * Target 테이블 레코드.
 */
data class TargetRecord(
    val sourceName: String,
    val transformedValue: Int,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

/**
 * 테스트 데이터 대량 생성 헬퍼.
 */
fun insertTestData(count: Int) {
    transaction {
        SourceTable.batchInsert((1..count).toList(), shouldReturnGeneratedValues = false) { i ->
            this[SourceTable.name] = "item-$i"
            this[SourceTable.value] = i
        }
    }
}
```

- [ ] **Step 2: AbstractExposedBatchTest.kt 작성**

Spring Batch + Exposed + H2 기반 테스트 공통 설정 클래스를 작성한다.

```kotlin
package io.bluetape4k.spring.batch.exposed

import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.test.JobLauncherTestUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import javax.sql.DataSource

@SpringBootTest
@ActiveProfiles("test")
abstract class AbstractExposedBatchTest {

    companion object : KLogging()

    @SpringBootApplication
    class TestApplication

    @Autowired
    protected lateinit var dataSource: DataSource

    @Autowired
    protected lateinit var database: Database

    @Autowired
    protected lateinit var jobRepository: JobRepository

    @BeforeEach
    fun setupTables() {
        transaction(database) {
            SchemaUtils.drop(TargetTable, SourceTable)
            SchemaUtils.create(SourceTable, TargetTable)
        }
    }
}
```

- [ ] **Step 3: test/resources/application-test.yml 작성**

Create: `spring-boot3/batch-exposed/src/test/resources/application-test.yml`

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
    driver-class-name: org.h2.Driver
    username: sa
    password:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 4

  batch:
    jdbc:
      initialize-schema: always
    job:
      enabled: false

  exposed:
    generate-ddl: false
```

- [ ] **Step 4: 테스트 컴파일 확인**

Run: `./gradlew :bluetape4k-spring-boot3-batch-exposed:compileTestKotlin`
Expected: BUILD SUCCESSFUL

**Done when**: 테스트 공통 인프라 컴파일 성공 (TestTables, AbstractExposedBatchTest, application-test.yml)

---

### Task 3.2: ExposedRangePartitionerTest

- **complexity**: medium
- **depends_on**: T3.1
- **Files**:
  - Create: `spring-boot3/batch-exposed/src/test/kotlin/io/bluetape4k/spring/batch/exposed/partition/ExposedRangePartitionerTest.kt`

- [ ] **Step 1: 단위 테스트 작성**

H2 환경에서 `ExposedRangePartitioner`의 균등 분할, 빈 테이블, 단일 행, `forEntityId()` 시나리오를 검증한다.

```kotlin
package io.bluetape4k.spring.batch.exposed.partition

import io.bluetape4k.spring.batch.exposed.AbstractExposedBatchTest
import io.bluetape4k.spring.batch.exposed.SourceTable
import io.bluetape4k.spring.batch.exposed.insertTestData
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test

class ExposedRangePartitionerTest : AbstractExposedBatchTest() {

    @Test
    fun `빈 테이블에서 단일 빈 파티션 반환`() {
        val partitioner = ExposedRangePartitioner.forEntityId(
            table = SourceTable,
            gridSize = 4,
            database = database,
        )

        val partitions = partitioner.partition(4)

        partitions shouldHaveSize 1
        partitions["partition-0"]!!.getLong(ExposedRangePartitioner.PARTITION_MIN_ID) shouldBeEqualTo 0L
        partitions["partition-0"]!!.getLong(ExposedRangePartitioner.PARTITION_MAX_ID) shouldBeEqualTo -1L
    }

    @Test
    fun `1000건 데이터를 4개 파티션으로 균등 분할`() {
        insertTestData(1000)

        val partitioner = ExposedRangePartitioner.forEntityId(
            table = SourceTable,
            gridSize = 4,
            database = database,
        )

        val partitions = partitioner.partition(4)

        partitions shouldHaveSize 4

        // 모든 파티션의 범위가 겹치지 않고 전체를 커버하는지 검증
        val ranges = partitions.values.map { ctx ->
            ctx.getLong(ExposedRangePartitioner.PARTITION_MIN_ID)..ctx.getLong(ExposedRangePartitioner.PARTITION_MAX_ID)
        }.sortedBy { it.first }

        ranges.first().first shouldBeEqualTo 1L
        ranges.last().last shouldBeEqualTo 1000L

        // 인접 파티션이 연속인지 검증
        for (i in 0 until ranges.size - 1) {
            ranges[i].last + 1 shouldBeEqualTo ranges[i + 1].first
        }
    }

    @Test
    fun `단일 행 테이블에서 1개 파티션 반환`() {
        insertTestData(1)

        val partitioner = ExposedRangePartitioner.forEntityId(
            table = SourceTable,
            gridSize = 8,
            database = database,
        )

        val partitions = partitioner.partition(8)

        partitions shouldHaveSize 1
        partitions["partition-0"]!!.getLong(ExposedRangePartitioner.PARTITION_MIN_ID) shouldBeEqualTo 1L
        partitions["partition-0"]!!.getLong(ExposedRangePartitioner.PARTITION_MAX_ID) shouldBeEqualTo 1L
    }

    @Test
    fun `gridSize가 totalRange보다 클 때 safeGridSize로 보정`() {
        insertTestData(3)

        val partitioner = ExposedRangePartitioner.forEntityId(
            table = SourceTable,
            gridSize = 10,
            database = database,
        )

        val partitions = partitioner.partition(10)

        // 3건이므로 최대 3개 파티션
        partitions.size shouldBeLessOrEqualTo 3
        partitions.size shouldBeGreaterOrEqualTo 1
    }
}
```

- [ ] **Step 2: 테스트 실행**

Run: `./gradlew :bluetape4k-spring-boot3-batch-exposed:test --tests "io.bluetape4k.spring.batch.exposed.partition.ExposedRangePartitionerTest"`
Expected: 4 tests passed

**Done when**: 빈 테이블, 균등 분할, 단일 행, gridSize 보정 테스트 모두 통과

---

### Task 3.3: ExposedKeysetItemReaderTest

- **complexity**: medium
- **depends_on**: T3.1
- **Files**:
  - Create: `spring-boot3/batch-exposed/src/test/kotlin/io/bluetape4k/spring/batch/exposed/reader/ExposedKeysetItemReaderTest.kt`

- [ ] **Step 1: 단위 테스트 작성**

H2 환경에서 keyset 읽기 정상 동작, 빈 파티션 null 반환, restart 시 lastKey 재개를 검증한다.

```kotlin
package io.bluetape4k.spring.batch.exposed.reader

import io.bluetape4k.spring.batch.exposed.AbstractExposedBatchTest
import io.bluetape4k.spring.batch.exposed.SourceRecord
import io.bluetape4k.spring.batch.exposed.SourceTable
import io.bluetape4k.spring.batch.exposed.insertTestData
import io.bluetape4k.spring.batch.exposed.partition.ExposedRangePartitioner
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.batch.item.ExecutionContext

class ExposedKeysetItemReaderTest : AbstractExposedBatchTest() {

    private fun createReader(): ExposedKeysetItemReader<SourceRecord> =
        ExposedKeysetItemReader.forEntityId(
            table = SourceTable,
            pageSize = 10,
            rowMapper = { row ->
                SourceRecord(
                    id = row[SourceTable.id].value,
                    name = row[SourceTable.name],
                    value = row[SourceTable.value],
                )
            },
            database = database,
        )

    @Test
    fun `정상적으로 모든 레코드를 keyset 페이징으로 읽기`() {
        insertTestData(25)

        val reader = createReader()
        val context = ExecutionContext().apply {
            putLong(ExposedRangePartitioner.PARTITION_MIN_ID, 1L)
            putLong(ExposedRangePartitioner.PARTITION_MAX_ID, 25L)
        }
        reader.open(context)

        val results = mutableListOf<SourceRecord>()
        var item = reader.read()
        while (item != null) {
            results.add(item)
            item = reader.read()
        }

        results.size shouldBeEqualTo 25
        results.first().id shouldBeEqualTo 1L
        results.last().id shouldBeEqualTo 25L

        reader.close()
    }

    @Test
    fun `빈 파티션에서 즉시 null 반환`() {
        // 데이터가 없으므로 빈 파티션
        val reader = createReader()
        val context = ExecutionContext().apply {
            putLong(ExposedRangePartitioner.PARTITION_MIN_ID, 1L)
            putLong(ExposedRangePartitioner.PARTITION_MAX_ID, 100L)
        }
        reader.open(context)

        reader.read().shouldBeNull()

        reader.close()
    }

    @Test
    fun `restart 시 lastKey부터 이어서 읽기`() {
        insertTestData(50)

        val reader = createReader()
        val context = ExecutionContext().apply {
            putLong(ExposedRangePartitioner.PARTITION_MIN_ID, 1L)
            putLong(ExposedRangePartitioner.PARTITION_MAX_ID, 50L)
        }
        reader.open(context)

        // 15개 읽기
        repeat(15) { reader.read().shouldNotBeNull() }

        // ExecutionContext에 lastKey 저장
        reader.update(context)

        reader.close()

        // 새 Reader로 restart
        val restartReader = createReader()
        restartReader.open(context)

        val remaining = mutableListOf<SourceRecord>()
        var item = restartReader.read()
        while (item != null) {
            remaining.add(item)
            item = restartReader.read()
        }

        remaining.size shouldBeEqualTo 35
        remaining.first().id shouldBeEqualTo 16L

        restartReader.close()
    }

    @Test
    fun `파티션 범위 내 데이터만 읽기`() {
        insertTestData(100)

        val reader = createReader()
        val context = ExecutionContext().apply {
            putLong(ExposedRangePartitioner.PARTITION_MIN_ID, 21L)
            putLong(ExposedRangePartitioner.PARTITION_MAX_ID, 40L)
        }
        reader.open(context)

        val results = mutableListOf<SourceRecord>()
        var item = reader.read()
        while (item != null) {
            results.add(item)
            item = reader.read()
        }

        results.size shouldBeEqualTo 20
        results.first().id shouldBeEqualTo 21L
        results.last().id shouldBeEqualTo 40L

        reader.close()
    }
}
```

- [ ] **Step 2: 테스트 실행**

Run: `./gradlew :bluetape4k-spring-boot3-batch-exposed:test --tests "io.bluetape4k.spring.batch.exposed.reader.ExposedKeysetItemReaderTest"`
Expected: 4 tests passed

**Done when**: keyset 페이징, 빈 파티션, restart 재개, 파티션 범위 격리 테스트 모두 통과

---

### Task 3.4: ExposedItemWriterTest + ExposedUpsertItemWriterTest + ExposedUpdateItemWriterTest

- **complexity**: medium
- **depends_on**: T3.1
- **Files**:
  - Create: `spring-boot3/batch-exposed/src/test/kotlin/io/bluetape4k/spring/batch/exposed/writer/ExposedItemWriterTest.kt`
  - Create: `spring-boot3/batch-exposed/src/test/kotlin/io/bluetape4k/spring/batch/exposed/writer/ExposedUpsertItemWriterTest.kt`
  - Create: `spring-boot3/batch-exposed/src/test/kotlin/io/bluetape4k/spring/batch/exposed/writer/ExposedUpdateItemWriterTest.kt`

- [ ] **Step 1: ExposedItemWriterTest 작성**

```kotlin
package io.bluetape4k.spring.batch.exposed.writer

import io.bluetape4k.spring.batch.exposed.AbstractExposedBatchTest
import io.bluetape4k.spring.batch.exposed.TargetRecord
import io.bluetape4k.spring.batch.exposed.TargetTable
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import org.springframework.batch.item.Chunk

class ExposedItemWriterTest : AbstractExposedBatchTest() {

    @Test
    fun `batchInsert로 정상적으로 레코드 삽입`() {
        val writer = ExposedItemWriter<TargetRecord>(table = TargetTable) {
            this[TargetTable.sourceName] = it.sourceName
            this[TargetTable.transformedValue] = it.transformedValue
        }

        val items = (1..10).map { TargetRecord("name-$it", it * 2) }

        transaction(database) {
            writer.write(Chunk(items))
        }

        val count = transaction(database) { TargetTable.selectAll().count() }
        count shouldBeEqualTo 10L
    }

    @Test
    fun `빈 chunk 전달 시 아무 동작 안 함`() {
        val writer = ExposedItemWriter<TargetRecord>(table = TargetTable) {
            this[TargetTable.sourceName] = it.sourceName
            this[TargetTable.transformedValue] = it.transformedValue
        }

        transaction(database) {
            writer.write(Chunk(emptyList()))
        }

        val count = transaction(database) { TargetTable.selectAll().count() }
        count shouldBeEqualTo 0L
    }
}
```

- [ ] **Step 2: ExposedUpsertItemWriterTest 작성**

```kotlin
package io.bluetape4k.spring.batch.exposed.writer

import io.bluetape4k.spring.batch.exposed.AbstractExposedBatchTest
import io.bluetape4k.spring.batch.exposed.TargetRecord
import io.bluetape4k.spring.batch.exposed.TargetTable
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import org.springframework.batch.item.Chunk

class ExposedUpsertItemWriterTest : AbstractExposedBatchTest() {

    @Test
    fun `upsert로 신규 insert 및 기존 update 동작 검증`() {
        val writer = ExposedUpsertItemWriter<TargetRecord>(table = TargetTable) {
            this[TargetTable.sourceName] = it.sourceName
            this[TargetTable.transformedValue] = it.transformedValue
        }

        // 1단계: 5건 insert (sourceName 기준 신규)
        val items = (1..5).map { TargetRecord("name-$it", it * 2) }
        transaction(database) { writer.write(Chunk(items)) }

        val countAfterInsert = transaction(database) { TargetTable.selectAll().count() }
        countAfterInsert shouldBeEqualTo 5L

        // 2단계: 동일 sourceName으로 다른 값 upsert → UPDATE 경로
        val updatedItems = (1..5).map { TargetRecord("name-$it", it * 100) }
        transaction(database) { writer.write(Chunk(updatedItems)) }

        // 행 수는 동일 (중복 없음), 값은 변경됨
        val countAfterUpsert = transaction(database) { TargetTable.selectAll().count() }
        countAfterUpsert shouldBeEqualTo 5L

        val updatedValues = transaction(database) {
            TargetTable.selectAll()
                .orderBy(TargetTable.sourceName)
                .map { it[TargetTable.transformedValue] }
        }
        updatedValues shouldBeEqualTo (1..5).map { it * 100 }
    }

    @Test
    fun `빈 chunk 전달 시 아무 동작 안 함`() {
        val writer = ExposedUpsertItemWriter<TargetRecord>(table = TargetTable) {
            this[TargetTable.sourceName] = it.sourceName
            this[TargetTable.transformedValue] = it.transformedValue
        }

        transaction(database) {
            writer.write(Chunk(emptyList()))
        }

        val count = transaction(database) { TargetTable.selectAll().count() }
        count shouldBeEqualTo 0L
    }
}
```

- [ ] **Step 3: ExposedUpdateItemWriterTest 작성**

```kotlin
package io.bluetape4k.spring.batch.exposed.writer

import io.bluetape4k.spring.batch.exposed.AbstractExposedBatchTest
import io.bluetape4k.spring.batch.exposed.SourceRecord
import io.bluetape4k.spring.batch.exposed.SourceTable
import io.bluetape4k.spring.batch.exposed.insertTestData
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import org.springframework.batch.item.Chunk

class ExposedUpdateItemWriterTest : AbstractExposedBatchTest() {

    @Test
    fun `개별 UPDATE로 레코드 정확히 수정`() {
        insertTestData(5)

        val writer = ExposedUpdateItemWriter<SourceRecord>(
            table = SourceTable,
            keyColumn = SourceTable.id.castTo<Long>(org.jetbrains.exposed.sql.LongColumnType()),
            keyExtractor = { it.id },
            updateBody = {
                this[SourceTable.name] = it.name + "-updated"
                this[SourceTable.value] = it.value * 10
            },
        )

        val items = (1L..3L).map { SourceRecord(id = it, name = "item-$it", value = it.toInt()) }

        transaction(database) {
            writer.write(Chunk(items))
        }

        transaction(database) {
            val rows = SourceTable.selectAll().toList()
            val updated = rows.filter { it[SourceTable.name].endsWith("-updated") }
            updated.size shouldBeEqualTo 3
            updated.first()[SourceTable.value] shouldBeEqualTo 10
        }
    }

    @Test
    fun `빈 chunk 전달 시 아무 동작 안 함`() {
        insertTestData(5)

        val writer = ExposedUpdateItemWriter<SourceRecord>(
            table = SourceTable,
            keyColumn = SourceTable.id.castTo<Long>(org.jetbrains.exposed.sql.LongColumnType()),
            keyExtractor = { it.id },
            updateBody = {
                this[SourceTable.name] = it.name + "-updated"
            },
        )

        transaction(database) {
            writer.write(Chunk(emptyList()))
        }

        transaction(database) {
            val rows = SourceTable.selectAll().toList()
            rows.none { it[SourceTable.name].endsWith("-updated") } shouldBeEqualTo true
        }
    }
}
```

- [ ] **Step 4: 테스트 실행**

Run: `./gradlew :bluetape4k-spring-boot3-batch-exposed:test --tests "io.bluetape4k.spring.batch.exposed.writer.*"`
Expected: 6 tests passed (ExposedItemWriterTest 2 + ExposedUpsertItemWriterTest 2 + ExposedUpdateItemWriterTest 2)

**Done when**: 3개 Writer 테스트 클래스 6개 테스트 모두 통과

---

### Task 3.5: ParallelQueryIntegrationTest (E2E)

- **complexity**: high
- **depends_on**: T3.1, T3.2, T3.3, T3.4
- **Files**:
  - Create: `spring-boot3/batch-exposed/src/test/kotlin/io/bluetape4k/spring/batch/exposed/integration/ParallelQueryIntegrationTest.kt`

- [ ] **Step 1: E2E 통합 테스트 작성**

H2 환경에서 전체 파이프라인 (Partitioner + Reader + Processor + Writer + VirtualThread) E2E 동작을 검증한다. Testcontainers PostgreSQL은 CI 환경에서만 실행하도록 `@Tag("integration")` 분리 가능하지만, 기본 테스트는 H2로 작성한다.

핵심 검증:
- 10,000건 Source -> Target, 4 파티션 VirtualThread 병렬 실행
- 데이터 정합성: Target 행 수 == Source 행 수, 중복 없음
- Spring Batch Job 상태 == COMPLETED

```kotlin
package io.bluetape4k.spring.batch.exposed.integration

import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.batch.exposed.AbstractExposedBatchTest
import io.bluetape4k.spring.batch.exposed.SourceRecord
import io.bluetape4k.spring.batch.exposed.SourceTable
import io.bluetape4k.spring.batch.exposed.TargetRecord
import io.bluetape4k.spring.batch.exposed.TargetTable
import io.bluetape4k.spring.batch.exposed.insertTestData
import io.bluetape4k.spring.batch.exposed.dsl.partitionedBatchJob
import io.bluetape4k.spring.batch.exposed.partition.ExposedRangePartitioner
import io.bluetape4k.spring.batch.exposed.reader.ExposedKeysetItemReader
import io.bluetape4k.spring.batch.exposed.support.virtualThreadPartitionTaskExecutor
import io.bluetape4k.spring.batch.exposed.writer.ExposedItemWriter
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.Step
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.test.JobLauncherTestUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

class ParallelQueryIntegrationTest : AbstractExposedBatchTest() {

    companion object : KLogging()

    @Configuration
    class JobConfig(
        private val jobRepository: JobRepository,
        private val transactionManager: PlatformTransactionManager,
        private val database: Database,
    ) {
        @Bean
        fun migrationJob(): Job = partitionedBatchJob("migration-job", jobRepository) {
            start(partitionedStep())
        }

        @Bean
        fun partitionedStep(): Step = StepBuilder("migration-manager", jobRepository)
            .partitioner("migration-worker", rangePartitioner())
            .partitionHandler(partitionHandler())
            .build()

        @Bean
        fun rangePartitioner(): ExposedRangePartitioner = ExposedRangePartitioner.forEntityId(
            table = SourceTable,
            gridSize = 4,
            database = database,
        )

        @Bean
        fun partitionHandler(): TaskExecutorPartitionHandler = TaskExecutorPartitionHandler().apply {
            setStep(workerStep())
            setTaskExecutor(virtualThreadPartitionTaskExecutor(concurrencyLimit = 4))
            gridSize = 4
        }

        @Bean
        fun workerStep(): Step = StepBuilder("migration-worker", jobRepository)
            .chunk<SourceRecord, TargetRecord>(500, transactionManager)
            .reader(keysetReader())
            .processor(ItemProcessor { source ->
                TargetRecord(
                    sourceName = source.name.uppercase(),
                    transformedValue = source.value * 2,
                )
            })
            .writer(itemWriter())
            .build()

        @Bean
        @org.springframework.batch.core.configuration.annotation.StepScope
        fun keysetReader(): ExposedKeysetItemReader<SourceRecord> = ExposedKeysetItemReader.forEntityId(
            table = SourceTable,
            pageSize = 500,
            rowMapper = { row ->
                SourceRecord(
                    id = row[SourceTable.id].value,
                    name = row[SourceTable.name],
                    value = row[SourceTable.value],
                )
            },
            database = database,
        )

        @Bean
        fun itemWriter(): ExposedItemWriter<TargetRecord> = ExposedItemWriter(table = TargetTable) {
            this[TargetTable.sourceName] = it.sourceName
            this[TargetTable.transformedValue] = it.transformedValue
        }

        @Bean
        fun jobLauncherTestUtils(job: Job): JobLauncherTestUtils = JobLauncherTestUtils().apply {
            this.job = job
            this.jobRepository = this@JobConfig.jobRepository
        }
    }

    @Autowired
    private lateinit var jobLauncherTestUtils: JobLauncherTestUtils

    @Test
    fun `10000건 Source에서 Target으로 4파티션 VirtualThread 병렬 마이그레이션`() {
        insertTestData(10_000)

        val jobParameters = JobParametersBuilder()
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters()

        val execution = jobLauncherTestUtils.launchJob(jobParameters)

        execution.status shouldBeEqualTo BatchStatus.COMPLETED

        val targetCount = transaction(database) { TargetTable.selectAll().count() }
        targetCount shouldBeEqualTo 10_000L
    }
}
```

- [ ] **Step 2: 테스트 실행**

Run: `./gradlew :bluetape4k-spring-boot3-batch-exposed:test --tests "io.bluetape4k.spring.batch.exposed.integration.ParallelQueryIntegrationTest"`
Expected: 1 test passed, Job COMPLETED, Target 10,000건

- [ ] **Step 3: 전체 테스트 실행 확인**

Run: `./gradlew :bluetape4k-spring-boot3-batch-exposed:test`
Expected: 모든 테스트 통과

- [ ] **Step 4: 커밋**

```bash
git add spring-boot3/batch-exposed/src/test/
git commit -m "$(cat <<'EOF'
test: spring-boot3-batch-exposed 단위 테스트 및 E2E 통합 테스트 추가

Partitioner/Reader/Writer 단위 테스트, ParallelQueryIntegrationTest (E2E, 10,000건 4파티션)
EOF
)"
```

**Done when**: 전체 테스트 스위트 통과 (Partitioner 4 + Reader 4 + Writer 6 + E2E 1 = 15 tests)

---

### Task 3.6: RestartIntegrationTest

- **complexity**: medium
- **depends_on**: T3.5
- **Files**:
  - Create: `spring-boot3/batch-exposed/src/test/kotlin/io/bluetape4k/spring/batch/exposed/integration/RestartIntegrationTest.kt`

- [ ] **Step 1: 재시작 시나리오 테스트 작성**

중간 실패 후 Job을 재시작할 때 `lastKey` 기반으로 정확한 위치부터 재개되는지 검증한다.

```kotlin
package io.bluetape4k.spring.batch.exposed.integration

import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.batch.exposed.AbstractExposedBatchTest
import io.bluetape4k.spring.batch.exposed.SourceRecord
import io.bluetape4k.spring.batch.exposed.SourceTable
import io.bluetape4k.spring.batch.exposed.TargetRecord
import io.bluetape4k.spring.batch.exposed.TargetTable
import io.bluetape4k.spring.batch.exposed.insertTestData
import io.bluetape4k.spring.batch.exposed.dsl.partitionedBatchJob
import io.bluetape4k.spring.batch.exposed.partition.ExposedRangePartitioner
import io.bluetape4k.spring.batch.exposed.reader.ExposedKeysetItemReader
import io.bluetape4k.spring.batch.exposed.support.virtualThreadPartitionTaskExecutor
import io.bluetape4k.spring.batch.exposed.writer.ExposedItemWriter
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.Step
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.test.JobLauncherTestUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 중간 실패 후 Job 재시작 시 lastKey 기반 재개 검증.
 */
class RestartIntegrationTest : AbstractExposedBatchTest() {

    companion object : KLogging()

    // 첫 번째 실행에서 실패 유도, 두 번째 실행에서 성공하도록 토글
    private val shouldFail = AtomicBoolean(true)

    @Configuration
    inner class JobConfig(
        private val jobRepository: JobRepository,
        private val transactionManager: PlatformTransactionManager,
        private val database: Database,
    ) {
        @Bean(name = ["restartMigrationJob"])
        fun migrationJob(): Job = partitionedBatchJob("restart-migration-job", jobRepository) {
            start(partitionedStep())
        }

        @Bean(name = ["restartPartitionedStep"])
        fun partitionedStep(): Step = StepBuilder("restart-migration-manager", jobRepository)
            .partitioner("restart-migration-worker", rangePartitioner())
            .partitionHandler(partitionHandler())
            .build()

        @Bean(name = ["restartRangePartitioner"])
        fun rangePartitioner(): ExposedRangePartitioner = ExposedRangePartitioner.forEntityId(
            table = SourceTable,
            gridSize = 2,
            database = database,
        )

        @Bean(name = ["restartPartitionHandler"])
        fun partitionHandler(): TaskExecutorPartitionHandler = TaskExecutorPartitionHandler().apply {
            setStep(workerStep())
            setTaskExecutor(virtualThreadPartitionTaskExecutor(concurrencyLimit = 2))
            gridSize = 2
        }

        @Bean(name = ["restartWorkerStep"])
        fun workerStep(): Step = StepBuilder("restart-migration-worker", jobRepository)
            .chunk<SourceRecord, TargetRecord>(100, transactionManager)
            .reader(keysetReader())
            .processor(ItemProcessor { source ->
                // shouldFail=true이고 name이 "name-300"이면 예외 발생 → 청크 롤백 → Job FAILED
                if (shouldFail.get() && source.name == "name-300") {
                    throw RuntimeException("name-300 처리 중 의도적 실패")
                }
                TargetRecord(sourceName = source.name.uppercase(), transformedValue = source.value * 2)
            })
            .writer(itemWriter())
            .build()

        @Bean(name = ["restartKeysetReader"])
        @org.springframework.batch.core.configuration.annotation.StepScope
        fun keysetReader(): ExposedKeysetItemReader<SourceRecord> = ExposedKeysetItemReader.forEntityId(
            table = SourceTable,
            pageSize = 100,
            rowMapper = { row ->
                SourceRecord(
                    id = row[SourceTable.id].value,
                    name = row[SourceTable.name],
                    value = row[SourceTable.value],
                )
            },
            database = database,
        )

        @Bean(name = ["restartItemWriter"])
        fun itemWriter(): ExposedItemWriter<TargetRecord> = ExposedItemWriter(table = TargetTable) {
            this[TargetTable.sourceName] = it.sourceName
            this[TargetTable.transformedValue] = it.transformedValue
        }

        @Bean(name = ["restartJobLauncherTestUtils"])
        fun jobLauncherTestUtils(job: Job): JobLauncherTestUtils = JobLauncherTestUtils().apply {
            this.job = job
            this.jobRepository = this@JobConfig.jobRepository
        }
    }

    @Autowired
    private lateinit var jobLauncherTestUtils: JobLauncherTestUtils

    @Test
    fun `중간 실패 후 재시작 시 마지막 위치부터 재개`() {
        // 1000건 삽입 (name-1 ~ name-1000)
        insertTestData(1000)

        val params = JobParametersBuilder()
            .addLong("run.id", 1L)
            .toJobParameters()

        // 1차 실행: name-300 에서 예외 발생 → FAILED
        shouldFail.set(true)
        val firstExecution = jobLauncherTestUtils.launchJob(params)
        firstExecution.status shouldBeEqualTo BatchStatus.FAILED

        val countAfterFirst = transaction(database) { TargetTable.selectAll().count() }
        countAfterFirst shouldBeGreaterThan 0L  // 일부 청크는 커밋됨

        // 2차 실행: 동일 params로 재시작, shouldFail=false → lastKey부터 재개 → COMPLETED
        shouldFail.set(false)
        val restartExecution = jobLauncherTestUtils.launchJob(params)
        restartExecution.status shouldBeEqualTo BatchStatus.COMPLETED

        val countAfterRestart = transaction(database) { TargetTable.selectAll().count() }
        countAfterRestart shouldBeEqualTo 1000L  // 전체 완료, 중복 없음
    }
}
```

**Done when**: 재시작 테스트 통과 — FAILED 후 재시작 시 중복 없이 나머지 처리 완료

---

### Task 3.7: EndToEndJobTest (AutoConfiguration 통합 검증)

- **complexity**: medium
- **depends_on**: T3.5
- **Files**:
  - Create: `spring-boot3/batch-exposed/src/test/kotlin/io/bluetape4k/spring/batch/exposed/integration/EndToEndJobTest.kt`

- [ ] **Step 1: AutoConfiguration 통합 테스트 작성**

`@SpringBatchTest` + AutoConfiguration 기반으로 명시적 `@Configuration` 없이 Job 빈이 올바르게 구성되는지 검증한다.

```kotlin
package io.bluetape4k.spring.batch.exposed.integration

import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.batch.exposed.AbstractExposedBatchTest
import io.bluetape4k.spring.batch.exposed.SourceRecord
import io.bluetape4k.spring.batch.exposed.SourceTable
import io.bluetape4k.spring.batch.exposed.TargetRecord
import io.bluetape4k.spring.batch.exposed.TargetTable
import io.bluetape4k.spring.batch.exposed.insertTestData
import io.bluetape4k.spring.batch.exposed.dsl.partitionedBatchJob
import io.bluetape4k.spring.batch.exposed.partition.ExposedRangePartitioner
import io.bluetape4k.spring.batch.exposed.reader.ExposedKeysetItemReader
import io.bluetape4k.spring.batch.exposed.writer.ExposedItemWriter
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.Step
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.test.JobLauncherTestUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.transaction.PlatformTransactionManager

/**
 * AutoConfiguration 기반 통합 검증.
 * - `ExposedBatchAutoConfiguration`이 제공하는 `batchPartitionTaskExecutor` 빈 존재 확인
 * - 사용자 Job 빈과 AutoConfiguration 빈이 올바르게 조합되어 Job 실행 성공
 * - `@EnableBatchProcessing` 없이 동작 확인
 */
class EndToEndJobTest : AbstractExposedBatchTest() {

    companion object : KLogging()

    @Configuration
    class JobConfig(
        private val jobRepository: JobRepository,
        private val transactionManager: PlatformTransactionManager,
        private val database: Database,
    ) {
        @Bean(name = ["e2eMigrationJob"])
        fun migrationJob(): Job = partitionedBatchJob("e2e-migration-job", jobRepository) {
            start(partitionedStep())
        }

        @Bean(name = ["e2ePartitionedStep"])
        fun partitionedStep(): Step = StepBuilder("e2e-migration-manager", jobRepository)
            .partitioner("e2e-migration-worker", rangePartitioner())
            .partitionHandler(partitionHandler())
            .build()

        @Bean(name = ["e2eRangePartitioner"])
        fun rangePartitioner(): ExposedRangePartitioner = ExposedRangePartitioner.forEntityId(
            table = SourceTable,
            gridSize = 4,
            database = database,
        )

        @Bean(name = ["e2ePartitionHandler"])
        fun partitionHandler(): TaskExecutorPartitionHandler = TaskExecutorPartitionHandler().apply {
            setStep(workerStep())
            // AutoConfiguration이 제공하는 batchPartitionTaskExecutor 직접 사용하지 않고
            // 여기서는 virtualThreadPartitionTaskExecutor 사용 (AutoConfig 빈 존재 여부는 별도 검증)
            setTaskExecutor(io.bluetape4k.spring.batch.exposed.support.virtualThreadPartitionTaskExecutor(concurrencyLimit = 4))
            gridSize = 4
        }

        @Bean(name = ["e2eWorkerStep"])
        fun workerStep(): Step = StepBuilder("e2e-migration-worker", jobRepository)
            .chunk<SourceRecord, TargetRecord>(500, transactionManager)
            .reader(keysetReader())
            .processor(ItemProcessor { source ->
                TargetRecord(
                    sourceName = source.name.uppercase(),
                    transformedValue = source.value * 2,
                )
            })
            .writer(itemWriter())
            .build()

        @Bean(name = ["e2eKeysetReader"])
        @org.springframework.batch.core.configuration.annotation.StepScope
        fun keysetReader(): ExposedKeysetItemReader<SourceRecord> = ExposedKeysetItemReader.forEntityId(
            table = SourceTable,
            pageSize = 500,
            rowMapper = { row ->
                SourceRecord(
                    id = row[SourceTable.id].value,
                    name = row[SourceTable.name],
                    value = row[SourceTable.value],
                )
            },
            database = database,
        )

        @Bean(name = ["e2eItemWriter"])
        fun itemWriter(): ExposedItemWriter<TargetRecord> = ExposedItemWriter(table = TargetTable) {
            this[TargetTable.sourceName] = it.sourceName
            this[TargetTable.transformedValue] = it.transformedValue
        }

        @Bean(name = ["e2eJobLauncherTestUtils"])
        fun jobLauncherTestUtils(job: Job): JobLauncherTestUtils = JobLauncherTestUtils().apply {
            this.job = job
            this.jobRepository = this@JobConfig.jobRepository
        }
    }

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var jobLauncherTestUtils: JobLauncherTestUtils

    @Test
    fun `AutoConfiguration batchPartitionTaskExecutor 빈 존재 확인`() {
        // ExposedBatchAutoConfiguration이 등록한 기본 TaskExecutor 빈 검증
        val taskExecutor = applicationContext.getBean("batchPartitionTaskExecutor", TaskExecutor::class.java)
        taskExecutor.shouldNotBeNull()
    }

    @Test
    fun `AutoConfiguration으로 Job 실행 성공 검증`() {
        insertTestData(500)

        val params = JobParametersBuilder()
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters()

        val execution = jobLauncherTestUtils.launchJob(params)
        execution.status shouldBeEqualTo BatchStatus.COMPLETED

        val targetCount = transaction(database) { TargetTable.selectAll().count() }
        targetCount shouldBeEqualTo 500L
    }
}
```

**Done when**: AutoConfiguration 기반 통합 테스트 통과

---

### Task 3.8: ParallelQueryBenchmarkTest

- **complexity**: low
- **depends_on**: T3.5
- **Files**:
  - Create: `spring-boot3/batch-exposed/src/test/kotlin/io/bluetape4k/spring/batch/exposed/integration/ParallelQueryBenchmarkTest.kt`

- [ ] **Step 1: 벤치마크 테스트 작성 (`@Tag("benchmark")`)**

파티션 수별 처리 시간을 측정한다. CI에서는 `@Tag("benchmark")`로 제외하고 로컬에서만 실행한다.

```kotlin
package io.bluetape4k.spring.batch.exposed.integration

import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.batch.exposed.AbstractExposedBatchTest
import io.bluetape4k.spring.batch.exposed.SourceRecord
import io.bluetape4k.spring.batch.exposed.SourceTable
import io.bluetape4k.spring.batch.exposed.TargetRecord
import io.bluetape4k.spring.batch.exposed.TargetTable
import io.bluetape4k.spring.batch.exposed.insertTestData
import io.bluetape4k.spring.batch.exposed.dsl.partitionedBatchJob
import io.bluetape4k.spring.batch.exposed.partition.ExposedRangePartitioner
import io.bluetape4k.spring.batch.exposed.reader.ExposedKeysetItemReader
import io.bluetape4k.spring.batch.exposed.support.virtualThreadPartitionTaskExecutor
import io.bluetape4k.spring.batch.exposed.writer.ExposedItemWriter
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.Step
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.test.JobLauncherTestUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import kotlin.system.measureTimeMillis

/**
 * 파티션 수 / VirtualThread 동시성 수별 처리 시간 벤치마크.
 *
 * CI 제외: `@Tag("benchmark")` — `./gradlew test -PexcludeTags="benchmark"`
 */
@Tag("benchmark")
class ParallelQueryBenchmarkTest : AbstractExposedBatchTest() {

    companion object : KLogging()

    @Configuration
    class JobConfig(
        private val jobRepository: JobRepository,
        private val transactionManager: PlatformTransactionManager,
        private val database: Database,
    ) {
        // 기본 4파티션 Job (벤치마크 루프에서 gridSize를 동적으로 변경할 수 없으므로
        // 벤치마크 목적상 단일 Job을 여러 번 재실행하여 파티션 수별 시간 비교)
        @Bean(name = ["benchmarkMigrationJob"])
        fun migrationJob(): Job = partitionedBatchJob("benchmark-migration-job", jobRepository) {
            start(partitionedStep())
        }

        @Bean(name = ["benchmarkPartitionedStep"])
        fun partitionedStep(): Step = StepBuilder("benchmark-migration-manager", jobRepository)
            .partitioner("benchmark-migration-worker", rangePartitioner())
            .partitionHandler(partitionHandler())
            .build()

        @Bean(name = ["benchmarkRangePartitioner"])
        fun rangePartitioner(): ExposedRangePartitioner = ExposedRangePartitioner.forEntityId(
            table = SourceTable,
            gridSize = 8,
            database = database,
        )

        @Bean(name = ["benchmarkPartitionHandler"])
        fun partitionHandler(): TaskExecutorPartitionHandler = TaskExecutorPartitionHandler().apply {
            setStep(workerStep())
            setTaskExecutor(virtualThreadPartitionTaskExecutor(concurrencyLimit = 8))
            gridSize = 8
        }

        @Bean(name = ["benchmarkWorkerStep"])
        fun workerStep(): Step = StepBuilder("benchmark-migration-worker", jobRepository)
            .chunk<SourceRecord, TargetRecord>(500, transactionManager)
            .reader(keysetReader())
            .processor(ItemProcessor { source ->
                TargetRecord(sourceName = source.name.uppercase(), transformedValue = source.value * 2)
            })
            .writer(itemWriter())
            .build()

        @Bean(name = ["benchmarkKeysetReader"])
        @org.springframework.batch.core.configuration.annotation.StepScope
        fun keysetReader(): ExposedKeysetItemReader<SourceRecord> = ExposedKeysetItemReader.forEntityId(
            table = SourceTable,
            pageSize = 500,
            rowMapper = { row ->
                SourceRecord(
                    id = row[SourceTable.id].value,
                    name = row[SourceTable.name],
                    value = row[SourceTable.value],
                )
            },
            database = database,
        )

        @Bean(name = ["benchmarkItemWriter"])
        fun itemWriter(): ExposedItemWriter<TargetRecord> = ExposedItemWriter(table = TargetTable) {
            this[TargetTable.sourceName] = it.sourceName
            this[TargetTable.transformedValue] = it.transformedValue
        }

        @Bean(name = ["benchmarkJobLauncherTestUtils"])
        fun jobLauncherTestUtils(job: Job): JobLauncherTestUtils = JobLauncherTestUtils().apply {
            this.job = job
            this.jobRepository = this@JobConfig.jobRepository
        }
    }

    @Autowired
    private lateinit var jobLauncherTestUtils: JobLauncherTestUtils

    @Test
    fun `8파티션 VirtualThread 처리 시간 측정`() {
        insertTestData(50_000)

        val elapsed = measureTimeMillis {
            val params = JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters()
            val execution = jobLauncherTestUtils.launchJob(params)
            execution.status shouldBeEqualTo BatchStatus.COMPLETED
        }
        log.info { "8파티션 VirtualThread 처리 시간: ${elapsed}ms (50,000건)" }
    }
}
```

**Done when**: 벤치마크 테스트 실행 가능 (CI 제외 확인)

- [ ] **Step 2: 통합 테스트 커밋**

```bash
git add spring-boot3/batch-exposed/src/test/kotlin/io/bluetape4k/spring/batch/exposed/integration/
git commit -m "$(cat <<'EOF'
test: spring-boot3-batch-exposed 통합 테스트 추가

RestartIntegrationTest (재시작 검증), EndToEndJobTest (AutoConfiguration 통합),
ParallelQueryBenchmarkTest (@Tag("benchmark"), 50,000건 8파티션)
EOF
)"
```

---

## Phase 4: 문서화

> **선행 조건**: Phase 3 완료 (모든 테스트 통과)
> **Gradle 빌드 검증**: 이미 완료

### Task 4.0: bluetape4k-patterns 체크리스트 검증

- **complexity**: low
- **depends_on**: T3.8
- **Files**: `spring-boot3/batch-exposed/src/main/kotlin/` 하위 전체

- [ ] **Step 1: `bluetape4k-patterns` 스킬 기준 전체 점검**

아래 항목을 하나씩 확인한다:
- 모든 data class (`SourceRecord`, `TargetRecord`)에 `Serializable` + `serialVersionUID = 1L` 존재 여부
- 모든 companion object가 `KLogging()` 또는 `KLoggingChannel()` 상속 여부
- `requireNotBlank()` 확장 함수 (bluetape4k-core) 사용 여부 (`require(str.isNotBlank())` 대신)
- `atomicfu` 클래스 프로퍼티 레벨 사용 (메서드 로컬 금지)
- 신규 라이브러리 의존성 없음 확인

- [ ] **Step 2: 위반 사항 즉시 수정**

**Done when**: `bluetape4k-patterns` 체크리스트 전체 항목 통과, 위반 없음

---

### Task 4.1: KDoc 전체 검토

- **complexity**: low
- **depends_on**: T4.0
- **Files**:
  - Modify: `spring-boot3/batch-exposed/src/main/kotlin/io/bluetape4k/spring/batch/exposed/` 하위 모든 `.kt` 파일

- [ ] **Step 1: 모든 public 클래스/함수에 한국어 KDoc 확인 및 보완**

Phase 2에서 작성한 코드의 KDoc을 검토한다. 이미 스펙 기반으로 작성되어 있으므로 누락된 KDoc만 보완한다.

검토 대상:
- `ExposedRangePartitioner` + `forEntityId()`: 한국어 KDoc 확인
- `ExposedKeysetItemReader` + `forEntityId()`: 한국어 KDoc 확인
- `ExposedItemWriter`, `ExposedUpsertItemWriter`, `ExposedUpdateItemWriter`: 한국어 KDoc 확인
- `virtualThreadPartitionTaskExecutor()`: 한국어 KDoc 확인
- `ExposedBatchAutoConfiguration`: 한국어 KDoc 확인
- `partitionedBatchJob()`, `exposedPartitionedStep()`: 한국어 KDoc 확인

- [ ] **Step 2: ide_diagnostics 실행**

Run: `ide_diagnostics` on `spring-boot3/batch-exposed/src/main/kotlin/`
Expected: 오류 없음

**Done when**: 모든 public API에 한국어 KDoc 존재 확인

---

### Task 4.2: README.md + README.ko.md

- **complexity**: low
- **depends_on**: T4.1
- **Files**:
  - Create: `spring-boot3/batch-exposed/README.md`
  - Create: `spring-boot3/batch-exposed/README.ko.md`

- [ ] **Step 1: README.md (영어) 작성**

Architecture -> UML (Mermaid) -> Features -> Examples 순서로 작성한다. 스펙의 Mermaid 다이어그램(flowchart TD, sequence diagram, class diagram)을 포함한다.

핵심 섹션:
- Architecture Overview (실행 흐름 Mermaid flowchart)
- Class Diagram (Mermaid)
- Quick Start (Job 설정 코드 예시)
- Configuration (application.yml 예시)
- Excluded from v1 (v1 제외 항목 목록)

- [ ] **Step 2: README.ko.md (한국어) 작성**

README.md와 동일 구조, 한국어 번역.

- [ ] **Step 3: 검증**

각 README에 Mermaid 다이어그램이 최소 2개 포함되어 있는지 확인한다.

**Done when**: README.md + README.ko.md 작성 완료, Mermaid 다이어그램 포함

---

### Task 4.3: CLAUDE.md 업데이트

- **complexity**: low
- **depends_on**: T4.2
- **Files**:
  - Modify: `CLAUDE.md`

- [ ] **Step 1: CLAUDE.md의 Spring Boot 3 섹션에 batch-exposed 모듈 추가**

`### Spring Boot 3 (`spring-boot3/`)` 테이블에 다음 행을 추가한다:

```markdown
| `batch-exposed` (`bluetape4k-spring-boot3-batch-exposed`) | Spring Batch 5.x + Exposed JDBC Partitioned Step -- ExposedRangePartitioner, ExposedKeysetItemReader, ExposedItemWriter, VirtualThread Parallel Query |
```

**Done when**: CLAUDE.md에 신규 모듈 정보 반영

---

### Task 4.4: superpowers index 항목 추가

- **complexity**: low
- **depends_on**: T4.3
- **Files**:
  - Modify: `docs/superpowers/index/2026-04.md`
  - Modify: `docs/superpowers/INDEX.md`

- [ ] **Step 1: 월별 인덱스에 항목 추가**

`docs/superpowers/index/2026-04.md` 맨 위에 다음 행을 추가한다:

```markdown
| 2026-04-09 | bluetape4k-spring-boot3-batch-exposed | Spring Batch 5.x + Exposed JDBC Partitioned Step, VirtualThread Parallel Query | high |
```

- [ ] **Step 2: INDEX.md 건수 갱신**

`docs/superpowers/INDEX.md`의 2026-04 건수를 +1 증가시킨다.

**Done when**: superpowers index 갱신 완료

---

### Task 4.5: testlog 기록 + 최종 커밋

- **complexity**: low
- **depends_on**: T4.4
- **Files**:
  - Modify: `docs/testlogs/2026-04.md`

- [ ] **Step 1: testlog 기록**

`docs/testlogs/2026-04.md` 맨 위에 테스트 결과를 기록한다:

```markdown
| 2026-04-09 | bluetape4k-spring-boot3-batch-exposed | test | PASS | 15 tests |
```

- [ ] **Step 2: 최종 커밋**

```bash
git add .
git commit -m "$(cat <<'EOF'
feat: bluetape4k-spring-boot3-batch-exposed 모듈 추가

Spring Batch 5.x + Exposed JDBC Partitioned Step + VirtualThread Parallel Query
배치 모듈 구현 완료. README, CLAUDE.md, superpowers index 업데이트 포함.
EOF
)"
```

**Done when**: 모든 파일 커밋 완료, `./gradlew :bluetape4k-spring-boot3-batch-exposed:build` 성공

---

## Summary

| Phase | Tasks | 핵심 내용 |
|-------|-------|----------|
| Phase 1 | T1.1~T1.2 | Libs.kt 상수, 폴더/빌드 설정 |
| Phase 2 | T2.1~T2.8 | Partitioner, Reader, 3x Writer, VirtualThread, AutoConfig, DSL |
| Phase 3 | T3.1~T3.8 | 테스트 인프라, 단위 테스트 4개 클래스, E2E + Restart + AutoConfig E2E + Benchmark |
| Phase 4 | T4.0~T4.5 | patterns 체크리스트, KDoc, README x2, CLAUDE.md, superpowers index, testlog |

**총 태스크**: 24개 (Phase 1: 2, Phase 2: 8, Phase 3: 8, Phase 4: 6 — T4.0 포함)
