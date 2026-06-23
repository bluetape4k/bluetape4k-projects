# Issue #851 Mutiny Coroutine `asUni` Cold Review

## Scope

- `utils/mutiny/src/main/kotlin/io/bluetape4k/mutiny/CoroutineSupport.kt`
- `utils/mutiny/src/test/kotlin/io/bluetape4k/mutiny/CoroutineSupportTest.kt`
- `utils/mutiny/README.md`
- `utils/mutiny/README.ko.md`

## Verdict

Local 7-tier equivalent review: APPROVE.

P0/P1 findings: 0.

## Review Notes

| Lens | Finding | Severity | Resolution |
|---|---|---:|---|
| Correctness | `async { ... }.asUni()` started work when the Uni was created, not subscribed. | P1 | Deferred creation now happens inside `Uni.createFrom().emitter`, which Mutiny runs at subscription time. |
| Cancellation | Subscriber cancellation must stop the coroutine job. | P1 | `emitter.onTermination` cancels the active `Deferred`. |
| Failure semantics | Coroutine exceptions and cancellation must reach Mutiny subscribers. | P1 | `invokeOnCompletion` forwards item, failure, and cancellation to the emitter. |
| API documentation | Callers need to know whether `asUni` is cold or hot. | P2 | KDoc and EN/KO README now state that `asUni` is cold and subscription-cancellable. |
| Test evidence | Existing tests only proved eventual completion. | P1 | Added cold-start, subscription-cancel, and failure/cancellation propagation regressions. |

## Verification

- RED: `./gradlew :bluetape4k-mutiny:test --tests "io.bluetape4k.mutiny.CoroutineSupportTest.asUni does not start suspend block before subscription" --no-build-cache` failed with `Expected <1> to equal to <0>`.
- GREEN targeted: same module task with cold-start, cancellation, and failure/cancellation tests passed with 3 tests.
- Module: `./gradlew :bluetape4k-mutiny:compileKotlin :bluetape4k-mutiny:compileTestKotlin :bluetape4k-mutiny:test --no-build-cache` passed with 29 tests.
- Build: `./gradlew :bluetape4k-mutiny:build --no-build-cache` passed.
