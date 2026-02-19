# Module bluetape4k-leader

## 개요

분산 환경에서 여러 프로세스/스레드 간에 동일한 작업이 중복 실행되는 것을 방지하기 위한 리더 선출(Leader Election) 기능을 제공합니다.

Leader로 선출된 인스턴스만 작업을 수행할 수 있어, 스케줄 작업, 배치 작업 등에서 중복 실행을 방지할 수 있습니다.

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-leader:${version}")
}
```

## 주요 기능

- **LeaderElection**: 동기 방식 리더 선출
- **AsyncLeaderElection**: 비동기 방식 리더 선출 (CompletableFuture)
- **SuspendLeaderElection**: 코루틴 방식 리더 선출 (suspend 함수)

## 사용 예시

### 동기 방식 (LeaderElection)

```kotlin
import io.bluetape4k.leader.LeaderElection

class MyScheduler(private val leaderElection: LeaderElection) {

    fun executeTask() {
        // 리더로 선출된 경우에만 작업 수행
        val result = leaderElection.runIfLeader("scheduled-task-lock") {
            // 리더로 선출되면 수행할 코드
            println("I'm a leader! Performing scheduled task...")
            performExpensiveOperation()
            "Task completed"
        }

        // 리더가 아니면 null 반환
        if (result == null) {
            println("Not a leader, skipping task")
        }
    }

    private fun performExpensiveOperation(): String {
        // 시간이 오래 걸리는 작업
        Thread.sleep(1000)
        return "Success"
    }
}
```

### 비동기 방식 (AsyncLeaderElection)

```kotlin
import io.bluetape4k.leader.AsyncLeaderElection
import java.util.concurrent.CompletableFuture

class MyAsyncService(private val leaderElection: AsyncLeaderElection) {

    fun executeAsyncTask(): CompletableFuture<String?> {
        return leaderElection.runAsyncIfLeader("async-task-lock") {
            CompletableFuture.supplyAsync {
                // 비동기 작업 수행
                performAsyncOperation()
            }
        }
    }

    private fun performAsyncOperation(): String {
        return "Async task completed"
    }
}
```

### 코루틴 방식 (SuspendLeaderElection)

```kotlin
import io.bluetape4k.leader.coroutines.SuspendLeaderElection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MyCoroutineService(private val leaderElection: SuspendLeaderElection) {

    suspend fun executeSuspendTask(): String? {
        return leaderElection.runIfLeader("coroutine-task-lock") {
            // suspend 함수 내에서 작업 수행
            withContext(Dispatchers.IO) {
                performSuspendOperation()
            }
        }
    }

    private suspend fun performSuspendOperation(): String {
        // suspend 작업
        kotlinx.coroutines.delay(1000)
        return "Suspend task completed"
    }
}
```

### Spring Boot 통합 예시

```kotlin
import io.bluetape4k.leader.LeaderElection
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ScheduledTaskRunner(private val leaderElection: LeaderElection) {

    @Scheduled(fixedRate = 60000)  // 1분마다
    fun runScheduledTask() {
        leaderElection.runIfLeader("cleanup-job") {
            // 리더에서만 실행되는 정리 작업
            cleanupOldData()
        }
    }

    @Scheduled(cron = "0 0 2 * * ?")  // 매일 새벽 2시
    fun runDailyBatch() {
        val result = leaderElection.runIfLeader("daily-batch") {
            runBatchJob()
        }

        if (result == null) {
            log.info("Skipped batch job - not a leader")
        } else {
            log.info("Batch job completed: $result")
        }
    }
}
```

### Redis 기반 리더 선출

```kotlin
import io.bluetape4k.leader.LeaderElection

// Redis 기반 분산 락을 사용하는 리더 선출
class RedisLeaderElection(
    private val redisTemplate: RedisTemplate<String, String>
): LeaderElection {

    override fun <T> runIfLeader(lockName: String, action: () -> T): T? {
        val lockKey = "leader:$lockName"
        val lockValue = UUID.randomUUID().toString()

        // Redis SET NX EX로 분산 락 획득
        val acquired = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, lockValue, Duration.ofMinutes(5))

        return if (acquired == true) {
            try {
                action()
            } finally {
                // 락 해제 (LUA 스크립트로 원자적 해제)
                releaseLock(lockKey, lockValue)
            }
        } else {
            null
        }
    }

    private fun releaseLock(key: String, value: String) {
        // 원자적 락 해제 구현
    }
}
```

## 인터페이스

### LeaderElection (동기)

```kotlin
interface LeaderElection {
    /**
     * 리더로 선출되면 [action]을 수행하고, 그렇지 않다면 수행하지 않습니다.
     *
     * @param lockName lock name - lock 획득에 성공하면 leader로 승격
     * @param action leader로 승격되면 수행할 코드 블럭
     * @return 작업 결과 (리더가 아니면 null)
     */
    fun <T> runIfLeader(lockName: String, action: () -> T): T?
}
```

### AsyncLeaderElection (비동기)

```kotlin
interface AsyncLeaderElection {
    /**
     * 리더로 선출되면 [action]을 수행하고, 그렇지 않다면 수행하지 않습니다.
     *
     * @param lockName lock name
     * @param executor 비동기 작업에 수행할 Executor
     * @param action leader로 승격되면 수행할 코드 블럭
     * @return 작업 결과를 담은 CompletableFuture (리더가 아니면 null로 완료)
     */
    fun <T> runAsyncIfLeader(
        lockName: String,
        executor: Executor = ForkJoinPool.commonPool(),
        action: () -> CompletableFuture<T>,
    ): CompletableFuture<T>
}
```

### SuspendLeaderElection (코루틴)

```kotlin
interface SuspendLeaderElection {
    /**
     * 리더로 선출되면 [action]을 수행하고, 그렇지 않다면 수행하지 않습니다.
     *
     * @param lockName lock name
     * @param action leader로 승격되면 수행할 suspend 코드 블럭
     * @return 작업 결과 (리더가 아니면 null)
     */
    suspend fun <T> runIfLeader(
        lockName: String,
        action: suspend () -> T,
    ): T?
}
```

## 주요 기능 상세

| 파일                                    | 설명                 |
|---------------------------------------|--------------------|
| `LeaderElection.kt`                   | 동기 방식 리더 선출 인터페이스  |
| `AsyncLeaderElection.kt`              | 비동기 방식 리더 선출 인터페이스 |
| `coroutines/SuspendLeaderElection.kt` | 코루틴 방식 리더 선출 인터페이스 |

## 사용 시나리오

1. **스케줄 작업**: 여러 인스턴스에서 동일한 스케줄 작업이 실행되지 않도록 방지
2. **배치 작업**: 대용량 데이터 처리 작업을 하나의 인스턴스에서만 실행
3. **캐시 갱신**: 분산 캐시의 갱신 작업을 하나의 인스턴스에서만 수행
4. **알림 발송**: 중복 알림 방지
5. **데이터 동기화**: 외부 시스템과의 동기화 작업 중복 방지
