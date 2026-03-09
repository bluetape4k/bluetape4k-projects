package io.bluetape4k.cache.nearcache

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.lettuce.core.TrackingArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.push.PushListener
import io.lettuce.core.codec.StringCodec
import kotlinx.atomicfu.atomic
import java.nio.ByteBuffer

/**
 * Redis RESP3 CLIENT TRACKING 기반 local cache invalidation 리스너.
 *
 * Redis 서버에서 키가 변경될 때 CLIENT TRACKING이 invalidation push 메시지를 보내고,
 * 수신 즉시 [LettuceLocalCache.invalidate]를 호출해 로컬 캐시 항목을 무효화한다.
 *
 * ## Prefix Key 처리
 * Redis key는 `{cacheName}:{originalKey}` 형태로 저장된다.
 * invalidation 메시지에서 수신한 key에서 cacheName prefix를 제거한 후
 * localCache를 무효화한다. prefix가 일치하지 않는 key는 다른 cacheName의
 * invalidation이므로 무시한다.
 *
 * - `CLIENT TRACKING ON NOLOOP`: 자신이 쓴 키는 invalidation을 받지 않는다.
 * - `PushMessage.getContent()`: content[0] = type ByteBuffer, content[1] = keys List<ByteBuffer> or null
 *
 * @param V 값 타입
 */
class TrackingInvalidationListener<V: Any>(
    private val frontCache: LettuceLocalCache<String, V>,
    private val connection: StatefulRedisConnection<String, V>,
    private val cacheName: String,
): AutoCloseable {

    companion object: KLogging() {
        private val trackingEnabled = TrackingArgs.Builder.enabled().noloop()
        private val trackingDisabled = TrackingArgs.Builder.enabled(false)
    }

    private val started = atomic(false)

    /**
     * invalidate push 메시지를 처리하는 PushListener.
     * content[0] = type (ByteBuffer), content[1] = key list (List<ByteBuffer>) or null (= full flush)
     */
    private val pushListener = PushListener { message ->
        if (message.type == "invalidate") {
            handleInvalidation(message.content)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun handleInvalidation(content: List<Any?>) {
        // content[0] = type string as ByteBuffer (already matched "invalidate")
        // content[1] = null (flush all) or List<ByteBuffer> (key bytes)
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
     */
    fun start() {
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
