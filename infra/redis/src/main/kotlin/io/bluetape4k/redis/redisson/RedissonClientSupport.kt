package io.bluetape4k.redis.redisson

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.api.RedissonReactiveClient
import org.redisson.client.codec.Codec
import org.redisson.config.Config
import java.io.File
import java.io.InputStream
import java.net.URL

fun configFromYamlOf(input: InputStream, codec: Codec = RedissonCodecs.Default): Config {
    return Config.fromYAML(input).apply { this.codec = codec }
}

fun configFromYamlOf(content: String, codec: Codec = RedissonCodecs.Default): Config {
    return Config.fromYAML(content).apply { this.codec = codec }
}

fun configFromYamlOf(file: File, codec: Codec = RedissonCodecs.Default): Config {
    return Config.fromYAML(file).apply { this.codec = codec }
}

fun configFromYamlOf(url: URL, codec: Codec = RedissonCodecs.Default): Config {
    return Config.fromYAML(url).apply { this.codec = codec }
}


inline fun redissonClient(block: Config.() -> Unit): RedissonClient {
    return redissonClientOf(Config().apply(block))
}

fun redissonClientOf(config: Config): RedissonClient {
    return Redisson.create(config)
}

inline fun redissonReactiveClient(bllock: Config.() -> Unit): RedissonReactiveClient {
    return redissonReactiveClientOf(Config().apply(bllock))
}

fun redissonReactiveClientOf(config: Config): RedissonReactiveClient {
    return redissonClientOf(config).reactive()
}
