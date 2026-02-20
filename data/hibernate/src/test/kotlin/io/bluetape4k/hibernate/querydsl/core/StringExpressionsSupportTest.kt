package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.types.dsl.Expressions
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
class StringExpressionsSupportTest {

    private val str = Expressions.stringPath("str")

    @Test
    fun `plus operator concatenates string and expression`() {
        (str + "suffix").toString().shouldNotBeEmpty()
        (str + Expressions.constant("expr")).toString().shouldNotBeEmpty()
    }

    @Test
    fun `trim helpers produce expressions`() {
        str.ltrim().toString().shouldNotBeNull()
        str.rtrim().toString().shouldNotBeNull()
    }

    @Test
    fun `padding helpers support length and char overloads`() {
        str.lpad(5).toString().shouldNotBeEmpty()
        str.lpad(5, '0').toString().shouldNotBeEmpty()
        str.rpad(6).toString().shouldNotBeEmpty()
        str.rpad(6, 'x').toString().shouldNotBeEmpty()
    }

    @Test
    fun `padding helpers support expression overloads`() {
        val len = Expressions.numberPath(Int::class.javaObjectType, "len")
        str.lpad(len).toString().shouldNotBeEmpty()
        str.rpad(len).toString().shouldNotBeEmpty()
        str.lpad(len, '0').toString().shouldNotBeEmpty()
        str.rpad(len, 'x').toString().shouldNotBeEmpty()
    }

    @Test
    fun `indexing returns character expression`() {
        val ch = str[1]
        ch.type shouldBeEqualTo Character::class.java
    }
}
