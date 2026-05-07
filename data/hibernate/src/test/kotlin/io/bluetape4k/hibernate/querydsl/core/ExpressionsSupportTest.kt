package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.types.PathMetadataFactory
import com.querydsl.core.types.dsl.Expressions
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.util.Date

class ExpressionsSupportTest {

    @Test
    fun `all은 빈 컬렉션에서 true를 반환한다`() {
        val result = emptyList<com.querydsl.core.types.dsl.BooleanExpression>().all()
        result shouldBeEqualTo Expressions.TRUE
    }

    @Test
    fun `any는 빈 컬렉션에서 false를 반환한다`() {
        val result = emptyList<com.querydsl.core.types.dsl.BooleanExpression>().any()
        result shouldBeEqualTo Expressions.FALSE
    }

    @Test
    fun `all과 any는 비어있지 않은 컬렉션에서 논리식을 조합한다`() {
        val flag = Expressions.booleanPath("flag")
        val p1 = flag.isTrue
        val p2 = flag.isFalse

        val allExpr = listOf(p1, p2).all()
        val anyExpr = listOf(p1, p2).any()

        allExpr.toString() shouldBeEqualTo "flag = true && flag = false"
        anyExpr.toString() shouldBeEqualTo "flag = true || flag = false"
    }

    @Test
    fun `all은 단일 요소 컬렉션을 그대로 반환한다`() {
        val flag = Expressions.booleanPath("flag")
        val expr = listOf(flag.isTrue).all()

        expr.toString() shouldBeEqualTo "flag = true"
    }

    @Test
    fun `any는 단일 요소 컬렉션을 그대로 반환한다`() {
        val flag = Expressions.booleanPath("flag")
        val expr = listOf(flag.isFalse).any()

        expr.toString() shouldBeEqualTo "flag = false"
    }

    // Date/Time expressions

    @Test
    fun `currentDateExpr는 DateExpression을 반환한다`() {
        val expr = currentDateExpr()
        expr.shouldNotBeNull()
        expr.type shouldBeEqualTo Date::class.java
    }

    @Test
    fun `currentTimeExpr는 TimeExpression을 반환한다`() {
        val expr = currentTimeExpr()
        expr.shouldNotBeNull()
    }

    @Test
    fun `currentTimestampExpr는 DateTimeExpression을 반환한다`() {
        val expr = currentTimestampExpr()
        expr.shouldNotBeNull()
        expr.type shouldBeEqualTo Date::class.java
    }

    // toExpression

    @Test
    fun `toExpression는 상수 Expression을 반환한다`() {
        val expr = 42.toExpression()
        expr.shouldNotBeNull()
        expr.toString() shouldBeEqualTo "42"
    }

    @Test
    fun `toExpression with alias는 alias SimpleExpression을 반환한다`() {
        val alias = Expressions.numberPath(Int::class.java, "n")
        val expr = 42.toExpression(alias)
        expr.shouldNotBeNull()
    }

    // Template factories

    @Test
    fun `simpleTemplateOf는 SimpleTemplate을 반환한다`() {
        val tmpl = simpleTemplateOf<String>("upper({0})", "hello")
        tmpl.shouldNotBeNull()
    }

    @Test
    fun `simpleTemplateOf with list는 SimpleTemplate을 반환한다`() {
        val tmpl = simpleTemplateOf<String>("upper({0})", listOf("hello"))
        tmpl.shouldNotBeNull()
    }

    @Test
    fun `dslTemplateOf는 DslTemplate을 반환한다`() {
        val tmpl = dslTemplateOf<String>("upper({0})", "hello")
        tmpl.shouldNotBeNull()
    }

    @Test
    fun `dslTemplateOf with list는 DslTemplate을 반환한다`() {
        val tmpl = dslTemplateOf<String>("upper({0})", listOf("hello"))
        tmpl.shouldNotBeNull()
    }

    @Test
    fun `comparableTemplateOf는 ComparableTemplate을 반환한다`() {
        val tmpl = comparableTemplateOf<String>("upper({0})", "hello")
        tmpl.shouldNotBeNull()
    }

    @Test
    fun `dateTemplateOf는 DateTemplate을 반환한다`() {
        val tmpl = dateTemplateOf<Date>("current_date")
        tmpl.shouldNotBeNull()
    }

    @Test
    fun `dateTimeTemplateOf는 DateTimeTemplate을 반환한다`() {
        val tmpl = dateTimeTemplateOf<Date>("current_timestamp")
        tmpl.shouldNotBeNull()
    }

    @Test
    fun `timeTemplateOf는 TimeTemplate을 반환한다`() {
        val tmpl = timeTemplateOf<java.sql.Time>("current_time")
        tmpl.shouldNotBeNull()
    }

    @Test
    fun `enumTemplateOf는 EnumTemplate을 반환한다`() {
        val tmpl = enumTemplateOf<TestEnum>("{0}", TestEnum.A)
        tmpl.shouldNotBeNull()
    }

    @Test
    fun `numberTemplateOf는 NumberTemplate을 반환한다`() {
        val tmpl = numberTemplateOf<Int>("1 + 1")
        tmpl.shouldNotBeNull()
    }

    @Test
    fun `stringTemplateOf는 StringTemplate을 반환한다`() {
        val tmpl = stringTemplateOf("upper({0})", "hello")
        tmpl.shouldNotBeNull()
    }

    @Test
    fun `booleanTemplateOf는 BooleanTemplate을 반환한다`() {
        val tmpl = booleanTemplateOf("1 = 1")
        tmpl.shouldNotBeNull()
    }

    @Test
    fun `booleanTemplateOf with list args를 반환한다`() {
        val tmpl = booleanTemplateOf("1 = {0}", listOf(1))
        tmpl.shouldNotBeNull()
    }

    // Path factories

    @Test
    fun `simplePathOf 변수명으로 SimplePath를 생성한다`() {
        val path = simplePathOf<String>("myVar")
        path.shouldNotBeNull()
        path.metadata.name shouldBeEqualTo "myVar"
    }

    @Test
    fun `simplePathOf parent+property로 SimplePath를 생성한다`() {
        val parent = simplePathOf<Any>("root")
        val child = simplePathOf<String>(parent, "child")
        child.shouldNotBeNull()
    }

    @Test
    fun `simplePathOf metadata로 SimplePath를 생성한다`() {
        val meta = PathMetadataFactory.forVariable("myVar")
        val path = simplePathOf<String>(meta)
        path.shouldNotBeNull()
    }

    @Test
    fun `dslPathOf 변수명으로 DslPath를 생성한다`() {
        val path = dslPathOf<String>("myVar")
        path.shouldNotBeNull()
        path.metadata.name shouldBeEqualTo "myVar"
    }

    @Test
    fun `dslPathOf parent+property로 DslPath를 생성한다`() {
        val parent = dslPathOf<Any>("root")
        val child = dslPathOf<String>(parent, "child")
        child.shouldNotBeNull()
    }

    @Test
    fun `dslPathOf metadata로 DslPath를 생성한다`() {
        val meta = PathMetadataFactory.forVariable("myVar")
        val path = dslPathOf<String>(meta)
        path.shouldNotBeNull()
    }

    @Test
    fun `comparablePathOf 변수명으로 ComparablePath를 생성한다`() {
        val path = comparablePathOf<Int>("score")
        path.shouldNotBeNull()
        path.metadata.name shouldBeEqualTo "score"
    }

    @Test
    fun `comparablePathOf parent+property로 ComparablePath를 생성한다`() {
        val parent = simplePathOf<Any>("root")
        val path = comparablePathOf<Int>(parent, "score")
        path.shouldNotBeNull()
    }

    @Test
    fun `comparablePathOf metadata로 ComparablePath를 생성한다`() {
        val meta = PathMetadataFactory.forVariable("score")
        val path = comparablePathOf<Int>(meta)
        path.shouldNotBeNull()
    }

    @Test
    fun `comparableEntityPathOf 변수명으로 ComparableEntityPath를 생성한다`() {
        val path = comparableEntityPathOf<String>("entity")
        path.shouldNotBeNull()
    }

    @Test
    fun `numberPathOf 변수명으로 NumberPath를 생성한다`() {
        val path = numberPathOf<Int>("count")
        path.shouldNotBeNull()
        path.metadata.name shouldBeEqualTo "count"
    }

    @Test
    fun `numberPathOf parent+property로 NumberPath를 생성한다`() {
        val parent = simplePathOf<Any>("root")
        val path = numberPathOf<Int>(parent, "count")
        path.shouldNotBeNull()
    }

    @Test
    fun `numberPathOf metadata로 NumberPath를 생성한다`() {
        val meta = PathMetadataFactory.forVariable("count")
        val path = numberPathOf<Int>(meta)
        path.shouldNotBeNull()
    }

    @Test
    fun `stringPathOf 변수명으로 StringPath를 생성한다`() {
        val path = stringPathOf("name")
        path.shouldNotBeNull()
        path.metadata.name shouldBeEqualTo "name"
    }

    @Test
    fun `booleanPathOf 변수명으로 BooleanPath를 생성한다`() {
        val path = booleanPathOf("active")
        path.shouldNotBeNull()
        path.metadata.name shouldBeEqualTo "active"
    }

    @Test
    fun `booleanPathOf parent+variable로 BooleanPath를 생성한다`() {
        val parent = simplePathOf<Any>("root")
        val path = booleanPathOf(parent, "active")
        path.shouldNotBeNull()
    }

    @Test
    fun `booleanPathOf metadata로 BooleanPath를 생성한다`() {
        val meta = PathMetadataFactory.forVariable("active")
        val path = booleanPathOf(meta)
        path.shouldNotBeNull()
    }

    @Test
    fun `datePathOf 변수명으로 DatePath를 생성한다`() {
        val path = datePathOf<Date>("createdAt")
        path.shouldNotBeNull()
    }

    @Test
    fun `datePathOf parent+property로 DatePath를 생성한다`() {
        val parent = simplePathOf<Any>("root")
        val path = datePathOf<Date>(parent, "createdAt")
        path.shouldNotBeNull()
    }

    @Test
    fun `datePathOf metadata로 DatePath를 생성한다`() {
        val meta = PathMetadataFactory.forVariable("createdAt")
        val path = datePathOf<Date>(meta)
        path.shouldNotBeNull()
    }

    @Test
    fun `dateTimePathOf 변수명으로 DateTimePath를 생성한다`() {
        val path = dateTimePathOf<Date>("updatedAt")
        path.shouldNotBeNull()
    }

    @Test
    fun `dateTimePathOf parent+property로 DateTimePath를 생성한다`() {
        val parent = simplePathOf<Any>("root")
        val path = dateTimePathOf<Date>(parent, "updatedAt")
        path.shouldNotBeNull()
    }

    @Test
    fun `dateTimePathOf metadata로 DateTimePath를 생성한다`() {
        val meta = PathMetadataFactory.forVariable("updatedAt")
        val path = dateTimePathOf<Date>(meta)
        path.shouldNotBeNull()
    }

    @Test
    fun `timePathOf 변수명으로 TimePath를 생성한다`() {
        val path = timePathOf<java.sql.Time>("startTime")
        path.shouldNotBeNull()
    }

    @Test
    fun `timePathOf parent+property로 TimePath를 생성한다`() {
        val parent = simplePathOf<Any>("root")
        val path = timePathOf<java.sql.Time>(parent, "startTime")
        path.shouldNotBeNull()
    }

    @Test
    fun `timePathOf metadata로 TimePath를 생성한다`() {
        val meta = PathMetadataFactory.forVariable("startTime")
        val path = timePathOf<java.sql.Time>(meta)
        path.shouldNotBeNull()
    }

    @Test
    fun `enumPathOf 변수명으로 EnumPath를 생성한다`() {
        val path = enumPathOf<TestEnum>("status")
        path.shouldNotBeNull()
        path.metadata.name shouldBeEqualTo "status"
    }

    @Test
    fun `enumPathOf parent+property로 EnumPath를 생성한다`() {
        val parent = simplePathOf<Any>("root")
        val path = enumPathOf<TestEnum>(parent, "status")
        path.shouldNotBeNull()
    }

    @Test
    fun `enumPathOf metadata로 EnumPath를 생성한다`() {
        val meta = PathMetadataFactory.forVariable("status")
        val path = enumPathOf<TestEnum>(meta)
        path.shouldNotBeNull()
    }

    // eqOrNull

    @Test
    fun `StringPath_eqOrNull은 null 입력 시 null을 반환한다`() {
        val path = stringPathOf("name")
        val expr = path.eqOrNull(null)
        expr shouldBeEqualTo null
    }

    @Test
    fun `StringPath_eqOrNull은 값 입력 시 BooleanExpression을 반환한다`() {
        val path = stringPathOf("name")
        val expr = path.eqOrNull("Alice")
        expr.shouldNotBeNull()
        expr.toString() shouldBeEqualTo "name = Alice"
    }

    // Expression list/set

    @Test
    fun `simpleExpressionListOf Tuple은 Expression을 반환한다`() {
        val p1 = Expressions.stringPath("a")
        val p2 = Expressions.stringPath("b")
        val expr = simpleExpressionListOf(p1, p2)
        expr.shouldNotBeNull()
    }

    @Test
    fun `expressionListOf Tuple은 Expression을 반환한다`() {
        val p1 = Expressions.stringPath("a")
        val p2 = Expressions.stringPath("b")
        val expr = expressionListOf(p1, p2)
        expr.shouldNotBeNull()
    }

    @Test
    fun `expressionSetOf Tuple은 Expression을 반환한다`() {
        val p1 = Expressions.stringPath("a")
        val p2 = Expressions.stringPath("b")
        val expr = expressionSetOf(p1, p2)
        expr.shouldNotBeNull()
    }

    @Test
    fun `nullExpressionOf는 NullExpression을 반환한다`() {
        val expr = nullExpressionOf<String>()
        expr.shouldNotBeNull()
    }

    @Test
    fun `Path_nullExpression은 NullExpression을 반환한다`() {
        val path = stringPathOf("name")
        val expr = path.nullExpression()
        expr.shouldNotBeNull()
    }

    enum class TestEnum { A, B, C }
}
