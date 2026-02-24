package io.bluetape4k.ignite3.cache

import io.bluetape4k.ignite3.AbstractIgnite3Test
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.net.URI
import javax.cache.CacheException

/**
 * [Ignite3NearCachingProvider]의 JCache SPI 동작을 검증하는 통합 테스트입니다.
 *
 * - [Ignite3NearCachingProvider.getCacheManager]: URI → Manager getOrCreate 시멘틱
 * - [Ignite3NearCacheJCacheManager.createNearCache]: [IgniteNearCacheConfig]로 [NearCache] 생성
 * - [Ignite3NearCacheJCacheManager.getCache]: 캐시 이름으로 조회
 * - [Ignite3NearCacheJCacheManager.destroyCache]: 캐시 제거
 * - [Ignite3NearCachingProvider.close]: Provider 전체 종료
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Ignite3NearCachingProviderTest: AbstractIgnite3Test() {

    companion object: KLogging() {
        private const val TABLE_NAME = "TEST_PROVIDER_CACHE"
    }

    private lateinit var provider: Ignite3NearCachingProvider

    /** Testcontainers 서버 URI (`ignite3://host:port` 형식) */
    private val serverUri get() = URI("ignite3://${ignite3Server.url}")

    @BeforeEach
    fun setup() {
        provider = Ignite3NearCachingProvider()
        // 테이블이 존재하면 기존 데이터 삭제
        runCatching {
            igniteClient.sql().execute(null, "DELETE FROM $TABLE_NAME").close()
        }
    }

    @AfterEach
    fun teardown() {
        runCatching { provider.close() }
    }

    @Test
    fun `getCacheManager 반환 타입이 Ignite3NearCacheJCacheManager 이어야 함`() {
        val manager = provider.getCacheManager(serverUri, null, null)
        manager.shouldBeInstanceOf<Ignite3NearCacheJCacheManager>()
    }

    @Test
    fun `동일한 URI로 getCacheManager 호출 시 같은 인스턴스 반환`() {
        val manager1 = provider.getCacheManager(serverUri, null, null)
        val manager2 = provider.getCacheManager(serverUri, null, null)

        (manager1 === manager2).shouldBeEqualTo(true)
    }

    @Test
    fun `null URI로 getCacheManager 호출 시 기본 URI 사용`() {
        // provider의 기본 URI가 DEFAULT_URI와 동일한지 확인 (연결 없이 설정 검증)
        provider.defaultURI shouldBeEqualTo Ignite3NearCachingProvider.DEFAULT_URI

        // null URI를 넘기면 DEFAULT_URI(localhost:10800)로 연결을 시도함
        // 테스트 환경에서는 localhost:10800에 서버가 없으므로 연결 실패 예외가 발생해야 함
        assertThrows<Exception> {
            provider.getCacheManager(null, null, null)
        }
    }

    @Test
    fun `createNearCache로 NearCache 생성 후 put-get 동작 검증`() {
        val manager = provider.getCacheManager(serverUri, null, null) as Ignite3NearCacheJCacheManager
        val config = igniteNearCacheConfig<Long, String>(tableName = TABLE_NAME)
        val cache = manager.createNearCache(config)

        cache.put(1L, "hello")
        cache.get(1L) shouldBeEqualTo "hello"
    }

    @Test
    fun `createNearCache 후 getCache로 동일 인스턴스 조회 가능`() {
        val manager = provider.getCacheManager(serverUri, null, null) as Ignite3NearCacheJCacheManager
        val config = igniteNearCacheConfig<Long, String>(tableName = TABLE_NAME)
        manager.createNearCache(config)

        val cache = manager.getCache<Long, String>(TABLE_NAME)
        cache.shouldNotBeNull()
    }

    @Test
    fun `동일한 이름으로 createNearCache 두 번 호출 시 CacheException 발생`() {
        val manager = provider.getCacheManager(serverUri, null, null) as Ignite3NearCacheJCacheManager
        val config = igniteNearCacheConfig<Long, String>(tableName = TABLE_NAME)
        manager.createNearCache(config)

        assertThrows<CacheException> {
            manager.createNearCache(config)
        }
    }

    @Test
    fun `IgniteNearCacheConfig 외 설정으로 createCache 호출 시 CacheException 발생`() {
        val manager = provider.getCacheManager(serverUri, null, null) as Ignite3NearCacheJCacheManager

        assertThrows<CacheException> {
            manager.createCache<Long, String, javax.cache.configuration.MutableConfiguration<Long, String>>(
                TABLE_NAME,
                javax.cache.configuration.MutableConfiguration()
            )
        }
    }

    @Test
    fun `destroyCache 후 getCache로 조회하면 null 반환`() {
        val manager = provider.getCacheManager(serverUri, null, null) as Ignite3NearCacheJCacheManager
        val config = igniteNearCacheConfig<Long, String>(tableName = TABLE_NAME)
        manager.createNearCache(config)
        manager.destroyCache(TABLE_NAME)

        val cache = manager.getCache<Long, String>(TABLE_NAME)
        cache.shouldBeNull()
    }

    @Test
    fun `getCacheNames에 생성된 캐시 이름이 포함됨`() {
        val manager = provider.getCacheManager(serverUri, null, null) as Ignite3NearCacheJCacheManager
        val config = igniteNearCacheConfig<Long, String>(tableName = TABLE_NAME)
        manager.createNearCache(config)

        manager.cacheNames.toList().contains(TABLE_NAME).shouldBeEqualTo(true)
    }

    @Test
    fun `manager close 후 isClosed true 반환`() {
        val manager = provider.getCacheManager(serverUri, null, null) as Ignite3NearCacheJCacheManager
        manager.isClosed.shouldBeEqualTo(false)

        manager.close()
        manager.isClosed.shouldBeEqualTo(true)
    }

    @Test
    fun `manager close 후 getCacheManager 호출 시 새 인스턴스 반환`() {
        val manager1 = provider.getCacheManager(serverUri, null, null)
        manager1.close()

        // 기존 manager가 닫혔으므로 provider는 새 manager를 생성해야 함
        val manager2 = provider.getCacheManager(serverUri, null, null)
        (manager1 === manager2).shouldBeEqualTo(false)
        manager2.isClosed.shouldBeEqualTo(false)
    }

    @Test
    fun `provider close 후 getCacheManager 호출 시 새 인스턴스 반환`() {
        val manager1 = provider.getCacheManager(serverUri, null, null)
        provider.close()

        // provider가 닫혀도 provider 자체는 재사용 가능 (새 manager 생성)
        val manager2 = provider.getCacheManager(serverUri, null, null)
        (manager1 === manager2).shouldBeEqualTo(false)
    }
}
