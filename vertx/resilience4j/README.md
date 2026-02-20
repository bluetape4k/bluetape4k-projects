# Module bluetape4k-vertx-resilience4j

Vert.x 작업 시 Resilience4j를 적용할 수 있도록 해주는 모듈입니다.

## 개요

`bluetape4k-vertx-resilience4j`는 Vert.x의
`Future<T>`에 Resilience4j의 회복성 패턴(Circuit Breaker, Retry, Rate Limiter, Bulkhead, Time Limiter)을 적용할 수 있도록 확장 함수와 데코레이터를 제공합니다.

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-vertx-resilience4j:${version}")
}
```

## 주요 기능

### 1. Circuit Breaker

Vert.x Future에 Circuit Breaker 패턴을 적용합니다.

```kotlin
import io.bluetape4k.vertx.resilience4j.*
import io.github.resilience4j.circuitbreaker.CircuitBreaker

val circuitBreaker = CircuitBreaker.ofDefaults("my-cb")

// executeVertxFuture - 바로 실행
val result: Future<String> = circuitBreaker.executeVertxFuture {
    vertx.executeBlocking<String> { promise ->
        promise.complete("Hello")
    }
}

// decorateVertxFuture - 데코레이터 생성
val decorated = circuitBreaker.decorateVertxFuture {
    service.fetchData()
}
val future = decorated.invoke()
```

### 2. Retry

Vert.x Future에 재시도 패턴을 적용합니다.

```kotlin
import io.bluetape4k.vertx.resilience4j.*
import io.github.resilience4j.retry.Retry

val retry = Retry.ofDefaults("my-retry")

val result: Future<String> = retry.executeVertxFuture(scheduler) {
    unstableService.call()
}
```

### 3. Rate Limiter

Vert.x Future에 속도 제한을 적용합니다.

```kotlin
import io.bluetape4k.vertx.resilience4j.*
import io.github.resilience4j.ratelimiter.RateLimiter

val rateLimiter = RateLimiter.ofDefaults("my-rl")

val result: Future<String> = rateLimiter.executeVertxFuture {
    apiService.request()
}
```

### 4. Bulkhead

Vert.x Future에 동시 실행 수 제한을 적용합니다.

```kotlin
import io.bluetape4k.vertx.resilience4j.*
import io.github.resilience4j.bulkhead.Bulkhead

val bulkhead = Bulkhead.ofDefaults("my-bh")

val result: Future<String> = bulkhead.executeVertxFuture {
    heavyOperation()
}
```

### 5. Time Limiter

Vert.x Future에 실행 시간 제한을 적용합니다.

```kotlin
import io.bluetape4k.vertx.resilience4j.*
import io.github.resilience4j.timelimiter.TimeLimiter

val timeLimiter = TimeLimiter.ofDefaults("my-tl")

val result: Future<String> = timeLimiter.executeVertxFuture(scheduler) {
    potentiallySlowOperation()
}
```

### 6. VertxDecorators (조합 데코레이터)

여러 Resilience4j 컴포넌트를 조합하여 Vert.x Future에 적용합니다.

```kotlin
import io.bluetape4k.vertx.resilience4j.VertxDecorators

// 여러 패턴 조합
val decorated = VertxDecorators.ofSupplier {
    apiClient.fetchData()
}
    .withCircuitBreaker(circuitBreaker)
    .withRetry(retry, scheduler)
    .withRateLimiter(rateLimiter)
    .withBulkhead(bulkhead)
    .withTimeLimiter(timeLimiter, scheduler)
    .withFallback { result, throwable ->
        // 실패 시 대체 로직
        "default-value"
    }
    .decorate()

// 실행
val result = decorated().asCompletableFuture().get()
```

### 7. Fallback 처리

```kotlin
// 예외 타입별 Fallback
VertxDecorators.ofSupplier { riskyOperation() }
    .withCircuitBreaker(circuitBreaker)
    .withFallback(IOException::class) { ex ->
        fallbackValue
    }
    .decorate()

// 결과값 기반 Fallback
VertxDecorators.ofSupplier { apiCall() }
    .withFallback(
        resultPredicate = { it == null },
        resultHandler = { getFromCache() }
    )
    .decorate()
```

## 주요 파일/클래스 목록

| 파일                                    | 설명                 |
|---------------------------------------|--------------------|
| `VertxDecorators.kt`                  | 복합 데코레이터 빌더        |
| `VertxFutureCircuitBreakerSupport.kt` | Circuit Breaker 확장 |
| `VertxFutureRetrySupport.kt`          | Retry 확장           |
| `VertxFutureRateLimiterSupport.kt`    | Rate Limiter 확장    |
| `VertxFutureBulkheadSupport.kt`       | Bulkhead 확장        |
| `VertxFutureTimeLimiterSupport.kt`    | Time Limiter 확장    |
| `VertxFutureSupport.kt`               | Fallback 등 공통 확장   |

## 테스트

```bash
./gradlew :vertx:resilience4j:test
```

## 참고

- [Resilience4j](https://resilience4j.readme.io/)
- [Eclipse Vert.x](https://vertx.io/)
- [bluetape4k-resilience4j](../../infra/resilience4j/README.md)
- [bluetape4k-vertx-core](../core/README.md)
