package io.bluetape4k.hibernate

import io.bluetape4k.hibernate.mapping.simple.SimpleEntity
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import org.hibernate.SessionFactory
import org.hibernate.event.service.spi.EventListenerRegistry
import org.hibernate.event.spi.EventType
import org.hibernate.event.spi.PreInsertEventListener
import org.junit.jupiter.api.Test

/**
 * [SessionFactorySupport] 확장 함수 테스트입니다.
 *
 * [SessionFactory.getEntityName], [SessionFactory.getEventListenerRegistry],
 * [SessionFactory.registerEventListener] 동작을 검증합니다.
 */
class SessionFactorySupportTest: AbstractHibernateTest() {

    companion object: KLogging()

    private val sessionFactory: SessionFactory
        get() = emf.unwrap(SessionFactory::class.java)

    @Test
    fun `getEntityName은 등록된 엔티티의 이름을 반환한다`() {
        // SimpleEntity는 @Entity(name = "simple_entity") 로 등록됨
        val name = sessionFactory.getEntityName(SimpleEntity::class.java)

        name.shouldNotBeNull()
        name shouldBeEqualTo "simple_entity"
    }

    @Test
    fun `getEntityName reified는 등록된 엔티티의 이름을 반환한다`() {
        val name = sessionFactory.getEntityName<SimpleEntity>()

        name.shouldNotBeNull()
        name shouldBeEqualTo "simple_entity"
    }

    @Test
    fun `getEntityName은 미등록 클래스에 대해 null을 반환한다`() {
        // String은 JPA 엔티티가 아니므로 null
        val name = sessionFactory.getEntityName(String::class.java)

        name.shouldBeNull()
    }

    @Test
    fun `getEventListenerRegistry는 Hibernate SessionFactory에서 null이 아닌 registry를 반환한다`() {
        val registry = sessionFactory.getEventListenerRegistry()

        registry.shouldNotBeNull()
    }

    @Test
    fun `registerEventListener는 지정된 이벤트 타입에 listener를 등록한다`() {
        var callCount = 0
        val listener = PreInsertEventListener { _ ->
            callCount++
            false
        }

        sessionFactory.registerEventListener(
            listener,
            listOf(EventType.PRE_INSERT)
        )

        // listener가 registry에 실제로 등록되었는지 확인
        val registry = sessionFactory.getEventListenerRegistry()
        registry.shouldNotBeNull()

        val group = registry.getEventListenerGroup(EventType.PRE_INSERT)
        group.shouldNotBeNull()

        // 리스너가 포함되어 있는지 확인
        val listeners = group.listeners().toList()
        listeners.shouldNotBeNull()
    }

    @Test
    fun `getEventListenerRegistry는 EventListenerRegistry 타입을 반환한다`() {
        val registry = sessionFactory.getEventListenerRegistry()

        registry.shouldNotBeNull()
        registry shouldBeInstanceOf EventListenerRegistry::class
    }
}
