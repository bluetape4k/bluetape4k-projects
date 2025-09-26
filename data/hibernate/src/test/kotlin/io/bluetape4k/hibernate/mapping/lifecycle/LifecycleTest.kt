package io.bluetape4k.hibernate.mapping.lifecycle

import io.bluetape4k.hibernate.AbstractHibernateTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * NOTE: SpringBoot 환경에 `@EnableJpaAuditing(modifyOnCreate = true)` 설정이 필요하다.
 */
class LifecycleTest(
    @param:Autowired private val repository: LifecycleRepository,
): AbstractHibernateTest() {

    companion object: KLogging()

    @Test
    fun `equals for between transient object & abstract jpa entity`() {
        val name1 = faker.name().name()
        val name2 = faker.name().name()

        val transient1 = LifecycleEntity(name1)
        val transient2 = LifecycleEntity(name2)

        val entity1 = LifecycleEntity(name1)

        // jpa entity 는 어느 한쪽 또는 둘다 transient 일때 business signature 를 비교합니다.
        // 모두 transient 이므로 equalProperties에서 name 으로만 비교한다.
        entity1 shouldBeEqualTo transient1
        transient1 shouldBeEqualTo entity1

        repository.save(entity1)

        // entity1 은 persist=true, transient1.persist=false 로 항상 false를 반환
        entity1 shouldNotBeEqualTo transient1
        transient1 shouldNotBeEqualTo entity1

        entity1 shouldNotBeEqualTo transient2
        transient2 shouldNotBeEqualTo entity1

        val saved = repository.saveAndFlush(transient1)
        saved shouldNotBeEqualTo entity1
    }

    @Test
    fun `entity with AuditingEventListener`() {
        val entity = LifecycleEntity("New Entity")

        entity.id.shouldBeNull()
        entity.createdAt.shouldBeNull()
        entity.updatedAt.shouldBeNull()

        val saved = repository.save(entity)

        saved.id.shouldNotBeNull()
        saved.createdAt.shouldNotBeNull()
        saved.updatedAt.shouldNotBeNull()

        saved.name = "Updated entity"

        // NOTE: flush를 하지 않으면 실제로 데이터가 저장된 게 아니기 때문에 같은 속성값을 가지게 된다.
        saved.updatedAt shouldBeEqualTo entity.updatedAt

        // flush 를 한 후에야 updateAt 값이 변경된다.
        val updated = repository.saveAndFlush(saved)

        log.debug { "updated=$updated" }
        updated.updatedAt!!.isAfter(saved.createdAt!!).shouldBeTrue()
    }
}
