package io.bluetape4k.spring.redis.serializer

import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

/**
 * Spring Data Redis용 [RedisSerializationContext]`<K, V>` 를 빌드합니다.
 *
 * ```
 * @Bean
 * fun reactiveRedisTemplate(factory: ReactiveRedisConnectionFactory): ReactiveRedisTemplate<*, *> {
 *     val context = redisSerializationContext<ByteArray, ByteArray>(RedisSerializer.byteArray()) {
 *         key(RedisSerializer.byteArray())
 *         hashKey(RedisSerializer.byteArray())
 *         value(RedisBinarySerializers.LZ4)
 *         hashValue(RedisBinarySerializers.LZ4)
 *         string(RedisSerializer.string())
 *     }
 *
 *     return ReactiveRedisTemplate(factory, context)
 * }
 * ```
 *
 * @param K Key type
 * @param V Value type
 * @param defaultSerializer  default serializer
 * @param builder  [RedisSerializationContext.RedisSerializationContextBuilder] initializer
 * @return [RedisSerializationContext] instance
 */
inline fun <K: Any, V: Any> redisSerializationContext(
    defaultSerializer: RedisSerializer<*>? = null,
    builder: RedisSerializationContext.RedisSerializationContextBuilder<K, V>.() -> Unit,
): RedisSerializationContext<K, V> {
    val context = defaultSerializer?.let {
        RedisSerializationContext.newSerializationContext(it)
    } ?: RedisSerializationContext.newSerializationContext<K, V>()

    return context.apply(builder).build()
}

/**
 * Spring Data Redis용 [RedisSerializationContext]`<K, V>` 를 빌드합니다.
 *
 * ```
 * @Bean
 * fun reactiveRedisTemplate(factory: ReactiveRedisConnectionFactory): ReactiveRedisTemplate<*, *> {
 *    val context = redisSerializationContextOf<String, ByteArray>(
 *          keySerializer = RedisSerializer.string(),
 *          valueSerializer = RedisBinarySerializers.LZ4Kryo,
 *          defaultSerializer = RedisSerializer.string()
 *    )
 *    return ReactiveRedisTemplate(factory, context)
 * }
 * ```
 *
 * @param K Key type
 * @param V Value type
 * @param keySerializer Key serializer
 * @param valueSerializer Value serializer
 * @param defaultSerializer default serializer
 * @return [RedisSerializationContext] instance
 */
inline fun <K: Any, V: Any> redisSerializationContextOf(
    keySerializer: RedisSerializer<K>,
    valueSerializer: RedisSerializer<V>,
    defaultSerializer: RedisSerializer<*>? = null,
    builder: RedisSerializationContext.RedisSerializationContextBuilder<K, V>.() -> Unit = {},
): RedisSerializationContext<K, V> =
    redisSerializationContext(defaultSerializer) {
        key(keySerializer)
        value(valueSerializer)
        hashKey(keySerializer)
        hashValue(valueSerializer)

        builder()
    }

/**
 * Spring Data Redis용 [RedisSerializationContext]`<String, V>` 를 빌드합니다.
 *
 * ```
 * @Bean
 * fun reactiveRedisTemplate(factory: ReactiveRedisConnectionFactory): ReactiveRedisTemplate<String, *> {
 *   val context = redisSerializationContextOf<ByteArray>(
 *      valueSerializer = RedisBinarySerializers.LZ4Kryo
 *   )
 *   return ReactiveRedisTemplate(factory, context)
 * }
 * ```
 *
 * @param V Value type
 * @param valueSerializer Value serializer
 * @param defaultSerializer default serializer
 * @return [RedisSerializationContext] instance
 * @see redisSerializationContextOf
 */
inline fun <V: Any> redisSerializationContextOf(
    valueSerializer: RedisSerializer<V>,
    defaultSerializer: RedisSerializer<*>? = null,
    builder: RedisSerializationContext.RedisSerializationContextBuilder<String, V>.() -> Unit = {},
): RedisSerializationContext<String, V> =
    redisSerializationContext(defaultSerializer) {
        key(StringRedisSerializer.UTF_8)
        value(valueSerializer)
        hashKey(StringRedisSerializer.UTF_8)
        hashValue(valueSerializer)

        builder()
    }
