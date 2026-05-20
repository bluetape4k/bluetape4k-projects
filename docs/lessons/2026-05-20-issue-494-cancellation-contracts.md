# Issue 494 Cancellation Contracts

Date: 2026-05-20
Issue: #494

## Context

Coroutine wrappers in multiple modules needed a reusable way to prove that
`CancellationException` is rethrown, waiter slots are cleared after cancellation,
and underlying resources such as Retrofit calls are cancelled.

## Decision

Add cancellation contract helpers to `bluetape4k-junit5` instead of copying
launch/cancel scaffolding into each module. Keep the helper signatures on core
coroutine and `Duration` types so they remain usable from normal coroutine tests.

## Outcome

- Added `resultOfNonCancellation` and `runCatchingNonCancellation` for
  `Result`-style wrappers that must preserve structured cancellation.
- Added helper assertions for propagation, cancelled waiter cleanup, and
  resource cancellation.
- Replaced local cancellation scaffolding in `bluetape4k-coroutines` and
  `bluetape4k-micrometer`.
- Added a real MockWebServer delayed-response cancellation test for
  `bluetape4k-retrofit2`.
- Documented usage and checklist in the English and Korean JUnit5 READMEs.

## Verification

- `./gradlew :bluetape4k-junit5:compileKotlin :bluetape4k-junit5:test --console=plain --no-configuration-cache`
- `./gradlew :bluetape4k-coroutines:test --tests '*ResumableTest' --console=plain --no-configuration-cache`
- `./gradlew :bluetape4k-micrometer:test --tests '*ObservationCoroutinesSupportTest' --console=plain --no-configuration-cache`
- `./gradlew :bluetape4k-retrofit2:test --tests '*SuspendRetrofitCallSupportTest' --console=plain --no-configuration-cache`
- `git diff --check`

## Future Guidance

When adding suspend wrappers that return `Result`, do not use plain
`runCatching` around suspend calls. Use `runCatchingNonCancellation`, or rethrow
`CancellationException` explicitly before converting non-cancellation failures.
