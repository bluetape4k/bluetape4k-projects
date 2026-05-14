# Module bluetape4k-bucket4j

[English](./README.md) | 한국어

Bucket4j 기반으로 애플리케이션 레벨 Rate Limiter를 구성하기 위한 래퍼/유틸 모듈입니다.

## 주요 기능

- **Custom Key 기반 제한**: IP가 아닌 `userId`, `apiKey`, `tenantId` 같은 키 기준으로 제어
- **로컬/분산 환경 지원**: in-memory(`Local*`)와 Redis 기반 분산(`Distributed*`) 구현 제공
- **동기/코루틴 API 동시 제공**: `RateLimiter`, `SuspendRateLimiter`
- **즉시 소비 시도 계약**: `SuspendRateLimiter.consume`은 대기하지 않고 즉시 소비 시도 후 `CONSUMED/REJECTED`를 반환
- **Probe 기반 결과 계산**: 소비 성공 여부와 남은 토큰 수를 `ConsumptionProbe` 한 번의 조회 결과로 계산해 추가 토큰 조회를 줄임
- **Bucket 구성 DSL**: `bucketConfiguration { ... }`, `addBandwidth { ... }` 헬퍼 제공
- **Redis ProxyManager 헬퍼**: Lettuce/Redisson용 `*ProxyManagerOf` 유틸 제공
- **안정적인 결과 계약**:
  `RateLimitResult(status, consumedTokens, availableTokens, errorMessage, diagnostics)`로 소비/거절/오류를 일관되게 반환
- **재시도 진단 정보**: 거절 결과에서 `retryAfter`, refill/reset nanos, 안정적인 `RateLimitRejectionReason` 제공
- **요청 검증 내장**: 빈 key, 직렬화된 key 512 bytes 초과, `0 이하 token`, 정책 상한(`MAX_TOKENS_PER_REQUEST`) 초과 요청을 사전에 차단

## 클래스 구조

### Bucket4j 통합 클래스 다이어그램

```mermaid
classDiagram
    direction TB

    class RateLimiter~K~ {
        <<interface>>
        +consume(key: K, numToken: Long) RateLimitResult
    }

    class SuspendRateLimiter~K~ {
        <<interface>>
        +consume(key: K, numToken: Long) RateLimitResult
    }

    class RateLimitResult {
        +status: RateLimitStatus
        +consumedTokens: Long
        +availableTokens: Long
        +errorMessage: String?
        +diagnostics: RateLimitDiagnostics
        +retryAfter: Duration?
        +isConsumed: Boolean
        +isRejected: Boolean
        +isError: Boolean
        +consumed(consumedTokens, availableTokens, diagnostics) RateLimitResult
        +rejected(availableTokens, diagnostics) RateLimitResult
        +error(cause) RateLimitResult
    }

    class RateLimitDiagnostics {
        +nanosToWaitForRefill: Long
        +nanosToWaitForReset: Long
        +rejectionReason: RateLimitRejectionReason?
        +rejected(nanosToWaitForRefill, nanosToWaitForReset, rejectionReason) RateLimitDiagnostics
    }

    class RateLimitStatus {
        <<enumeration>>
        CONSUMED
        REJECTED
        ERROR
    }

    class RateLimitRejectionReason {
        <<enumeration>>
        INSUFFICIENT_TOKENS
    }

    class LocalRateLimiter {
        -bucketProvider: LocalBucketProvider
        +consume(key, numToken) RateLimitResult
    }

    class LocalSuspendRateLimiter {
        -bucketProvider: LocalSuspendBucketProvider
        +consume(key, numToken) RateLimitResult
    }

    class DistributedRateLimiter {
        -bucketProxyProvider: BucketProxyProvider
        +consume(key, numToken) RateLimitResult
    }

    class DistributedSuspendRateLimiter {
        -asyncBucketProxyProvider: AsyncBucketProxyProvider
        -defaultTimeout: Duration?
        +consume(key, numToken) RateLimitResult
        +consume(key, numToken, timeout) RateLimitResult
    }

    class AbstractLocalBucketProvider~T~ {
        #bucketConfiguration: BucketConfiguration
        #keyPrefix: String
        #cache: LoadingCache
        +resolveBucket(key: String) T
        #createBucket() T
    }

    class LocalBucketProvider {
        +createBucket() LocalBucket
    }

    class LocalSuspendBucketProvider {
        +createBucket() SuspendLocalBucket
    }

    class SuspendLocalBucket {
        +tryConsume(tokensToConsume, maxWaitTime) Boolean
        +consume(tokensToConsume)
    }

    class BucketProxyProvider {
        #proxyManager: ProxyManager
        #bucketConfiguration: BucketConfiguration
        +resolveBucket(key: String) BucketProxy
    }

    class AsyncBucketProxyProvider {
        #asyncProxyManager: AsyncProxyManager
        #bucketConfiguration: BucketConfiguration
        +resolveBucket(key: String) AsyncBucketProxy
    }

    RateLimiter <|.. LocalRateLimiter
    RateLimiter <|.. DistributedRateLimiter
    SuspendRateLimiter <|.. LocalSuspendRateLimiter
    SuspendRateLimiter <|.. DistributedSuspendRateLimiter
    AbstractLocalBucketProvider <|-- LocalBucketProvider
    AbstractLocalBucketProvider <|-- LocalSuspendBucketProvider
    LocalBucketProvider <-- LocalRateLimiter
    LocalSuspendBucketProvider <-- LocalSuspendRateLimiter
    LocalSuspendBucketProvider ..> SuspendLocalBucket: creates
    BucketProxyProvider <-- DistributedRateLimiter
    AsyncBucketProxyProvider <-- DistributedSuspendRateLimiter
    RateLimitResult --> RateLimitStatus
    RateLimitResult --> RateLimitDiagnostics
    RateLimitDiagnostics --> RateLimitRejectionReason

    style RateLimiter fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style SuspendRateLimiter fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style RateLimitResult fill:#F57F17,stroke:#E65100,color:#000000
    style RateLimitDiagnostics fill:#FFF3E0,stroke:#FFB74D,color:#E65100
    style RateLimitStatus fill:#F57F17,stroke:#E65100,color:#000000
    style RateLimitRejectionReason fill:#FFF3E0,stroke:#FFB74D,color:#E65100
    style LocalRateLimiter fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style LocalSuspendRateLimiter fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style DistributedRateLimiter fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style DistributedSuspendRateLimiter fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style AbstractLocalBucketProvider fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style LocalBucketProvider fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style LocalSuspendBucketProvider fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style SuspendLocalBucket fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style BucketProxyProvider fill:#ECEFF1,stroke:#B0BEC5,color:#37474F
    style AsyncBucketProxyProvider fill:#ECEFF1,stroke:#B0BEC5,color:#37474F

```

### Rate Limiting 시퀀스 다이어그램

#### 로컬 Rate Limiter — 토큰 소비 흐름

```mermaid
sequenceDiagram
    participant Caller
    participant LocalRateLimiter
    participant LocalBucketProvider
    participant LocalBucket
    Caller ->> LocalRateLimiter: consume("user:1", 1)
    LocalRateLimiter ->> LocalRateLimiter: validateRateLimitRequest(key, numToken)
    LocalRateLimiter ->> LocalBucketProvider: resolveBucket("user:1")
    LocalBucketProvider -->> LocalRateLimiter: LocalBucket (캐시에서 반환)
    LocalRateLimiter ->> LocalBucket: tryConsumeAndReturnRemaining(1)

    alt 토큰 충분 (CONSUMED)
        LocalBucket -->> LocalRateLimiter: probe { isConsumed=true, remaining=9, resetNanos=... }
        LocalRateLimiter -->> Caller: RateLimitResult(CONSUMED, consumed=1, available=9, diagnostics)
    else 토큰 부족 (REJECTED)
        LocalBucket -->> LocalRateLimiter: probe { isConsumed=false, remaining=0, refillNanos=... }
        LocalRateLimiter -->> Caller: RateLimitResult(REJECTED, consumed=0, available=0, retryAfter)
    else 오류 발생 (ERROR)
        LocalBucket -->> LocalRateLimiter: Exception
        LocalRateLimiter -->> Caller: RateLimitResult(ERROR, errorMessage=...)
    end
```

#### 분산 Suspend Rate Limiter — Redis 기반 코루틴 흐름

```mermaid
sequenceDiagram
    participant Caller
    participant DistributedSuspendRateLimiter
    participant AsyncBucketProxyProvider
    participant AsyncBucketProxy
    participant Redis
    Caller ->> DistributedSuspendRateLimiter: consume("tenant:a", 1)
    DistributedSuspendRateLimiter ->> AsyncBucketProxyProvider: resolveBucket("tenant:a")
    AsyncBucketProxyProvider -->> DistributedSuspendRateLimiter: AsyncBucketProxy
    DistributedSuspendRateLimiter ->> AsyncBucketProxy: tryConsumeAndReturnRemaining(1)
    AsyncBucketProxy ->> Redis: EVALSHA (atomic Lua script)

    alt 토큰 충분
        Redis -->> AsyncBucketProxy: consumed=1, remaining=99
        AsyncBucketProxy -->> DistributedSuspendRateLimiter: CompletableFuture (await)
        DistributedSuspendRateLimiter -->> Caller: RateLimitResult(CONSUMED, 1, 99, diagnostics)
    else 토큰 부족
        Redis -->> AsyncBucketProxy: consumed=0, remaining=0, refill wait
        AsyncBucketProxy -->> DistributedSuspendRateLimiter: CompletableFuture (await)
        DistributedSuspendRateLimiter -->> Caller: RateLimitResult(REJECTED, 0, 0, retryAfter)
    else timeout 또는 저장소 오류
        AsyncBucketProxy -->> DistributedSuspendRateLimiter: failure
        DistributedSuspendRateLimiter -->> Caller: RateLimitResult(ERROR, errorMessage)
    end
```

## Bucket4j 직접 사용 대비 추가 기능

`bluetape4k-bucket4j`는 Bucket4j를 직접 사용할 때 반복되는 보일러플레이트를 줄이는 데 초점이 있습니다.

- **키 기반 버킷 조회 표준화**: `LocalBucketProvider`, `BucketProxyProvider`, `AsyncBucketProxyProvider`
- **RateLimiter 추상화 제공**: `consume(key, token)` 호출로 로컬/분산 구현체 교체가 쉬움
- **코루틴 친화 구현**: `SuspendLocalBucket`, `LocalSuspendRateLimiter`, `DistributedSuspendRateLimiter`
- **Redis 연동 초기화 단순화**: `lettuceBasedProxyManagerOf`, `redissonBasedProxyManagerOf`
- **추가 원격 조회 최소화**: distributed/local rate limiter는 잔여 토큰 계산을 위해 별도 `availableTokens` 조회를 하지 않음
- **Facade 경계 명확화**: 이 모듈은 token-bucket rate limiting만 담당합니다. retry, timeout, circuit breaker, bulkhead, fallback 정책은 Resilience4j를 사용하세요.

## 의존성 추가

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-bucket4j:${version}")

    // Redis 기반 분산 Rate Limiter 사용 시
    implementation("io.lettuce:lettuce-core") // 또는 org.redisson:redisson
    implementation("com.bucket4j:bucket4j_jdk17-lettuce") // 또는 bucket4j_jdk17-redisson
}
```

## 사용 예

### 1) Local Rate Limiter

```kotlin
val config = bucketConfiguration {
    addBandwidth { Bandwidth.simple(10, Duration.ofSeconds(1)) }
}

val bucketProvider = LocalBucketProvider(config)
val rateLimiter: RateLimiter<String> = LocalRateLimiter(bucketProvider)

val result = rateLimiter.consume("user:1001", 1)
// result.status, result.consumedTokens, result.availableTokens, result.retryAfter
```

### 2) Distributed Rate Limiter (Redis + Lettuce)

```kotlin
val config = bucketConfiguration {
    addBandwidth { Bandwidth.simple(100, Duration.ofMinutes(1)) }
}

val redisClient = RedisClient.create("redis://localhost:6379")
val proxyManager = lettuceBasedProxyManagerOf(redisClient) {
    withClientSideConfig(ClientSideConfig.getDefault())
}

val bucketProvider = BucketProxyProvider(proxyManager, config)
val rateLimiter: RateLimiter<String> = DistributedRateLimiter(bucketProvider)

val result = rateLimiter.consume("tenant:a:user:42", 1)
```

### 3) Coroutine 기반 Rate Limiter

```kotlin
val config = bucketConfiguration {
    addBandwidth { Bandwidth.simple(20, Duration.ofSeconds(1)) }
}

val bucketProvider = LocalSuspendBucketProvider(config)
val rateLimiter: SuspendRateLimiter<String> = LocalSuspendRateLimiter(bucketProvider)

val result = rateLimiter.consume("user:1001", 1)

when (result.status) {
    RateLimitStatus.CONSUMED -> {
        // 허용됨
    }
    RateLimitStatus.REJECTED -> {
        // 토큰 부족으로 거절됨
        // result.retryAfter를 애플리케이션 정책으로 반올림해 429 Retry-After에 사용할 수 있음
    }
    RateLimitStatus.ERROR -> {
        // Redis 장애/통신 오류 등
        // result.errorMessage 확인 가능
    }
}
```

> 참고: `SuspendRateLimiter.consume`은 내부적으로 대기하지 않는 즉시 소비 시도 API입니다. 토큰 부족 시 `REJECTED`가 즉시 반환되며, 재시도/백오프는 호출자 정책으로 처리합니다.

### 4) 분산 코루틴 Timeout

```kotlin
val rateLimiter = DistributedSuspendRateLimiter(
    asyncBucketProxyProvider = asyncBucketProxyProvider,
    defaultTimeout = 100.milliseconds,
)

val result = rateLimiter.consume("tenant:a:user:42", 1)
```

timeout은 비동기 Redis 작업 시간을 제한합니다. timeout은 `RateLimitStatus.ERROR`로 반환되며, 코루틴 취소는 여전히 `CancellationException`으로 전파됩니다. 취소 후에도 이미 전송된 Redis 명령은 완료될 수 있습니다.
호출별 timeout overload는 `DistributedSuspendRateLimiter` 구체 타입에서 사용할 수 있습니다. `SuspendRateLimiter`로 주입받는 코드는 bean 생성 시 `defaultTimeout`을 설정하세요.

### 5) Bandwidth ID 기반 Configuration Replacement

```kotlin
val initial = bucketConfiguration {
    addBandwidth {
        Bandwidth.builder()
            .capacity(10)
            .refillGreedy(10, Duration.ofMinutes(1))
            .id("per-minute")
            .build()
    }
}

val replacement = bucketConfiguration {
    addBandwidth {
        Bandwidth.builder()
            .capacity(20)
            .refillGreedy(20, Duration.ofMinutes(1))
            .id("per-minute")
            .build()
    }
}

bucket.replaceConfiguration(replacement, TokensInheritanceStrategy.PROPORTIONALLY)
```

설정을 교체하기 전에 안정적인 bandwidth ID를 지정하세요. ID가 일치하지 않으면 Bucket4j가 변경된 limit 사이에서 토큰을 안전하게 상속할 수 없습니다.

## Public API 계약 메모

- `RateLimiter.consume`, `SuspendRateLimiter.consume`은 모두 `key`와 `numToken`을 먼저 검증합니다.
  `key`는 blank일 수 없고, prefix가 적용된 직렬화 key는 512 bytes 이하여야 하며, `numToken`은 `1..MAX_TOKENS_PER_REQUEST` 범위여야 합니다.
- `DistributedRateLimiter`, `DistributedSuspendRateLimiter`는
  `ConsumptionProbe` 한 번으로 소비 여부와 잔여 토큰을 계산합니다. 따라서 결과를 만들기 위해 추가 Redis round-trip을 발생시키지 않습니다.
- `BucketProxyProvider`,
  `AsyncBucketProxyProvider`는 기본 prefix를 사용해 bucket key를 namespacing 합니다. 운영 환경에서 여러 rate limit 정책이 같은 Redis를 공유한다면 prefix를 명시적으로 분리하는 것이 안전합니다.
- Redis client 생명주기, connection pool, shutdown, expiration strategy는 호출자가 소유합니다. 분산 버킷은 정책 TTL에 맞는 Bucket4j expiration after write 전략을 구성하세요.
- `LocalBucketProvider`, `LocalSuspendBucketProvider`는 같은 key에 대해 동일한 버킷 상태를 재사용합니다.
- `SuspendLocalBucket.tryConsume(maxWaitTime)`는 대기가 필요하면 코루틴을 `delay`로 일시 중단하고, 취소 시 `CancellationException`을 그대로 전파합니다.
- `RateLimitResult.error(cause)`는 정제된 public message를 `errorMessage`에 저장합니다. URI user-info credential은 redaction되고 메시지는 256자로 제한됩니다.
- `RateLimitResult.retryAfter`는 거절 결과의 Bucket4j refill nanos에서만 계산됩니다. HTTP header 반올림 정책은 애플리케이션이 소유합니다.

## Spring Boot 환경 구성

이 모듈은 Spring Boot Auto Configuration을 제공하기보다, 애플리케이션 빈으로 조립해 사용하는 방식에 적합합니다.

```kotlin
@Configuration
class RateLimitConfig {
    @Bean
    fun bucketConfiguration(): BucketConfiguration =
        bucketConfiguration {
            addBandwidth { Bandwidth.simple(60, Duration.ofMinutes(1)) }
        }

    @Bean
    fun localRateLimiter(config: BucketConfiguration): RateLimiter<String> =
        LocalRateLimiter(LocalBucketProvider(config))
}
```

WebFlux/WebMVC 필터(또는 인터셉터)에서 `RateLimiter`를 주입받아 `consume(key)`를 호출하면 애플리케이션 정책으로 쉽게 연결할 수 있습니다.

## 구현 메모

- `SuspendLocalBucket`은 대기 시 `delay`를 사용해 코루틴 친화적으로 동작합니다.
- 대기 중 코루틴이 취소되면 interrupt 이벤트를 기록하고 취소를 그대로 전파합니다.
- `LocalSuspendRateLimiter`, `DistributedSuspendRateLimiter`는 `CancellationException`을 `ERROR`로 변환하지 않고 그대로 전파합니다.
- `maxWaitTime`이 비정상적으로 큰 경우 nanos 변환 overflow를 `IllegalArgumentException`으로 처리합니다.
- `AbstractLocalBucketProvider`는 blank key를 허용하지 않습니다.
- `BucketProxyProvider`, `AsyncBucketProxyProvider`는 bucket resolve 시점에 잔여 토큰을 읽지 않아 불필요한 원격 조회를 방지합니다.
