# Issue 798: Protobuf strict default codecs

## Context

`ProtobufSerializer` and Redis Protobuf codecs were documented as allow-listed Protobuf codecs, but their default behavior still delegated non-Protobuf values and non-Protobuf bytes to Kryo/Kryo5 fallback serializers. That made the advertised shared-boundary trust profile too broad.

## Decision

Make the default Protobuf profile strict. Non-Protobuf payloads are rejected unless the caller chooses an explicit trusted-internal API:

- `ProtobufSerializer.trustedInternalProtobuf()`
- `LettuceProtobufCodecs.trustedInternal*Protobuf()`
- `RedissonProtobufCodec.trustedInternal()`
- `RedissonProtobufCodecs.TrustedInternal*Protobuf`

## Outcome

- Default Protobuf serializer and Redis codecs reject fallback-format payloads.
- Legacy mixed Protobuf + Kryo fallback remains available for trusted internal Redis stores.
- Tests now separate strict shared-boundary behavior from trusted-internal compatibility behavior.
- `docs/security/serialization-trust-profiles.md` documents the distinction.

## Verification

- Red test before implementation: new trusted-internal API references failed compilation.
- `./gradlew :bluetape4k-protobuf:compileTestKotlin --warning-mode all --rerun-tasks --no-daemon --no-configuration-cache`
- `./gradlew :bluetape4k-protobuf:test --tests "io.bluetape4k.protobuf.serializers.ProtobufSerializerTest" --tests "io.bluetape4k.protobuf.serializers.redis.LettuceProtobufCodecsTest" --tests "io.bluetape4k.protobuf.serializers.redis.RedissonProtobufCodecTest" --rerun-tasks --no-daemon --no-configuration-cache`
- `./gradlew :bluetape4k-protobuf:test --no-daemon --no-configuration-cache`: 209 tests, 0 failures, 0 errors, 0 skipped.
- `git diff --check`

## Future Guard

Do not label a codec as `AllowListedTypes` if default decode paths silently fall back to a serializer that can load arbitrary application object graphs. Keep legacy fallback paths explicit and name them as trusted-internal APIs.

## Concurrency Helper Gate

`MultithreadingTester`, `SuspendedJobTester`, and `StructuredTaskScopeTester` were not applicable. This is a synchronous encode/decode trust-boundary change with no concurrent state, coroutine lifecycle, or structured task-scope behavior.
