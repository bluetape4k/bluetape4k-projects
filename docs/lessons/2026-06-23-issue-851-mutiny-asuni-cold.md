# Issue #851 Mutiny Coroutine `asUni` Cold Contract

Issue #851 found that `CoroutineScope.asUni { ... }` created a `Deferred`
immediately and then converted it to a `Uni`. That made the suspend block hot:
creating the `Uni` was enough to start side effects even when no subscriber
existed.

## Decision

Create the coroutine inside `Uni.createFrom().emitter { ... }`, because Mutiny
invokes the emitter consumer on subscription. The bridge now creates a fresh
`Deferred` per subscriber, forwards completion/failure from `invokeOnCompletion`,
and cancels the `Deferred` when the Uni subscription terminates before the
coroutine completes.

## Lessons

- Reactive bridge helpers must match reactive demand semantics. A `Uni` factory
  that starts work before subscription is a side-effect leak.
- `Deferred.asUni()` has useful result and cancellation forwarding, but it is
  only cold if the `Deferred` itself is created at subscription time.
- Cancellation tests should prove both directions: cancelling the subscription
  cancels the coroutine, and coroutine cancellation/failure reaches the Uni.

## Verification

- RED: `./gradlew :bluetape4k-mutiny:test --tests "io.bluetape4k.mutiny.CoroutineSupportTest.asUni does not start suspend block before subscription" --no-build-cache` failed with `Expected <1> to equal to <0>`.
- GREEN targeted: `./gradlew :bluetape4k-mutiny:test --tests "io.bluetape4k.mutiny.CoroutineSupportTest.asUni does not start suspend block before subscription" --tests "io.bluetape4k.mutiny.CoroutineSupportTest.asUni cancellation cancels running coroutine" --tests "io.bluetape4k.mutiny.CoroutineSupportTest.asUni propagates failure and cancellation exceptions" --no-build-cache`
- Module: `./gradlew :bluetape4k-mutiny:compileKotlin :bluetape4k-mutiny:compileTestKotlin :bluetape4k-mutiny:test --no-build-cache` passed with 29 tests.
- Build: `./gradlew :bluetape4k-mutiny:build --no-build-cache` passed.
