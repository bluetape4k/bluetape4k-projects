---
title: SerializationContext와 template 구성
description: RedisSerializationContext DSL과 convenience overload를 이용해 key·value·hash 형식을 동기·reactive template에 적용합니다.
manualId: bluetape4k-spring-boot-redis
chapterId: serialization-context-templates
---

# SerializationContext와 template 구성

## 네 serialization slot 맞추기

Redis template에는 key, value, hash key, hash value가 각각 serializer를 가집니다. 같은 logical type을 일반 value와 hash value로 모두 쓴다면 두 slot을 같은 형식으로 맞춥니다.

```kotlin
val context = redisSerializationContext<String, Any> {
    key(RedisSerializer.string())
    value(RedisBinarySerializers.LZ4Fory)
    hashKey(RedisSerializer.string())
    hashValue(RedisBinarySerializers.LZ4Fory)
}
```

DSL은 Spring Data Redis builder를 그대로 받아 `build()`까지 수행합니다. 설정하지 않은 slot의 동작은 Spring builder 계약을 따르므로 필요한 slot은 명시하는 편이 안전합니다.

## key와 value를 함께 지정하기

첫 `redisSerializationContextOf` overload는 key·value serializer를 받고 같은 조합을 hash key·hash value에도 적용합니다.

```kotlin
val context = redisSerializationContextOf<String, Any>(
    keySerializer = RedisSerializer.string(),
    valueSerializer = RedisBinarySerializers.ZstdFory,
)
```

마지막 `builder` block에서 hash value나 string serializer만 다르게 덮어쓸 수도 있습니다. 다만 한 template 안에서 value와 hash value 형식이 달라지면 운영자가 key type만 보고 payload를 해석하기 어려워집니다.

## String key convenience overload

value serializer만 받는 overload는 key와 hash key를 `StringRedisSerializer.UTF_8`로 고정합니다.

```kotlin
val context = redisSerializationContextOf<Any>(
    valueSerializer = RedisBinarySerializers.LZ4Kryo,
)
```

대부분의 애플리케이션 key에 이 형태가 충분합니다. binary object를 key로 쓰면 class 변화가 key identity를 바꿀 수 있으므로 피하는 편이 좋습니다.

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

이 모듈은 bean 이름, qualifier나 primary 여부를 정하지 않습니다. 여러 template을 등록한다면 업무별 bean 이름과 key prefix를 함께 관리합니다.

## 동기 RedisTemplate 구성

동기 template은 Spring Data Redis property에 직접 serializer를 지정합니다.

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

동기와 reactive template이 같은 key를 읽는다면 각각 round trip만 테스트하지 말고 서로 쓴 값을 교차해서 읽어 봅니다.

## defaultSerializer의 의미

`redisSerializationContext(defaultSerializer)`는 default serializer로 builder를 시작합니다. 이후 key·value·hash 설정은 해당 slot을 덮어씁니다. default만 설정하고 type별 형식을 암묵적으로 맡기기보다 공개 key schema에는 각 slot을 적는 편이 낫습니다.

## Source와 tests

- [`RedisSerializationContextSupport.kt`](../../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisSerializationContextSupport.kt)
- [`RedisSerializationContextSupportTest.kt`](../../../../../spring-boot/redis/src/test/kotlin/io/bluetape4k/spring/redis/serializer/RedisSerializationContextSupportTest.kt)
- [`README.ko.md`](../../../../../spring-boot/redis/README.ko.md)
