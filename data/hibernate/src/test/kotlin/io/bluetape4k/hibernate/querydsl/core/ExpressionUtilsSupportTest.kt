package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.types.Expression
import com.querydsl.core.types.ExpressionUtils
import com.querydsl.core.types.Ops
import com.querydsl.core.types.dsl.Expressions
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.Test

class ExpressionUtilsSupportTest {

    companion object: KLogging()

    private val str = Expressions.stringPath("str")
    private val num = Expressions.numberPath(Int::class.javaObjectType, "num")

    @Test
    fun `path and template helpers create expressions`() {
        val path = pathOf<String>("name")
        log.debug { "path: $path" }
        path.toString() shouldBeEqualTo "name"

        val child = pathOf<String>(path, "child")
        log.debug { "child: $child" }
        child.toString() shouldBeEqualTo "name.child"

        val templ = templateExpressionOf<String>("lower({0})", str)
        log.debug { "templ: $templ" }
        templ.toString() shouldBeEqualTo "lower(str)"
    }

    @Test
    fun `pathOf with metadata creates path`() {
        val metadata = Expressions.stringPath("myVar").metadata
        val path = pathOf<String>(metadata)

        log.debug { "path: $path" }
        path.toString() shouldBeEqualTo "myVar"
        path.metadata.name shouldBeEqualTo "myVar"
    }

    @Test
    fun `templateExpressionOf with list args creates expression`() {
        val expr = templateExpressionOf<String>("lower({0})", listOf(str))
        log.debug { "expr: $expr" }
        expr.toString() shouldBeEqualTo "lower(str)"
    }

    @Test
    fun `predicate builders wrap operators`() {
        val op = Ops.EQ
        val predicate = op.newPredicate(str, Expressions.constant("x"))
        predicate.toString().shouldNotBeNull()

        val andPredicate = predicate and predicate
        val orPredicate = predicate or predicate

        log.debug { "and: $andPredicate" }
        log.debug { "or: $orPredicate" }

        andPredicate.toString() shouldBeEqualTo "str = x && str = x"
        orPredicate.toString() shouldBeEqualTo "str = x || str = x"
    }

    @Test
    fun `Operator newOperation creates typed operation`() {
        val op = Ops.EQ.newOperation<Boolean>(str, Expressions.constant("hello"))
        log.debug { "op: $op" }
        op.toString() shouldBeEqualTo "str = hello"
    }

    @Test
    fun `allOrNull wraps predicates with AND`() {
        val p1 = str.eq("a")
        val p2 = str.ne("b")
        val predicate = listOf(p1, p2).allOrNull()
        log.debug { "predicates: $predicate" }
        predicate.toString() shouldBeEqualTo "str = a && str != b"
    }

    @Test
    fun `anyOrNull wraps predicates with OR`() {
        val p1 = str.eq("a")
        val p2 = str.ne("b")
        val predicate = listOf(p1, p2).anyOrNull()
        log.debug { "predicates: $predicate" }
        predicate.toString() shouldBeEqualTo "str = a || str != b"
    }

    @Test
    fun `Expression count builds count expression`() {
        val countExpr = str.count()
        log.debug { "count: $countExpr" }
        countExpr.toString() shouldBeEqualTo "count(str)"
    }

    @Test
    fun `Expression eq builds equality predicate`() {
        val other = Expressions.stringPath("other")
        val pred = str.eq(other)
        log.debug { "pred: $pred" }
        pred.toString() shouldBeEqualTo "str = other"
    }

    @Test
    fun `Expression isNull and isNotNull build null predicates`() {
        val isNull = str.isNull().shouldNotBeNull()
        val isNotNull = str.isNotNull().shouldNotBeNull()

        log.debug { "isNull: $isNull, isNotNull: $isNotNull" }
        isNull.toString() shouldBeEqualTo "str is null"
        isNotNull.toString() shouldBeEqualTo "str is not null"
    }

    @Test
    fun `Expression neConst and ne build inequality predicates`() {
        val other = Expressions.stringPath("other")

        val neConst = str.neConst("x").shouldNotBeNull()
        log.debug { "neConst: $neConst" }
        neConst.toString() shouldBeEqualTo "str != x"

        val ne = str.ne(other).shouldNotBeNull()
        log.debug { "ne: $ne" }
        ne.toString() shouldBeEqualTo "str != other"
    }

    @Test
    fun `inValues and notIn helpers build predicates`() {
        val list = listOf("a", "b")

        val inValues = str.inValues(list).shouldNotBeNull()
        log.debug { "inValues: $inValues" }
        inValues.toString() shouldBeEqualTo "str in [a, b]"

        val notIn = str.notIn(list).shouldNotBeNull()
        log.debug { "notIn: $notIn" }
        notIn.toString() shouldBeEqualTo "str not in [a, b]"
    }

    @Test
    fun `Predicate infix or builds OR predicate`() {
        val p1 = str.eq("a")
        val p2 = str.eq("b")
        val combined = p1.or(p2)
        log.debug { "combined: $combined" }
        combined.toString() shouldBeEqualTo "str = a || str = b"
    }

    @Test
    fun `distinctList removes duplicates`() {
        val exprs =
            listOf(str, num, str)
        val distinct = exprs.distinctList()
        log.debug { "distinctList: $distinct" }
        distinct.toString() shouldBeEqualTo "[str, num]"
    }

    @Test
    fun `Expression extract unwraps expression`() {
        val extracted = str.extract()
        log.debug { "extracted: $extracted" }
        extracted.toString() shouldBeEqualTo "str"
    }

    @Test
    fun `Expression lowercase converts to lower`() {
        val lower = str.lowercase()
        log.debug { "lower: $lower" }
        lower.toString() shouldBeEqualTo "lower(str)"
    }

    @Test
    fun `orderBy converts OrderSpecifiers`() {
        val spec = str.asc()
        val expr = listOf(spec).orderBy()
        log.debug { "expr: $expr" }
        expr.toString() shouldBeEqualTo "order()"
    }

    @Test
    fun `regex and like conversions`() {
        val likeToRegex = str.likeToRegex().shouldNotBeNull()
        log.debug { "likeToRegex: $likeToRegex" }
        likeToRegex.toString() shouldBeEqualTo "str"

        val regexLike = str.regexToLike().shouldNotBeNull()
        log.debug { "regexLike: $regexLike" }
        regexLike.toString() shouldBeEqualTo "str"
    }

    @Test
    fun `count and eqConst build expressions`() {
        val count = num.count().shouldNotBeNull()
        log.debug { "count: $count" }
        count.toString() shouldBeEqualTo "count(num)"

        val eqConst = num.eqConst(1).shouldNotBeNull()
        log.debug { "eqConst: $eqConst" }
        eqConst.toString() shouldBeEqualTo "num = 1"

    }

    @Test
    fun `count eq isNull isNotNull ne notIn on raw OperationImpl Expression`() {
        // OperationImpl 은 SimpleExpression 을 상속하지 않으므로 ExpressionUtils extension 이 호출됨
        val rawStr: Expression<String> = ExpressionUtils.operation(String::class.java, Ops.TRIM, str)

        rawStr.count().apply {
            log.debug { "rawStr.count(): $this" }
            this.toString() shouldBeEqualTo "count(trim(str))"
        }

        rawStr.isNull().apply {
            log.debug { "rawStr.isNull(): $this" }
            this.toString() shouldBeEqualTo "trim(str) is null"
        }

        rawStr.isNotNull().apply {
            log.debug { "rawStr.isNotNull(): $this" }
            this.toString() shouldBeEqualTo "trim(str) is not null"
        }
        rawStr.eq(str).apply {
            log.debug { "rawStr.eq(): $this" }
            this.toString() shouldBeEqualTo "trim(str) = str"
        }

        rawStr.ne(str).apply {
            log.debug { "rawStr.ne(): $this" }
            this.toString() shouldBeEqualTo "trim(str) != str"
        }

        rawStr.notIn(listOf("a", "b")).apply {
            log.debug { "rawStr.notIn(): $this" }
            this.toString() shouldBeEqualTo "trim(str) not in [a, b]"
        }
    }

    @Test
    fun `rootVariable and toExpression expose underlying values`() {
        val variable = pathOf<String>("root")
        log.debug { "variable: $variable" }
        variable.toString() shouldBeEqualTo "root"

        val root = variable.rootVariable()
        log.debug { "root: $root" }
        root shouldBeEqualTo "root"

        val root1 = variable.rootVariable(1)
        log.debug { "root1: $root1" }
        root1 shouldBeEqualTo "root_1"

        val expr = "value".toExpression()
        log.debug { "expr: $expr" }
        expr.toString() shouldBeEqualTo "value"
    }
}
