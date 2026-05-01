# Module bluetape4k-virtualthread-api

[English](./README.md) | 한국어

Virtual Thread 기능을 JDK 버전에 독립적으로 사용할 수 있도록 추상화한 API 모듈입니다.

## 개요

Java 21부터 정식 도입된 Virtual Thread는 기존 Platform Thread에 비해 훨씬 가벼운 경량 스레드입니다. 이 모듈은 JDK 21과 JDK 25의 Virtual Thread 구현체를 ServiceLoader 패턴을 통해 런타임에 자동으로 선택하여 사용할 수 있도록 지원합니다.

## 주요 기능

### 1. VirtualThreads - 런타임 선택 및 Executor 생성

현재 JVM 런타임에 맞는 Virtual Thread 구현체를 자동으로 선택하여 사용합니다.

```kotlin
import io.bluetape4k.concurrent.virtualthread.VirtualThreads

// 현재 런타임 확인
val runtimeName = VirtualThreads.runtimeName() // "jdk21" 또는 "jdk25"

// Virtual Thread Factory 생성
val factory = VirtualThreads.threadFactory(prefix = "my-vt-")

// Virtual Thread ExecutorService 생성
val executor = VirtualThreads.executorService()
executor.submit {
    println("Running on virtual thread: ${Thread.currentThread()}")
}
```

### 2. VirtualThreadRuntime - 구현체 인터페이스

JDK별 Virtual Thread 구현체가 구현해야 하는 인터페이스입니다.

```kotlin
interface VirtualThreadRuntime {
    val runtimeName: String        // 구현체 이름 (예: "jdk21")
    val priority: Int               // 우선순위 (높을수록 우선 선택)

    fun isSupported(): Boolean      // 현재 런타임에서 사용 가능한지 확인
    fun threadFactory(prefix: String): ThreadFactory
    fun executorService(): ExecutorService
}
```

### 3. StructuredTaskScopes - 구조화된 동시성 (Structured Concurrency)

Java의 StructuredTaskScope API를 추상화하여 JDK 버전에 관계없이 사용할 수 있습니다.

#### API 선택 가이드

```
모든 서브태스크가 성공해야 하는가?
  └─ Yes → failFast { }       (첫 번째 실패 시 나머지를 즉시 취소)
첫 번째 성공 결과만 필요한가?
  └─ Yes → firstSuccess { }   (첫 번째 성공 시 나머지를 취소)
부분 실패를 허용하고 성공/실패를 나누어 수집해야 하는가?
  └─ Yes → supervised { }     (모든 태스크 완료까지 기다린 후 결과를 분리)
```

#### `failFast` — 전체 성공 보장

모든 서브태스크가 성공해야 하며, 하나라도 실패하면 나머지를 즉시 취소합니다.

```kotlin
import io.bluetape4k.concurrent.virtualthread.StructuredTaskScopes

val results = StructuredTaskScopes.failFast { scope ->
    val task1 = scope.fork { fetchUserData() }
    val task2 = scope.fork { fetchOrderData() }
    val task3 = scope.fork { fetchInventoryData() }

    scope.join().throwIfFailed()
    Triple(task1.get(), task2.get(), task3.get())
}
```

#### `firstSuccess` — 첫 번째 성공 반환

가장 먼저 성공한 서브태스크 결과를 반환하고, 나머지는 취소합니다.

```kotlin
val fastestResult = StructuredTaskScopes.firstSuccess<String> { scope ->
    scope.fork { fetchFromApi1() }
    scope.fork { fetchFromApi2() }
    scope.fork { fetchFromApi3() }

    scope.join().result { error -> RuntimeException("All APIs failed", error) }
}
```

#### `supervised` — 부분 실패 허용

모든 서브태스크를 완료까지 실행하며, 성공 결과와 실패 예외를 별도로 수집합니다.

```kotlin
val (successes, errors) = StructuredTaskScopes.supervised<String, Pair<List<String>, List<Throwable>>> { scope ->
    scope.fork { fetchFromPrimaryDb() }   // 성공 가능
    scope.fork { fetchFromReplicaDb() }   // 실패 가능
    scope.fork { fetchFromCacheDb() }     // 성공 가능
    scope.join()
    scope.successfulResults() to scope.failedExceptions()
}
// successes: 정상 완료된 태스크 결과 목록
// errors: 실패한 태스크 예외 목록
println("결과 ${successes.size}개 수집, 실패 ${errors.size}건")
```

`joinUntil` 데드라인 지정 — 모든 scope 타입(`All`, `Any`, `Supervised`) 지원:

```kotlin
// Supervised scope — joinUntil로 데드라인 지정
StructuredTaskScopes.supervised<String, Unit> { scope ->
    scope.fork { longRunningTask() }
    scope.joinUntil(Instant.now().plusSeconds(5))  // 초과 시 TimeoutException
    val results = scope.successfulResults()
    val failures = scope.failedExceptions()
}

// failFast (all-scope) — 데드라인 + 실패 전파
StructuredTaskScopes.failFast { scope ->
    scope.fork { processA() }
    scope.fork { processB() }
    scope.joinUntil(Instant.now().plusSeconds(10))
        .throwIfFailed()
}

// firstSuccess (any-scope) — 가장 빠른 성공 + 데드라인
StructuredTaskScopes.firstSuccess<String> { scope ->
    scope.fork { slowTask() }
    scope.fork { fastTask() }
    scope.joinUntil(Instant.now().plusMillis(500))
        .result { RuntimeException("모든 작업 실패 또는 타임아웃", it) }
}
```

#### `getOrNull()` — 안전한 결과 접근

`StructuredSubtask.getOrNull()`은 `join()` 이전 호출 또는 실패/취소 상태에서 예외 대신 `null`을 반환합니다.

```kotlin
StructuredTaskScopes.failFast { scope ->
    val task = scope.fork { 42 }
    scope.join().throwIfFailed()
    task.getOrNull()   // 42 (SUCCESS 상태)
    // FAILED / UNAVAILABLE 상태에서는 null 반환
}
```

#### Deprecated API 마이그레이션

`all()`과 `any()`는 deprecated되었습니다. 다음과 같이 마이그레이션하세요:

| 기존 | 신규 |
|------|------|
| `StructuredTaskScopes.all(...) { }` | `StructuredTaskScopes.failFast { }` |
| `StructuredTaskScopes.any(...) { }` | `StructuredTaskScopes.firstSuccess { }` |

## ServiceLoader 메커니즘

이 API 모듈은 `java.util.ServiceLoader`를 사용하여 JDK별 구현체를 동적으로 로드합니다.

### 구현체 등록

각 JDK 구현 모듈(`jdk21`, `jdk25`)은 다음 파일들을 제공해야 합니다:

*META-INF/services/io.bluetape4k.concurrent.virtualthread.VirtualThreadRuntime*

```
io.bluetape4k.concurrent.virtualthread.jdk21.Jdk21VirtualThreadRuntime
```

*META-INF/services/io.bluetape4k.concurrent.virtualthread.StructuredTaskScopeProvider*

```
io.bluetape4k.concurrent.virtualthread.jdk21.Jdk21StructuredTaskScopeProvider
```

### 우선순위 기반 선택

- JDK 25 구현체: `priority = 25`
- JDK 21 구현체: `priority = 21`
- Platform Thread Fallback: `priority = Int.MIN_VALUE`

런타임에서 `isSupported()`가 `true`를 반환하는 구현체 중 우선순위가 가장 높은 것이 선택됩니다.

## 의존성

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-virtualthread-api")

    // 런타임에 맞는 구현체 선택
    runtimeOnly("io.github.bluetape4k:bluetape4k-virtualthread-jdk21")  // JDK 21 사용 시
    // 또는
    runtimeOnly("io.github.bluetape4k:bluetape4k-virtualthread-jdk25")  // JDK 25 사용 시
}
```

## Fallback 메커니즘

적합한 Virtual Thread 구현체가 없는 경우(예: JDK 17), 자동으로 Platform Thread 기반의 Fallback 구현체가 사용됩니다.

```kotlin
// JDK 17 환경에서 실행 시
VirtualThreads.runtimeName() // "platform-fallback"
VirtualThreads.executorService() // Executors.newCachedThreadPool() 반환
```

## 테스트

```kotlin
class VirtualThreadsTest {
    @Test
    fun `should select appropriate runtime`() {
        val runtime = VirtualThreads.runtime()
        println("Runtime: ${runtime.runtimeName}")

        runtime.isSupported() shouldBe true
    }

    @Test
    fun `should create virtual thread executor`() {
        val executor = VirtualThreads.executorService()
        val latch = CountDownLatch(10)

        repeat(10) {
            executor.submit {
                println("Task $it on ${Thread.currentThread()}")
                latch.countDown()
            }
        }

        latch.await(5, TimeUnit.SECONDS) shouldBe true
    }
}
```

## 클래스 다이어그램

```mermaid
classDiagram
    class VirtualThreadRuntime {
        <<interface>>
        +runtimeName: String
        +priority: Int
        +isSupported() Boolean
        +threadFactory(prefix) ThreadFactory
        +executorService() ExecutorService
    }

    class VirtualThreads {
        <<object>>
        +runtime() VirtualThreadRuntime
        +runtimeName() String
        +threadFactory(prefix) ThreadFactory
        +executorService() ExecutorService
    }

    class StructuredTaskScopeProvider {
        <<interface>>
        +providerName: String
        +priority: Int
        +isSupported() Boolean
        +withFailFast(name, factory, block) T
        +withFirstSuccess(name, factory, block) T
        +withSupervised(name, factory, block) R
        +withAll(name, factory, block) T
        +withAny(name, factory, block) T
    }

    class StructuredTaskScopeAll {
        <<interface>>
        +fork(task) StructuredSubtask~T~
        +join() StructuredTaskScopeAll
        +joinUntil(deadline) StructuredTaskScopeAll
        +throwIfFailed(handler) StructuredTaskScopeAll
        +close()
    }

    class StructuredTaskScopeAny~T~ {
        <<interface>>
        +fork(task) StructuredSubtask~T~
        +join() StructuredTaskScopeAny~T~
        +joinUntil(deadline) StructuredTaskScopeAny~T~
        +result(mapper) T
        +close()
    }

    class StructuredTaskScopeSupervised {
        <<interface>>
        +fork(task) StructuredSubtask~T~
        +join() StructuredTaskScopeSupervised~T~
        +joinUntil(deadline) StructuredTaskScopeSupervised~T~
        +successfulResults() List~T~
        +failedExceptions() List~Throwable~
        +close()
    }

    class StructuredTaskScopes {
        <<object>>
        +failFast(name, factory, block) T
        +firstSuccess(name, factory, block) T
        +supervised(name, factory, block) R
        +all(name, factory, block) T ~~deprecated~~
        +any(name, factory, block) T ~~deprecated~~
    }

    class Jdk21VirtualThreadRuntime {
        +runtimeName = "jdk21"
        +priority = 21
    }

    class Jdk25VirtualThreadRuntime {
        +runtimeName = "jdk25"
        +priority = 25
    }

    class PlatformThreadFallback {
        +runtimeName = "platform-fallback"
        +priority = MIN_VALUE
    }

    VirtualThreadRuntime <|-- Jdk21VirtualThreadRuntime
    VirtualThreadRuntime <|-- Jdk25VirtualThreadRuntime
    VirtualThreadRuntime <|-- PlatformThreadFallback
    VirtualThreads --> VirtualThreadRuntime : "ServiceLoader 선택"
    StructuredTaskScopes --> StructuredTaskScopeProvider : "ServiceLoader 선택"
    StructuredTaskScopeProvider --> StructuredTaskScopeSupervised : creates
    style StructuredTaskScopeSupervised fill:#EDE7F6,stroke:#B39DDB,color:#4527A0

    style VirtualThreadRuntime fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style VirtualThreads fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    style StructuredTaskScopeProvider fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style StructuredTaskScopes fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    style Jdk21VirtualThreadRuntime fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style Jdk25VirtualThreadRuntime fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style PlatformThreadFallback fill:#F5F5F5,stroke:#BDBDBD,color:#424242
```

## ServiceLoader 기반 런타임 선택 흐름

```mermaid
flowchart TD
    START["VirtualThreads.executorService()"] --> SL["ServiceLoader.load(VirtualThreadRuntime)"]
    SL --> CANDIDATES["구현체 목록 수집<br/>(classpath에 있는 모든 구현체)"]
    CANDIDATES --> FILTER["isSupported() == true 필터링"]
    FILTER --> SORT["priority 내림차순 정렬"]
    SORT --> SELECT["최우선 구현체 선택"]
    SELECT --> JDK25{"JDK 25 환경?"}
    JDK25 -->|"Yes"| USE25["Jdk25VirtualThreadRuntime<br/>(priority=25)"]
    JDK25 -->|"No, JDK 21"| USE21["Jdk21VirtualThreadRuntime<br/>(priority=21)"]
    JDK25 -->|"No, JDK 17-"| USEFALL["PlatformThreadFallback<br/>(CachedThreadPool)"]

    classDef entryStyle fill:#ECEFF1,stroke:#B0BEC5,color:#37474F,font-weight:bold
    classDef processStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef decisionStyle fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    classDef implStyle fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    classDef fallbackStyle fill:#F5F5F5,stroke:#BDBDBD,color:#424242

    class START entryStyle
    class SL,CANDIDATES,FILTER,SORT,SELECT processStyle
    class JDK25 decisionStyle
    class USE25,USE21 implStyle
    class USEFALL fallbackStyle
```

## 참고 자료

- [JEP 444: Virtual Threads (Java 21)](https://openjdk.org/jeps/444)
- [JEP 462: Structured Concurrency (Second Preview, Java 21)](https://openjdk.org/jeps/462)
- [Java ServiceLoader Documentation](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ServiceLoader.html)
