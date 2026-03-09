package io.bluetape4k.cache.nearcache

import com.github.benmanes.caffeine.cache.stats.CacheStats
import io.bluetape4k.concurrent.virtualthread.virtualThread
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requireNotBlank
import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.lettuce.core.KeyScanCursor
import io.lettuce.core.RedisClient
import io.lettuce.core.ScanArgs
import io.lettuce.core.ScanCursor
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.codec.StringCodec
import kotlinx.atomicfu.atomic
import org.redisson.api.RedissonClient
import org.redisson.client.codec.Codec
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

/**
 * Resilient Redisson + Lettuce RESP3 하이브리드 Near Cache (2-tier: Caffeine front + Redisson back) - 동기(Blocking) 구현.
 *
 * [RedissonResp3NearCache]와 동일한 public API를 유지하면서,
 * Redisson back cache 쓰기에 대해 다음을 추가한다:
 *
 * - **write-behind**: put/remove는 front cache에 즉시 반영하고,
 *   Redisson 쓰기는 [LinkedBlockingQueue]에 큐잉하여 daemon thread가 순차 처리
 * - **retry**: consumer thread에서 resilience4j [Retry]로 재시도 (지수 백오프 옵션)
 * - **get graceful degradation**: Redisson GET 실패 시 front 값 반환 또는 null
 * - **invalidation**: Lettuce RESP3 CLIENT TRACKING push로 front cache 무효화
 *
 * ```
 * Application (blocking)
 *     |
 * [ResilientRedissonResp3NearCache]
 *     |
 * +---+--------+
 * |            |
 * Front        LinkedBlockingQueue<BackCacheCommand>
 * Caffeine        |
 * (즉시반영)   Daemon Thread (consumer)
 *              (Retry.executeRunnable { redisson.getBucket().set/delete })
 * ```
 *
 * @param V 값 타입 (키는 항상 String)
 */
class ResilientRedissonResp3NearCache<V: Any>(
    private val redisson: RedissonClient,
    private val redisClient: RedisClient,
    private val redissonCodec: Codec = RedissonNearCache.defaultNearCacheCodec,
    private val config: ResilientRedissonResp3NearCacheConfig = ResilientRedissonResp3NearCacheConfig(
        RedissonResp3NearCacheConfig()
    ),
): AutoCloseable {

    companion object: KLogging() {
        /**
         * String 키/값 타입의 Resilient Redisson RESP3 Near Cache를 생성한다.
         */
        operator fun invoke(
            redisson: RedissonClient,
            redisClient: RedisClient,
            config: ResilientRedissonResp3NearCacheConfig = ResilientRedissonResp3NearCacheConfig(
                RedissonResp3NearCacheConfig()
            ),
        ): ResilientRedissonResp3NearCache<String> =
            ResilientRedissonResp3NearCache(redisson, redisClient, RedissonNearCache.defaultNearCacheCodec, config)
    }

    val cacheName: String get() = config.cacheName

    private val closed = atomic(false)
    val isClosed by closed

    private val frontCache: RedissonLocalCache<String, V> = CaffeineRedissonLocalCache(config.base)

    // Lettuce tracking 전용 연결 (StringCodec — key 이름만 필요, 값 타입 무관)
    private val trackingConnection: StatefulRedisConnection<String, String> =
        redisClient.connect(StringCodec.UTF8)
    private val trackingSync: RedisCommands<String, String> = trackingConnection.sync()

    private val trackingListener: RedissonTrackingInvalidationListener<V> =
        RedissonTrackingInvalidationListener(frontCache, trackingConnection, config.cacheName)

    private val retry: Retry = buildRetry()
    private val queue = LinkedBlockingQueue<BackCacheCommand<String, V>>(config.writeQueueCapacity)

    /**
     * write-behind로 삭제 요청된 키 집합 (tombstone).
     */
    private val tombstones: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /**
     * clearAll 호출 후 ClearBack이 처리될 때까지 Redis read를 차단하는 플래그.
     */
    private val clearPending = atomic(false)

    private val consumerThread: Thread = virtualThread(
        name = "resilient-redisson-writer-${config.cacheName}",
    ) {
        while (!closed.value) {
            try {
                val cmd = queue.take()
                try {
                    retry.executeRunnable { applyCommand(cmd) }
                } catch (e: Exception) {
                    log.error(e) { "Redisson write failed after ${config.retryMaxAttempts} retries, dropping command: $cmd" }
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
        }
    }

    init {
        if (config.useRespProtocol3) {
            runCatching { trackingListener.start() }
                .onFailure { e ->
                    log.warn(e) { "CLIENT TRACKING start failed, cache will work without invalidation" }
                }
        }
    }

    private fun buildRetry(): Retry {
        val intervalFn = if (config.retryExponentialBackoff) {
            IntervalFunction.ofExponentialBackoff(config.retryWaitDuration, 2.0)
        } else {
            IntervalFunction.of(config.retryWaitDuration)
        }
        val retryConfig = RetryConfig.custom<Any>()
            .maxAttempts(config.retryMaxAttempts)
            .intervalFunction(intervalFn)
            .build()
        return Retry.of("${config.cacheName}-write-retry", retryConfig)
    }

    private fun applyCommand(cmd: BackCacheCommand<String, V>) {
        when (cmd) {
            is BackCacheCommand.Put -> {
                setRedis(cmd.key, cmd.value)
                // write 후 async tracking 활성화 (write 이후이므로 경쟁 조건 무관)
                trackingConnection.async().get(config.redisKey(cmd.key))
            }
            is BackCacheCommand.PutAll -> {
                cmd.entries.forEach { (k, v) ->
                    setRedis(k, v)
                    trackingConnection.async().get(config.redisKey(k))
                }
            }
            is BackCacheCommand.Remove -> {
                redisson.getBucket<V>(config.redisKey(cmd.key), redissonCodec).delete()
                tombstones.remove(cmd.key)
            }
            is BackCacheCommand.RemoveAll -> {
                val rkeys = cmd.keys.map { config.redisKey(it) }.toTypedArray()
                if (rkeys.isNotEmpty()) redisson.keys.delete(*rkeys)
                tombstones.removeAll(cmd.keys)
            }
            is BackCacheCommand.ClearBack -> {
                clearBack()
                tombstones.clear()
                clearPending.value = false
            }
        }
    }

    /**
     * 키에 대한 값을 조회한다.
     * - front hit → return
     * - front miss → Redisson GET → front populate + tracking 활성화 → return
     * - Redisson 실패 시 [GetFailureStrategy]에 따라 처리
     */
    fun get(key: String): V? {
        key.requireNotBlank("key")

        if (tombstones.contains(key) || clearPending.value) return null
        frontCache.get(key)?.let { return it }

        return when (config.getFailureStrategy) {
            GetFailureStrategy.RETURN_FRONT_OR_NULL ->
                runCatching {
                    @Suppress("UNCHECKED_CAST")
                    redisson.getBucket<V>(config.redisKey(key), redissonCodec).get()
                }
                    .onFailure { e -> log.warn(e) { "Redisson GET failed for key=$key, returning null" } }
                    .getOrNull()
                    ?.also { value ->
                        frontCache.put(key, value)
                        trackingSync.get(config.redisKey(key))
                    }

            GetFailureStrategy.PROPAGATE_EXCEPTION -> {
                @Suppress("UNCHECKED_CAST")
                redisson.getBucket<V>(config.redisKey(key), redissonCodec).get()?.also { value ->
                    frontCache.put(key, value)
                    trackingSync.get(config.redisKey(key))
                }
            }
        }
    }

    /**
     * 여러 키에 대한 값을 한 번에 조회한다.
     */
    fun getAll(keys: Set<String>): Map<String, V> {
        if (clearPending.value) return emptyMap()
        val result = frontCache.getAll(keys).toMutableMap()
        val missedKeys = (keys - result.keys).filter { !tombstones.contains(it) }

        missedKeys.forEach { key ->
            runCatching {
                @Suppress("UNCHECKED_CAST")
                redisson.getBucket<V>(config.redisKey(key), redissonCodec).get()
            }
                .onFailure { e -> log.warn(e) { "Redisson GET failed for key=$key during getAll" } }
                .getOrNull()
                ?.let { value ->
                    result[key] = value
                    frontCache.put(key, value)
                    trackingSync.get(config.redisKey(key))
                }
        }
        return result
    }

    /**
     * 키-값 쌍을 저장한다.
     * - front cache 즉시 반영
     * - Redisson write는 queue로 큐잉 (write-behind)
     */
    fun put(key: String, value: V) {
        key.requireNotBlank("key")
        tombstones.remove(key)
        frontCache.put(key, value)
        if (!queue.offer(BackCacheCommand.Put(key, value))) {
            log.warn { "Write queue full, dropping Put for key=$key" }
        }
    }

    /**
     * 여러 키-값 쌍을 저장한다.
     */
    fun putAll(map: Map<String, V>) {
        tombstones.removeAll(map.keys)
        frontCache.putAll(map)
        if (!queue.offer(BackCacheCommand.PutAll(map))) {
            log.warn { "Write queue full, dropping PutAll for ${map.size} entries" }
        }
    }

    /**
     * 해당 키가 없을 때만 저장한다 (put-if-absent).
     * @return 기존 값(있었으면) 또는 null(새로 저장됨)
     */
    fun putIfAbsent(key: String, value: V): V? {
        key.requireNotBlank("key")

        val existing = get(key)
        if (existing != null) return existing

        @Suppress("UNCHECKED_CAST")
        val bucket = redisson.getBucket<V>(config.redisKey(key), redissonCodec)
        val setted = if (config.redisTtl != null) {
            bucket.setIfAbsent(value, config.redisTtl)
        } else {
            bucket.setIfAbsent(value)
        }
        return if (setted) {
            frontCache.put(key, value)
            null
        } else {
            @Suppress("UNCHECKED_CAST")
            bucket.get()
        }
    }

    /**
     * 기존 값을 새 값으로 교체한다 (키가 있을 때만).
     * @return 교체 성공 여부
     */
    fun replace(key: String, value: V): Boolean {
        key.requireNotBlank("key")
        if (tombstones.contains(key) || clearPending.value) return false

        // front cache에 있으면 write-behind 중이더라도 교체 허용 (back 반영 전)
        if (!frontCache.containsKey(key)) {
            @Suppress("UNCHECKED_CAST")
            val bucket = redisson.getBucket<V>(config.redisKey(key), redissonCodec)
            if (!bucket.isExists) return false
        }
        setRedis(key, value)
        frontCache.put(key, value)
        trackingConnection.async().get(config.redisKey(key))
        return true
    }

    /**
     * 기존 값이 [oldValue]와 같을 때만 [newValue]로 교체한다.
     */
    fun replace(key: String, oldValue: V, newValue: V): Boolean {
        val current = get(key) ?: return false
        if (current != oldValue) return false
        return replace(key, newValue)
    }

    /**
     * 조회 후 제거한다.
     */
    fun getAndRemove(key: String): V? {
        val value = get(key)
        if (value != null) remove(key)
        return value
    }

    /**
     * 조회 후 새 값으로 교체한다.
     */
    fun getAndReplace(key: String, value: V): V? {
        val existing = get(key) ?: return null
        put(key, value)
        return existing
    }

    /**
     * 해당 키가 캐시에 존재하는지 확인한다 (front or Redisson).
     */
    fun containsKey(key: String): Boolean {
        if (tombstones.contains(key) || clearPending.value) return false
        if (frontCache.containsKey(key)) return true
        return runCatching {
            redisson.getBucket<V>(config.redisKey(key), redissonCodec).isExists
        }
            .onFailure { e -> log.warn(e) { "Redisson isExists failed for key=$key" } }
            .getOrDefault(false)
    }

    /**
     * 키에 해당하는 캐시 항목을 제거한다.
     * - front cache 즉시 반영
     * - Redisson delete는 queue로 큐잉 (write-behind)
     */
    fun remove(key: String) {
        key.requireNotBlank("key")
        frontCache.remove(key)
        tombstones.add(key)
        if (!queue.offer(BackCacheCommand.Remove(key))) {
            log.warn { "Write queue full, dropping Remove for key=$key" }
        }
    }

    /**
     * 여러 키에 해당하는 캐시 항목을 제거한다.
     */
    fun removeAll(keys: Set<String>) {
        frontCache.removeAll(keys)
        tombstones.addAll(keys)
        if (!queue.offer(BackCacheCommand.RemoveAll(keys))) {
            log.warn { "Write queue full, dropping RemoveAll for ${keys.size} keys" }
        }
    }

    /**
     * 로컬 캐시만 비운다 (Redis 유지).
     */
    fun clearLocal() {
        frontCache.clear()
        log.debug { "Front cache cleared for cacheName=${config.cacheName}" }
    }

    /**
     * 로컬 캐시와 Redis를 모두 비운다 (write-behind).
     */
    fun clearAll() {
        clearLocal()
        clearPending.value = true
        if (!queue.offer(BackCacheCommand.ClearBack())) {
            log.warn { "Write queue full, dropping ClearBack for cacheName=${config.cacheName}" }
        }
    }

    /**
     * 로컬 캐시의 추정 크기.
     */
    fun localCacheSize(): Long = frontCache.estimatedSize()

    /**
     * Redis에서 이 cacheName에 속한 key의 개수를 반환한다.
     */
    fun backCacheSize(): Long {
        val pattern = "${config.cacheName}:*"
        var count = 0L
        var cursor: ScanCursor = ScanCursor.INITIAL
        do {
            val result: KeyScanCursor<String> =
                trackingSync.scan(cursor, ScanArgs.Builder.matches(pattern).limit(100))
            count += result.keys.size
            cursor = result
        } while (!result.isFinished)
        return count
    }

    /**
     * 로컬 캐시(Caffeine) 통계.
     */
    fun localStats(): CacheStats? = frontCache.stats()

    /**
     * 모든 리소스를 정리하고 연결을 닫는다.
     */
    override fun close() {
        if (closed.compareAndSet(expect = false, update = true)) {
            runCatching { consumerThread.interrupt() }
            runCatching { queue.clear() }
            runCatching { trackingListener.close() }
            runCatching { trackingConnection.close() }
            runCatching { frontCache.close() }
            log.debug { "ResilientRedissonResp3NearCache [${config.cacheName}] closed" }
        }
    }

    private fun clearBack() {
        val pattern = "${config.cacheName}:*"
        var cursor: ScanCursor = ScanCursor.INITIAL
        do {
            val result: KeyScanCursor<String> =
                trackingSync.scan(cursor, ScanArgs.Builder.matches(pattern).limit(100))
            if (result.keys.isNotEmpty()) {
                trackingSync.del(*result.keys.toTypedArray())
            }
            cursor = result
        } while (!result.isFinished)
    }

    @Suppress("UNCHECKED_CAST")
    private fun setRedis(key: String, value: V) {
        val bucket = redisson.getBucket<V>(config.redisKey(key), redissonCodec)
        val ttl = config.redisTtl
        if (ttl != null) {
            bucket.set(value, ttl)
        } else {
            bucket.set(value)
        }
    }
}
