# Issue #879 Coroutines Test Bridge Removal

Issue #879 removed the deprecated `io.bluetape4k.coroutines.tests` bridge
package from production coroutines sources. The repository now uses the owner
modules directly: Flow assertions from `bluetape4k-assertions` and dispatcher
test helpers from `bluetape4k-junit5`.

## Decision

- Delete the bridge sources under `bluetape4k-coroutines`.
- Move `withSingleThread` and `withParallels` to
  `io.bluetape4k.junit5.coroutines`.
- Migrate coroutines tests and examples to
  `io.bluetape4k.assertions.coroutines`.
- Preserve the newer assertion module's cancellation contract by asserting
  `CancellationException` with `assertFailsWith` where needed.

## Lessons

- Deprecated bridge APIs can hide owner-module semantics. The old bridge
  accepted `CancellationException` through `assertError`, while the assertion
  module intentionally rethrows cancellation. Migration should preserve the
  owner module contract instead of recreating bridge behavior.
- Shared test helpers should avoid adding new module dependencies when moved.
  `bluetape4k-junit5` does not need `bluetape4k-core` just to validate
  `parallelism` or ignore executor shutdown failures.
- Full-package scans should distinguish active Kotlin sources from historical
  planning notes. The old package remains only in archived `docs/superpowers`
  context.

## Verification

- `./gradlew :bluetape4k-junit5:compileKotlin` passed.
- `./gradlew :bluetape4k-junit5:test :bluetape4k-assertions:test` passed.
- `./gradlew :bluetape4k-coroutines:compileTestKotlin :bluetape4k-coroutines:test` passed.
- `./gradlew :bluetape4k-examples-coroutines-demo:compileTestKotlin` passed; it still reports the pre-existing unused-expression warning in `TimeoutExamples.kt`.
- `./gradlew :bluetape4k-coroutines:compileTestKotlin --warning-mode all` had no old bridge package, `FlowAssertions`, or `TestSupport` warning hits.
- `rg "io\\.bluetape4k\\.coroutines\\.tests" -g '*.kt' -g '*.kts'` returned no active Kotlin/KTS matches.
- Test result XML totals: junit5 269 tests, assertions 689 tests, coroutines 566 tests; all failures/errors 0.
- `git diff --check` passed.
