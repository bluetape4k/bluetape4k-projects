package io.bluetape4k.redis.redisson.cache

import io.bluetape4k.support.requireNotBlank
import org.redisson.api.RMapCache
import org.redisson.api.RedissonClient
import org.redisson.api.options.MapCacheOptions

/**
 * DSL 스타일로 [MapCacheOptions]를 설정하여 [RMapCache]를 생성합니다.
 *
 * TTL이 지정된 엔트리를 지원하는 Redis Map 캐시를 생성합니다.
 *
 * ```kotlin
 * val cache = mapCache<String, User>("users", redissonClient) {
 *     maxSize(1000)
 *     timeToLiveInSeconds(300)
 * }
 * // cache != null
 * ```
 *
 * @param K 캐시 키 타입 (reified)
 * @param V 캐시 값 타입 (reified)
 * @param name 캐시 이름
 * @param redissonClient Redisson 클라이언트 인스턴스
 * @param builder [MapCacheOptions] DSL 블록
 * @return 설정이 적용된 [RMapCache] 인스턴스
 */
inline fun <reified K: Any, reified V: Any> mapCache(
    name: String,
    redissonClient: RedissonClient,
    builder: MapCacheOptions<K, V>.() -> Unit = {},
): RMapCache<K, V> {
    name.requireNotBlank("name")
    val options = MapCacheOptions.name<K, V>(name).apply(builder)
    return redissonClient.getMapCache(options)
}
