# Issue 475 Core Null Assertions

## Context

Issue #475 tracks removing Kotlin `!!` from production sources in small module
slices. The first slice intentionally covered only `:bluetape4k-core`, because
the issue asks not to replace all 86 occurrences in one PR.

## Decision

Remove `!!` without changing public exception contracts. For deprecated
`assertXxx` map helpers, keep `AssertionError`; for `requireXxx` map helpers,
keep `IllegalArgumentException`. KDoc examples should use Elvis `error(...)`
instead of `!!`. `SingletonHolder` should use `checkNotNull` only for the
factory-consumed invariant inside the existing lock.

## Outcome

The core target files no longer contain `!!` in production source or examples.
Null map receiver tests were added for `assertHasKey`, `assertHasValue`,
`assertContains`, `requireHasKey`, `requireHasValue`, and `requireContains` to
lock the existing exception-type contracts.

## Verification

- `rg -n '!!'` over the issue #475 core target files returned no matches.
- IntelliJ reformat/import optimization succeeded for touched production and
  test files.
- IntelliJ reference lookup succeeded after indexing for the changed map helper
  APIs.
- `./gradlew :bluetape4k-core:compileKotlin :bluetape4k-core:test --console=plain --no-configuration-cache`
  passed with 1588 tests.
- Codex CLI final review reported P0/P1 none.
- Claude CLI review was skipped after the user directed not to call Claude due
  to recurring usage-limit failures.

## Future Guard

Continue #475 in module-sized PRs. Prefer contract-preserving local variables or
explicit `checkNotNull`/Elvis failures over broad rewrites, and add focused
tests when a `!!` removal touches an exception-type contract.
