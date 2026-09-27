package io.bluetape4k.hibernate.cache.lettuce

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.hibernate.cache.lettuce.model.Person
import io.bluetape4k.hibernate.cache.lettuce.model.VersionedItem
import io.bluetape4k.hibernate.findAs
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * 트랜잭션 롤백 후 캐시 상태 검증 테스트.
 */
class HibernateTransactionRollbackTest: AbstractHibernateNearCacheTest() {

    companion object: KLogging()

    @BeforeEach
    fun reset() {
        sessionFactory.cache.evictAllRegions()
        sessionFactory.statistics.clear()
    }

    @Test
    fun `insert 롤백 후 엔티티가 캐시에 존재하지 않는다`() {
        var personId: Long? = null

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val p = Person().apply { name = "Rollback Insert"; age = 30 }
            s.persist(p)
            personId = p.id.shouldNotBeNull()
            s.transaction.rollback()
        }

        sessionFactory.cache.containsEntity(Person::class.java, personId).shouldBeFalse()
    }

    @Test
    fun `update 롤백 후 원래 값을 조회한다`() {
        val personId =
            sessionFactory.openSession().use { s ->
                s.beginTransaction()
                val p = Person().apply {
                    name = "Original"
                    age = 25
                }
                s.persist(p)
                s.transaction.commit()
                p.id.shouldNotBeNull()
            }

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            s.findAs<Person>(personId).shouldNotBeNull()
            s.transaction.commit()
        }

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val p = s.findAs<Person>(personId)!!
            p.name = "Modified But Rolled Back"
            s.flush()
            s.transaction.rollback()
        }


        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val loaded = s.findAs<Person>(personId).shouldNotBeNull()
            log.debug { "Person: $loaded" }
            s.transaction.commit()
            loaded.name shouldBeEqualTo "Original"
        }
    }

    @Test
    fun `delete 롤백 후 엔티티를 다시 조회할 수 있다`() {
        val personId =
            sessionFactory.openSession().use { s ->
                s.beginTransaction()
                val p = Person().apply { name = "Delete Rollback"; age = 20 }
                s.persist(p)
                s.transaction.commit()
                p.id.shouldNotBeNull()
            }

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            s.findAs<Person>(personId).shouldNotBeNull()
            s.transaction.commit()
        }

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val p = s.findAs<Person>(personId).shouldNotBeNull()
            log.debug { "Person: $p" }
            s.remove(p)
            s.flush()
            s.transaction.rollback()
        }

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val loaded = s.findAs<Person>(personId).shouldNotBeNull()
            log.debug { "Person: $loaded" }
            s.transaction.commit()
            loaded.name shouldBeEqualTo "Delete Rollback"
        }
    }

    @Test
    fun `READ_WRITE 전략 update 롤백 후 원래 값 반환`() {
        val itemId =
            sessionFactory.openSession().use { s ->
                s.beginTransaction()
                val item = VersionedItem().apply { name = "SafeItem"; price = 100 }
                s.persist(item)
                s.transaction.commit()
                item.id.shouldNotBeNull()
            }

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            s.findAs<VersionedItem>(itemId).shouldNotBeNull()
            s.transaction.commit()
        }

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val item = s.findAs<VersionedItem>(itemId).shouldNotBeNull()
            log.debug { "VersionedItem: $item" }
            item.price = 9999
            s.flush()
            s.transaction.rollback()
        }


        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val item = s.findAs<VersionedItem>(itemId).shouldNotBeNull()
            log.debug { "VersionedItem: $item" }
            item.price shouldBeEqualTo 100
            s.transaction.commit()
        }
    }
}
