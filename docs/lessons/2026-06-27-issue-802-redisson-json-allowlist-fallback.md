# Issue 802: Redisson JSON allow-list fallback boundary

## Context

`Jackson3Codec` and `Fastjson2Codec` advertised `allowedPackagePrefixes` as a Redis trust-boundary control, but malformed or non-JSON payloads could still fall through to the Fory fallback decoder. That made the documented allow-list weaker than the public API contract.

## Decision

When `allowedPackagePrefixes` is configured, JSON codec decode fallback is disabled by default. Existing trusted-internal behavior remains unchanged for `allowedPackagePrefixes = null`, and a caller must explicitly set `allowFallbackDecode = true` for a trusted migration window.

## Outcome

- Allow-listed `Jackson3Codec` and `Fastjson2Codec` now reject fallback-format binary payloads with `SecurityException`.
- `RedissonCodecs.jackson3(...)` and `RedissonCodecs.fastjson2(...)` inherit the safe default.
- README trust-profile guidance now documents the default rejection and explicit migration escape hatch.

## Verification

- Red test before implementation: allow-listed Jackson3/Fastjson2 binary fallback payload tests failed because no exception was thrown.
- `./gradlew :bluetape4k-redisson:compileTestKotlin --warning-mode all --no-daemon --no-configuration-cache`
- `./gradlew :bluetape4k-redisson:test --tests "io.bluetape4k.redis.redisson.codec.*CodecTest" --no-build-cache --no-daemon --no-configuration-cache`
- `./gradlew :bluetape4k-redisson:test --no-daemon --no-configuration-cache`
- `git diff --check`

## Future Guard

Do not describe a codec factory as `AllowListedTypes` unless all decode fallback paths either validate the same type boundary or are disabled by default. If a legacy migration path is needed, make it explicit in the API and in README trust-profile text.

## Concurrency Helper Gate

`MultithreadingTester`, `SuspendedJobTester`, and `StructuredTaskScopeTester` were not applicable here. The change does not add shared mutable state, coroutine lifecycle behavior, or structured task scope behavior; it only narrows synchronous decode fallback behavior at a Redis codec trust boundary.
