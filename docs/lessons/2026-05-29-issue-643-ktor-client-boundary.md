# Issue 643 Ktor Client Boundary

## Context

Issue #643 asked whether `bluetape4k-projects` should introduce a dedicated `bluetape4k-ktor-client` module or keep Ktor client support in an existing module.

## Decision

Keep Ktor client ownership in `bluetape4k-http`. Add only thin helpers for explicit-engine client creation, Kotlinx JSON content negotiation, and timeout defaults.

Avoid a separate Ktor client module until there is a broader API surface that cannot fit the HTTP module without dependency or ownership confusion.

## Outcome

`KtorHttpClientSupport` now exposes:

- `defaultKtorClientJson`
- `KtorClientTimeouts`
- `ktorJsonHttpClientOf`
- `ktorCioJsonHttpClientOf`

The README locale set documents that retry, resilience, authentication, logging, and service-specific plugins remain application-level concerns or belong in existing dedicated modules.

## Verification

- `./gradlew :bluetape4k-http:test --tests 'io.bluetape4k.http.ktor.KtorHttpClientSupportTest' --no-configuration-cache`

## Future Guard

When adding Ktor client helpers, require a clear cross-application pattern first. Prefer explicit engine selection and narrow plugin installation over a broad facade.
