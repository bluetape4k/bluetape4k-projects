package io.bluetape4k.hibernate

import io.bluetape4k.hibernate.mapping.simple.SimpleEntity
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class SessionSupportTest: AbstractHibernateTest() {

    @Test
    fun `session reified helpers 는 타입 정보를 줄여준다`() {
        val entity = SimpleEntity("session-reified")
        tem.persist(entity)
        flushAndClear()

        val session = em.currentSession()

        val loaded = session.findAs<SimpleEntity>(entity.id!!)
        loaded.shouldNotBeNull()
        loaded.name shouldBeEqualTo entity.name

        val reference = session.getReferenceAs<SimpleEntity>(entity.id!!)
        reference.id shouldBeEqualTo entity.id

        val count = session.createQueryAs<Long>(
            "select count(e) from simple_entity e where e.name = :name"
        )
            .setParameter("name", entity.name)
            .singleResult
        count.toLong() shouldBeEqualTo 1L

        val loadedName = session.createNativeQueryAs<String>(
            "select name from simple_entity where id = :id"
        )
            .setParameter("id", entity.id)
            .singleResult
        loadedName shouldBeEqualTo entity.name
    }
}
