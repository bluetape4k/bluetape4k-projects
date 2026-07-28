# 이슈 798: Protobuf strict default codec

## 배경

`ProtobufSerializer`와 Redis Protobuf codec은 allow-listed Protobuf codec으로
문서화되어 있었지만, 기본 동작은 여전히 non-Protobuf value와 non-Protobuf bytes를
Kryo/Kryo5 fallback serializer에 위임했다. 이 때문에 문서화된 shared-boundary trust
profile보다 실제 기본 경계가 넓었다.

## 결정

기본 Protobuf profile을 strict하게 만든다. Non-Protobuf payload는 호출자가 명시적인
trusted-internal API를 선택하지 않는 한 거부한다.

- `ProtobufSerializer.trustedInternalProtobuf()`
- `LettuceProtobufCodecs.trustedInternal*Protobuf()`
- `RedissonProtobufCodec.trustedInternal()`
- `RedissonProtobufCodecs.TrustedInternal*Protobuf`

## 결과

- 기본 Protobuf serializer와 Redis codec은 fallback-format payload를 거부한다.
- Legacy mixed Protobuf + Kryo fallback은 trusted internal Redis store용으로 계속 제공된다.
- 테스트는 strict shared-boundary 동작과 trusted-internal compatibility 동작을 분리한다.
- `docs/security/serialization-trust-profiles.md`는 이 차이를 문서화한다.

## 검증

- 구현 전 red test: 새 trusted-internal API reference가 compile에 실패했다.
- `./gradlew :bluetape4k-protobuf:compileTestKotlin --warning-mode all --rerun-tasks --no-daemon --no-configuration-cache`
- `./gradlew :bluetape4k-protobuf:test --tests "io.bluetape4k.protobuf.serializers.ProtobufSerializerTest" --tests "io.bluetape4k.protobuf.serializers.redis.LettuceProtobufCodecsTest" --tests "io.bluetape4k.protobuf.serializers.redis.RedissonProtobufCodecTest" --rerun-tasks --no-daemon --no-configuration-cache`
- `./gradlew :bluetape4k-protobuf:test --no-daemon --no-configuration-cache`: 209 tests, 0 failures, 0 errors, 0 skipped.
- `git diff --check`

## 향후 방지책

기본 decode path가 arbitrary application object graph를 load할 수 있는 serializer로
조용히 fallback한다면 codec을 `AllowListedTypes`로 표기하지 않는다. Legacy fallback
path는 명시적으로 유지하고 trusted-internal API라는 이름을 붙인다.

## 동시성 helper gate

`MultithreadingTester`, `SuspendedJobTester`, `StructuredTaskScopeTester`는 적용 대상이
아니었다. 이는 concurrent state, coroutine lifecycle, structured task-scope behavior가
없는 synchronous encode/decode trust-boundary 변경이다.
