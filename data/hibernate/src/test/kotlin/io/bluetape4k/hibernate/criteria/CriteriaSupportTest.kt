package io.bluetape4k.hibernate.criteria

import io.bluetape4k.hibernate.AbstractHibernateTest
import io.bluetape4k.hibernate.mapping.simple.SimpleEntity
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test

class CriteriaSupportTest: AbstractHibernateTest() {

    @Test
    fun `attribute 와 eq 를 사용해 criteria query를 작성할 수 있다`() {
        listOf("alpha", "beta").forEach { name ->
            tem.persist(SimpleEntity(name))
        }
        flushAndClear()

        val cb = em.criteriaBuilder
        val cq = cb.createQueryAs<SimpleEntity>()
        val root = cq.from<SimpleEntity>()

        cq.select(root)
            .where(cb.eq(root.attribute(SimpleEntity::name), "alpha"))

        val loaded = em.createQuery(cq).resultList
        loaded shouldHaveSize 1
        loaded.first().name shouldBeEqualTo "alpha"
    }

    @Test
    fun `inValues 와 ne 를 조합해 조건식을 만들 수 있다`() {
        val alpha = SimpleEntity("alpha").apply { description = "keep" }
        val beta = SimpleEntity("beta").apply { description = "skip" }
        val gamma = SimpleEntity("gamma").apply { description = "keep" }
        listOf(alpha, beta, gamma).forEach { tem.persist(it) }
        flushAndClear()

        val cb = em.criteriaBuilder
        val cq = cb.createQueryAs<SimpleEntity>()
        val root = cq.from<SimpleEntity>()
        val names = cb.inValues(root.attribute(SimpleEntity::name))
            .value("alpha")
            .value("beta")
            .value("gamma")

        cq.select(root)
            .where(
                names,
                cb.ne(root.attribute(SimpleEntity::description), "skip")
            )

        val loaded = em.createQuery(cq).resultList
        loaded shouldHaveSize 2
    }

    @Test
    fun `eq 와 ne 를 Expression 간 비교로 조합할 수 있다`() {
        listOf("delta", "epsilon").forEach { tem.persist(SimpleEntity(it)) }
        flushAndClear()

        val cb = em.criteriaBuilder
        val cq = cb.createQueryAs<SimpleEntity>()
        val root = cq.from<SimpleEntity>()

        val namePath = root.attribute(SimpleEntity::name)
        val keepLiteral = cb.literal("delta")

        cq.select(root)
            .where(
                cb.eq(namePath, keepLiteral),
                cb.ne(namePath, cb.literal("epsilon"))
            )

        val loaded = em.createQuery(cq).resultList
        loaded shouldHaveSize 1
        loaded.first().name shouldBeEqualTo "delta"
    }

    @Test
    fun `inValues 는 일치하지 않으면 빈 결과를 반환한다`() {
        listOf("zeta", "eta").forEach { tem.persist(SimpleEntity(it)) }
        flushAndClear()

        val cb = em.criteriaBuilder
        val cq = cb.createQueryAs<SimpleEntity>()
        val root = cq.from<SimpleEntity>()

        val names = cb.inValues(root.attribute(SimpleEntity::name))
            .value("theta")
            .value("iota")

        cq.select(root).where(names)

        val loaded = em.createQuery(cq).resultList
        loaded.shouldBeEmpty()
    }
}
