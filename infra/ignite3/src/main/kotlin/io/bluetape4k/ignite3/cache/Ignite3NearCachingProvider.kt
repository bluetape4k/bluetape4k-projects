package io.bluetape4k.ignite3.cache

import io.bluetape4k.ignite3.igniteClient as createIgniteClient
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import java.net.URI
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import javax.cache.CacheManager
import javax.cache.configuration.OptionalFeature
import javax.cache.spi.CachingProvider
import kotlin.concurrent.withLock

/**
 * Apache Ignite 3.x [NearCache][io.bluetape4k.cache.nearcache.NearCache]를 제공하는
 * JCache [CachingProvider] SPI 구현체입니다.
 *
 * URI 형식: `ignite3://host:port` (예: `ignite3://localhost:10800`)
 *
 * 동일한 URI + ClassLoader 조합으로 요청하면 동일한 [Ignite3NearCacheJCacheManager] 인스턴스를 반환하며,
 * [close] 호출 시 관리 중인 모든 [Ignite3NearCacheJCacheManager]를 닫습니다.
 *
 * **SPI 등록**: `META-INF/services/javax.cache.spi.CachingProvider`에 등록되어 있습니다.
 *
 * ### 사용 예시
 * ```kotlin
 * val provider = Ignite3NearCachingProvider()
 * val manager = provider.getCacheManager(
 *     URI("ignite3://localhost:10800"), null, null
 * ) as Ignite3NearCacheJCacheManager
 *
 * val config = igniteNearCacheConfig<Long, String>(tableName = "MY_CACHE")
 * val cache = manager.createCache("MY_CACHE", config)
 * cache.put(1L, "hello")
 * ```
 */
class Ignite3NearCachingProvider: CachingProvider {

    companion object: KLogging() {
        /** 기본 Ignite 3.x 씬 클라이언트 호스트 */
        const val DEFAULT_HOST = "localhost"

        /** 기본 Ignite 3.x 씬 클라이언트 포트 */
        const val DEFAULT_PORT = 10800

        /** 기본 URI (`ignite3://localhost:10800`) */
        val DEFAULT_URI: URI = URI("ignite3://$DEFAULT_HOST:$DEFAULT_PORT")
    }

    /** ClassLoader → (URI → Manager) 매핑 레지스트리 */
    private val managers = ConcurrentHashMap<ClassLoader, MutableMap<URI, Ignite3NearCacheJCacheManager>>()
    private val lock = ReentrantLock()

    /**
     * 지정한 URI와 ClassLoader에 대응하는 [Ignite3NearCacheJCacheManager]를 반환합니다.
     *
     * 동일한 URI + ClassLoader 조합이면 동일 인스턴스를 반환합니다 (getOrCreate 시멘틱).
     * URI가 null이면 [getDefaultURI]를, classLoader가 null이면 [getDefaultClassLoader]를 사용합니다.
     *
     * URI 형식: `ignite3://host:port`
     *
     * @param uri 연결할 Ignite 3.x 서버 URI (`ignite3://host:port` 형식)
     * @param classLoader [CacheManager] 로드에 사용할 ClassLoader
     * @param properties 추가 설정 속성 (현재 미사용)
     */
    override fun getCacheManager(uri: URI?, classLoader: ClassLoader?, properties: Properties?): CacheManager {
        val cacheUri = uri ?: defaultURI
        val cacheClassLoader = classLoader ?: defaultClassLoader

        log.debug { "Ignite3 NearCacheManager 조회. uri=$cacheUri" }

        val uri2manager = managers.computeIfAbsent(cacheClassLoader) { ConcurrentHashMap() }

        // 이미 등록된 Manager가 있으면 반환
        uri2manager[cacheUri]?.let { return it }

        // URI에서 host:port 추출 후 IgniteClient 생성
        val address = parseAddress(cacheUri)
        log.debug { "Ignite3 NearCacheManager 신규 생성. address=$address, uri=$cacheUri" }
        val client = createIgniteClient(address)

        val manager = Ignite3NearCacheJCacheManager(
            client = client,
            classLoader = cacheClassLoader,
            cacheProvider = this,
            properties = properties,
            uri = cacheUri,
        )

        // putIfAbsent: 다른 스레드가 먼저 생성한 경우 새로 만든 client/manager를 버림
        val existingManager = uri2manager.putIfAbsent(cacheUri, manager)
        if (existingManager != null) {
            log.debug { "동시 생성 감지 - 기존 Manager 반환. uri=$cacheUri" }
            runCatching { client.close() }
            return existingManager
        }
        return manager
    }

    /**
     * URI에서 `host:port` 주소 문자열을 추출합니다.
     *
     * URI 파싱에 실패하면 기본값(`localhost:10800`)을 반환합니다.
     */
    private fun parseAddress(uri: URI): String {
        val host = uri.host?.takeIf { it.isNotBlank() } ?: DEFAULT_HOST
        val port = if (uri.port > 0) uri.port else DEFAULT_PORT
        return "$host:$port"
    }

    /**
     * 지정한 URI와 ClassLoader에 대응하는 [Ignite3NearCacheJCacheManager]를 반환합니다.
     * properties는 기본값([getDefaultProperties])을 사용합니다.
     */
    override fun getCacheManager(uri: URI?, classLoader: ClassLoader?): CacheManager =
        getCacheManager(uri, classLoader, defaultProperties)

    /**
     * 기본 URI와 ClassLoader로 [Ignite3NearCacheJCacheManager]를 반환합니다.
     */
    override fun getCacheManager(): CacheManager =
        getCacheManager(defaultURI, defaultClassLoader)

    /** 기본 [ClassLoader]를 반환합니다. */
    override fun getDefaultClassLoader(): ClassLoader = javaClass.classLoader

    /** 기본 URI (`ignite3://localhost:10800`)를 반환합니다. */
    override fun getDefaultURI(): URI = DEFAULT_URI

    /** 기본 [Properties]를 반환합니다 (빈 Properties). */
    override fun getDefaultProperties(): Properties = Properties()

    /**
     * 등록된 모든 ClassLoader의 [Ignite3NearCacheJCacheManager]를 닫습니다.
     */
    override fun close() {
        lock.withLock {
            managers.keys.toList().forEach { close(it) }
        }
    }

    /**
     * 지정한 ClassLoader에 대응하는 모든 [Ignite3NearCacheJCacheManager]를 닫습니다.
     */
    override fun close(classLoader: ClassLoader) {
        log.info { "Ignite3NearCachingProvider 종료. classLoader=$classLoader" }
        managers.remove(classLoader)?.values?.forEach { manager ->
            runCatching { manager.close() }
        }
    }

    /**
     * 지정한 URI + ClassLoader에 대응하는 [Ignite3NearCacheJCacheManager]를 닫습니다.
     */
    override fun close(uri: URI, classLoader: ClassLoader) {
        log.info { "Ignite3NearCachingProvider 항목 제거. uri=$uri, classLoader=$classLoader" }
        managers[classLoader]?.let { uri2manager ->
            uri2manager.remove(uri)?.let { manager ->
                runCatching { manager.close() }
            }
            if (uri2manager.isEmpty()) {
                managers.remove(classLoader)
            }
        }
    }

    /**
     * 선택적 JCache 기능 지원 여부를 반환합니다 (현재 미지원).
     */
    override fun isSupported(optionalFeature: OptionalFeature?): Boolean = false
}
