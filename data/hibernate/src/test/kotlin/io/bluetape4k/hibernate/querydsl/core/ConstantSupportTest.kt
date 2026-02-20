package io.bluetape4k.hibernate.querydsl.core

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class ConstantSupportTest {

    @Test
    fun `primitive constantOf helpers return correct values`() {
        constantOf(true).shouldNotBeNull().constant.shouldBeTrue()
        constantOf('a').shouldNotBeNull().constant shouldBeEqualTo 'a'
        constantOf(1.toByte()).shouldNotBeNull().constant shouldBeEqualTo 1.toByte()
        constantOf(2).shouldNotBeNull().constant shouldBeEqualTo 2
        constantOf(3L).shouldNotBeNull().constant shouldBeEqualTo 3L
        constantOf(4.toShort()).shouldNotBeNull().constant shouldBeEqualTo 4.toShort()
    }

    @Test
    fun `generic constantOf preserves type`() {
        val value = listOf("x", "y")
        val constant = constantOf(value)

        constant.shouldNotBeNull()
        constant.constant shouldBeEqualTo value
    }
}
