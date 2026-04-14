package io.bluetape4k.hibernate.cache.lettuce

import io.bluetape4k.hibernate.cache.lettuce.model.CompositePerson
import io.bluetape4k.hibernate.cache.lettuce.model.CompositePersonId
import io.bluetape4k.hibernate.cache.lettuce.model.NaturalUser
import io.bluetape4k.hibernate.cache.lettuce.model.Person
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.hibernate.cache.spi.RegionFactory
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HibernateAdvancedKeyCacheTest: AbstractHibernateNearCacheTest() {

    @BeforeEach
    fun clearCacheAndData() {
        sessionFactory.cache.evictAllRegions()
        sessionFactory.statistics.clear()
        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            session.createMutationQuery("DELETE FROM NaturalUser").executeUpdate()
            session.createMutationQuery("DELETE FROM CompositePerson").executeUpdate()
            session.transaction.commit()
        }
    }

    @Test
    fun `composite id entity는 2nd level cache를 사용하고 Redis key는 문자열이다`() {
        val id = CompositePersonId(companyCode = "ACME", employeeNo = 1001L)

        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            session.persist(
                CompositePerson().apply {
                    this.id = id
                    name = "Composite Alice"
                }
            )
            session.transaction.commit()
        }

        sessionFactory.statistics.clear()

        repeat(2) {
            sessionFactory.openSession().use { session ->
                session.beginTransaction()
                session.find(CompositePerson::class.java, id).shouldNotBeNull()
                session.transaction.commit()
            }
        }

        val regionFactory = (sessionFactory as SessionFactoryImplementor).serviceRegistry
            .getService(org.hibernate.cache.spi.RegionFactory::class.java) as LettuceNearCacheRegionFactory
        val regionName = regionFactory.getCaches().keys.first { it.contains("CompositePerson") }
        val redisKeys = redisKeys("$regionName:*")

        redisKeys.size shouldBeGreaterThan 0
        redisKeys.any { it.contains("[ACME, 1001]") }.shouldBeTrue()
    }

    @Test
    fun `natural-id cache는 hit를 만들고 Redis key는 NaturalId 문자열 표현을 사용한다`() {
        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            session.persist(
                NaturalUser().apply {
                    email = "natural@example.com"
                    displayName = "Natural User"
                }
            )
            session.transaction.commit()
        }

        sessionFactory.statistics.clear()

        repeat(2) {
            sessionFactory.openSession().use { session ->
                session.beginTransaction()
                session.byNaturalId(NaturalUser::class.java)
                    .using("email", "natural@example.com")
                    .load()
                    .shouldNotBeNull()
                session.transaction.commit()
            }
        }

        sessionFactory.statistics.naturalIdCachePutCount shouldBeGreaterThan 0L
        sessionFactory.statistics.naturalIdCacheHitCount shouldBeGreaterThan 0L

        val regionFactory = (sessionFactory as SessionFactoryImplementor).serviceRegistry
            .getService(org.hibernate.cache.spi.RegionFactory::class.java) as LettuceNearCacheRegionFactory
        val regionName =
            regionFactory.getCaches().keys.first { it.contains("##NaturalId") || it.contains("NaturalUser") }
        val redisKeys = redisKeys("$regionName:*")

        redisKeys.any { key -> key.contains("##NaturalId[") }.shouldBeTrue()
        redisKeys.any { key -> key.contains("natural@example.com") }.shouldBeTrue()
    }

    @Test
    fun `update timestamps cache도 문자열 key를 사용한다`() {
        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            session.persist(
                Person().apply {
                    name = "Timestamp User"
                    age = 33
                }
            )
            session.transaction.commit()
        }

        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            session.createSelectionQuery(
                "select p from Person p where p.age >= :age",
                Person::class.java,
            )
                .setParameter("age", 30)
                .setCacheable(true)
                .list()
                .size shouldBeEqualTo 1
            session.transaction.commit()
        }

        sessionFactory.statistics.updateTimestampsCachePutCount shouldBeGreaterThan 0L

        val redisKeys = redisKeys("${RegionFactory.DEFAULT_UPDATE_TIMESTAMPS_REGION_UNQUALIFIED_NAME}:*")

        redisKeys.shouldNotBeNull()
        redisKeys.any { key -> key.contains("persons") }.shouldBeTrue()
    }
}
