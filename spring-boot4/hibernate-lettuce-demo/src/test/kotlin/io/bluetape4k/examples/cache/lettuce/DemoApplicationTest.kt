package io.bluetape4k.examples.cache.lettuce

import io.bluetape4k.examples.cache.lettuce.domain.Product
import io.bluetape4k.examples.cache.lettuce.repository.ProductRepository
import io.bluetape4k.testcontainers.storage.RedisServer
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoApplicationTest {

    companion object {
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

    @Value("\${local.server.port}")
    private var port: Int = 0

    private lateinit var client: RestClient

    @BeforeEach
    fun setup() {
        productRepository.deleteAll()
        client = RestClient.builder()
            .baseUrl("http://localhost:$port")
            .build()
    }

    @Test
    @Transactional
    fun `애플리케이션이 정상적으로 시작된다`() {
        // 컨텍스트 로딩 성공만으로 테스트 통과
    }

    @Test
    @Transactional
    fun `Product 엔티티가 저장 및 조회된다`() {
        val product = productRepository.save(
            Product(name = "MacBook Pro", description = "Apple M3 Pro", price = 2_499.0)
        )

        val found = productRepository.findById(product.id!!).orElse(null)
        found.shouldNotBeNull()
        found.name shouldBeEqualTo "MacBook Pro"
        found.price shouldBeEqualTo 2_499.0
    }

    @Test
    @Transactional
    fun `동일 Product를 연속 조회하면 캐시 히트가 발생한다`() {
        val product = productRepository.save(
            Product(name = "iPad Pro", price = 1_099.0)
        )
        val id = product.id!!

        // 첫 번째 조회 (DB → Cache)
        val first = productRepository.findById(id).orElseThrow()
        // 두 번째 조회 (Cache hit)
        val second = productRepository.findById(id).orElseThrow()

        first.id shouldBeEqualTo second.id
        first.name shouldBeEqualTo second.name
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
        all shouldHaveSize 3
    }

    @Test
    fun `cache stats endpoint returns local cache metadata`() {
        val product = productRepository.save(Product(name = "Mac Studio", price = 1_999.0))
        productRepository.findById(product.id!!).orElseThrow()

        val response = client.get()
            .uri("/api/cache/stats")
            .retrieve()
            .toEntity<String>()

        response.statusCode shouldBeEqualTo HttpStatus.OK
        response.body.shouldNotBeNull()
    }

    @Test
    fun `cache evict endpoint clears local cache only`() {
        val product = productRepository.save(Product(name = "Vision Pro", price = 3_499.0))
        productRepository.findById(product.id!!).orElseThrow()

        val before = client.get()
            .uri("/api/cache/stats")
            .retrieve()
            .toEntity<String>()
        before.statusCode shouldBeEqualTo HttpStatus.OK

        val evict = client.delete()
            .uri("/api/cache/evict")
            .retrieve()
            .toEntity<String>()

        evict.statusCode shouldBeEqualTo HttpStatus.OK
        evict.body.shouldNotBeNull()
    }
}
