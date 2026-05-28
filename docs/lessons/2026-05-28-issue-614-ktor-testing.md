# Issue 614 - Ktor testing helpers

## Context

`bluetape4k-ktor-core` and `bluetape4k-ktor-observability` introduced reusable
Ktor server helpers, but tests still repeated JSON decoding, status assertions,
and standard API error payload checks.

## Decision

Add a small `bluetape4k-ktor-testing` API surface:

- `installBluetape4kKtorCoreForTest` keeps Ktor `testApplication` ownership in
  the test while reducing core setup boilerplate.
- `bluetape4kJsonClient` uses the same JSON defaults as `bluetape4k-ktor-core`.
- `decodeJsonBody`, `shouldHaveStatus`, `shouldHaveJsonBody`, and
  `shouldHaveApiError` centralize response assertions.
- `bluetape4kJsonMockEngine` covers one-response JSON client tests without
  adding a larger mocking abstraction.

## Outcome

The idgenerator Ktor example now consumes the shared response helpers instead
of hand-decoding every response with a local `Json` instance.

## Verification

- `./gradlew :bluetape4k-ktor-testing:compileKotlin :bluetape4k-ktor-testing:compileTestKotlin :idgenerator-ktor-demo:compileTestKotlin`
- `./gradlew :bluetape4k-ktor-testing:test :bluetape4k-ktor-testing:koverXmlReport :idgenerator-ktor-demo:test`
- `git diff --check`

## Future guard

Keep this module focused on test-scoped helpers. Do not wrap the full Ktor test
lifecycle unless repeated consumer tests prove a specific lifecycle abstraction
is worth the loss of explicit setup.
