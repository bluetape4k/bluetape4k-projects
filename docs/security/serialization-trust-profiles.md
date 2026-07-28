# Serialization Trust Profile

bluetape4k codec은 serialized data가 deserialization에 얼마나 영향을 줄 수 있는지를 네 가지
trust profile로 설명한다.

| Profile | Dynamic type loading | 기본 safety boundary | 예시 |
|---|---|---|---|
| `TrustedInternal` | May load classes chosen by data written inside the same trusted deployment. | Use only for private caches or queues controlled by one application boundary. | Redisson Fory/Kryo defaults, Redisson Jackson/Fastjson when `allowedPackagePrefixes = null`, `trustedInternal*Protobuf` fallback codecs. |
| `AllowListedTypes` | Loads only classes allowed by package prefixes, class names, or object input filters. | Suitable for shared infrastructure and mixed producer/consumer deployments. | Kafka Jackson codecs with `allowedTypePackages`, strict `ProtobufSerializer`, strict `RedissonProtobufCodec`, secure Kryo/Fory factories, JDK object input filter. |
| `NoDynamicTypeLoading` | Serialized data does not choose a class. | Safest shape when the caller already knows the value type. | Static value-type JSON serializers and non-polymorphic decode APIs. |
| `UnsafeLegacyCompatibility` | Restores allow-all legacy behavior through an explicit unsafe name. | Temporary migration only in fully trusted internal deployments. | `AbstractKafkaCodec.ALLOW_ALL_TYPES_UNSAFE`, `RedissonProtobufCodec.ALLOW_ALL_CLASSES_UNSAFE`. |

## 기본값

| 영역 | 기본 profile | 메모 |
|---|---|---|
| Kafka 3/4 Jackson codecs | `AllowListedTypes` | `emptySet()` denies all dynamic class loading; configure package prefixes for production or use `ALLOW_ALL_TYPES_UNSAFE` only for trusted legacy migration. |
| ProtobufSerializer | `AllowListedTypes` | Defaults to `io.bluetape4k.` and `com.google.protobuf.` and rejects non-Protobuf values or bytes unless `ProtobufSerializer.trustedInternalProtobuf()` is selected. |
| LettuceProtobufCodecs | `AllowListedTypes` | Default `*Protobuf()` factories are strict. Their caller-owned `ByteBuf` path preserves the same allowlist and wire contract. Use `trustedInternal*Protobuf()` only for internal Redis stores that must read legacy Kryo fallback payloads; compressed, fallback, custom-prefix, and single-argument `ByteBuffer` paths remain compatibility paths. |
| RedissonProtobufCodec | `AllowListedTypes` | Default constructors and `RedissonProtobufCodecs.*Protobuf` values are strict. Use `RedissonProtobufCodec.trustedInternal()` or `RedissonProtobufCodecs.TrustedInternal*Protobuf` only for legacy fallback payloads; legacy allow-all requires `ALLOW_ALL_CLASSES_UNSAFE`. |
| Redisson Jackson3/Fastjson2 | `TrustedInternal` by default | Set `allowedPackagePrefixes` to move JSONB/polymorphic JSON use into `AllowListedTypes`. |
| Kryo/Fory binary serializers | `TrustedInternal` by default | Use secure factories when data crosses a shared or untrusted boundary. |
| JDK binary serializer | `AllowListedTypes` | Applies the default JDK object input filter and remains deprecated for new general use. |

## Migration 지침

Shared Kafka topic, Redis key, queue, 또는 producer가 현재 process trust boundary 밖에 있을 수
있는 경계에서는 `AllowListedTypes` 또는 `NoDynamicTypeLoading`을 우선한다.

Unsafe legacy constant는 의도적인 migration bridge로만 사용한다.

```kotlin
// Kafka legacy compatibility: trusted internal deployments only.
override val allowedTypePackages = AbstractKafkaCodec.ALLOW_ALL_TYPES_UNSAFE

// Redisson Protobuf legacy compatibility: trusted internal deployments only.
val codec = RedissonProtobufCodec.trustedInternal(
    allowedClassPrefixes = RedissonProtobufCodec.ALLOW_ALL_CLASSES_UNSAFE,
)

// Lettuce Protobuf legacy fallback bytes: trusted internal deployments only.
val lettuceCodec = LettuceProtobufCodecs.trustedInternalProtobuf<MyTrustedValue>()
```

저장된 모든 payload와 producer가 확인되면 unsafe setting을 좁은 package prefix로 교체한다.
