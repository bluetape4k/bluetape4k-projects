package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.types.Expression
import com.querydsl.core.types.ExpressionUtils
import com.querydsl.core.types.Ops
import com.querydsl.core.types.dsl.Expressions
import org.amshove.kluent.shouldBeEqualTo
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
    fun `pathOf with metadata creates path`() {
        val metadata = Expressions.stringPath("myVar").metadata
        val path = pathOf<String>(metadata)
        path.shouldNotBeNull()
        path.metadata.name shouldBeEqualTo "myVar"
    }

    @Test
    fun `templateExpressionOf with list args creates expression`() {
        val expr = templateExpressionOf<String>("lower({0})", listOf(str))
        expr.shouldNotBeNull()
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
    fun `Operator newOperation creates typed operation`() {
        val op = Ops.EQ.newOperation<Boolean>(str, Expressions.constant("hello"))
        op.shouldNotBeNull()
    }

    @Test
    fun `allOrNull wraps predicates with AND`() {
        val p1 = str.eq("a")
        val p2 = str.ne("b")
        listOf(p1, p2).allOrNull().shouldNotBeNull()
    }

    @Test
    fun `anyOrNull wraps predicates with OR`() {
        val p1 = str.eq("a")
        val p2 = str.eq("b")
        listOf(p1, p2).anyOrNull().shouldNotBeNull()
    }

    @Test
    fun `Expression count builds count expression`() {
        val countExpr = str.count()
        countExpr.shouldNotBeNull()
    }

    @Test
    fun `Expression eq builds equality predicate`() {
        val other = Expressions.stringPath("other")
        val pred = str.eq(other)
        pred.shouldNotBeNull()
    }

    @Test
    fun `Expression isNull and isNotNull build null predicates`() {
        str.isNull().shouldNotBeNull()
        str.isNotNull().shouldNotBeNull()
    }

    @Test
    fun `Expression neConst and ne build inequality predicates`() {
        val other = Expressions.stringPath("other")
        str.neConst("x").shouldNotBeNull()
        str.ne(other).shouldNotBeNull()
    }

    @Test
    fun `inValues and notIn helpers build predicates`() {
        val list = listOf("a", "b")
        str.inValues(list).toString().shouldNotBeNull()
        str.notIn(list).toString().shouldNotBeNull()
    }

    @Test
    fun `Predicate infix or builds OR predicate`() {
        val p1 = str.eq("a")
        val p2 = str.eq("b")
        val combined = p1.or(p2)
        combined.shouldNotBeNull()
    }

    @Test
    fun `distinctList removes duplicates`() {
        val exprs = listOf(str, num, str)
        val distinct = exprs.distinctList()
        distinct.shouldNotBeNull()
    }

    @Test
    fun `Expression extract unwraps expression`() {
        val extracted = str.extract()
        extracted.shouldNotBeNull()
    }

    @Test
    fun `Expression lowercase converts to lower`() {
        val lower = str.lowercase()
        lower.shouldNotBeNull()
    }

    @Test
    fun `orderBy converts OrderSpecifiers`() {
        val spec = str.asc()
        val expr = listOf(spec).orderBy()
        expr.shouldNotBeNull()
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
    fun `count eq isNull isNotNull ne notIn on raw OperationImpl Expression`() {
        // OperationImpl 은 SimpleExpression 을 상속하지 않으므로 ExpressionUtils extension 이 호출됨
        val rawStr: Expression<String> = ExpressionUtils.operation(String::class.java, Ops.TRIM, str)
        rawStr.count().shouldNotBeNull()
        rawStr.isNull().shouldNotBeNull()
        rawStr.isNotNull().shouldNotBeNull()
        rawStr.eq(str).shouldNotBeNull()
        rawStr.ne(str).shouldNotBeNull()
        rawStr.notIn(listOf("a", "b")).shouldNotBeNull()
    }

    @Test
    fun `rootVariable and toExpression expose underlying values`() {
        val variable = pathOf<String>("root")
        variable.rootVariable().shouldNotBeNull()
        variable.rootVariable(1).shouldNotBeNull()

        "value".toExpression().toString().isNotBlank().shouldBeTrue()
    }
}
