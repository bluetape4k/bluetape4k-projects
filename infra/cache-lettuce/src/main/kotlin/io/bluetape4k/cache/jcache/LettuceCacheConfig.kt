package io.bluetape4k.cache.jcache

import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
import javax.cache.configuration.MutableConfiguration

/**
 * Lettuce JCache 전용 설정 클래스입니다.
 *
 * [MutableConfiguration]을 확장하여 JCache 표준 설정과
 * Lettuce 고유 설정(TTL, 직렬화)을 통합합니다.
 *
 * ## TTL 계약
 * - [ttlSeconds]는 Redis hash key 전체의 TTL입니다.
 * - `put`, `putAll`, `putIfAbsent`, `replace` 계열 쓰기 API는 성공 시 TTL을 동일하게 갱신합니다.
 * - Redis 8 이상에서는 `HSETEX`를 우선 사용하되 hash key `EXPIRE`도 함께 갱신합니다.
 * - Redis 7 이하에서는 `HSET/HMSET + EXPIRE` fallback 경로를 사용합니다.
 * - `null`이면 만료를 사용하지 않습니다.
 */
class LettuceCacheConfig<K: Any, V: Any>(
    /** Redis hash key 전체에 적용할 TTL(초). `null`이면 만료를 사용하지 않습니다. */
    val ttlSeconds: Long? = null,
    /** JCache key를 Redis hash field 문자열로 변환하는 함수입니다. */
    val keyCodec: ((K) -> String)? = null,
    /** Redis hash field 문자열을 JCache key로 복원하는 함수입니다. */
    val keyDecoder: ((String) -> K)? = null,
    /** 캐시 값을 바이트 배열로 직렬화/역직렬화하는 [LettuceBinaryCodec] 인스턴스입니다. */
    val codec: LettuceBinaryCodec<*> = LettuceBinaryCodecs.lz4Fory<Any>(),
    keyType: Class<K>,
    valueType: Class<V>,
): MutableConfiguration<K, V>() {
    init {
        setTypes(keyType, valueType)
    }
}

/**
 * [LettuceCacheConfig] 인스턴스를 생성하는 간편 DSL 함수입니다.
 *
 * 생성된 설정은 [LettuceCacheManager.createCache] 또는 [LettuceJCaching.getOrCreate]에 전달할 수 있습니다.
 */
inline fun <reified K: Any, reified V: Any> lettuceCacheConfigOf(
    ttlSeconds: Long? = null,
    noinline keyCodec: ((K) -> String)? = null,
    noinline keyDecoder: ((String) -> K)? = null,
    codec: LettuceBinaryCodec<*> = LettuceBinaryCodecs.lz4Fory<Any>(),
): LettuceCacheConfig<K, V> = LettuceCacheConfig(
    ttlSeconds = ttlSeconds,
    keyCodec = keyCodec,
    keyDecoder = keyDecoder,
    codec = codec,
    keyType = K::class.java,
    valueType = V::class.java,
)
