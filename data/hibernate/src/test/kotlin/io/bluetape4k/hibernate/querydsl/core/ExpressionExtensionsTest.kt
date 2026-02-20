package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.types.dsl.Expressions
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
class ExpressionExtensionsTest {

    private val flag = Expressions.booleanPath("flag")
    private val num = Expressions.numberPath(Int::class.javaObjectType, "num")
    private val str = Expressions.stringPath("str")

    @Test
    fun `boolean operators produce composed expression`() {
        (!flag).toString().shouldNotBeEmpty()
        (flag and flag).toString().shouldNotBeEmpty()
        (flag or flag).toString().shouldNotBeEmpty()
        (flag xor flag).toString().shouldNotBeEmpty()
        (flag xnor flag).toString().shouldNotBeEmpty()
    }

    @Test
    fun `numeric operators support expression and constant operands`() {
        (num + num).toString().shouldNotBeEmpty()
        (num - num).toString().shouldNotBeEmpty()
        (num * num).toString().shouldNotBeEmpty()
        (num / num).toString().shouldNotBeEmpty()
        (num % num).toString().shouldNotBeEmpty()

        (num + 1).toString().shouldNotBeEmpty()
        (num - 1).toString().shouldNotBeEmpty()
        (num * 2).toString().shouldNotBeEmpty()
        (num / 2).toString().shouldNotBeEmpty()
        (num % 2).toString().shouldNotBeEmpty()

        (-num).toString().shouldNotBeEmpty()
    }

    @Test
    fun `string operators handle concat and index access`() {
        val concat = str + "suffix"
        concat.toString().shouldNotBeEmpty()

        val concatExpr = str + Expressions.constant("expr")
        concatExpr.toString().shouldNotBeEmpty()

        val ch = str[1]
        ch.shouldNotBeNull()
        ch.type shouldBeEqualTo Character::class.java
    }

    @Test
    fun `string trimming and padding helpers are exposed`() {
        str.ltrim().toString().shouldContain("ltrim")
        str.rtrim().toString().shouldContain("rtrim")

        str.lpad(5).toString().shouldContain("lpad")
        str.lpad(5, '0').toString().shouldContain("lpad")
        str.rpad(6).toString().shouldContain("rpad")
        str.rpad(6, ' ').toString().shouldContain("rpad")

        // expression overloads
        val len = Expressions.numberPath(Int::class.javaObjectType, "len")
        str.lpad(len).toString().shouldContain("lpad")
        str.rpad(len).toString().shouldContain("rpad")
    }
}
