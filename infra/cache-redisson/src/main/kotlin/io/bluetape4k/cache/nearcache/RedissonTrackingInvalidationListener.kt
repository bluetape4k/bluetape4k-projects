package io.bluetape4k.cache.nearcache

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.bluetape4k.redis.lettuce.RedisCommandSupports
import io.lettuce.core.RedisClient
import io.lettuce.core.TrackingArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.push.PushListener
import io.lettuce.core.codec.StringCodec
import kotlinx.atomicfu.atomic
import java.nio.ByteBuffer

/**
 * Redis RESP3 CLIENT TRACKING 기반 로컬 캐시 invalidation 리스너 (Redisson 하이브리드 전용).
 *
 * Redis 6.0 이상의 RESP3 프로토콜 전용 기능으로, `CLIENT TRACKING ON` 명령을 통해
 * Redis 서버가 클라이언트에게 키 변경 사실을 push 방식으로 알린다.
 * Redisson이 데이터를 쓸 때 Redis 서버가 invalidation push 메시지를 보내고,
 * 수신 즉시 [RedissonLocalCache.invalidate]를 호출해 로컬 캐시 항목을 무효화한다.
 *
 * ## RESP3 전용
 * RESP2 기반 연결에서는 push 메시지를 수신할 수 없으므로,
 * [RedissonResp3NearCacheConfig.useRespProtocol3]가 `true`일 때만 활성화해야 한다.
 *
 * ## NOLOOP 옵션
 * `CLIENT TRACKING ON NOLOOP` 옵션으로 활성화하므로, 동일 Lettuce 연결에서 직접 쓴 키에 대해서는
 * invalidation push를 받지 않는다. 단, Redisson 데이터 연결과 Lettuce tracking 연결은
 * 서로 다른 별도 연결이므로, Redisson을 통해 쓴 키는 NOLOOP 제외 대상이 아니다.
 * 따라서 자신이 Redisson으로 쓴 키도 Lettuce tracking 연결에 invalidation이 전파될 수 있다.
 *
 * ## Push 리스너 실행 컨텍스트
 * [pushListener]는 Lettuce 내부 Netty I/O 스레드에서 실행된다.
 * 따라서 [handleInvalidation] 내부는 빠르게 완료해야 하며, 블로킹 작업을 수행해서는 안 된다.
 *
 * ## Prefix Key 처리
 * Redis key는 `{cacheName}:{originalKey}` 형태로 저장된다.
 * invalidation 메시지에서 수신한 key에서 cacheName prefix를 제거한 후
 * localCache를 무효화한다. prefix가 일치하지 않는 key는 다른 cacheName의
 * invalidation이므로 무시한다.
 *
 * @param V 값 타입
 * @param frontCache invalidation 대상 로컬 캐시
 * @param connection Lettuce tracking 전용 연결 (RESP3 프로토콜)
 * @param cacheName 이 리스너가 처리할 캐시 이름 (prefix 필터링에 사용)
 * @param redisClient Redis 버전 체크에 사용할 [RedisClient] (null이면 버전 체크 생략)
 */
class RedissonTrackingInvalidationListener<V : Any>(
    private val frontCache: RedissonLocalCache<String, V>,
    private val connection: StatefulRedisConnection<String, String>,
    private val cacheName: String,
    private val redisClient: RedisClient? = null,
) : AutoCloseable {

    companion object : KLogging() {
        private val trackingEnabled = TrackingArgs.Builder.enabled().noloop()
        private val trackingDisabled = TrackingArgs.Builder.enabled(false)
    }

    private val started = atomic(false)

    /**
     * Redis 서버로부터 수신한 invalidation push 메시지를 처리하는 [PushListener].
     *
     * Netty I/O 스레드에서 호출되므로 빠르게 반환해야 한다.
     * `invalidate` 타입 메시지만 처리하며, 그 외 타입은 무시한다.
     * - `content[0]`: 메시지 타입 ([ByteBuffer])
     * - `content[1]`: 무효화할 키 목록 (`List<ByteBuffer>`) 또는 `null` (전체 flush)
     */
    private val pushListener = PushListener { message ->
        if (message.type == "invalidate") {
            handleInvalidation(message.content)
        }
    }

    /**
     * invalidation push 메시지의 content를 파싱하여 로컬 캐시를 무효화한다.
     *
     * Netty I/O 스레드에서 실행되므로 블로킹 작업 없이 빠르게 완료해야 한다.
     *
     * - [content] 두 번째 요소가 `null`이면 전체 flush ([RedissonLocalCache.clear]) 수행
     * - [ByteBuffer] 단일 키 또는 `List<ByteBuffer>` 다중 키를 지원한다
     * - `{cacheName}:` prefix가 일치하는 키만 처리하고 나머지는 무시한다
     *
     * @param content push 메시지의 content 리스트
     */
    @Suppress("UNCHECKED_CAST")
    private fun handleInvalidation(content: List<Any?>) {
        val keysRaw = if (content.size >= 2) content[1] else null

        if (keysRaw == null) {
            log.debug { "Received full invalidation flush" }
            frontCache.clear()
            return
        }

        val prefix = "${cacheName}:"
        val keys = when (keysRaw) {
            is List<*>    -> (keysRaw as List<ByteBuffer?>)
                .filterNotNull()
                .mapNotNull { buf ->
                    val fullKey = StringCodec.UTF8.decodeKey(buf.duplicate())
                    if (fullKey.startsWith(prefix)) fullKey.removePrefix(prefix) else null
                }
            is ByteBuffer -> {
                val fullKey = StringCodec.UTF8.decodeKey(keysRaw.duplicate())
                if (fullKey.startsWith(prefix)) listOf(fullKey.removePrefix(prefix)) else emptyList()
            }
            else          -> emptyList()
        }

        if (keys.isNotEmpty()) {
            log.debug { "Invalidating ${keys.size} keys from local cache: $keys" }
            frontCache.invalidateAll(keys)
        }
    }

    /**
     * CLIENT TRACKING을 활성화하고 push 리스너를 등록한다.
     *
     * [redisClient]가 제공된 경우, `CLIENT` 명령어 지원 여부를 먼저 확인한다.
     * Redis 6.0 미만 등 `CLIENT TRACKING`을 지원하지 않는 서버에서는 경고 로그를 출력하고
     * tracking을 시작하지 않는다.
     */
    fun start() {
        if (redisClient != null && !RedisCommandSupports.supports(redisClient, "CLIENT")) {
            log.warn { "Redis 서버가 CLIENT 명령어를 지원하지 않아 CLIENT TRACKING을 비활성화합니다. cacheName=$cacheName" }
            return
        }
        if (started.compareAndSet(expect = false, update = true)) {
            try {
                connection.addListener(pushListener)
                connection.sync().clientTracking(trackingEnabled)
                log.debug { "CLIENT TRACKING (RESP3) enabled for cacheName=$cacheName" }
            } catch (e: Exception) {
                started.value = false
                connection.removeListener(pushListener)
                log.warn(e) { "Failed to enable CLIENT TRACKING: ${e.message}" }
                throw e
            }
        }
    }

    /**
     * CLIENT TRACKING을 비활성화하고 push 리스너를 제거한다.
     */
    override fun close() {
        if (started.compareAndSet(expect = true, update = false)) {
            runCatching {
                connection.sync().clientTracking(trackingDisabled)
            }.onFailure { e ->
                log.warn(e) { "Failed to disable CLIENT TRACKING: ${e.message}" }
            }
            connection.removeListener(pushListener)
            log.debug { "CLIENT TRACKING disabled and listener removed for cacheName=$cacheName" }
        }
    }
}
