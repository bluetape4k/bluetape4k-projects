package io.bluetape4k.hibernate.querydsl.core

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test

class ConstantSupportTest {

    @Test
    fun `primitive constantOf helpers return correct values`() {
        constantOf(true).constant.shouldBeTrue()
        constantOf('a').constant shouldBeEqualTo 'a'
        constantOf(1.toByte()).constant shouldBeEqualTo 1.toByte()
        constantOf(2).constant shouldBeEqualTo 2
        constantOf(3L).constant shouldBeEqualTo 3L
        constantOf(4.toShort()).constant shouldBeEqualTo 4.toShort()
    }

    @Test
    fun `generic constantOf preserves type`() {
        val value = listOf("x", "y")
        val constant = constantOf(value)
        constant.constant shouldBeEqualTo value
    }
}
