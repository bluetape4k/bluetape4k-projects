package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.types.PathMetadataFactory
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.Expressions
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.Test
import java.util.*

class ExpressionsSupportTest {

    companion object: KLogging()

    @Test
    fun `all은 빈 컬렉션에서 true를 반환한다`() {
        val allExpr = emptyList<BooleanExpression>().all()
        log.debug { "all expr: $allExpr" }
        allExpr shouldBeEqualTo Expressions.TRUE
    }

    @Test
    fun `any는 빈 컬렉션에서 false를 반환한다`() {
        val anyExpr = emptyList<BooleanExpression>().any()
        log.debug { "any expr=$anyExpr" }
        anyExpr shouldBeEqualTo Expressions.FALSE
    }

    @Test
    fun `all과 any는 비어있지 않은 컬렉션에서 논리식을 조합한다`() {
        val flag = Expressions.booleanPath("flag")
        val p1 = flag.isTrue
        val p2 = flag.isFalse

        val allExpr = listOf(p1, p2).all()
        log.debug { "all expr: $allExpr" }
        allExpr.toString() shouldBeEqualTo "flag = true && flag = false"

        val anyExpr = listOf(p1, p2).any()
        log.debug { "any expr=$anyExpr" }
        anyExpr.toString() shouldBeEqualTo "flag = true || flag = false"
    }

    @Test
    fun `all은 단일 요소 컬렉션을 그대로 반환한다`() {
        val flag = Expressions.booleanPath("flag")
        val expr = listOf(flag.isTrue).all()

        log.debug { "all expr: $expr" }
        expr.toString() shouldBeEqualTo "flag = true"
    }

    @Test
    fun `any는 단일 요소 컬렉션을 그대로 반환한다`() {
        val flag = Expressions.booleanPath("flag")
        val expr = listOf(flag.isFalse).any()

        log.debug { "any expr: $expr" }
        expr.toString() shouldBeEqualTo "flag = false"
    }

    // Date/Time expressions

    @Test
    fun `currentDateExpr는 DateExpression을 반환한다`() {
        val expr = currentDateExpr()

        log.debug { "current date expr: $expr" }
        expr.toString() shouldBeEqualTo "current_date()"
        expr.type shouldBeEqualTo Date::class.java
    }

    @Test
    fun `currentTimeExpr는 TimeExpression을 반환한다`() {
        val expr = currentTimeExpr()
        log.debug { "current time expr: $expr" }
        expr.toString() shouldBeEqualTo "current_time()"
    }

    @Test
    fun `currentTimestampExpr는 DateTimeExpression을 반환한다`() {
        val expr = currentTimestampExpr()
        log.debug { "current timestamp expr: $expr" }
        expr.toString() shouldBeEqualTo "current_timestamp()"
    }

    // toExpression

    @Test
    fun `toExpression는 상수 Expression을 반환한다`() {
        val expr = 42.toExpression()
        log.debug { "expr: $expr" }
        expr.toString() shouldBeEqualTo "42"
    }

    @Test
    fun `toExpression with alias는 alias SimpleExpression을 반환한다`() {
        val alias = Expressions.numberPath(Int::class.java, "n")
        val expr = 42.toExpression(alias)
        log.debug { "alias expr: $expr" }
        expr.toString() shouldBeEqualTo "42 as n"
    }

    // Template factories

    @Test
    fun `simpleTemplateOf는 SimpleTemplate을 반환한다`() {
        val tmpl = simpleTemplateOf<String>("upper({0})", "hello")
        log.debug { "tmpl: $tmpl" }
        tmpl.toString() shouldBeEqualTo "upper(hello)"
    }

    @Test
    fun `simpleTemplateOf with list는 SimpleTemplate을 반환한다`() {
        val tmpl = simpleTemplateOf<String>("upper({0})", listOf("hello"))
        log.debug { "tmpl: $tmpl" }
        tmpl.toString() shouldBeEqualTo "upper(hello)"
    }

    @Test
    fun `dslTemplateOf는 DslTemplate을 반환한다`() {
        val tmpl = dslTemplateOf<String>("upper({0})", "hello")
        log.debug { "tmpl: $tmpl" }
        tmpl.toString() shouldBeEqualTo "upper(hello)"
    }

    @Test
    fun `dslTemplateOf with list는 DslTemplate을 반환한다`() {
        val tmpl = dslTemplateOf<String>("upper({0})", listOf("hello"))
        log.debug { "tmpl: $tmpl" }
        tmpl.toString() shouldBeEqualTo "upper(hello)"
    }

    @Test
    fun `comparableTemplateOf는 ComparableTemplate을 반환한다`() {
        val tmpl = comparableTemplateOf<String>("upper({0})", "hello")
        log.debug { "tmpl: $tmpl" }
        tmpl.toString() shouldBeEqualTo "upper(hello)"
    }

    @Test
    fun `dateTemplateOf는 DateTemplate을 반환한다`() {
        val tmpl = dateTemplateOf<Date>("current_date")
        log.debug { "tmpl: $tmpl" }
        tmpl.toString() shouldBeEqualTo "current_date"
    }

    @Test
    fun `dateTimeTemplateOf는 DateTimeTemplate을 반환한다`() {
        val tmpl = dateTimeTemplateOf<Date>("current_timestamp")
        log.debug { "tmpl: $tmpl" }
        tmpl.toString() shouldBeEqualTo "current_timestamp"
    }

    @Test
    fun `timeTemplateOf는 TimeTemplate을 반환한다`() {
        val tmpl = timeTemplateOf<java.sql.Time>("current_time")
        log.debug { "tmpl: $tmpl" }
        tmpl.toString() shouldBeEqualTo "current_time"
    }

    @Test
    fun `enumTemplateOf는 EnumTemplate을 반환한다`() {
        val tmpl = enumTemplateOf<TestEnum>("{0}", TestEnum.A)
        log.debug { "tmpl: $tmpl" }
        tmpl.toString() shouldBeEqualTo "A"
    }

    @Test
    fun `numberTemplateOf는 NumberTemplate을 반환한다`() {
        val tmpl = numberTemplateOf<Int>("1 + 1")
        log.debug { "tmpl: $tmpl" }
        tmpl.toString() shouldBeEqualTo "1 + 1"
    }

    @Test
    fun `stringTemplateOf는 StringTemplate을 반환한다`() {
        val tmpl = stringTemplateOf("upper({0})", "hello")
        log.debug { "tmpl: $tmpl" }
        tmpl.toString() shouldBeEqualTo "upper(hello)"
    }

    @Test
    fun `booleanTemplateOf는 BooleanTemplate을 반환한다`() {
        val tmpl = booleanTemplateOf("1 = 1")
        log.debug { "tmpl: $tmpl" }
        tmpl.toString() shouldBeEqualTo "1 = 1"
    }

    @Test
    fun `booleanTemplateOf with list args를 반환한다`() {
        val tmpl = booleanTemplateOf("1 = {0}", listOf(1))
        log.debug { "tmpl: $tmpl" }
        tmpl.toString() shouldBeEqualTo "1 = 1"
    }

    // Path factories

    @Test
    fun `simplePathOf 변수명으로 SimplePath를 생성한다`() {
        val path = simplePathOf<String>("myVar")
        log.debug { "path: $path" }
        path.metadata.name shouldBeEqualTo "myVar"
    }

    @Test
    fun `simplePathOf parent+property로 SimplePath를 생성한다`() {
        val parent = simplePathOf<Any>("root")
        val child = simplePathOf<String>(parent, "child")

        log.debug { "child path: $child" }
        child.toString() shouldBeEqualTo "root.child"
    }

    @Test
    fun `simplePathOf metadata로 SimplePath를 생성한다`() {
        val meta = PathMetadataFactory.forVariable("myVar")
        val path = simplePathOf<String>(meta)
        log.debug { "path: $path" }
        path.toString() shouldBeEqualTo "myVar"
        path.metadata.name shouldBeEqualTo "myVar"
    }

    @Test
    fun `dslPathOf 변수명으로 DslPath를 생성한다`() {
        val path = dslPathOf<String>("myVar")
        log.debug { "path: $path" }
        path.toString() shouldBeEqualTo "myVar"
        path.metadata.name shouldBeEqualTo "myVar"
    }

    @Test
    fun `dslPathOf parent+property로 DslPath를 생성한다`() {
        val parent = dslPathOf<Any>("root")
        val child = dslPathOf<String>(parent, "child")
        log.debug { "child path: $child" }
        child.toString() shouldBeEqualTo "root.child"
    }

    @Test
    fun `dslPathOf metadata로 DslPath를 생성한다`() {
        val meta = PathMetadataFactory.forVariable("myVar")
        val path = dslPathOf<String>(meta)
        log.debug { "path: $path" }
        path.toString() shouldBeEqualTo "myVar"
        path.metadata.name shouldBeEqualTo "myVar"
    }

    @Test
    fun `comparablePathOf 변수명으로 ComparablePath를 생성한다`() {
        val path = comparablePathOf<Int>("score")
        log.debug { "path: $path" }
        path.toString() shouldBeEqualTo "score"
        path.metadata.name shouldBeEqualTo "score"
    }

    @Test
    fun `comparablePathOf parent+property로 ComparablePath를 생성한다`() {
        val parent = simplePathOf<Any>("root")
        val path = comparablePathOf<Int>(parent, "score")
        log.debug { "child path: $path" }
        path.toString() shouldBeEqualTo "root.score"
        path.metadata.name shouldBeEqualTo "score"
    }

    @Test
    fun `comparablePathOf metadata로 ComparablePath를 생성한다`() {
        val meta = PathMetadataFactory.forVariable("score")
        val path = comparablePathOf<Int>(meta)
        log.debug { "path: $path" }
        path.toString() shouldBeEqualTo "score"
        path.metadata.name shouldBeEqualTo "score"
    }

    @Test
    fun `comparableEntityPathOf 변수명으로 ComparableEntityPath를 생성한다`() {
        val path = comparableEntityPathOf<String>("entity")
        log.debug { "path: $path" }
        path.toString() shouldBeEqualTo "entity"
    }

    @Test
    fun `numberPathOf 변수명으로 NumberPath를 생성한다`() {
        val path = numberPathOf<Int>("count")
        log.debug { "path: $path" }
        path.metadata.name shouldBeEqualTo "count"
    }

    @Test
    fun `numberPathOf parent+property로 NumberPath를 생성한다`() {
        val parent = simplePathOf<Any>("root")
        val path = numberPathOf<Int>(parent, "count")
        log.debug { "path: $path" }
        path.toString() shouldBeEqualTo "root.count"
        path.metadata.name shouldBeEqualTo "count"
    }

    @Test
    fun `numberPathOf metadata로 NumberPath를 생성한다`() {
        val meta = PathMetadataFactory.forVariable("count")
        val path = numberPathOf<Int>(meta)
        log.debug { "path: $path" }
        path.toString() shouldBeEqualTo "count"
        path.metadata.name shouldBeEqualTo "count"
    }

    @Test
    fun `stringPathOf 변수명으로 StringPath를 생성한다`() {
        val path = stringPathOf("name")
        log.debug { "path: $path" }
        path.toString() shouldBeEqualTo "name"
        path.metadata.name shouldBeEqualTo "name"
    }

    @Test
    fun `booleanPathOf 변수명으로 BooleanPath를 생성한다`() {
        val path = booleanPathOf("active")
        log.debug { "path: $path" }
        path.toString() shouldBeEqualTo "active"
        path.metadata.name shouldBeEqualTo "active"
    }

    @Test
    fun `booleanPathOf parent+variable로 BooleanPath를 생성한다`() {
        val parent = simplePathOf<Any>("root")
        val path = booleanPathOf(parent, "active")
        log.debug { "path: $path" }
        path.toString() shouldBeEqualTo "root.active"
        path.metadata.name shouldBeEqualTo "active"
    }

    @Test
    fun `booleanPathOf metadata로 BooleanPath를 생성한다`() {
        val meta = PathMetadataFactory.forVariable("active")
        val path = booleanPathOf(meta)
        log.debug { "path: $path" }
        path.toString() shouldBeEqualTo "active"
        path.metadata.name shouldBeEqualTo "active"
    }

    @Test
    fun `datePathOf 변수명으로 DatePath를 생성한다`() {
        val path = datePathOf<Date>("createdAt")
        log.debug { "path: $path" }
        path.toString() shouldBeEqualTo "createdAt"
    }

    @Test
    fun `datePathOf parent+property로 DatePath를 생성한다`() {
        val parent = simplePathOf<Any>("root")
        val path = datePathOf<Date>(parent, "createdAt")
        log.debug { "path: $path" }
        path.toString() shouldBeEqualTo "root.createdAt"
        path.metadata.name shouldBeEqualTo "createdAt"
    }

    @Test
    fun `datePathOf metadata로 DatePath를 생성한다`() {
        val meta = PathMetadataFactory.forVariable("createdAt")
        val path = datePathOf<Date>(meta)
        log.debug { "path: $path" }
        path.toString() shouldBeEqualTo "createdAt"
        path.metadata.name shouldBeEqualTo "createdAt"
    }

    @Test
    fun `dateTimePathOf 변수명으로 DateTimePath를 생성한다`() {
        val path = dateTimePathOf<Date>("updatedAt")
        log.debug { "path: $path" }
        path.toString() shouldBeEqualTo "updatedAt"
        path.metadata.name shouldBeEqualTo "updatedAt"
    }

    @Test
    fun `dateTimePathOf parent+property로 DateTimePath를 생성한다`() {
        val parent = simplePathOf<Any>("root")
        val path = dateTimePathOf<Date>(parent, "updatedAt")
        log.debug { "path: $path" }
        path.toString() shouldBeEqualTo "root.updatedAt"
        path.metadata.name shouldBeEqualTo "updatedAt"
    }

    @Test
    fun `dateTimePathOf metadata로 DateTimePath를 생성한다`() {
        val meta = PathMetadataFactory.forVariable("updatedAt")
        val path = dateTimePathOf<Date>(meta)
        log.debug { "path: $path" }
        path.toString() shouldBeEqualTo "updatedAt"
        path.metadata.name shouldBeEqualTo "updatedAt"
    }

    @Test
    fun `timePathOf 변수명으로 TimePath를 생성한다`() {
        val path = timePathOf<java.sql.Time>("startTime")
        log.debug { "path: $path" }
        path.toString() shouldBeEqualTo "startTime"
        path.metadata.name shouldBeEqualTo "startTime"
    }

    @Test
    fun `timePathOf parent+property로 TimePath를 생성한다`() {
        val parent = simplePathOf<Any>("root")
        val path = timePathOf<java.sql.Time>(parent, "startTime")
        log.debug { "path: $path" }
        path.toString() shouldBeEqualTo "root.startTime"
        path.metadata.name shouldBeEqualTo "startTime"
    }

    @Test
    fun `timePathOf metadata로 TimePath를 생성한다`() {
        val meta = PathMetadataFactory.forVariable("startTime")
        val path = timePathOf<java.sql.Time>(meta)
        log.debug { "path: $path" }
        path.toString() shouldBeEqualTo "startTime"
        path.metadata.name shouldBeEqualTo "startTime"
    }

    @Test
    fun `enumPathOf 변수명으로 EnumPath를 생성한다`() {
        val path = enumPathOf<TestEnum>("status")
        log.debug { "path: $path" }
        path.toString() shouldBeEqualTo "status"
        path.metadata.name shouldBeEqualTo "status"
    }

    @Test
    fun `enumPathOf parent+property로 EnumPath를 생성한다`() {
        val parent = simplePathOf<Any>("root")
        val path = enumPathOf<TestEnum>(parent, "status")
        log.debug { "path: $path" }
        path.toString() shouldBeEqualTo "root.status"
        path.metadata.name shouldBeEqualTo "status"
    }

    @Test
    fun `enumPathOf metadata로 EnumPath를 생성한다`() {
        val meta = PathMetadataFactory.forVariable("status")
        val path = enumPathOf<TestEnum>(meta)
        log.debug { "path: $path" }
        path.toString() shouldBeEqualTo "status"
        path.metadata.name shouldBeEqualTo "status"
    }

    // eqOrNull

    @Test
    fun `StringPath_eqOrNull은 null 입력 시 null을 반환한다`() {
        val path = stringPathOf("name")
        val expr = path.eqOrNull(null)
        log.debug { "path: $path, expr: $expr" }
        path.toString() shouldBeEqualTo "name"
        path.metadata.name shouldBeEqualTo "name"
        expr.shouldBeNull()
    }

    @Test
    fun `StringPath_eqOrNull은 값 입력 시 BooleanExpression을 반환한다`() {
        val path = stringPathOf("name")
        val expr = path.eqOrNull("Alice")
        log.debug { "path: $path, expr: $expr" }
        expr.toString() shouldBeEqualTo "name = Alice"
    }

    // Expression list/set

    @Test
    fun `simpleExpressionListOf Tuple은 Expression을 반환한다`() {
        val p1 = Expressions.stringPath("a")
        val p2 = Expressions.stringPath("b")
        val expr = simpleExpressionListOf(p1, p2)
        log.debug { "expr: $expr" }
        expr.toString() shouldBeEqualTo "a, b"
    }

    @Test
    fun `expressionListOf Tuple은 Expression을 반환한다`() {
        val p1 = Expressions.stringPath("a")
        val p2 = Expressions.stringPath("b")
        val expr = expressionListOf(p1, p2)
        log.debug { "expr: $expr" }
        expr.toString() shouldBeEqualTo "a, b"
    }

    @Test
    fun `expressionSetOf Tuple은 Expression을 반환한다`() {
        val p1 = Expressions.stringPath("a")
        val p2 = Expressions.stringPath("b")
        val expr = expressionSetOf(p1, p2)
        log.debug { "expr: $expr" }
        expr.toString() shouldBeEqualTo "a, b"
    }

    @Test
    fun `nullExpressionOf는 NullExpression을 반환한다`() {
        val expr = nullExpressionOf<String>()
        log.debug { "expr: $expr" }
        expr.toString() shouldBeEqualTo "null"
    }

    @Test
    fun `Path_nullExpression은 NullExpression을 반환한다`() {
        val path = stringPathOf("name")
        val expr = path.nullExpression()
        log.debug { "expr: $expr" }
        expr.toString() shouldBeEqualTo "null"
    }

    enum class TestEnum {
        A,
        B,
        C
    }
}
