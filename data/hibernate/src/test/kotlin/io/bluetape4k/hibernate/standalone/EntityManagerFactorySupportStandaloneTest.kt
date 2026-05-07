package io.bluetape4k.hibernate.standalone

import io.bluetape4k.hibernate.withNewEntityManager
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import io.bluetape4k.assertions.assertFailsWith

class EntityManagerFactorySupportStandaloneTest : AbstractStandaloneHibernateTest() {

    override fun entityClasses() = listOf(StandaloneEntity::class.java)

    @BeforeEach
    fun clearData() {
        inTransaction {
            createQuery("DELETE FROM StandaloneEntity").executeUpdate()
        }
    }

    @Test
    fun `withNewEntityManager는 트랜잭션 내에서 작업을 수행한다`() {
        val entity = emf.withNewEntityManager { em ->
            val e = StandaloneEntity("emf-test")
            em.persist(e)
            e
        }
        entity.id.shouldNotBeNull()

        readOnly {
            val count = createQuery("SELECT COUNT(e) FROM StandaloneEntity e", Long::class.java)
                .singleResult
            count shouldBeEqualTo 1L
        }
    }

    @Test
    fun `withNewEntityManager는 예외 발생 시 롤백한다`() {
        assertFailsWith<RuntimeException> {
            emf.withNewEntityManager { em ->
                em.persist(StandaloneEntity("rollback-emf"))
                throw RuntimeException("rollback trigger")
            }
        }

        readOnly {
            val count = createQuery("SELECT COUNT(e) FROM StandaloneEntity e", Long::class.java)
                .singleResult
            count shouldBeEqualTo 0L
        }
    }
}
