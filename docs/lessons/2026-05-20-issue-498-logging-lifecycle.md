# Lessons Learned - Issue #498 KLoggingChannel Lifecycle

## Context

`KLoggingChannel` needed explicit lifecycle ownership for tests and reloadable applications. The old test suite only verified no-throw logging calls and did not prove delivery or collector shutdown.

## Decision

`KLoggingChannel` now implements `AutoCloseable`, uses one shared default runtime scope/shutdown hook, and exposes `closeAndJoin()` for suspend cleanup boundaries. Custom scopes remain caller-owned.

## Outcome

Tests now capture Logback events, assert level/message/error delivery, and verify collector cancellation plus post-close event dropping. The collector starts `UNDISPATCHED` to avoid dropping first events before `MutableSharedFlow` subscription is established.

## Verification

- IDE imports optimized for changed Kotlin files.
- IDE diagnostics: index ready, no build errors; per-file fresh analysis unavailable because files were not open in the IDE.
- `./gradlew :bluetape4k-logging:compileKotlin :bluetape4k-logging:compileTestKotlin --no-configuration-cache`
- `./gradlew :bluetape4k-logging:test --tests 'io.bluetape4k.logging.coroutines.KLoggingChannelTest' --no-configuration-cache`
- `./gradlew :bluetape4k-logging:test --no-configuration-cache`
- `git diff --check`

## Future Guard

For async logging tests, attach a real appender and assert emitted events. Avoid `Thread.sleep` drain tests; prove lifecycle state through the collector job or observable output.
