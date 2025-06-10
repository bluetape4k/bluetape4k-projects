package io.bluetape4k.cache.nearcache.coroutines

import io.bluetape4k.cache.jcache.coroutines.SuspendCache
import io.bluetape4k.cache.jcache.coroutines.SuspendCacheEntry
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.info
import io.bluetape4k.logging.trace
import io.bluetape4k.logging.warn
import io.bluetape4k.utils.Runtimex
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.chunked
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.cache.configuration.MutableCacheEntryListenerConfiguration
import kotlin.system.measureTimeMillis


/**
 * 분산환경에서 front cache, back cache 를 활용하여 빠른 Access 와 분산환경에서의 Data consistency를 만족하는
 * Coroutines 환경하에서 사용하는 Near Cache입니다.
 *
 *
 * @param K Cache entry key type
 * @param V Cache entry value type
 * @property frontCache 로컬 캐시
 * @property backCache 분산환경에서 사용할 원격 캐시
 * @constructor Create empty Co near cache
 */
class NearSuspendCache<K: Any, V: Any> private constructor(
    private val frontCache: SuspendCache<K, V>,
    private val backCache: SuspendCache<K, V>,
    private val checkExpiryPeriod: Long,
): SuspendCache<K, V> by backCache,
   CoroutineScope by CoroutineScope(CoroutineName("nearCoCache") + Dispatchers.IO) {

    companion object: KLoggingChannel() {
        const val DEFAULT_EXPIRY_CHECK_PERIOD = 30_000L

        operator fun <K: Any, V: Any> invoke(
            frontCache: SuspendCache<K, V>,
            backCache: SuspendCache<K, V>,
            checkExpiryPeriod: Long = DEFAULT_EXPIRY_CHECK_PERIOD,
        ): NearSuspendCache<K, V> {
            log.info { "Back cache의 event 를 수신하는 listener를 생성합니다..." }

            val cacheEntryEventListenerCfg = MutableCacheEntryListenerConfiguration(
                { BackSuspendCacheEntryEventListener(frontCache) },
                null,
                false,
                false
            )
            log.info { "back cache의 이벤트를 수신할 수 있도록 listener 등록. listenerCfg=$cacheEntryEventListenerCfg" }
            backCache.registerCacheEntryListener(cacheEntryEventListenerCfg)

            log.info { "Create CoNearCache instance." }
            return NearSuspendCache(frontCache, backCache, checkExpiryPeriod)
        }
    }

    init {
        if (checkExpiryPeriod in 1000..Int.MAX_VALUE) {
            runBlocking(Dispatchers.IO) {
                checkBackCacheExpiration()
            }
        }
    }

    private suspend fun checkBackCacheExpiration() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val job = scope.launch {
            val entrySizer = atomic(0)

            while (!isClosed() && isActive) {
                runCatching {
                    delay(checkExpiryPeriod)
                    log.trace { "backCache의 cache entry가 expire 되었는지 검사합니다... check expiration period=$checkExpiryPeriod" }
                    entrySizer.value = 0
                    val elapsed = measureTimeMillis {
                        entries().chunked(100)
                            .onEach { entries ->
                                runCatching {
                                    if (!isClosed()) {
                                        val frontKeys = entries.map { it.key }.toSet()
                                        entrySizer.addAndGet(frontKeys.size)

                                        val backKeys = backCache.getAll(frontKeys).map { it.key }.toSet()
                                        val keysToRemove = frontKeys - backKeys
                                        log.trace { "key size to expire in frontCache=${keysToRemove.size}" }
                                        if (keysToRemove.isNotEmpty()) {
                                            frontCache.removeAll(keysToRemove)
                                        }
                                    }
                                }.onFailure {
                                    log.warn(it) { "Fail to check bachCache expiration." }
                                }
                            }
                            .collect()
                    }
                    log.trace { "bachCache cache entry expire 검사 완료. entrySize=${entrySizer.value}, elapsed=$elapsed msec" }
                }
            }
        }

        Runtimex.addShutdownHook {
            runCatching { job.cancel() }
        }
    }

    override fun entries(): Flow<SuspendCacheEntry<K, V>> = frontCache.entries()

    override suspend fun clear() {
        log.info { "NearCoCache의 Front cache를 Clear합니다." }
        runCatching { frontCache.clear() }
    }

    suspend fun clearAll() = coroutineScope {
        log.info {
            "front cache, back cache 모두 clear 합니다. 단 back cache 를 공유한 다른 near cache에는 전파되지 않습니다. " +
                    "전파를 위해서는 removeAll을 사용하세요"
        }
        val frontClearJob = launch(coroutineContext) { frontCache.clear() }
        val backClearJob = launch(coroutineContext) { backCache.clear() }

        frontClearJob.join()
        backClearJob.join()
    }

    override suspend fun close() {
        log.info { "Near Cache 의 Front Cache를 Close 합니다." }
        runCatching { frontCache.close() }
    }

    override fun isClosed(): Boolean = frontCache.isClosed()

    override suspend fun containsKey(key: K): Boolean {
        return frontCache.containsKey(key) || backCache.containsKey(key)
    }

    override suspend fun get(key: K): V? = coroutineScope {
        frontCache.get(key)
            ?: backCache.get(key)
                ?.also { value ->
                    launch(coroutineContext) {
                        frontCache.put(key, value)
                    }
                }
    }

    override fun getAll(): Flow<SuspendCacheEntry<K, V>> {
        return frontCache.getAll()
    }

    override fun getAll(vararg keys: K): Flow<SuspendCacheEntry<K, V>> =
        getAll(keys.toSet())

    override fun getAll(keys: Set<K>): Flow<SuspendCacheEntry<K, V>> {
        return frontCache.getAll(keys)
    }

    override suspend fun getAndPut(key: K, value: V): V? = coroutineScope {
        frontCache.getAndPut(key, value)?.apply {
            launch(coroutineContext) {
                backCache.putIfAbsent(key, value)
            }
        }
    }

    override suspend fun getAndRemove(key: K): V? {
        log.trace { "get and remove if exists cache entry. key=$key" }
        return get(key)?.apply { remove(key) }
    }

    override suspend fun getAndReplace(key: K, value: V): V? {
        log.trace { "get entry, and put new value if exists. key=$key, new value=$value" }
        return get(key)?.apply { put(key, value) }
    }

    override suspend fun put(key: K, value: V) = coroutineScope {
        frontCache.put(key, value).apply {
            launch(coroutineContext) {
                backCache.put(key, value)
            }
        }
    }

    override suspend fun putAll(map: Map<K, V>) = coroutineScope {
        frontCache.putAll(map).apply {
            launch {
                backCache.putAll(map)
            }
        }
    }

    override suspend fun putAllFlow(entries: Flow<Pair<K, V>>) {
        entries.onEach { put(it.first, it.second) }.collect()
    }

    override suspend fun putIfAbsent(key: K, value: V): Boolean = coroutineScope {
        frontCache.putIfAbsent(key, value).apply {
            launch {
                backCache.putIfAbsent(key, value)
            }
        }
    }

    override suspend fun remove(key: K): Boolean = coroutineScope {
        frontCache.remove(key).apply {
            launch {
                backCache.remove(key)
            }
        }
    }

    override suspend fun remove(key: K, oldValue: V): Boolean {
        frontCache.remove(key, oldValue)
        // TODO: 왜  backCache.remove(key, oldValue) 를 직접 사용하지 않았는지 이유를 기록해야 한다
        // NOTE: 아마 remove(key, oldValue) 는 event 를 발생시키지 않아서 직접 remove를 수행하도록 하는 걸로 추측한다 
        if (backCache.containsKey(key) && backCache.get(key) == oldValue) {
            return backCache.remove(key)
        }
        return false
    }

    override suspend fun removeAll() = coroutineScope {
        frontCache.removeAll()
        // NOTE: Redisson에서는 bulk operation 의 경우 REMOVED event 가 발생하지 않습니다!!!
        backCache.entries()
            .map {
                launch {
                    backCache.remove(it.key)
                }
            }
            .toList()
            .joinAll()
    }

    override suspend fun removeAll(vararg keys: K) {
        removeAll(keys.toSet())
    }

    override suspend fun removeAll(keys: Set<K>) = coroutineScope {
        frontCache.removeAll(keys)
        // NOTE: Redisson에서는 bulk operation 의 경우 REMOVED event 가 발생하지 않습니다!!!
        keys.map {
            launch { remove(it) }
        }.joinAll()
    }

    override suspend fun replace(key: K, oldValue: V, newValue: V): Boolean {
        frontCache.replace(key, oldValue, newValue)

        // NOTE: Redisson에서는 replace 가 event 를 발생시키지 않습니다!!!
        if (backCache.containsKey(key) && backCache.get(key) == oldValue) {
            put(key, newValue)
            return true
        }
        return false
    }

    override suspend fun replace(key: K, value: V): Boolean {
        frontCache.replace(key, value)

        // NOTE: Redisson에서는 replace 가 event 를 발생시키지 않습니다!!!
        if (backCache.containsKey(key)) {
            put(key, value)
            return true
        }
        return false
    }
}
