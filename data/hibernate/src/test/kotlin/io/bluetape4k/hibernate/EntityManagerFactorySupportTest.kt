package io.bluetape4k.hibernate

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.hibernate.mapping.simple.SimpleEntity
import org.junit.jupiter.api.Test

class EntityManagerFactorySupportTest: AbstractHibernateTest() {

    @Test
    fun `withNewEntityManager 는 정상 수행 시 commit 한다`() {
        val name = "emf-commit"
        deleteByName(name)

        emf.withNewEntityManager { em ->
            em.persist(SimpleEntity(name))
        }
        clear()

        val count = em
            .createQueryAs<Long>("select count(e) from simple_entity e where e.name = :name")
            .setParameter("name", name)
            .singleResult

        count shouldBeEqualTo 1L
        deleteByName(name)
    }

    @Test
    fun `withNewEntityManager 는 결과 값을 그대로 반환한다`() {
        val name = "emf-return"
        deleteByName(name)

        val returned = emf.withNewEntityManager { em ->
            em.persist(SimpleEntity(name))
            "result-$name"
        }
        returned shouldBeEqualTo "result-$name"
        deleteByName(name)
    }

    @Test
    fun `withNewEntityManager 는 예외 발생 시 rollback 하고 예외를 전파한다`() {
        val name = "emf-rollback"
        deleteByName(name)

        assertFailsWith<IllegalStateException> {
            emf.withNewEntityManager { em ->
                em.persist(SimpleEntity(name))
                throw IllegalStateException("boom")
            }
        }
        clear()

        val count = em
            .createQueryAs<Long>(
                "select count(e) from simple_entity e where e.name = :name",
            )
            .setParameter("name", name)
            .singleResult
            .shouldNotBeNull()

        count shouldBeEqualTo 0L
        deleteByName(name)
    }

    private fun deleteByName(name: String) {
        emf.withNewEntityManager { em ->
            em.createQuery("delete from simple_entity e where e.name = :name")
                .setParameter("name", name)
                .executeUpdate()
        }
    }
}
