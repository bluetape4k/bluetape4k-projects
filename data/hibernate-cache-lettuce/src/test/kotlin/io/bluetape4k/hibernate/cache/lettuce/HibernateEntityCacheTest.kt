package io.bluetape4k.hibernate.cache.lettuce

import io.bluetape4k.hibernate.cache.lettuce.model.Person
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.hibernate.cache.spi.RegionFactory
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

/**
 * Hibernate Entity 2nd Level Cache 통합 테스트.
 *
 * - persist → session close → new session load → 2nd level cache hit 확인
 * - evict → reload → DB miss 확인
 * - Hibernate statistics를 통해 캐시 동작 검증
 */
class HibernateEntityCacheTest: AbstractHibernateNearCacheTest() {

    @BeforeEach
    fun clearCacheAndStats() {
        sessionFactory.cache.evictAllRegions()
        sessionFactory.statistics.clear()
    }

    @Test
    fun `Entity 저장 후 새 세션에서 재조회 시 2nd level cache hit`() {
        // 1. Session 1: Entity 저장 → 2nd level cache 적재
        val personId = sessionFactory.openSession().use { session ->
            session.beginTransaction()
            val p = Person().apply { name = "Alice"; age = 30 }
            session.persist(p)
            session.transaction.commit()
            p.id!!
        }

        sessionFactory.statistics.clear()

        // 2. Session 2: 1st level cache miss → 2nd level cache hit
        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            val loaded = session.find(Person::class.java, personId)
            session.transaction.commit()
            loaded.shouldNotBeNull()
            loaded.name shouldBeEqualTo "Alice"
        }

        // 3. Session 3: 또 조회 → 2nd level cache hit
        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            session.find(Person::class.java, personId).shouldNotBeNull()
            session.transaction.commit()
        }

        val stats = sessionFactory.statistics
        stats.secondLevelCacheHitCount shouldBeGreaterThan 0L
    }

    @Test
    fun `동일 Entity를 MultithreadingTester로 병렬 조회해도 2nd level cache hit가 누적된다`() {
        val personId = sessionFactory.openSession().use { session ->
            session.beginTransaction()
            val p = Person().apply { name = "Parallel Alice"; age = 31 }
            session.persist(p)
            session.transaction.commit()
            p.id!!
        }

        // warm-up to populate 2nd level cache
        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            session.find(Person::class.java, personId).shouldNotBeNull()
            session.transaction.commit()
        }
        sessionFactory.statistics.clear()

        val loadedNames = Collections.synchronizedList(mutableListOf<String>())
        MultithreadingTester()
            .workers(6)
            .rounds(3)
            .add {
                sessionFactory.openSession().use { session ->
                    session.beginTransaction()
                    val loaded = session.find(Person::class.java, personId).shouldNotBeNull()
                    loadedNames += loaded.name
                    session.transaction.commit()
                }
            }
            .run()

        loadedNames.size shouldBeEqualTo 18
        loadedNames.forEach { it shouldBeEqualTo "Parallel Alice" }
        sessionFactory.statistics.secondLevelCacheHitCount shouldBeGreaterThan 0L
    }

    @Test
    fun `캐시에 없는 Entity 조회 시 2nd level cache miss`() {
        sessionFactory.statistics.clear()

        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            session.find(Person::class.java, Long.MAX_VALUE).shouldBeNull()
            session.transaction.commit()
        }

        // 존재하지 않는 엔티티이므로 miss (put도 없음)
        val stats = sessionFactory.statistics
        stats.secondLevelCacheHitCount shouldBeEqualTo 0L
    }

    @Test
    fun `캐시 evict 후 재조회 시 DB에서 로드된다`() {
        // 1. Entity 저장
        val personId = sessionFactory.openSession().use { session ->
            session.beginTransaction()
            val p = Person().apply { name = "Bob"; age = 25 }
            session.persist(p)
            session.transaction.commit()
            p.id!!
        }

        // 2. Session 2: 2nd level cache에 올리기
        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            session.find(Person::class.java, personId)
            session.transaction.commit()
        }

        // 3. 2nd level cache evict
        sessionFactory.cache.evictEntityData(Person::class.java, personId)
        sessionFactory.statistics.clear()

        // 4. Session 3: 재조회 → cache miss → DB에서 로드
        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            val loaded = session.find(Person::class.java, personId)
            session.transaction.commit()
            loaded.shouldNotBeNull()
            loaded.name shouldBeEqualTo "Bob"
        }

        val stats = sessionFactory.statistics
        stats.secondLevelCacheMissCount shouldBeGreaterThan 0L
    }

    @Test
    fun `Entity 수정 후 캐시가 갱신된다`() {
        // 1. Entity 저장
        val personId = sessionFactory.openSession().use { session ->
            session.beginTransaction()
            val p = Person().apply { name = "Charlie"; age = 20 }
            session.persist(p)
            session.transaction.commit()
            p.id!!
        }

        // 2. Entity 수정
        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            val p = session.find(Person::class.java, personId)
            p!!.name = "Charlie Updated"
            session.transaction.commit()
        }

        sessionFactory.statistics.clear()

        // 3. 수정 후 재조회 → 최신 값 확인
        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            val loaded = session.find(Person::class.java, personId)
            session.transaction.commit()
            loaded.shouldNotBeNull()
            loaded.name shouldBeEqualTo "Charlie Updated"
        }
    }

    @Test
    fun `region 전체 evict는 local 과 Redis 모두 제거한다`() {
        val personId = sessionFactory.openSession().use { session ->
            session.beginTransaction()
            val p = Person().apply { name = "Eve"; age = 28 }
            session.persist(p)
            session.transaction.commit()
            p.id!!
        }

        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            session.find(Person::class.java, personId).shouldNotBeNull()
            session.transaction.commit()
        }

        val regionFactory = (sessionFactory as SessionFactoryImplementor).serviceRegistry
            .getService(RegionFactory::class.java) as LettuceNearCacheRegionFactory
        val regionName = regionFactory.getCaches().keys.firstOrNull { it.contains("Person") }.also {
            it.shouldNotBeNull()
        }!!
        regionFactory.getCaches()[regionName]!!.backCacheSize().shouldBeGreaterThan(0L)

        sessionFactory.cache.evictEntityData(Person::class.java)

        val cache = regionFactory.getCaches()[regionName]!!
        cache.localCacheSize() shouldBeEqualTo 0L
        cache.backCacheSize() shouldBeEqualTo 0L
        cache.containsKey(personId.toString()) shouldBeEqualTo false
    }

    @Test
    fun `동일 Entity를 StructuredTaskScopeTester로 병렬 조회해도 값 일관성을 유지한다`() {
        assumeTrue(structuredTaskScopeAvailable(), "StructuredTaskScope runtime is not available")

        val personId = sessionFactory.openSession().use { session ->
            session.beginTransaction()
            val p = Person().apply { name = "Structured Bob"; age = 29 }
            session.persist(p)
            session.transaction.commit()
            p.id!!
        }

        val loadedNames = Collections.synchronizedList(mutableListOf<String>())
        StructuredTaskScopeTester()
            .rounds(6)
            .add {
                sessionFactory.openSession().use { session ->
                    session.beginTransaction()
                    val loaded = session.find(Person::class.java, personId).shouldNotBeNull()
                    loadedNames += loaded.name
                    session.transaction.commit()
                }
            }
            .run()

        loadedNames.size shouldBeEqualTo 6
        loadedNames.forEach { it shouldBeEqualTo "Structured Bob" }
    }

    private fun structuredTaskScopeAvailable(): Boolean =
        runCatching {
            Class.forName("java.util.concurrent.StructuredTaskScope\$ShutdownOnFailure")
        }.isSuccess
}
