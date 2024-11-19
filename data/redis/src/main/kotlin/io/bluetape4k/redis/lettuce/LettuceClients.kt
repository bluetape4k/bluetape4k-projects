package io.bluetape4k.redis.lettuce

import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.RedisConst
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.resource.ClientResources
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

/**
 * Lettuce 의 [RedisClient] 등을 생성해주는 유틸리티 클래스입니다.
 */
object LettuceClients: KLogging() {

    @JvmField
    val DEFAULT_REDIS_URI: RedisURI = getRedisURI()

    fun getRedisURI(
        host: String = RedisConst.DEFAULT_HOST,
        port: Int = RedisConst.DEFAULT_PORT,
        timeoutInMillis: Long = RedisConst.DEFAULT_TIMEOUT_MILLIS,
    ): RedisURI {
        return RedisURI.builder()
            .withHost(host)
            .withPort(port)
            .withTimeout(timeoutInMillis.milliseconds.toJavaDuration())
            .build()
    }

    /**
     * [RedisClient] 인스턴스를 생성합니다.
     *
     * ```
     * val client = LettuceClients.clientOf("redis://localhost:6379")
     * ```
     *
     * @param url Redis Server URL (e.g. redis://localhost:6379)
     * @return [RedisClient] instance
     */
    fun clientOf(url: String): RedisClient = clientOf(RedisURI.create(url))

    /**
     * [RedisClient] 인스턴스를 생성합니다.
     *
     * ```
     * val client = LettuceClients.clientOf(RedisURI.create("redis://localhost:6379"))
     * ```
     *
     * @param redisUri Redis Server URI
     * @return [RedisClient] instance
     */
    fun clientOf(redisUri: RedisURI): RedisClient = RedisClient.create(redisUri)

    /**
     * [RedisClient] 인스턴스를 생성합니다.
     *
     * ```
     * val client = LettuceClients.clientOf(ClientResources.create())
     * ```
     *
     * @param clientResources [ClientResources] instance
     * @return [RedisClient] instance
     */
    fun clientOf(clientResources: ClientResources): RedisClient = RedisClient.create(clientResources)

    /**
     * [RedisClient] 인스턴스를 생성합니다.
     *
     * ```
     * val client = LettuceClients.clientOf("localhost", 6379, 3000)
     * ```
     *
     * @param host  redis server host
     * @param port  redis server port
     * @param timeoutInMillis connectim timeout in milliseconds
     * @return [RedisClient] instance
     */
    fun clientOf(
        host: String = RedisConst.DEFAULT_HOST,
        port: Int = RedisConst.DEFAULT_PORT,
        timeoutInMillis: Long = RedisConst.DEFAULT_TIMEOUT_MILLIS,
    ): RedisClient =
        clientOf(getRedisURI(host, port, timeoutInMillis))

    /**
     * [client]를 이용하여 [StatefulRedisConnection]을 생성합니다. (sync)
     *
     * ```
     * val connection = LettuceClients.connect(client)
     * ```
     */
    fun connect(client: RedisClient): StatefulRedisConnection<String, String> =
        client.connect()

    /**
     * [client]와 [codec]를 이용하여 [StatefulRedisConnection]을 생성합니다. (sync)
     *
     * ```
     * val connection = LettuceClients.connect(client, StringCodec.UTF8)
     * ```
     */
    fun <V: Any> connect(client: RedisClient, codec: RedisCodec<String, V>): StatefulRedisConnection<String, V> =
        client.connect(codec)

    /**
     * [client]를 이용하여 [RedisCommands]`<String, String>` 를 생성합니다.
     *
     * ```
     * val commands = LettuceClients.commands(client)
     * ```
     */
    fun commands(client: RedisClient): RedisCommands<String, String> =
        connect(client).sync()

    /**
     * [client]와 [codec]를 이용하여 [RedisCommands]`<String, V>` 를 생성합니다.
     *
     * ```
     * val commands = LettuceClients.commands(client, StringCodec.UTF8)
     * ```
     */
    fun <V: Any> commands(client: RedisClient, codec: RedisCodec<String, V>): RedisCommands<String, V> =
        connect(client, codec).sync()

    /**
     * [client]를 이용하여 [RedisAsyncCommands]`<String, String>` 를 생성합니다.
     *
     * ```
     * val asyncCommands = LettuceClients.asyncCommands(client)
     * ```
     */
    fun asyncCommands(client: RedisClient): RedisAsyncCommands<String, String> =
        connect(client).async()

    /**
     * [client]와 [codec]를 이용하여 [RedisAsyncCommands]`<String, V>` 를 생성합니다.
     *
     * ```
     * val asyncCommands = LettuceClients.asyncCommands(client, StringCodec.UTF8)
     * ```
     */
    fun <V: Any> asyncCommands(client: RedisClient, codec: RedisCodec<String, V>): RedisAsyncCommands<String, V> =
        connect(client, codec).async()
}
