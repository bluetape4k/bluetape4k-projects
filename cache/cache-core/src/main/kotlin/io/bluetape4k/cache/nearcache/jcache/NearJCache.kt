package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.JCacheEntryEventListener
import io.bluetape4k.cache.jcache.getConfiguration
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeoutException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import javax.cache.Cache
import javax.cache.CacheException
import javax.cache.configuration.CacheEntryListenerConfiguration
import javax.cache.configuration.Configuration
import javax.cache.configuration.MutableCacheEntryListenerConfiguration
import javax.cache.event.CacheEntryEvent
import javax.cache.event.EventType
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
@Suppress("TooManyFunctions")
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
        require(!frontCache.getConfiguration<K, V, Configuration<K, V>>().isStoreByValue) {
            "NearJCache actual front cache must use store-by-reference; " +
                    "the supplied cache configuration is store-by-value"
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
        @Suppress("TooGenericExceptionCaught")
        operator fun <K: Any, V: Any> invoke(
            nearCacheCfg: NearJCacheConfig<K, V>,
            backCache: JCache<K, V>,
        ): NearJCache<K, V> {
            require(!nearCacheCfg.frontCacheConfiguration.isStoreByValue) {
                "NearJCache front cache must use store-by-reference; " +
                        "configure a filtered copier before enabling store-by-value"
            }
            val frontCacheManager = nearCacheCfg.cacheManagerFactory.create()

            // back cache의 event를 수신하여 반영할 front cache 생성
            log.info { "front cache 생성. name=${nearCacheCfg.cacheName}" }
            val frontCache =
                frontCacheManager.createCache(nearCacheCfg.cacheName, nearCacheCfg.frontCacheConfiguration)

            log.info { "Create NearCache instance. config=$nearCacheCfg" }
            return try {
                NearJCache(frontCache, backCache, nearCacheCfg).also {
                    it.registerBackCacheListener()
                }
            } catch (e: Throwable) {
                closeFrontCacheAfterFailure(
                    frontCache = frontCache,
                    primaryFailure = e,
                    operation = "constructor-rollback",
                    cacheName = nearCacheCfg.cacheName,
                )
                throw e
            }
        }

        @Suppress("TooGenericExceptionCaught")
        private fun closeFrontCacheAfterFailure(
            frontCache: JCache<*, *>,
            primaryFailure: Throwable,
            operation: String,
            cacheName: String,
        ) {
            try {
                frontCache.close()
            } catch (cleanupFailure: Throwable) {
                if (cleanupFailure !== primaryFailure) {
                    primaryFailure.addSuppressed(cleanupFailure)
                }
                log.error(cleanupFailure) {
                    "NearJCache front cleanup failed. " +
                            "operation=$operation, cache=$cacheName, provider=${frontCache.javaClass.name}"
                }
            }
        }
    }

    private val lock = ReentrantLock()
    private val mutationGate = ReentrantLock()
    private val compoundGate = ReentrantLock()
    private val backWriteLock = ReentrantLock(true)
    private val inlineSelfEventReconciliation = ThreadLocal<Boolean>()
    private val listenerRegistrationLock = ReentrantLock()
    private val frontCloseCompleted = AtomicBoolean(false)
    private val closeStarted = AtomicBoolean(false)
    private val closeCompleted = AtomicBoolean(false)
    private val mutationEpoch = AtomicLong()
    private val backWriteGeneration = AtomicLong()
    private class BackCacheListenerRegistration<K: Any, V: Any>(
        val configuration: CacheEntryListenerConfiguration<K, V>,
        val active: AtomicReference<Boolean> = AtomicReference(true),
    )
    private class SelfEventMatcher<K: Any, V: Any>(
        val keys: Set<K>,
        private val eventTypes: Set<EventType>,
        private val values: Map<K, V>? = null,
    ) {
        fun matches(eventType: EventType, events: List<CacheEntryEvent<out K, out V>>): Boolean {
            if (events.isEmpty() || eventType !in eventTypes) return false
            return events.all { event ->
                event.key in keys &&
                    (values == null || (values.containsKey(event.key) && values[event.key] == event.value))
            }
        }
    }

    private class ActiveSelfEventContext<K: Any, V: Any>(
        val matcher: SelfEventMatcher<K, V>,
    ) {
        private val remainingKeys = ConcurrentHashMap.newKeySet<K>().apply { addAll(matcher.keys) }

        @Synchronized
        fun matchesAndConsume(eventType: EventType, events: List<CacheEntryEvent<out K, out V>>): Boolean {
            if (!matcher.matches(eventType, events) || events.any { it.key !in remainingKeys }) return false
            events.forEach { remainingKeys.remove(it.key) }
            return true
        }
    }

    private val backCacheListener = AtomicReference<BackCacheListenerRegistration<K, V>?>(null)
    private val activeSelfEventContexts = CopyOnWriteArrayList<ActiveSelfEventContext<K, V>>()
    private data class BackCacheWriteState(
        val operationId: Long,
        val operation: String,
        val completion: CompletableFuture<Unit>,
    )

    private fun BackCacheWriteState.toCompletion(): BackCacheWriteCompletion =
        BackCacheWriteCompletion(
            operationId = operationId,
            operation = operation,
            completion = completion.minimalCompletionStage(),
        )

    private val nextBackCacheWriteOperationId = AtomicLong()
    private val lastBackCacheWriteState = AtomicReference(
        BackCacheWriteState(
            operationId = 0L,
            operation = "initial",
            completion = CompletableFuture.completedFuture(Unit),
        )
    )
    private val backCacheWriteListeners = CopyOnWriteArrayList<(BackCacheWriteCompletion) -> Unit>()

    private object CompoundResultUnset

    /**
     * 마지막으로 예약한 Back Cache write-through의 operation ID, 이름, 완료 상태를
     * 하나의 원자 스냅숏으로 반환합니다.
     *
     * `completion`은 내부 future를 변경할 수 없는 관찰 전용 단계입니다. 여러
     * property를 따로 읽어 operation 상관관계를 맞추지 말고, 상관관계가 필요한
     * 모니터링·재시도·감사 코드에서는 이 스냅숏을 한 번 읽어 사용하세요.
     */
    val lastBackCacheWrite: BackCacheWriteCompletion
        get() = lastBackCacheWriteState.get().toCompletion()

    /**
     * 마지막으로 예약한 Back Cache write-through의 완료 상태입니다.
     *
     * 동기 모드에서는 성공 또는 실패가 즉시 완료된 [CompletableFuture]로 기록되고,
     * 비동기 모드에서는 bounded retry와 timeout을 포함한 Back Cache 작업의 성공·실패가
     * 기록됩니다. 이 property는 기존 호출자 호환성을 위한 단일 값 접근자이며,
     * operation ID와 함께 관찰해야 하면 [lastBackCacheWrite]를 사용하세요. 반환값은
     * 내부 completion의 복사본이므로 호출자가 완료 상태를 변경할 수 없습니다.
     */
    val lastBackCacheWriteCompletion: CompletableFuture<Unit>
        get() = lastBackCacheWriteState.get().completion.copy()

    /**
     * 마지막 write-through 작업의 operation ID입니다.
     *
     * 기존 호환성을 위한 접근자이며 operation 결과와 함께 읽을 때는
     * [lastBackCacheWrite]를 사용하세요.
     */
    val lastBackCacheWriteOperationId: Long
        get() = lastBackCacheWriteState.get().operationId

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

    @Suppress("TooGenericExceptionCaught")
    override fun clear() {
        compoundGate.withLock {
            val hadBackCacheListener = backCacheListener.get() != null
            detachBackCacheListener()
            var primaryFailure: Throwable? = null
            try {
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
            } catch (e: Throwable) {
                primaryFailure = e
            }
            if (hadBackCacheListener) {
                try {
                    registerBackCacheListener()
                } catch (registrationFailure: Throwable) {
                    if (primaryFailure == null) {
                        primaryFailure = registrationFailure
                    } else if (registrationFailure !== primaryFailure) {
                        primaryFailure.addSuppressed(registrationFailure)
                    }
                }
            }
            primaryFailure?.let { throw it }
        }
    }

    /**
     * Back Cache 이벤트를 이 NearJCache의 mutation gate로 연결합니다.
     *
     * 기본 팩토리는 자동으로 호출합니다. 직접 생성자를 사용하는 외부 팩토리는
     * 이벤트 기반 Front 동기화가 필요할 때 한 번 호출해야 하며, 이미 등록된 경우
     * 중복 등록하지 않습니다.
     */
    @Suppress("TooGenericExceptionCaught")
    fun registerBackCacheListener() {
        compoundGate.withLock {
            check(!closeStarted.get()) { "NearJCache listener registration is unavailable after close started" }
            listenerRegistrationLock.withLock {
                if (backCacheListener.get() != null) return

                lateinit var registration: BackCacheListenerRegistration<K, V>
                val listener = JCacheEntryEventListener<K, V>(frontCache) { eventType, events ->
                    applyBackCacheEvents(registration, eventType, events)
                }
                val configuration = MutableCacheEntryListenerConfiguration(
                    { listener },
                    null,
                    false,
                    config.isSynchronous
                )
                registration = BackCacheListenerRegistration(configuration)
                backCacheListener.set(registration)
                try {
                    log.info { "back cache 이벤트 listener 등록. cache=${config.cacheName}" }
                    backCache.registerCacheEntryListener(configuration)
                } catch (e: Throwable) {
                    backCacheListener.compareAndSet(registration, null)
                    registration.active.set(false)
                    log.error(e) {
                        "NearJCache back cache listener registration failed. " +
                                "operation=register, cache=${config.cacheName}, " +
                                "provider=${backCache.javaClass.name}"
                    }
                    throw e
                }
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

    /**
     * 이 wrapper가 등록한 Back listener와 소유한 Front cache만 정리합니다.
     * 전달받은 Back cache/provider는 닫지 않습니다.
     *
     * listener 또는 Front 정리 실패는 호출자에게 전달하며, 여러 정리 단계가 실패하면
     * 첫 실패를 주 예외로 유지하고 이후 실패를 suppressed 예외로 연결합니다. 성공한
     * close는 idempotent하며, listener 또는 Front 정리 실패 후에는 다음 호출에서
     * 해당 정리를 재시도합니다. close가 시작된 뒤에는 listener를 다시 등록할 수 없습니다.
     */
    @Suppress("TooGenericExceptionCaught")
    override fun close() {
        compoundGate.withLock {
            if (closeCompleted.get()) return
            closeStarted.set(true)

            var primaryFailure: Throwable? = null
            try {
                detachBackCacheListener()
            } catch (listenerFailure: Throwable) {
                primaryFailure = listenerFailure
                log.error(listenerFailure) {
                    "NearJCache cleanup failed. operation=close-listener, " +
                            "cache=${config.cacheName}, provider=${backCache.javaClass.name}"
                }
            }

            if (!frontCloseCompleted.get()) {
                try {
                    lock.withLock {
                        log.debug { "Near Cache 의 Front Cache를 Close 합니다." }
                        frontCache.close()
                    }
                    frontCloseCompleted.set(true)
                } catch (frontFailure: Throwable) {
                    if (primaryFailure == null) {
                        primaryFailure = frontFailure
                    } else if (frontFailure !== primaryFailure) {
                        primaryFailure.addSuppressed(frontFailure)
                    }
                    log.error(frontFailure) {
                        "NearJCache front cleanup failed. operation=close, " +
                                "cache=${config.cacheName}, provider=${frontCache.javaClass.name}"
                    }
                }
            }

            primaryFailure?.let { throw it }
            closeCompleted.set(true)
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
    @Suppress("ReturnCount", "TooGenericExceptionCaught")
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
                } catch (e: CancellationException) {
                    throw e
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

    @Suppress("TooGenericExceptionCaught")
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
                    } catch (e: CancellationException) {
                        throw e
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

    /**
     * Back Cache의 원자 compound 연산 결과를 기준으로 Front Cache를 동기화합니다.
     *
     * JCache compound 연산은 호출자가 이전 값을 즉시 받아야 하므로 설정의
     * 비동기 write-through 여부와 무관하게 Back Cache 원자 연산을 완료한 뒤
     * Front Cache를 갱신합니다. Back provider가 호출 중 동기 listener를
     * 실행할 수 있으므로 Back 호출 중에는 mutation gate를 잡지 않고,
     * 동일 wrapper의 compound 순서만 별도 gate로 직렬화합니다. Back provider가
     * 제공하는 `getAnd*` 경계를 우회하는 front read/put 조합은 사용하지 않습니다.
     */
    override fun getAndPut(key: K, value: V): V? = compoundGate.withLock {
        mutationGate.withLock { mutationEpoch.incrementAndGet() }
        val oldValue = runCompoundBackCacheOperation("getAndPut") {
            backCache.getAndPut(key, value)
        }
        mutationGate.withLock { frontCache.put(key, value) }
        oldValue
    }

    override fun getAndRemove(key: K): V? = compoundGate.withLock {
        mutationGate.withLock { mutationEpoch.incrementAndGet() }
        val oldValue = runCompoundBackCacheOperation("getAndRemove") {
            backCache.getAndRemove(key)
        }
        mutationGate.withLock { frontCache.remove(key) }
        oldValue
    }

    override fun getAndReplace(key: K, value: V): V? = compoundGate.withLock {
        log.trace { "get and replace. key=$key" }
        mutationGate.withLock { mutationEpoch.incrementAndGet() }
        val oldValue = runCompoundBackCacheOperation("getAndReplace") {
            backCache.getAndReplace(key, value)
        }
        mutationGate.withLock {
            if (oldValue == null) {
                frontCache.remove(key)
            } else {
                frontCache.put(key, value)
            }
        }
        oldValue
    }

    operator fun set(key: K, value: V) {
        put(key, value)
    }

    override fun put(key: K, value: V) {
        mutationGate.withLock {
            mutationEpoch.incrementAndGet()
            frontCache.put(key, value)
            syncBackCache(
                operation = "put",
                expectedBackWriteGeneration = backWriteGeneration.get(),
                selfEventMatcher = SelfEventMatcher(
                    keys = setOf(key),
                    eventTypes = setOf(EventType.CREATED, EventType.UPDATED),
                    values = mapOf(key to value),
                ),
            ) { backCache.put(key, value) }
        }
    }

    override fun putAll(map: Map<out K, V>) {
        mutationGate.withLock {
            mutationEpoch.incrementAndGet()
            frontCache.putAll(map)
            syncBackCache(
                operation = "putAll",
                expectedBackWriteGeneration = backWriteGeneration.get(),
                selfEventMatcher = SelfEventMatcher(
                    keys = map.keys,
                    eventTypes = setOf(EventType.CREATED, EventType.UPDATED),
                    values = map.entries.associate { it.key to it.value },
                ),
            ) { backCache.putAll(map) }
        }
    }

    override fun putIfAbsent(key: K, value: V): Boolean = mutationGate.withLock {
        frontCache.putIfAbsent(key, value).also { inserted ->
            if (inserted) {
                mutationEpoch.incrementAndGet()
                syncBackCache(
                    operation = "putIfAbsent",
                    expectedBackWriteGeneration = backWriteGeneration.get(),
                    selfEventMatcher = SelfEventMatcher(
                        keys = setOf(key),
                        eventTypes = setOf(EventType.CREATED),
                        values = mapOf(key to value),
                    ),
                ) {
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
                syncBackCache(
                    operation = "remove",
                    expectedBackWriteGeneration = backWriteGeneration.get(),
                    selfEventMatcher = SelfEventMatcher(
                        keys = setOf(key),
                        eventTypes = setOf(EventType.REMOVED),
                    ),
                ) { backCache.remove(key) }
            }
        }
    }

    override fun remove(key: K, oldValue: V): Boolean = mutationGate.withLock {
        frontCache.remove(key, oldValue).also { removed ->
            if (removed) {
                mutationEpoch.incrementAndGet()
                syncBackCache(
                    operation = "remove(key, oldValue)",
                    expectedBackWriteGeneration = backWriteGeneration.get(),
                    selfEventMatcher = SelfEventMatcher(
                        keys = setOf(key),
                        eventTypes = setOf(EventType.REMOVED),
                    ),
                ) {
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
            syncBackCache(
                operation = "removeAll(keys)",
                expectedBackWriteGeneration = backWriteGeneration.get(),
                selfEventMatcher = SelfEventMatcher(
                    keys = keys,
                    eventTypes = setOf(EventType.REMOVED),
                ),
            ) {
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
                    operation = "replace(key, oldValue, newValue)",
                    expectedBackWriteGeneration = backWriteGeneration.get(),
                    selfEventMatcher = SelfEventMatcher(
                        keys = setOf(key),
                        eventTypes = setOf(EventType.UPDATED),
                        values = mapOf(key to newValue),
                    ),
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
                syncBackCache(
                    operation = "replace",
                    expectedBackWriteGeneration = backWriteGeneration.get(),
                    selfEventMatcher = SelfEventMatcher(
                        keys = setOf(key),
                        eventTypes = setOf(EventType.UPDATED),
                        values = mapOf(key to value),
                    ),
                ) {
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
        selfEventMatcher: SelfEventMatcher<K, V>? = null,
        syncTask: () -> Unit,
    ): CompletableFuture<Unit> {
        val completion = CompletableFuture<Unit>()
        publishBackCacheWrite(operation, completion)
        val reconcileInlineSelfEvent = synchronous && mutationGate.isHeldByCurrentThread()
        val guardedSyncTask = {
            runGuardedBackCacheWrite(
                expectedBackWriteGeneration = expectedBackWriteGeneration,
                selfEventMatcher = selfEventMatcher,
                reconcileInlineSelfEvent = reconcileInlineSelfEvent,
                syncTask = syncTask,
            )
        }
        if (synchronous) {
            val timeoutMillis = config.syncRemoteTimeout.coerceAtLeast(NearJCacheConfig.DEFAULT_SYNC_REMOTE_TIMEOUT)
            return try {
                runSynchronousBackCacheWrite(timeoutMillis, guardedSyncTask)
                completion.complete(Unit)
                completion
            } catch (e: Throwable) {
                val failure = unwrapCompletionFailure(e)
                val callerFailure = if (failure is TimeoutException) {
                    CacheException(
                        "NearJCache synchronous back cache write timed out after ${timeoutMillis}ms. " +
                                "operation=$operation, cache=${config.cacheName}",
                        failure
                    )
                } else {
                    failure
                }
                completion.completeExceptionally(callerFailure)
                log.error(callerFailure) {
                    "NearJCache synchronous back cache write failed. operation=$operation, " +
                            "cache=${config.cacheName}, provider=${backCache.javaClass.name}"
                }
                throw callerFailure
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

    private fun runGuardedBackCacheWrite(
        expectedBackWriteGeneration: Long,
        selfEventMatcher: SelfEventMatcher<K, V>?,
        reconcileInlineSelfEvent: Boolean,
        syncTask: () -> Unit,
    ) {
        backWriteLock.withLock {
            if (expectedBackWriteGeneration != backWriteGeneration.get()) return@withLock

            // 호출자가 mutationGate를 잡은 동기 mutation만 operation context를 사용합니다.
            // 비동기 write는 provider callback 전에 gate를 해제하므로 기존 순서 경로를 유지합니다.
            val context = selfEventMatcher
                ?.takeIf { reconcileInlineSelfEvent }
                ?.let { ActiveSelfEventContext(it) }
            context?.let(activeSelfEventContexts::add)
            try {
                if (reconcileInlineSelfEvent) {
                    withInlineSelfEventReconciliation(syncTask)
                } else {
                    syncTask()
                }
            } finally {
                context?.let(activeSelfEventContexts::remove)
            }
        }
    }

    /**
     * 동기 write-through도 호출자 스레드를 원격 provider의 blocking wait에 묶지 않습니다.
     * timeout 시 작업을 interrupt하고 executor를 즉시 종료해 테스트/애플리케이션 lifecycle에
     * 남는 전용 실행기를 최소화합니다. Provider가 interrupt를 무시하면 실제 backend 작업은
     * 늦게 완료될 수 있으므로 [backWriteLock]이 완료 순서를 계속 직렬화합니다.
     *
     * 동기 mutation이 호출자 [mutationGate]를 잡은 동안 provider가 같은 write의 listener를
     * inline 또는 synchronous callback thread에서 호출하면, key/type/value가 일치하는
     * self-event는 write worker 또는 active operation context를 통해 Front에 직접 반영합니다.
     * 매칭되지 않는 다른 wrapper나 외부 write의 listener event와 비동기 write는 기존처럼
     * [mutationGate]를 통해 직렬화됩니다. JCache event에는 operation ID가 없으므로
     * 동일 key/type/value의 외부 event는 self-event와 구분할 수 없습니다.
     */
    @Suppress("ThrowsCount")
    private fun runSynchronousBackCacheWrite(timeoutMillis: Long, syncTask: () -> Unit) {
        val executor = Executors.newVirtualThreadPerTaskExecutor()
        val future = executor.submit { syncTask() }
        try {
            future.get(timeoutMillis, TimeUnit.MILLISECONDS)
        } catch (e: ExecutionException) {
            throw unwrapCompletionFailure(e)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw CancellationException("Synchronous back cache write was interrupted").also { it.initCause(e) }
        } catch (e: TimeoutException) {
            future.cancel(true)
            throw e
        } finally {
            executor.shutdownNow()
        }
    }

    private fun <R> runCompoundBackCacheOperation(operation: String, operationBlock: () -> R): R {
        val result = AtomicReference<Any?>(CompoundResultUnset)
        syncBackCache(
            operation = operation,
            synchronous = true,
            expectedBackWriteGeneration = backWriteGeneration.get(),
        ) {
            result.set(operationBlock())
        }
        val value = result.get()
        check(value !== CompoundResultUnset) {
            "NearJCache compound operation completed without a result. operation=$operation"
        }
        @Suppress("UNCHECKED_CAST")
        return value as R
    }

    private inline fun <R> withInlineSelfEventReconciliation(block: () -> R): R {
        val previous = inlineSelfEventReconciliation.get()
        inlineSelfEventReconciliation.set(true)
        return try {
            block()
        } finally {
            if (previous == null) {
                inlineSelfEventReconciliation.remove()
            } else {
                inlineSelfEventReconciliation.set(previous)
            }
        }
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
        lastBackCacheWriteState.set(state)
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

    private fun detachBackCacheListener() {
        listenerRegistrationLock.withLock {
            val registration = backCacheListener.get() ?: return
            registration.active.set(false)
            backCache.deregisterCacheEntryListener(registration.configuration)
            backCacheListener.compareAndSet(registration, null)
        }
    }

    private fun applyBackCacheEvents(
        registration: BackCacheListenerRegistration<K, V>,
        eventType: EventType,
        events: List<CacheEntryEvent<out K, out V>>,
    ) {
        if (!registration.active.get()) return
        val activeSelfEvent = activeSelfEventContexts.any { context ->
            context.matchesAndConsume(eventType, events)
        }
        if (inlineSelfEventReconciliation.get() == true || activeSelfEvent) {
            applyBackCacheEventsToFront(registration, eventType, events)
        } else {
            mutationGate.withLock {
                applyBackCacheEventsToFront(registration, eventType, events)
            }
        }
    }

    private fun applyBackCacheEventsToFront(
        registration: BackCacheListenerRegistration<K, V>,
        eventType: EventType,
        events: List<CacheEntryEvent<out K, out V>>,
    ) {
        if (!registration.active.get() || backCacheListener.get() !== registration) return
        mutationEpoch.incrementAndGet()
        when (eventType) {
            EventType.CREATED, EventType.UPDATED ->
                frontCache.putAll(events.associate { it.key to it.value })
            EventType.REMOVED, EventType.EXPIRED ->
                frontCache.removeAll(events.map { it.key }.toSet())
        }
    }
}
