package io.bluetape4k.cache.nearcache.spring

import io.bluetape4k.cache.jcache.jcachingProvider
import io.bluetape4k.cache.nearcache.RedisNearCacheConfig
import io.bluetape4k.cache.nearcache.RedissonNearCachingProvider
import io.bluetape4k.cache.nearcache.redisNearCacheConfigurationOf
import io.bluetape4k.codec.Base58
import io.bluetape4k.cache.RedisServers
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.redisson.jcache.configuration.RedissonConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import javax.cache.Cache
import javax.cache.configuration.MutableConfiguration
import javax.cache.expiry.EternalExpiryPolicy

@SpringBootTest(
    properties = [
        /**
         * Spring cache 에서 사용할 jcache provider 를 RedissonNearCachingProvider 로 지정합니다.
         */
        "spring.cache.jcache.provider=io.bluetape4k.cache.nearcache.RedissonNearCachingProvider"
    ]
)
class SpringCacheUsingNearCacheTest {

    /**
     * Spring cache 를 이용해 반환값을 캐시하는 메소드를 가진
     */
    open class HasCacheableMethod {

        @Cacheable("test-cache")
        open fun someCacheableFunc(id: String): String? {
            return id.ifBlank { null }
        }
    }

    @Configuration
    open class RedisNearCacheForSpringCacheConfiguration {
        /**
         * Spring cache 에서 사용할 jcache cache 를 만듭니다.
         * 반드시 Bean 으로 등록할 필요는 없으나, Spring autoconfiguration 이 실행되기 이전에 캐시가 만들어져야 합니다.
         * */

        @Bean
        open fun nearCacheForSpringCache(): Cache<Any, Any> {
            val backCacheConfiguration = MutableConfiguration<Any, Any>().apply {
                this.setExpiryPolicyFactory(EternalExpiryPolicy.factoryOf())
            }

            val redissonConfig = RedissonConfiguration.fromInstance(RedisServers.redisson, backCacheConfiguration)

            /**
             * BackCache 용 redisson jcache 설정인 [RedissonConfiguration] 과
             * FrontCache 및 공용 설정인 [io.bluetape4k.cache.nearcache.NearCacheConfig] 을 포함하는 [RedisNearCacheConfig]
             * 을 생성합니다.
             */
            val redisNearCacheConfig: RedisNearCacheConfig<Any, Any> = redisNearCacheConfigurationOf<Any, Any> {
                this.redissonConfig = redissonConfig as RedissonConfiguration<Any, Any>
            }

            /**
             * RedisNearCacheConfig 을 이용해 cache 를 생성합니다.
             */
            return jcachingProvider<RedissonNearCachingProvider>().cacheManager.createCache(
                "test-cache", redisNearCacheConfig
            )
        }

        /**
         * Spring Cache 를 이용해 결과값을 캐싱하는 메소드를 가지는 bean 을 정의합니다.
         *
         */
        @Bean
        open fun someCacheable(): HasCacheableMethod {
            return HasCacheableMethod()
        }
    }

    /**
     * NearCache 를 통해 Spring Cache를 사용하는 SpringBootApplication 입니다.
     *
     * ** `spring.cache.jcache.provider` 프로퍼티가 [RedissonNearCachingProvider] 로 설정되어야 합니다.**
     *
     * (이 테스트에서의 @SpringBootTest 어노테이션에서 정의해 두었습니다)
     */
    @EnableCaching
    @Import(RedisNearCacheForSpringCacheConfiguration::class)
    @SpringBootApplication
    open class UseNearCacheForSpringCacheApplication

    /**
     * 이하의 코드는 Spring cache + NearCache 가 제대로 동작하는지 테스트하기 위한 테스트 코드입니다.
     */
    companion object: KLogging()


    @Autowired
    private lateinit var someCacheable: HasCacheableMethod

    @Autowired
    private lateinit var cache: Cache<Any, Any?>

    @BeforeEach
    fun setup() {
        cache.clear()
    }

    @Test
    fun `cacheable value should be calculated only once`() {
        val arg = Base58.randomString(16)
        // Cacheable 메소드 최초 호출 전에는 cache 에 값 없음
        cache.get(arg) shouldBeEqualTo null
        val first = someCacheable.someCacheableFunc(arg)
        // Cacheable 메소드 최초 호출 후에는 리턴값이 캐시됨
        cache.get(arg) shouldBeEqualTo arg
        val second = someCacheable.someCacheableFunc(arg)
        second shouldBeEqualTo first
    }

    @Test
    fun `cacheable should work when cached value is null`() {
        val arg = ""
        // Cacheable 메소드 최초 호출 전에는 cache 에 값 없음
        cache.get(arg) shouldBeEqualTo null
        val first = someCacheable.someCacheableFunc(arg)
        // Cacheable 메소드 최초 호출 후에는 리턴값이 캐시됨
        cache.get(arg) shouldBeEqualTo null
        val second = someCacheable.someCacheableFunc(arg)
        second shouldBeEqualTo first
    }

    /**
     * [MultithreadingTester]를 사용하여 여러 스레드에서 동시에 [@Cacheable] 메서드를 호출할 때
     * Spring 캐시 추상화가 동시성 환경에서 올바르게 동작하는지 검증하는 테스트입니다.
     */
    @Test
    fun `multithreading - cacheable method should return consistent results under concurrent access`() {
        val arg = Base58.randomString(16)
        // 최초 호출로 캐시에 값을 등록
        val expected = someCacheable.someCacheableFunc(arg)
        expected shouldBeEqualTo arg

        MultithreadingTester()
            .workers(16)
            .rounds(4)
            .add {
                val result = someCacheable.someCacheableFunc(arg)
                result shouldBeEqualTo expected
            }
            .run()

        // 동시 호출 후에도 캐시에 값이 올바르게 유지되어야 함
        cache.get(arg).shouldNotBeNull() shouldBeEqualTo arg
    }

    /**
     * [StructuredTaskScopeTester]를 사용하여 Virtual Thread 기반으로 [@Cacheable] 메서드를 동시에 호출하고
     * 캐시 조회와 쓰기가 동시성 환경에서 올바르게 동작하는지 검증하는 테스트입니다.
     */
    @Test
    fun `structured task scope - cacheable method should return consistent results under concurrent access`() {
        val arg = Base58.randomString(16)
        // 최초 호출로 캐시에 값을 등록
        val expected = someCacheable.someCacheableFunc(arg)
        expected shouldBeEqualTo arg

        StructuredTaskScopeTester()
            .rounds(32)
            .add {
                val result = someCacheable.someCacheableFunc(arg)
                result shouldBeEqualTo expected
            }
            .run()

        // 동시 호출 후에도 캐시에 값이 올바르게 유지되어야 함
        cache.get(arg).shouldNotBeNull() shouldBeEqualTo arg
    }
}
