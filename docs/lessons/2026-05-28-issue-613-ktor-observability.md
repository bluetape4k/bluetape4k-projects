# Issue 613 - Ktor observability baseline helpers

## Context

Issue #613 adds reusable observability helpers after `bluetape4k-ktor-core` was
merged. The target is production diagnostics without hiding application-owned
policy such as metric registry, exporter, and tracing backend choices.

## Decision

Provide explicit Ktor-native helpers:

- `installBluetape4kKtorObservability()` installs `CallId`, `CallLogging`, and
  optional `MicrometerMetrics`.
- `KtorCorrelationId` sanitizes inbound correlation IDs before MDC or response
  propagation.
- `prometheusScrapeRoute()` exposes a route only when the application supplies a
  `PrometheusMeterRegistry`.

Micrometer core is an API dependency because the installer configuration accepts
`MeterRegistry`. Prometheus registry stays compile-only for main code and is
documented as application-owned because only the optional scrape helper requires
it.

## Outcome

The module avoids global mutable registries and does not install tracing by
default. OpenTelemetry policy remains application-owned or a future narrower
module decision.

## Verification

- `./gradlew :bluetape4k-ktor-observability:compileKotlin :bluetape4k-ktor-observability:compileTestKotlin`
- `./gradlew :bluetape4k-ktor-observability:test :bluetape4k-ktor-observability:koverXmlReport`
- Kover XML: line coverage 88/92 (95.7%).

## Future Guard

Do not echo caller-supplied correlation headers directly. New observability
helpers should either sanitize inbound values first or leave propagation to the
application.
