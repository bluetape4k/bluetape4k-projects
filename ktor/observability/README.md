# bluetape4k-ktor-observability

[English](./README.md) | [한국어](./README.ko.md)

Explicit Ktor observability defaults for bluetape4k applications.

## Component Diagram

![Ktor Observability Components](../../docs/images/readme-diagrams/ktor-observability-component-01.png)

## Features

- `installBluetape4kKtorObservability()` installs the selected Ktor observability plugins explicitly.
- Ktor `CallId` propagates only sanitized correlation IDs.
- `CallLogging` writes MDC-backed request logs without query strings in the default message.
- `MicrometerMetrics` is installed only when the application supplies a `MeterRegistry`.
- `prometheusScrapeRoute()` exposes an optional scrape route backed by an application-owned `PrometheusMeterRegistry`.
- Optional OpenTelemetry server tracing uses the application-owned `OpenTelemetry` instance.
- The combined installer shares `Bluetape4kKtorObservabilityConfig.correlationId` with CallId, CallLogging, and tracing by default.
- Set `KtorOpenTelemetryTracingConfig.correlationId` only when tracing needs an explicit header or length policy override.

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
trace attribute. Raw request headers are not recorded. Traced requests always
record the bounded `correlation.present` attribute.

The combined installer applies one correlation policy to CallId, CallLogging,
response propagation, and tracing. A tracing configuration without
`correlationId` inherits the top-level policy, including its request header and
`maxLength`. Set `KtorOpenTelemetryTracingConfig.correlationId` to keep a
deliberate trace-specific policy separate from the application correlation ID.

## Dependency Policy

The module installs Ktor `CallId`, `CallLogging`, and `MicrometerMetrics`
explicitly. Applications still own the actual `MeterRegistry`, exporters, and
tracing backend. OpenTelemetry tracing is not installed by default; add the
`opentelemetry-ktor-3.0` instrumentation dependency only when tracing helpers
are used. That instrumentation is versioned from the OpenTelemetry alpha BOM, so
keep the application-owned telemetry setup easy to upgrade.

Incoming correlation IDs are never echoed directly. They are trimmed, filtered
to safe characters, capped, and only then added to MDC or response headers.
