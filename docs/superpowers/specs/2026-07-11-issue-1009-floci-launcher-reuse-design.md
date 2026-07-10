# Issue 1009 Floci Launcher Reuse Design

## Problem

`FlociServer.Launcher.floci` is singleton-scoped to a Gradle test JVM, but it
currently constructs `FlociServer()` with the public constructor default of
`reuse=true`. On developer machines that opted into Testcontainers reuse, two
module JVMs can attach to an old Docker container instead of creating their
own isolated Floci instance.

## Decision

Keep the public `FlociServer` constructor default unchanged for compatibility.
Change only the standard test Launcher to construct `FlociServer(reuse = false)`.
The Launcher remains lazy and JVM-scoped, so all tests in one module keep one
container while separate module test JVMs receive independent containers.

## Alternatives

1. Change the public default to `false`: rejected because direct local developer
   use is a broader API behavior change.
2. Disable reuse only in AWS Gradle tasks: rejected because every consumer of
   the shared Launcher needs the same invariant.
3. Change the Launcher only: selected because it targets the shared test path
   without removing explicit local reuse from direct construction.

## Boundaries

- This slice changes `FlociServer` only; other server wrappers remain follow-up
  work under issue #1009.
- It does not relax any repository test mutex or change CI concurrency.
- AWS will consume the resulting `1.11.1-SNAPSHOT` artifact before its bounded
  Floci parallelism change.

## Acceptance Criteria

- `FlociServer.Launcher.floci` is created with `reuse=false`.
- Launcher startup and `ShutdownQueue` registration remain unchanged.
- The existing Floci test suite continues to prove real container startup.
- The current snapshot can be consumed by `bluetape4k-aws` without a catalog
  version change.
