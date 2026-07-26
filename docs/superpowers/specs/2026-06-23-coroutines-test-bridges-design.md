# Coroutines Test Bridge Removal Design

## Problem

Issue #879 removes the deprecated `io.bluetape4k.coroutines.tests` helper surface from `bluetape4k-coroutines` main sources. The current repository still contains:

- `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/tests/FlowAssertions.kt`
- `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/tests/TestSupport.kt`
- coroutine tests and examples importing `io.bluetape4k.coroutines.tests.*`

The replacement owners already exist:

- Flow assertions: `testing/assertions/src/main/kotlin/io/bluetape4k/assertions/coroutines/FlowAssertions.kt`
- coroutine test dispatchers: `testing/junit5/src/main/kotlin/io/bluetape4k/junit5/coroutines`

## Constraints

- Keep production behavior unchanged; this is a test-helper API cleanup.
- Remove the deprecated package from coroutines production sources.
- Preserve supported migration paths:
    - `io.bluetape4k.assertions.coroutines.*`
    - `io.bluetape4k.junit5.coroutines.*`
- Keep public contributor artifacts in English.
- Do not include Gradle 10 cleanup.
- Do not run Testcontainers-backed tasks in parallel. This scope does not need Testcontainers.

## Current Evidence

- `rg "io\.bluetape4k\.coroutines\.tests"` finds bridge files plus test/example imports.
- `bluetape4k/coroutines/build.gradle.kts` already has test dependencies on
  `:bluetape4k-assertions` and `:bluetape4k-junit5`.
- `testing/junit5` currently has coroutine support but not `withSingleThread` or
  `withParallels`.
- `testing/assertions` already has duplicate-aware `assertResultSet`, unlike the deprecated bridge's set-based implementation.

## Approach

Chosen approach: move dispatcher helpers into `bluetape4k-junit5`, migrate all coroutines tests/examples to owner module imports, then delete the bridge files and bridge-only tests.

Rejected alternatives:

- Keep deprecated bridge for another cycle: rejected because issue #879 explicitly removes the compatibility surface for 1.11.0.
- Keep dispatcher helpers in `bluetape4k-coroutines` test sources only: rejected because the accepted owner package is `io.bluetape4k.junit5.coroutines`.

## Risks

- `assertResultSet` is now duplicate-aware. If tests fail after import migration, inspect whether the old set-based bridge masked duplicate-count bugs.
- Moving dispatcher helpers may require dependency direction checks. The current target module already depends on `bluetape4k-core`, so existing validation helpers remain available.
- Broad mechanical import replacement can hide stale references in docs. Final
  `rg` must distinguish historical design docs from active source/tests.

## Acceptance Criteria

- No active source/test/example imports remain for
  `io.bluetape4k.coroutines.tests`.
- `FlowAssertions.kt` is removed from `bluetape4k-coroutines`.
- `TestSupport.kt` no longer exists under `bluetape4k-coroutines` main sources.
- `withSingleThread` and `withParallels` compile from
  `io.bluetape4k.junit5.coroutines`.
- Flow assertion call sites compile against
  `io.bluetape4k.assertions.coroutines`.
- Existing targeted tests pass:
    - `:bluetape4k-junit5`
    - `:bluetape4k-assertions`
    - `:bluetape4k-coroutines`
    - `:examples:coroutines-demo`

## DoD

- Spec and plan committed before implementation.
- Targeted compile/test commands pass.
- `rg "io\.bluetape4k\.coroutines\.tests"` has no active source/test/example references outside deliberate historical docs.
- Local review records P0/P1 = 0.
- PR body ends with `## DoD Status` and PR metadata matches issue #879.
