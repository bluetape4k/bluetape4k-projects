package io.bluetape4k.redis.redisson

import io.bluetape4k.redis.redisson.codec.RedissonCodecs
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.api.RedissonReactiveClient
import org.redisson.client.codec.Codec
import org.redisson.config.Config
import org.redisson.config.ConstantDelay
import java.io.File
import java.io.InputStream
import java.net.URL
import java.time.Duration

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
fun configFromYamlOf(
    input: InputStream,
    codec: Codec = RedissonCodecs.Default,
): Config {
    return Config.fromYAML(input).apply { this.codec = codec }
}

/**
 * YAML 문자열로 Redisson [Config]를 생성하고 codec을 설정합니다.
 *
 * ```kotlin
 * val yaml = """
 *     singleServerConfig:
 *       address: "redis://127.0.0.1:6379"
 * """.trimIndent()
 * val config = configFromYamlOf(yaml)
 * // config.codec != null
 * ```
 *
 * @param content YAML 형식의 Redisson 설정 문자열
 * @param codec 적용할 Codec (기본값: [RedissonCodecs.Default])
 * @return 설정이 적용된 [Config]
 */
fun configFromYamlOf(content: String, codec: Codec = RedissonCodecs.Default): Config {
    return Config.fromYAML(content).apply { this.codec = codec }
}

/**
 * YAML 파일로 Redisson [Config]를 생성하고 codec을 설정합니다.
 *
 * ```kotlin
 * val file = File("redisson.yaml")
 * val config = configFromYamlOf(file)
 * // config.codec != null
 * ```
 *
 * @param file YAML 설정 파일
 * @param codec 적용할 Codec (기본값: [RedissonCodecs.Default])
 * @return 설정이 적용된 [Config]
 */
fun configFromYamlOf(file: File, codec: Codec = RedissonCodecs.Default): Config {
    return Config.fromYAML(file).apply { this.codec = codec }
}

/**
 * YAML URL로 Redisson [Config]를 생성하고 codec을 설정합니다.
 *
 * ```kotlin
 * val url = URL("file:///etc/redisson/config.yaml")
 * val config = configFromYamlOf(url)
 * // config.codec != null
 * ```
 *
 * @param url YAML 설정 파일의 URL
 * @param codec 적용할 Codec (기본값: [RedissonCodecs.Default])
 * @return 설정이 적용된 [Config]
 */
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
 * val client = redissonClient { useSingleServer().address = RedissonConst.DEFAULT_URL }
 * // client != null
 * ```
 */
inline fun redissonClient(block: Config.() -> Unit): RedissonClient {
    return redissonClientOf(Config().apply(block))
}

/**
 * 전달된 [config]로 [RedissonClient]를 생성합니다.
 *
 * ```kotlin
 * val config = Config().apply {
 *     useSingleServer().setAddress(RedissonConst.DEFAULT_URL)
 * }
 * val client = redissonClientOf(config)
 * // client != null
 * ```
 *
 * @param config Redisson 설정
 * @return 생성된 [RedissonClient]
 */
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
 * val reactive = redissonReactiveClient { useSingleServer().address = RedissonConst.DEFAULT_URL }
 * // reactive != null
 * ```
 */
inline fun redissonReactiveClient(block: Config.() -> Unit): RedissonReactiveClient {
    return redissonReactiveClientOf(Config().apply(block))
}

/**
 * 전달된 [config]로 [RedissonReactiveClient]를 생성합니다.
 *
 * ```kotlin
 * val config = Config().apply {
 *     useSingleServer().setAddress(RedissonConst.DEFAULT_URL)
 * }
 * val reactiveClient = redissonReactiveClientOf(config)
 * // reactiveClient != null
 * ```
 *
 * @param config Redisson 설정
 * @return 생성된 [RedissonReactiveClient]
 */
fun redissonReactiveClientOf(config: Config): RedissonReactiveClient {
    return redissonClientOf(config).reactive()
}

/**
 * 고동시성 환경에 적합한 Connection Pool 및 Netty 스레드 설정을 [Config]에 적용합니다.
 *
 * ## 적용 내용
 * - threads: CPU × 2 (최대 32)
 * - nettyThreads: CPU × 2 (최대 32)
 * - SingleServerConfig:
 *   - connectionPoolSize: CPU × 8 (64~256)
 *   - connectionMinimumIdleSize: CPU × 2 (8~32)
 *   - keepAlive: true
 *   - pingConnectionInterval: 30초
 *   - tcpNoDelay: true
 *   - connectTimeout: 3초
 *   - timeout: 5초
 *   - retryAttempts: 3
 *   - retryInterval: 1초
 *
 * ```kotlin
 * val config = Config().applyHighConcurrencyDefaults()
 * config.useSingleServer().address = RedissonConst.DEFAULT_URL
 * val client = redissonClientOf(config)
 * ```
 *
 * @return 설정이 적용된 [Config] (chain 가능)
 */
fun Config.applyHighConcurrencyDefaults(): Config = apply {
    threads = RedissonConst.DEFAULT_NETTY_THREADS
    nettyThreads = RedissonConst.DEFAULT_NETTY_THREADS
    // keepAlive/tcpNoDelay moved to top-level Config in Redisson 4.x (non-deprecated API)
    isTcpKeepAlive = true
    isTcpNoDelay = true
    useSingleServer().apply {
        connectionPoolSize = RedissonConst.DEFAULT_CONNECTION_POOL_SIZE
        connectionMinimumIdleSize = RedissonConst.DEFAULT_CONNECTION_MIN_IDLE_SIZE
        pingConnectionInterval = RedissonConst.DEFAULT_PING_INTERVAL_MILLIS
        connectTimeout = RedissonConst.DEFAULT_CONNECT_TIMEOUT_MILLIS
        timeout = RedissonConst.DEFAULT_OPERATION_TIMEOUT_MILLIS
        retryAttempts = RedissonConst.DEFAULT_RETRY_ATTEMPTS
        // retryInterval is deprecated → use retryDelay (DelayStrategy)
        retryDelay = ConstantDelay(Duration.ofMillis(RedissonConst.DEFAULT_RETRY_INTERVAL_MILLIS.toLong()))
        idleConnectionTimeout = RedissonConst.DEFAULT_IDLE_CONNECTION_TIMEOUT_MILLIS
    }
}

/**
 * 고동시성 환경에 최적화된 [RedissonClient]를 생성합니다.
 *
 * [applyHighConcurrencyDefaults]를 적용한 [Config]로 클라이언트를 생성합니다.
 *
 * ```kotlin
 * val client = redissonClientForHighConcurrency("redis://127.0.0.1:6379")
 * ```
 *
 * @param url Redis 서버 URL (예: "redis://127.0.0.1:6379")
 * @param codec 사용할 Codec (기본값: [RedissonCodecs.Default])
 * @return 고동시성 설정이 적용된 [RedissonClient]
 */
fun redissonClientForHighConcurrency(
    url: String,
    codec: Codec = RedissonCodecs.Default,
): RedissonClient {
    val config = Config().applyHighConcurrencyDefaults()
    config.useSingleServer().address = url
    config.codec = codec
    return Redisson.create(config)
}
