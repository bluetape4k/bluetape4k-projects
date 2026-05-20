# Issue 544 FlowEvent Value-Class Evaluation

## Context

Issue #544 asked whether `FlowEvent.Value` and `FlowEvent.Error` should move
from data classes to Kotlin/JVM value classes to reduce hot-path allocations in
Flow operators.

## Decision

Keep both wrappers as data classes. Kotlin/JVM value classes can implement
interfaces, but they are boxed whenever they are used as another type. The
current API emits and consumes these events as `FlowEvent<T>`, so a value-class
implementation would still allocate in the important interface-typed path.

## Outcome

The old TODO comments were replaced with explicit KDoc decisions, and tests now
lock the source conveniences that would disappear with value classes:
destructuring through `component1()` and `copy()`.

## Verification

- Kotlin official documentation for inline value classes was checked for
  interface inheritance and boxing rules.
- `rg` and IDE reference lookup were used to inspect `FlowEvent` call sites;
  the IDE was partially unavailable due dumb mode, so `rg` was the source of
  truth for current usage.

## Future Agents

Do not revisit this as a plain `@JvmInline value class ... : FlowEvent<T>`
change. Reopen only if the public API can avoid the `FlowEvent<T>` interface
upcast on hot paths, or if Kotlin/JVM changes boxing behavior for interface
and generic use.
