# bluetape4k-spring-boot3-batch-exposed

[English](./README.md) | 한국어

**Spring Boot 3 + Spring Batch 5 + Exposed 통합 모듈**

Spring Batch 5와 JetBrains Exposed를 통합하는 고성능 배치 처리 모듈입니다.
Keyset 기반 페이지 읽기 Reader, Exposed 기반 Writer, VirtualThread 병렬 실행을 위한
Range Partitioner, Spring Boot Auto-Configuration을 제공합니다.

## 아키텍처

```mermaid
classDiagram
    class ExposedKeysetItemReader {
        -database: Database
        -pageSize: Int
        -column: ExpressionWithColumnType~Long~
        -table: Table
        -rowMapper: (ResultRow) -> T
        +open(executionContext)
        +read(): T?
        +update(executionContext)
        +close()
        +forEntityId()$
    }
    class ExposedItemWriter {
        -database: Database
        -table: Table
        -mapper: (InsertStatement, T) -> Unit
        +write(chunk)
    }
    class ExposedUpdateItemWriter {
        -database: Database
        -table: Table
        -idExtractor: (T) -> Any
        -mapper: (UpdateStatement, T) -> Unit
        +write(chunk)
    }
    class ExposedUpsertItemWriter {
        -database: Database
        -table: Table
        -mapper: (UpsertStatement, T) -> Unit
        +write(chunk)
    }
    class ExposedRangePartitioner {
        -table: IdTable~Long~
        -gridSize: Int
        -database: Database
        +partition(gridSize): Map~String, ExecutionContext~
        +forEntityId()$
    }
    class ExposedBatchAutoConfiguration {
        +batchPartitionTaskExecutor(): TaskExecutor
    }

    ExposedKeysetItemReader ..|> ItemStreamReader
    ExposedItemWriter ..|> ItemWriter
    ExposedUpdateItemWriter ..|> ItemWriter
    ExposedUpsertItemWriter ..|> ItemWriter
    ExposedRangePartitioner ..|> Partitioner
```

```mermaid
sequenceDiagram
    participant Manager as 파티션 매니저 Step
    participant Partitioner as ExposedRangePartitioner
    participant Handler as TaskExecutorPartitionHandler
    participant Worker as Worker Step (×N VirtualThread)
    participant Reader as ExposedKeysetItemReader
    participant Writer as ExposedItemWriter

    Manager->>Partitioner: partition(gridSize)
    Partitioner-->>Manager: Map<String, ExecutionContext> (파티션별 minId/maxId)
    Manager->>Handler: handle(stepSplitter, stepExecution)
    loop 각 파티션 (VirtualThread)
        Handler->>Worker: execute(executionContext)
        Worker->>Reader: open(executionContext) — minId/maxId/lastKey 설정
        loop 페이지 단위
            Worker->>Reader: read() → T
            Reader->>Reader: fetchNextPage (WHERE id > lastKey AND id <= maxId)
        end
        Worker->>Writer: write(chunk)
        Writer->>Writer: Exposed batchInsert
        Worker->>Reader: update(executionContext) — lastKey 저장 (재시작 지원)
    end
```

## 주요 기능

- **`ExposedKeysetItemReader<T>`** — Keyset 페이지 읽기 Reader
  - `WHERE column > lastKey AND column <= maxId ORDER BY column LIMIT pageSize`
  - 재시작 시 `lastKey`를 `ExecutionContext`에 저장하여 마지막 위치부터 재개
  - `@Synchronized read()`로 스레드 안전 보장
  - 팩토리: `forEntityId(table, pageSize, rowMapper, database)`

- **`ExposedItemWriter<T>`** — Exposed `batchInsert` 기반 대량 INSERT

- **`ExposedUpdateItemWriter<T>`** — Exposed DSL 기반 대량 UPDATE

- **`ExposedUpsertItemWriter<T>`** — Exposed `batchUpsert` 기반 대량 UPSERT

- **`ExposedRangePartitioner`** — `[minId, maxId]` 범위를 N개 파티션으로 분할
  - 테이블에서 `MIN(id)` / `MAX(id)` 자동 조회
  - 파티션별 `minId` / `maxId`를 `ExecutionContext`에 저장

- **`ExposedBatchAutoConfiguration`** — Spring Boot Auto-Configuration
  - `batchPartitionTaskExecutor` (VirtualThread 기반 `TaskExecutor`) 자동 등록

- **`virtualThreadPartitionTaskExecutor(concurrencyLimit)`** — 동시성 제한 VirtualThread `TaskExecutor` 생성 헬퍼

- **`partitionedBatchJob` DSL** — 파티션된 `Job` 빌드를 위한 Kotlin DSL

## 사용 예시

### build.gradle.kts

```kotlin
implementation("io.github.bluetape4k:bluetape4k-spring-boot3-batch-exposed")
```

### 파티션 마이그레이션 Job

```kotlin
@TestConfiguration
class MigrationJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val database: Database,
) {
    @Bean
    fun migrationJob(): Job = partitionedBatchJob("my-migration-job", jobRepository) {
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
            TargetRecord(sourceName = source.name.uppercase(), transformedValue = source.value * 2)
        })
        .writer(itemWriter())
        .build()

    @Bean
    @StepScope
    fun keysetReader(): ExposedKeysetItemReader<SourceRecord> = ExposedKeysetItemReader.forEntityId(
        table = SourceTable,
        pageSize = 500,
        rowMapper = { row ->
            SourceRecord(id = row[SourceTable.id].value, name = row[SourceTable.name], value = row[SourceTable.value])
        },
        database = database,
    )

    @Bean
    fun itemWriter(): ExposedItemWriter<TargetRecord> = ExposedItemWriter(table = TargetTable) {
        this[TargetTable.sourceName] = it.sourceName
        this[TargetTable.transformedValue] = it.transformedValue
    }
}
```

### 재시작 지원

동일한 Job 파라미터로 재실행하면 `lastKey` 이후부터 자동 재개됩니다:

```kotlin
// 1차 실행: 중간에 실패
val firstExecution = jobLauncher.run(job, params)  // BatchStatus.FAILED

// 2차 실행: 동일 params — lastKey부터 재개
val restartExecution = jobLauncher.run(job, params)  // BatchStatus.COMPLETED
```

## 벤치마크 결과

로컬 머신 기준, 50,000건, 청크 크기 500으로 측정한 결과입니다.
PostgreSQL은 Testcontainers(`postgres:18-alpine`)로 실행합니다.

```mermaid
xychart-beta
    title "파티션 수별 처리 시간 (ms, 낮을수록 좋음)"
    x-axis ["1파티션 (순차)", "4파티션 (병렬)", "8파티션 (병렬)"]
    y-axis "처리 시간 (ms)" 0 --> 1200
    line [1026, 575, 358]
    line [1127, 371, 282]
```

### H2 인메모리 DB

| 파티션 수 | 동시성 | 처리 시간 (ms) | 속도 향상 |
|:---------:|:------:|:--------------:|:---------:|
| 1 (순차) | 1 | 1,026 | 1.0× |
| 4 (병렬) | 4 | 575 | **1.8×** |
| 8 (병렬) | 8 | 358 | **2.9×** |

### PostgreSQL (Testcontainers)

| 파티션 수 | 동시성 | 처리 시간 (ms) | 속도 향상 |
|:---------:|:------:|:--------------:|:---------:|
| 1 (순차) | 1 | 1,127 | 1.0× |
| 4 (병렬) | 4 | 371 | **3.0×** |
| 8 (병렬) | 8 | 282 | **4.0×** |

> **핵심 인사이트:** PostgreSQL에서 병렬화 효과가 H2보다 크게 나타납니다 (H2 2.9× → PG 4.0×).
> 실제 네트워크·I/O 지연이 있는 환경일수록 쿼리 당 대기 시간이 길어지므로,
> 동시 실행으로 얻는 절대적 시간 단축 효과가 더 큽니다.

로컬에서 직접 실행:

```bash
# H2 비교 벤치마크
./gradlew :bluetape4k-spring-boot3-batch-exposed:test \
  --tests "*PartitionComparisonBenchmarkTest" -PincludeTags="benchmark"

# PostgreSQL 비교 벤치마크
./gradlew :bluetape4k-spring-boot3-batch-exposed:test \
  --tests "*PartitionComparisonPgBenchmarkTest" -PincludeTags="benchmark"
```

## 모듈 의존성

```
bluetape4k-spring-boot3-batch-exposed
  ├── spring-batch-core (5.x)
  ├── spring-batch-test
  ├── bluetape4k-exposed-jdbc
  └── bluetape4k-virtualthread-api
```
