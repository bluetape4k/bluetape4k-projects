# bluetape4k-leader 모듈 - 완전 분석 요약

**생성일**: 2026-03-07  
**분석 범위**: utils/leader 모듈 전체 (소스 + 테스트)  
**분석 결과**: 3개 상세 문서 생성

---

## 빠른 참조

### 모듈 개요

- **모듈명**: `bluetape4k-leader`
- **패키지**: `io.bluetape4k.leader`
- **목적**: 로컬 JVM 환경에서 리더(Leader) 선출 및 동시 실행 제어
- **파일 수**: 13개 소스 + 6개 테스트
- **라인 수**: ~670 소스 + ~800 테스트
- **외부 의존성**: 0개 (JDK + Kotlin stdlib + Coroutines만)

### 핵심 기능

- **동기 리더 선출**: ReentrantLock 기반 블로킹 API
- **비동기 리더 선출**: CompletableFuture 기반 비차단 API
- **Virtual Thread 지원**: Java 21+ 경량 스레드 기반 (I/O 최적화)
- **Coroutines 지원**: suspend 기반 비차단 API
- **복수 리더 지원**: Semaphore 기반 동시 실행 수 제한

### 미지원 사항

- ❌ 분산 환경 (ZooKeeper, Redis, Hazelcast)
- ❌ 재진입 (Coroutines Mutex 기반 구현)
- ❌ Java 20 이하 (Virtual Thread는 21+ 필수)

---

## 아키텍처 계층

### 1단계: 인터페이스 계층 (공개 API)

```
동기 (Blocking)
├── LeaderElection
│   └── runIfLeader(lockName, action) → T
└── LeaderGroupElection
    └── runIfLeader(lockName, action) → T

비동기 (CompletableFuture)
├── AsyncLeaderElection
│   └── runAsyncIfLeader(lockName, executor, action) → CompletableFuture<T>
└── (LeaderGroupElection 없음)

Virtual Thread (Java 21+)
├── VirtualThreadLeaderElection
│   └── runAsyncIfLeader(lockName, action) → VirtualFuture<T>
└── (복수 리더 없음)

Coroutines (suspend)
├── SuspendLeaderElection
│   └── runIfLeader(lockName, action) → T (suspend)
└── SuspendLeaderGroupElection
    └── runIfLeader(lockName, action) → T (suspend)
```

### 2단계: 구현체 계층 (local/)

```
local/
├── LocalLeaderElection
│   ├── ReentrantLock 기반
│   ├── 동기 + 비동기 모두 구현
│   └── 재진입 지원
├── LocalAsyncLeaderElection
│   ├── ReentrantLock 기반
│   └── 비동기만 구현
├── LocalVirtualThreadLeaderElection
│   ├── ReentrantLock + Virtual Thread
│   └── VirtualFuture 반환
└── LocalLeaderGroupElection
    ├── Semaphore 기반
    └── 최대 N개 동시 실행

coroutines/
├── LocalSuspendLeaderElection
│   ├── Mutex 기반
│   └── 재진입 미지원
└── LocalSuspendLeaderGroupElection
    ├── Semaphore 기반
    └── 최대 N개 동시 실행
```

---

## 7개 공개 인터페이스

| 인터페이스                           | 메서드                     | 기반            | 특징                                |
|---------------------------------|-------------------------|---------------|-----------------------------------|
| **LeaderElection**              | runIfLeader()           | ReentrantLock | 동기 블로킹, 재진입 가능                    |
| **AsyncLeaderElection**         | runAsyncIfLeader()      | Executor      | CompletableFuture 반환              |
| **VirtualThreadLeaderElection** | runAsyncIfLeader()      | VirtualThread | VirtualFuture 반환                  |
| **LeaderGroupElection**         | runIfLeader()           | Semaphore     | 최대 N개 동시 실행                       |
| **SuspendLeaderElection**       | runIfLeader() (suspend) | Mutex         | Coroutine suspend                 |
| **SuspendLeaderGroupElection**  | runIfLeader() (suspend) | Semaphore     | 최대 N개 동시 실행 (suspend)             |
| **LeaderGroupState**            | (데이터 클래스)               | -             | lockName, maxLeaders, activeCount |

---

## 6개 구현체

| 구현체                                  | 인터페이스                                | 기술            | 동시성 모델                 |
|--------------------------------------|--------------------------------------|---------------|------------------------|
| **LocalLeaderElection**              | LeaderElection + AsyncLeaderElection | ReentrantLock | 동기 + CompletableFuture |
| **LocalAsyncLeaderElection**         | AsyncLeaderElection                  | ReentrantLock | CompletableFuture 비동기만 |
| **LocalVirtualThreadLeaderElection** | VirtualThreadLeaderElection          | VirtualThread | Virtual Thread 경량 스레드  |
| **LocalLeaderGroupElection**         | LeaderGroupElection                  | Semaphore     | 동시 실행 수 제한 (블로킹)       |
| **LocalSuspendLeaderElection**       | SuspendLeaderElection                | Mutex         | Coroutine suspend      |
| **LocalSuspendLeaderGroupElection**  | SuspendLeaderGroupElection           | Semaphore     | Coroutine 동시 실행 수 제한   |

---

## 핵심 설계 패턴

### 1. lockName별 Lock 캐싱

```kotlin
private val locks = ConcurrentHashMap<String, ReentrantLock>()

private fun getLock(lockName: String): ReentrantLock =
    locks.computeIfAbsent(lockName) { ReentrantLock() }
```

**목적**: 동일 lockName으로 여러 번 호출해도 동일 Lock 사용 → 상호 배제 보장

### 2. try-finally 안전성

```kotlin
override fun <T> runIfLeader(lockName: String, action: () -> T): T {
    val semaphore = getSemaphore(lockName)
    semaphore.acquire()
    try {
        return action()
    } finally {
        semaphore.release()  // 예외 시에도 반드시 해제
    }
}
```

**목적**: 모든 경로에서 Lock/Semaphore 해제 보장 → 데드락 방지

### 3. 확장 함수 활용

```kotlin
// ReentrantLock
lock.withLock { action() }

// Mutex
mutex.withLock { action() }

// Semaphore
semaphore.withPermit { action() }
```

**목적**: 안전하고 간결한 자원 관리

### 4. Companion object 팩토리

```kotlin
class LocalLeaderGroupElection private constructor(override val maxLeaders: Int) {
    companion object {
        operator fun invoke(maxLeaders: Int = 2): LeaderGroupElection {
            require(maxLeaders > 0) { "..." }
            return LocalLeaderGroupElection(maxLeaders)
        }
    }
}

// 사용
val election = LocalLeaderGroupElection(maxLeaders = 3)
```

**목적**: 생성 로직 중앙화 + 입력 검증

### 5. 기본 Executor = VirtualThreadExecutor

```kotlin
fun <T> runAsyncIfLeader(
    lockName: String,
    executor: Executor = VirtualThreadExecutor,  // 기본값
    action: () -> CompletableFuture<T>,
): CompletableFuture<T>
```

**목적**: Java 21+ Virtual Thread의 경량성 활용

### 6. 계층적 인터페이스 설계

```
LeaderElection (동기)
    ↑
    extends AsyncLeaderElection (CompletableFuture)
    
VirtualThreadLeaderElection (Virtual Thread)
    ↓
    다른 메서드 시그니처

SuspendLeaderElection (Coroutines)
    ↓
    suspend 함수
```

**목적**: 각 비동기 모델에 최적화된 API

---

## 테스트 전략

### 커버리지 영역

| 테스트 영역    | 테스트 수 | 검증 항목                              |
|-----------|-------|------------------------------------|
| **정상 경로** | 6개    | 리더 획득, 작업 실행, 결과 반환                |
| **예외 처리** | 6개    | 예외 전파, 복구, 안전성                     |
| **동시성**   | 8개    | 멀티스레드 직렬화, Semaphore 제약            |
| **재진입**   | 2개    | ReentrantLock 재진입, Mutex 주의        |
| **상태 조회** | 4개    | activeCount, availableSlots, state |

### 주요 테스트 기법

1. **UUID 기반 lockName**: 테스트 간 격리
   ```kotlin
   private fun randomLockName() = "lock-${UUID.randomUUID()}"
   ```

2. **MultithreadingTester**: 동시성 검증
   ```kotlin
   MultithreadingTester()
       .workers(8)
       .rounds(10)
       .add { /* task 1 */ }
       .add { /* task 2 */ }
       .run()
   ```

3. **AtomicInteger**: 스레드 안전 카운팅
   ```kotlin
   val counter = AtomicInteger(0)
   counter.incrementAndGet()
   ```

4. **runCatching**: 예외 복구 검증
   ```kotlin
   runCatching {
       election.runIfLeader("lock") { throw Exception() }
   }
   // 다음 호출은 성공해야 함
   ```

---

## 의존성 분석

### 직접 의존성

```gradle
api(project(":bluetape4k-core"))
implementation(Libs.kotlinx_coroutines_core)
testImplementation(project(":bluetape4k-junit5"))
testImplementation(Libs.kotlinx_coroutines_test)
```

### 전이 의존성

| 라이브러리                      | 버전    | 용도                                   | 참고        |
|----------------------------|-------|--------------------------------------|-----------|
| kotlin-stdlib              | 2.3   | Kotlin API                           | Gradle 자동 |
| kotlinx-coroutines-core    | 1.9.x | suspend, Mutex                       | 명시적       |
| java.util.concurrent       | JDK   | ReentrantLock, Semaphore             | JDK 내장    |
| java.util.concurrent.locks | JDK   | ReentrantLock, ReadWriteLock         | JDK 내장    |
| bluetape4k-core            | -     | VirtualFuture, VirtualThreadExecutor | 프로젝트 내부   |
| bluetape4k-junit5          | -     | MultithreadingTester                 | 프로젝트 내부   |

### 외부 라이브러리 0개

- ZooKeeper ❌
- Redis ❌
- Hazelcast ❌
- Curator ❌

---

## 사용 시나리오별 선택

### 시나리오 1: 배치 작업 직렬화

```kotlin
// 단일 JVM, 동기 API, 단순함
val election = LocalLeaderElection()
election.runIfLeader("daily-backup") {
    performBackup()
}
```

### 시나리오 2: 비동기 웹 요청 처리

```kotlin
// CompletableFuture 기반
val election = LocalLeaderElection()
val future = election.runAsyncIfLeader("api-lock") {
    CompletableFuture.supplyAsync { callRemoteAPI() }
}
```

### 시나리오 3: I/O 집약적 작업 (Java 21+)

```kotlin
// Virtual Thread로 carrier thread 절약
val election = LocalVirtualThreadLeaderElection()
val result = election.runAsyncIfLeader("io-task") {
    blockingDatabaseQuery()
}.await()
```

### 시나리오 4: 복수 동시 작업 제어

```kotlin
// 최대 5개만 동시 실행
val election = LocalLeaderGroupElection(maxLeaders = 5)
election.runIfLeader("batch-process") {
    processChunk()
}
```

### 시나리오 5: Coroutines 기반 마이크로서비스

```kotlin
// suspend 함수, 경량 Coroutine
val election = LocalSuspendLeaderElection()
suspend fun handleRequest() {
    election.runIfLeader("request-lock") {
        suspendingWork()
    }
}
```

---

## 주의사항 요약

### ⚠️ 재진입 (Reentrancy)

| 구현체                              | 재진입   | 설명                   |
|----------------------------------|-------|----------------------|
| LocalLeaderElection              | ✅ 가능  | ReentrantLock 지원     |
| LocalVirtualThreadLeaderElection | ✅ 가능  | ReentrantLock 지원     |
| LocalSuspendLeaderElection       | ❌ 불가  | Mutex는 재진입 미지원 → 데드락 |
| LocalLeaderGroupElection         | ⚠️ 주의 | Semaphore → 슬롯 소진 가능 |

### ⚠️ 분산 환경 미지원

모든 구현체는 **단일 JVM 프로세스**에서만 작동

```kotlin
// ❌ 이렇게 하면 안 됨
val election = LocalLeaderElection()

// Instance A (JVM 1)
election.runIfLeader("job") { instanceA() }

// Instance B (JVM 2)
election.runIfLeader("job") { instanceB() }  // 동시 실행됨!
```

### ⚠️ Java 21+ 요구사항

VirtualThreadLeaderElection은 Java 21 이상 필수

```kotlin
// VirtualThreadExecutor 기본값도 Java 21+
val result = election.runAsyncIfLeader("task") {  // Error in Java 20-
    CompletableFuture.completedFuture("ok")
}
```

### ⚠️ 상태 조회 근사값

```kotlin
val activeCount = election.activeCount("job")  // TOCTOU 문제
// 이 시점 이후 activeCount가 변경될 수 있음
```

---

## 확장 포인트

### 새 구현체 추가 가능 구조

```
leader/
├── local/              (현재: 로컬 JVM)
├── zookeeper/          (가능: ZooKeeper 분산)
├── redis/              (가능: Redis 분산)
├── hazelcast/          (가능: Hazelcast 분산)
└── ...
```

### 새 인터페이스 추가 가능

```kotlin
// Reactor Mono 기반
interface ReactiveLeaderElection {
    fun <T> runIfLeader(lockName: String, action: () -> Mono<T>): Mono<T>
}

// RxJava Observable 기반
interface RxLeaderElection {
    fun <T> runIfLeader(lockName: String, action: () -> Observable<T>): Observable<T>
}
```

---

## 문서 네비게이션

이 분석은 3개 상세 문서로 구성됩니다:

### 1️⃣ `leader-module-analysis.md` (완전 기술 분석)

**내용**:

- 모듈 개요 및 소스 구조
- 7개 인터페이스 상세 정의
- 6개 구현체 패턴 분석
- 테스트 패턴 상세
- 기반 기술 (ReentrantLock, Semaphore, Mutex 등)
- 설계 패턴 8가지
- 주의사항 및 제약사항
- 10개 사용 시나리오
- 성능 특성 비교
- 확장 포인트

**대상**: 아키텍처 이해, 기술 검토, 깊은 학습

### 2️⃣ `leader-api-quick-ref.md` (API 빠른 참조)

**내용**:

- 구현체 선택 가이드
- 각 API 시그니처 + 사용 예
- 예외 처리 패턴
- 동시성 보장 예제
- 상태 조회 패턴
- Executor 커스터마이징
- lockName 네이밍 규칙
- 성능 팁
- 테스트 패턴
- 흔한 실수 8가지

**대상**: 빠른 개발, API 사용법, 트러블슈팅

### 3️⃣ `leader-file-index.md` (파일 구조 및 역할)

**내용**:

- 디렉토리 구조 및 파일 역할
- 13개 소스 파일 상세 설명
- 6개 테스트 파일 커버리지
- 의존성 그래프
- 라인 수 통계
- 패키지 구조
- 확장 포인트

**대상**: 코드 탐색, 리팩토링, 기여

---

## 빠른 체크리스트

### 모듈 이해하기

- [ ] LeaderElection vs AsyncLeaderElection vs VirtualThreadLeaderElection 차이 이해
- [ ] LeaderGroupElection (Semaphore 복수 리더) 이해
- [ ] Coroutines 기반 구현 (Mutex, Semaphore) 이해
- [ ] 재진입(reentrancy) 제약 이해
- [ ] 분산 환경 미지원 인식

### 개발하기

- [ ] 동기/비동기/Coroutines 중 선택
- [ ] 단일 리더 vs 복수 리더 선택
- [ ] lockName 네이밍 규칙 준수
- [ ] 예외 처리 (try-catch, runCatching)
- [ ] 멀티스레드 테스트 작성

### 성능 최적화

- [ ] I/O 집약적: Virtual Thread 사용
- [ ] 많은 동시 작업: Coroutines 사용
- [ ] 동시 실행 제한: LeaderGroupElection 사용
- [ ] 기본 Executor 확인: VirtualThreadExecutor (Java 21+)

---

## 핵심 통계

| 항목            | 수치                                               |
|---------------|--------------------------------------------------|
| 소스 파일         | 13개                                              |
| 테스트 파일        | 6개                                               |
| 소스 라인         | ~670                                             |
| 테스트 라인        | ~800                                             |
| 공개 인터페이스      | 7개                                               |
| 구현체           | 6개                                               |
| 외부 의존성        | 0개                                               |
| JDK 기술        | 3개 (ReentrantLock, Semaphore, ConcurrentHashMap) |
| Kotlin 기술     | 2개 (확장함수, DSL)                                   |
| Coroutines 기술 | 2개 (Mutex, Semaphore)                            |

---

## 최종 요약

**bluetape4k-leader**는:

✅ **단순함**: 복잡한 분산 기능 없이 핵심만 집중  
✅ **다양성**: 동기/비동기/Coroutines/Virtual Thread 모두 지원  
✅ **안전성**: try-finally, withLock 패턴으로 확실한 자원 관리  
✅ **확장 가능**: 새 구현체/인터페이스 추가 용이한 설계  
✅ **테스트 완비**: 정상/예외/동시성 모두 검증

❌ **분산 환경 미지원**: 단일 JVM만 (외부 락 서비스 별도 필요)  
❌ **Mutex 재진입 미지원**: Coroutines 기반 구현의 한계

---

**분석 완료** ✓  
**세부 문서**: `.omc/` 디렉토리 내 3개 마크다운 파일 참조
