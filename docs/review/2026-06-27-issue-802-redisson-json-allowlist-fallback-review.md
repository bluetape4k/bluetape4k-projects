# Issue 802 Review: Redisson JSON allow-list fallback boundary

## Scope

- `Jackson3Codec`
- `Fastjson2Codec`
- `RedissonCodecs` safe factories
- Redisson codec regression tests
- Redisson README locale pair

## Findings

No P0/P1 findings.

## Checks

- The default behavior for `allowedPackagePrefixes = null` still permits fallback decode for trusted-internal compatibility.
- The allow-listed constructor path rejects Fory fallback-format binary payloads by default.
- The explicit migration path requires `allowFallbackDecode = true`.
- `RedissonCodecs.jackson3(...)` and `RedissonCodecs.fastjson2(...)` reject fallback binary payloads.
- README English/Korean trust-profile text describes the safer default and migration exception.

## Verification Evidence

- Red test before implementation failed with `Expected SecurityException but no exception was thrown` for both JSON codecs.
- `:bluetape4k-redisson:compileTestKotlin --warning-mode all`: passed; only existing Gradle Kotlin DSL deprecation warnings were reported.
- Suggested codec tests: passed.
- Full `:bluetape4k-redisson:test`: 295 tests, 0 failures, 0 errors, 0 skipped.
- `git diff --check`: passed.

## Residual Risk

This patch does not introduce tagged/versioned JSON envelopes. The explicit `allowFallbackDecode = true` migration option remains broad and should only be used for trusted one-time migration reads.

## Concurrency Helper Gate

No multithreading, coroutine, or structured-task helper was added because the behavior is a synchronous decoder trust-boundary check without shared mutable state or async lifecycle changes.
