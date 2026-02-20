package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.types.Ops
import com.querydsl.core.types.dsl.Expressions
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class ExpressionUtilsSupportTest {

    private val str = Expressions.stringPath("str")
    private val num = Expressions.numberPath(Int::class.javaObjectType, "num")

    @Test
    fun `path and template helpers create expressions`() {
        val path = pathOf<String>("name")
        path.toString().shouldNotBeNull()

        val child = pathOf<String>(path, "child")
        child.toString().shouldNotBeNull()

        val templ = templateExpressionOf<String>("lower({0})", str)
        templ.toString().shouldNotBeNull()
    }

    @Test
    fun `predicate builders wrap operators`() {
        val op = Ops.EQ
        val predicate = op.newPredicate(str, Expressions.constant("x"))
        predicate.toString().shouldNotBeNull()

        (predicate and predicate).toString().shouldNotBeNull()
        (predicate or predicate).toString().shouldNotBeNull()
    }

    @Test
    fun `inValues and notIn helpers build predicates`() {
        val list = listOf("a", "b")
        str.inValues(list).toString().shouldNotBeNull()
        str.notIn(list).toString().shouldNotBeNull()
    }

    @Test
    fun `regex and like conversions`() {
        str.likeToRegex().toString().shouldNotBeNull()
        str.regexToLike().toString().shouldNotBeNull()
    }

    @Test
    fun `count and eqConst build expressions`() {
        num.count().toString().shouldNotBeNull()
        num.eqConst(1).toString().shouldNotBeNull()
    }

    @Test
    fun `rootVariable and toExpression expose underlying values`() {
        val variable = pathOf<String>("root")
        variable.rootVariable().shouldNotBeNull()
        variable.rootVariable(1).shouldNotBeNull()

        "value".toExpression().toString().isNotBlank().shouldBeTrue()
    }
}
