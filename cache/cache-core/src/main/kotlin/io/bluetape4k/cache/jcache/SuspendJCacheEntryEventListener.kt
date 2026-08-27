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
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.LongAdder
import javax.cache.event.CacheEntryCreatedListener
import javax.cache.event.CacheEntryEvent
import javax.cache.event.CacheEntryExpiredListener
import javax.cache.event.CacheEntryRemovedListener
import javax.cache.event.CacheEntryUpdatedListener

private const val DEFAULT_MAX_IN_FLIGHT_CALLBACKS = 64

/**
 * SuspendJCache listener의 수명 동안 누적되는 low-cardinality 관측 snapshot입니다.
 *
 * 향후 공통 metric backend에 연결할 때의 계약은
 * `bluetape4k.cache.jcache.listener.callbacks` 이름과
 * `outcome=accepted|rejected|ignored|cancelled|failed` 태그입니다. `cache_type` 태그는
 * 정제된 cache 구현 클래스명만 사용하며 key/value/source는 보존하거나 기록하지
 * 않습니다. 카운터와 in-flight gauge는 listener 인스턴스별로 생성되고 [close] 후에도
 * reset하지 않습니다. 현재 core 모듈은 backend를 직접 의존하지 않으므로 이 타입은
 * internal/test-only 경계로 유지합니다.
 */
internal data class SuspendJCacheEntryEventListenerObservation(
    val cacheType: String,
    val acceptedCallbacks: Long,
    val rejectedCallbacks: Long,
    val ignoredCallbacks: Long,
    val cancelledCallbacks: Long,
    val failedCallbacks: Long,
    val closeRequests: Long,
    val inFlightCallbacks: Int,
    val maxInFlightCallbacks: Int,
)

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
    private val observation = ListenerObservationRecorder(cacheIdentifier, maxInFlightCallbacks)

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            observation.recordCloseRequest()
            scope.cancel()
        }
    }

    @JvmSynthetic
    internal fun observationSnapshotForTest(): SuspendJCacheEntryEventListenerObservation =
        observation.snapshot()

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
        if (!shouldAcceptCallback()) {
            observation.recordIgnored()
            return
        }
        if (!admission.tryAcquire()) {
            observation.recordRejected()
            log.debug {
                "Reject callback because admission is full. " +
                        "operation=$operation cache=$cacheIdentifier maxInFlight=$maxInFlightCallbacks"
            }
            return
        }
        observation.recordAccepted()
        var permitTransferred = false
        try {
            if (!shouldAcceptCallback()) {
                observation.recordIgnored()
                return
            }
            val block = blockFactory()
            if (!shouldAcceptCallback()) {
                observation.recordIgnored()
                return
            }
            val job = scope.launch(start = CoroutineStart.LAZY) {
                try {
                    if (!closed.get()) {
                        applyEvent(operation, block)
                    }
                } finally {
                    admission.release()
                    observation.recordRelease()
                }
            }
            job.invokeOnCompletion { cause ->
                if (cause is CancellationException) {
                    observation.recordCancelled()
                }
            }
            if (!job.start()) {
                admission.release()
                observation.recordRelease()
            }
            permitTransferred = true
        } catch (e: CancellationException) {
            observation.recordCancelled()
            throw e
        } catch (e: Exception) {
            observation.recordFailed()
            throw e
        } finally {
            if (!permitTransferred) {
                admission.release()
                observation.recordRelease()
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
            observation.recordFailed()
            log.error(e) { "Fail to $operation. cache=$cacheIdentifier" }
        }
    }

    private data class EventCopy<K: Any, V: Any>(val key: K, val value: V)

    private fun String.sanitizeLogIdentifier(): String {
        val sanitized = replace(Regex("[^A-Za-z0-9._\\$-]"), "_").take(128)
        return sanitized.ifEmpty { "unknown" }
    }

    private class ListenerObservationRecorder(
        private val cacheType: String,
        private val maxInFlightCallbacks: Int,
    ) {
        private val acceptedCallbacks = LongAdder()
        private val rejectedCallbacks = LongAdder()
        private val ignoredCallbacks = LongAdder()
        private val cancelledCallbacks = LongAdder()
        private val failedCallbacks = LongAdder()
        private val closeRequests = LongAdder()
        private val inFlightCallbacks = AtomicInteger()

        fun recordAccepted() {
            acceptedCallbacks.increment()
            inFlightCallbacks.incrementAndGet()
        }

        fun recordRejected() = rejectedCallbacks.increment()
        fun recordIgnored() = ignoredCallbacks.increment()
        fun recordCancelled() = cancelledCallbacks.increment()
        fun recordFailed() = failedCallbacks.increment()
        fun recordCloseRequest() = closeRequests.increment()
        fun recordRelease() = inFlightCallbacks.decrementAndGet()

        fun snapshot(): SuspendJCacheEntryEventListenerObservation =
            SuspendJCacheEntryEventListenerObservation(
                cacheType = cacheType,
                acceptedCallbacks = acceptedCallbacks.sum(),
                rejectedCallbacks = rejectedCallbacks.sum(),
                ignoredCallbacks = ignoredCallbacks.sum(),
                cancelledCallbacks = cancelledCallbacks.sum(),
                failedCallbacks = failedCallbacks.sum(),
                closeRequests = closeRequests.sum(),
                inFlightCallbacks = inFlightCallbacks.get(),
                maxInFlightCallbacks = maxInFlightCallbacks,
            )
    }
}
