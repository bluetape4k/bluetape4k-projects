package io.bluetape4k.cache

import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs

/**
 * Lettuce 를 캐시로 사용 시 기본 Codec : [LettuceBinaryCodecs.lz4Fory]
 */
fun <V: Any> lettuceDefaultCodec(): LettuceBinaryCodec<V> =
    LettuceBinaryCodecs.lz4Fory()
