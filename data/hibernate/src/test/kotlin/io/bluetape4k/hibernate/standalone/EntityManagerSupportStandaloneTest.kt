package io.bluetape4k.hibernate.standalone

import io.bluetape4k.hibernate.asSessionImpl
import io.bluetape4k.hibernate.countAll
import io.bluetape4k.hibernate.createQueryAs
import io.bluetape4k.hibernate.currentSession
import io.bluetape4k.hibernate.currentSessionImpl
import io.bluetape4k.hibernate.delete
import io.bluetape4k.hibernate.deleteAll
import io.bluetape4k.hibernate.deleteById
import io.bluetape4k.hibernate.exists
import io.bluetape4k.hibernate.findAll
import io.bluetape4k.hibernate.findAs
import io.bluetape4k.hibernate.findOne
import io.bluetape4k.hibernate.getReference
import io.bluetape4k.hibernate.isLoaded
import io.bluetape4k.hibernate.newQuery
import io.bluetape4k.hibernate.save
import io.bluetape4k.hibernate.sessionFactory
import io.bluetape4k.hibernate.setPaging
import jakarta.persistence.TypedQuery
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class EntityManagerSupportStandaloneTest : AbstractStandaloneHibernateTest() {

    override fun entityClasses() = listOf(StandaloneEntity::class.java)

    @BeforeEach
    fun clearData() {
        inTransaction { deleteAll<StandaloneEntity>() }
    }

    @Test
    fun `currentSession은 Hibernate Session을 반환한다`() {
        inTransaction {
            val session = currentSession()
            session.shouldNotBeNull()
        }
    }

    @Test
    fun `sessionFactory는 SessionFactory를 반환한다`() {
        inTransaction {
            val sf = sessionFactory()
            sf.shouldNotBeNull()
        }
    }

    @Test
    fun `newQuery는 TypedQuery를 반환한다`() {
        inTransaction {
            val query = newQuery<StandaloneEntity>()
            query.shouldNotBeNull()
        }
    }

    @Test
    fun `save는 새 엔티티를 영속화한다`() {
        inTransaction {
            val entity = StandaloneEntity("save-test")
            save(entity)
        }
        readOnly {
            countAll<StandaloneEntity>() shouldBeEqualTo 1L
        }
    }

    @Test
    fun `save는 detached 엔티티를 merge한다`() {
        val entity = StandaloneEntity("merge-test")
        inTransaction { save(entity) }

        val id = entity.id!!
        inTransaction {
            // detached 상태에서 save → merge
            entity.name = "merged"
            save(entity)
        }
        readOnly {
            val loaded = findAs<StandaloneEntity>(id)!!
            loaded.name shouldBeEqualTo "merged"
        }
    }

    @Test
    fun `findAs는 id로 엔티티를 조회한다`() {
        val entity = StandaloneEntity("find-test")
        inTransaction { save(entity) }

        readOnly {
            val loaded = findAs<StandaloneEntity>(entity.id!!)
            loaded.shouldNotBeNull()
            loaded.name shouldBeEqualTo "find-test"
        }
    }

    @Test
    fun `findAs는 없는 id에 대해 null을 반환한다`() {
        readOnly {
            val result = findAs<StandaloneEntity>(Long.MAX_VALUE)
            result.shouldBeNull()
        }
    }

    @Test
    fun `findOne은 findAs와 동일하게 동작한다`() {
        val entity = StandaloneEntity("findone-test")
        inTransaction { save(entity) }

        readOnly {
            val loaded = findOne<StandaloneEntity>(entity.id!!)
            loaded.shouldNotBeNull()
        }
    }

    @Test
    fun `exists는 엔티티 존재 여부를 반환한다`() {
        val entity = StandaloneEntity("exists-test")
        inTransaction { save(entity) }

        readOnly {
            exists<StandaloneEntity>(entity.id!!).shouldBeTrue()
            exists<StandaloneEntity>(Long.MAX_VALUE).shouldBeFalse()
        }
    }

    @Test
    fun `countAll은 엔티티 총 개수를 반환한다`() {
        inTransaction {
            save(StandaloneEntity("count-1"))
            save(StandaloneEntity("count-2"))
            save(StandaloneEntity("count-3"))
        }

        readOnly {
            countAll<StandaloneEntity>() shouldBeEqualTo 3L
        }
    }

    @Test
    fun `deleteAll은 모든 엔티티를 삭제하고 삭제 수를 반환한다`() {
        inTransaction {
            save(StandaloneEntity("del-1"))
            save(StandaloneEntity("del-2"))
        }

        inTransaction {
            deleteAll<StandaloneEntity>() shouldBeEqualTo 2
        }

        readOnly {
            countAll<StandaloneEntity>() shouldBeEqualTo 0L
        }
    }

    @Test
    fun `deleteById는 id로 엔티티를 삭제한다`() {
        val entity = StandaloneEntity("delete-by-id")
        inTransaction { save(entity) }

        inTransaction {
            deleteById<StandaloneEntity>(entity.id!!)
        }

        readOnly {
            countAll<StandaloneEntity>() shouldBeEqualTo 0L
        }
    }

    @Test
    fun `deleteById는 없는 id에 대해 예외를 발생시키지 않는다`() {
        assertDoesNotThrow {
            inTransaction {
                deleteById<StandaloneEntity>(Long.MAX_VALUE)
            }
        }
    }

    @Test
    fun `delete는 persisted 엔티티를 삭제한다`() {
        val entity = StandaloneEntity("delete-test")
        inTransaction { save(entity) }

        inTransaction {
            val loaded = findAs<StandaloneEntity>(entity.id!!)!!
            delete(loaded)
        }

        readOnly {
            countAll<StandaloneEntity>() shouldBeEqualTo 0L
        }
    }

    @Test
    fun `isLoaded는 영속화된 엔티티에 대해 true를 반환한다`() {
        val entity = StandaloneEntity("isloaded-test")
        inTransaction { save(entity) }

        inTransaction {
            val loaded = findAs<StandaloneEntity>(entity.id!!)!!
            isLoaded(loaded).shouldBeTrue()
        }
    }

    @Test
    fun `isLoaded는 null 엔티티에 대해 false를 반환한다`() {
        inTransaction {
            isLoaded(null).shouldBeFalse()
        }
    }

    @Test
    fun `findAll은 모든 엔티티를 반환한다`() {
        inTransaction {
            save(StandaloneEntity("all-1"))
            save(StandaloneEntity("all-2"))
        }

        readOnly {
            val all = findAll(StandaloneEntity::class.java)
            all shouldHaveSize 2
        }
    }

    @Test
    fun `createQueryAs는 TypedQuery를 반환한다`() {
        inTransaction {
            save(StandaloneEntity("query-test"))
        }

        readOnly {
            val query = createQueryAs<Long>("select count(e) from StandaloneEntity e")
            val count = query.singleResult
            count shouldBeEqualTo 1L
        }
    }

    @Test
    fun `currentSessionImpl은 SessionImpl을 반환한다`() {
        inTransaction {
            val sessionImpl = currentSessionImpl()
            sessionImpl.shouldNotBeNull()
        }
    }

    @Test
    fun `asSessionImpl은 SessionImpl을 반환한다`() {
        inTransaction {
            val sessionImpl = asSessionImpl()
            sessionImpl.shouldNotBeNull()
        }
    }

    @Test
    fun `isLoaded는 프로퍼티 이름으로 로드 여부를 반환한다`() {
        val entity = StandaloneEntity("isloaded-prop-test")
        inTransaction { save(entity) }

        inTransaction {
            val loaded = findAs<StandaloneEntity>(entity.id!!)!!
            isLoaded(loaded, "name").shouldBeTrue()
            isLoaded(null, "name").shouldBeFalse()
        }
    }

    @Test
    fun `getReference는 프록시를 반환한다`() {
        val entity = StandaloneEntity("ref-test")
        inTransaction { save(entity) }

        inTransaction {
            val ref = getReference<StandaloneEntity>(entity.id!!)
            ref.shouldNotBeNull()
        }
    }

    @Test
    fun `createQueryAs with KClass는 TypedQuery를 반환한다`() {
        inTransaction { save(StandaloneEntity("kclass-test")) }

        readOnly {
            val query = createQueryAs(
                "select count(e) from StandaloneEntity e",
                Long::class
            )
            query.singleResult shouldBeEqualTo 1L
        }
    }

    @Test
    fun `setPaging은 쿼리에 페이징을 적용한다`() {
        inTransaction {
            save(StandaloneEntity("paging-1"))
            save(StandaloneEntity("paging-2"))
            save(StandaloneEntity("paging-3"))
        }

        readOnly {
            val query = createQueryAs<StandaloneEntity>("SELECT e FROM StandaloneEntity e")
                .setPaging(firstResult = 0, maxResults = 2)
            val results = query.resultList
            results shouldHaveSize 2
        }
    }
}
