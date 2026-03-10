package io.bluetape4k.redis.redisson.cache

import org.redisson.api.RLocalCachedMap
import org.redisson.api.RedissonClient
import org.redisson.api.options.LocalCachedMapOptions

/**
 * [RLocalCachedMap<K, V>] 를 생성합니다.
 *
 * @param name 캐시 이름
 * @param redissonClient RedissonClient
 * @param builder LocalCachedMapOptions 설정
 */
inline fun <reified K: Any, reified V: Any> localCachedMap(
    name: String,
    redissonClient: RedissonClient,
    @BuilderInference builder: LocalCachedMapOptions<K, V>.() -> Unit = {},
): RLocalCachedMap<K, V> {
    val options = LocalCachedMapOptions.name<K, V>(name).apply(builder)
    return redissonClient.getLocalCachedMap(options)
}
