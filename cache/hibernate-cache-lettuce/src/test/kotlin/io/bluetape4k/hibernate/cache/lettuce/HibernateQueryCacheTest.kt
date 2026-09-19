package io.bluetape4k.hibernate.cache.lettuce

import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.hibernate.cache.lettuce.model.Person
import io.bluetape4k.hibernate.createQueryAs
import io.bluetape4k.hibernate.findAs
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HibernateQueryCacheTest: AbstractHibernateNearCacheTest() {

    companion object: KLogging()

    @BeforeEach
    fun clearCacheAndData() {
        sessionFactory.cache.evictAllRegions()
        sessionFactory.statistics.clear()
        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            session.createMutationQuery("DELETE FROM Person").executeUpdate()
            session.transaction.commit()
        }
    }

    @Test
    fun `cacheable query를 다시 실행하면 query cache hit가 증가한다`() {
        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            session.persist(Person().apply { name = "Alice"; age = 30 })
            session.persist(Person().apply { name = "Bob"; age = 35 })
            session.transaction.commit()
        }

        sessionFactory.statistics.clear()

        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            session
                .createQueryAs<Person>("select p from Person p where p.age >= :age order by p.id")
                .setParameter("age", 30)
                .setCacheable(true)
                .list() shouldHaveSize 2
            session.transaction.commit()
        }

        val statsAfterFirstQuery = sessionFactory.statistics
        statsAfterFirstQuery.queryCachePutCount shouldBeGreaterThan 0L

        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            session
                .createQueryAs<Person>("select p from Person p where p.age >= :age order by p.id")
                .setParameter("age", 30)
                .setCacheable(true)
                .list() shouldHaveSize 2
            session.transaction.commit()
        }

        sessionFactory.statistics.queryCacheHitCount shouldBeGreaterThan 0L
    }

    @Test
    fun `엔티티 변경 후 cacheable query는 stale 결과를 반환하지 않는다`() {
        val personId = sessionFactory.openSession().use { session ->
            session.beginTransaction()
            val person = Person().apply { name = "Charlie"; age = 30 }
            session.persist(person)
            session.transaction.commit()
            person.id.shouldNotBeNull()
        }

        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            session.createQueryAs<Person>("select p from Person p where p.age >= :age")
                .setParameter("age", 20)
                .setCacheable(true)
                .list() shouldHaveSize 1
            session.transaction.commit()
        }

        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            session.createQueryAs<Person>("select p from Person p where p.age >= :age")
                .setParameter("age", 20)
                .setCacheable(true)
                .list() shouldHaveSize 1
            session.transaction.commit()
        }

        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            val person = session.findAs<Person>(personId).shouldNotBeNull()
            person.age = 10
            session.transaction.commit()
        }

        sessionFactory.statistics.clear()

        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            session
                .createQueryAs<Person>("select p from Person p where p.age >= :age")
                .setParameter("age", 20)
                .setCacheable(true)
                .list().shouldBeEmpty()
            session.transaction.commit()
        }

        sessionFactory.statistics.queryCacheMissCount shouldBeGreaterThan 0L
    }
}
