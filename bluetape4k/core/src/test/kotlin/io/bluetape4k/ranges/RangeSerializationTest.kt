package io.bluetape4k.ranges

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.NotSerializableException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamClass

class RangeSerializationTest {

    @Test
    fun `네 가지 기본 Range는 Int 경계의 Java serialization round trip을 유지한다`() {
        val ranges = listOf<Range<Int>>(
            closedClosedRangeOf(1, 5),
            closedOpenRangeOf(1, 5),
            openClosedRangeOf(1, 5),
            openOpenRangeOf(1, 5),
        )

        ranges.forEach { range ->
            deserialize<Range<Int>>(serialize(range)) shouldBeEqualTo range
            ObjectStreamClass.lookup(range.javaClass).serialVersionUID shouldBeEqualTo 1L
        }
    }

    @Test
    fun `직렬화 가능한 String 경계도 범위 의미를 보존한다`() {
        val range = openClosedRangeOf("a", "z")

        deserialize<Range<String>>(serialize(range)) shouldBeEqualTo range
    }

    @Test
    fun `endpoint가 Serializable이 아니면 Java serialization이 명시적으로 실패한다`() {
        val range = closedClosedRangeOf(NonSerializablePoint(1), NonSerializablePoint(5))

        assertFailsWith<NotSerializableException> { serialize(range) }
    }

    private class NonSerializablePoint(private val value: Int): Comparable<NonSerializablePoint> {
        override fun compareTo(other: NonSerializablePoint): Int = value.compareTo(other.value)
    }

    private fun serialize(value: Any): ByteArray = ByteArrayOutputStream().use { bytes ->
        ObjectOutputStream(bytes).use { it.writeObject(value) }
        bytes.toByteArray()
    }

    private inline fun <reified T> deserialize(bytes: ByteArray): T =
        ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() as T }
}
