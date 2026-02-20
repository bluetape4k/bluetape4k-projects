package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.types.dsl.Expressions
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class MathExpressionsSupportTest {

    private val num = Expressions.numberPath(Double::class.javaObjectType, "num")

    @Test
    fun `trigonometry helpers build expressions`() {
        num.acos().toString().shouldNotBeNull()
        num.asin().toString().shouldNotBeNull()
        num.atan().toString().shouldNotBeNull()
        num.cos().toString().shouldNotBeNull()
        num.cosh().toString().shouldNotBeNull()
        num.cot().toString().shouldNotBeNull()
        num.coth().toString().shouldNotBeNull()
        num.degrees().toString().shouldNotBeNull()
        num.radians().toString().shouldNotBeNull()
    }

    @Test
    fun `log and power helpers build expressions`() {
        num.exp().toString().shouldNotBeNull()
        num.ln().toString().shouldNotBeNull()
        num.log(10).toString().shouldNotBeNull()
        num.power(2).toString().shouldNotBeNull()
    }

    @Test
    fun `min max and round helpers build expressions`() {
        val other = Expressions.numberPath(Double::class.javaObjectType, "other")

        (num max other).toString().shouldNotBeNull()
        (num min other).toString().shouldNotBeNull()
        num.round().toString().shouldNotBeNull()
        num.round(2).toString().shouldNotBeNull()
    }

    @Test
    fun `random and sign produce number expressions`() {
        randomExprOf().type.shouldNotBeNull()
        randomExprOf(1).type.shouldNotBeNull()
        num.sign().toString().isNotBlank().shouldBeTrue()
    }

    @Test
    fun `hyperbolic and tangent helpers build expressions`() {
        num.sin().toString().shouldNotBeNull()
        num.sinh().toString().shouldNotBeNull()
        num.tan().toString().shouldNotBeNull()
        num.tanh().toString().shouldNotBeNull()
    }
}
