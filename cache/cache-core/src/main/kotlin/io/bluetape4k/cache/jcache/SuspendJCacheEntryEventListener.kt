package io.bluetape4k.cache.jcache

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.logging.trace
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import java.util.concurrent.atomic.AtomicBoolean
import javax.cache.event.CacheEntryCreatedListener
import javax.cache.event.CacheEntryEvent
import javax.cache.event.CacheEntryExpiredListener
import javax.cache.event.CacheEntryRemovedListener
import javax.cache.event.CacheEntryUpdatedListener

private const val DEFAULT_MAX_IN_FLIGHT_CALLBACKS = 64

/**
 * Back Cache의 엔트리 이벤트(생성/수정/삭제/만료)를 수신하여 Front Cache([targetCache])에 반영하는 리스너입니다.
 *
 * JCache 이벤트 콜백은 동기식으로 호출되므로, `runBlocking` 대신 전용 [kotlinx.coroutines.CoroutineScope]에서
 * `launch`를 사용하여 스레드 풀 고갈과 데드락을 방지합니다.
 * callback admission은 non-blocking `tryAcquire()`로 선형화하며, 기본적으로
 * listener마다 최대 64개의 in-flight callback job만 허용합니다. 상한에 도달한
 * callback은 이벤트 snapshot을 만들기 전에 내부 queue 없이 즉시 거부하고
 * sanitized debug log를 남깁니다. permit을 확보한 callback만 JCache event의
 * key/value를 immutable snapshot으로 복사합니다.
 * [close]는 취소를 요청하지만 callback 완료를 기다리지 않습니다.
 *
 * ```kotlin
 * val frontCache = CaffeineSuspendJCache<String, Int> { maximumSize(1000) }
 * val backCache = CaffeineSuspendJCache<String, Int> { maximumSize(10000) }
 * val listener = SuspendJCacheEntryEventListener(frontCache)
 * val listenerCfg = MutableCacheEntryListenerConfiguration(
 *     { listener }, null, false, false
 * )
 * backCache.registerCacheEntryListener(listenerCfg)
 * backCache.put("hello", 5)
 * // frontCache에 "hello" -> 5 가 코루틴으로 비동기 동기화됨
 * ```
 *
 * @param K 캐시 키 타입
 * @param V 캐시 값 타입
 * @property targetCache 이벤트를 반영할 Front Cache
 */
class SuspendJCacheEntryEventListener<K: Any, V: Any> private constructor(
    private val targetCache: SuspendJCache<K, V>,
    private val scope: CoroutineScope,
    private val maxInFlightCallbacks: Int,
    @Suppress("UNUSED_PARAMETER") marker: Unit,
): CacheEntryCreatedListener<K, V>,
   CacheEntryUpdatedListener<K, V>,
   CacheEntryRemovedListener<K, V>,
   CacheEntryExpiredListener<K, V>,
   AutoCloseable {

    constructor(targetCache: SuspendJCache<K, V>): this(
        targetCache,
        CoroutineScope(SupervisorJob() + Dispatchers.IO),
        DEFAULT_MAX_IN_FLIGHT_CALLBACKS,
        Unit,
    )

    companion object: KLoggingChannel() {
        @JvmSynthetic
        internal fun <K: Any, V: Any> forTest(
            targetCache: SuspendJCache<K, V>,
            scope: CoroutineScope,
            maxInFlightCallbacks: Int = DEFAULT_MAX_IN_FLIGHT_CALLBACKS,
        ): SuspendJCacheEntryEventListener<K, V> =
            SuspendJCacheEntryEventListener(targetCache, scope, maxInFlightCallbacks, Unit)
    }

    init {
        require(maxInFlightCallbacks > 0) { "maxInFlightCallbacks must be positive" }
    }

    private val closed = AtomicBoolean(false)
    private val cacheIdentifier = targetCache::class.java.name.sanitizeLogIdentifier()
    private val admission = Semaphore(maxInFlightCallbacks)

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            scope.cancel()
        }
    }

    /**
     * Called after one or more entries have been created.
     *
     * @param events The entries just created.
     * @throws CacheEntryListenerException if there is problem executing the listener
     */
    override fun onCreated(events: MutableIterable<CacheEntryEvent<out K, out V>>) {
        submit("put all created cache entries") {
            val eventCopies = events.map { EventCopy(it.key, it.value) }
            log.trace { "BackCache cache entry created. cache=$cacheIdentifier count=${eventCopies.size}" }
            suspend {
                targetCache.putAll(eventCopies.associate { it.key to it.value })
            }
        }
    }

    /**
     * Called after one or more entries have been updated.
     *
     * @param events The entries just updated.
     * @throws CacheEntryListenerException if there is problem executing the listener
     */
    override fun onUpdated(events: MutableIterable<CacheEntryEvent<out K, out V>>) {
        submit("put all updated cache entries") {
            val eventCopies = events.map { EventCopy(it.key, it.value) }
            log.trace { "BackCache cache entry updated. cache=$cacheIdentifier count=${eventCopies.size}" }
            suspend {
                targetCache.putAll(eventCopies.associate { it.key to it.value })
            }
        }
    }

    /**
     * Called after one or more entries have been removed. If no entry existed for
     * a key an event is not raised for it.
     *
     * @param events The entries just removed.
     * @throws CacheEntryListenerException if there is problem executing the listener
     */
    override fun onRemoved(events: MutableIterable<CacheEntryEvent<out K, out V>>) {
        submit("remove all removed cache entries") {
            val eventKeys = events.map { it.key }
            log.trace { "BackCache cache entry removed. cache=$cacheIdentifier count=${eventKeys.size}" }
            suspend {
                targetCache.removeAll(eventKeys.toCollection(LinkedHashSet()))
            }
        }
    }

    /**
     * Called after one or more entries have been expired by the cache. This is not
     * necessarily when an entry is expired, but when the cache detects the expiry.
     *
     * @param events The entries just removed.
     * @throws CacheEntryListenerException if there is problem executing the listener
     */
    override fun onExpired(events: MutableIterable<CacheEntryEvent<out K, out V>>) {
        submit("remove all expired cache entries") {
            val eventKeys = events.map { it.key }
            log.trace { "BackCache cache entry expired. cache=$cacheIdentifier count=${eventKeys.size}" }
            suspend {
                targetCache.removeAll(eventKeys.toCollection(LinkedHashSet()))
            }
        }
    }

    @Suppress("ReturnCount", "TooGenericExceptionCaught")
    private fun submit(operation: String, blockFactory: () -> (suspend () -> Unit)) {
        if (!shouldAcceptCallback()) return
        if (!admission.tryAcquire()) {
            log.debug {
                "Reject callback because admission is full. " +
                        "operation=$operation cache=$cacheIdentifier maxInFlight=$maxInFlightCallbacks"
            }
            return
        }
        var permitTransferred = false
        try {
            if (!shouldAcceptCallback()) return
            val block = blockFactory()
            if (!shouldAcceptCallback()) return
            val job = scope.launch(start = CoroutineStart.LAZY) {
                try {
                    if (!closed.get()) {
                        applyEvent(operation, block)
                    }
                } finally {
                    admission.release()
                }
            }
            if (!job.start()) {
                admission.release()
            }
            permitTransferred = true
        } finally {
            if (!permitTransferred) {
                admission.release()
            }
        }
    }

    /**
     * Callback admission is linearized by a successful [Semaphore.tryAcquire] after
     * the closed and target-cache checks. A callback that owns a permit is in flight;
     * [close] requests cooperative cancellation without waiting for that backend call
     * to return.
     */
    private fun shouldAcceptCallback(): Boolean = !closed.get() && !targetCache.isClosed()

    @Suppress("TooGenericExceptionCaught")
    private suspend fun applyEvent(
        operation: String,
        block: suspend () -> Unit,
    ) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.error(e) { "Fail to $operation. cache=$cacheIdentifier" }
        }
    }

    private data class EventCopy<K: Any, V: Any>(val key: K, val value: V)

    private fun String.sanitizeLogIdentifier(): String {
        val sanitized = replace(Regex("[^A-Za-z0-9._\\$-]"), "_").take(128)
        return sanitized.ifEmpty { "unknown" }
    }
}
