# Issue 542 Near Cache Close Failure

## Context

`ResilientNearCacheDecorator.close()` ignored delegate close failures with bare
`runCatching`, making shutdown resource leaks silent.

## Decision

Align the blocking decorator with the suspend decorator: delegate `close()` is
called directly, and non-fatal failures are logged with the cache name while the
close path remains best-effort. Keep lifecycle log messages in English so ops
teams can grep them consistently.

## Outcome

The close path now emits a warning instead of silently discarding the failure.
A targeted unit test locks both the non-throwing lifecycle behavior and the
warning log message.

## Verification

`./gradlew :bluetape4k-cache-core:test --tests "io.bluetape4k.cache.nearcache.ResilientNearCacheDecoratorTest"` passed with 9 tests.

Claude Code Opus review initially flagged the missing log assertion as P2.
After adding `InMemoryLogbackAppender` coverage and changing the message to
English, rereview reported no remaining P0/P1/P2 findings.

## Future Guard

Do not use bare `runCatching { close() }` in resource lifecycle code unless the
failure is intentionally logged or otherwise observable. Tests should lock the
observable lifecycle signal, not just the absence of an exception.
