# Serialization Trust Profiles

bluetape4k codecs use four trust profiles to describe how much serialized data
can influence deserialization.

| Profile | Dynamic type loading | Default safety boundary | Examples |
|---|---|---|---|
| `TrustedInternal` | May load classes chosen by data written inside the same trusted deployment. | Use only for private caches or queues controlled by one application boundary. | Redisson Fory/Kryo defaults, Redisson Jackson/Fastjson when `allowedPackagePrefixes = null`. |
| `AllowListedTypes` | Loads only classes allowed by package prefixes, class names, or object input filters. | Suitable for shared infrastructure and mixed producer/consumer deployments. | Kafka Jackson codecs with `allowedTypePackages`, `ProtobufSerializer`, `RedissonProtobufCodec`, secure Kryo/Fory factories, JDK object input filter. |
| `NoDynamicTypeLoading` | Serialized data does not choose a class. | Safest shape when the caller already knows the value type. | Static value-type JSON serializers and non-polymorphic decode APIs. |
| `UnsafeLegacyCompatibility` | Restores allow-all legacy behavior through an explicit unsafe name. | Temporary migration only in fully trusted internal deployments. | `AbstractKafkaCodec.ALLOW_ALL_TYPES_UNSAFE`, `RedissonProtobufCodec.ALLOW_ALL_CLASSES_UNSAFE`. |

## Defaults

| Area | Default profile | Notes |
|---|---|---|
| Kafka 3/4 Jackson codecs | `AllowListedTypes` | `emptySet()` denies all dynamic class loading; configure package prefixes for production or use `ALLOW_ALL_TYPES_UNSAFE` only for trusted legacy migration. |
| ProtobufSerializer | `AllowListedTypes` | Defaults to `io.bluetape4k.` and `com.google.protobuf.`. |
| RedissonProtobufCodec | `AllowListedTypes` | Uses the same default prefixes as `ProtobufSerializer` and Kryo5 fallback for non-Protobuf values; legacy allow-all requires `ALLOW_ALL_CLASSES_UNSAFE`. |
| Redisson Jackson3/Fastjson2 | `TrustedInternal` by default | Set `allowedPackagePrefixes` to move JSONB/polymorphic JSON use into `AllowListedTypes`. |
| Kryo/Fory binary serializers | `TrustedInternal` by default | Use secure factories when data crosses a shared or untrusted boundary. |
| JDK binary serializer | `AllowListedTypes` | Applies the default JDK object input filter and remains deprecated for new general use. |

## Migration Guidance

For shared Kafka topics, Redis keys, queues, or any boundary where a producer can
be outside the current process trust boundary, prefer `AllowListedTypes` or
`NoDynamicTypeLoading`.

Use unsafe legacy constants only as an intentional migration bridge:

```kotlin
// Kafka legacy compatibility: trusted internal deployments only.
override val allowedTypePackages = AbstractKafkaCodec.ALLOW_ALL_TYPES_UNSAFE

// Redisson Protobuf legacy compatibility: trusted internal deployments only.
val codec = RedissonProtobufCodec(
    allowedClassPrefixes = RedissonProtobufCodec.ALLOW_ALL_CLASSES_UNSAFE,
)
```

Replace unsafe settings with narrow package prefixes once all stored payloads and
producers are known.
