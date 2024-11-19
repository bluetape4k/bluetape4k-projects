package io.bluetape4k.redis.redisson.options

import org.redisson.api.options.LocalCachedMapOptions
import org.redisson.api.options.LocalCachedMapParams
import org.redisson.client.codec.Codec

val LocalCachedMapOptions<*, *>.name: String?
    get() = (this as LocalCachedMapParams<*, *>).name

val LocalCachedMapOptions<*, *>.codec: Codec?
    get() = (this as LocalCachedMapParams<*, *>).codec
