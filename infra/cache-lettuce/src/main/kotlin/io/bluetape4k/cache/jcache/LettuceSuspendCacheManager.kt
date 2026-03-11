package io.bluetape4k.cache.jcache

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.info
import io.bluetape4k.redis.lettuce.RedisCommandSupports
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
/**
 * Lettuce 기반 [LettuceSuspendCache] 인스턴스의 생성/조회/종료를 관리합니다.
 *
 * ## 동작/계약
 * - `cacheName`을 키로 내부 `ConcurrentHashMap`에 캐시 래퍼를 저장/재사용합니다.
 * - 매니저가 닫히면(`isClosed == true`) 대부분의 공개 API가 즉시 예외를 발생시킵니다.
 * - `getOrCreate`는 같은 이름에 대해 기존 캐시를 재사용하고, 없을 때만 Redis 연결을 생성합니다.
 *
 * ```kotlin
 * val manager = LettuceSuspendCacheManager(redisClient, ttlSeconds = 60)
 * val users = manager.getOrCreate<String>("users")
 * val same = manager.getOrCreate<String>("users")
 * // users === same
 * ```
 */
class LettuceSuspendCacheManager(
    /** 캐시 생성에 사용할 Redis 클라이언트입니다. */
    val redisClient: RedisClient,
    /** 기본 TTL(초). `getOrCreate`에 개별 TTL이 없을 때 사용됩니다. */
    val ttlSeconds: Long? = null,
    /** 기본 바이너리 codec. `null`이면 Lettuce 기본 codec을 사용합니다. */
    val codec: LettuceBinaryCodec<Any>? = null,
) {

    companion object: KLoggingChannel()

    private val caches = ConcurrentHashMap<String, LettuceSuspendCache<out Any>>()

    private val closed = atomic(false)

    private val supportsHSetEx: Boolean by lazy {
        RedisCommandSupports.supportsHSetEx(redisClient)
    }

    private fun checkNotClosed() {
        if (isClosed) {
            error("LettuceSuspendCacheManager is closed.")
        }
    }

    /** 매니저 종료 여부를 반환합니다. */
    val isClosed by closed

    /**
     * 이름에 해당하는 캐시를 조회하거나 새로 생성해 반환합니다.
     *
     * ## 동작/계약
     * - 매니저가 닫혀 있으면 `IllegalStateException`이 발생합니다.
     * - [cacheName] blank 입력은 `requireNotBlank("cacheName")`로 `IllegalArgumentException`이 발생합니다.
     * - [ttlSeconds], [codec]가 null이면 매니저 기본값을 사용합니다.
     *
     * ```kotlin
     * val cache = manager.getOrCreate<String>("users")
     * val same = manager.getOrCreate<String>("users")
     * // cache === same
     * ```
     */
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
                this@LettuceSuspendCacheManager,
                supportsHSetEx = supportsHSetEx,
                closeResource = { conn.close() },
            )
        } as LettuceSuspendCache<V>
    }

    /**
     * 이름으로 기존 캐시 인스턴스를 조회합니다.
     *
     * ## 동작/계약
     * - 매니저 종료 상태에서는 `IllegalStateException`이 발생합니다.
     * - [cacheName] blank 입력은 `IllegalArgumentException`을 발생시킵니다.
     * - 캐시가 없으면 `null`을 반환하며 새 캐시는 생성하지 않습니다.
     *
     * ```kotlin
     * val cache = manager.getCache<String>("users")
     * // cache == null || cache.cacheName == "users"
     * ```
     */
    fun <V: Any> getCache(cacheName: String): LettuceSuspendCache<V>? {
        checkNotClosed()
        cacheName.requireNotBlank("cacheName")

        return caches[cacheName] as? LettuceSuspendCache<V>
    }

    /**
     * 지정한 이름의 캐시를 비우고 매니저에서 제거합니다.
     *
     * ## 동작/계약
     * - 매니저가 닫혀 있으면 `IllegalStateException`이 발생합니다.
     * - [cacheName] blank 입력은 `IllegalArgumentException`을 발생시킵니다.
     * - 캐시가 존재할 때만 `clear()`를 수행하고 맵에서 제거합니다.
     *
     * ```kotlin
     * manager.destroyCache("users")
     * val deleted = manager.getCache<String>("users")
     * // deleted == null
     * ```
     */
    suspend fun destroyCache(cacheName: String) {
        checkNotClosed()
        cacheName.requireNotBlank("cacheName")

        caches[cacheName]?.let { cache ->
            log.info { "Destroy LettuceSuspendCache. name=$cacheName" }
            cache.clear()
            caches.remove(cacheName)
        }
    }

    /**
     * 캐시 이름을 기준으로 매니저 등록 목록에서 제거합니다.
     *
     * ## 동작/계약
     * - Redis 데이터 삭제 없이 내부 맵에서만 제거합니다.
     * - 존재하지 않는 캐시는 무시됩니다.
     * - 매니저 종료 여부와 관계없이 호출 가능합니다.
     *
     * ```kotlin
     * manager.closeCache(cache)
     * // manager.getCache<Any>(cache.cacheName) == null
     * ```
     */
    fun closeCache(cache: LettuceSuspendCache<*>) {
        caches.remove(cache.cacheName)
    }

    /**
     * 매니저를 종료하고 등록된 캐시들을 순차적으로 닫습니다.
     *
     * ## 동작/계약
     * - 최초 1회만 실제 종료 로직이 수행되며 이후 호출은 즉시 반환합니다.
     * - 각 캐시 종료 중 예외는 `runCatching`으로 무시하고 다음 캐시 종료를 계속합니다.
     * - 종료 후 `getOrCreate/getCache/destroyCache`는 `IllegalStateException`을 발생시킵니다.
     *
     * ```kotlin
     * manager.close()
     * // manager.isClosed == true
     * ```
     */
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
