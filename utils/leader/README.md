# Module bluetape4k-leader

English | [한국어](./README.ko.md)

## Overview

Provides leader election functionality to prevent duplicate execution of the same task across multiple processes or threads in a distributed environment.

Only the elected leader instance executes the task, which prevents duplicate runs in scheduled jobs, batch processing, and similar scenarios.

## Dependency

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-leader:${version}")
}
```

## Interface Hierarchy

### Single Leader Election (only 1 concurrent execution per lockName)

```
AsyncLeaderElection          VirtualThreadLeaderElection  SuspendLeaderElection
       ↑                                                   (independent coroutine tier)
LeaderElection
```

- **`AsyncLeaderElection`**: BASE — async leader election (returns `CompletableFuture`)
- **`LeaderElection`**: extends `AsyncLeaderElection` — adds synchronous `runIfLeader`
- **`VirtualThreadLeaderElection`**: independent — Virtual Thread-based async (returns `VirtualFuture`)
- **`SuspendLeaderElection`**: independent — coroutine-based (`suspend fun`)

### Group Leader Election (up to N concurrent executions per lockName, Semaphore-based)

```
LeaderGroupElectionState  (maxLeaders, activeCount, availableSlots, state)
         ↑
AsyncLeaderGroupElection   VirtualThreadLeaderGroupElection  SuspendLeaderGroupElection
         ↑                       (LeaderGroupElectionState)    (LeaderGroupElectionState)
LeaderGroupElection
```

- **`LeaderGroupElectionState`**: common state query interface
- **`AsyncLeaderGroupElection`**: BASE — async group election (returns `CompletableFuture`)
- **`LeaderGroupElection`**: extends `AsyncLeaderGroupElection` — adds synchronous `runIfLeader`
- **`VirtualThreadLeaderGroupElection`**: independent — Virtual Thread-based (returns `VirtualFuture`)
- **`SuspendLeaderGroupElection`**: independent — coroutine-based (`suspend fun`)

### Local Implementations

**Single leader:**

| Implementation | Interface | Synchronization |
|----------------|-----------|-----------------|
| `LocalLeaderElection` | `LeaderElection` | `ReentrantLock` (reentrant) |
| `LocalAsyncLeaderElection` | `AsyncLeaderElection` | `ReentrantLock` + `CompletableFuture` |
| `LocalVirtualThreadLeaderElection` | `VirtualThreadLeaderElection` | `ReentrantLock` + Virtual Thread |
| `LocalSuspendLeaderElection` | `SuspendLeaderElection` | Kotlin `Mutex` (non-reentrant) |

**Group leader:**

| Implementation | Interface | Synchronization |
|----------------|-----------|-----------------|
| `LocalLeaderGroupElection` | `LeaderGroupElection` | `java.util.concurrent.Semaphore` |
| `LocalAsyncLeaderGroupElection` | `AsyncLeaderGroupElection` | `java.util.concurrent.Semaphore` + `CompletableFuture` |
| `LocalVirtualThreadLeaderGroupElection` | `VirtualThreadLeaderGroupElection` | `java.util.concurrent.Semaphore` + Virtual Thread |
| `LocalSuspendLeaderGroupElection` | `SuspendLeaderGroupElection` | `kotlinx.coroutines.sync.Semaphore` |

## Usage Examples

### Synchronous (LeaderElection)

```kotlin
import io.bluetape4k.leader.LeaderElection

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

### Asynchronous (AsyncLeaderElection)

```kotlin
import io.bluetape4k.leader.AsyncLeaderElection
import java.util.concurrent.CompletableFuture

class MyAsyncService(private val leaderElection: AsyncLeaderElection) {

    fun executeAsyncTask(): CompletableFuture<String?> {
        return leaderElection.runAsyncIfLeader("async-task-lock") {
            CompletableFuture.supplyAsync { performAsyncOperation() }
        }
    }
}
```

### Coroutine-based (SuspendLeaderElection)

```kotlin
import io.bluetape4k.leader.coroutines.SuspendLeaderElection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

### Virtual Thread (VirtualThreadLeaderElection)

```kotlin
import io.bluetape4k.leader.local.LocalVirtualThreadLeaderElection

val election = LocalVirtualThreadLeaderElection()

val future = election.runAsyncIfLeader("job-lock") {
    performExpensiveIO()  // Virtual Thread yields the carrier thread during I/O blocking
}

val result = future.await()
```

### Group Leader Election — up to N concurrent executions

```kotlin
import io.bluetape4k.leader.local.LocalLeaderGroupElection

// Synchronous — up to 3 threads concurrently
val election = LocalLeaderGroupElection(maxLeaders = 3)

val result = election.runIfLeader("batch-job") {
    processChunk()  // acquires slot → executes → releases slot automatically
}

// State query (LeaderGroupElectionState interface)
val state = election.state("batch-job")
println("Active leaders: ${state.activeCount} / ${state.maxLeaders}")
println("Available slots: ${state.availableSlots}")
println("Full: ${state.isFull}, Empty: ${state.isEmpty}")
```

### Spring Boot Integration

```kotlin
import io.bluetape4k.leader.LeaderElection
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

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

## Use Cases

### Single Leader (LeaderElection)

1. **Scheduled jobs**: Prevent the same job from running on multiple instances simultaneously
2. **Cache refresh**: Run cache update logic on only one instance at a time
3. **Notifications**: Prevent duplicate alerts from being sent
4. **Data synchronization**: Prevent duplicate sync jobs with external systems

### Group Leader (LeaderGroupElection)

1. **Parallel batch processing**: Split large datasets into N chunks and process concurrently with concurrency control
2. **Rate limiting**: Limit concurrent outbound API calls
3. **Worker pool management**: Allow only a fixed number of workers to execute a task at the same time
4. **Resource protection**: Control concurrency for tasks that use limited resources such as DB connections
