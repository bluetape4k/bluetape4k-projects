package io.bluetape4k.redis.redisson.cache

import io.bluetape4k.support.requireNotBlank
import org.redisson.api.RLocalCachedMap
import org.redisson.api.RedissonClient
import org.redisson.api.options.LocalCachedMapOptions

/**
 * DSL 스타일로 [LocalCachedMapOptions]를 설정하여 [RLocalCachedMap]을 생성합니다.
 *
 * ## 사용 예
 * ```kotlin
 * val map = localCachedMap<String, User>("users", redissonClient) {
 *     cacheSize(1000)
 *     evictionPolicy(LocalCachedMapOptions.EvictionPolicy.LRU)
 *     timeToLive(Duration.ofMinutes(10))
 * }
 * ```
 *
 * @param K 캐시 키 타입 (reified)
 * @param V 캐시 값 타입 (reified)
 * @param name 캐시(Redis 맵) 이름
 * @param redissonClient Redisson 클라이언트 인스턴스
 * @param builder [LocalCachedMapOptions] DSL 블록
 * @return 설정이 적용된 [RLocalCachedMap] 인스턴스
 */
inline fun <reified K: Any, reified V: Any> localCachedMap(
    name: String,
    redissonClient: RedissonClient,
    builder: LocalCachedMapOptions<K, V>.() -> Unit = {},
): RLocalCachedMap<K, V> {
    name.requireNotBlank("name")
    val options = LocalCachedMapOptions.name<K, V>(name).apply(builder)
    return redissonClient.getLocalCachedMap(options)
}
