# bluetape4k-leader 빠른 참조 (API Cheat Sheet)

## 선택 가이드

### "어떤 구현체를 사용할까?"

```
단일 리더 (1개만 실행) + 동기 필요?
  → LocalLeaderElection

단일 리더 + CompletableFuture 비동기?
  → LocalLeaderElection (runAsyncIfLeader 사용)
  또는 LocalAsyncLeaderElection

단일 리더 + Virtual Thread (Java 21+)?
  → LocalVirtualThreadLeaderElection

단일 리더 + Coroutines?
  → LocalSuspendLeaderElection

최대 N개 동시 실행 + 동기?
  → LocalLeaderGroupElection(maxLeaders = N)

최대 N개 동시 실행 + Coroutines?
  → LocalSuspendLeaderGroupElection(maxLeaders = N)
```

---

## API 시그니처 (모든 구현체)

### LocalLeaderElection

```kotlin
val election = LocalLeaderElection()

// 동기 (ReentrantLock 블로킹)
val result: T = election.runIfLeader("lock-name") {
    // T를 반환하는 코드
    computeResult()
}

// 비동기 (CompletableFuture)
val future: CompletableFuture<T> = election.runAsyncIfLeader(
    "lock-name",
    executor = VirtualThreadExecutor  // 기본값
) {
    CompletableFuture.supplyAsync { computeResult() }
}
result = future.join()
```

### LocalAsyncLeaderElection (비동기만)

```kotlin
val election = LocalAsyncLeaderElection()

val future: CompletableFuture<T> = election.runAsyncIfLeader(
    "lock-name",
    executor = ForkJoinPool.commonPool()  // 커스텀 executor
) {
    CompletableFuture.supplyAsync { computeResult() }
}
```

### LocalVirtualThreadLeaderElection

```kotlin
val election = LocalVirtualThreadLeaderElection()

val vfuture: VirtualFuture<T> = election.runAsyncIfLeader("lock-name") {
    // T를 직접 반환 (CompletableFuture 래핑 불필요)
    blockingIO()
}

// await() 로 결과 대기
val result: T = vfuture.await()

// 또는 CompletableFuture로 변환
val cFuture: CompletableFuture<T> = vfuture.toCompletableFuture()
```

### LocalLeaderGroupElection

```kotlin
val election = LocalLeaderGroupElection(maxLeaders = 3)

// 최대 3개 스레드가 동시 실행
val result: T = election.runIfLeader("batch-lock") {
    processChunk()
}

// 상태 조회
val activeCount: Int = election.activeCount("batch-lock")
val availableSlots: Int = election.availableSlots("batch-lock")
val state: LeaderGroupState = election.state("batch-lock")

println("활성: ${state.activeCount}/${state.maxLeaders}, 남은 슬롯: ${state.availableSlots}")
```

### LocalSuspendLeaderElection

```kotlin
val election = LocalSuspendLeaderElection()

// suspend 함수 내에서만 호출 가능
suspend fun myJob() {
    val result: T = election.runIfLeader("suspend-lock") {
        // suspend 함수 호출 가능
        delay(100)
        computeResult()
    }
}

// 사용 예
runBlocking {
    myJob()
}

// 또는 coroutineScope
coroutineScope {
    myJob()
}
```

### LocalSuspendLeaderGroupElection

```kotlin
val election = LocalSuspendLeaderGroupElection(maxLeaders = 5)

suspend fun parallelBatch() {
    coroutineScope {
        repeat(20) { index ->
            launch {
                election.runIfLeader("batch-job") {
                    // 최대 5개 코루틴만 동시 실행
                    log.info("처리 중: $index")
                    delay(100)
                }
            }
        }
    }
}
```

---

## 예외 처리

### 동기 API

```kotlin
val election = LocalLeaderElection()

try {
    val result = election.runIfLeader("lock") {
        throw IllegalStateException("작업 실패")
    }
} catch (e: IllegalStateException) {
    // action에서 발생한 예외가 직접 전파됨
    log.error("작업 중 예외 발생", e)
}

// 예외 후에도 락은 자동으로 해제됨 (다음 호출 정상 작동)
val result = election.runIfLeader("lock") { "복구됨" }
```

### 비동기 API (CompletableFuture)

```kotlin
val election = LocalLeaderElection()

election.runAsyncIfLeader("lock") {
    CompletableFuture.failedFuture(RuntimeException("비동기 실패"))
}.exceptionally { e ->
    log.error("비동기 작업 실패", e)
    "폴백 값"
}.thenAccept { result ->
    log.info("결과: $result")
}
```

### Coroutines API

```kotlin
val election = LocalSuspendLeaderElection()

suspend fun safeJob() {
    try {
        election.runIfLeader("lock") {
            throw IllegalArgumentException("작업 실패")
        }
    } catch (e: IllegalArgumentException) {
        log.error("작업 중 예외", e)
    }
}
```

---

## 동시성 보장

### 직렬 실행 (Mutual Exclusion)

```kotlin
val election = LocalLeaderElection()

// 다음 코드는 항상 직렬로 실행됨
// Thread A: time=0~10ms, Thread B: time=10~20ms
Thread {
    election.runIfLeader("exclusive-lock") {
        Thread.sleep(10)
        println("스레드 A")
    }
}.start()

Thread {
    election.runIfLeader("exclusive-lock") {
        Thread.sleep(10)
        println("스레드 B")  // A가 끝난 후 실행
    }
}.start()
```

### 복수 동시 실행 (Semaphore)

```kotlin
val election = LocalLeaderGroupElection(maxLeaders = 2)

// 최대 2개만 동시 실행
repeat(5) { index ->
    thread {
        election.runIfLeader("bulk-lock") {
            println("작업 $index")
            Thread.sleep(1000)
        }
    }
}
// 출력: 0, 1 (병렬)
//       2, 3 (병렬)
//       4 (단독)
```

### 재진입 (Reentrancy)

```kotlin
val election = LocalLeaderElection()

// ReentrantLock 사용 → 재진입 가능
val result = election.runIfLeader("reentrant-lock") {
    val inner = election.runIfLeader("reentrant-lock") {
        "내부"
    }
    "외부: $inner"
}
// result == "외부: 내부" ✅

// Coroutines Mutex는 재진입 불가 ❌
val suspendElection = LocalSuspendLeaderElection()
suspend fun deadlock() {
    suspendElection.runIfLeader("mutex-lock") {
        // 데드락! Mutex는 재진입을 지원하지 않음
        suspendElection.runIfLeader("mutex-lock") {
            "절대 실행 안 됨"
        }
    }
}
```

---

## 상태 조회 패턴

### LeaderGroupState 활용

```kotlin
val election = LocalLeaderGroupElection(maxLeaders = 3)

// 각각 호출하면서 작업 수행
election.runIfLeader("batch") { Task 1 }

val state = election.state("batch")
println("활성 리더: ${state.activeCount}/${state.maxLeaders}")
println("남은 슬롯: ${state.availableSlots}")
println("가득 찼나? ${state.isFull}")
println("비었나? ${state.isEmpty}")

// 상태:
// LeaderGroupState(lockName=batch, maxLeaders=3, activeCount=1)
// 활성 리더: 1/3
// 남은 슬롯: 2
// 가득 찼나? false
// 비었나? false
```

---

## 의존성 주입 패턴

### Executor 커스터마이징

```kotlin
// 기본 (VirtualThreadExecutor)
val result = election.runAsyncIfLeader("task") {
    CompletableFuture.completedFuture("ok")
}.join()

// ForkJoinPool 사용
val result = election.runAsyncIfLeader(
    "task",
    executor = ForkJoinPool.commonPool()
) {
    CompletableFuture.completedFuture("ok")
}.join()

// 커스텀 ThreadPoolExecutor
val executor = ThreadPoolExecutor(4, 8, 60, TimeUnit.SECONDS, LinkedBlockingQueue())
val result = election.runAsyncIfLeader(
    "task",
    executor = executor
) {
    CompletableFuture.completedFuture("ok")
}.join()
```

---

## lockName 네이밍 규칙

### 권장 패턴

```kotlin
// ✅ Good: 명확한 목적
election.runIfLeader("daily-backup-job") { ... }
election.runIfLeader("report-generation") { ... }
election.runIfLeader("cache-refresh") { ... }

// ⚠️ Okay: 범용적
election.runIfLeader("job-1") { ... }

// ❌ Bad: 모호함
election.runIfLeader("task") { ... }
election.runIfLeader("lock") { ... }

// 💡 Tip: 계층적 네이밍
election.runIfLeader("batch/daily/backup") { ... }
election.runIfLeader("worker/job-processor") { ... }
```

---

## 성능 팁

### 1. Virtual Thread는 I/O 블로킹에 최적

```kotlin
// ✅ Good: Virtual Thread (I/O 블로킹)
val election = LocalVirtualThreadLeaderElection()
election.runAsyncIfLeader("db-query") {
    // 데이터베이스 쿼리 → carrier thread 반납
    Database.query(sql)
}.await()

// ⚠️ CPU 바운드는 기존 스레드 사용
val election = LocalLeaderElection()
election.runIfLeader("cpu-intensive") {
    // CPU 연산 → Virtual Thread 이점 없음
    expensiveComputation()
}
```

### 2. Coroutines는 많은 수의 동시 작업에 최적

```kotlin
// ✅ Good: 1000개 동시 코루틴
coroutineScope {
    repeat(1000) {
        launch {
            election.runIfLeader("task-$it") {
                networkCall()
            }
        }
    }
}

// ❌ Bad: 1000개 스레드는 리소스 낭비
repeat(1000) {
    thread {
        election.runIfLeader("task-$it") {
            networkCall()
        }
    }
}
```

### 3. LeaderGroupElection으로 동시성 제어

```kotlin
val election = LocalLeaderGroupElection(maxLeaders = 10)

// 10개씩만 실행되도록 조절
repeat(1000) { index ->
    thread {
        election.runIfLeader("parallel-batch") {
            processItem(index)
        }
    }
}
```

---

## 테스트 패턴

### 단위 테스트

```kotlin
class MyJobTest {
    private val election = LocalLeaderElection()

    @Test
    fun `리더 획득 시 작업이 실행된다`() {
        val result = election.runIfLeader("test-lock") {
            "success"
        }
        result shouldBeEqualTo "success"
    }

    @Test
    fun `예외는 전파된다`() {
        assertThrows<IllegalStateException> {
            election.runIfLeader("test-lock") {
                throw IllegalStateException()
            }
        }
    }

    @Test
    fun `락은 안전하게 해제된다`() {
        runCatching {
            election.runIfLeader("test-lock") {
                throw Exception()
            }
        }

        // 다음 호출은 정상 작동해야 함
        val result = election.runIfLeader("test-lock") {
            "ok"
        }
        result shouldBeEqualTo "ok"
    }
}
```

### 동시성 테스트

```kotlin
@Test
fun `멀티스레드에서 직렬 실행을 보장한다`() {
    val election = LocalLeaderElection()
    val results = mutableListOf<Int>()

    MultithreadingTester()
        .workers(8)
        .rounds(10)
        .add {
            election.runIfLeader("serial-lock") {
                results.add(1)
            }
        }
        .run()

    results.size shouldBeEqualTo 80  // 8 workers * 10 rounds
}
```

---

## 흔한 실수와 해결법

### 실수 1: Mutex 재진입

```kotlin
// ❌ 데드락!
val election = LocalSuspendLeaderElection()
suspend fun deadlock() {
    election.runIfLeader("lock") {
        election.runIfLeader("lock") {  // 재진입 불가 → 데드락
            "never"
        }
    }
}

// ✅ 해결: ReentrantLock 사용
val election = LocalLeaderElection()
val result = election.runIfLeader("lock") {
    election.runIfLeader("lock") {  // 재진입 가능
        "ok"
    }
}
```

### 실수 2: lockName 공유 안 함

```kotlin
// ❌ 각각 다른 스레드에서 실행 (서로 다른 lock)
election.runIfLeader("job-A") { /* Thread 1 */ }
election.runIfLeader("job-B") { /* Thread 2 */ }
// 병렬 실행됨 (의도 실패)

// ✅ 같은 lockName 사용
election.runIfLeader("critical-section") { /* Thread 1 */ }
election.runIfLeader("critical-section") { /* Thread 2 */ }
// 순차 실행 보장
```

### 실수 3: 장시간 블로킹 작업

```kotlin
// ⚠️ 주의: 다른 요청들이 대기
election.runIfLeader("slow-job") {
    Thread.sleep(10_000)  // 10초 블로킹
    // 다른 요청들은 모두 대기...
}

// ✅ 개선: 복수 리더로 동시성 높임
val election = LocalLeaderGroupElection(maxLeaders = 5)
election.runIfLeader("fast-job") {
    // 최대 5개까지 동시 실행
}
```

