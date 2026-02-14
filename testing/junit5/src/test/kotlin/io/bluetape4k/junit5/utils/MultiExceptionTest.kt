package io.bluetape4k.junit5.utils

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MultiExceptionTest {

    @Test
    fun `예외가 하나면 원본 예외를 던진다`() {
        val subject = MultiException()
        subject.add(IllegalArgumentException("single"))

        val thrown = assertFailsWith<IllegalArgumentException> {
            subject.throwIfNotEmpty()
        }

        assertEquals("single", thrown.message)
    }

    @Test
    fun `예외가 여러개면 MultiException 을 던진다`() {
        val subject = MultiException()
        subject.add(IllegalArgumentException("one"))
        subject.add(IllegalStateException("two"))

        val thrown = assertFailsWith<MultiException> {
            subject.throwIfNotEmpty()
        }

        assertTrue(thrown.message.contains("2 nested exceptions"))
    }

    @Test
    fun `중첩 MultiException 을 add 하면 내부 예외만 병합한다`() {
        val nested = MultiException().apply {
            add(IllegalArgumentException("one"))
            add(IllegalStateException("two"))
        }
        val subject = MultiException()
        subject.add(nested)

        val thrown = assertFailsWith<MultiException> {
            subject.throwIfNotEmpty()
        }

        assertTrue(thrown.message.contains("2 nested exceptions"))
        assertTrue(thrown.message.contains("IllegalArgumentException"))
        assertTrue(thrown.message.contains("IllegalStateException"))
    }
}
