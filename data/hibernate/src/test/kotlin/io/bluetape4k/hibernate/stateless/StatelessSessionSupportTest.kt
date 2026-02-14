package io.bluetape4k.hibernate.stateless

import io.bluetape4k.hibernate.AbstractHibernateTest
import io.bluetape4k.hibernate.createQueryAs
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.hibernate.LockMode
import org.hibernate.graph.GraphSemantic
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

        val count = em.createQueryAs<Long>("select count(e) from StatelessEntity e where e.name = :name")
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

        val count = em.createQueryAs<Long>("select count(e) from StatelessEntity e where e.name = :name")
            .setParameter("name", entityName)
            .singleResult

        count.toLong() shouldBeEqualTo 0L
    }

    @Test
    fun `getAs 는 EntityGraph 와 GraphSemantic 오버로드를 지원한다`() {
        val entityName = "support-graph"
        val entity = StatelessEntity(entityName).also {
            it.firstname = "first"
            it.lastname = "last"
        }
        em.withStateless { stateless ->
            stateless.insert(entity)
        }
        clear()

        val entityId = em.createQueryAs<Int>("select e.id from StatelessEntity e where e.name = :name")
            .setParameter("name", entityName)
            .singleResult

        em.withStateless { stateless ->
            val graph = stateless.createEntityGraphAs<StatelessEntity>().apply {
                addAttributeNodes("firstname", "lastname")
            }

            val loaded = stateless.getAs(graph, GraphSemantic.LOAD, entityId)
            loaded.shouldNotBeNull()
            loaded.name shouldBeEqualTo entityName

            val loadedWithLock = stateless.getAs(graph, GraphSemantic.LOAD, entityId, LockMode.NONE)
            loadedWithLock.shouldNotBeNull()
            loadedWithLock.name shouldBeEqualTo entityName
        }
    }
}
