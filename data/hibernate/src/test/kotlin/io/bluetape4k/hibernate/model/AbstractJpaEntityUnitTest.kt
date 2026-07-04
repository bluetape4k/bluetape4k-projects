package io.bluetape4k.hibernate.model

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test

class AbstractJpaEntityUnitTest {

    // 테스트용 구체 엔티티
    class TestEntity(
        val name: String,
        override var id: Long? = null,
    ) : AbstractJpaEntity<Long>() {
        override fun equalProperties(other: Any): Boolean =
            other is TestEntity && name == other.name
    }

    @Test
    fun `isPersisted는 id가 null이면 false를 반환한다`() {
        val entity = TestEntity("test")
        entity.isPersisted.shouldBeFalse()
    }

    @Test
    fun `isPersisted는 id가 설정되면 true를 반환한다`() {
        val entity = TestEntity("test", id = 1L)
        entity.isPersisted.shouldBeTrue()
    }

    @Test
    fun `identifier는 id가 null이면 IllegalStateException을 발생시킨다`() {
        val entity = TestEntity("test")
        assertFailsWith<IllegalStateException> {
            entity.identifier
        }
    }

    @Test
    fun `identifier는 id가 설정되면 값을 반환한다`() {
        val entity = TestEntity("test", id = 42L)
        entity.identifier.shouldNotBeNull()
    }

    @Test
    fun `두 transient 엔티티는 equalProperties로 비교된다`() {
        val e1 = TestEntity("alice")
        val e2 = TestEntity("alice")
        (e1 == e2).shouldBeTrue()
    }

    @Test
    fun `두 transient 엔티티는 name이 다르면 false`() {
        val e1 = TestEntity("alice")
        val e2 = TestEntity("bob")
        (e1 == e2).shouldBeFalse()
    }

    @Test
    fun `두 persisted 엔티티는 id로 비교된다`() {
        val e1 = TestEntity("alice", id = 1L)
        val e2 = TestEntity("alice", id = 2L)
        (e1 == e2).shouldBeFalse()
    }

    @Test
    fun `같은 id의 persisted 엔티티는 equals true`() {
        val e1 = TestEntity("alice", id = 1L)
        val e2 = TestEntity("bob", id = 1L)
        (e1 == e2).shouldBeTrue()
    }

    @Test
    fun `persisted와 transient 엔티티는 false`() {
        val persisted = TestEntity("alice", id = 1L)
        val transient = TestEntity("alice")
        (persisted == transient).shouldBeFalse()
    }

    @Test
    fun `null과 비교하면 false`() {
        val entity = TestEntity("test")
        entity.equals(null).shouldBeFalse()
    }

    @Test
    fun `다른 타입과 비교하면 false`() {
        val entity = TestEntity("test")
        (entity.equals("string")).shouldBeFalse()
    }

    @Test
    fun `동일한 transient 엔티티는 같은 hashCode를 반환한다`() {
        val e1 = TestEntity("alice")
        val e2 = TestEntity("alice")

        e1.hashCode() shouldBeEqualTo e2.hashCode()
    }

    @Test
    fun `동일한 transient 엔티티는 hash set에서 하나의 논리 요소로 처리된다`() {
        val e1 = TestEntity("alice")
        val e2 = TestEntity("alice")
        val entities = hashSetOf(e1)

        entities.add(e2).shouldBeFalse()

        entities shouldHaveSize 1
        entities.contains(e2).shouldBeTrue()
    }

    @Test
    fun `hashCode는 persisted 엔티티에서 id hashCode를 반환한다`() {
        val entity = TestEntity("test", id = 42L)
        entity.hashCode() shouldBeEqualTo 42L.hashCode()
    }

    @Test
    fun `toString은 id를 포함한다`() {
        val entity = TestEntity("test", id = 1L)
        val str = entity.toString()
        str.shouldNotBeNull()
    }
}
