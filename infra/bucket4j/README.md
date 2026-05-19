# Module bluetape4k-bucket4j

English | [한국어](./README.ko.md)

A wrapper and utility module for building application-level rate limiters using Bucket4j.

## Key Features

- **Custom key-based limiting**: Control by `userId`, `apiKey`, `tenantId`, or any key — not just IP addresses
- **Local and distributed support**: In-memory (`Local*`) and Redis-based distributed (`Distributed*`) implementations
- **Sync and coroutine APIs**: `RateLimiter` and `SuspendRateLimiter`
- **Immediate consume contract**:
  `SuspendRateLimiter.consume` attempts consumption immediately without waiting and returns `CONSUMED` or `REJECTED`
- **Probe-based result calculation**: Derives consumption outcome and remaining tokens from a single
  `ConsumptionProbe` query, avoiding extra token lookups
- **Bucket configuration DSL**: `bucketConfiguration { ... }` and `addBandwidth { ... }` helpers
- **Redis ProxyManager helpers**: `*ProxyManagerOf` utilities for Lettuce and Redisson
- **Stable result contract**:
  `RateLimitResult(status, consumedTokens, availableTokens, errorMessage, diagnostics)` consistently represents consumed, rejected, and error outcomes
- **Retry diagnostics**: Rejected results expose `retryAfter`, refill/reset nanos, and a stable
  `RateLimitRejectionReason`
- **Built-in request validation**: Rejects blank keys, serialized keys above 512 bytes, tokens <= 0, and requests
  exceeding the policy maximum (`MAX_TOKENS_PER_REQUEST`) upfront

## Class Structure

### Bucket4j Integration Class Diagram

![Bucket4j Integration Class Diagram 1](../../docs/images/readme-diagrams/infra-bucket4j-diagram-01.svg)

### Rate Limiting Sequence Diagrams

#### Local Rate Limiter — Token Consumption Flow

![Local Rate Limiter — Token Consumption Flow 2](../../docs/images/readme-diagrams/infra-bucket4j-diagram-02.svg)

#### Distributed Suspend Rate Limiter — Redis-Based Coroutine Flow

![Distributed Suspend Rate Limiter — Redis-Based Coroutine Flow 3](../../docs/images/readme-diagrams/infra-bucket4j-diagram-03.svg)

## What This Module Adds Over Raw Bucket4j

`bluetape4k-bucket4j` focuses on eliminating boilerplate when using Bucket4j directly.

- **Standardized key-based bucket resolution**: `LocalBucketProvider`, `BucketProxyProvider`, `AsyncBucketProxyProvider`
- **RateLimiter abstraction**: Swap between local and distributed implementations via the same
  `consume(key, token)` call
- **Coroutine-friendly implementations**: `SuspendLocalBucket`, `LocalSuspendRateLimiter`,
  `DistributedSuspendRateLimiter`
- **Simplified Redis initialization**: `lettuceBasedProxyManagerOf`, `redissonBasedProxyManagerOf`
- **Minimized extra remote calls**: Distributed and local rate limiters do not issue additional
  `availableTokens` queries to build results
- **Facade boundary**: This module owns token-bucket rate limiting only. Use Resilience4j for retry, timeout,
  circuit breaker, bulkhead, and fallback policies.

## Dependency

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-bucket4j:${version}")

    // For Redis-based distributed rate limiting
    implementation("io.lettuce:lettuce-core") // or org.redisson:redisson
    implementation("com.bucket4j:bucket4j_jdk17-lettuce") // or bucket4j_jdk17-redisson
}
```

## Usage Examples

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

### 3) Coroutine-Based Rate Limiter

```kotlin
val config = bucketConfiguration {
    addBandwidth { Bandwidth.simple(20, Duration.ofSeconds(1)) }
}

val bucketProvider = LocalSuspendBucketProvider(config)
val rateLimiter: SuspendRateLimiter<String> = LocalSuspendRateLimiter(bucketProvider)

val result = rateLimiter.consume("user:1001", 1)

when (result.status) {
    RateLimitStatus.CONSUMED -> {
        // Allowed
    }
    RateLimitStatus.REJECTED -> {
        // Rejected due to insufficient tokens
        // Use result.retryAfter for a 429 Retry-After policy after application-level rounding
    }
    RateLimitStatus.ERROR -> {
        // Redis failure or communication error
        // Check result.errorMessage
    }
}
```

> Note:
`SuspendRateLimiter.consume` attempts immediate consumption internally without waiting. When tokens are insufficient,
`REJECTED` is returned immediately. Retry/backoff logic is the caller's responsibility.

### 4) Distributed Coroutine Timeout

```kotlin
val rateLimiter = DistributedSuspendRateLimiter(
    asyncBucketProxyProvider = asyncBucketProxyProvider,
    defaultTimeout = 100.milliseconds,
)

val result = rateLimiter.consume("tenant:a:user:42", 1)
```

The timeout bounds the async Redis operation. A timeout returns `RateLimitStatus.ERROR`; coroutine cancellation still
throws `CancellationException`. The in-flight Redis command can still complete after the coroutine is cancelled.
The per-call timeout overload is available on `DistributedSuspendRateLimiter`; code injected as `SuspendRateLimiter`
should configure `defaultTimeout` at bean construction time.

### 5) Configuration Replacement With Bandwidth IDs

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

Assign stable bandwidth IDs before replacing a configuration. Without matching IDs, Bucket4j cannot safely inherit
tokens across changed limits.

## Public API Contract Notes

- Both `RateLimiter.consume` and `SuspendRateLimiter.consume` validate `key` and `numToken` first.
  `key` must not be blank, the prefixed serialized key must be at most 512 bytes, and `numToken` must be in the range
  `1..MAX_TOKENS_PER_REQUEST`.
- `DistributedRateLimiter` and
  `DistributedSuspendRateLimiter` derive the consumption outcome and remaining tokens from a single
  `ConsumptionProbe`, so no extra Redis round-trips are made to build results.
- `BucketProxyProvider` and
  `AsyncBucketProxyProvider` namespace bucket keys using a default prefix. In production, if multiple rate limiting policies share the same Redis instance, it is safer to use distinct prefixes.
- Callers own Redis client lifecycle, connection pooling, shutdown, and expiration strategy. For distributed buckets,
  configure Bucket4j expiration after write according to the policy TTL.
- `LocalBucketProvider` and `LocalSuspendBucketProvider` reuse the same bucket state for the same key.
- `SuspendLocalBucket.tryConsume(maxWaitTime)` suspends the coroutine with
  `delay` when waiting is needed, and propagates `CancellationException` unchanged on cancellation.
- `RateLimitResult.error(cause)` stores a sanitized public message in `errorMessage`. URI user-info credentials are
  redacted and messages are capped at 256 characters.
- `RateLimitResult.retryAfter` is derived from Bucket4j refill nanos for rejected results only. HTTP header rounding
  remains an application policy.

## Spring Boot Configuration

This module is better suited for assembly as application beans than Spring Boot Auto Configuration.

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

Inject `RateLimiter` into a WebFlux/WebMVC filter or interceptor and call
`consume(key)` to apply application-level rate limiting policies.

## Implementation Notes

- `SuspendLocalBucket` uses `delay` when waiting, making it coroutine-friendly.
- If the coroutine is cancelled while waiting, the interrupt event is recorded and cancellation is propagated as-is.
- `LocalSuspendRateLimiter` and `DistributedSuspendRateLimiter` propagate
  `CancellationException` unchanged rather than converting it to `ERROR`.
- An unusually large `maxWaitTime` that overflows during nanosecond conversion is handled with an
  `IllegalArgumentException`.
- `AbstractLocalBucketProvider` does not accept blank keys.
- `BucketProxyProvider` and
  `AsyncBucketProxyProvider` do not read remaining tokens at bucket resolution time, preventing unnecessary remote calls.
