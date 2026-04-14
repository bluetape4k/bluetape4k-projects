# bluetape4k-leader

[English](./README.md) | 한국어

분산 환경에서 여러 프로세스/스레드 간에 동일한 작업이 중복 실행되는 것을 방지합니다. **리더**로 선출된 인스턴스만 작업을 실행하고 나머지는 건너뜁니다.

## 사용 시나리오

### 단일 리더 (`LeaderElection`) — 동시에 1개만 실행

| 시나리오    | 효과                                   |
|---------|--------------------------------------|
| 스케줄 작업  | 여러 인스턴스에서 동일한 스케줄 작업이 중복 실행되지 않도록 방지 |
| 캐시 갱신   | 분산 캐시의 갱신 작업을 하나의 인스턴스에서만 수행         |
| 알림 발송   | 중복 알림 방지                             |
| 데이터 동기화 | 외부 시스템과의 동기화 작업 중복 방지                |

### 복수 리더 (`LeaderGroupElection`) — 동시에 N개까지 실행

| 시나리오          | 효과                                  |
|---------------|-------------------------------------|
| 병렬 배치 처리      | 대용량 데이터를 N개 청크로 나누어 동시 처리 (처리 수 제어) |
| Rate Limiting | 외부 API 동시 호출 수 제한                   |
| 작업 풀 관리       | 정해진 수의 워커만 특정 작업을 동시에 수행하도록 제어      |
| 리소스 보호        | DB 연결 등 제한된 리소스를 사용하는 작업의 동시성 제어    |

## 아키텍처

### 개념 개요 — 단일 리더

```mermaid
flowchart LR
    subgraph Instances["분산 인스턴스"]
        I1["인스턴스 1"]
        I2["인스턴스 2"]
        I3["인스턴스 3"]
    end

    I1 -->|" runIfLeader(lockName) "| LE[LeaderElection]
    I2 -->|" runIfLeader(lockName) "| LE
    I3 -->|" runIfLeader(lockName) "| LE
    LE -->|" 락 획득 "| L["리더\n(작업 실행)"]
    LE -->|" 락 실패 "| S["비리더\n(null 반환)"]
    L --> R["결과 T"]
    S --> N["null (건너뜀)"]

    classDef coreStyle fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32,font-weight:bold
    classDef serviceStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef utilStyle fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    classDef asyncStyle fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    classDef dataStyle fill:#F57F17,stroke:#F57F17,color:#000000

    class I1,I2,I3 utilStyle
    class LE coreStyle
    class L asyncStyle
    class S,N dataStyle
    class R dataStyle
```

### 개념 개요 — 복수 리더 (Semaphore)

```mermaid
flowchart LR
    subgraph Instances["5개 인스턴스"]
        I1["인스턴스 1"] & I2["인스턴스 2"] & I3["인스턴스 3"] & I4["인스턴스 4"] & I5["인스턴스 5"]
    end

    I1 & I2 & I3 & I4 & I5 -->|" runIfLeader(lockName) "| GE["LeaderGroupElection\n(maxLeaders = 3)"]
    GE -->|" 슬롯 획득 (3개 당선) "| L["리더들\n(동시 실행)"]
    GE -->|" 슬롯 없음 (2개 탈락) "| S["비리더\n(null 반환)"]

    classDef coreStyle fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32,font-weight:bold
    classDef serviceStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef utilStyle fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    classDef asyncStyle fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    classDef dataStyle fill:#F57F17,stroke:#F57F17,color:#000000

    class I1,I2,I3,I4,I5 utilStyle
    class GE coreStyle
    class L asyncStyle
    class S dataStyle
```

### 클래스 다이어그램 — 단일 리더

```mermaid
classDiagram
    class AsyncLeaderElection {
        <<interface>>
        +runAsyncIfLeader(lockName, block): CompletableFuture~T?~
    }

    class LeaderElection {
        <<interface>>
        +runIfLeader(lockName, block): T?
    }

    class VirtualThreadLeaderElection {
        <<interface>>
        +runAsyncIfLeader(lockName, block): VirtualFuture~T?~
    }

    class SuspendLeaderElection {
        <<interface>>
        +runIfLeader(lockName, block): T?
    }

    class LocalLeaderElection {
        -lock: ReentrantLock
        +runIfLeader(lockName, block): T?
    }

    class LocalAsyncLeaderElection {
        -lock: ReentrantLock
        +runAsyncIfLeader(lockName, block): CompletableFuture~T?~
    }

    class LocalVirtualThreadLeaderElection {
        -lock: ReentrantLock
        +runAsyncIfLeader(lockName, block): VirtualFuture~T?~
    }

    class LocalSuspendLeaderElection {
        -mutex: Mutex
        +runIfLeader(lockName, block): T?
    }

    AsyncLeaderElection <|-- LeaderElection
    LeaderElection <|.. LocalLeaderElection
    AsyncLeaderElection <|.. LocalAsyncLeaderElection
    VirtualThreadLeaderElection <|.. LocalVirtualThreadLeaderElection
    SuspendLeaderElection <|.. LocalSuspendLeaderElection

    style AsyncLeaderElection fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style LeaderElection fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style VirtualThreadLeaderElection fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style SuspendLeaderElection fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style LocalLeaderElection fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style LocalAsyncLeaderElection fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style LocalVirtualThreadLeaderElection fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style LocalSuspendLeaderElection fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
```

### 클래스 다이어그램 — 복수 리더

```mermaid
classDiagram
    class LeaderGroupElectionState {
        <<interface>>
        +maxLeaders: Int
        +activeCount(lockName): Int
        +availableSlots(lockName): Int
        +isFull(lockName): Boolean
        +isEmpty(lockName): Boolean
    }

    class AsyncLeaderGroupElection {
        <<interface>>
        +runAsyncIfLeader(lockName, block): CompletableFuture~T?~
    }

    class LeaderGroupElection {
        <<interface>>
        +runIfLeader(lockName, block): T?
    }

    class VirtualThreadLeaderGroupElection {
        <<interface>>
        +runAsyncIfLeader(lockName, block): VirtualFuture~T?~
    }

    class SuspendLeaderGroupElection {
        <<interface>>
        +runIfLeader(lockName, block): T?
    }

    class LocalLeaderGroupElection {
        -semaphore: java.util.concurrent.Semaphore
    }

    class LocalAsyncLeaderGroupElection {
        -semaphore: java.util.concurrent.Semaphore
    }

    class LocalVirtualThreadLeaderGroupElection {
        -semaphore: java.util.concurrent.Semaphore
    }

    class LocalSuspendLeaderGroupElection {
        -semaphore: kotlinx.coroutines.sync.Semaphore
    }

    LeaderGroupElectionState <|.. AsyncLeaderGroupElection
    AsyncLeaderGroupElection <|-- LeaderGroupElection
    LeaderGroupElection <|.. LocalLeaderGroupElection
    AsyncLeaderGroupElection <|.. LocalAsyncLeaderGroupElection
    VirtualThreadLeaderGroupElection <|.. LocalVirtualThreadLeaderGroupElection
    SuspendLeaderGroupElection <|.. LocalSuspendLeaderGroupElection

    style LeaderGroupElectionState fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style AsyncLeaderGroupElection fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style LeaderGroupElection fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style VirtualThreadLeaderGroupElection fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style SuspendLeaderGroupElection fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style LocalLeaderGroupElection fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style LocalAsyncLeaderGroupElection fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style LocalVirtualThreadLeaderGroupElection fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style LocalSuspendLeaderGroupElection fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
```

### 실행 시퀀스 — 단일 리더

```mermaid
sequenceDiagram
    box "분산 인스턴스" #E8F5E9
    participant I1 as 인스턴스 1
    participant I2 as 인스턴스 2
    end
    box "조정 레이어" #E3F2FD
    participant LE as LeaderElection
    participant Lock as Lock (ReentrantLock / Mutex)
    end
    box "실행 레이어" #FFF3E0
    participant Task
    end
    I1 ->> LE: runIfLeader("job-lock") { task }
    I2 ->> LE: runIfLeader("job-lock") { task }
    LE ->> Lock: tryLock("job-lock")
    Lock -->> LE: 획득 성공 → I1이 리더
    Lock -->> LE: 획득 실패 → I2는 비리더
    LE ->> Task: block 실행 (I1만)
    Task -->> LE: 결과 T
    LE -->> I1: 결과 T
    LE -->> I2: null (건너뜀)
    LE ->> Lock: unlock("job-lock")
```

## 사용 예시

### 동기 방식 (`LeaderElection`)

```kotlin
class MyScheduler(private val leaderElection: LeaderElection) {

    fun executeTask() {
        val result = leaderElection.runIfLeader("scheduled-task-lock") {
            println("리더입니다! 스케줄 작업을 수행합니다...")
            performExpensiveOperation()
            "Task completed"
        }

        if (result == null) {
            println("리더가 아닙니다. 작업을 건너뜁니다.")
        }
    }
}
```

### 비동기 방식 (`AsyncLeaderElection`)

```kotlin
class MyAsyncService(private val leaderElection: AsyncLeaderElection) {

    fun executeAsyncTask(): CompletableFuture<String?> {
        return leaderElection.runAsyncIfLeader("async-task-lock") {
            CompletableFuture.supplyAsync { performAsyncOperation() }
        }
    }
}
```

### 코루틴 방식 (`SuspendLeaderElection`)

```kotlin
class MyCoroutineService(private val leaderElection: SuspendLeaderElection) {

    suspend fun executeSuspendTask(): String? {
        return leaderElection.runIfLeader("coroutine-task-lock") {
            withContext(Dispatchers.IO) {
                performSuspendOperation()
            }
        }
    }
}
```

### Virtual Thread 방식 (`VirtualThreadLeaderElection`)

```kotlin
val election = LocalVirtualThreadLeaderElection()

val future = election.runAsyncIfLeader("job-lock") {
    performExpensiveIO()  // I/O 블로킹 시 Virtual Thread가 캐리어 스레드를 양보
}

val result = future.await()
```

### 복수 리더 선출 — 동시에 N개까지 실행

```kotlin
// 동기 방식 — 최대 3개 스레드 동시 실행
val election = LocalLeaderGroupElection(maxLeaders = 3)

val result = election.runIfLeader("batch-job") {
    processChunk()  // 슬롯 획득 → 실행 → 자동 반납
}

// 상태 조회
val state = election.state("batch-job")
println("활성 리더: ${state.activeCount} / ${state.maxLeaders}")
println("남은 슬롯: ${state.availableSlots}")
```

### Spring Boot 통합 예시

```kotlin
@Component
class ScheduledTaskRunner(private val leaderElection: LeaderElection) {

    @Scheduled(fixedRate = 60000)
    fun runScheduledTask() {
        leaderElection.runIfLeader("cleanup-job") {
            cleanupOldData()
        }
    }

    @Scheduled(cron = "0 0 2 * * ?")
    fun runDailyBatch() {
        val result = leaderElection.runIfLeader("daily-batch") {
            runBatchJob()
        }
        log.info("Batch job completed: $result")
    }
}
```

## 의존성

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-leader:${version}")
}
```
