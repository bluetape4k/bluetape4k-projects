package io.bluetape4k.cache.jcache.lettuce

import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.BinarySerializers
import javax.cache.configuration.MutableConfiguration

/**
 * Lettuce JCache 전용 설정 클래스입니다.
 *
 * [MutableConfiguration]을 확장하여 JCache 표준 설정과
 * Lettuce 고유 설정(TTL, 직렬화)을 통합합니다.
 */
class LettuceCacheConfig<K: Any, V: Any>(
    val ttlSeconds: Long? = null,
    val keyCodec: ((K) -> String)? = null,
    val serializer: BinarySerializer = BinarySerializers.Fory,
    keyType: Class<K>,
    valueType: Class<V>,
): MutableConfiguration<K, V>() {
    init {
        setTypes(keyType, valueType)
    }
}

/**
 * [LettuceCacheConfig] DSL 빌더 함수입니다.
 */
inline fun <reified K: Any, reified V: Any> lettuceCacheConfigOf(
    ttlSeconds: Long? = null,
    noinline keyCodec: ((K) -> String)? = null,
    serializer: BinarySerializer = BinarySerializers.Fory,
): LettuceCacheConfig<K, V> = LettuceCacheConfig(
    ttlSeconds = ttlSeconds,
    keyCodec = keyCodec,
    serializer = serializer,
    keyType = K::class.java,
    valueType = V::class.java,
)
