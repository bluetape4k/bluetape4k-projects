package io.bluetape4k.hibernate.cache.lettuce

import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.hibernate.cache.lettuce.model.Person
import io.bluetape4k.hibernate.createQueryAs
import io.bluetape4k.hibernate.findAs
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Query Cache 고급 테스트.
 */
class HibernateQueryCacheAdvancedTest: AbstractHibernateNearCacheTest() {

    companion object: KLogging() {
        const val PERSON_QUERY_REGION = "io.bluetape4k.person.queries"
    }

    @BeforeEach
    fun reset() {
        sessionFactory.cache.evictAllRegions()
        sessionFactory.statistics.clear()
        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            s.createMutationQuery("DELETE FROM Person").executeUpdate()
            s.transaction.commit()
        }
    }

    @Test
    fun `명명 Query Region을 사용한 캐시 쿼리가 hit된다`() {
        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            s.persist(
                Person().apply {
                    name = "Named1"
                    age = 25
                }
            )
            s.persist(
                Person().apply {
                    name = "Named2"
                    age = 35
                }
            )
            s.transaction.commit()
        }
        sessionFactory.statistics.clear()

        val hql = "select p from Person p where p.age > :minAge order by p.id"

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            s.createQueryAs<Person>(hql)
                .setParameter("minAge", 20)
                .setCacheable(true)
                .setCacheRegion(PERSON_QUERY_REGION)
                .list() shouldHaveSize 2
            s.transaction.commit()
        }

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            s.createQueryAs<Person>(hql)
                .setParameter("minAge", 20)
                .setCacheable(true)
                .setCacheRegion(PERSON_QUERY_REGION)
                .list() shouldHaveSize 2
            s.transaction.commit()
        }

        sessionFactory.statistics.queryCacheHitCount shouldBeGreaterThan 0L
    }

    @Test
    fun `파라미터 값이 다르면 별도 캐시 키로 저장된다`() {
        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            s.persist(Person().apply { name = "Young"; age = 20 })
            s.persist(Person().apply { name = "Senior"; age = 60 })
            s.transaction.commit()
        }
        sessionFactory.statistics.clear()

        val hql = "select p from Person p where p.age < :maxAge order by p.id"

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            s.createQueryAs<Person>(hql)
                .setParameter("maxAge", 30)
                .setCacheable(true)
                .list()
                .size shouldBeEqualTo 1
            s.transaction.commit()
        }

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            s.createQueryAs<Person>(hql)
                .setParameter("maxAge", 70)
                .setCacheable(true)
                .list() shouldHaveSize 2
            s.transaction.commit()
        }

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            s.createQueryAs<Person>(hql)
                .setParameter("maxAge", 30)
                .setCacheable(true)
                .list() shouldHaveSize 1
            s.transaction.commit()
        }

        sessionFactory.statistics.queryCacheHitCount shouldBeGreaterThan 0L
    }

    @Test
    fun `특정 Query Region evict 후 해당 쿼리만 다시 DB를 조회한다`() {
        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            s.persist(Person().apply { name = "RegionEvict"; age = 40 })
            s.transaction.commit()
        }

        val hql = "select p from Person p order by p.id"

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            s.createQueryAs<Person>(hql)
                .setCacheable(true)
                .setCacheRegion(PERSON_QUERY_REGION)
                .list().shouldNotBeEmpty()
            s.transaction.commit()
        }

        sessionFactory.cache.evictQueryRegion(PERSON_QUERY_REGION)
        sessionFactory.statistics.clear()

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            s.createQueryAs<Person>(hql)
                .setCacheable(true)
                .setCacheRegion(PERSON_QUERY_REGION)
                .list().shouldNotBeEmpty()
            s.transaction.commit()
        }

        sessionFactory.statistics.queryCacheMissCount shouldBeGreaterThan 0L
    }

    @Test
    fun `결과가 없는 쿼리도 캐시에 저장되어 두 번째 호출이 hit된다`() {
        sessionFactory.statistics.clear()

        val hql = "select p from Person p where p.age > :age"

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            s.createQueryAs<Person>(hql)
                .setParameter("age", 9999)
                .setCacheable(true)
                .list().shouldBeEmpty()
            s.transaction.commit()
        }

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            s.createQueryAs<Person>(hql)
                .setParameter("age", 9999)
                .setCacheable(true)
                .list().shouldBeEmpty()
            s.transaction.commit()
        }

        sessionFactory.statistics.queryCacheHitCount shouldBeGreaterThan 0L
    }

    @Test
    fun `엔티티 변경 후 기본 Query Region의 캐시가 무효화된다`() {
        val personId = sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val p = Person().apply { name = "Invalidate"; age = 30 }
            s.persist(p)
            s.transaction.commit()
            p.id.shouldNotBeNull()
        }

        val hql = "select p from Person p where p.age >= :age order by p.id"

        repeat(2) {
            sessionFactory.openSession().use { s ->
                s.beginTransaction()
                s.createQueryAs<Person>(hql)
                    .setParameter("age", 25)
                    .setCacheable(true)
                    .list().shouldNotBeEmpty()
                s.transaction.commit()
            }
        }

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val p = s.findAs<Person>(personId).shouldNotBeNull()
            p.age = 10
            s.transaction.commit()
        }
        sessionFactory.statistics.clear()

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            s.createQueryAs<Person>(hql)
                .setParameter("age", 25)
                .setCacheable(true)
                .list().shouldBeEmpty()
            s.transaction.commit()
        }

        sessionFactory.statistics.queryCacheMissCount shouldBeGreaterThan 0L
    }
}
