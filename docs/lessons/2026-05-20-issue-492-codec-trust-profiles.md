# 이슈 492 Codec Trust Profile

## 배경

Issue #492는 shared trust-profile vocabulary, explicit unsafe legacy codec mode, Kafka secure/legacy
example, representative codec test가 필요했다.

## 결정

`SerializationTrustProfile`을 shared public vocabulary로 추가하고
`docs/security/serialization-trust-profiles.md`에 codec default를 문서화한다.
`RedissonProtobufCodec` default를 Kryo5 fallback을 가진 `AllowListedTypes`로 만들고,
`RedissonProtobufCodec`과 `ProtobufSerializer`의 package-prefix matching을 강화해 protobuf dynamic
type loading을 harden한다.

## 결과

Kafka/Kafka4 문서는 `emptySet()`을 deny-all로 설명하고 explicit allowlist와 unsafe legacy example을
보여준다. Redisson protobuf unsafe legacy behavior는 `ALLOW_ALL_CLASSES_UNSAFE`를 통해서만 사용할 수
있다. Prefix spoofing과 blank allowlist entry는 test로 거부된다.

## 검증

- 수정한 Kotlin file에 IntelliJ reformat/import optimization 실행.
- `./gradlew :bluetape4k-io:test --tests 'io.bluetape4k.io.serializer.SerializationTrustProfileTest' :bluetape4k-protobuf:test :bluetape4k-kafka:test --tests 'io.bluetape4k.kafka.codec.JacksonKafkaCodecSecurityTest' :bluetape4k-kafka4:test --tests 'io.bluetape4k.kafka.codec.JacksonKafkaCodecSecurityTest' --tests 'io.bluetape4k.kafka.codec.KafkaCodecAllowlistTest' --console=plain --no-configuration-cache` 통과.
- `javap`로 `RedissonProtobufCodec`이 no-arg, `Codec`, `Set`, `(Codec, Set)`, Redisson class-loader constructor를 유지함을 확인.
- `git diff --check` 통과.

## 다음번 가이드

Deny-all default를 문서화할 때는 default polymorphic round-trip을 여전히 암시하는 README snippet을
scan한다. Migration doc을 publish하기 전에 named-argument constructor example이 실제 public Kotlin
API와 맞는지도 확인한다.
