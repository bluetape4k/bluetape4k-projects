package io.bluetape4k.cache.nearcache.spring

import io.bluetape4k.cache.jcache.jcachingProvider
import io.bluetape4k.cache.nearcache.redis.RedisNearCacheConfig
import io.bluetape4k.cache.nearcache.redis.RedisNearCachingProvider
import io.bluetape4k.cache.nearcache.redis.redisNearCacheConfigurationOf
import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.RedisServer
import org.amshove.kluent.shouldBeEqualTo
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
         * Spring cache 에서 사용할 jcache provider 를 RedisNearCachingProvider 로 지정합니다.
         */
        "spring.cache.jcache.provider=io.bluetape4k.cache.nearcache.redis.RedisNearCachingProvider"
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

            val redissonConfig = RedissonConfiguration.fromInstance(redisson, backCacheConfiguration)

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
            return jcachingProvider<RedisNearCachingProvider>().cacheManager.createCache(
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
     * ** `spring.cache.jcache.provider` 프로퍼티가 [RedisNearCachingProvider] 로 설정되어야 합니다.**
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
    companion object: KLogging() {
        private val redis by lazy { RedisServer.Launcher.redis }

        private val redisson by lazy {
            RedisServer.Launcher.RedissonLib.getRedisson()
        }
    }


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
        val arg = TimebasedUuid.nextBase62String()
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
}
