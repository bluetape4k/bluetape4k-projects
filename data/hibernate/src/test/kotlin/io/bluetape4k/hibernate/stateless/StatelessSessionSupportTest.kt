package io.bluetape4k.hibernate.stateless

import io.bluetape4k.hibernate.AbstractHibernateTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class StatelessSessionSupportTest: AbstractHibernateTest() {

    @Test
    fun `withStateless 는 정상 수행 시 commit 한다`() {
        val entityName = "support-commit"

        em.withStateless { stateless ->
            stateless.insert(StatelessEntity(entityName))
        }
        clear()

        val count = em.createQuery(
            "select count(e) from StatelessEntity e where e.name = :name",
            Long::class.javaObjectType
        )
            .setParameter("name", entityName)
            .singleResult

        count.toLong() shouldBeEqualTo 1L
    }

    @Test
    fun `withStateless 는 예외 발생 시 rollback 하고 예외를 전파한다`() {
        val entityName = "support-rollback"

        assertFailsWith<IllegalStateException> {
            em.withStateless { stateless ->
                stateless.insert(StatelessEntity(entityName))
                throw IllegalStateException("boom")
            }
        }
        clear()

        val count = em.createQuery(
            "select count(e) from StatelessEntity e where e.name = :name",
            Long::class.javaObjectType
        )
            .setParameter("name", entityName)
            .singleResult

        count.toLong() shouldBeEqualTo 0L
    }
}
