package io.bluetape4k.hibernate

import io.bluetape4k.hibernate.mapping.simple.SimpleEntity
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class EntityManagerSupportTest: AbstractHibernateTest() {

    @Test
    fun `deleteById는 없는 id에 대해서도 예외를 발생시키지 않는다`() {
        assertDoesNotThrow {
            em.deleteById<SimpleEntity>(Long.MAX_VALUE)
        }
    }

    @Test
    fun `countAll과 deleteAll은 엔티티 개수 집계와 삭제를 지원한다`() {
        val names = listOf("support-count-1", "support-count-2")
        names.forEach { tem.persist(SimpleEntity(it)) }
        flushAndClear()

        em.countAll<SimpleEntity>() shouldBeEqualTo 2L
        em.deleteAll<SimpleEntity>() shouldBeEqualTo 2
        em.countAll<SimpleEntity>() shouldBeEqualTo 0L
    }
}
