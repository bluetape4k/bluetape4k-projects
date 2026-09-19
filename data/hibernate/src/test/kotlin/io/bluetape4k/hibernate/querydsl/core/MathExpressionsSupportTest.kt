package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.types.dsl.Expressions
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

class MathExpressionsSupportTest {

    companion object: KLogging()

    private val num = Expressions.numberPath(Double::class.javaObjectType, "num")

    @Test
    fun `trigonometry helpers build expressions`() {
        num.acos().toString() shouldBeEqualTo "acos(num)"
        num.asin().toString() shouldBeEqualTo "asin(num)"
        num.atan().toString() shouldBeEqualTo "atan(num)"
        num.cos().toString() shouldBeEqualTo "cos(num)"
        num.cosh().toString() shouldBeEqualTo "cosh(num)"
        num.cot().toString() shouldBeEqualTo "cot(num)"
        num.coth().toString() shouldBeEqualTo "coth(num)"
        num.degrees().toString() shouldBeEqualTo "degrees(num)"
        num.radians().toString() shouldBeEqualTo "radians(num)"
    }

    @Test
    fun `log and power helpers build expressions`() {
        num.exp().toString() shouldBeEqualTo "exp(num)"
        num.ln().toString() shouldBeEqualTo "ln(num)"
        num.log(10).toString() shouldBeEqualTo "log(num,10)"
        num.power(2).toString() shouldBeEqualTo "pow(num,2)"
    }

    @Test
    fun `min max and round helpers build expressions`() {
        val other = Expressions.numberPath(Double::class.javaObjectType, "other")

        (num max other).toString() shouldBeEqualTo "max(num,other)"
        (num min other).toString() shouldBeEqualTo "min(num,other)"
        num.round().toString() shouldBeEqualTo "round(num)"
        num.round(2).toString() shouldBeEqualTo "round(num,2)"
    }

    @Test
    fun `random and sign produce number expressions`() {
        randomExprOf().type.name shouldBeEqualTo "java.lang.Double"
        randomExprOf(1).type.name shouldBeEqualTo "java.lang.Double"
        num.sign().toString() shouldBeEqualTo "sign(num)"
    }

    @Test
    fun `hyperbolic and tangent helpers build expressions`() {
        num.sin().toString() shouldBeEqualTo "sin(num)"
        num.sinh().toString() shouldBeEqualTo "sinh(num)"
        num.tan().toString() shouldBeEqualTo "tan(num)"
        num.tanh().toString() shouldBeEqualTo "tanh(num)"
    }
}
