@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package io.bluetape4k.hibernate.criteria

import io.bluetape4k.collections.toList
import io.bluetape4k.hibernate.AbstractHibernateTest
import io.bluetape4k.hibernate.createQueryAs
import io.bluetape4k.hibernate.mapping.simple.SimpleEntity
import jakarta.persistence.TypedQuery
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Test

class TypedQuerySupportTest: AbstractHibernateTest() {

    @Test
    fun `longList, longArray, longStream 은 Long query 결과를 변환한다`() {
        val entities = listOf(
            SimpleEntity("typed-query-1"),
            SimpleEntity("typed-query-2")
        )
        entities.forEach { tem.persist(it) }
        flushAndClear()

        fun newLongIdQuery(): TypedQuery<java.lang.Long> = em
            .createQueryAs<java.lang.Long>(
                "select e.id from simple_entity e where e.name like :prefix order by e.id"
            )
            .setParameter("prefix", "typed-query-%")

        val asList = newLongIdQuery().longList()
        asList.size shouldBeEqualTo entities.size

        val asArray = newLongIdQuery().longArray()
        asArray.size shouldBeEqualTo entities.size
        asArray.toList() shouldBeEqualTo asList

        val asStream = newLongIdQuery().longStream()
        asStream.toList() shouldBeEqualTo asList
    }

    @Test
    fun `longResult 와 findOneOrNull 은 단일 결과와 빈 결과를 처리한다`() {
        tem.persist(SimpleEntity("typed-single"))
        flushAndClear()

        val countQuery: TypedQuery<java.lang.Long> = em
            .createQueryAs<java.lang.Long>(
                "select count(e) from simple_entity e where e.name = :name"
            )
            .setParameter("name", "typed-single")

        countQuery.longResult() shouldBeEqualTo 1L

        val emptyQuery: TypedQuery<java.lang.Long> = em
            .createQueryAs<java.lang.Long>(
                "select e.id from simple_entity e where e.name = :name"
            )
            .setParameter("name", "typed-none")

        emptyQuery.findOneOrNull().shouldBeNull()
    }

    @Test
    fun `intList, intArray, intStream 은 Integer query 결과를 변환한다`() {
        val entities = listOf(
            SimpleEntity("typed-int-1"),
            SimpleEntity("typed-int-2")
        )
        entities.forEach { tem.persist(it) }
        flushAndClear()

        fun newIntLengthQuery(): TypedQuery<java.lang.Integer> = em
            .createQueryAs<java.lang.Integer>(
                "select length(e.name) from simple_entity e where e.name like :prefix order by e.id"
            )
            .setParameter("prefix", "typed-int-%")

        val asList = newIntLengthQuery().intList()
        asList.size shouldBeEqualTo entities.size

        val asArray = newIntLengthQuery().intArray()
        asArray.size shouldBeEqualTo entities.size
        asArray.toList() shouldBeEqualTo asList

        val asStream = newIntLengthQuery().intStream()
        asStream.toList() shouldBeEqualTo asList
    }

    @Test
    fun `intResult 는 단일 결과와 빈 결과를 처리한다`() {
        tem.persist(SimpleEntity("typed-int-single"))
        flushAndClear()

        val singleLengthQuery: TypedQuery<java.lang.Integer> = em
            .createQueryAs<java.lang.Integer>(
                "select length(e.name) from simple_entity e where e.name = :name"
            )
            .setParameter("name", "typed-int-single")

        singleLengthQuery.intResult() shouldBeEqualTo "typed-int-single".length

        val emptyLengthQuery: TypedQuery<java.lang.Integer> = em
            .createQueryAs<java.lang.Integer>(
                "select length(e.name) from simple_entity e where e.name = :name"
            )
            .setParameter("name", "typed-int-none")

        emptyLengthQuery.intResult().shouldBeNull()
    }
}
