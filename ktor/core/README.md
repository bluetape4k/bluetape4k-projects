# bluetape4k-ktor-core

[English](./README.md) | [한국어](./README.ko.md)

Small Ktor server defaults for bluetape4k applications.

## Architecture Diagram

![Ktor Core Architecture](../../docs/images/readme-diagrams/ktor-core-architecture-01.png)

## Features

- `Bluetape4kKtorJson.defaultJson()` provides shared kotlinx serialization defaults.
- `installBluetape4kKtorCore()` installs the baseline Ktor plugins explicitly.
- `ApiErrorResponse` and `StatusPagesConfig.bluetape4kErrorResponses()` produce consistent JSON error payloads.
- `/healthz` and `/readyz` routes return `HealthResponse.up()` by default.
- Query and path parameter helpers keep repeated Ktor route validation compact.
- `installApplicationResourceLifecycle()` closes application-owned synchronous resources on
  `ApplicationStopped`.

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
health/readiness routes. When an application already owns one of those Ktor
plugins, disable that part with `Bluetape4kKtorCoreConfig`.

## Application-owned resources

Install the resource lifecycle explicitly during application startup, then register only resources
owned by that application:

```kotlin
import io.bluetape4k.ktor.core.installApplicationResourceLifecycle

fun Application.module() {
    val resources = installApplicationResourceLifecycle()
    val client = createClient()

    resources.register(client)
}
```

The registry closes pending entries in reverse registration order and closes a late registration
immediately. Registration tokens and registry shutdown are idempotent: each entry is claimed at most
once even when they race. Close failures are isolated and reported with an opaque registration ID,
close phase, and fatal flag; exception messages, stack traces, class names, and resource strings are
not exposed.

Close actions run synchronously on the caller thread. Register only trusted startup-owned actions
that finish within a bounded time. The registry does not create a coroutine scope, dispatcher,
thread, timeout, retry, or backend-specific drain policy. If a resource is still used by a background
job, its adapter must perform a bounded drain before resource shutdown or otherwise guarantee that
the job cannot access the resource.

`ApplicationStopped` may still be raised after Ktor's application-job disposal times out, and its
handlers run outside that disposal timeout. The lifecycle therefore does not prove that every
application coroutine has completed. It is a graceful-shutdown facility only; `SIGKILL`, JVM crash,
and OOM paths do not guarantee cleanup. Keep exactly one lifecycle bridge as the registry's close
owner, and place dependent resources in one composite action or register them in dependency order.
