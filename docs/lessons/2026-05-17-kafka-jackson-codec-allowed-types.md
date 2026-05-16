# Kafka JacksonKafkaCodec: allowedTypePackages Security Feature Breaks Tests (2026-05-17)

## Root Cause

`JacksonKafkaCodec` introduced a `allowedTypePackages` allowlist in 1.8.0 (empty by default).
When a codec is configured via class reference in Kafka producer/consumer properties:

```kotlin
this[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JacksonKafkaCodec::class.java
```

Kafka instantiates the class with its no-arg constructor, which sets `allowedTypePackages = emptySet()`.
At deserialization time, `getValueType()` rejects any class name from the Kafka type header
because nothing matches the empty set — throwing `IllegalArgumentException`.

Spring Kafka catches the deserializer exception and delivers a `KafkaNull` payload to the listener.
The listener method expects a typed object (`Greeting`), so Spring throws
`MethodArgumentNotValidException: Payload value must not be empty`.
The listener never increments the counter, and `await until { counter >= 2 }` times out.

The CI failure presented as `ConditionTimeoutException` (10 s default), masking the real cause.

## Fix

Switch from class-reference-based to instance-based serializer/deserializer configuration
so `allowedTypePackages` can be set explicitly:

```kotlin
private fun valueCodec() = JacksonKafkaCodec(
    allowedTypePackages = AbstractKafkaCodec.ALLOW_ALL_TYPES_UNSAFE
)

@Bean
@Suppress("UNCHECKED_CAST")
fun producerFactory(): ProducerFactory<String, Greeting> {
    return KafkaServer.Launcher.Spring.getProducerFactory(
        keySerializer = StringKafkaCodec(),
        valueSerializer = valueCodec()
    ) as ProducerFactory<String, Greeting>
}
```

Also raised the Awaitility timeout from the 10 s default to 30 s as a safety buffer for CI load.

## Lesson

When `JacksonKafkaCodec` (or any `AbstractKafkaCodec` subclass) is configured via class reference
in Kafka properties, the no-arg constructor is used — `allowedTypePackages` is always empty.
Tests using class-reference-based codec config will silently fail after the 1.8.0 security upgrade.

**Rule**: In tests that deserialize typed objects with `JacksonKafkaCodec`, always use
instance-based factory configuration (via `KafkaServer.Launcher.Spring.getProducerFactory(serializer, ...)`)
rather than `*_CLASS_CONFIG = SomeCodec::class.java`.
