# Issue #609 Ktor Module Family Design

Date: 2026-05-28 Repo: `bluetape4k-projects`
Issues: #609, #610

## Context

`bluetape4k-projects` already contains Ktor dependencies and one Ktor example group, but reusable Ktor server behavior is still copied across examples and sibling bluetape4k repositories. The first 1.10.0 slice should provide a server-side Ktor foundation inside this repository before any client, resilience, OpenAPI, or auth/security modules are promoted from backlog.

The module family stays in `bluetape4k-projects` for the first iteration because the intended API is framework glue over existing core, logging, assertion, testing, and HTTP utilities. Keeping it here lets the first modules reuse current build conventions, version catalog aliases, CI, README locale rules, and release metadata without creating a new repository boundary before the API shape is proven.

## Current Evidence

- `settings.gradle.kts` registers module families through `includeModules`. Adding `includeModules("ktor", withBaseDir = true)` will produce published names such as `bluetape4k-ktor-core`, matching `spring-boot/*` naming.
- The repository version catalog currently pins Ktor to `3.5.0` and already has aliases for client core/CIO/mock, server core/CIO, server content negotiation, server status pages, serialization with kotlinx JSON, and server test host.
- Existing examples repeatedly install Ktor plugins and hand-code validation, status responses, JSON test clients, and health/management style routes.
- Sibling repositories already show useful patterns:
  `bluetape4k-aws` has Ktor plugin implementations and AWS examples,
  `bluetape4k-graph` has graph plugin runtime tests, `bluetape4k-leader` has a leader-management plugin, and workshop repos have DB-backed Ktor examples.
- Official Ktor documentation for 3.5.x keeps plugin installation explicit with
  `install(...)`, allows global or route-scoped plugin installation, documents
  `StatusPages` exception/status handlers, `ContentNegotiation` JSON setup,
  `CallLogging` MDC support, `MicrometerMetrics` registry and Prometheus scrape routes, and `testApplication` plus `createClient` for server tests.

## Goals

1. Add a small, reviewable server-side Ktor module family under `ktor/`.
2. Keep route behavior explicit while removing repeated framework setup.
3. Standardize JSON, error responses, health/readiness, observability, and test client helpers across Ktor examples.
4. Preserve coroutine cancellation and avoid hiding Ktor plugin installation.
5. Leave client, resilience, OpenAPI, and auth/security work as backlog modules until the server foundation proves stable extension points.

## Initial Module Set

### `ktor/core` -> `bluetape4k-ktor-core`

First implementation target for #612.

Responsibilities:

- Provide a shared kotlinx `Json` configuration for Ktor server/client testing integration.
- Provide an explicit baseline installer such as
  `Application.installBluetape4kKtorCore(...)`.
- Install and configure `ContentNegotiation` only when the caller opts in.
- Provide an API error response model and `StatusPages` mapping helpers.
- Provide small route helpers for `/healthz` and `/readyz`.
- Provide request/query/path validation helpers only where they remove repeated boilerplate without hiding route-specific rules.

Dependencies:

- Ktor server core.
- Ktor server content negotiation.
- Ktor server status pages.
- Ktor serialization kotlinx JSON.
- Existing bluetape4k core validation/logging utilities where applicable.

Non-responsibilities:

- No authentication or authorization framework.
- No client abstraction beyond test support.
- No OpenAPI generation.
- No resilience policy execution.

### `ktor/observability` -> `bluetape4k-ktor-observability`

Second implementation target for #613.

Responsibilities:

- Provide explicit CallId and CallLogging installation helpers.
- Define correlation-id extraction, validation, sanitization, length limit, MDC key, and response propagation rules.
- Provide a MicrometerMetrics installer that accepts the application-owned registry.
- Provide an optional Prometheus scrape route helper when Prometheus registry is on the application classpath.
- Integrate status/error logging with the `ktor/core` error model without leaking caller-controlled header values.

Dependencies:

- `bluetape4k-ktor-core`.
- Ktor CallId/CallLogging/Micrometer dependencies when catalog aliases exist.
- Micrometer core as needed; registry/exporter dependencies should stay application-owned or optional.

Non-responsibilities:

- No global meter registry ownership.
- No forced tracing/exporter choice.
- No unsanitized caller header echoing.

### `ktor/testing` -> `bluetape4k-ktor-testing`

Third implementation target for #614.

Responsibilities:

- Provide `testApplication` setup helpers for bluetape4k Ktor modules.
- Provide a JSON `createClient` helper using the same JSON defaults as
  `ktor/core`.
- Provide response decode helpers for kotlinx serialization.
- Provide assertions for status, JSON body, and API error response payloads.
- Provide optional `MockEngine` helpers only if the design keeps client testing in this module.

Dependencies:

- `bluetape4k-ktor-core`.
- Ktor server test host.
- Ktor client content negotiation and mock client if helper scope includes it.
- `bluetape4k-assertions`.

Non-responsibilities:

- No Testcontainers wrapper.
- No broad HTTP client production abstraction.
- No framework-specific DB fixture ownership.

## Backlog Modules

These issues remain outside the first 1.10.0 server foundation unless #610 is reopened with new evidence:

- #643 `feat(ktor): implement bluetape4k-ktor-client`
- #644 `feat(ktor): implement bluetape4k-ktor-resilience4j`
- #645 `feat(ktor): implement bluetape4k-ktor-openapi`
- #646 `feat(ktor): implement bluetape4k-ktor-auth`

## Public API Shape

Prefer explicit extension functions and small immutable configuration values. Do not require every configuration holder to be a `data class` when it must carry non-serializable framework objects such as `Json`; keep serializable public value objects separate from runtime strategy objects.

```kotlin
class Bluetape4kKtorCoreConfig(
    val json: Json = Bluetape4kKtorJson.defaultJson(),
    val installContentNegotiation: Boolean = true,
    val installStatusPages: Boolean = true,
    val healthPath: String = "/healthz",
    val readinessPath: String = "/readyz",
)

fun Application.installBluetape4kKtorCore(
    config: Bluetape4kKtorCoreConfig = Bluetape4kKtorCoreConfig(),
)
```

The concrete names can change during #612, but the contract should stay:
explicit installation, immutable options, public KDoc, no framework-global mutable state, and route-specific behavior left in the application route. Any public `data class` introduced by implementation must implement `Serializable`
and carry only fields that make that contract honest.

## Error Model

`ktor/core` should define a small serializable error response model, for example:

```kotlin
@Serializable
data class ApiErrorResponse(
    val code: String,
    val message: String,
    val details: Map<String, String> = emptyMap(),
) : Serializable
```

Mapping rules:

- Caller validation failures map to `400 Bad Request`.
- Missing resources map to `404 Not Found` only when the route chooses that semantic.
- Unexpected exceptions map to `500 Internal Server Error` without exposing the raw exception message by default.
- `CancellationException` must be rethrown before broad exception handling.

## Observability Contract

Correlation-id handling must be conservative:

- Accept configured request headers such as `X-Request-ID` or `X-Correlation-ID`.
- Trim, validate against a safe character set, and cap length before MDC or response propagation.
- Generate a server-side id when no valid inbound id exists.
- Never log or echo unsanitized inbound values.

Metrics:

- The application owns the `MeterRegistry`.
- Helpers may install `MicrometerMetrics` with a provided registry and optional timers/tags.
- Prometheus support should be a route helper over an application-provided
  `PrometheusMeterRegistry`, not a global registry factory.

## Testing Contract

Ktor `testApplication` runs requests internally without binding a real socket. The testing module should standardize setup and assertions while preserving ordinary Ktor test readability:

- Use `createClient` for JSON-capable clients.
- Reuse `ktor/core` JSON defaults.
- Use bluetape4k assertion APIs in tests.
- Keep real IO/Testcontainers tests outside this module unless a later issue proves a reusable fixture belongs here.

## Module Registration And Release Rules

New modules must update the full registration chain:

- `settings.gradle.kts` with `includeModules("ktor", withBaseDir = true)`.
- `ktor/*/build.gradle.kts`.
- Root README and README.ko module lists.
- Repo-local `AGENTS.md` module group list.
- CI path filters/jobs and Nightly coverage where required.
- Coverage artifact naming and summary `needs`.
- `./gradlew projects` proof.

## Non-Goals

- No separate repository in the first slice.
- No replacement of Ktor's plugin system.
- No hidden framework auto-install through global mutable state.
- No broad authentication/security framework before repeated requirements are captured.
- No KMP/Kotlin Native target in the initial server-focused modules.
- No Ktor client production abstraction in the server foundation.

## Acceptance Criteria Mapping

- Initial module set is `ktor/core`, `ktor/observability`, and `ktor/testing`.
- The first implementation slice is #611 scaffold plus #612 core, with observability/testing split into later PRs.
- Coroutine cancellation and explicit plugin installation contracts are part of the spec.
- Backlog split criteria are captured for client, resilience, OpenAPI, and auth.
- Official Ktor 3.5.x documentation has been checked for the plugin APIs used by the design.

## References

- Ktor server plugins: https://ktor.io/docs/server-plugins.html
- Ktor server serialization: https://ktor.io/docs/server-serialization.html
- Ktor status pages: https://ktor.io/docs/server-status-pages.html
- Ktor call logging: https://ktor.io/docs/server-call-logging.html
- Ktor Micrometer metrics: https://ktor.io/docs/server-metrics-micrometer.html
- Ktor server testing: https://ktor.io/docs/server-testing.html
