package io.bluetape4k.ignite3.cache

import io.bluetape4k.cache.nearcache.NearCache
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotNull
import org.apache.ignite.client.IgniteClient
import java.net.URI
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import javax.cache.Cache
import javax.cache.CacheException
import javax.cache.CacheManager
import javax.cache.configuration.Configuration
import javax.cache.spi.CachingProvider
import kotlin.concurrent.withLock

/**
 * Apache Ignite 3.x [NearCache]를 위한 JCache [CacheManager] 구현체입니다.
 *
 * [Ignite3NearCachingProvider]가 생성하며, Ignite 3.x 테이블 자동 생성과
 * [NearCache] 인스턴스 생명주기를 관리합니다.
 *
 * - [createCache]: [IgniteNearCacheConfig]를 받아 Ignite 테이블 자동 생성 후 [NearCache] 반환
 * - [getCache]: 캐시 이름으로 기존 [NearCache] 조회
 * - [destroyCache]: [NearCache]를 레지스트리에서 제거하고 Front/Back Cache 초기화
 * - [close]: 모든 [NearCache]와 [IgniteClient]를 닫음
 *
 * @property client Ignite 3.x 씬 클라이언트 (close 시 함께 닫힘)
 * @property classLoader [ClassLoader] 인스턴스
 * @property cacheProvider 이 Manager를 생성한 [Ignite3NearCachingProvider]
 * @property properties [CacheManager] 속성 (nullable)
 * @property uri 연결 URI (`ignite3://host:port` 형식, nullable)
 */
class Ignite3NearCacheJCacheManager(
    val client: IgniteClient,
    private val classLoader: ClassLoader,
    val cacheProvider: CachingProvider,
    private val properties: Properties?,
    private val uri: URI?,
): CacheManager {

    companion object: KLogging()

    /** 테이블 자동 생성에 사용하는 내부 IgniteNearCacheManager */
    private val igniteNearCacheManager = IgniteNearCacheManager(client)

    /** 관리 중인 NearCache 인스턴스 맵 (캐시명 → NearCache) */
    private val caches = ConcurrentHashMap<String, NearCache<*, *>>()

    private val closed = AtomicBoolean(false)
    private val lock = ReentrantLock()

    private fun checkNotClosed() {
        check(!isClosed) { "Ignite3NearCacheJCacheManager가 이미 닫혀 있습니다." }
    }

    /** 이 Manager를 생성한 [CachingProvider]를 반환합니다. */
    override fun getCachingProvider(): CachingProvider = cacheProvider

    /** 이 Manager의 URI를 반환합니다. */
    override fun getURI(): URI? = uri

    /** 이 Manager의 [ClassLoader]를 반환합니다. */
    override fun getClassLoader(): ClassLoader = classLoader

    /** 이 Manager의 속성을 반환합니다. */
    override fun getProperties(): Properties? = properties

    /**
     * 새 [NearCache]를 생성하는 내부 구현 메서드입니다.
     *
     * [createCache] 및 [createNearCache] 확장 함수에서 공통으로 사용합니다.
     * Ignite 3.x 테이블이 없으면 DDL(`CREATE TABLE IF NOT EXISTS`)을 자동 실행합니다.
     *
     * @param K 캐시 키 타입
     * @param V 캐시 값 타입
     * @param cacheName 캐시 이름 (중복 생성 시 [CacheException] 발생)
     * @param config [IgniteNearCacheConfig] 설정
     * @return 생성된 [NearCache] 인스턴스
     * @throws CacheException 동일 이름의 캐시가 이미 존재하는 경우
     */
    @PublishedApi
    internal fun <K: Any, V: Any> createNearCacheImpl(
        cacheName: String,
        config: IgniteNearCacheConfig<K, V>,
    ): NearCache<K, V> {
        checkNotClosed()
        log.info { "Ignite3 NearCache 생성. cacheName=$cacheName, tableName=${config.tableName}" }

        // Ignite 3.x 테이블 자동 생성 (CREATE TABLE IF NOT EXISTS)
        igniteNearCacheManager.ensureTable(
            tableName = config.tableName,
            keyType = config.keyType,
            valueType = config.valueType,
            keyColumn = config.keyColumn,
            valueColumn = config.valueColumn,
        )

        // Back Cache(Ignite3JCache) + Front Cache(Caffeine/JCache) = NearCache 생성
        val backCache = Ignite3JCache(
            tableName = config.tableName,
            client = client,
            keyType = config.keyType,
            valueType = config.valueType,
            keyColumn = config.keyColumn,
            valueColumn = config.valueColumn,
        )
        val nearCache = NearCache(config, backCache)

        // getOrCreate 시멘틱: 이미 존재하는 경우 예외 발생
        val existingCache = caches.putIfAbsent(cacheName, nearCache)
        if (existingCache != null) {
            runCatching { nearCache.close() }
            throw CacheException("캐시 [$cacheName]가 이미 존재합니다.")
        }

        log.debug { "Ignite3 NearCache 생성 완료. cacheName=$cacheName" }
        return nearCache
    }

    /**
     * 새 [NearCache]를 생성하여 관리합니다.
     *
     * [configuration]은 반드시 [IgniteNearCacheConfig] 타입이어야 합니다.
     * [IgniteNearCacheConfig]는 [Configuration]을 구현하므로 JCache 표준 API에서 직접 전달 가능합니다.
     * Ignite 3.x 테이블이 없으면 DDL(`CREATE TABLE IF NOT EXISTS`)을 자동 실행합니다.
     *
     * @param cacheName 캐시 이름 (중복 생성 시 [CacheException] 발생)
     * @param configuration [IgniteNearCacheConfig] 타입의 캐시 설정
     * @return 생성된 [NearCache] 인스턴스
     * @throws CacheException 동일 이름의 캐시가 이미 존재하거나 설정이 잘못된 경우
     * @throws IllegalStateException Manager가 이미 닫혀 있는 경우
     */
    @Suppress("UNCHECKED_CAST", "CAST_NEVER_SUCCEEDS")
    override fun <K: Any, V: Any, C: Configuration<K, V>> createCache(
        cacheName: String,
        configuration: C,
    ): Cache<K, V> {
        val config = configuration as? IgniteNearCacheConfig<K, V>
            ?: throw CacheException("configuration은 IgniteNearCacheConfig 타입이어야 합니다. type=${configuration::class.qualifiedName}")
        return createNearCacheImpl(cacheName, config)
    }

    /**
     * 캐시 이름과 키/값 타입으로 관리 중인 [NearCache]를 조회합니다.
     *
     * @param cacheName 캐시 이름
     * @param keyType 키 타입 클래스
     * @param valueType 값 타입 클래스
     * @return [NearCache] 인스턴스 또는 null
     */
    @Suppress("UNCHECKED_CAST")
    override fun <K: Any, V: Any> getCache(
        cacheName: String?,
        keyType: Class<K>?,
        valueType: Class<V>?,
    ): Cache<K, V>? {
        checkNotClosed()
        cacheName.requireNotBlank("cacheName")
        keyType.requireNotNull("keyType")
        valueType.requireNotNull("valueType")

        log.debug { "Ignite3 NearCache 조회. cacheName=$cacheName" }
        return caches[cacheName] as? NearCache<K, V>
    }

    /**
     * 캐시 이름으로 관리 중인 [NearCache]를 조회합니다 (타입 검사 없음).
     *
     * @param cacheName 캐시 이름
     * @return [NearCache] 인스턴스 또는 null
     */
    @Suppress("UNCHECKED_CAST")
    override fun <K: Any, V: Any> getCache(cacheName: String?): Cache<K, V>? {
        checkNotClosed()
        return getCache(cacheName, Any::class.java, Any::class.java) as? NearCache<K, V>
    }

    /**
     * 관리 중인 모든 캐시 이름 목록을 반환합니다.
     */
    override fun getCacheNames(): MutableIterable<String> = caches.keys.toMutableSet()

    /**
     * 지정한 이름의 [NearCache]를 레지스트리에서 제거하고 Front/Back Cache를 초기화합니다.
     *
     * `clearAllCache()` 호출 후 `close()`를 호출하므로, 제거 후 해당 캐시에 접근하면 오류가 발생합니다.
     *
     * @param cacheName 제거할 캐시 이름
     */
    override fun destroyCache(cacheName: String?) {
        checkNotClosed()
        cacheName.requireNotBlank("cacheName")

        log.debug { "Ignite3 NearCache 제거. cacheName=$cacheName" }
        caches.remove(cacheName)?.let { cache ->
            log.info { "Ignite3 NearCache 제거 완료. cacheName=$cacheName" }
            runCatching { cache.clearAllCache() }
            runCatching { cache.close() }
        }
    }

    /**
     * 관리 기능을 활성화/비활성화합니다 (현재 미지원).
     */
    override fun enableManagement(cacheName: String?, enabled: Boolean) {
        log.info { "enableManagement - 현재 지원하지 않습니다." }
    }

    /**
     * 통계 수집을 활성화/비활성화합니다 (현재 미지원).
     */
    override fun enableStatistics(cacheName: String?, enabled: Boolean) {
        log.info { "enableStatistics - 현재 지원하지 않습니다." }
    }

    /**
     * Manager를 닫고 모든 관리 중인 [NearCache]와 [IgniteClient]를 정리합니다.
     *
     * 호출 즉시 [isClosed]가 `true`로 변경되어, 이후 모든 작업은 [IllegalStateException]이 발생합니다.
     * 이미 닫혀 있으면 호출을 무시합니다.
     */
    override fun close() {
        if (isClosed) return

        lock.withLock {
            if (!isClosed) {
                // closed를 먼저 설정해 재진입(re-entrancy)에 의한 이중 close 방지
                closed.set(true)
                log.info { "Ignite3NearCacheJCacheManager 종료 시작. uri=$uri" }

                // Provider 레지스트리에서 제거 (provider.close(uri, classLoader) → manager.close() 재진입 방지됨)
                runCatching { cacheProvider.close(uri, classLoader) }

                // 관리 중인 모든 NearCache 닫기
                caches.values.forEach { cache ->
                    runCatching { cache.close() }
                }
                caches.clear()

                // IgniteNearCacheManager 닫기
                runCatching { igniteNearCacheManager.close() }

                // IgniteClient 닫기 (이 Manager가 소유)
                runCatching { client.close() }

                log.info { "Ignite3NearCacheJCacheManager 종료 완료. uri=$uri" }
            }
        }
    }

    /**
     * Manager가 닫혀 있는지 여부를 반환합니다.
     */
    override fun isClosed(): Boolean = closed.get()

    /**
     * 이 Manager를 지정한 타입으로 변환합니다.
     *
     * @throws IllegalArgumentException 지원하지 않는 타입인 경우
     */
    override fun <T: Any> unwrap(clazz: Class<T>): T {
        if (clazz.isAssignableFrom(javaClass)) return clazz.cast(this)
        throw IllegalArgumentException("$clazz 타입으로 변환할 수 없습니다.")
    }
}

/**
 * [IgniteNearCacheConfig]를 사용하여 [NearCache]를 생성하는 편의 확장 함수입니다.
 *
 * [CacheManager.createCache]는 `C: Configuration<K,V>` 타입 바운드로 인해 [IgniteNearCacheConfig]를
 * 직접 전달할 수 없으므로, 이 함수를 통해 타입 파라미터 추론 없이 간결하게 사용할 수 있습니다.
 *
 * @param K 캐시 키 타입
 * @param V 캐시 값 타입
 * @param config [IgniteNearCacheConfig] 설정
 * @param cacheName 캐시 이름 (기본값: `config.tableName`)
 * @return 생성된 [NearCache] 인스턴스
 */
inline fun <reified K: Any, reified V: Any> Ignite3NearCacheJCacheManager.createNearCache(
    config: IgniteNearCacheConfig<K, V>,
    cacheName: String = config.tableName,
): NearCache<K, V> = createNearCacheImpl(cacheName, config)
