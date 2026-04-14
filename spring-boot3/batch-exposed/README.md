# bluetape4k-spring-boot3-batch-exposed

English | [한국어](./README.ko.md)

**Spring Batch + Exposed Integration for Spring Boot 3**

A high-performance batch processing module that integrates Spring Batch 5 with JetBrains Exposed.
Provides keyset-based pagination readers, efficient Exposed-backed writers, range partitioners
for VirtualThread parallel execution, and Spring Boot Auto-Configuration.

## Architecture

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

    style ExposedKeysetItemReader fill:#00897B,stroke:#00695C,color:#FFFFFF
    style ExposedItemWriter fill:#00897B,stroke:#00695C,color:#FFFFFF
    style ExposedUpdateItemWriter fill:#00897B,stroke:#00695C,color:#FFFFFF
    style ExposedUpsertItemWriter fill:#00897B,stroke:#00695C,color:#FFFFFF
    style ExposedRangePartitioner fill:#E65100,stroke:#BF360C,color:#FFFFFF
    style ExposedBatchAutoConfiguration fill:#2E7D32,stroke:#1B5E20,color:#FFFFFF
    style ItemStreamReader fill:#1565C0,stroke:#0D47A1,color:#FFFFFF
    style ItemWriter fill:#1565C0,stroke:#0D47A1,color:#FFFFFF
    style Partitioner fill:#1565C0,stroke:#0D47A1,color:#FFFFFF
```

```mermaid
sequenceDiagram
    box rgb(27,94,32) Batch Management
        participant Manager as Partition Manager Step
        participant Partitioner as ExposedRangePartitioner
        participant Handler as TaskExecutorPartitionHandler
    end
    box rgb(178,223,219) Batch Execution
        participant Worker as Worker Step (×N VirtualThread)
        participant Reader as ExposedKeysetItemReader
        participant Writer as ExposedItemWriter
    end

    Manager->>Partitioner: partition(gridSize)
    Partitioner-->>Manager: Map<String, ExecutionContext> (minId/maxId per partition)
    Manager->>Handler: handle(stepSplitter, stepExecution)
    loop each partition (VirtualThread)
        Handler->>Worker: execute(executionContext)
        Worker->>Reader: open(executionContext) — sets minId/maxId/lastKey
        loop pages
            Worker->>Reader: read() → T
            Reader->>Reader: fetchNextPage (keyset WHERE id > lastKey AND id <= maxId)
        end
        Worker->>Writer: write(chunk)
        Writer->>Writer: batchInsert via Exposed
        Worker->>Reader: update(executionContext) — saves lastKey for restart
    end
```

## Features

- **`ExposedKeysetItemReader<T>`** — Keyset pagination reader
  - `WHERE column > lastKey AND column <= maxId ORDER BY column LIMIT pageSize`
  - Persists `lastKey` in `ExecutionContext` for restart support
  - Thread-safe with `reentrantLock().withLock { ... }` in `read()` (Virtual Thread-friendly)
  - Factory: `forEntityId(table, pageSize, rowMapper, database)`

- **`ExposedItemWriter<T>`** — Batch INSERT via Exposed `batchInsert`

- **`ExposedUpdateItemWriter<T>`** — Batch UPDATE via Exposed DSL

- **`ExposedUpsertItemWriter<T>`** — Batch UPSERT via Exposed `batchUpsert`

- **`ExposedRangePartitioner`** — Divides `[minId, maxId]` range into N partitions
  - Reads `MIN(id)` and `MAX(id)` from the table
  - Stores `minId` / `maxId` per partition in `ExecutionContext`

- **`ExposedBatchAutoConfiguration`** — Spring Boot Auto-Configuration
  - Registers `batchPartitionTaskExecutor` (VirtualThread-based `TaskExecutor`)

- **`virtualThreadPartitionTaskExecutor(concurrencyLimit)`** — Helper to create a VirtualThread `TaskExecutor` with concurrency limit

- **`partitionedBatchJob` DSL** — Kotlin DSL for building partitioned `Job`

## Usage

### build.gradle.kts

```kotlin
implementation("io.github.bluetape4k:bluetape4k-spring-boot3-batch-exposed")
```

### Partitioned Migration Job

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

## Benchmark Results

Measured on a local machine with 50,000 rows, chunk size 500.
PostgreSQL runs via Testcontainers (`postgres:18-alpine`).

```mermaid
xychart-beta
    title "Partition Count vs Elapsed Time (ms, lower is better)"
    x-axis ["1 partition (seq)", "4 partitions (parallel)", "8 partitions (parallel)"]
    y-axis "Elapsed Time (ms)" 0 --> 1200
    line [1026, 575, 358]
    line [1127, 371, 282]
```

### H2 In-Memory DB

| Partitions | Concurrency | Elapsed (ms) | Speedup |
|:----------:|:-----------:|:------------:|:-------:|
| 1 (sequential) | 1 | 1,026 | 1.0× |
| 4 (parallel) | 4 | 575 | **1.8×** |
| 8 (parallel) | 8 | 358 | **2.9×** |

### PostgreSQL (Testcontainers)

| Partitions | Concurrency | Elapsed (ms) | Speedup |
|:----------:|:-----------:|:------------:|:-------:|
| 1 (sequential) | 1 | 1,127 | 1.0× |
| 4 (parallel) | 4 | 371 | **3.0×** |
| 8 (parallel) | 8 | 282 | **4.0×** |

> **Key insight:** PostgreSQL shows greater parallel speedup (4.0×) than H2 (2.9×) because
> real network and I/O latency per query makes concurrent execution more beneficial.

Run benchmarks locally:

```bash
# H2 comparison
./gradlew :bluetape4k-spring-boot3-batch-exposed:test \
  --tests "*PartitionComparisonBenchmarkTest" -PincludeTags="benchmark"

# PostgreSQL comparison
./gradlew :bluetape4k-spring-boot3-batch-exposed:test \
  --tests "*PartitionComparisonPgBenchmarkTest" -PincludeTags="benchmark"
```

## Module Dependencies

```
bluetape4k-spring-boot3-batch-exposed
  ├── spring-batch-core (5.x)
  ├── spring-batch-test
  ├── bluetape4k-exposed-jdbc
  └── bluetape4k-virtualthread-api
```
