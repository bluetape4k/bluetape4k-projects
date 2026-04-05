# bluetape4k-leader

English | [한국어](./README.ko.md)

Prevents duplicate execution of the same task across multiple processes or threads in a distributed environment. Only the elected
**leader** instance executes the task — all others skip it.

## Use Cases

### Single Leader (`LeaderElection`) — only 1 concurrent execution

| Scenario             | Why it helps                                                           |
|----------------------|------------------------------------------------------------------------|
| Scheduled jobs       | Prevent the same job from running on multiple instances simultaneously |
| Cache refresh        | Run cache update logic on only one instance at a time                  |
| Notifications        | Prevent duplicate alerts from being sent                               |
| Data synchronization | Prevent duplicate sync jobs with external systems                      |

### Group Leader (`LeaderGroupElection`) — up to N concurrent executions

| Scenario                  | Why it helps                                                                        |
|---------------------------|-------------------------------------------------------------------------------------|
| Parallel batch processing | Split large datasets into N chunks, process concurrently with concurrency control   |
| Rate limiting             | Limit concurrent outbound API calls to a backend or external service                |
| Worker pool management    | Allow only a fixed number of workers to run a task at the same time                 |
| Resource protection       | Control concurrency for tasks that consume limited resources (DB connections, etc.) |

## Architecture

### Concept Overview — Single Leader

```mermaid
flowchart LR
    subgraph Instances["Distributed Instances"]
        I1["Instance 1"]
        I2["Instance 2"]
        I3["Instance 3"]
    end

    I1 -->|"runIfLeader(lockName)"| LE[LeaderElection]
    I2 -->|"runIfLeader(lockName)"| LE
    I3 -->|"runIfLeader(lockName)"| LE

    LE -->|"lock acquired"| L["Leader\n(executes task)"]
    LE -->|"lock failed"| S["Non-Leader\n(returns null)"]

    L --> R["Result T"]
    S --> N["null (skipped)"]
```

### Concept Overview — Group Leader (Semaphore)

```mermaid
flowchart LR
    subgraph Instances["5 Instances"]
        I1["Instance 1"] & I2["Instance 2"] & I3["Instance 3"] & I4["Instance 4"] & I5["Instance 5"]
    end

    I1 & I2 & I3 & I4 & I5 -->|"runIfLeader(lockName)"| GE["LeaderGroupElection\n(maxLeaders = 3)"]

    GE -->|"slot acquired (3 win)"| L["Leaders\n(execute concurrently)"]
    GE -->|"slot unavailable (2 lose)"| S["Non-Leaders\n(return null)"]
```

### Class Diagram — Single Leader

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
```

### Class Diagram — Group Leader

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
```

### Execution Sequence — Single Leader

```mermaid
sequenceDiagram
    participant I1 as Instance 1
    participant I2 as Instance 2
    participant LE as LeaderElection
    participant Lock as Lock (ReentrantLock / Mutex)
    participant Task

    I1->>LE: runIfLeader("job-lock") { task }
    I2->>LE: runIfLeader("job-lock") { task }

    LE->>Lock: tryLock("job-lock")
    Lock-->>LE: acquired → I1 is leader
    Lock-->>LE: not acquired → I2 is not leader

    LE->>Task: execute block (I1 only)
    Task-->>LE: result T

    LE-->>I1: result T
    LE-->>I2: null (skipped)

    LE->>Lock: unlock("job-lock")
```

## Usage Examples

### Synchronous (`LeaderElection`)

```kotlin
class MyScheduler(private val leaderElection: LeaderElection) {

    fun executeTask() {
        val result = leaderElection.runIfLeader("scheduled-task-lock") {
            println("I'm the leader! Running scheduled task...")
            performExpensiveOperation()
            "Task completed"
        }

        if (result == null) {
            println("Not the leader, skipping task")
        }
    }
}
```

### Asynchronous (`AsyncLeaderElection`)

```kotlin
class MyAsyncService(private val leaderElection: AsyncLeaderElection) {

    fun executeAsyncTask(): CompletableFuture<String?> {
        return leaderElection.runAsyncIfLeader("async-task-lock") {
            CompletableFuture.supplyAsync { performAsyncOperation() }
        }
    }
}
```

### Coroutine (`SuspendLeaderElection`)

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

### Virtual Thread (`VirtualThreadLeaderElection`)

```kotlin
val election = LocalVirtualThreadLeaderElection()

val future = election.runAsyncIfLeader("job-lock") {
    performExpensiveIO()  // Virtual Thread yields the carrier thread during I/O blocking
}

val result = future.await()
```

### Group Leader — up to N concurrent executions

```kotlin
// Synchronous — up to 3 threads concurrently
val election = LocalLeaderGroupElection(maxLeaders = 3)

val result = election.runIfLeader("batch-job") {
    processChunk()  // acquires slot → executes → releases slot automatically
}

// State query
val state = election.state("batch-job")
println("Active leaders: ${state.activeCount} / ${state.maxLeaders}")
println("Available slots: ${state.availableSlots}")
```

### Spring Boot Integration

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

## Dependency

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-leader:${version}")
}
```
