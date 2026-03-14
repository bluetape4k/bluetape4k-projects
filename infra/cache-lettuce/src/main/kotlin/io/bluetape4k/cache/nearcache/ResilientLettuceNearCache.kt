package io.bluetape4k.cache.nearcache

import com.github.benmanes.caffeine.cache.stats.CacheStats
import io.bluetape4k.concurrent.virtualthread.virtualThread
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.logging.warn
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
import io.bluetape4k.support.requireNotBlank
import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.lettuce.core.KeyScanCursor
import io.lettuce.core.MSetExArgs
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisFuture
import io.lettuce.core.ScanArgs
import io.lettuce.core.ScanCursor
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.codec.RedisCodec
import kotlinx.atomicfu.atomic
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

/**
 * Resilient Lettuce Near Cache (2-tier: Caffeine front + Redis back) - 동기(Blocking) 구현.
 *
 * [LettuceNearCache]와 동일한 public API를 유지하면서,
 * Redis back cache 쓰기에 대해 다음을 추가한다:
 *
 * - **write-behind**: put/remove는 front cache에 즉시 반영하고,
 *   Redis 쓰기는 [LinkedBlockingQueue]에 큐잉하여 daemon thread가 순차 처리
 * - **retry**: consumer thread에서 resilience4j [Retry]로 재시도 (지수 백오프 옵션)
 * - **get graceful degradation**: Redis GET 실패 시 front 값 반환 또는 null
 *
 * ```
 * Application (blocking)
 *     |
 * [ResilientLettuceNearCache]
 *     |
 * +---+--------+
 * |            |
 * Front        LinkedBlockingQueue<BackCacheCommand>
 * Caffeine        |
 * (즉시반영)   Daemon Thread (consumer)
 *              (Retry.executeRunnable { syncCommands.set/del })
 * ```
 *
 * @param V 값 타입 (키는 항상 String)
 */
class ResilientLettuceNearCache<V : Any>(
    private val redisClient: RedisClient,
    private val codec: RedisCodec<String, V> = LettuceBinaryCodecs.lz4Fory(),
    private val config: ResilientLettuceNearCacheConfig<String, V> =
        ResilientLettuceNearCacheConfig(
            LettuceNearCacheConfig()
        ),
) : AutoCloseable {
    companion object : KLogging() {
        /**
         * String 키/값 타입의 Resilient Near Cache를 생성한다.
         */
        operator fun invoke(
            redisClient: RedisClient,
            config: ResilientLettuceNearCacheConfig<String, String> =
                ResilientLettuceNearCacheConfig(
                    LettuceNearCacheConfig()
                ),
        ): ResilientLettuceNearCache<String> =
            ResilientLettuceNearCache(redisClient, LettuceBinaryCodecs.lz4Fory(), config)
    }

    val cacheName: String get() = config.cacheName

    private val closed = atomic(false)
    val isClosed by closed

    private val frontCache: LettuceLocalCache<String, V> = LettuceCaffeineLocalCache(config.base)
    private val connection: StatefulRedisConnection<String, V> = redisClient.connect(codec)
    private val syncCommands: RedisCommands<String, V> = connection.sync()
    private val trackingListener: TrackingInvalidationListener<V> =
        TrackingInvalidationListener(frontCache, connection, config.cacheName)

    private val retry: Retry = buildRetry()
    private val queue = LinkedBlockingQueue<BackCacheCommand<String, V>>(config.writeQueueCapacity)

    /**
     * write-behind로 삭제 요청된 키 집합 (tombstone).
     * remove 후 write-behind 완료 전까지 get/containsKey가 Redis를 읽어 stale 값을 반환하는 것을 방지.
     */
    private val tombstones: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /**
     * clearAll 호출 후 ClearBack이 처리될 때까지 Redis read를 차단하는 플래그.
     */
    private val clearPending = atomic(false)

    private val consumerThread: Thread =
        virtualThread(
            name = "resilient-near-cache-writer-${config.cacheName}"
        ) {
            while (!closed.value) {
                try {
                    val cmd = queue.take()
                    try {
                        retry.executeRunnable { applyCommand(cmd) }
                    } catch (e: Exception) {
                        log.error(
                            e
                        ) { "Redis write failed after ${config.retryMaxAttempts} retries, dropping command: $cmd" }
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
                    log.warn(e) { "CLIENT TRACKING start failed, cache will work without invalidation: ${e.message}" }
                }
        }
    }

    private fun buildRetry(): Retry {
        val intervalFn =
            if (config.retryExponentialBackoff) {
                IntervalFunction.ofExponentialBackoff(config.retryWaitDuration, 2.0)
            } else {
                IntervalFunction.of(config.retryWaitDuration)
            }
        val retryConfig =
            RetryConfig
                .custom<Any>()
                .maxAttempts(config.retryMaxAttempts)
                .intervalFunction(intervalFn)
                .build()
        return Retry.of("${config.cacheName}-write-retry", retryConfig)
    }

    private fun applyCommand(cmd: BackCacheCommand<String, V>) {
        when (cmd) {
            is BackCacheCommand.Put -> {
                setRedis(cmd.key, cmd.value)
            }
            is BackCacheCommand.PutAll -> {
                msetRedis(cmd.entries)
            }
            is BackCacheCommand.Remove -> {
                syncCommands.del(config.redisKey(cmd.key))
                tombstones.remove(cmd.key)
            }
            is BackCacheCommand.RemoveAll -> {
                val rKeys = cmd.keys.map { config.redisKey(it) }.toTypedArray()
                if (rKeys.isNotEmpty()) syncCommands.del(*rKeys)
                tombstones.removeAll(cmd.keys)
            }
            is BackCacheCommand.ClearBack -> {
                clearBackCache()
                tombstones.clear()
                clearPending.value = false
            }
        }
    }

    /**
     * 키에 대한 값을 조회한다.
     * - front hit → return
     * - front miss → Redis GET → front populate → return
     * - Redis 실패 시 [GetFailureStrategy]에 따라 처리
     */
    fun get(key: String): V? {
        key.requireNotBlank("key")

        if (tombstones.contains(key) || clearPending.value) return null
        frontCache.get(key)?.let { return it }

        return when (config.getFailureStrategy) {
            GetFailureStrategy.RETURN_FRONT_OR_NULL -> {
                runCatching { syncCommands.get(config.redisKey(key)) }
                    .onFailure { e -> log.warn(e) { "Redis GET failed for key=$key, returning null" } }
                    .getOrNull()
                    ?.also { value -> frontCache.put(key, value) }
            }
            GetFailureStrategy.PROPAGATE_EXCEPTION -> {
                syncCommands.get(config.redisKey(key))?.also { value ->
                    frontCache.put(key, value)
                }
            }
        }
    }

    /**
     * 여러 키에 대한 값을 한 번에 조회한다 (multi-get pipeline).
     */
    fun getAll(keys: Set<String>): Map<String, V> {
        if (clearPending.value) return emptyMap()
        val result = frontCache.getAll(keys).toMutableMap()
        val missedKeys = (keys - result.keys).filter { !tombstones.contains(it) }

        if (missedKeys.isNotEmpty()) {
            val pipeline: RedisAsyncCommands<String, V> = connection.async()
            val futures: Map<String, RedisFuture<V>> =
                missedKeys.associateWith { key ->
                    pipeline.get(config.redisKey(key))
                }
            connection.flushCommands()
            futures.forEach { (key, future) ->
                runCatching { future.get() }
                    .onFailure { e -> log.warn(e) { "Redis GET failed for key=$key during getAll" } }
                    .getOrNull()
                    ?.let { value ->
                        result[key] = value
                        frontCache.put(key, value)
                    }
            }
        }
        return result
    }

    /**
     * 키-값 쌍을 저장한다.
     * - front cache 즉시 반영
     * - Redis write는 queue로 큐잉 (write-behind)
     */
    fun put(
        key: String,
        value: V,
    ) {
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
    fun putAll(entries: Map<String, V>) {
        tombstones.removeAll(entries.keys)
        entries.forEach { (k, v) -> frontCache.put(k, v) }
        if (!queue.offer(BackCacheCommand.PutAll(entries))) {
            log.warn { "Write queue full, dropping PutAll for ${entries.size} entries" }
        }
    }

    /**
     * 키-값이 없을 때만 저장한다. Redis setnx를 사용하여 원자적으로 처리한다.
     * @return 이미 존재하는 값이 있으면 기존 값, 새로 저장하면 null
     */
    fun putIfAbsent(
        key: String,
        value: V,
    ): V? {
        val existing = get(key)
        if (existing != null) return existing

        val rKey = config.redisKey(key)
        val setted = syncCommands.setnx(rKey, value)
        return if (setted) {
            config.redisTtl?.let { ttl -> syncCommands.expire(rKey, ttl.seconds) }
            frontCache.put(key, value)
            null
        } else {
            syncCommands.get(rKey)
        }
    }

    /**
     * 기존 값을 새 값으로 교체한다 (키가 있을 때만).
     * @return 교체 성공 여부
     */
    fun replace(
        key: String,
        value: V,
    ): Boolean {
        key.requireNotBlank("key")
        if (tombstones.contains(key) || clearPending.value) return false

        if (!frontCache.containsKey(key)) {
            if (syncCommands.exists(config.redisKey(key)) == 0L) return false
        }
        val ok = syncCommands.set(config.redisKey(key), value, SetArgs.Builder.xx()) != null
        if (ok) {
            frontCache.put(key, value)
        }
        return ok
    }

    /**
     * 기존 값이 [oldValue]와 같을 때만 [newValue]로 교체한다.
     */
    fun replace(
        key: String,
        oldValue: V,
        newValue: V,
    ): Boolean {
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
    fun getAndReplace(
        key: String,
        value: V,
    ): V? {
        val existing = get(key) ?: return null
        put(key, value)
        return existing
    }

    /**
     * 해당 키가 캐시에 존재하는지 확인한다 (front or Redis).
     */
    fun containsKey(key: String): Boolean {
        if (tombstones.contains(key) || clearPending.value) return false
        if (frontCache.containsKey(key)) return true
        return syncCommands.exists(config.redisKey(key)) > 0L
    }

    /**
     * 키에 해당하는 캐시 항목을 제거한다.
     * - front cache 즉시 반영
     * - Redis delete는 queue로 큐잉 (write-behind)
     */
    fun remove(key: String) {
        key.requireNotBlank("key")
        frontCache.remove(key)
        tombstones.add(key)
        if (!queue.offer(BackCacheCommand.Remove(key))) {
            tombstones.remove(key)
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
            tombstones.removeAll(keys)
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
     * 로컬 캐시와 Redis를 모두 비운다.
     * SCAN으로 이 cacheName의 key만 삭제한다 (다른 cacheName 데이터 보존).
     */
    fun clearAll() {
        clearLocal()
        clearPending.value = true
        if (!queue.offer(BackCacheCommand.ClearBack())) {
            clearPending.value = false
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
                syncCommands.scan(cursor, ScanArgs.Builder.matches(pattern).limit(NearCache.SCAN_BATCH_SIZE))
            count += result.keys.size
            cursor = result
        } while (!result.isFinished)
        return count
    }

    /**
     * 로컬 캐시(Caffeine) 통계. [LettuceNearCacheConfig.recordStats]가 true일 때만 유효한 값을 반환한다.
     */
    fun localStats(): CacheStats? = frontCache.stats()

    /**
     * 모든 리소스를 정리하고 연결을 닫는다.
     */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            runCatching { consumerThread.interrupt() }
            runCatching { queue.clear() }
            runCatching { trackingListener.close() }
            runCatching { connection.close() }
            runCatching { frontCache.close() }
            log.debug { "ResilientLettuceNearCache [${config.cacheName}] closed" }
        }
    }

    private fun clearBackCache() {
        val pattern = "${config.cacheName}:*"
        var cursor: ScanCursor = ScanCursor.INITIAL
        do {
            val result: KeyScanCursor<String> =
                syncCommands.scan(cursor, ScanArgs.Builder.matches(pattern).limit(NearCache.SCAN_BATCH_SIZE))
            if (result.keys.isNotEmpty()) {
                syncCommands.del(*result.keys.toTypedArray())
            }
            cursor = result
        } while (!result.isFinished)
        log.debug { "Redis back cache cleared for cacheName=${config.cacheName}" }
    }

    private val redisTtlArgs: SetArgs? by lazy {
        config.redisTtl?.let { SetArgs.Builder.ex(it) }
    }

    private fun setRedis(
        key: String,
        value: V,
    ) {
        val rKey = config.redisKey(key)

        if (redisTtlArgs != null) {
            syncCommands.set(rKey, value, redisTtlArgs)
        } else {
            syncCommands.set(rKey, value)
        }
    }

    private val redisTtlExArgs: MSetExArgs? by lazy {
        config.redisTtl?.let { MSetExArgs.Builder.ex(it) }
    }

    private fun msetRedis(map: Map<String, V>) {
        val rMap = map.map { config.redisKey(it.key) to it.value }.toMap()

        if (redisTtlExArgs != null) {
            syncCommands.msetex(rMap, redisTtlExArgs)
        } else {
            syncCommands.mset(rMap)
        }
    }
}
