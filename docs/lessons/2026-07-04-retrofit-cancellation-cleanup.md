# Retrofit cancellation races must close delivered response bodies

## Context

Issue #948 found a Retrofit coroutine bridge race where a response could arrive
after coroutine cancellation without explicit response-body cleanup evidence.

## Decision

When cancellation wins before `onResponse`, close the Retrofit response body
before cancelling the continuation. Also close the delivered response from the
`resume` cancellation handler if cancellation wins after resume but before
dispatch.

## Verification

- `./gradlew :bluetape4k-retrofit2:test --tests 'io.bluetape4k.retrofit2.SuspendRetrofitCallSupportTest'`
- `git diff --check`

## Future guidance

Coroutine HTTP bridges should close response resources on every cancellation
race path. Prefer `resume(value) { ... }` cleanup handlers plus an explicit
`!cont.isActive` check before delivery.
