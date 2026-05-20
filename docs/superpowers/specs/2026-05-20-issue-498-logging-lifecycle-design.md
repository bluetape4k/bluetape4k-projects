# Issue #498 KLoggingChannel Lifecycle Design

## Context

`KLoggingChannel` currently creates one `CoroutineScope(SupervisorJob() + Dispatchers.IO)` and one JVM shutdown hook per logger instance. This keeps companion-object usage simple, but tests and reloadable applications cannot explicitly stop the collector job.

## Goals

- Preserve existing `companion object : KLoggingChannel()` source compatibility.
- Add an explicit lifecycle surface that applications and tests can close.
- Avoid one JVM shutdown hook per logger instance.
- Document when to use `KLoggingChannel` instead of `KLogging`.
- Verify collector cancellation and observable log delivery with assertions.

## Design

- Make `KLoggingChannel` implement `AutoCloseable`.
- Keep the no-arg constructor by adding a default `CoroutineScope` parameter.
- Use a private `KLoggingChannelRuntime` registry for the default shared `SupervisorJob + Dispatchers.IO` scope and a single shutdown hook.
- Keep custom scope ownership external: `close()` cancels this channel collector, not the injected scope.
- Add `closeAndJoin()` for suspend shutdown/test boundaries that need deterministic collector termination.
- After close, `send()` drops events instead of suspending or restarting the collector.

## Compatibility

- Existing companion-object declarations continue to compile.
- Existing suspend logging methods keep the same names and signatures.
- New public API is additive: `isClosed`, `close()`, and `closeAndJoin()`.

## Non-Goals

- Do not replace `MutableSharedFlow` with a new queue implementation in this issue.
- Do not add a Spring-specific integration hook until a concrete Spring lifecycle adapter issue exists.
- Do not add dependencies.

## Verification

- Compile `:bluetape4k-logging`.
- Run `KLoggingChannelTest`.
- Run full `:bluetape4k-logging:test`.
- Review diff in the current Codex session per user instruction.
