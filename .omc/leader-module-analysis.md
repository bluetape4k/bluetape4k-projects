# bluetape4k-leader 모듈 철저 분석

## 1. 모듈 개요

**패키지**: `io.bluetape4k.leader`  
**설명**: 로컬 JVM 환경에서 리더(Leader) 선출 및 동시 실행 제어를 제공하는 유틸리티 모듈  
**핵심 특징**:

- 3계층 API 제공 (동기/비동기/Coroutines)
- ReentrantLock, Semaphore 기반 구현
- Virtual Thread 지원 (Java 21+)
- 분산 환경 미지원 (단일 JVM만)

---

## 2. 소스 파일 구조

```
src/main/kotlin/io/bluetape4k/leader/
├── LeaderElection.kt                      # 동기 리더 선출 인터페이스
├── AsyncLeaderElection.kt                 # 비동기 리더 선출 인터페이스
├── VirtualThreadLeaderElection.kt         # Virtual Thread 리더 선출 인터페이스
├── LeaderGroupElection.kt                 # 복수 리더 선출 인터페이스
├── LeaderGroupState.kt                    # 리더 그룹 상태 데이터 클래스
├── local/
│   ├── LocalLeaderElection.kt             # ReentrantLock 기반 동기 구현
│   ├── LocalAsyncLeaderElection.kt        # ReentrantLock 기반 비동기 구현
│   ├── LocalVirtualThreadLeaderElection.kt # Virtual Thread + ReentrantLock 구현
│   └── LocalLeaderGroupElection.kt        # Semaphore 기반 복수 리더 구현
└── coroutines/
    ├── SuspendLeaderElection.kt           # Coroutines suspend 리더 선출 인터페이스
    ├── LocalSuspendLeaderElection.kt      # Mutex 기반 suspend 리더 선출 구현
    ├── SuspendLeaderGroupElection.kt      # Coroutines suspend 복수 리더 인터페이스
    └── LocalSuspendLeaderGroupElection.kt # Coroutines Semaphore 기반 복수 리더 구현

src/test/kotlin/io/bluetape4k/leader/
├── local/
│   ├── LocalLeaderElectionTest.kt
│   ├── LocalAsyncLeaderElectionTest.kt
│   ├── LocalVirtualThreadLeaderElectionTest.kt
│   └── LocalLeaderGroupElectionTest.kt
└── coroutines/
    ├── LocalSuspendLeaderElectionTest.kt
    └── LocalSuspendLeaderGroupElectionTest.kt
```

**총 13개 소스 파일 + 6개 테스트 파일**

---

## 3. 공개 API 설계 (Interface 계층)

### 3.1 LeaderElection (동기)

```kotlin
interface LeaderElection: AsyncLeaderElection {
    fun <T> runIfLeader(lockName: String, action: () -> T): T
}
```

**계약**:

- lockName 기준으로 리더 획득 성공 시만 action 실행
- action 예외는 호출자에게 전파
- 블로킹 API (호출 스레드가 대기)

---

### 3.2 AsyncLeaderElection (비동기)

```kotlin
interface AsyncLeaderElection {
    fun <T> runAsyncIfLeader(
        lockName: String,
        executor: Executor = VirtualThreadExecutor,
        action: () -> CompletableFuture<T>,
    ): CompletableFuture<T>
}
```

**특징**:

- CompletableFuture 기반 비동기 실행
- 기본 Executor는 VirtualThreadExecutor (Java 21+)
- Virtual Thread는 락 대기 시 carrier thread 반납

---

### 3.3 VirtualThreadLeaderElection (Virtual Thread)

```kotlin
interface VirtualThreadLeaderElection {
    fun <T> runAsyncIfLeader(
        lockName: String,
        action: () -> T,
    ): VirtualFuture<T>
}
```

**차별점**:

- action이 CompletableFuture 래핑 없이 T를 직접 반환
- 반환 타입 VirtualFuture (await() API)
- 내부적으로 Virtual Thread 사용

---

### 3.4 LeaderGroupElection (복수 리더)

```kotlin
interface LeaderGroupElection {
    val maxLeaders: Int
    
    fun activeCount(lockName: String): Int
    fun availableSlots(lockName: String): Int
    fun state(lockName: String): LeaderGroupState
    fun <T> runIfLeader(lockName: String, action: () -> T): T
}
```

**차별점**:

- Semaphore 기반으로 동시 리더 수 제한 (maxLeaders)
- 슬롯이 가득 찬 경우 호출 스레드 블로킹
- 공정한 순서 보장 (Semaphore fair=true)

---

### 3.5 LeaderGroupState (상태 데이터)

```kotlin
data class LeaderGroupState(
    val lockName: String,
    val maxLeaders: Int,
    val activeCount: Int,
) {
    val availableSlots: Int get() = maxLeaders - activeCount
    val isFull: Boolean get() = activeCount >= maxLeaders
    val isEmpty: Boolean get() = activeCount == 0
}
```

---

### 3.6 SuspendLeaderElection (Coroutines)

```kotlin
interface SuspendLeaderElection {
    suspend fun <T> runIfLeader(
        lockName: String,
        action: suspend () -> T,
    ): T
}
```

**특징**:

- Coroutines Mutex 기반 (suspend 함수)
- action이 suspend 함수 람다
- Coroutine 간 직렬 실행

---

### 3.7 SuspendLeaderGroupElection (Coroutines 복수)

```kotlin
interface SuspendLeaderGroupElection {
    val maxLeaders: Int
    
    fun activeCount(lockName: String): Int
    fun availableSlots(lockName: String): Int
    fun state(lockName: String): LeaderGroupState
    suspend fun <T> runIfLeader(lockName: String, action: suspend () -> T): T
}
```

**특징**:

- Coroutines Semaphore 기반
- suspend 함수로 Coroutine 블로킹

---

## 4. 구현체 분석

### 4.1 LocalLeaderElection (ReentrantLock 동기)

**구현 패턴**:

```kotlin
class LocalLeaderElection : LeaderElection {
    private val locks = ConcurrentHashMap<String, ReentrantLock>()
    
    override fun <T> runIfLeader(lockName: String, action: () -> T): T =
        getLock(lockName).withLock(action)
    
    override fun <T> runAsyncIfLeader(...): CompletableFuture<T> =
        CompletableFuture.supplyAsync(
            { getLock(lockName).withLock { action().join() } },
            executor
        )
}
```

**특징**:

- lockName별로 ReentrantLock 인스턴스 생성 (ConcurrentHashMap 캐싱)
- withLock {} 확장함수로 안전한 lock/unlock
- 재진입(reentrancy) 지원
- 동기/비동기 모두 구현

---

### 4.2 LocalAsyncLeaderElection (ReentrantLock 비동기만)

**구현 패턴**:

```kotlin
class LocalAsyncLeaderElection : AsyncLeaderElection {
    private val locks = ConcurrentHashMap<String, ReentrantLock>()
    
    override fun <T> runAsyncIfLeader(
        lockName: String,
        executor: Executor,
        action: () -> CompletableFuture<T>,
    ): CompletableFuture<T> =
        CompletableFuture.supplyAsync(
            { getLock(lockName).withLock { action().join() } },
            executor
        )
}
```

**특징**:

- AsyncLeaderElection만 구현 (동기 runIfLeader 없음)
- 비동기만 필요한 경우 선택

---

### 4.3 LocalVirtualThreadLeaderElection (Virtual Thread)

**구현 패턴**:

```kotlin
class LocalVirtualThreadLeaderElection : VirtualThreadLeaderElection {
    private val locks = ConcurrentHashMap<String, ReentrantLock>()
    
    override fun <T> runAsyncIfLeader(lockName: String, action: () -> T): VirtualFuture<T> =
        virtualFuture {
            getLock(lockName).withLock { action() }
        }
}
```

**특징**:

- VirtualFuture DSL로 Virtual Thread 생성
- 내부적으로 ReentrantLock 사용
- action은 T를 직접 반환 (CompletableFuture 래핑 없음)

---

### 4.4 LocalLeaderGroupElection (Semaphore 복수)

**구현 패턴**:

```kotlin
class LocalLeaderGroupElection private constructor(override val maxLeaders: Int): LeaderGroupElection {
    private val semaphores = ConcurrentHashMap<String, Semaphore>()
    
    override fun <T> runIfLeader(lockName: String, action: () -> T): T {
        val semaphore = getSemaphore(lockName)
        semaphore.acquire()
        try {
            return action()
        } finally {
            semaphore.release()
        }
    }
    
    override fun activeCount(lockName: String): Int =
        maxLeaders - getSemaphore(lockName).availablePermits()
}
```

**특징**:

- Semaphore(maxLeaders, fair=true) 사용
- acquire/release 쌍으로 슬롯 관리
- try-finally로 예외 안전성 보장
- 공정한 FIFO 순서 (fair=true)
- Companion object 팩토리로 생성 (검증 추가)

---

### 4.5 LocalSuspendLeaderElection (Mutex suspend)

**구현 패턴**:

```kotlin
class LocalSuspendLeaderElection : SuspendLeaderElection {
    private val mutexes = ConcurrentHashMap<String, Mutex>()
    
    override suspend fun <T> runIfLeader(
        lockName: String,
        action: suspend () -> T,
    ): T = getMutex(lockName).withLock { action() }
}
```

**특징**:

- Coroutines Mutex (재진입 미지원)
- Mutex.withLock {} suspend 확장함수
- 재진입 호출 시 데드락 주의

---

### 4.6 LocalSuspendLeaderGroupElection (Coroutines Semaphore)

**구현 패턴**:

```kotlin
class LocalSuspendLeaderGroupElection private constructor(override val maxLeaders: Int = 2):
    SuspendLeaderGroupElection {
    
    private val semaphores = ConcurrentHashMap<String, Semaphore>()
    
    override suspend fun <T> runIfLeader(
        lockName: String,
        action: suspend () -> T,
    ): T = getSemaphore(lockName).withPermit { action() }
    
    override fun activeCount(lockName: String): Int =
        maxLeaders - getSemaphore(lockName).availablePermits
}
```

**특징**:

- kotlinx.coroutines.sync.Semaphore
- Semaphore.withPermit {} suspend 확장함수
- availablePermits로 상태 조회

---

## 5. 테스트 패턴 분석

### 5.1 기본 테스트 (LocalLeaderElectionTest 예시)

**테스트 프레임워크**:

- JUnit 5 (@Test)
- Kluent (assertions: shouldBeEqualTo)
- MultithreadingTester (동시성 테스트)
- UUID 기반 lockName 생성

**주요 테스트 케이스**:

```kotlin
@Test
fun `runIfLeader - 리더로 선출되어 action 을 실행하고 결과를 반환한다`() {
    val result = election.runIfLeader(randomLockName()) { "hello" }
    result shouldBeEqualTo "hello"
}

@Test
fun `runIfLeader - action 예외 발생 시 예외가 호출자에게 전파된다`() {
    assertThrows<RuntimeException> {
        election.runIfLeader(randomLockName()) {
            throw RuntimeException("테스트 예외")
        }
    }
}

@Test
fun `runIfLeader - action 예외 후에도 락이 해제되어 다음 호출이 성공한다`() {
    val lockName = randomLockName()
    runCatching {
        election.runIfLeader(lockName) { throw RuntimeException("실패") }
    }
    
    val result = election.runIfLeader(lockName) { "복구 성공" }
    result shouldBeEqualTo "복구 성공"
}

@Test
fun `runIfLeader - 동일 스레드에서 동일 lockName 으로 중첩 호출(재진입)이 가능하다`() {
    val lockName = randomLockName()
    val result = election.runIfLeader(lockName) {
        election.runIfLeader(lockName) { "재진입 성공" }
    }
    result shouldBeEqualTo "재진입 성공"
}

@Test
fun `runIfLeader - 멀티스레드 동시 실행 시 직렬 처리를 보장한다`() {
    val lockName = randomLockName()
    val counter = AtomicInteger(0)
    val numThreads = 8
    val roundsPerThread = 4
    
    MultithreadingTester()
        .workers(numThreads)
        .rounds(roundsPerThread)
        .add { election.runIfLeader(lockName) { counter.incrementAndGet() } }
        .add { election.runIfLeader(lockName) { counter.incrementAndGet() } }
        .run()
    
    counter.get() shouldBeEqualTo numThreads * roundsPerThread
}

@Test
fun `runAsyncIfLeader - 리더로 선출되어 비동기 action 을 실행하고 결과를 반환한다`() {
    val result = election.runAsyncIfLeader(randomLockName()) {
        CompletableFuture.completedFuture("async-ok")
    }.join()
    result shouldBeEqualTo "async-ok"
}
```

**테스트 특징**:

- 긍정 경로 (정상 실행)
- 예외 처리 (예외 전파, 복구)
- 동시성 (멀티스레드, 중첩 호출)
- 재진입(reentrancy) 검증

---

## 6. 의존성 (build.gradle.kts)

```kotlin
dependencies {
    // VirtualFuture, VirtualThreadExecutor 사용을 위해 core 의존성 추가
    api(project(":bluetape4k-core"))

    testImplementation(project(":bluetape4k-junit5"))

    // Coroutines
    implementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}
```

**의존성**:

- `bluetape4k-core`: VirtualFuture, VirtualThreadExecutor, requireNotBlank 확장
- `bluetape4k-junit5`: JUnit 5 확장, MultithreadingTester
- `kotlinx-coroutines-core`: suspend 지원
- `kotlinx-coroutines-test`: Coroutines 테스트

**외부 라이브러리**: 없음 (JDK 내장만 사용)

---

## 7. 사용하는 기반 기술

| 기술                                             | 사용 범위                                                                                | 목적                                        |
|------------------------------------------------|--------------------------------------------------------------------------------------|-------------------------------------------|
| **ReentrantLock** (java.util.concurrent.locks) | LocalLeaderElection, LocalVirtualThreadLeaderElection                                | 스레드 간 상호 배제 + 재진입 지원                      |
| **Semaphore** (java.util.concurrent)           | LocalLeaderGroupElection                                                             | 동시 리더 수 제한 (공정한 FIFO)                     |
| **ConcurrentHashMap**                          | 모든 구현체                                                                               | lockName별 Lock/Semaphore 캐싱 (thread-safe) |
| **CompletableFuture**                          | AsyncLeaderElection, LocalAsyncLeaderElection                                        | 비동기 실행 + 합성                               |
| **Virtual Threads** (Java 21+)                 | VirtualThreadLeaderElection, LocalVirtualThreadLeaderElection, VirtualThreadExecutor | 경량 스레드 기반 리더 선출                           |
| **Mutex** (kotlinx.coroutines.sync)            | LocalSuspendLeaderElection                                                           | Coroutine 간 상호 배제                         |
| **Semaphore** (kotlinx.coroutines.sync)        | LocalSuspendLeaderGroupElection                                                      | Coroutine 동시 실행 제한                        |
| **Coroutines** (kotlinx.coroutines)            | coroutines/*                                                                         | suspend 함수 기반 비차단 API                     |

---

## 8. 핵심 설계 패턴

### 8.1 계층적 인터페이스 설계

```
LeaderElection (동기)
    ↑
    implements AsyncLeaderElection (CompletableFuture)
    
VirtualThreadLeaderElection (Virtual Thread)
    ↓
    action: () -> T (간단한 시그니처)

SuspendLeaderElection (Coroutines)
    ↓
    suspend action: suspend () -> T
```

**목적**: 각 비동기 모델(CompletableFuture, Virtual Thread, Coroutines)에 맞는 API 제공

### 8.2 lockName별 Lock 캐싱

```kotlin
private val locks = ConcurrentHashMap<String, ReentrantLock>()

private fun getLock(lockName: String): ReentrantLock {
    lockName.requireNotBlank("lockName")
    return locks.computeIfAbsent(lockName) { ReentrantLock() }
}
```

**목적**: 동일 lockName으로 여러 번 호출해도 동일 Lock 인스턴스 사용 → 상호 배제 보장

### 8.3 try-finally로 안전한 자원 해제

```kotlin
override fun <T> runIfLeader(lockName: String, action: () -> T): T {
    val semaphore = getSemaphore(lockName)
    semaphore.acquire()
    try {
        return action()
    } finally {
        semaphore.release()  // 예외 발생해도 반드시 해제
    }
}
```

**목적**: action 예외 시에도 Lock/Semaphore 해제 보장 → 데드락 방지

### 8.4 Companion object 팩토리 + 검증

```kotlin
class LocalLeaderGroupElection private constructor(override val maxLeaders: Int): LeaderGroupElection {
    companion object {
        operator fun invoke(maxLeaders: Int = 2): LeaderGroupElection {
            require(maxLeaders > 0) { "maxLeaders 는 1 이상이어야 합니다." }
            return LocalLeaderGroupElection(maxLeaders)
        }
    }
}

// 사용
val election = LocalLeaderGroupElection(maxLeaders = 3)  // invoke() 호출
```

**목적**: 생성 로직 중앙화 + 입력 검증 + Kotlin DSL 스타일

### 8.5 기본 Executor로 VirtualThreadExecutor 사용

```kotlin
override fun <T> runAsyncIfLeader(
    lockName: String,
    executor: Executor = VirtualThreadExecutor,  // 기본값
    action: () -> CompletableFuture<T>,
): CompletableFuture<T>
```

**목적**: Java 21+ Virtual Thread의 경량성 활용 → I/O 블로킹 시 carrier thread 반납

---

## 9. 주의사항 및 제약사항

### 9.1 재진입(Reentrancy)

| 구현체                              | 재진입 지원 | 이유                      |
|----------------------------------|--------|-------------------------|
| LocalLeaderElection              | ✅ 지원   | ReentrantLock           |
| LocalAsyncLeaderElection         | ✅ 지원   | ReentrantLock           |
| LocalVirtualThreadLeaderElection | ✅ 지원   | ReentrantLock           |
| LocalSuspendLeaderElection       | ❌ 미지원  | Coroutines Mutex (비재진입) |
| LocalLeaderGroupElection         | ⚠️ 주의  | Semaphore → 슬롯 소진 가능    |
| LocalSuspendLeaderGroupElection  | ⚠️ 주의  | Semaphore → 슬롯 소진 가능    |

### 9.2 분산 환경 미지원

- 모든 구현체는 **단일 JVM 프로세스** 내에서만 작동
- ZooKeeper, Redis, Hazelcast 등 분산 락 미지원
- 여러 JVM 인스턴스에서 리더 선출이 필요한 경우 다른 솔루션 필요

### 9.3 상태 조회 근사값

```kotlin
override fun activeCount(lockName: String): Int =
    maxLeaders - getSemaphore(lockName).availablePermits()
```

- Semaphore.availablePermits()는 시점의 스냅샷
- 멀티스레드 환경에서 호출 직후 변경 가능 (TOCTOU 문제)

### 9.4 Java 21+ 요구사항

- VirtualThreadLeaderElection은 Java 21+ 필수
- VirtualThreadExecutor도 Java 21+ 빌드
- 낮은 버전 JVM에서는 일반 Executor 사용

---

## 10. 사용 시나리오

### 10.1 단순 배치 작업 직렬화 (동기)

```kotlin
val election = LocalLeaderElection()
election.runIfLeader("daily-backup") {
    performBackup()  // 다른 인스턴스/스레드와 동시 실행 불가
}
```

### 10.2 비동기 작업 (CompletableFuture)

```kotlin
val election = LocalLeaderElection()
val future = election.runAsyncIfLeader("async-task") {
    CompletableFuture.supplyAsync { heavyComputation() }
}
future.thenApply { result -> processResult(result) }
```

### 10.3 Virtual Thread 기반 (Java 21+)

```kotlin
val election = LocalVirtualThreadLeaderElection()
val vfuture = election.runAsyncIfLeader("vt-task") {
    blockingIO()  // Virtual Thread가 처리 → carrier thread 반납
}
val result = vfuture.await()
```

### 10.4 복수 동시 작업 제어

```kotlin
val election = LocalLeaderGroupElection(maxLeaders = 3)
// 최대 3개 스레드만 동시 실행
election.runIfLeader("batch-process") {
    processChunk()
}
```

### 10.5 Coroutines 기반

```kotlin
val election = LocalSuspendLeaderElection()
coroutineScope {
    repeat(10) {
        launch {
            election.runIfLeader("coro-task") {
                suspendingWork()
            }
        }
    }
}
```

---

## 11. 성능 특성

| 구현체                              | 오버헤드                                   | 적합 워크로드                  |
|----------------------------------|----------------------------------------|--------------------------|
| LocalLeaderElection              | 낮음 (ReentrantLock)                     | 일반적인 배치, 스레드 기반 환경       |
| LocalVirtualThreadLeaderElection | 매우 낮음 (Virtual Thread + ReentrantLock) | I/O 블로킹 많은 작업 (Java 21+) |
| LocalLeaderGroupElection         | 낮음 (Semaphore)                         | 동시 작업 수 제한 필요            |
| LocalSuspendLeaderElection       | 매우 낮음 (Coroutine Mutex)                | Coroutine 기반 시스템         |
| LocalSuspendLeaderGroupElection  | 낮음 (Coroutine Semaphore)               | Coroutine 동시 작업 제어       |

---

## 12. 확장 포인트

현재 구현체는 모두 `local/` 패키지에 위치하며, 다음과 같이 확장 가능:

### 12.1 새 구현체 추가 경로

```
leader/distributed/
├── ZooKeeperLeaderElection.kt
├── RedisLeaderElection.kt
├── HazelcastLeaderElection.kt
└── ...
```

### 12.2 새 인터페이스 추가

```kotlin
interface ReactiveLeaderElection {
    fun <T> runIfLeader(lockName: String, action: () -> Mono<T>): Mono<T>
}
```

---

## 13. 요약 테이블

| 측면            | 설명                                                             |
|---------------|----------------------------------------------------------------|
| **모듈명**       | bluetape4k-leader (utils/leader)                               |
| **의존성**       | bluetape4k-core, kotlinx-coroutines-core                       |
| **인터페이스 수**   | 7개 (3개 단일 리더, 2개 복수 리더, 2개 Coroutines)                         |
| **구현체 수**     | 6개 (모두 local/)                                                 |
| **테스트 파일**    | 6개 (2개 배치, 4개 동시성)                                             |
| **외부 라이브러리**  | 0개 (JDK + Kotlin stdlib만)                                      |
| **지원 동시성 모델** | Threads (ReentrantLock/Semaphore), Virtual Threads, Coroutines |
| **분산 환경 지원**  | ❌ 단일 JVM만                                                      |
| **Java 버전**   | Java 21+ (Virtual Thread 위해)                                   |
