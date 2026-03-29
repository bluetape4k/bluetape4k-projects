package io.bluetape4k.spring.boot.autoconfigure.cache.lettuce

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.testcontainers.storage.RedisServer
import jakarta.persistence.Cacheable
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest(
    classes = [LettuceNearCacheIntegrationTest.TestConfig::class],
    properties = [
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "bluetape4k.cache.lettuce-near.metrics.enabled=true",
        "bluetape4k.cache.lettuce-near.metrics.enable-caffeine-stats=true",
    ]
)
class LettuceNearCacheIntegrationTest {

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

    @Configuration
    @EnableAutoConfiguration
    class TestConfig

    @Autowired
    private lateinit var itemRepository: TestItemRepository

    @Test
    @Transactional
    fun `엔티티가 저장되고 조회된다`() {
        val item = itemRepository.save(TestItem(name = "TestItem"))
        val found = itemRepository.findById(item.id!!).orElse(null)
        found.shouldNotBeNull()
        found.name shouldBeEqualTo "TestItem"
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `MultithreadingTester 병렬 조회에서도 동일 엔티티를 안정적으로 읽는다`() {
        val item = itemRepository.save(TestItem(name = "ParallelItem"))
        val hitCount = AtomicInteger(0)

        MultithreadingTester()
            .workers(6)
            .rounds(3)
            .add {
                val found = itemRepository.findById(item.id!!).orElse(null)
                found.shouldNotBeNull()
                found.name shouldBeEqualTo "ParallelItem"
                hitCount.incrementAndGet()
            }
            .run()

        hitCount.get() shouldBeEqualTo 18
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `StructuredTaskScopeTester 병렬 조회에서도 동일 엔티티 이름을 유지한다`() {
        assumeTrue(structuredTaskScopeAvailable(), "StructuredTaskScope runtime is not available")

        val item = itemRepository.save(TestItem(name = "StructuredItem"))
        val names = Collections.synchronizedList(mutableListOf<String>())

        StructuredTaskScopeTester()
            .rounds(4)
            .add {
                val found = itemRepository.findById(item.id!!).orElse(null)
                found.shouldNotBeNull()
                names += found.name
            }
            .run()

        names.size shouldBeEqualTo 4
        names.forEach { it shouldBeEqualTo "StructuredItem" }
    }

    private fun structuredTaskScopeAvailable(): Boolean =
        runCatching {
            Class.forName("java.util.concurrent.StructuredTaskScope\$ShutdownOnFailure")
        }.isSuccess
}

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class TestItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var name: String = "",
)

interface TestItemRepository : JpaRepository<TestItem, Long>
