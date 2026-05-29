# bluetape4k-ktor-resilience4j

[English](./README.md) | [한국어](./README.ko.md)

Route-scoped Resilience4j helpers for Ktor server applications in the bluetape4k ecosystem.

## Features

- `KtorResiliencePolicies` for caller-owned retry, circuit breaker, rate limiter, and time limiter objects.
- `withKtorResilience()` for protecting any suspend block.
- `resilientRoute()`, `resilientGet()`, and `resilientPost()` for route-scoped policy application.
- `StatusPagesConfig.bluetape4kResilienceErrors()` for safe JSON error responses.
- Cancellation-safe circuit breaker handling: `CancellationException` is rethrown and not counted as a failure.

## Dependency

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-ktor-resilience4j")
}
```

## Usage

```kotlin
import io.bluetape4k.ktor.core.Bluetape4kKtorCoreConfig
import io.bluetape4k.ktor.core.bluetape4kErrorResponses
import io.bluetape4k.ktor.core.installBluetape4kKtorCore
import io.bluetape4k.ktor.resilience4j.KtorResiliencePolicies
import io.bluetape4k.ktor.resilience4j.bluetape4kResilienceErrors
import io.bluetape4k.ktor.resilience4j.resilientGet
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.retry.Retry
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing

fun Application.module() {
    installBluetape4kKtorCore(Bluetape4kKtorCoreConfig(installStatusPages = false))
    install(StatusPages) {
        bluetape4kResilienceErrors()
        bluetape4kErrorResponses()
    }

    val policies = KtorResiliencePolicies(
        circuitBreaker = CircuitBreaker.ofDefaults("products.route"),
        retry = Retry.ofDefaults("products.route"),
    )

    routing {
        resilientGet("/products", policies) {
            call.respondText("ok")
        }
    }
}
```

## Error Mapping

| Failure | HTTP status | Error code |
|---|---:|---|
| Open circuit | 503 | `circuit_breaker_open` |
| Rate limiter rejection | 429 | `rate_limited` |
| Time limiter timeout | 504 | `timeout` |

Messages are generic and safe for clients. Policy names remain in the caller-owned Resilience4j objects for metrics and registry ownership.

## Non-goals

- No global Ktor plugin or hidden registry.
- No application configuration binding.
- No authentication, logging, tracing, or OpenAPI integration.
- No client-specific facade.
