# Issue #744: Suspending observation lifecycle

## Context

`Observation.withObservationContextSuspending` bound Micrometer observations to a
coroutine context but only stopped observations from catch blocks. Successful
suspend paths could complete without `onStop`.

## Decision

Call `observation.stop()` from `finally` for suspending observation context
helpers. Keep cancellation separate from non-cancellation errors: cancellation
rethrows without recording an error, and non-cancellation exceptions still call
`observation.error(e)` before the final stop.

## Verification

- A pre-fix regression failed with `handler.stopped == 0`.
- The same focused regression passed after the fix.
- Related observation coroutine and event telemetry tests passed.
- Full `:bluetape4k-micrometer:test` passed.

## Future Guard

Any helper that starts or owns an `Observation` must stop it in `finally`.
Tests should assert handler `onStop` counts for success paths, not only current
context cleanup.
