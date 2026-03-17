# bluetape4k-leader 파일 인덱스 및 역할

## 디렉토리 구조

```
utils/leader/
├── build.gradle.kts                                    # Gradle 빌드 설정
├── README.md                                           # 모듈 설명 (미포함 - 검증 필요)
└── src/
    ├── main/kotlin/io/bluetape4k/leader/
    │   ├── LeaderElection.kt                          # [인터페이스] 동기 리더 선출
    │   ├── AsyncLeaderElection.kt                     # [인터페이스] 비동기 리더 선출 (CompletableFuture)
    │   ├── VirtualThreadLeaderElection.kt             # [인터페이스] Virtual Thread 리더 선출
    │   ├── LeaderGroupElection.kt                     # [인터페이스] 복수 리더 선출 (Semaphore)
    │   ├── LeaderGroupState.kt                        # [데이터] 리더 그룹 상태
    │   ├── local/                                     # 로컬 JVM 구현체
    │   │   ├── LocalLeaderElection.kt                 # [구현] ReentrantLock 기반 동기 + 비동기
    │   │   ├── LocalAsyncLeaderElection.kt            # [구현] ReentrantLock 기반 비동기만
    │   │   ├── LocalVirtualThreadLeaderElection.kt    # [구현] Virtual Thread + ReentrantLock
    │   │   └── LocalLeaderGroupElection.kt            # [구현] Semaphore 기반 복수 리더
    │   └── coroutines/                                # Coroutines suspend 구현체
    │       ├── SuspendLeaderElection.kt               # [인터페이스] Coroutines suspend 리더 선출
    │       ├── LocalSuspendLeaderElection.kt          # [구현] Mutex 기반 suspend 리더 선출
    │       ├── SuspendLeaderGroupElection.kt          # [인터페이스] Coroutines suspend 복수 리더
    │       └── LocalSuspendLeaderGroupElection.kt     # [구현] Coroutines Semaphore 기반 복수 리더
    └── test/kotlin/io/bluetape4k/leader/
        ├── local/
        │   ├── LocalLeaderElectionTest.kt             # 동기 + 비동기 테스트
        │   ├── LocalAsyncLeaderElectionTest.kt        # 비동기전용 테스트
        │   ├── LocalVirtualThreadLeaderElectionTest.kt # Virtual Thread 테스트
        │   └── LocalLeaderGroupElectionTest.kt        # 복수 리더 테스트
        └── coroutines/
            ├── LocalSuspendLeaderElectionTest.kt      # suspend 리더 테스트
            └── LocalSuspendLeaderGroupElectionTest.kt # suspend 복수 리더 테스트
```

---

## 파일별 상세 역할

### 루트 파일

#### build.gradle.kts

**용도**: Gradle 빌드 설정  
**핵심 의존성**:

- `api(project(":bluetape4k-core"))` - VirtualFuture, VirtualThreadExecutor
- `implementation(Libs.kotlinx_coroutines_core)` - Coroutines

**특수 설정**:

```kotlin
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}
```

testImplementation이 compileOnly 의존성에도 접근 가능하게 함

---

### 인터페이스 계층 (src/main/kotlin)

#### LeaderElection.kt

**역할**: 동기 리더 선출 계약  
**상속**: AsyncLeaderElection  
**주요 메서드**:

```kotlin
fun <T> runIfLeader(lockName: String, action: () -> T): T
```

**특징**:

- AsyncLeaderElection을 extends (동기 메서드 추가)
- 블로킹 API
- 호출 스레드가 직접 대기

**구현체**: LocalLeaderElection

---

#### AsyncLeaderElection.kt

**역할**: CompletableFuture 기반 비동기 리더 선출 계약  
**주요 메서드**:

```kotlin
fun <T> runAsyncIfLeader(
    lockName: String,
    executor: Executor = VirtualThreadExecutor,
    action: () -> CompletableFuture<T>,
): CompletableFuture<T>
```

**특징**:

- 기본 Executor는 VirtualThreadExecutor (Java 21+)
- action은 CompletableFuture를 반환해야 함

**구현체**:

- LocalLeaderElection (동기 + 비동기)
- LocalAsyncLeaderElection (비동기만)

---

#### VirtualThreadLeaderElection.kt

**역할**: Virtual Thread 기반 리더 선출 계약  
**주요 메서드**:

```kotlin
fun <T> runAsyncIfLeader(
    lockName: String,
    action: () -> T,
): VirtualFuture<T>
```

**특징**:

- AsyncLeaderElection과 다른 메서드 시그니처
- action이 CompletableFuture 없이 T를 직접 반환
- 반환 타입 VirtualFuture (await() API)

**구현체**: LocalVirtualThreadLeaderElection

**사용 조건**: Java 21+

---

#### LeaderGroupElection.kt

**역할**: Semaphore 기반 복수 리더 선출 계약  
**주요 메서드**:

```kotlin
val maxLeaders: Int
fun activeCount(lockName: String): Int
fun availableSlots(lockName: String): Int
fun state(lockName: String): LeaderGroupState
fun <T> runIfLeader(lockName: String, action: () -> T): T
```

**특징**:

- 최대 maxLeaders개 동시 실행 허용
- 슬롯이 가득 찬 경우 호출 스레드 블로킹
- 상태 조회 메서드 제공

**구현체**: LocalLeaderGroupElection

---

#### LeaderGroupState.kt

**역할**: 리더 그룹의 불변 상태 데이터  
**구조**:

```kotlin
data class LeaderGroupState(
    val lockName: String,
    val maxLeaders: Int,
    val activeCount: Int,
) {
    val availableSlots: Int
    val isFull: Boolean
    val isEmpty: Boolean
}
```

**용도**: LeaderGroupElection.state() 반환값

---

#### SuspendLeaderElection.kt

**역할**: Coroutines suspend 리더 선출 계약  
**주요 메서드**:

```kotlin
suspend fun <T> runIfLeader(
    lockName: String,
    action: suspend () -> T,
): T
```

**특징**:

- Coroutines Mutex 기반
- action은 suspend 함수
- Coroutine이 suspend될 수 있음 (블로킹 없음)

**구현체**: LocalSuspendLeaderElection

---

#### SuspendLeaderGroupElection.kt

**역할**: Coroutines suspend 복수 리더 선출 계약  
**주요 메서드**:

```kotlin
suspend fun <T> runIfLeader(
    lockName: String,
    action: suspend () -> T,
): T
```

**특징**:

- Coroutines Semaphore 기반
- 최대 maxLeaders개 동시 실행
- Coroutine suspend

**구현체**: LocalSuspendLeaderGroupElection

---

### 구현체 계층 (local/)

#### LocalLeaderElection.kt

**역할**: ReentrantLock 기반 로컬 동기 + 비도기 리더 선출  
**구현 인터페이스**: LeaderElection (AsyncLeaderElection도 상속)  
**내부 자료구조**:

```kotlin
private val locks = ConcurrentHashMap<String, ReentrantLock>()
```

**핵심 패턴**:

- lockName별 ReentrantLock 캐싱
- withLock {} 확장함수로 안전한 lock/unlock
- 동기: action 직접 실행
- 비동기: CompletableFuture.supplyAsync로 executor에서 실행

**특징**:

- 재진입(reentrancy) 지원
- 동기 + 비동기 모두 구현
- 블로킹 API

**테스트**: LocalLeaderElectionTest.kt (8개 테스트)

---

#### LocalAsyncLeaderElection.kt

**역할**: ReentrantLock 기반 비동기만 제공  
**구현 인터페이스**: AsyncLeaderElection  
**구현**:

```kotlin
override fun <T> runAsyncIfLeader(
    lockName: String,
    executor: Executor,
    action: () -> CompletableFuture<T>,
): CompletableFuture<T> =
    CompletableFuture.supplyAsync(
        { getLock(lockName).withLock { action().join() } },
        executor
    )
```

**특징**:

- 비동기 API만 제공 (동기 runIfLeader 없음)
- 비동기만 필요한 경우 선택

**테스트**: LocalAsyncLeaderElectionTest.kt

---

#### LocalVirtualThreadLeaderElection.kt

**역할**: Virtual Thread 기반 로컬 리더 선출  
**구현 인터페이스**: VirtualThreadLeaderElection  
**내부 자료구조**:

```kotlin
private val locks = ConcurrentHashMap<String, ReentrantLock>()
```

**핵심 구현**:

```kotlin
override fun <T> runAsyncIfLeader(lockName: String, action: () -> T): VirtualFuture<T> =
    virtualFuture {
        getLock(lockName).withLock { action() }
    }
```

**특징**:

- VirtualFuture DSL로 Virtual Thread 생성
- 내부 ReentrantLock으로 상호 배제
- action이 T를 직접 반환 (CompletableFuture 래핑 없음)

**테스트**: LocalVirtualThreadLeaderElectionTest.kt

**사용 조건**: Java 21+

---

#### LocalLeaderGroupElection.kt

**역할**: Semaphore 기반 로컬 복수 리더 선출  
**구현 인터페이스**: LeaderGroupElection  
**내부 자료구조**:

```kotlin
private val semaphores = ConcurrentHashMap<String, Semaphore>()
// Semaphore(maxLeaders, fair=true) 생성
```

**핵심 구현**:

```kotlin
override fun <T> runIfLeader(lockName: String, action: () -> T): T {
    val semaphore = getSemaphore(lockName)
    semaphore.acquire()
    try {
        return action()
    } finally {
        semaphore.release()
    }
}
```

**특징**:

- Semaphore로 동시 실행 수 제한
- acquire/release 쌍으로 안전한 슬롯 관리
- try-finally로 예외 안전성 보장
- fair=true로 공정한 FIFO 순서

**생성 방식**:

```kotlin
// Companion object 팩토리 + 검증
val election = LocalLeaderGroupElection(maxLeaders = 3)
```

**테스트**: LocalLeaderGroupElectionTest.kt

---

### Coroutines 구현체 (coroutines/)

#### LocalSuspendLeaderElection.kt

**역할**: Coroutines Mutex 기반 suspend 리더 선출  
**구현 인터페이스**: SuspendLeaderElection  
**내부 자료구조**:

```kotlin
private val mutexes = ConcurrentHashMap<String, Mutex>()
```

**핵심 구현**:

```kotlin
override suspend fun <T> runIfLeader(
    lockName: String,
    action: suspend () -> T,
): T = getMutex(lockName).withLock { action() }
```

**특징**:

- Coroutines Mutex (kotlinx.coroutines.sync.Mutex)
- Coroutine이 suspend될 수 있음 (블로킹 아님)
- 재진입 미지원 (주의: 중첩 호출 시 데드락)

**주의사항**:

```kotlin
// ❌ 데드락!
suspendElection.runIfLeader("lock") {
    suspendElection.runIfLeader("lock") {  // Mutex는 재진입 불가
        "never"
    }
}
```

**테스트**: LocalSuspendLeaderElectionTest.kt

---

#### LocalSuspendLeaderGroupElection.kt

**역할**: Coroutines Semaphore 기반 suspend 복수 리더 선출  
**구현 인터페이스**: SuspendLeaderGroupElection  
**내부 자료구조**:

```kotlin
private val semaphores = ConcurrentHashMap<String, Semaphore>()
// kotlinx.coroutines.sync.Semaphore(maxLeaders) 생성
```

**핵심 구현**:

```kotlin
override suspend fun <T> runIfLeader(
    lockName: String,
    action: suspend () -> T,
): T = getSemaphore(lockName).withPermit { action() }
```

**특징**:

- Coroutines Semaphore (kotlinx.coroutines.sync.Semaphore)
- suspend으로 Coroutine 대기
- 복수 동시 실행 제어

**생성 방식**:

```kotlin
// Companion object 팩토리 + 검증
val election = LocalSuspendLeaderGroupElection(maxLeaders = 5)
```

**테스트**: LocalSuspendLeaderGroupElectionTest.kt

---

## 테스트 파일 분석

### local/ 테스트

#### LocalLeaderElectionTest.kt (8개 테스트)

**커버리지**:

1. ✅ 정상 실행 (runIfLeader)
2. ✅ 독립적 lockName
3. ❌ 예외 전파
4. ❌ 예외 후 복구
5. ✅ 재진입 가능
6. ✅ 멀티스레드 직렬 처리
7. ✅ 비동기 정상 실행 (runAsyncIfLeader)
8. ❌ 비동기 예외 전파
9. ❌ 비동기 예외 후 복구
10. ✅ 비동기 멀티스레드 직렬 처리

**핵심 테스트 유틸**:

- `randomLockName()` - UUID 기반 고유 lockName
- `MultithreadingTester` - 동시성 테스트
- `AtomicInteger` - 스레드 안전 카운팅

---

#### LocalAsyncLeaderElectionTest.kt

**커버리지**:

- 비동기 메서드만 테스트
- CompletableFuture 조작

---

#### LocalVirtualThreadLeaderElectionTest.kt

**커버리지**:

- VirtualFuture 반환 검증
- `.await()` / `.toCompletableFuture()` 테스트

---

#### LocalLeaderGroupElectionTest.kt

**커버리지**:

- maxLeaders 제약 검증
- activeCount, availableSlots, state 조회
- Semaphore 슬롯 반환
- 멀티스레드 동시 실행

---

### coroutines/ 테스트

#### LocalSuspendLeaderElectionTest.kt

**커버리지**:

- suspend 함수 실행
- 코루틴 기반 동시성
- Mutex 잠금

---

#### LocalSuspendLeaderGroupElectionTest.kt

**커버리지**:

- suspend 복수 리더
- Coroutine Semaphore 동시 실행 제한

---

## 의존성 그래프

```
LeaderElection (인터페이스)
    ↑
    implements
    ↓
LocalLeaderElection ←extends→ AsyncLeaderElection
                                   ↑
                                   implements
                                   ↓
                          LocalAsyncLeaderElection

VirtualThreadLeaderElection (인터페이스)
    ↑
    implements
    ↓
LocalVirtualThreadLeaderElection

LeaderGroupElection (인터페이스)
    ↑
    implements
    ↓
LocalLeaderGroupElection

SuspendLeaderElection (인터페이스)
    ↑
    implements
    ↓
LocalSuspendLeaderElection

SuspendLeaderGroupElection (인터페이스)
    ↑
    implements
    ↓
LocalSuspendLeaderGroupElection

LeaderGroupState (데이터)
    ↓
    used by
    ↓
LeaderGroupElection, SuspendLeaderGroupElection
```

---

## 코드 라인 수 (추정)

| 파일                                  | 라인       | 유형      |
|-------------------------------------|----------|---------|
| LeaderElection.kt                   | ~25      | 인터페이스   |
| AsyncLeaderElection.kt              | ~50      | 인터페이스   |
| VirtualThreadLeaderElection.kt      | ~35      | 인터페이스   |
| LeaderGroupElection.kt              | ~60      | 인터페이스   |
| LeaderGroupState.kt                 | ~30      | 데이터     |
| SuspendLeaderElection.kt            | ~30      | 인터페이스   |
| SuspendLeaderGroupElection.kt       | ~55      | 인터페이스   |
| LocalLeaderElection.kt              | ~60      | 구현      |
| LocalAsyncLeaderElection.kt         | ~50      | 구현      |
| LocalVirtualThreadLeaderElection.kt | ~40      | 구현      |
| LocalLeaderGroupElection.kt         | ~100     | 구현      |
| LocalSuspendLeaderElection.kt       | ~45      | 구현      |
| LocalSuspendLeaderGroupElection.kt  | ~90      | 구현      |
| **합계**                              | **~670** | **소스**  |
| **테스트**                             | **~800** | **테스트** |

---

## 패키지 구조

```
io.bluetape4k.leader/
├── (루트 패키지)
│   ├── LeaderElection
│   ├── AsyncLeaderElection
│   ├── VirtualThreadLeaderElection
│   ├── LeaderGroupElection
│   └── LeaderGroupState
├── local/
│   ├── LocalLeaderElection
│   ├── LocalAsyncLeaderElection
│   ├── LocalVirtualThreadLeaderElection
│   └── LocalLeaderGroupElection
└── coroutines/
    ├── SuspendLeaderElection
    ├── LocalSuspendLeaderElection
    ├── SuspendLeaderGroupElection
    └── LocalSuspendLeaderGroupElection
```

**설계 철학**:

- 루트: 공개 인터페이스 + 데이터 클래스
- local/: 로컬 JVM 구현체 (현재 유일한 구현)
- coroutines/: Coroutines 기반 인터페이스 + 구현

---

## 확장 포인트

### 새 구현체 추가 가능 패키지

```
leader/zookeeper/
├── ZooKeeperLeaderElection.kt
├── ZooKeeperAsyncLeaderElection.kt
└── ZooKeeperLeaderGroupElection.kt

leader/redis/
├── RedisLeaderElection.kt
└── RedisLeaderGroupElection.kt

leader/hazelcast/
├── HazelcastLeaderElection.kt
└── HazelcastLeaderGroupElection.kt
```

### 새 인터페이스 추가 가능

```kotlin
// Reactor Mono 기반
interface ReactiveLeaderElection {
    fun <T> runIfLeader(lockName: String, action: () -> Mono<T>): Mono<T>
}

// RxJava 기반
interface RxLeaderElection {
    fun <T> runIfLeader(lockName: String, action: () -> Observable<T>): Observable<T>
}
```
