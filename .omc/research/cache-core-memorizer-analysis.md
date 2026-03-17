# cache-core Memorizer 완전 분석

## 디렉토리 구조

```
infra/cache-core/src/main/kotlin/io/bluetape4k/cache/memorizer/
├── AsyncMemorizer.kt (인터페이스)
├── Memorizer.kt (인터페이스)
├── SuspendMemorizer.kt (인터페이스)
├── caffeine/
│   ├── CaffeineMemorizer.kt
│   ├── AsyncCaffeineMemorizer.kt
│   └── SuspendCaffeineMemorizer.kt
├── cache2k/
│   ├── Cache2kMemorizer.kt
│   ├── AsyncCache2kMemorizer.kt
│   └── SuspendCache2kMemorizer.kt
├── ehcache/
│   ├── EhCacheMemorizer.kt
│   ├── AsyncEhCacheMemorizer.kt
│   └── SuspendEhCacheMemorizer.kt
├── inmemory/
│   ├── InMemoryMemorizer.kt
│   ├── AsyncInMemoryMemorizer.kt
│   └── SuspendInMemoryMemorizer.kt
└── jcache/
    ├── JCacheMemorizer.kt
    ├── AsyncJCacheMemorizer.kt
    └── SuspendJCacheMemorizer.kt
```

---

## 1. 핵심 인터페이스 (3개)

### 1.1 Memorizer<T, R>

**파일**: `Memorizer.kt` | **패키지**: `io.bluetape4k.cache.memorizer`

```kotlin
interface Memorizer<in T: Any, out R: Any>: (T) -> R {
    fun clear()
}
```

**역할**: 동기 함수 결과를 입력 키 기준으로 메모이제이션  
**계약**: 동일 입력에 대한 반환값 재사용, clear() 호출 시 모든 캐시 엔트리 제거

---

### 1.2 AsyncMemorizer<T, R>

**파일**: `AsyncMemorizer.kt` | **패키지**: `io.bluetape4k.cache.memorizer`

```kotlin
interface AsyncMemorizer<in T: Any, R: Any>: (T) -> CompletableFuture<R> {
    fun clear()
}
```

**역할**: CompletableFuture 기반 비동기 함수 결과를 메모이제이션  
**계약**: 동일 입력에 대해 동일한 비동기 계산 결과 재사용

---

### 1.3 SuspendMemorizer<T, R>

**파일**: `SuspendMemorizer.kt` | **패키지**: `io.bluetape4k.cache.memorizer`

```kotlin
interface SuspendMemorizer<in T: Any, out R: Any>: suspend (T) -> R {
    suspend fun clear()
}
```

**역할**: suspend 함수 결과를 입력 키 기준으로 메모이제이션  
**특이사항**: clear() 메서드도 suspend 함수

---

## 2. 구현체 3중 패턴

각 캐시 백엔드마다 **동일한 3개 구현체** 제공:

| 클래스명                  | 인터페이스                    | 호출 방식      | 반환값                    |
|-----------------------|--------------------------|------------|------------------------|
| `XxxMemorizer`        | `Memorizer<T, R>`        | 동기 함수      | `R`                    |
| `AsyncXxxMemorizer`   | `AsyncMemorizer<T, R>`   | 함수         | `CompletableFuture<R>` |
| `SuspendXxxMemorizer` | `SuspendMemorizer<T, R>` | suspend 함수 | `R`                    |

---

## 3. 백엔드별 구현 (5개)

### 3.1 Caffeine (205줄 총)

**구현체**:

- `CaffeineMemorizer` (54줄)
- `AsyncCaffeineMemorizer` (70줄)
- `SuspendCaffeineMemorizer` (81줄)

**특징**:

- 동기화: ReentrantLock (sync/async), Mutex (suspend)
- 조회: `cache.getIfPresent()` + 수동 put
- Clear: `cache.cleanUp()`
- 팩토리: 2가지 형식 (캐시 기준, 함수 기준)

**코드 예시**:

```kotlin
fun <T: Any, R: Any> Cache<T, R>.memorizer(
    @BuilderInference evaluator: (T) -> R,
): CaffeineMemorizer<T, R> = CaffeineMemorizer(this, evaluator)

fun <T: Any, R: Any> ((T) -> R).withMemorizer(
    cache: Cache<T, R>
): CaffeineMemorizer<T, R> = CaffeineMemorizer(cache, this)
```

---

### 3.2 Cache2k (213줄 총)

**구현체**:

- `Cache2kMemorizer` (54줄)
- `Cache2kAsyncMemorizer` (80줄)
- `SuspendCache2kMemorizer` (79줄)

**특징**:

- 원자적 조회: `cache.computeIfAbsent()` (sync만)
- 비동기: `cache.containsKey()` + 수동 put
- 에러 처리: Cache2kAsyncMemorizer에서 `BluetapeException` + warn 로그
- Clear: `cache.clear()`

**AsyncMemorizer 특수성**:

```kotlin
// 실패 시 명시적 로깅
cache.put(input, value)
promise.complete(value)
log.debug { "Success to run `asyncEvaluator`. input=$input, result=$value" }
```

---

### 3.3 EhCache (201줄 총)

**구현체**:

- `EhCacheMemorizer` (53줄)
- `AsyncEhCacheMemorizer` (68줄)
- `SuspendEhCacheMemorizer` (80줄)

**특징**:

- 동기화: ReentrantLock (sync/async), Mutex (suspend)
- 조회: `cache.get()` + 수동 put
- Clear: `cache.clear()`
- 팩토리: Caffeine과 동일 패턴

---

### 3.4 InMemory (구현체당 ~40-50줄)

**구현체**:

- `InMemoryMemorizer`
- `AsyncInMemoryMemorizer`
- `SuspendInMemoryMemorizer`

**특징**:

- 저장소: `ConcurrentHashMap<T, R>`
- 동기화: ReentrantLock (sync/async), Mutex (suspend)
- 원자적 조회: `resultCache.getOrPut()`
- 팩토리: 함수 기준만 제공 → `((T) -> R).memorizer()`
- SuspendInMemoryMemorizer: trace 레벨 로깅 ("Cache miss for key...")

**특수성**:

```kotlin
// AsyncInMemoryMemorizer: try-catch 감싼 invoke()
try {
    if (resultCache.containsKey(input)) {
        promise.complete(resultCache[input])
    } else {
        evaluator(input).whenComplete { result, error -> ... }
    }
} catch (e: Throwable) {
    promise.completeExceptionally(e)
}
```

---

### 3.5 JCache (구현체당 ~40-50줄)

**구현체**:

- `JCacheMemorizer`
- `AsyncJCacheMemorizer`
- `SuspendJCacheMemorizer`

**특징**:

- 저장소: `javax.cache.Cache<T, R>`
- 동기화: ReentrantLock (sync/async), Mutex (suspend)
- 원자적 조회: `jcache.getOrPut()` (확장함수 활용)
- AsyncMemorizer: try-catch 감싼 invoke()
- 팩토리: 2가지 형식 모두 제공

---

## 4. 구현 패턴 비교표

### 동기화 전략

```
동기/비동기: ReentrantLock
Suspend:    Mutex (kotlinx.coroutines.sync)
```

### 캐시 조회 방식

| 백엔드      | 원자적 방법                | 수동 방법                    |
|----------|-----------------------|--------------------------|
| Caffeine | ❌                     | `getIfPresent() + put()` |
| Cache2k  | ✅ `computeIfAbsent()` | -                        |
| EhCache  | ❌                     | `get() + put()`          |
| InMemory | ✅ `getOrPut()`        | -                        |
| JCache   | ✅ `getOrPut()`        | -                        |

### 에러 처리

| 구현                     | 처리 방식                             |
|------------------------|-----------------------------------|
| Cache2kAsyncMemorizer  | BluetapeException + warn/debug 로그 |
| AsyncInMemoryMemorizer | try-catch (invoke 전체)             |
| AsyncJCacheMemorizer   | try-catch (invoke 전체)             |
| 나머지                    | 예외 그대로 전파                         |

---

## 5. 팩토리 함수 패턴

### 형식 1: 캐시 객체 기준 (Cache 확장함수)

```kotlin
fun <T: Any, R: Any> Cache<T, R>.memorizer(
    @BuilderInference evaluator: (T) -> R
): XxxMemorizer<T, R>

fun <T: Any, R: Any> Cache<T, R>.asyncMemorizer(
    @BuilderInference evaluator: (T) -> CompletableFuture<R>
): AsyncXxxMemorizer<T, R>

fun <T: Any, R: Any> Cache<T, R>.suspendMemorizer(
    @BuilderInference evaluator: suspend (T) -> R
): SuspendXxxMemorizer<T, R>
```

### 형식 2: 함수 객체 기준 (함수 확장함수)

```kotlin
fun <T: Any, R: Any> ((T) -> R).withMemorizer(
    cache: Cache<T, R>
): XxxMemorizer<T, R>

fun <T: Any, R: Any> ((T) -> CompletableFuture<R>).withAsyncMemorizer(
    cache: Cache<T, R>
): AsyncXxxMemorizer<T, R>

fun <T: Any, R: Any> (suspend (T) -> R).withSuspendMemorizer(
    cache: Cache<T, R>
): SuspendXxxMemorizer<T, R>
```

### InMemory 특수성

- 형식 2만 제공 (캐시 객체 없이 함수만으로 생성)
- `((T) -> R).memorizer()`

---

## 6. 핵심 API 계약

### Memorizer 호출 패턴

```kotlin
val memo: Memorizer<String, Int> = { it.length }.memorizer()
val size1 = memo("hello")  // evaluator 실행
val size2 = memo("hello")  // 캐시 반환
memo.clear()               // 모든 엔트리 제거
```

### AsyncMemorizer 호출 패턴

```kotlin
val asyncMemo: AsyncMemorizer<String, Int> = 
    { key: String -> CompletableFuture.completedFuture(key.length) }.asyncMemorizer()
val futureSize = asyncMemo("hello")  // CompletableFuture<Int> 반환
futureSize.thenAccept { println(it) }
asyncMemo.clear()
```

### SuspendMemorizer 호출 패턴

```kotlin
val suspendMemo: SuspendMemorizer<String, Int> = 
    suspendMemorizer { it.length }
val size = suspendMemo("hello")  // suspend 컨텍스트에서 호출
suspendMemo.clear()              // suspend 함수
```

---

## 7. 주요 설계 결정

1. **3중 구현**: 동기/비동기/suspend 모두 동일한 인터페이스로 지원
2. **백엔드 선택**: 캐시 라이브러리에 무관하게 동일한 API
3. **팩토리 이중화**: 캐시 또는 함수 기준으로 생성 가능
4. **동기화 차이**: ReentrantLock (JVM 동기) vs Mutex (코루틴)
5. **원자성 활용**: 지원하는 백엔드는 원자적 연산 사용
6. **에러 전략**: 대부분 전파하되, 특정 백엔드는 명시적 로깅

---

## 8. 파일 크기 요약

| 항목                  | 총 줄 수         |
|---------------------|---------------|
| Memorizer.kt        | ~30           |
| AsyncMemorizer.kt   | ~30           |
| SuspendMemorizer.kt | ~30           |
| caffeine/ (3개)      | 205           |
| cache2k/ (3개)       | 213           |
| ehcache/ (3개)       | 201           |
| inmemory/ (3개)      | ~130-150      |
| jcache/ (3개)        | ~130-150      |
| **전체 memorizer**    | **~900-1000** |
