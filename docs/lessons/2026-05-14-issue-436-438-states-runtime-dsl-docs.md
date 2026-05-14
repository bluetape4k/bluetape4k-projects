## Issue 436-438 States Runtime DSL Docs

Context: Issues #436, #437, and #438 extended `utils/states` after the
StateMachine comparison in #251.

Decision: Keep `bluetape4k-states` Kotlin/JVM focused. Add an optional reactive
runtime in a separate package, and implement nested state-family transitions by
extending transition resolution instead of replacing the existing flat DSL.

Outcome: Added inherited transition resolution, `reactiveStateMachine`, one-time
effects, lifecycle side effects, keyed side-effect restart control, tests, and
English/Korean README positioning guidance. PR review then tightened reactive
lifecycle behavior: follow-up events are queued asynchronously, transition
cancellation still restarts target side effects, `close()` cancels active side
effects and rejects later sends, and side-effect registry updates are protected
by an explicit lock.

Verification: Ran IDE diagnostics on touched Kotlin files with zero errors. Ran
`./gradlew :bluetape4k-states:test --no-configuration-cache`; result was
57 passing and BUILD SUCCESSFUL. Claude follow-up review found no blocking,
high, or medium issues after the lifecycle fixes.

Future agents: Preserve exact transition precedence over inherited transitions.
Keep reactive runtime optional; do not move UI/Compose or KMP concerns into this
module without a separate design issue. For reactive side effects, keep
`close()` and `restartSideEffects()` lifecycle updates coordinated; a thread-safe
map alone is not enough for compound check/cancel/replace operations.
