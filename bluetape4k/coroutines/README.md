# Module bluetape4k-coroutines

Kotlin Coroutines를 더 효율적이고 안전하게 사용하기 위한 유틸리티 라이브러리입니다.

## 주요 기능

- **DeferredValue**: Suspend 함수 기반 지연 계산
- **Deferred Extensions**: map, flatMap, zip 등 함수형 연산자
- **Flow Extensions**: chunked, windowed, async 등 고급 Flow 연산
- **AsyncFlow**: 비동기 매핑을 순서 보장하며 처리
- **CoroutineScope 관리**: 다양한 CoroutineScope 구현체
- **Reactor 통합**: Project Reactor와의 상호 변환
- **CompletableFuture 통합**: Java Future와 Coroutines 연동

## 의존성 추가

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-coroutines:${version}")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.8.0")
}
```

## 주요 기능 상세

### 1. DeferredValue - Suspend Lazy 계산

일반 `lazy`는 blocking이지만, `DeferredValue`는 suspend 함수로 값을 계산합니다.

```kotlin
import io.bluetape4k.coroutines.DeferredValue
import kotlinx.coroutines.delay

val lazyValue = DeferredValue {
    delay(100)  // Suspend function
    System.currentTimeMillis()
}

// 사용 시에만 계산
val value = lazyValue.value  // suspend로 접근
println(value)

// 재사용 시 재계산 안 함
val value2 = lazyValue.value  // 동일한 값 반환
```

**일반 lazy와의 차이:**

```kotlin
// ❌ 일반 lazy: suspend 함수 사용 불가
val lazy1 = lazy {
    // delay(100)  // 컴파일 에러!
    System.currentTimeMillis()
}

// ✅ DeferredValue: suspend 함수 사용 가능
val lazy2 = DeferredValue {
    delay(100)  // OK!
    System.currentTimeMillis()
}
```

**map, flatMap 지원:**

```kotlin
val x = DeferredValue {
    delay(100)
    42
}

val doubled = x.map { it * 2 }
println(doubled.value)  // 84

val nested = DeferredValue {
    delay(100)
    DeferredValue {
        delay(50)
        42
    }
}

val flattened = nested.flatMap { it }
println(flattened.value)  // 42
```

### 2. Deferred Extensions - 함수형 연산

`Deferred<T>`에 함수형 프로그래밍 연산자를 추가합니다.

#### zip - 여러 Deferred 결합

```kotlin
import io.bluetape4k.coroutines.zip
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

suspend fun example() = coroutineScope {
    val user = async {
        delay(10)
        "John"
    }

    val age = async {
        delay(20)
        30
    }

    // 두 결과를 결합
    val result = zip(user, age) { u, a ->
        "$u is $a years old"
    }

    println(result.await())  // "John is 30 years old"
}
```

#### zipWith - Deferred 연결

```kotlin
import io.bluetape4k.coroutines.zipWith

val user = async { delay(10); "John" }
val age = async { delay(20); 30 }

val result = user.zipWith(age) { u, a -> "$u-$a" }
println(result.await())  // "John-30"
```

#### map - Deferred 변환

```kotlin
import io.bluetape4k.coroutines.map

val deferred = async {
    delay(10)
    "Hello"
}

val mapped = deferred.map { it.uppercase() }
println(mapped.await())  // "HELLO"
```

#### flatMap - 중첩된 Deferred 평탄화

```kotlin
import io.bluetape4k.coroutines.flatMap

val nested = async {
    listOf(1, 2, 3)
}

val list = nested.flatMap { numbers ->
    numbers.map { it * 2 }
}

println(list.await())  // [2, 4, 6]
```

### 3. Flow Extensions

#### chunked, windowed - 배치 처리

```kotlin
import io.bluetape4k.coroutines.flow.chunked
import io.bluetape4k.coroutines.flow.windowed
import kotlinx.coroutines.flow.asFlow

// Chunked: 고정 크기 배치
(1..20).asFlow()
    .chunked(5)
    .collect { chunk ->
        println(chunk)  // [1,2,3,4,5], [6,7,8,9,10], ...
    }

// Windowed: 슬라이딩 윈도우
(1..10).asFlow()
    .windowed(size = 3, step = 1)
    .collect { window ->
        println(window)  // [1,2,3], [2,3,4], [3,4,5], ...
    }
```

**사용 사례:**

- 배치 처리 (DB bulk insert)
- 이동 평균 계산
- 스트림 데이터 분석

#### AsyncFlow - 비동기 매핑 (순서 보장)

`flatMapMerge`는 완료 순서대로 반환하지만, `AsyncFlow`는 원본 순서를 보장합니다.

```kotlin
import io.bluetape4k.coroutines.flow.async
import kotlinx.coroutines.delay
import kotlin.random.Random

(1..20).asFlow()
    .async(Dispatchers.Default) { item ->
        delay(Random.nextLong(10))  // 랜덤 지연
        item * 2
    }
    .map { it + 1 }
    .collect { result ->
        // 1, 2, 3, 4, ... 순서대로 출력됨 (완료 시간과 무관)
        println(result)
    }
```

**flatMapMerge vs AsyncFlow:**

```kotlin
// flatMapMerge: 완료 순서대로 (순서 보장 안 됨)
(1..5).asFlow()
    .flatMapMerge { item ->
        flow {
            delay(Random.nextLong(100))
            emit(item)
        }
    }
    .collect { println(it) }  // 3, 1, 5, 2, 4 (랜덤 순서)

// AsyncFlow: 원본 순서 보장
(1..5).asFlow()
    .async { item ->
        delay(Random.nextLong(100))
        item
    }
    .collect { println(it) }  // 1, 2, 3, 4, 5 (항상 순서 유지)
```

#### buffering Extensions

```kotlin
import io.bluetape4k.coroutines.flow.bufferedUnchanged
import io.bluetape4k.coroutines.flow.bufferedSliding

// 값이 변경될 때만 emit
flowOf(1, 1, 2, 2, 2, 3, 3, 1)
    .bufferedUnchanged()
    .collect { println(it) }  // 1, 2, 3, 1

// 슬라이딩 버퍼
(1..10).asFlow()
    .bufferedSliding(size = 3)
    .collect { window ->
        println(window)  // [1,2,3], [2,3,4], ...
    }
```

### 4. CoroutineScope 관리

다양한 용도의 CoroutineScope 구현체를 제공합니다.

#### DefaultCoroutineScope

```kotlin
import io.bluetape4k.coroutines.DefaultCoroutineScope

val scope = DefaultCoroutineScope()

scope.launch {
    // Coroutine 실행
}

// 종료 시
scope.cancel()
```

#### IoCoroutineScope - I/O 작업용

```kotlin
import io.bluetape4k.coroutines.IoCoroutineScope

val ioScope = IoCoroutineScope()

ioScope.launch {
    // I/O 작업 (파일, 네트워크)
    readFile()
}
```

#### VirtualThreadCoroutineScope - Virtual Thread (Java 21+)

```kotlin
import io.bluetape4k.coroutines.VirtualThreadCoroutineScope

val vtScope = VirtualThreadCoroutineScope()

vtScope.launch {
    // Virtual Thread로 실행
    blockingOperation()
}
```

#### ThreadPoolCoroutineScope - 커스텀 스레드 풀

```kotlin
import io.bluetape4k.coroutines.ThreadPoolCoroutineScope

val poolScope = ThreadPoolCoroutineScope(
    threadPoolSize = 10,
    name = "my-pool"
)

poolScope.launch {
    // 커스텀 스레드 풀에서 실행
}
```

### 5. Reactor 통합

Project Reactor의 `Mono`, `Flux`와 Coroutines 간 변환을 지원합니다.

```kotlin
import io.bluetape4k.coroutines.reactor.asFlow
import io.bluetape4k.coroutines.reactor.asMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

// Mono -> suspend
val mono = Mono.just("Hello")
val result = mono.awaitSingle()  // "Hello"

// suspend -> Mono
suspend fun getUser(): User = ...
val userMono = ::getUser.asMono()

// Flux -> Flow
val flux = Flux.range(1, 10)
val flow = flux.asFlow()

flow.collect { println(it) }

// Flow -> Flux
val flow = flowOf(1, 2, 3)
val flux = flow.asFlux()
```

### 6. CompletableFuture 통합

Java의 `CompletableFuture`와 Coroutines 간 변환을 지원합니다.

```kotlin
import io.bluetape4k.coroutines.support.await
import io.bluetape4k.coroutines.support.asFuture
import java.util.concurrent.CompletableFuture

// CompletableFuture -> suspend
val future = CompletableFuture.supplyAsync {
    Thread.sleep(100)
    "Result"
}
val result = future.await()

// suspend -> CompletableFuture
suspend fun compute(): String {
    delay(100)
    return "Result"
}

val future = ::compute.asFuture()
```

### 7. Exception 처리

Coroutines에서 예외를 안전하게 처리합니다.

```kotlin
import io.bluetape4k.coroutines.support.coExecute
import io.bluetape4k.coroutines.support.coTry

// Try-Catch를 Result로
val result = coTry {
    riskyOperation()
}

result.onSuccess { value ->
    println("Success: $value")
}.onFailure { error ->
    println("Failed: $error")
}

// Silent execution (예외 무시)
coExecute {
    mayFailOperation()
}
```

### 8. Logging 지원

Coroutines 환경에서 로깅을 쉽게 사용할 수 있습니다.

```kotlin
import io.bluetape4k.coroutines.slf4j.withLoggingContext

suspend fun processRequest(requestId: String) {
    withLoggingContext("requestId" to requestId) {
        log.info { "Processing request" }
        // MDC에 requestId 자동 추가
        doWork()
    }
}
```

## 전체 예시

```kotlin
import io.bluetape4k.coroutines.*
import io.bluetape4k.coroutines.flow.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class DataProcessor {
    private val scope = IoCoroutineScope()

    suspend fun processBatch(items: List<Item>): List<Result> = coroutineScope {
        // AsyncFlow로 병렬 처리 (순서 보장)
        items.asFlow()
            .async(Dispatchers.IO) { item ->
                processItem(item)
            }
            .collect()
    }

    suspend fun processStream(): Unit = coroutineScope {
        // 스트림 데이터를 배치로 처리
        streamFlow()
            .chunked(100)  // 100개씩 배치
            .collect { batch ->
                saveBatch(batch)
            }
    }

    suspend fun aggregateResults(): Report = coroutineScope {
        // 여러 비동기 작업 결합
        val users = async { fetchUsers() }
        val orders = async { fetchOrders() }
        val stats = async { calculateStats() }

        zip(users, orders, stats) { u, o, s ->
            Report(u, o, s)
        }.await()
    }

    suspend fun processWithRetry(item: Item): Result {
        return coTry {
            processItem(item)
        }.getOrElse {
            // 재시도 로직
            retry(3) { processItem(item) }
        }
    }

    fun cleanup() {
        scope.cancel()
    }
}
```

## 모범 사례

### 1. CoroutineScope 관리

```kotlin
// ✅ 좋은 예: CoroutineScope 명시적 관리
class MyService {
    private val scope = IoCoroutineScope()

    fun processAsync(data: Data) = scope.launch {
        process(data)
    }

    fun close() {
        scope.cancel()
    }
}

// ❌ 나쁜 예: GlobalScope 사용
fun processAsync(data: Data) = GlobalScope.launch {
    process(data)  // 종료 시점 제어 불가
}
```

### 2. AsyncFlow 활용

```kotlin
// ✅ 순서가 중요한 경우: AsyncFlow
users.asFlow()
    .async { user ->
        enrichUserData(user)
    }
    .collect { println(it.id) }  // 1, 2, 3, ...

// ✅ 순서가 중요하지 않은 경우: flatMapMerge
users.asFlow()
    .flatMapMerge { user ->
        flow { emit(enrichUserData(user)) }
    }
    .collect { println(it.id) }  // 3, 1, 5, ... (빠른 순서)
```

### 3. 배치 처리

```kotlin
// ✅ 대량 데이터 배치 처리
largeDataFlow()
    .chunked(1000)
    .collect { batch ->
        database.batchInsert(batch)
    }

// ❌ 하나씩 처리 (비효율)
largeDataFlow()
    .collect { item ->
        database.insert(item)  // 너무 많은 DB 호출
    }
```

### 4. Exception 처리

```kotlin
// ✅ Result 패턴 사용
val result = coTry {
    riskyOperation()
}

result.fold(
    onSuccess = { value -> process(value) },
    onFailure = { error -> handleError(error) }
)

// ✅ supervisorScope로 격리
supervisorScope {
    launch { task1() }  // 실패해도 다른 작업에 영향 없음
    launch { task2() }
}
```

## 성능 고려사항

### AsyncFlow vs flatMapMerge

```kotlin
// AsyncFlow: 순서 보장이 필요할 때
// - 약간의 오버헤드 있음
// - 메모리: O(n) - 버퍼링 필요

// flatMapMerge: 순서 불필요할 때
// - 더 빠름
// - 메모리: O(concurrency)
```

### 배치 크기 선택

```kotlin
// 너무 작으면: 오버헤드 증가
.chunked(10)  // 배치가 너무 작음

// 너무 크면: 메모리 부담
    .chunked(100000)  // 배치가 너무 큼

// 적절한 크기: 100-1000
    .chunked(500)  // ✅ 균형 잡힌 크기
```

## 참고 자료

- [Kotlin Coroutines 공식 문서](https://kotlinlang.org/docs/coroutines-overview.html)
- [Kotlin Coroutines Best Practices](https://kt.academy/article/cc-best-practices)
- [Kotlin Flow 가이드](https://kotlinlang.org/docs/flow.html)
- [Project Reactor](https://projectreactor.io/)
