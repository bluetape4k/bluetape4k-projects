package io.bluetape4k.redis.lettuce

import io.bluetape4k.logging.KLogging
import io.lettuce.core.ClientOptions
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.SocketOptions
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.resource.ClientResources
import io.lettuce.core.resource.DefaultClientResources
import java.time.Duration
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

    private val NCPU: Int = Runtime.getRuntime().availableProcessors()

    /**
     * NCPU 크기로 튜닝된 공유 [ClientResources] 싱글톤.
     * 여러 클라이언트 생성 시 이벤트 루프 스레드 풀을 공유합니다.
     */
    @JvmField
    val DEFAULT_CLIENT_RESOURCES: ClientResources =
        DefaultClientResources.builder()
            .ioThreadPoolSize(NCPU)
            .computationThreadPoolSize(NCPU)
            .build()

    // 개선: connect() 시 직렬화된 접근이 필요하지만 monitor lock 은 Virtual Thread 를
    //       pin 시키므로 ReentrantLock 으로 대체합니다 (프로젝트 규칙: VT 에서 synchronized 금지).
    private val defaultConnectionLocks = ConcurrentHashMap<RedisClient, ReentrantLock>()
    private val codecConnectionLocks = ConcurrentHashMap<CodecConnectionKey<*>, ReentrantLock>()

    @JvmField
    val DEFAULT_REDIS_URI: RedisURI = getRedisURI()

    // 개선: connectTimeout 하드코딩 제거 → 시스템 프로퍼티로 SRE 튜닝 가능.
    //       -Dbluetape4k.lettuce.connectTimeoutMs=5000 (기본값 5000ms)
    private val DEFAULT_CONNECT_TIMEOUT: Duration =
        Duration.ofMillis(
            System.getProperty("bluetape4k.lettuce.connectTimeoutMs", "5000").toLong()
        )

    private fun buildTunedClientOptions(): ClientOptions {
        val socketOptions = SocketOptions.builder()
            .keepAlive(true)
            .tcpNoDelay(true)
            .connectTimeout(DEFAULT_CONNECT_TIMEOUT)
            .build()
        return ClientOptions.builder()
            .socketOptions(socketOptions)
            .build()
    }

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
    fun clientOf(redisUri: RedisURI): RedisClient =
        RedisClient.create(DEFAULT_CLIENT_RESOURCES, redisUri).apply { setOptions(buildTunedClientOptions()) }

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
    fun clientOf(clientResources: ClientResources): RedisClient =
        RedisClient.create(clientResources).apply { setOptions(buildTunedClientOptions()) }

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

    /**
     * 공유 [DEFAULT_CLIENT_RESOURCES]를 종료합니다.
     * 애플리케이션 종료 시 호출하세요.
     */
    fun shutdown() {
        runCatching { DEFAULT_CLIENT_RESOURCES.shutdown().get() }
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

/**
 * [StatefulRedisConnection]의 autoFlushCommands를 비활성화하고 [block] 내 명령을
 * 한 번의 flushCommands()로 파이프라인 전송합니다. await는 반드시 블록 외부에서 수행하세요.
 *
 * ```kotlin
 * val futures = connection.withPipeline { cmd ->
 *     (0 until 1000).map { i -> cmd.set("key:$i", "value") }
 * }
 * futures.map { async { it.await() } }.awaitAll()
 * ```
 */
fun <K, V, T> StatefulRedisConnection<K, V>.withPipeline(
    block: (RedisAsyncCommands<K, V>) -> T,
): T {
    setAutoFlushCommands(false)
    return try {
        block(async()).also { flushCommands() }
    } finally {
        setAutoFlushCommands(true)
    }
}
