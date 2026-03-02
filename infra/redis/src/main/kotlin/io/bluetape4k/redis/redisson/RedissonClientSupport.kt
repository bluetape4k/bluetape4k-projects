package io.bluetape4k.redis.redisson

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.api.RedissonReactiveClient
import org.redisson.client.codec.Codec
import org.redisson.config.Config
import java.io.File
import java.io.InputStream
import java.net.URL

/**
 * YAML 입력 스트림으로 Redisson [Config]를 생성하고 codec을 설정합니다.
 *
 * ## 동작/계약
 * - `Config.fromYAML(input)` 결과에 [codec]을 적용합니다.
 * - YAML 파싱 오류는 Redisson 예외로 전파됩니다.
 * - [codec] 미지정 시 [RedissonCodecs.Default]를 사용합니다.
 *
 * ```kotlin
 * val config = configFromYamlOf(inputStream)
 * // config.codec != null
 * ```
 */
fun configFromYamlOf(input: InputStream, codec: Codec = RedissonCodecs.Default): Config {
    return Config.fromYAML(input).apply { this.codec = codec }
}

/** YAML 문자열로 Redisson [Config]를 생성하고 codec을 설정합니다. */
fun configFromYamlOf(content: String, codec: Codec = RedissonCodecs.Default): Config {
    return Config.fromYAML(content).apply { this.codec = codec }
}

/** YAML 파일로 Redisson [Config]를 생성하고 codec을 설정합니다. */
fun configFromYamlOf(file: File, codec: Codec = RedissonCodecs.Default): Config {
    return Config.fromYAML(file).apply { this.codec = codec }
}

/** YAML URL로 Redisson [Config]를 생성하고 codec을 설정합니다. */
fun configFromYamlOf(url: URL, codec: Codec = RedissonCodecs.Default): Config {
    return Config.fromYAML(url).apply { this.codec = codec }
}

/**
 * DSL 블록으로 [RedissonClient]를 생성합니다.
 *
 * ## 동작/계약
 * - 새 [Config]를 만든 뒤 [block]을 적용하고 [redissonClientOf]로 위임합니다.
 * - 호출마다 새 클라이언트 인스턴스를 생성합니다.
 *
 * ```kotlin
 * val client = redissonClient { useSingleServer().address = RedisConst.DEFAULT_URL }
 * // client != null
 * ```
 */
inline fun redissonClient(block: Config.() -> Unit): RedissonClient {
    return redissonClientOf(Config().apply(block))
}

/** 전달된 [config]로 [RedissonClient]를 생성합니다. */
fun redissonClientOf(config: Config): RedissonClient {
    return Redisson.create(config)
}

/**
 * DSL 블록으로 [RedissonReactiveClient]를 생성합니다.
 *
 * ## 동작/계약
 * - 새 [Config]를 만든 뒤 [block]을 적용해 reactive client를 만듭니다.
 * - 내부적으로 [redissonReactiveClientOf]에 위임합니다.
 *
 * ```kotlin
 * val reactive = redissonReactiveClient { useSingleServer().address = RedisConst.DEFAULT_URL }
 * // reactive != null
 * ```
 */
inline fun redissonReactiveClient(block: Config.() -> Unit): RedissonReactiveClient {
    return redissonReactiveClientOf(Config().apply(block))
}

/** 전달된 [config]로 [RedissonReactiveClient]를 생성합니다. */
fun redissonReactiveClientOf(config: Config): RedissonReactiveClient {
    return redissonClientOf(config).reactive()
}
