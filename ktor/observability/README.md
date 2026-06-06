# bluetape4k-ktor-observability

[English](./README.md) | [한국어](./README.ko.md)

Ktor observability helpers for the bluetape4k ecosystem.

## Component Diagram

![Ktor Observability Components](../../docs/images/readme-diagrams/ktor-observability-component-01.png)

## Features

- `installBluetape4kKtorObservability()` for explicit Ktor observability setup.
- Sanitized correlation ID propagation through Ktor `CallId`.
- `CallLogging` MDC integration with query-string-free default log messages.
- Micrometer `MicrometerMetrics` installation when an application supplies a `MeterRegistry`.
- Optional Prometheus scrape route backed by an application-owned `PrometheusMeterRegistry`.
- Optional OpenTelemetry server tracing backed by an application-owned `OpenTelemetry` instance.

## Dependency

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-ktor-observability")

    // Required only when Prometheus scrape routing is used.
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Required only when OpenTelemetry Ktor tracing helpers are used.
    implementation("io.opentelemetry.instrumentation:opentelemetry-ktor-3.0")
}
```

## Micrometer and Prometheus Usage

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

## OpenTelemetry Tracing Usage

Tracing is explicit and opt-in. Create the OpenTelemetry SDK, exporters, and
propagators in the application, then pass the resulting `OpenTelemetry`
instance to the helper.

```kotlin
import io.bluetape4k.ktor.observability.Bluetape4kKtorObservabilityConfig
import io.bluetape4k.ktor.observability.KtorOpenTelemetryTracingConfig
import io.bluetape4k.ktor.observability.installBluetape4kKtorObservability
import io.ktor.server.application.Application
import io.opentelemetry.api.OpenTelemetry

fun Application.module(openTelemetry: OpenTelemetry) {
    installBluetape4kKtorObservability(
        Bluetape4kKtorObservabilityConfig(
            tracing = KtorOpenTelemetryTracingConfig(
                openTelemetry = openTelemetry,
                captureSanitizedCorrelationId = true
            )
        )
    )
}
```

`captureSanitizedCorrelationId` records only the sanitized `correlation.id`
trace attribute. It does not record the raw request header. A bounded
`correlation.present` attribute is recorded on traced requests.

## Dependency Policy

The module installs Ktor `CallId`, `CallLogging`, and `MicrometerMetrics`
explicitly. Applications still own the actual `MeterRegistry`, exporters, and
tracing backend. OpenTelemetry tracing is not installed by default; add the
`opentelemetry-ktor-3.0` instrumentation dependency only when tracing helpers
are used. That instrumentation is versioned from the OpenTelemetry alpha BOM, so
keep the application-owned telemetry setup easy to upgrade.

Incoming correlation IDs are never echoed directly. They are trimmed, filtered
to safe characters, capped, and only then added to MDC or response headers.
