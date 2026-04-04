package io.bluetape4k.redis.redisson.options

import org.redisson.api.options.LocalCachedMapOptions
import org.redisson.api.options.LocalCachedMapParams
import org.redisson.client.codec.Codec

/**
 * [LocalCachedMapOptions]가 보유한 캐시 이름을 조회합니다.
 *
 * Redisson 구현체가 [LocalCachedMapParams]인 경우에만 값을 반환할 수 있습니다.
 *
 * ```kotlin
 * val options = LocalCachedMapOptions.name<String, Any>("my-cache")
 * val name = options.name
 * // name == "my-cache"
 * ```
 */
val LocalCachedMapOptions<*, *>.name: String?
    get() = (this as? LocalCachedMapParams<*, *>)?.name

/**
 * [LocalCachedMapOptions]에 설정된 [Codec]을 조회합니다.
 *
 * Redisson 구현체가 [LocalCachedMapParams]인 경우에만 값을 반환할 수 있습니다.
 *
 * ```kotlin
 * val options = LocalCachedMapOptions.name<String, Any>("my-cache")
 *     .codec(RedissonCodecs.LZ4Fory)
 * val codec = options.codec
 * // codec != null
 * ```
 */
val LocalCachedMapOptions<*, *>.codec: Codec?
    get() = (this as? LocalCachedMapParams<*, *>)?.codec
