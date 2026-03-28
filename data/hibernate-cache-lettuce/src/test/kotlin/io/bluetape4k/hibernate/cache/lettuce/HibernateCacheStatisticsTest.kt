package io.bluetape4k.hibernate.cache.lettuce

import io.bluetape4k.hibernate.cache.lettuce.model.Department
import io.bluetape4k.hibernate.cache.lettuce.model.Employee
import io.bluetape4k.hibernate.cache.lettuce.model.Person
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Region별 CacheRegionStatistics 검증 테스트.
 */
class HibernateCacheStatisticsTest: AbstractHibernateNearCacheTest() {
    @BeforeEach
    fun resetAll() {
        sessionFactory.cache.evictAllRegions()
        sessionFactory.statistics.clear()
    }

    @Test
    fun `엔티티 Region 통계 - persist 시 put, 재조회 시 hit 누적`() {
        val personRegion = Person::class.java.name

        val personId =
            sessionFactory.openSession().use { s ->
                s.beginTransaction()
                val p =
                    Person().apply {
                        name = "Stats Alice"
                        age = 30
                    }
                s.persist(p)
                s.transaction.commit()
                p.id!!
            }
        sessionFactory.statistics.clear()

        repeat(3) {
            sessionFactory.openSession().use { s ->
                s.beginTransaction()
                s.find(Person::class.java, personId).shouldNotBeNull()
                s.transaction.commit()
            }
        }

        val regionStats = sessionFactory.statistics.getDomainDataRegionStatistics(personRegion)
        regionStats.shouldNotBeNull()
        regionStats.hitCount shouldBeGreaterThan 0L
    }

    @Test
    fun `컬렉션 Region 통계 - OneToMany 컬렉션 로드 시 put 후 hit`() {
        val collectionRegion = "${Department::class.java.name}.employees"

        val deptId =
            sessionFactory.openSession().use { s ->
                s.beginTransaction()
                val dept = Department().apply { name = "Engineering" }
                val emp1 = Employee().apply { name = "E1" }
                val emp2 = Employee().apply { name = "E2" }
                dept.addEmployee(emp1)
                dept.addEmployee(emp2)
                s.persist(dept)
                s.transaction.commit()
                dept.id!!
            }
        sessionFactory.statistics.clear()

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val d = s.find(Department::class.java, deptId)!!
            d.employees.size
            s.transaction.commit()
        }
        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val d = s.find(Department::class.java, deptId)!!
            d.employees.size
            s.transaction.commit()
        }

        val collectionStats = sessionFactory.statistics.getDomainDataRegionStatistics(collectionRegion)
        collectionStats.shouldNotBeNull()
        collectionStats.hitCount shouldBeGreaterThan 0L
    }

    @Test
    fun `쿼리 캐시 통계 - 동일 쿼리 반복 시 queryCacheHitCount 증가`() {
        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            s.persist(
                Person().apply {
                    name = "Q1"
                    age = 21
                }
            )
            s.persist(
                Person().apply {
                    name = "Q2"
                    age = 22
                }
            )
            s.transaction.commit()
        }
        sessionFactory.statistics.clear()

        val hql = "select p from Person p where p.age > :age order by p.id"
        repeat(3) {
            sessionFactory.openSession().use { s ->
                s.beginTransaction()
                s
                    .createSelectionQuery(hql, Person::class.java)
                    .setParameter("age", 20)
                    .setCacheable(true)
                    .list()
                s.transaction.commit()
            }
        }

        sessionFactory.statistics.queryCacheHitCount shouldBeGreaterThan 0L
        sessionFactory.statistics.queryCachePutCount shouldBeGreaterThan 0L
    }

    @Test
    fun `전체 통계 - secondLevelCachePutCount와 hit 비율 검증`() {
        val ids =
            sessionFactory.openSession().use { s ->
                s.beginTransaction()
                val people =
                    (1..5).map { i ->
                        Person().apply {
                            name = "Bulk$i"
                            age = 20 + i
                        }
                    }
                people.forEach { s.persist(it) }
                s.transaction.commit()
                people.map { it.id!! }
            }
        sessionFactory.statistics.clear()

        ids.forEach { id ->
            sessionFactory.openSession().use { s ->
                s.beginTransaction()
                s.find(Person::class.java, id)
                s.transaction.commit()
            }
        }
        val putCount = sessionFactory.statistics.secondLevelCachePutCount
        putCount shouldBeGreaterThan 0L

        ids.forEach { id ->
            sessionFactory.openSession().use { s ->
                s.beginTransaction()
                s.find(Person::class.java, id)
                s.transaction.commit()
            }
        }
        sessionFactory.statistics.secondLevelCacheHitCount shouldBeGreaterThan 0L
    }
}
