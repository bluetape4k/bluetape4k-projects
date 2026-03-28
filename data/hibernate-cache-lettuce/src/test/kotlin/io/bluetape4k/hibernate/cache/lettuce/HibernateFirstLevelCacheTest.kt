package io.bluetape4k.hibernate.cache.lettuce

import io.bluetape4k.hibernate.cache.lettuce.model.Person
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBe
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * 1st Level Cache (Session 스코프) 관리 테스트.
 */
class HibernateFirstLevelCacheTest : AbstractHibernateNearCacheTest() {
    @BeforeEach
    fun reset() {
        sessionFactory.cache.evictAllRegions()
        sessionFactory.statistics.clear()
    }

    @Test
    fun `session 내에서 조회한 엔티티는 session contains가 true`() {
        val personId =
            sessionFactory.openSession().use { s ->
                s.beginTransaction()
                val p =
                    Person().apply {
                        name = "Session Alice"
                        age = 30
                    }
                s.persist(p)
                s.transaction.commit()
                p.id!!
            }

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val loaded = s.find(Person::class.java, personId).shouldNotBeNull()
            s.contains(loaded).shouldBeTrue()
            s.transaction.commit()
        }
    }

    @Test
    fun `session detach 후 동일 ID 재조회 시 새 인스턴스를 반환한다`() {
        val personId =
            sessionFactory.openSession().use { s ->
                s.beginTransaction()
                val p =
                    Person().apply {
                        name = "Detach Bob"
                        age = 35
                    }
                s.persist(p)
                s.transaction.commit()
                p.id!!
            }

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val loaded1 = s.find(Person::class.java, personId).shouldNotBeNull()
            s.detach(loaded1)
            s.contains(loaded1).shouldBeFalse()

            val loaded2 = s.find(Person::class.java, personId).shouldNotBeNull()
            loaded2 shouldNotBe loaded1
            s.transaction.commit()
        }
    }

    @Test
    fun `session clear 후 재조회 시 2nd level cache에서 로드된다`() {
        val personId =
            sessionFactory.openSession().use { s ->
                s.beginTransaction()
                val p =
                    Person().apply {
                        name = "Clear Charlie"
                        age = 28
                    }
                s.persist(p)
                s.transaction.commit()
                p.id!!
            }

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            s.find(Person::class.java, personId)
            s.transaction.commit()
        }
        sessionFactory.statistics.clear()

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val loaded1 = s.find(Person::class.java, personId).shouldNotBeNull()
            s.clear()
            s.contains(loaded1).shouldBeFalse()

            val loaded2 = s.find(Person::class.java, personId).shouldNotBeNull()
            loaded2 shouldNotBe loaded1
            s.transaction.commit()
        }

        sessionFactory.statistics.secondLevelCacheHitCount shouldBeGreaterThan 0L
    }

    @Test
    fun `session evict 후 해당 엔티티만 1st level cache에서 제거된다`() {
        val person1Id =
            sessionFactory.openSession().use { s ->
                s.beginTransaction()
                val p =
                    Person().apply {
                        name = "Evict P1"
                        age = 20
                    }
                s.persist(p)
                s.transaction.commit()
                p.id!!
            }
        val person2Id =
            sessionFactory.openSession().use { s ->
                s.beginTransaction()
                val p =
                    Person().apply {
                        name = "Keep P2"
                        age = 25
                    }
                s.persist(p)
                s.transaction.commit()
                p.id!!
            }

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val p1 = s.find(Person::class.java, person1Id).shouldNotBeNull()
            val p2 = s.find(Person::class.java, person2Id).shouldNotBeNull()

            s.evict(p1)

            s.contains(p1).shouldBeFalse()
            s.contains(p2).shouldBeTrue()
            s.transaction.commit()
        }
    }

    @Test
    fun `2nd level cache evict 후 DB에서 재로드된다`() {
        val personId =
            sessionFactory.openSession().use { s ->
                s.beginTransaction()
                val p =
                    Person().apply {
                        name = "Full Evict"
                        age = 33
                    }
                s.persist(p)
                s.transaction.commit()
                p.id!!
            }

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            s.find(Person::class.java, personId)
            s.transaction.commit()
        }

        sessionFactory.cache.evictEntityData(Person::class.java, personId)
        sessionFactory.statistics.clear()

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val loaded = s.find(Person::class.java, personId).shouldNotBeNull()
            loaded.name shouldBeEqualTo "Full Evict"
            s.transaction.commit()
        }

        sessionFactory.statistics.secondLevelCacheMissCount shouldBeGreaterThan 0L
    }
}
