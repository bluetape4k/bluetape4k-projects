# Issue 790: Jackson2 typed mapper allowlist preservation

## Context

`Jackson.createTypedJsonMapper(...)` built a `BasicPolymorphicTypeValidator`, then replaced Jackson's configured default typing resolver with a raw `StdTypeResolverBuilder`. That removed the validator from the actual polymorphic deserialization path and allowed a root `Any` payload with an external `@class` id.

## Decision

Use Jackson's `activateDefaultTypingAsProperty(...)` so the configured validator stays attached to property-based `@class` type ids. For the safe factory, validate package prefixes with `allowIfSubType(...)` instead of `allowIfBaseType(...)`, because allowing a base type can approve every legal subtype once the nominal base type matches.

## Outcome

- Disallowed root `Any` polymorphic payloads now fail with `InvalidTypeIdException`.
- Disallowed nested payloads still fail, and allowed package payloads still round-trip with `@class` type ids.
- `typedJsonMapper` remains only a deprecated legacy compatibility mapper and is no longer promoted in examples for untrusted JSON.

## Verification

- Red test before implementation: root `Any` payload with `com.example.disallowed.DisallowedTypedPayload` did not throw.
- `./gradlew :bluetape4k-jackson2:test --tests "io.bluetape4k.jackson.JacksonTest.createTypedJsonMapper*" --no-daemon --no-configuration-cache`
- `./gradlew :bluetape4k-jackson2:test --tests "io.bluetape4k.jackson.JacksonTest" --no-daemon --no-configuration-cache`
- `./gradlew :bluetape4k-jackson2:test --no-daemon --no-configuration-cache`
- `./gradlew :bluetape4k-jackson2:compileTestKotlin --warning-mode all --rerun-tasks --no-daemon --no-configuration-cache`
- `git diff --check`

## Future Guard

Do not call `setDefaultTyping(...)` with a raw `StdTypeResolverBuilder` after installing a `PolymorphicTypeValidator`; it can silently bypass the validator. For package allowlists, include a root `Any` payload regression test, not only an envelope or nested-property test.

## Concurrency Helper Gate

`MultithreadingTester`, `SuspendedJobTester`, and `StructuredTaskScopeTester` were not applicable here. This fix does not add concurrency, coroutine, virtual-thread, or structured-task behavior; it narrows synchronous Jackson default typing validation.
