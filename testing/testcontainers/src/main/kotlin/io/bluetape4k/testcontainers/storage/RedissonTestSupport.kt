package io.bluetape4k.testcontainers.storage

import org.redisson.codec.ForyCodec
import org.redisson.codec.LZ4Codec

@JvmField
internal val TEST_REDISSON_CODEC = LZ4Codec(ForyCodec())
