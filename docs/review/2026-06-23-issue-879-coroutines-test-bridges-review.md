# Issue #879 Coroutines Test Bridge Removal 검토

## Scope

- `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/tests/FlowAssertions.kt`
- `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/tests/TestSupport.kt`
- `bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/tests/FlowAssertionsBridgeTest.kt`
- `testing/junit5/src/main/kotlin/io/bluetape4k/junit5/coroutines/CoroutineSupport.kt`
- coroutines test imports under `bluetape4k/coroutines/src/test/kotlin`
- coroutines demo test imports under `examples/coroutines-demo/src/test/kotlin`

## Verdict

Local 7-tier equivalent review: APPROVE.

P0/P1 findings: 0.

## Review Notes

| Lens | Finding | Severity | Resolution |
|---|---|---:|---|
| API ownership | Deprecated Flow assertion bridges lived in `bluetape4k-coroutines` production sources. | P2 | Deleted the bridge file and migrated call sites to `io.bluetape4k.assertions.coroutines`. |
| API ownership | Dispatcher test helpers also lived under the removed bridge package. | P2 | Moved `withSingleThread` and `withParallels` to `io.bluetape4k.junit5.coroutines`. |
| Dependency boundary | Moving helpers to `junit5` could accidentally introduce a `core` dependency through support helpers. | P1 | Kept validation and cleanup local with `require` and `runCatching`. |
| Cancellation semantics | Assertion-module `assertError` intentionally rethrows `CancellationException`. | P1 | Updated the source-cancellation test to assert cancellation directly with `assertFailsWith`. |
| Migration completeness | Old-package imports could remain in tests or examples. | P1 | Active Kotlin/KTS scan for `io.bluetape4k.coroutines.tests` returns no matches. |
| Documentation drift | Historical design notes still mention the removed package. | P3 | Left archived `docs/superpowers` references intact and documented the distinction in the lesson. |

## Verification

- `./gradlew :bluetape4k-junit5:compileKotlin` passed.
- `./gradlew :bluetape4k-junit5:test :bluetape4k-assertions:test` passed.
- `./gradlew :bluetape4k-coroutines:compileTestKotlin :bluetape4k-coroutines:test` passed.
- `./gradlew :bluetape4k-examples-coroutines-demo:compileTestKotlin` passed with only the existing `TimeoutExamples.kt` unused-expression warning.
- `./gradlew :bluetape4k-coroutines:compileTestKotlin --warning-mode all` produced no old bridge package or bridge file warning hits.
- `rg "io\\.bluetape4k\\.coroutines\\.tests" -g '*.kt' -g '*.kts'` returned no active Kotlin/KTS matches.
- Full `rg "io\\.bluetape4k\\.coroutines\\.tests|FlowAssertionsBridgeTest"` now reports only historical `docs/superpowers` references.
- Test result XML totals: junit5 269 tests, assertions 689 tests, coroutines 566 tests; all failures/errors 0.
- `git diff --check` passed.
