# JDK25 JoinUntil Interrupt Semantics

## Context

Issue #953 found that the JDK25 structured-scope `joinUntil` fallback converted
every `InterruptedException` into `TimeoutException` and cleared the caller's
interrupt status.

## Decision

Track only the scheduled timeout interrupt as timeout evidence. Preserve
pre-existing and external interrupts by rethrowing `InterruptedException` and
restoring the caller thread interrupt flag.

## Outcome

Timeout joins still throw `TimeoutException`, while cancellation or interrupt
signals from outside the timeout scheduler remain diagnosable.

## Verification

- `./gradlew :bluetape4k-virtualthread-jdk25:test --tests 'io.bluetape4k.concurrent.virtualthread.jdk25.Jdk25StructuredTaskScopeProviderTest' --tests 'io.bluetape4k.concurrent.virtualthread.jdk25.Jdk25StructuredTaskScopeProviderExtTest'`

## Future Guidance

Do not clear interrupt status in structured-concurrency helpers unless the code
can prove it created the interrupt signal being consumed.
