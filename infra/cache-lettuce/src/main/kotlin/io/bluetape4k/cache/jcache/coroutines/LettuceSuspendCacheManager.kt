package io.bluetape4k.cache.jcache.coroutines

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.info
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec
import io.bluetape4k.support.requireNotBlank
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.atomicfu.atomic
import java.util.concurrent.ConcurrentHashMap

@OptIn(ExperimentalLettuceCoroutinesApi::class)
@Suppress("UNCHECKED_CAST")
class LettuceSuspendCacheManager(
    val redisClient: RedisClient,
    val ttlSeconds: Long? = null,
    val codec: LettuceBinaryCodec<Any>? = null,
) {

    companion object: KLoggingChannel()

    private val caches = ConcurrentHashMap<String, LettuceSuspendCache<out Any>>()

    private val closed = atomic(false)

    private fun checkNotClosed() {
        if (isClosed) {
            error("LettuceSuspendCacheManager is closed.")
        }
    }

    val isClosed by closed

    fun <V: Any> getOrCreate(
        cacheName: String,
        ttlSeconds: Long? = null,
        codec: LettuceBinaryCodec<V>? = null,
    ): LettuceSuspendCache<V> {
        checkNotClosed()
        cacheName.requireNotBlank("cacheName")

        return caches.computeIfAbsent(cacheName) { name ->
            log.info { "Create LettuceSuspendCache. name=$name" }

            val conn = redisClient.connect(codec ?: this@LettuceSuspendCacheManager.codec)
            val commands = conn.coroutines() as RedisCoroutinesCommands<String, V>

            LettuceSuspendCache(
                name,
                commands,
                ttlSeconds ?: this@LettuceSuspendCacheManager.ttlSeconds,
                this@LettuceSuspendCacheManager
            )
        } as LettuceSuspendCache<V>
    }

    fun <V: Any> getCache(cacheName: String): LettuceSuspendCache<V>? {
        checkNotClosed()
        cacheName.requireNotBlank("cacheName")

        return caches[cacheName] as? LettuceSuspendCache<V>
    }

    suspend fun destroyCache(cacheName: String) {
        checkNotClosed()
        cacheName.requireNotBlank("cacheName")

        caches[cacheName]?.let { cache ->
            log.info { "Destroy LettuceSuspendCache. name=$cacheName" }
            cache.clear()
            caches.remove(cacheName)
        }
    }

    fun closeCache(cache: LettuceSuspendCache<*>) {
        caches.remove(cache.cacheName)
    }

    suspend fun close() {
        if (isClosed) {
            return
        }
        if (closed.compareAndSet(expect = false, update = true)) {
            log.info { "Close LettuceSuspendCacheManager." }

            caches.values.forEach { cache ->
                runCatching { cache.close() }
            }
        }
    }

}
