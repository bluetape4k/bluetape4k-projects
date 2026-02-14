package io.bluetape4k.hibernate

import io.bluetape4k.hibernate.mapping.simple.SimpleEntity
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class EntityManagerFactorySupportTest: AbstractHibernateTest() {

    @Test
    fun `withNewEntityManager 는 정상 수행 시 commit 한다`() {
        val name = "emf-commit"
        cleanup(name)

        emf.withNewEntityManager { em ->
            em.persist(SimpleEntity(name))
        }
        clear()

        val count = em.createQuery(
            "select count(e) from simple_entity e where e.name = :name",
            Long::class.javaObjectType
        )
            .setParameter("name", name)
            .singleResult

        count.toLong() shouldBeEqualTo 1L
        cleanup(name)
    }

    @Test
    fun `withNewEntityManager 는 예외 발생 시 rollback 하고 예외를 전파한다`() {
        val name = "emf-rollback"
        cleanup(name)

        assertFailsWith<IllegalStateException> {
            emf.withNewEntityManager { em ->
                em.persist(SimpleEntity(name))
                throw IllegalStateException("boom")
            }
        }
        clear()

        val count = em.createQuery(
            "select count(e) from simple_entity e where e.name = :name",
            Long::class.javaObjectType
        )
            .setParameter("name", name)
            .singleResult

        count.toLong() shouldBeEqualTo 0L
        cleanup(name)
    }

    private fun cleanup(name: String) {
        emf.withNewEntityManager { em ->
            em.createQuery("delete from simple_entity e where e.name = :name")
                .setParameter("name", name)
                .executeUpdate()
        }
    }
}
