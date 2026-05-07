package io.bluetape4k.hibernate.model

import io.bluetape4k.hibernate.AbstractHibernateTest
import io.bluetape4k.hibernate.mapping.simple.SimpleEntity
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * [JpaEntity] 인터페이스와 [AbstractJpaEntity] 추상 클래스의 동작을 검증하는 테스트입니다.
 *
 * - `identifier` 프로퍼티가 id가 null일 때 [IllegalStateException]을 발생시키는지 확인합니다.
 * - `isPersisted` 프로퍼티가 id 존재 여부에 따라 올바르게 반환되는지 확인합니다.
 */
class JpaEntityModelTest: AbstractHibernateTest() {

    companion object: KLogging()

    @Test
    fun `identifier는 id가 null이면 IllegalStateException을 발생시킨다`() {
        // Transient(미영속화) 엔티티는 id가 null
        val transient = SimpleEntity("identifier-null-test")

        transient.id.shouldBeNull()
        transient.isPersisted.shouldBeFalse()

        assertThrows<IllegalStateException> {
            transient.identifier
        }
    }

    @Test
    fun `identifier는 id가 설정된 경우 값을 반환한다`() {
        val entity = SimpleEntity("identifier-present-test")
        tem.persistAndFlush(entity)
        flushAndClear()

        val loaded = em.find(SimpleEntity::class.java, entity.id)

        loaded.shouldNotBeNull()
        loaded.isPersisted.shouldBeTrue()
        loaded.identifier.shouldNotBeNull()
        loaded.identifier shouldBeEqualTo entity.id
    }

    @Test
    fun `isPersisted는 영속화 전에 false를 반환한다`() {
        val transient = SimpleEntity("ispersisted-transient")

        transient.isPersisted.shouldBeFalse()
    }

    @Test
    fun `isPersisted는 영속화 후에 true를 반환한다`() {
        val entity = SimpleEntity("ispersisted-persisted")
        tem.persistAndFlush(entity)

        entity.isPersisted.shouldBeTrue()
    }

    @Test
    fun `두 transient 엔티티는 동일한 business key를 가지면 equals가 true이다`() {
        val e1 = SimpleEntity("same-name")
        val e2 = SimpleEntity("same-name")

        // 둘 다 transient이므로 business key(name)로 비교
        (e1 == e2).shouldBeTrue()
    }

    @Test
    fun `두 persisted 엔티티는 id로 비교한다`() {
        val e1 = SimpleEntity("persisted-1-${System.nanoTime()}")
        val e2 = SimpleEntity("persisted-2-${System.nanoTime()}")

        tem.persistAndFlush(e1)
        tem.persistAndFlush(e2)
        flushAndClear()

        // 다른 id이므로 false
        (e1 == e2).shouldBeFalse()
    }
}
