package io.bluetape4k.hibernate.cache.lettuce

import io.bluetape4k.hibernate.cache.lettuce.model.Person
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * 동시 쓰기 충돌 및 NONSTRICT_READ_WRITE 최종 일관성 테스트.
 */
class HibernateConcurrentWriteTest : AbstractHibernateNearCacheTest() {
    @BeforeEach
    fun reset() {
        sessionFactory.cache.evictAllRegions()
        sessionFactory.statistics.clear()
    }

    @Test
    fun `다수 쓰레드가 동시에 서로 다른 엔티티를 persist해도 캐시가 누적된다`() {
        val count = AtomicInteger(0)
        val persistedIds = Collections.synchronizedList(mutableListOf<Long>())

        MultithreadingTester()
            .workers(8)
            .rounds(5)
            .add {
                sessionFactory.openSession().use { s ->
                    s.beginTransaction()
                    val idx = count.incrementAndGet()
                    val p =
                        Person().apply {
                            name = "Concurrent$idx"
                            age = 20 + (idx % 50)
                        }
                    s.persist(p)
                    s.transaction.commit()
                    persistedIds += p.id!!
                }
            }.run()

        // NONSTRICT_READ_WRITE: cache is populated on load, not on insert.
        // Read each entity in a new session to warm the 2nd-level cache.
        sessionFactory.statistics.clear()
        persistedIds.forEach { id ->
            sessionFactory.openSession().use { s ->
                s.beginTransaction()
                s.find(Person::class.java, id)
                s.transaction.commit()
            }
        }

        sessionFactory.statistics.secondLevelCachePutCount shouldBeGreaterThan 0L
    }

    @Test
    fun `읽기-쓰기 혼합 워크로드에서 캐시 hit이 발생한다`() {
        val ids =
            (1..5).map {
                sessionFactory.openSession().use { s ->
                    s.beginTransaction()
                    val p =
                        Person().apply {
                            name = "RW$it"
                            age = 20 + it
                        }
                    s.persist(p)
                    s.transaction.commit()
                    p.id!!
                }
            }

        ids.forEach { id ->
            sessionFactory.openSession().use { s ->
                s.beginTransaction()
                s.find(Person::class.java, id)
                s.transaction.commit()
            }
        }
        sessionFactory.statistics.clear()

        val writeCount = AtomicInteger(0)
        MultithreadingTester()
            .workers(10)
            .rounds(3)
            .add {
                val id = ids.random()
                val isWrite = writeCount.incrementAndGet() % 5 == 0

                if (isWrite) {
                    sessionFactory.openSession().use { s ->
                        s.beginTransaction()
                        val p = s.find(Person::class.java, id)
                        p?.age = (20..60).random()
                        s.transaction.commit()
                    }
                } else {
                    sessionFactory.openSession().use { s ->
                        s.beginTransaction()
                        s.find(Person::class.java, id).shouldNotBeNull()
                        s.transaction.commit()
                    }
                }
            }.run()

        sessionFactory.statistics.secondLevelCacheHitCount shouldBeGreaterThan 0L
    }

    @Test
    fun `다수 쓰레드가 동시에 쿼리 캐시를 조회해도 일관성이 유지된다`() {
        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            (1..10).forEach { i ->
                s.persist(
                    Person().apply {
                        name = "QC$i"
                        age = 20 + i
                    }
                )
            }
            s.transaction.commit()
        }

        val hql = "select p from Person p where p.age > :age order by p.id"
        sessionFactory.statistics.clear()

        val resultCounts = Collections.synchronizedList(mutableListOf<Int>())
        MultithreadingTester()
            .workers(6)
            .rounds(5)
            .add {
                sessionFactory.openSession().use { s ->
                    s.beginTransaction()
                    val results =
                        s
                            .createSelectionQuery(hql, Person::class.java)
                            .setParameter("age", 20)
                            .setCacheable(true)
                            .list()
                    resultCounts += results.size
                    s.transaction.commit()
                }
            }.run()

        resultCounts.distinct().size shouldBeEqualTo 1
        sessionFactory.statistics.queryCacheHitCount shouldBeGreaterThan 0L
    }
}
