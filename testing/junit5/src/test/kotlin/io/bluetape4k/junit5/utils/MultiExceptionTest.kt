package io.bluetape4k.junit5.utils

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class MultiExceptionTest {
    @Test
    fun `초기 상태는 비어있다`() {
        val subject = MultiException()
        subject.isEmpty().shouldBeTrue()
    }

    @Test
    fun `예외가 없으면 throwIfNotEmpty는 아무것도 던지지 않는다`() {
        val subject = MultiException()
        // 예외 없이 반환되어야 함
        subject.throwIfNotEmpty()
    }

    @Test
    fun `null 을 add 하면 무시된다`() {
        val subject = MultiException()
        subject.add(null)
        subject.isEmpty().shouldBeTrue()
    }

    @Test
    fun `예외를 add 하면 isEmpty 가 false 를 반환한다`() {
        val subject = MultiException()
        subject.add(IllegalArgumentException("x"))
        subject.isEmpty().shouldBeFalse()
    }

    @Test
    fun `예외가 하나면 원본 예외를 던진다`() {
        val subject = MultiException()
        subject.add(IllegalArgumentException("single"))

        val thrown =
            assertFailsWith<IllegalArgumentException> {
                subject.throwIfNotEmpty()
            }

        thrown.message shouldBeEqualTo "single"
    }

    @Test
    fun `예외가 여러개면 MultiException 을 던진다`() {
        val subject = MultiException()
        subject.add(IllegalArgumentException("one"))
        subject.add(IllegalStateException("two"))

        val thrown =
            assertFailsWith<MultiException> {
                subject.throwIfNotEmpty()
            }

        thrown.message shouldContain "2 nested exceptions"
    }

    @Test
    fun `중첩 MultiException 을 add 하면 내부 예외만 병합한다`() {
        val nested =
            MultiException().apply {
                add(IllegalArgumentException("one"))
                add(IllegalStateException("two"))
            }
        val subject = MultiException()
        subject.add(nested)

        val thrown =
            assertFailsWith<MultiException> {
                subject.throwIfNotEmpty()
            }

        thrown.message shouldContain "2 nested exceptions"
        thrown.message shouldContain "IllegalArgumentException"
        thrown.message shouldContain "IllegalStateException"
    }

    @Test
    fun `message 는 예외 개수 정보를 포함한다`() {
        val subject =
            MultiException().apply {
                add(RuntimeException("a"))
                add(RuntimeException("b"))
                add(RuntimeException("c"))
            }
        subject.message shouldContain "3 nested exceptions"
    }
}
