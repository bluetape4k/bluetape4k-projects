# Issue 798 검토: Protobuf strict default codecs

## Scope

- `ProtobufSerializer`
- Lettuce Protobuf codec factories
- Redisson Protobuf codec constructors and registry factories
- Protobuf Redis codec regression tests
- Serialization trust-profile documentation

## 발견 사항

No P0/P1 findings.

## Checks

- `ProtobufSerializer()` rejects non-Protobuf values and fallback-format bytes by default.
- `ProtobufSerializer.trustedInternalProtobuf()` preserves legacy Kryo fallback compatibility.
- `LettuceProtobufCodecs.*Protobuf()` factories are strict by default.
- `LettuceProtobufCodecs.trustedInternal*Protobuf()` factories preserve trusted-internal fallback compatibility.
- `RedissonProtobufCodec()` and `RedissonProtobufCodecs.*Protobuf` values are strict by default.
- `RedissonProtobufCodec.trustedInternal()` and `RedissonProtobufCodecs.TrustedInternal*Protobuf` values preserve trusted-internal fallback compatibility.
- Trust-profile documentation distinguishes strict shared-boundary Protobuf codecs from trusted-internal fallback codecs.

## Verification Evidence

- Red test before implementation failed in `:bluetape4k-protobuf:compileTestKotlin` because the new trusted-internal APIs did not exist.
- `:bluetape4k-protobuf:compileTestKotlin --warning-mode all --rerun-tasks`: passed; remaining warnings are existing Gradle Kotlin DSL deprecations outside the touched Protobuf source/test code.
- Targeted Protobuf serializer, Lettuce codec, and Redisson codec tests with `--rerun-tasks`: passed.
- Full `:bluetape4k-protobuf:test`: 209 tests, 0 failures, 0 errors, 0 skipped.
- `git diff --check`: passed.
- CodeGraph review context: 8 changed files, low risk, 0 impacted nodes reported.

## Residual Risk

Existing callers that used default Protobuf codecs for mixed Kotlin object storage must switch to the explicit trusted-internal APIs. This is the intended security tightening for shared-boundary defaults.

## Concurrency Helper Gate

No `MultithreadingTester`, `SuspendedJobTester`, or `StructuredTaskScopeTester` coverage was added. The change narrows synchronous codec encode/decode trust boundaries and does not add shared mutable state, coroutine lifecycle behavior, or structured task scope behavior.
