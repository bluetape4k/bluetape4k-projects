package io.bluetape4k.testcontainers.storage

import org.redisson.codec.Kryo5Codec
import org.redisson.codec.LZ4Codec

@JvmField
internal val TEST_REDISSON_CODEC = LZ4Codec(Kryo5Codec())
