# Issue 612 - Ktor core baseline helpers

## Context

Issue #612 implements the first reusable Ktor runtime API after the Ktor module
family was scaffolded in #611. The idgenerator Ktor example showed repeated
manual JSON, `StatusPages`, health route, and query parameter validation code.

## Decision

Provide a small explicit Ktor-native core surface:

- `installBluetape4kKtorCore()` for opt-in baseline installation.
- `Bluetape4kKtorJson.defaultJson()` for shared JSON defaults.
- `ApiErrorResponse`, `HealthResponse`, and health/readiness routes.
- `StatusPagesConfig.bluetape4kErrorResponses()` with cancellation rethrow.
- route parameter helpers that throw `IllegalArgumentException` so default
  status pages map caller input failures to HTTP 400.

The module exposes Ktor and kotlinx serialization dependencies as `api` because
the public API mentions `Json`, `StatusPagesConfig`, and Ktor application types.

## Outcome

The first core API is intentionally framework-light: no Spring Boot
auto-configuration, no hidden application routes beyond health/readiness, and no
client helpers. The example migration stays in #615 after the shared API settles.

## Verification

- `./gradlew :bluetape4k-ktor-core:compileKotlin :bluetape4k-ktor-core:compileTestKotlin`
- `./gradlew :bluetape4k-ktor-core:test :bluetape4k-ktor-core:koverXmlReport`
- Kover XML: line coverage 90/98 (91.8%).

## Future Guard

When adding more Ktor shared helpers, keep the installer explicit and avoid
installing Ktor plugins that applications commonly customize unless they are
individually switchable in config.
