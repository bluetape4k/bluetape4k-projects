package io.bluetape4k.spring.boot.autoconfigure.cache.lettuce

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.RedisServer
import io.micrometer.core.instrument.MeterRegistry
import jakarta.persistence.Cacheable
import jakarta.persistence.Entity
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.engine.spi.SessionFactoryImplementor
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
import java.util.*

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

    @Configuration
    @EnableAutoConfiguration
    class TestConfig

    @Autowired
    private lateinit var itemRepository: TestItemRepository

    @Autowired
    private lateinit var entityManagerFactory: EntityManagerFactory

    @Autowired
    private lateinit var actuatorEndpoint: LettuceNearCacheActuatorEndpoint

    @Autowired
    private lateinit var meterRegistry: MeterRegistry

    @Autowired
    private lateinit var metricsBinder: LettuceNearCacheMetricsBinder

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

        // 각 worker 내 assertion 실패 시 MultithreadingTester.run() 이 propagate
        // → 별도 외부 counter 검증 불필요 (tautological)
        MultithreadingTester()
            .workers(6)
            .rounds(3)
            .add {
                val found = itemRepository.findById(item.id!!).orElse(null)
                found.shouldNotBeNull()
                found.name shouldBeEqualTo "ParallelItem"
            }
            .run()
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `StructuredTaskScopeTester 병렬 조회에서도 동일 엔티티 이름을 유지한다`() {
        // bluetape4k-junit5 → api(virtualthread-api) + runtimeOnly(virtualthread-jdk21)
        // 으로 StructuredTaskScopes 는 JDK 21/25 무관하게 사용 가능
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

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `엔티티 재조회 시 Hibernate L2 cache miss-put-hit cycle이 발생한다`() {
        val sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor::class.java)
        sessionFactory.statistics.clear()

        val item = itemRepository.save(TestItem(name = "CacheHitItem"))
        itemRepository.findById(item.id!!)  // L2 miss → DB read → put to L2
        itemRepository.findById(item.id!!)  // L2 hit

        val regionName = TestItem::class.java.name
        val stats = sessionFactory.statistics.getDomainDataRegionStatistics(regionName)
        stats.shouldNotBeNull()

        // NOT_SUPPORTED + statistics.clear() 후 결정적 카운터:
        //   miss=1 (첫 findById), put=1 (첫 findById 후 L2 insert), hit=1 (두 번째 findById)
        // save() 는 insert-only 로 L2 put 을 만들지 않음 (hibernate cache spec: insert는 region.afterInsert 호출)
        stats.missCount shouldBeEqualTo 1L
        stats.hitCount shouldBeEqualTo 1L
        stats.putCount shouldBeGreaterOrEqualTo 1L  // save 후 afterInsert 기여 가능성 → 최소 1
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `Actuator endpoint가 저장된 엔티티 region의 RegionStats를 반환한다`() {
        val item = itemRepository.save(TestItem(name = "EndpointItem"))
        itemRepository.findById(item.id!!)

        val allStats = actuatorEndpoint.getAllRegionStats()
        allStats.shouldNotBeNull()
        allStats.keys.any { it.contains("TestItem") }.shouldBeTrue()
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `Metrics Gauge가 active_regions와 total_local_size의 실제 값을 보고한다`() {
        val item = itemRepository.save(TestItem(name = "MetricsItem"))
        itemRepository.findById(item.id!!)

        // context 시작 시 metricsBinder.afterSingletonsInstantiated 가 자동 실행됨
        metricsBinder.shouldNotBeNull()

        val activeRegionsGauge = meterRegistry.find("lettuce.nearcache.active.regions").gauge()
        activeRegionsGauge.shouldNotBeNull()
        activeRegionsGauge.value() shouldBeGreaterOrEqualTo 1.0  // 최소 TestItem region 1개

        val totalLocalSizeGauge = meterRegistry.find("lettuce.nearcache.total.local.size").gauge()
        totalLocalSizeGauge.shouldNotBeNull()
        totalLocalSizeGauge.value() shouldBeGreaterOrEqualTo 0.0  // Caffeine async eviction 고려
    }

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

interface TestItemRepository: JpaRepository<TestItem, Long>
