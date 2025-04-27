package io.bluetape4k.redis.redisson

import org.redisson.api.RMapCache
import org.redisson.api.RedissonClient
import org.redisson.api.options.MapCacheOptions

/**
 * [RMapCache<K, V>] 를 생성합니다.
 *
 * @param name 캐시 이름
 * @param redissonClient RedissonClient
 * @param block MapCacheOptions 설정
 */
inline fun <reified K: Any, reified V: Any> mapCache(
    name: String,
    redissonClient: RedissonClient,
    block: MapCacheOptions<K, V>.() -> Unit = {},
): RMapCache<K, V> {
    val options = MapCacheOptions.name<K, V>(name).apply(block)
    return redissonClient.getMapCache(options)
}
