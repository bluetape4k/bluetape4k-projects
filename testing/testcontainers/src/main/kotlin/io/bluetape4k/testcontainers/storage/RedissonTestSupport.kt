package io.bluetape4k.testcontainers.storage

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.codec.Kryo5Codec
import org.redisson.codec.LZ4Codec
import org.redisson.config.Config
import java.time.Duration

@JvmField
val TEST_REDISSON_CODEC = LZ4Codec(Kryo5Codec())

fun redissonConfig(url: String): Config {

    val config = Config()

    config.useSingleServer()
        .setAddress(url)
        .setRetryAttempts(3)
        .setRetryDelay { Duration.ofMillis(it * 10L + 10L) }
        .setConnectionMinimumIdleSize(8)

    config.codec = config.codec ?: TEST_REDISSON_CODEC
    return config
}

fun redissonClient(url: String): RedissonClient = Redisson.create(redissonConfig(url))
