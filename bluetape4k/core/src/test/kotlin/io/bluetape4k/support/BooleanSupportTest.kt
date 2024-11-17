package io.bluetape4k.support

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class BooleanSupportTest {

    @Test
    fun `boolean ifTrue`() {
        fun condition() = true

        val result = condition().ifTrue { true } ?: false
        result.shouldBeTrue()

        fun condition2() = false
        val result2 = condition2().ifTrue { true } ?: false
        result2.shouldBeFalse()
    }

    @Test
    fun `boolean ifFalse`() {
        fun condition() = true

        val result = condition().ifFalse { true } ?: false
        result.shouldBeFalse()

        fun condition2() = false
        val result2 = condition2().ifFalse { true } ?: false
        result2.shouldBeTrue()
    }

    @Test
    fun `compare booleans`() {
        compareBoolean(true, true) shouldBeEqualTo 0
        compareBoolean(true, false) shouldBeEqualTo 1
        compareBoolean(false, true) shouldBeEqualTo -1
        compareBoolean(false, false) shouldBeEqualTo 0
    }

    @Test
    fun `boolean supplier then infix function`() {
        fun condition() = true

        val result = condition().then { true } ?: false
        result.shouldBeTrue()

        fun condition2() = false
        val result2 = condition2().then(true) ?: false
        result2.shouldBeFalse()
    }

    @Test
    fun `boolean infix function`() {
        fun condition() = true
        val result = condition().then { true } ?: false
        result.shouldBeTrue()

        fun condition2() = false
        val result2 = condition2().then(true) ?: false
        result2.shouldBeFalse()
    }
}
