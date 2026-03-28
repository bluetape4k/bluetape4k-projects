package io.bluetape4k.hibernate.cache.lettuce

import io.bluetape4k.hibernate.cache.lettuce.model.Department
import io.bluetape4k.hibernate.cache.lettuce.model.Employee
import io.bluetape4k.hibernate.cache.lettuce.model.Person
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Cache containsEntity / containsCollection / 명시적 Region eviction 테스트.
 */
class HibernateCacheContainmentTest : AbstractHibernateNearCacheTest() {
    @BeforeEach
    fun resetAll() {
        sessionFactory.cache.evictAllRegions()
        sessionFactory.statistics.clear()
    }

    @Test
    fun `persist 및 로드 후 containsEntity가 true`() {
        val personId =
            sessionFactory.openSession().use { s ->
                s.beginTransaction()
                val p =
                    Person().apply {
                        name = "Contain Alice"
                        age = 30
                    }
                s.persist(p)
                s.transaction.commit()
                p.id!!
            }

        // 새 세션에서 읽어 2LC에 적재
        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            s.find(Person::class.java, personId)
            s.transaction.commit()
        }

        sessionFactory.cache.containsEntity(Person::class.java, personId).shouldBeTrue()

        sessionFactory.cache.evictEntityData(Person::class.java, personId)
        sessionFactory.cache.containsEntity(Person::class.java, personId).shouldBeFalse()
    }

    @Test
    fun `존재하지 않는 엔티티는 containsEntity가 false`() {
        sessionFactory.cache.containsEntity(Person::class.java, Long.MAX_VALUE).shouldBeFalse()
    }

    @Test
    fun `컬렉션 로드 후 containsCollection이 true, evict 후 false`() {
        val deptId =
            sessionFactory.openSession().use { s ->
                s.beginTransaction()
                val dept = Department().apply { name = "QA" }
                dept.addEmployee(Employee().apply { name = "QA1" })
                s.persist(dept)
                s.transaction.commit()
                dept.id!!
            }

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val d = s.find(Department::class.java, deptId)!!
            d.employees.size
            s.transaction.commit()
        }

        sessionFactory.cache
            .containsCollection(
                "${Department::class.java.name}.employees",
                deptId
            ).shouldBeTrue()

        sessionFactory.cache.evictCollectionData(
            "${Department::class.java.name}.employees",
            deptId
        )
        sessionFactory.cache
            .containsCollection(
                "${Department::class.java.name}.employees",
                deptId
            ).shouldBeFalse()
    }

    @Test
    fun `evictEntityData(Class) 호출 시 해당 엔티티 전체 Region이 비워진다`() {
        val ids =
            (1..3).map {
                sessionFactory.openSession().use { s ->
                    s.beginTransaction()
                    val p =
                        Person().apply {
                            name = "Bulk$it"
                            age = 20 + it
                        }
                    s.persist(p)
                    s.transaction.commit()
                    p.id!!
                }
            }

        // 새 세션에서 읽어 2LC에 적재
        ids.forEach { id ->
            sessionFactory.openSession().use { s ->
                s.beginTransaction()
                s.find(Person::class.java, id)
                s.transaction.commit()
            }
        }

        ids.forEach { id ->
            sessionFactory.cache.containsEntity(Person::class.java, id).shouldBeTrue()
        }

        sessionFactory.cache.evictEntityData(Person::class.java)

        ids.forEach { id ->
            sessionFactory.cache.containsEntity(Person::class.java, id).shouldBeFalse()
        }
    }

    @Test
    fun `evictAllRegions 호출 시 모든 엔티티가 캐시에서 제거된다`() {
        val personId =
            sessionFactory.openSession().use { s ->
                s.beginTransaction()
                val p =
                    Person().apply {
                        name = "EvictAll"
                        age = 40
                    }
                s.persist(p)
                s.transaction.commit()
                p.id!!
            }
        val deptId =
            sessionFactory.openSession().use { s ->
                s.beginTransaction()
                val d = Department().apply { name = "ToEvict" }
                s.persist(d)
                s.transaction.commit()
                d.id!!
            }

        sessionFactory.cache.evictAllRegions()

        sessionFactory.cache.containsEntity(Person::class.java, personId).shouldBeFalse()
        sessionFactory.cache.containsEntity(Department::class.java, deptId).shouldBeFalse()
    }

    @Test
    fun `특정 쿼리 Region evict가 엔티티 Region에 영향을 주지 않는다`() {
        val personId =
            sessionFactory.openSession().use { s ->
                s.beginTransaction()
                val p =
                    Person().apply {
                        name = "RegionKeep"
                        age = 30
                    }
                s.persist(p)
                s.transaction.commit()
                p.id!!
            }

        // 새 세션에서 읽어 2LC에 적재
        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            s.find(Person::class.java, personId)
            s.transaction.commit()
        }

        sessionFactory.cache.evictQueryRegions()
        sessionFactory.cache.containsEntity(Person::class.java, personId).shouldBeTrue()
    }
}
