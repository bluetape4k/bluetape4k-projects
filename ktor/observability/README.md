# bluetape4k-ktor-observability

[English](./README.md) | [한국어](./README.ko.md)

Ktor observability helpers for the bluetape4k ecosystem.

## Features

- `installBluetape4kKtorObservability()` for explicit Ktor observability setup.
- Sanitized correlation ID propagation through Ktor `CallId`.
- `CallLogging` MDC integration with query-string-free default log messages.
- Micrometer `MicrometerMetrics` installation when an application supplies a `MeterRegistry`.
- Optional Prometheus scrape route backed by an application-owned `PrometheusMeterRegistry`.

## Dependency

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-ktor-observability")

    // Required only when Prometheus scrape routing is used.
    implementation("io.micrometer:micrometer-registry-prometheus")
}
```

## Usage

```kotlin
import io.bluetape4k.ktor.observability.Bluetape4kKtorObservabilityConfig
import io.bluetape4k.ktor.observability.installBluetape4kKtorObservability
import io.bluetape4k.ktor.observability.prometheusScrapeRoute
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

fun Application.module(registry: PrometheusMeterRegistry) {
    installBluetape4kKtorObservability(
        Bluetape4kKtorObservabilityConfig(meterRegistry = registry)
    )

    routing {
        prometheusScrapeRoute(registry)
    }
}
```

## Dependency Policy

The module installs Ktor `CallId`, `CallLogging`, and `MicrometerMetrics`
explicitly. Applications still own the actual `MeterRegistry`, exporters, and
tracing backend. OpenTelemetry tracing is not installed by default in this
module; add a tracing bridge in the application or a narrower future module when
the exporter policy is known.

Incoming correlation IDs are never echoed directly. They are trimmed, filtered
to safe characters, capped, and only then added to MDC or response headers.
