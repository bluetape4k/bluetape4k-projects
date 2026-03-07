package io.bluetape4k.exposed.core

import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.io.Serializable

/**
 * [HasIdentifier] 인터페이스 단위 테스트입니다.
 */
class HasIdentifierTest {

    private data class LongEntity(override val id: Long?): HasIdentifier<Long>
    private data class StringEntity(override val id: String?): HasIdentifier<String>
    private data class NullableIdEntity(override val id: Int?): HasIdentifier<Int>

    @Test
    fun `HasIdentifier는 Long 타입 id를 반환한다`() {
        val entity = LongEntity(42L)
        entity.id shouldBeEqualTo 42L
    }

    @Test
    fun `HasIdentifier는 String 타입 id를 반환한다`() {
        val entity = StringEntity("user-uuid-1234")
        entity.id shouldBeEqualTo "user-uuid-1234"
    }

    @Test
    fun `HasIdentifier 구현체는 Serializable이다`() {
        val entity = LongEntity(1L)
        entity shouldBeInstanceOf Serializable::class
    }

    @Test
    fun `같은 id를 가진 두 엔티티는 equals가 true다`() {
        val a = LongEntity(100L)
        val b = LongEntity(100L)
        a shouldBeEqualTo b
    }

    @Test
    fun `다른 id를 가진 두 엔티티는 equals가 false다`() {
        val a = LongEntity(1L)
        val b = LongEntity(2L)
        (a == b).shouldBeFalse()
    }

    @Test
    fun `HasIdentifier id는 null 일 수 있다`() {
        val entity = LongEntity(null)
        entity.id.shouldBeNull()
    }

    @Test
    fun `HasIdentifier id가 null이 아닌 경우 값을 반환한다`() {
        val entity = LongEntity(42L)
        entity.id.shouldNotBeNull()
        entity.id shouldBeEqualTo 42L
    }

    @Test
    fun `Int nullable id를 가진 엔티티를 생성할 수 있다`() {
        val withId = NullableIdEntity(10)
        val withoutId = NullableIdEntity(null)
        withId.id.shouldNotBeNull()
        withoutId.id.shouldBeNull()
    }
}
