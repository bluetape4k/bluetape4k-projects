package io.bluetape4k.hibernate.standalone

import io.bluetape4k.hibernate.getEntityName
import io.bluetape4k.hibernate.registerEventListener
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.assertions.shouldBeNull
import org.hibernate.event.spi.EventType
import org.hibernate.event.spi.PreInsertEvent
import org.hibernate.event.spi.PreInsertEventListener
import org.junit.jupiter.api.Test

class SessionFactorySupportStandaloneTest : AbstractStandaloneHibernateTest() {

    override fun entityClasses() = listOf(StandaloneEntity::class.java)

    @Test
    fun `getEntityName는 등록된 엔티티 이름을 반환한다`() {
        val name = sessionFactory.getEntityName(StandaloneEntity::class.java)
        name.shouldNotBeNull()
        name shouldBeEqualTo "StandaloneEntity"
    }

    @Test
    fun `getEntityName는 등록되지 않은 클래스에 대해 null을 반환한다`() {
        val name = sessionFactory.getEntityName(String::class.java)
        name.shouldBeNull()
    }

    @Test
    fun `getEntityName reified는 등록된 엔티티 이름을 반환한다`() {
        val name = sessionFactory.getEntityName<StandaloneEntity>()
        name.shouldNotBeNull()
    }

    @Test
    fun `registerEventListener는 이벤트 리스너를 등록한다`() {
        var called = false
        val listener = PreInsertEventListener { _: PreInsertEvent -> called = false; false }
        sessionFactory.registerEventListener(listener, listOf(EventType.PRE_INSERT))
        // 리스너 등록 후 예외 없이 완료
    }
}
