# bluetape4k-ktor-core

[English](./README.md) | [한국어](./README.ko.md)

Baseline Ktor server helpers for the bluetape4k ecosystem.

## Features

- `Bluetape4kKtorJson.defaultJson()` for shared kotlinx serialization defaults.
- `installBluetape4kKtorCore()` for explicit Ktor server baseline installation.
- `ApiErrorResponse` and `StatusPagesConfig.bluetape4kErrorResponses()` for JSON error payloads.
- `/healthz` and `/readyz` helpers returning `HealthResponse`.
- Query and path parameter helpers for repeated Ktor route validation.

## Dependency

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-ktor-core")
}
```

## Usage

```kotlin
import io.bluetape4k.ktor.core.installBluetape4kKtorCore
import io.bluetape4k.ktor.core.intQueryParameter
import io.bluetape4k.ktor.core.requiredPathParameter
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.module() {
    installBluetape4kKtorCore()

    routing {
        get("/items/{type}") {
            val type = call.requiredPathParameter("type")
            val size = call.intQueryParameter("size", defaultValue = 10, range = 1..100)

            call.respond(mapOf("type" to type, "size" to size))
        }
    }
}
```

The default installer adds content negotiation, JSON error handling, and
health/readiness routes. Disable individual parts through
`Bluetape4kKtorCoreConfig` when an application already owns that Ktor plugin.
