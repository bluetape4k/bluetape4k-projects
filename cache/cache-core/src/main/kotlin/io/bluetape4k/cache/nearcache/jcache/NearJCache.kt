package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.JCacheEntryEventListener
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.logging.info
import io.bluetape4k.logging.trace
import io.bluetape4k.logging.warn
import io.bluetape4k.support.asyncRunWithTimeout
import java.time.Duration
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.CompletionStage
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import javax.cache.Cache
import javax.cache.configuration.MutableCacheEntryListenerConfiguration
import kotlin.concurrent.withLock

/**
 * Back Cache write-through 작업의 operation별 완료 결과입니다.
 *
 * [completion]은 관찰 전용 [CompletionStage]이며, 동일 [NearJCache] 인스턴스의
 * 다른 mutation 결과와 섞이지 않도록 [operationId]로 상관관계를 유지합니다.
 */
data class BackCacheWriteCompletion(
    val operationId: Long,
    val operation: String,
    val completion: CompletionStage<Unit>,
)

/**
 * 분산 환경에서 로컬 캐시(Front Cache)와 원격 캐시(Back Cache)를 함께 사용하는 2-Tier 캐시 구현체입니다.
 *
 * NearCache는 다음과 같은 특징을 가집니다:
 * - **빠른 읽기**: 로컬 캐시(Front)에서 먼저 조회하여 네트워크 비용을 절감
 * - **데이터 일관성**: Back Cache의 변경 이벤트를 수신하여 Front Cache를 동기화
 * - **유연한 동기화**: 동기/비동기 모드 지원
 * - **자동 만료 감지**: 백그라운드 스레드로 Back Cache 만료 감지 및 Front Cache 갱신
 *
 * ```kotlin
 * // Redis를 Back Cache로 사용하는 NearCache 생성
 * val nearCache = NearCache(
 *     nearCacheCfg = NearCacheConfig(
 *         frontCacheName = "my-local-cache",
 *         isSynchronous = false  // 비동기 모드
 *     ),
 *     backCache = redisCache
 * )
 *
 * // 사용
 * nearCache.put("key", value)      // Front와 Back에 동시 저장
 * val value = nearCache.get("key") // Front에서 먼저 조회
 * ```
 *
 * @param K 캐시 키 타입
 * @param V 캐시 값 타입
 * @property frontCache 로컬 캐시 (예: Caffeine, Ehcache)
 * @property backCache 원격 캐시 (예: Redis, Hazelcast)
 * @property config NearCache 설정
 *
 * @see NearJCacheConfig
 * @see io.bluetape4k.cache.jcache.JCacheEntryEventListener
 */
class NearJCache<K: Any, V: Any>(
    val frontCache: JCache<K, V>,
    val backCache: JCache<K, V>,
    private val config: NearJCacheConfig<K, V>,
): JCache<K, V> by backCache {
    init {
        require(!config.frontCacheConfiguration.isStoreByValue) {
            "NearJCache front cache must use store-by-reference; " +
                    "configure a filtered copier before enabling store-by-value"
        }
    }

    companion object: KLogging() {
        /** Redis SCAN 명령의 배치 크기 */
        const val SCAN_BATCH_SIZE = 100L

        /** 기본 원격 캐시 동기화 타임아웃 (500ms) */
        val DEFAULT_SYNC_REMOTE_TIMEOUT: Duration = Duration.ofMillis(500)

        /** Back Cache bulk remove 작업의 배치 크기 */
        private const val REMOVE_BATCH_SIZE = 100

        /**
         * NearCache 인스턴스를 생성합니다.
         *
         * @param K 캐시 키 타입
         * @param V 캐시 값 타입
         * @param nearCacheCfg Front Cache 생성을 위한 설정
         * @param backCache 분산 환경에서 사용할 원격 캐시 인스턴스
         * @return [NearJCache] 인스턴스
         */
        operator fun <K: Any, V: Any> invoke(
            nearCacheCfg: NearJCacheConfig<K, V>,
            backCache: JCache<K, V>,
        ): NearJCache<K, V> {
            val frontCacheManager = nearCacheCfg.cacheManagerFactory.create()

            // back cache의 event를 수신하여 반영할 front cache 생성
            log.info { "front cache 생성. name=${nearCacheCfg.cacheName}" }
            val frontCache =
                frontCacheManager.createCache(nearCacheCfg.cacheName, nearCacheCfg.frontCacheConfiguration)

            // back cache의 event를 받아 front cache에 반영합니다.
            val jCacheEntryEventListenerCfg =
                MutableCacheEntryListenerConfiguration(
                    { JCacheEntryEventListener(frontCache) },
                    null,
                    false,
                    nearCacheCfg.isSynchronous
                )
            log.info { "back cache의 이벤트를 수신할 수 있도록 listener 등록. listenerCfg=$jCacheEntryEventListenerCfg" }
            backCache.registerCacheEntryListener(jCacheEntryEventListenerCfg)

            log.info { "Create NearCache instance. config=$nearCacheCfg" }
            return NearJCache(frontCache, backCache, nearCacheCfg)
        }
    }

    private val lock = ReentrantLock()
    private val mutationGate = ReentrantLock()
    private val backWriteLock = ReentrantLock(true)
    private val mutationEpoch = AtomicLong()
    private val backWriteGeneration = AtomicLong()
    private data class BackCacheWriteState(
        val operationId: Long,
        val operation: String,
        val completion: CompletableFuture<Unit>,
    )

    private val nextBackCacheWriteOperationId = AtomicLong()
    private val lastBackCacheWrite = AtomicReference(
        BackCacheWriteState(
            operationId = 0L,
            operation = "initial",
            completion = CompletableFuture.completedFuture(Unit),
        )
    )
    private val backCacheWriteListeners = CopyOnWriteArrayList<(BackCacheWriteCompletion) -> Unit>()

    /**
     * 마지막으로 예약한 Back Cache write-through의 완료 상태입니다.
     *
     * 동기 모드에서는 성공 또는 실패가 즉시 완료된 [CompletableFuture]로 기록되고,
     * 비동기 모드에서는 bounded retry와 timeout을 포함한 Back Cache 작업의 성공·실패가
     * 기록됩니다. 반환값은 내부 completion의 복사본이므로 호출자가 완료 상태를 변경할 수
     * 없습니다.
     */
    val lastBackCacheWriteCompletion: CompletableFuture<Unit>
        get() = lastBackCacheWrite.get().completion.copy()

    /** 마지막 write-through 작업의 operation ID입니다. */
    val lastBackCacheWriteOperationId: Long
        get() = lastBackCacheWrite.get().operationId

    /**
     * 모든 write-through 작업의 operation별 completion을 관찰할 listener를 등록합니다.
     *
     * timeout은 terminal failure로 기록하며, timeout 시 이미 실행 중인 backend 작업은
     * 취소되지 않을 수 있고 late completion은 재시도하지 않습니다. 즉, 중복 write를
     * 피하면서 호출자가 timeout과 late write 가능성을 모두 관찰할 수 있습니다.
     */
    fun addBackCacheWriteListener(listener: (BackCacheWriteCompletion) -> Unit): AutoCloseable {
        backCacheWriteListeners += listener
        return object: AutoCloseable {
            override fun close() {
                backCacheWriteListeners.remove(listener)
            }
        }
    }

    override fun iterator(): MutableIterator<Cache.Entry<K, V>> = frontCache.iterator()

    override fun clear() {
        mutationGate.withLock {
            mutationEpoch.incrementAndGet()
            val expectedBackWriteGeneration = backWriteGeneration.incrementAndGet()
            log.debug { "Near Cache의 Front와 Back cache를 Clear합니다." }
            frontCache.clear()
            syncBackCache(
                operation = "clear",
                synchronous = true,
                expectedBackWriteGeneration = expectedBackWriteGeneration,
            ) {
                backCache.clear()
            }
        }
    }

    /**
     * Front Cache와 Back Cache 모두 비웁니다.
     *
     * 단, Back Cache를 공유한 다른 NearCache 인스턴스에는 전파되지 않습니다.
     * 전파가 필요한 경우 `removeAll()`을 사용하세요.
     *
     * ```kotlin
     * val nearCache = NearJCache(frontCache, backCache, config)
     * nearCache.put("hello", 5)
     * nearCache.clearAllCache()
     * val value = nearCache.getDeeply("hello")
     * // value == null
     * ```
     */
    fun clearAllCache() {
        log.debug {
            "front cache, back cache 모두 clear 합니다. 단 back cache 를 공유한 다른 near cache에는 전파되지 않습니다. " +
                    "전파를 위해서는 removeAll을 사용하세요"
        }
        clear()
    }

    override fun close() {
        lock.withLock {
            log.debug { "Near Cache 의 Front Cache를 Close 합니다." }
            runCatching {
                frontCache.close()
            }
        }
    }

    override fun isClosed(): Boolean = frontCache.isClosed

    /**
     * 논리적 2-tier 캐시에서 키의 존재 여부를 확인합니다.
     *
     * 값 자체를 읽거나 Front Cache를 채우지 않고 Front, Back 순서로 확인합니다.
     */
    override fun containsKey(key: K): Boolean {
        if (mutationGate.withLock { frontCache.containsKey(key) }) {
            return true
        }
        return backCache.containsKey(key)
    }

    /**
     * Front Cache를 먼저 조회하고 miss이면 Back Cache에서 읽어 Front Cache에 채웁니다.
     *
     * Back 조회와 Front mutation이 겹쳐 epoch가 변경되면 오래된 값을 Front에 채우지
     * 않습니다. Front populate 실패 중 [RuntimeException]은 Back에서 확보한 값을
     * 호출자에게 반환하고, [Error]와 Back 조회 예외는 숨기지 않습니다.
     */
    override operator fun get(key: K): V? {
        val (frontValue, observedEpoch) = mutationGate.withLock {
            frontCache.get(key) to mutationEpoch.get()
        }
        if (frontValue != null) {
            return frontValue
        }

        val backValue = backCache.get(key) ?: return null
        mutationGate.withLock {
            if (mutationEpoch.get() == observedEpoch) {
                try {
                    frontCache.put(key, backValue)
                } catch (e: RuntimeException) {
                    log.warn(e) {
                        "NearJCache front populate failed. operation=get, " +
                                "cache=${config.cacheName}, provider=${frontCache.javaClass.name}"
                    }
                }
            }
        }
        return backValue
    }

    /**
     * 표준 [Cache.get]과 동일한 논리적 2-tier read-through를 수행합니다.
     *
     * ```kotlin
     * val nearCache = NearJCache(frontCache, backCache, config)
     * nearCache.put("hello", 5)
     * nearCache.clear()
     * val value = nearCache.getDeeply("hello")
     * // value == null
     * ```
     *
     * @param key 조회할 캐시 키
     * @return 조회된 값, 없으면 `null`
     */
    fun getDeeply(key: K): V? = get(key)

    fun getAll(vararg keys: K): MutableMap<K, V> = getAll(keys.toSet())

    override fun getAll(keys: Set<K>): MutableMap<K, V> {
        val (frontValues, missingKeys, observedEpoch) = mutationGate.withLock {
            val values = frontCache.getAll(keys)
            val missing = keys.filterNot(values::containsKey).toSet()
            Triple(values, missing, mutationEpoch.get())
        }
        if (missingKeys.isEmpty()) {
            return frontValues
        }

        val backValues = backCache.getAll(missingKeys)
        if (backValues.isNotEmpty()) {
            mutationGate.withLock {
                if (mutationEpoch.get() == observedEpoch) {
                    try {
                        frontCache.putAll(backValues)
                    } catch (e: RuntimeException) {
                        log.warn(e) {
                            "NearJCache front populate failed. operation=getAll, " +
                                    "cache=${config.cacheName}, provider=${frontCache.javaClass.name}"
                        }
                    }
                }
            }
        }

        return frontValues.toMutableMap().apply { putAll(backValues) }
    }

    override fun getAndRemove(key: K): V? {
        if (frontContainsKey(key)) {
            val oldValue = frontGet(key)
            remove(key)
            return oldValue
        }
        return null
    }

    override fun getAndReplace(key: K, value: V): V? {
        log.trace { "get and replace. key=$key" }
        if (frontContainsKey(key)) {
            log.trace { "get entry, and put new value. key=$key, new value=$value" }
            val oldValue = frontGet(key)
            put(key, value)
            return oldValue
        }
        return null
    }

    operator fun set(key: K, value: V) {
        put(key, value)
    }

    override fun put(key: K, value: V) {
        mutationGate.withLock {
            mutationEpoch.incrementAndGet()
            frontCache.put(key, value)
            syncBackCache("put", expectedBackWriteGeneration = backWriteGeneration.get()) {
                backCache.put(key, value)
            }
        }
    }

    override fun putAll(map: Map<out K, V>) {
        mutationGate.withLock {
            mutationEpoch.incrementAndGet()
            frontCache.putAll(map)
            syncBackCache("putAll", expectedBackWriteGeneration = backWriteGeneration.get()) {
                backCache.putAll(map)
            }
        }
    }

    override fun putIfAbsent(key: K, value: V): Boolean = mutationGate.withLock {
        frontCache.putIfAbsent(key, value).also { inserted ->
            if (inserted) {
                mutationEpoch.incrementAndGet()
                syncBackCache("putIfAbsent", expectedBackWriteGeneration = backWriteGeneration.get()) {
                    if (!backCache.containsKey(key)) {
                        backCache.put(key, value)
                    }
                }
            }
        }
    }

    override fun remove(key: K): Boolean = mutationGate.withLock {
        frontCache.remove(key).also { removed ->
            if (removed) {
                mutationEpoch.incrementAndGet()
                syncBackCache("remove", expectedBackWriteGeneration = backWriteGeneration.get()) {
                    backCache.remove(key)
                }
            }
        }
    }

    override fun remove(key: K, oldValue: V): Boolean = mutationGate.withLock {
        frontCache.remove(key, oldValue).also { removed ->
            if (removed) {
                mutationEpoch.incrementAndGet()
                syncBackCache("remove(key, oldValue)", expectedBackWriteGeneration = backWriteGeneration.get()) {
                    // Back cache listener 전파를 remove(key) 경로로 통일하기 위해 값 비교 후 단일 키 삭제를 사용한다.
                    if (backCache.containsKey(key) && backCache.get(key) == oldValue) {
                        backCache.remove(key)
                    }
                }
            }
        }
    }

    override fun removeAll() {
        mutationGate.withLock {
            mutationEpoch.incrementAndGet()
            frontCache.removeAll()
            syncBackCache("removeAll", expectedBackWriteGeneration = backWriteGeneration.get()) {
                // Redisson 에서는 bulk operation 의 경우 event 가 발생하지 않습니다!!!
                val failures = backCache
                    .chunked(REMOVE_BATCH_SIZE)
                    .flatMap { chunk -> removeBackCacheEntries(chunk.map { it.key }) }
                throwIfFailures("removeAll", failures)
            }
        }
    }

    override fun removeAll(keys: Set<K>) {
        mutationGate.withLock {
            mutationEpoch.incrementAndGet()
            frontCache.removeAll(keys)
            syncBackCache("removeAll(keys)", expectedBackWriteGeneration = backWriteGeneration.get()) {
                // Redisson 에서는 bulk operation 의 경우 event 가 발생하지 않습니다!!!
                val failures = removeBackCacheEntries(keys)
                throwIfFailures("removeAll(keys)", failures)
            }
        }
    }

    /**
     * 여러 키를 vararg 형식으로 일괄 삭제합니다.
     *
     * ```kotlin
     * val nearCache = NearJCache(frontCache, backCache, config)
     * nearCache.put("key1", 1)
     * nearCache.put("key2", 2)
     * nearCache.removeAll("key1", "key2")
     * val v1 = nearCache.getDeeply("key1")
     * // v1 == null
     * ```
     */
    fun removeAll(vararg keys: K) {
        removeAll(keys.toSet())
    }

    override fun replace(key: K, oldValue: V, newValue: V): Boolean = mutationGate.withLock {
        frontCache.replace(key, oldValue, newValue).also { replaced ->
            if (replaced) {
                mutationEpoch.incrementAndGet()
                syncBackCache(
                    "replace(key, oldValue, newValue)",
                    expectedBackWriteGeneration = backWriteGeneration.get()
                ) {
                    if (backCache.containsKey(key) && backCache.get(key) == oldValue) {
                        backCache.put(key, newValue)
                    }
                }
            }
        }
    }

    override fun replace(key: K, value: V): Boolean = mutationGate.withLock {
        frontCache.replace(key, value).also { replaced ->
            if (replaced) {
                mutationEpoch.incrementAndGet()
                syncBackCache("replace", expectedBackWriteGeneration = backWriteGeneration.get()) {
                    // Redisson 에서는 replace 가 event 를 발생시키지 않습니다.
                    if (backCache.containsKey(key)) {
                        backCache.put(key, value)
                    }
                }
            }
        }
    }

    override fun <T: Any> unwrap(clazz: Class<T>): T? {
        if (clazz.isAssignableFrom(javaClass)) {
            return clazz.cast(this)
        }
        return null
    }

    @Suppress("TooGenericExceptionCaught")
    private fun removeBackCacheEntries(keys: Iterable<K>): List<RuntimeException> {
        val failures = mutableListOf<RuntimeException>()
        keys.forEach { key ->
            try {
                backCache.remove(key)
            } catch (e: RuntimeException) {
                failures += e
            }
        }
        return failures
    }

    private fun throwIfFailures(operation: String, failures: List<RuntimeException>) {
        if (failures.isEmpty()) return

        val primary = failures.first()
        failures.drop(1).forEach { failure ->
            val suppressed =
                if (failure === primary) {
                    IllegalStateException("Repeated back cache failure", failure)
                } else {
                    failure
                }
            primary.addSuppressed(suppressed)
        }
        log.error(primary) {
            "NearJCache back cache bulk write failed. operation=$operation, failureCount=${failures.size}"
        }
        throw primary
    }

    @Suppress("TooGenericExceptionCaught")
    private fun syncBackCache(
        operation: String,
        synchronous: Boolean = config.isSynchronous,
        expectedBackWriteGeneration: Long = backWriteGeneration.get(),
        syncTask: () -> Unit,
    ): CompletableFuture<Unit> {
        val completion = CompletableFuture<Unit>()
        publishBackCacheWrite(operation, completion)
        val guardedSyncTask = {
            backWriteLock.withLock {
                if (expectedBackWriteGeneration == backWriteGeneration.get()) {
                    syncTask()
                }
            }
        }
        if (synchronous) {
            return try {
                guardedSyncTask()
                completion.complete(Unit)
                completion
            } catch (e: RuntimeException) {
                completion.completeExceptionally(e)
                throw e
            }
        }

        val timeoutMillis = config.syncRemoteTimeout.coerceAtLeast(NearJCacheConfig.DEFAULT_SYNC_REMOTE_TIMEOUT)
        runAsyncBackCacheWrite(
            operation = operation,
            timeoutMillis = timeoutMillis,
            retriesRemaining = config.syncRemoteRetryCount.coerceIn(
                0,
                NearJCacheConfig.MAX_SYNC_REMOTE_RETRY_COUNT
            ),
            completion = completion,
            syncTask = guardedSyncTask,
        )
        return completion
    }

    private fun frontContainsKey(key: K): Boolean = mutationGate.withLock {
        frontCache.containsKey(key)
    }

    private fun frontGet(key: K): V? = mutationGate.withLock {
        frontCache.get(key)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun publishBackCacheWrite(
        operation: String,
        completion: CompletableFuture<Unit>,
    ) {
        val state = BackCacheWriteState(
            operationId = nextBackCacheWriteOperationId.incrementAndGet(),
            operation = operation,
            completion = completion,
        )
        lastBackCacheWrite.set(state)
        completion.whenComplete { _, _ ->
            val result = BackCacheWriteCompletion(
                operationId = state.operationId,
                operation = state.operation,
                completion = completion.minimalCompletionStage(),
            )
            backCacheWriteListeners.forEach { listener ->
                try {
                    listener(result)
                } catch (e: RuntimeException) {
                    log.error(e) {
                        "NearJCache back cache write listener failed. operationId=${state.operationId}"
                    }
                }
            }
        }
    }

    private fun runAsyncBackCacheWrite(
        operation: String,
        timeoutMillis: Long,
        retriesRemaining: Int,
        completion: CompletableFuture<Unit>,
        syncTask: () -> Unit,
    ) {
        asyncRunWithTimeout(timeoutMillis) {
            syncTask()
        }.whenComplete { _, error ->
            if (error == null) {
                completion.complete(Unit)
                return@whenComplete
            }

            val failure = unwrapCompletionFailure(error)
            val retryable = failure is RuntimeException &&
                    failure !is TimeoutException &&
                    failure !is CancellationException
            if (retryable && retriesRemaining > 0) {
                log.warn(failure) {
                    "NearJCache retrying asynchronous back cache write. " +
                            "operation=$operation, retriesRemaining=${retriesRemaining - 1}"
                }
                runAsyncBackCacheWrite(
                    operation = operation,
                    timeoutMillis = timeoutMillis,
                    retriesRemaining = retriesRemaining - 1,
                    completion = completion,
                    syncTask = syncTask,
                )
            } else {
                log.error(failure) {
                    "NearJCache asynchronous back cache write failed. operation=$operation"
                }
                completion.completeExceptionally(failure)
            }
        }
    }

    private fun unwrapCompletionFailure(error: Throwable): Throwable =
        when (error) {
            is CompletionException, is ExecutionException -> error.cause ?: error
            else -> error
        }
}
