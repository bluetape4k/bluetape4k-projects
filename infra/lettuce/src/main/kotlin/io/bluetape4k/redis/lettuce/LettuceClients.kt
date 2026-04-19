package io.bluetape4k.redis.lettuce

import io.bluetape4k.logging.KLogging
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.resource.ClientResources
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

/**
 * Lettuce 의 [RedisClient] 등을 생성해주는 유틸리티 클래스입니다.
 */
object LettuceClients: KLogging() {
    private data class CodecConnectionKey<V: Any>(
        val client: RedisClient,
        val codec: RedisCodec<String, V>,
    )

    private val defaultConnections = ConcurrentHashMap<RedisClient, StatefulRedisConnection<String, String>>()
    private val codecConnections = ConcurrentHashMap<CodecConnectionKey<*>, StatefulRedisConnection<String, *>>()

    // 개선: connect() 시 직렬화된 접근이 필요하지만 monitor lock 은 Virtual Thread 를
    //       pin 시키므로 ReentrantLock 으로 대체합니다 (프로젝트 규칙: VT 에서 synchronized 금지).
    private val defaultConnectionLocks = ConcurrentHashMap<RedisClient, ReentrantLock>()
    private val codecConnectionLocks = ConcurrentHashMap<CodecConnectionKey<*>, ReentrantLock>()

    @JvmField
    val DEFAULT_REDIS_URI: RedisURI = getRedisURI()

    /**
     * Redis 연결 정보를 담는 [RedisURI]를 생성합니다.
     *
     * ```kotlin
     * val uri = LettuceClients.getRedisURI(host = "localhost", port = 6379)
     * val client = RedisClient.create(uri)
     * val conn = client.connect()
     * ```
     *
     * @param host Redis 서버 호스트 (기본값: "localhost")
     * @param port Redis 서버 포트 (기본값: 6379)
     * @param timeoutInMillis 연결 타임아웃 밀리초
     * @return [RedisURI] 인스턴스
     */
    fun getRedisURI(
        host: String = LettuceConst.DEFAULT_HOST,
        port: Int = LettuceConst.DEFAULT_PORT,
        timeoutInMillis: Long = LettuceConst.DEFAULT_TIMEOUT_MILLIS,
    ): RedisURI =
        RedisURI
            .builder()
            .withHost(host)
            .withPort(port)
            .withTimeout(timeoutInMillis.milliseconds.toJavaDuration())
            .build()

    /**
     * [RedisClient] 인스턴스를 생성합니다.
     *
     * ```kotlin
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
     * ```kotlin
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
     * ```kotlin
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
     * ```kotlin
     * val client = LettuceClients.clientOf("localhost", 6379, 3000)
     * ```
     *
     * @param host  redis server host
     * @param port  redis server port
     * @param timeoutInMillis connectim timeout in milliseconds
     * @return [RedisClient] instance
     */
    fun clientOf(
        host: String = LettuceConst.DEFAULT_HOST,
        port: Int = LettuceConst.DEFAULT_PORT,
        timeoutInMillis: Long = LettuceConst.DEFAULT_TIMEOUT_MILLIS,
    ): RedisClient = clientOf(getRedisURI(host, port, timeoutInMillis))

    /**
     * [client]를 이용하여 [StatefulRedisConnection]을 생성합니다. (sync)
     *
     * ```kotlin
     * val connection = LettuceClients.connect(client)
     * ```
     */
    fun connect(client: RedisClient): StatefulRedisConnection<String, String> = defaultConnection(client)

    /**
     * [client]와 [codec]를 이용하여 [StatefulRedisConnection]을 생성합니다.
     *
     * ```kotlin
     * val connection = LettuceClients.connect(client, StringCodec.UTF8)
     * ```
     */
    fun <V: Any> connect(
        client: RedisClient,
        codec: RedisCodec<String, V>,
    ): StatefulRedisConnection<String, V> = connection(client, codec)

    /**
     * [client]를 이용하여 [RedisCommands]`<String, String>` 를 생성합니다.
     *
     * ```kotlin
     * val commands = LettuceClients.commands(client)
     * ```
     */
    fun commands(client: RedisClient): RedisCommands<String, String> = defaultConnection(client).sync()

    /**
     * [client]와 [codec]를 이용하여 [RedisCommands]`<String, V>` 를 생성합니다.
     *
     * ```kotlin
     * val commands = LettuceClients.commands(client, StringCodec.UTF8)
     * ```
     */
    fun <V: Any> commands(
        client: RedisClient,
        codec: RedisCodec<String, V>,
    ): RedisCommands<String, V> = connect(client, codec).sync()

    /**
     * [client]를 이용하여 [RedisAsyncCommands]`<String, String>` 를 생성합니다.
     *
     * ```kotlin
     * val asyncCommands = LettuceClients.asyncCommands(client)
     * ```
     */
    fun asyncCommands(client: RedisClient): RedisAsyncCommands<String, String> = defaultConnection(client).async()

    /**
     * [client]와 [codec]를 이용하여 [RedisAsyncCommands]`<String, V>` 를 생성합니다.
     *
     * ```kotlin
     * val asyncCommands = LettuceClients.asyncCommands(client, StringCodec.UTF8)
     * ```
     */
    fun <V: Any> asyncCommands(
        client: RedisClient,
        codec: RedisCodec<String, V>,
    ): RedisAsyncCommands<String, V> = connect(client, codec).async()

    /**
     * [client]를 이용하여 [RedisCoroutinesCommands]`<String, String>` 를 생성합니다.
     *
     * ```kotlin
     * val coroutinesCommands = LettuceClients.coroutinesCommands(client)
     * ```
     */
    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    fun coroutinesCommands(client: RedisClient): RedisCoroutinesCommands<String, String> =
        defaultConnection(client).coroutines()

    /**
     * [client]와 [codec]를 이용하여 [RedisCoroutinesCommands]`<String, V>` 를 생성합니다.
     *
     * ```kotlin
     * val coroutinesCommands = LettuceClients.coroutinesCommands(client, StringCodec.UTF8)
     * ```
     */
    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    fun <V: Any> coroutinesCommands(
        client: RedisClient,
        codec: RedisCodec<String, V>,
    ): RedisCoroutinesCommands<String, V> = connect(client, codec).coroutines()

    /**
     * 캐시된 connection을 정리하고 [client]를 종료합니다.
     */
    fun shutdown(client: RedisClient) {
        runCatching { defaultConnections.remove(client)?.close() }
        defaultConnectionLocks.remove(client)
        codecConnections.entries.removeIf { (key, conn) ->
            if (key.client == client) {
                runCatching { conn.close() }
                codecConnectionLocks.remove(key)
                true
            } else {
                false
            }
        }
        client.shutdown()
    }

    // 개선: ConcurrentHashMap.compute() 는 bucket lock 을 잡은 채로 lambda 를 실행하므로,
    //       blocking I/O 인 client.connect() 를 lambda 내부에서 호출하면
    //       같은 bucket 으로 해시되는 다른 key 의 연결 생성도 함께 블록됩니다.
    //       → 먼저 lock 없이 빠르게 조회 후 유효한 연결이면 즉시 반환하고,
    //         필요할 때만 client 단위 ReentrantLock 아래에서 connect() 를 수행합니다.

    private fun defaultConnection(client: RedisClient): StatefulRedisConnection<String, String> {
        defaultConnections[client]?.takeIf { it.isOpen }?.let { return it }
        val lock = defaultConnectionLocks.computeIfAbsent(client) { ReentrantLock() }
        return lock.withLock {
            defaultConnections[client]?.takeIf { it.isOpen }?.let { return@withLock it }
            val fresh = client.connect()
            val prev = defaultConnections.put(client, fresh)
            if (prev != null && prev !== fresh) runCatching { prev.close() }
            fresh
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <V: Any> connection(
        client: RedisClient,
        codec: RedisCodec<String, V>,
    ): StatefulRedisConnection<String, V> {
        val key = CodecConnectionKey(client, codec)
        (codecConnections[key] as? StatefulRedisConnection<String, V>)
            ?.takeIf { it.isOpen }
            ?.let { return it }
        val lock = codecConnectionLocks.computeIfAbsent(key) { ReentrantLock() }
        return lock.withLock {
            (codecConnections[key] as? StatefulRedisConnection<String, V>)
                ?.takeIf { it.isOpen }
                ?.let { return@withLock it }
            val fresh = client.connect(codec)
            val prev = codecConnections.put(key, fresh)
            if (prev != null && prev !== fresh) runCatching { prev.close() }
            fresh
        }
    }
}
