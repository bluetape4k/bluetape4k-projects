package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.types.dsl.Expressions
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
class StringExpressionsSupportTest {

    companion object: KLogging()

    private val str = Expressions.stringPath("str")

    @Test
    fun `plus operator concatenates string and expression`() {
        (str + "suffix").toString() shouldBeEqualTo "str + suffix"
        (str + Expressions.constant("expr")).toString() shouldBeEqualTo "str + expr"
    }

    @Test
    fun `trim helpers produce expressions`() {
        str.ltrim().toString() shouldBeEqualTo "ltrim(str)"
        str.rtrim().toString() shouldBeEqualTo "rtrim(str)"
    }

    @Test
    fun `padding helpers support length and char overloads`() {
        str.lpad(5).toString() shouldBeEqualTo "lpad(str,5)"
        str.lpad(5, '0').toString() shouldBeEqualTo "lpad(str,5,'0')"
        str.rpad(6).toString() shouldBeEqualTo "rpad(str,6)"
        str.rpad(6, 'x').toString() shouldBeEqualTo "rpad(str,6,'x')"
    }

    @Test
    fun `padding helpers support expression overloads`() {
        val len = Expressions.numberPath(Int::class.javaObjectType, "len")

        str.lpad(len).toString() shouldBeEqualTo "lpad(str,len)"
        str.rpad(len).toString() shouldBeEqualTo "rpad(str,len)"
        str.lpad(len, '0').toString() shouldBeEqualTo "lpad(str,len,'0')"
        str.rpad(len, 'x').toString() shouldBeEqualTo "rpad(str,len,'x')"
    }

    @Test
    fun `indexing returns character expression`() {
        val ch = str[1]
        ch.type shouldBeEqualTo Character::class.java
    }
}
