---
title: SerializationContext and templates
description: Apply explicit key, value, and hash formats to synchronous and reactive templates with the DSL and convenience overloads.
manualId: bluetape4k-spring-boot-redis
chapterId: serialization-context-templates
---

# SerializationContext and templates

## Align four serialization slots

Redis templates have separate serializers for keys, values, hash keys, and hash values. Use the same format for logical values stored both directly and in hashes.

```kotlin
val context = redisSerializationContext<String, Any> {
    key(RedisSerializer.string())
    value(RedisBinarySerializers.LZ4Fory)
    hashKey(RedisSerializer.string())
    hashValue(RedisBinarySerializers.LZ4Fory)
}
```

The DSL delegates to the Spring Data Redis builder and calls `build()`. Explicitly configure each slot your application uses.

## Supply key and value serializers

The first `redisSerializationContextOf` overload applies the key and value serializers to the corresponding hash slots.

```kotlin
val context = redisSerializationContextOf<String, Any>(
    keySerializer = RedisSerializer.string(),
    valueSerializer = RedisBinarySerializers.ZstdFory,
)
```

The final builder block can override individual slots. Different value and hash-value formats are possible, but they make operational inspection harder.

## String-key convenience overload

The value-only overload fixes keys and hash keys to `StringRedisSerializer.UTF_8`.

```kotlin
val context = redisSerializationContextOf<Any>(
    valueSerializer = RedisBinarySerializers.LZ4Kryo,
)
```

This fits most application keyspaces. Object serialization for keys is risky because class changes can change key identity.

## ReactiveRedisTemplate bean

```kotlin
@Bean
fun ordersRedisTemplate(
    factory: ReactiveRedisConnectionFactory,
): ReactiveRedisTemplate<String, Any> {
    val context = redisSerializationContextOf<Any>(
        valueSerializer = RedisBinarySerializers.LZ4Fory,
    )
    return ReactiveRedisTemplate(factory, context)
}
```

The module does not choose bean names, qualifiers, or `@Primary`. When several templates exist, align each bean name with its key prefix and schema.

## Synchronous RedisTemplate

```kotlin
@Bean
fun ordersRedisTemplate(factory: RedisConnectionFactory) =
    RedisTemplate<String, Any>().apply {
        connectionFactory = factory
        keySerializer = RedisSerializer.string()
        valueSerializer = RedisBinarySerializers.LZ4Fory
        hashKeySerializer = RedisSerializer.string()
        hashValueSerializer = RedisBinarySerializers.LZ4Fory
        afterPropertiesSet()
    }
```

When synchronous and reactive templates share keys, test cross-reading between them rather than testing only self round trips.

## Meaning of defaultSerializer

`redisSerializationContext(defaultSerializer)` starts the builder with that default. Explicit key, value, and hash calls override their slots. Public keyspaces are easier to maintain when every used slot is visible in configuration.

## Sources and tests

- [`RedisSerializationContextSupport.kt`](../../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisSerializationContextSupport.kt)
- [`RedisSerializationContextSupportTest.kt`](../../../../../spring-boot/redis/src/test/kotlin/io/bluetape4k/spring/redis/serializer/RedisSerializationContextSupportTest.kt)
- [`README.md`](../../../../../spring-boot/redis/README.md)
