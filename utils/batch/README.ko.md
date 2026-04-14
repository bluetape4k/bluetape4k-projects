# bluetape4k-batch

한국어 | [English](./README.md)

Kotlin 코루틴 네이티브 배치 처리 프레임워크. Spring Batch 없이 경량화된 체크포인트 기반 청크 처리 파이프라인을 구현한다.

## 아키텍처

```mermaid
classDiagram
    class BatchJob {
        +name: String
        +params: Map
        +steps: List~BatchStep~
        +run() BatchReport
        +execute(WorkContext) WorkReport
    }

    class BatchStep {
        +name: String
        +chunkSize: Int
        +reader: BatchReader
        +processor: BatchProcessor
        +writer: BatchWriter
        +skipPolicy: SkipPolicy
        +retryPolicy: RetryPolicy
        +commitTimeout: Duration
    }

    class BatchStepRunner {
        -step: BatchStep
        -jobExecution: JobExecution
        -repository: BatchJobRepository
        +run() StepReport
    }

    class BatchJobRepository {
        <<interface>>
        +findOrCreateJobExecution()
        +findOrCreateStepExecution()
        +completeJobExecution()
        +completeStepExecution()
        +saveCheckpoint()
        +loadCheckpoint()
    }

    class BatchReader {
        <<interface>>
        +open()
        +read() T?
        +checkpoint() Any?
        +onChunkCommitted()
        +restoreFrom(checkpoint)
        +close()
    }

    class BatchWriter {
        <<interface>>
        +open()
        +write(items: List~T~)
        +close()
    }

    class SkipPolicy {
        <<fun interface>>
        +shouldSkip(exception, skipCount) Boolean
        NONE$
        ALL$
        maxSkips(n)$
    }

    BatchJob "1" o-- "1..*" BatchStep
    BatchStep --> BatchReader
    BatchStep --> BatchWriter
    BatchStep --> SkipPolicy
    BatchStepRunner --> BatchStep
    BatchStepRunner --> BatchJobRepository

    style BatchJob fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    style BatchStep fill:#FFFDE7,stroke:#FFF176,color:#F57F17
    style BatchStepRunner fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style BatchJobRepository fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style BatchReader fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style BatchWriter fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style SkipPolicy fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
```

```mermaid
sequenceDiagram
    box "오케스트레이션" #E8F5E9
    participant Job as BatchJob
    end
    box "실행" #E3F2FD
    participant Runner as BatchStepRunner
    end
    box "영속성" #FFF3E0
    participant Repo as BatchJobRepository
    end
    box "I/O" #F3E5F5
    participant Reader as BatchReader
    participant Writer as BatchWriter
    end

    Job->>Repo: findOrCreateJobExecution
    loop 각 Step
        Job->>Runner: run()
        Runner->>Repo: findOrCreateStepExecution
        alt 이미 COMPLETED
            Runner-->>Job: StepReport (skip 반환)
        else RUNNING / 신규
            Runner->>Reader: open()
            Runner->>Writer: open()
            Repo-->>Runner: loadCheckpoint
            opt checkpoint != null
                Runner->>Reader: restoreFrom(checkpoint)
            end
            loop 청크 루프
                loop chunkSize 번
                    Runner->>Reader: read()
                    Reader-->>Runner: 아이템 or null(EOF)
                end
                Runner->>Writer: write(chunk)
                Runner->>Repo: saveCheckpoint
                Runner->>Reader: onChunkCommitted()
            end
            Runner->>Repo: completeStepExecution
        end
    end
    Job->>Repo: completeJobExecution
```

## 주요 기능

- **코루틴 우선**: 모든 인터페이스가 `suspend`; `runBlocking` 및 스레드 블로킹 없음
- **체크포인트 재시작**: keyset 기반 체크포인트가 JVM 재시작 후에도 유지됨; 이미 완료된 Step은 자동 skip
- **청크 기반 파이프라인**: `BatchReader → BatchProcessor → BatchWriter` 파이프라인, 청크 크기 설정 가능
- **Skip 정책**: Processor/Writer 실패 시 per-item skip (`NONE` / `ALL` / `maxSkips(n)` / 커스텀 람다)
- **지수 백오프 재시도**: 청크 단위 재시도, 지연 시간 및 지수 백오프 설정 가능
- **커밋 타임아웃**: `WriteTimeoutException` 래퍼로 무한 대기 방지; 일반 오류처럼 재시도/skip
- **취소 안전**: `CancellationException`은 절대 삼키지 않음; `STOPPED` 상태 영속화 후 재던짐
- **Workflow 통합**: `BatchJob`이 `SuspendWork`를 구현하여 `bluetape4k-workflow` 파이프라인에 임베딩 가능
- **JDBC + R2DBC Reader/Writer**: Exposed 기반의 blocking/reactive 데이터베이스 구현체 제공

## 빠른 시작

### DSL로 Job 구성

```kotlin
val job = batchJob("importUsers") {
    repository(myJdbcRepository)
    params("date" to "2026-04-10")
    step<UserCsv, UserEntity>("loadStep") {
        reader(csvReader)
        processor { csv -> UserEntity(csv.name, csv.email) }
        writer(jdbcWriter)
        chunkSize(500)
        skipPolicy(SkipPolicy.maxSkips(100))
        retryPolicy(RetryPolicy(maxAttempts = 3, delay = 1.seconds))
        commitTimeout(30.seconds)
    }
}

val report = job.run()
when (report) {
    is BatchReport.Success           -> println("완료: ${report.stepReports[0].writeCount} rows")
    is BatchReport.PartiallyCompleted -> println("부분완료: skip=${report.stepReports.sumOf { it.skipCount }}")
    is BatchReport.Failure           -> println("실패: ${report.error.message}")
}
```

### 재시작 시나리오

```kotlin
// 1차 실행 — step2에서 실패
val report1 = job.run()  // BatchReport.Failure

// 2차 실행 — step1은 COMPLETED이므로 자동 skip, step2만 재실행
val report2 = job.run()
```

### Workflow에 임베딩

```kotlin
val pipeline = sequentialWorkflow {
    work(validationJob)  // BatchJob이 SuspendWork를 구현함
    work(importJob)
    work(reportJob)
}
val workReport = pipeline.run(WorkContext())
```

## 컴포넌트 설명

### 핵심 클래스

| 클래스 | 설명 |
|--------|------|
| `BatchJob` | Step들을 순차적으로 실행; 재시작 지원; `SuspendWork` 구현 |
| `BatchStep` | Reader → Processor → Writer 파이프라인 설정 |
| `BatchStepRunner` | 단일 Step의 청크 루프 실행 (skip/retry/checkpoint 포함) |

### API 인터페이스

| 인터페이스 | 설명 |
|-----------|------|
| `BatchReader<T>` | 아이템을 하나씩 읽음; 체크포인트 제공 |
| `BatchProcessor<I, O>` | 아이템 변환 (null 반환 = 필터링) |
| `BatchWriter<T>` | 청크 단위 아이템 저장 |
| `BatchJobRepository` | Job/Step 실행 상태 영속화 |
| `SkipPolicy` | 예외 발생 시 skip 여부 결정 |

### 구현체

| 클래스 | 설명 |
|--------|------|
| `InMemoryBatchJobRepository` | 메모리 기반 저장소 (테스트/단순 사용) |
| `ExposedJdbcBatchJobRepository` | Exposed + Virtual Threads JDBC 기반 저장소 |
| `ExposedR2dbcBatchJobRepository` | Exposed suspend 트랜잭션 R2DBC 기반 저장소 |
| `ExposedJdbcBatchReader<K, E>` | keyset 페이징 JDBC Reader |
| `ExposedR2dbcBatchReader<K, E>` | keyset 페이징 R2DBC Reader |
| `ExposedJdbcBatchWriter` | 벌크 JDBC insert/update Writer |
| `ExposedR2dbcBatchWriter` | 벌크 R2DBC insert Writer |

### Skip 정책

```kotlin
SkipPolicy.NONE                      // skip 없음 (기본값)
SkipPolicy.ALL                       // 모든 예외 skip
SkipPolicy.maxSkips(100L)            // 최대 100개 skip
SkipPolicy { e, count -> e is DataException && count < 50 }  // 커스텀
```

## 체크포인트 프로토콜

1. Reader가 `onChunkCommitted()` 호출 후 `checkpoint()`로 체크포인트 값을 반환
2. `BatchStepRunner`가 write 성공 후 Repository에 체크포인트 저장
3. 재시작 시 청크 루프 시작 전 `reader.restoreFrom(checkpoint)` 호출로 상태 복원
4. `TypedCheckpoint` 봉투(Jackson 3)로 모든 직렬화 가능 타입의 타입 안전 round-trip 보장

## BatchStatus 상태 전이

```
STARTING → RUNNING → COMPLETED
                   → COMPLETED_WITH_SKIPS
                   → FAILED
                   → STOPPED (취소)
```

**중요**: `COMPLETED` / `COMPLETED_WITH_SKIPS` 상태의 StepExecution은 재시작 시 자동으로 skip된다.

## 벤치마크

benchmark 체계는 `kotlinx-benchmark` 기반으로 재구성되었고, JDBC + Virtual Threads 및 R2DBC를 DB별 profile로 분리해 실행합니다.

| DB | 요약 | 상세 문서 |
|----|------|-----------|
| H2 | `seedBenchmark`, `endToEndBatchJobBenchmark` 기준 JDBC vs R2DBC 비교 | [H2 상세 결과](docs/benchmark/h2.md) |
| PostgreSQL | Testcontainers 기반으로 같은 시나리오를 JDBC/R2DBC로 비교 | [PostgreSQL 상세 결과](docs/benchmark/postgresql.md) |
| MySQL | seed 및 전체 batch job 실행을 JDBC/R2DBC로 비교 | [MySQL 상세 결과](docs/benchmark/mysql.md) |

- [Benchmark 문서 허브](docs/benchmark/README.ko.md)
- 실행 예시: `./gradlew :bluetape4k-batch:h2JdbcBenchmark`, `./gradlew :bluetape4k-batch:postgresR2dbcBenchmark`, `./gradlew :bluetape4k-batch:generateBenchmarkDocs`

### 비교 초점

- 핵심 비교 축: **JDBC vs R2DBC**
- 시나리오: `seedBenchmark`, `endToEndBatchJobBenchmark`
- 파라미터: `dataSize = 1000/10000/100000`, `poolSize = 10/30/60`, `parallelism = 1/4/8`
- 상세 표와 그래프는 `docs/benchmark/*.md` 에서 관리

```mermaid
xychart-beta
    title "Benchmark 문서 구조"
    x-axis [H2, PostgreSQL, MySQL]
    y-axis "문서화 범위" 0 --> 100
    bar [100, 100, 100]
    bar [100, 100, 100]
```

## 모듈 의존성

```kotlin
dependencies {
    implementation(project(":bluetape4k-batch"))
    // JDBC repository / reader / writer 사용 시:
    implementation(project(":bluetape4k-exposed-jdbc"))
    // R2DBC repository / reader / writer 사용 시:
    implementation(project(":bluetape4k-exposed-r2dbc"))
    // Workflow 임베딩 사용 시:
    implementation(project(":bluetape4k-workflow"))
}
```
