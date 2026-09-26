package io.bluetape4k.examples.cache.lettuce

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeBlank
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.examples.cache.lettuce.domain.Product
import io.bluetape4k.examples.cache.lettuce.repository.ProductRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.storage.RedisServer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class DemoApplicationTest {

    companion object: KLogging() {
        val redis: RedisServer by lazy { RedisServer.Launcher.redis }

        @JvmStatic
        @DynamicPropertySource
        fun configureRedis(registry: DynamicPropertyRegistry) {
            registry.add("bluetape4k.cache.lettuce-near.redis-uri") {
                "redis://${redis.host}:${redis.port}"
            }
        }
    }

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var applicationContext: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setup() {
        productRepository.deleteAll()
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build()
    }

    @Test
    @Transactional
    fun `애플리케이션이 정상적으로 시작된다`() {
        // 컨텍스트 로딩 성공만으로 테스트 통과
        productRepository.shouldNotBeNull()
        applicationContext.shouldNotBeNull()
    }

    @Test
    @Transactional
    fun `Product 엔티티가 저장 및 조회된다`() {
        val product = productRepository.save(
            Product(name = "MacBook Pro", description = "Apple M3 Pro", price = 2_499.0)
        )

        val found = productRepository.findByIdOrNull(product.id!!).shouldNotBeNull()
        log.debug { "found: $found" }
        found shouldBeEqualTo product
    }

    @Test
    @Transactional
    fun `동일 Product를 연속 조회하면 캐시 히트가 발생한다`() {
        val product = productRepository.save(
            Product(name = "iPad Pro", price = 1_099.0)
        )
        val id = product.id!!

        // 첫 번째 조회 (DB → Cache)
        val first = productRepository.findByIdOrNull(id).shouldNotBeNull()
        log.debug { "first=$first" }

        // 두 번째 조회 (Cache hit)
        val second = productRepository.findByIdOrNull(id).shouldNotBeNull()
        log.debug { "second=$second" }

        first shouldBeEqualTo product
        second shouldBeEqualTo product
    }

    @Test
    fun `여러 Product를 저장하고 전체 조회된다`() {
        productRepository.saveAll(
            listOf(
                Product(name = "iPhone 16", price = 999.0),
                Product(name = "Apple Watch", price = 399.0),
                Product(name = "AirPods Pro", price = 249.0),
            )
        )

        val all = productRepository.findAll()
        all.forEach { log.debug { "product=$it" } }
        all shouldHaveSize 3
    }

    @Test
    fun `cache stats endpoint returns local cache metadata`() {
        val product = productRepository.save(Product(name = "Mac Studio", price = 1_999.0))
        productRepository.findByIdOrNull(product.id!!).shouldNotBeNull()

        val response = mockMvc.perform(get("/api/cache/stats")).andReturn()

        log.debug { "response=${response.response.contentAsString}" }
        response.response.status shouldBeEqualTo HttpStatus.OK.value()
        response.response.contentAsString.shouldNotBeBlank()

    }

    @Test
    fun `cache evict endpoint clears local cache only`() {
        val product = productRepository.save(Product(name = "Vision Pro", price = 3_499.0))
        productRepository.findById(product.id!!).orElseThrow()

        val before = mockMvc.perform(get("/api/cache/stats")).andReturn()
        before.response.status shouldBeEqualTo HttpStatus.OK.value()
        log.debug { "before response=${before.response.contentAsString}" }

        val evict = mockMvc.perform(delete("/api/cache/evict")).andReturn()

        log.debug { "evict response=${evict.response.contentAsString}" }
        evict.response.status shouldBeEqualTo HttpStatus.OK.value()
        evict.response.contentAsString.shouldNotBeBlank()
    }
}
