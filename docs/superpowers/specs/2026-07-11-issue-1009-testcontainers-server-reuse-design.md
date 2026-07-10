# Issue 1009 Testcontainers Server Reuse Design

## Problem

The Testcontainers module exposes 53 `XxxServer` wrappers with 111 public or
internal `reuse` parameters defaulting to `true`. On hosts that enable
Testcontainers reuse, direct construction and JVM-scoped Launchers can attach
to containers created by another Gradle process, defeating module isolation.

## Decision

Every Server wrapper defaults `reuse` to `false`. Container reuse remains
available only when a caller explicitly passes `reuse = true`. Launchers keep
their lazy singleton-per-JVM lifecycle and therefore continue sharing one
container inside a module while separate module JVMs receive independent
containers.

## Compatibility

This changes default runtime behavior but not source or binary signatures.
Callers intentionally relying on reusable containers must opt in explicitly.
CI and ordinary tests never opt in.

## Boundaries

- Change all Server wrappers in `bluetape4k-testcontainers`, not only Floci.
- Keep Launcher lifecycle and `ShutdownQueue` behavior unchanged.
- Do not change repository Test mutexes in this PR.
- Publish a replacement `1.11.1-SNAPSHOT` only after CI succeeds.

## Acceptance Criteria

- No production `reuse: Boolean = true` default remains in the module.
- No production path calls `withReuse(true)` implicitly.
- Explicit `reuse = true` remains supported for local development.
- A policy test prevents reintroducing reusable defaults.
- Focused and proportional module tests pass.
