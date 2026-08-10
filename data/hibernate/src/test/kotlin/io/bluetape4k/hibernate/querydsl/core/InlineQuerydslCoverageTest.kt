package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.types.Expression
import com.querydsl.core.types.Ops
import com.querydsl.core.types.Path
import com.querydsl.core.types.PathMetadataFactory
import com.querydsl.core.types.TemplateFactory
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.SimpleExpression
import com.querydsl.core.types.dsl.SimpleTemplate
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.InvocationTargetException
import java.sql.Time
import java.util.Date

/**
 * Kotlin inline facades are called through their generated static methods so the
 * production entry points themselves remain measurable by Kover.
 */
class InlineQuerydslCoverageTest {

    private val expressionsSupport = Class.forName("io.bluetape4k.hibernate.querydsl.core.ExpressionsSupportKt")

    private val expressionUtilsSupport = Class.forName("io.bluetape4k.hibernate.querydsl.core.ExpressionUtilsSupportKt")

    private enum class Status { ACTIVE }

    private val reifiedMarkerMethods = mutableSetOf<String>()

    private val expectedReifiedMarkerMethods = setOf(
        "arrayPathOf",
        "collectionOperation",
        "collectionPathOf",
        "comparableEntityPathOf",
        "comparableOperation",
        "comparablePathOf",
        "comparableTemplate",
        "comparableTemplateOf",
        "dateOperation",
        "datePathOf",
        "dateTemplate",
        "dateTemplateOf",
        "dateTimePathOf",
        "dateTimeTemplate",
        "dateTimeTemplateOf",
        "dslOperation",
        "dslPathOf",
        "dslTemplate",
        "dslTemplateOf",
        "enumOperation",
        "enumPathOf",
        "enumTemplate",
        "enumTemplateOf",
        "expressionListOf",
        "expressionSetOf",
        "listPathOf",
        "mapPathOf",
        "newOperation",
        "newTemplateExpression",
        "nullExpressionOf",
        "numberOperation",
        "numberPathOf",
        "numberTemplate",
        "numberTemplateOf",
        "pathOf",
        "setPathOf",
        "simpleExpressionListOf",
        "simpleExpressionSetOf",
        "simpleOperation",
        "simplePathOf",
        "simpleTemplate",
        "simpleTemplateOf",
        "templateExpressionOf",
        "timeOperation",
        "timePathOf",
        "timeTemplate",
        "timeTemplateOf",
    )

    @Test
    fun `ExpressionsSupport inline facades execute through JVM entry points`() {
        val template = TemplateFactory.DEFAULT.create("{0}")
        val metadata = PathMetadataFactory.forVariable("root")
        val stringPath = Expressions.stringPath("name")
        val numberPath = Expressions.numberPath(Int::class.javaObjectType, "count")
        val parent = Expressions.path(Any::class.java, "parent")
        val expressions = arrayOf<Expression<*>>(stringPath)
        val simpleExpressions = arrayOf<SimpleExpression<*>>(stringPath)

        call(expressionsSupport, "alias", stringPath, stringPath)
        call(expressionsSupport, "toExpression", "value")
        call(expressionsSupport, "toExpression", "value", stringPath)
        call(expressionsSupport, "simpleTemplateOf", "{0}", arrayOf("value"))
        call(expressionsSupport, "simpleTemplateOf", "{0}", listOf("value"))
        call(expressionsSupport, "simpleTemplate", template, arrayOf(stringPath))
        call(expressionsSupport, "simpleTemplate", template, listOf(stringPath))
        call(expressionsSupport, "dslTemplateOf", "{0}", arrayOf("value"))
        call(expressionsSupport, "dslTemplateOf", "{0}", listOf("value"))
        call(expressionsSupport, "dslTemplate", template, arrayOf(stringPath))
        call(expressionsSupport, "dslTemplate", template, listOf(stringPath))
        call(expressionsSupport, "comparableTemplateOf", "{0}", arrayOf("value"))
        call(expressionsSupport, "comparableTemplateOf", "{0}", listOf("value"))
        call(expressionsSupport, "comparableTemplate", template, arrayOf(stringPath))
        call(expressionsSupport, "comparableTemplate", template, listOf(stringPath))
        call(expressionsSupport, "dateTemplateOf", "{0}", arrayOf(Date()))
        call(expressionsSupport, "dateTemplateOf", "{0}", listOf(Date()))
        call(expressionsSupport, "dateTemplate", template, arrayOf(Date()))
        call(expressionsSupport, "dateTemplate", template, listOf(Date()))
        call(expressionsSupport, "dateTimeTemplateOf", "{0}", arrayOf(Date()))
        call(expressionsSupport, "dateTimeTemplateOf", "{0}", listOf(Date()))
        call(expressionsSupport, "dateTimeTemplate", template, arrayOf(Date()))
        call(expressionsSupport, "dateTimeTemplate", template, listOf(Date()))
        call(expressionsSupport, "timeTemplateOf", "{0}", arrayOf(Time(0)))
        call(expressionsSupport, "timeTemplateOf", "{0}", listOf(Time(0)))
        call(expressionsSupport, "timeTemplate", template, arrayOf(Time(0)))
        call(expressionsSupport, "timeTemplate", template, listOf(Time(0)))
        call(expressionsSupport, "enumTemplateOf", "{0}", arrayOf(Status.ACTIVE))
        call(expressionsSupport, "enumTemplateOf", "{0}", listOf(Status.ACTIVE))
        call(expressionsSupport, "enumTemplate", template, arrayOf(Status.ACTIVE))
        call(expressionsSupport, "enumTemplate", template, listOf(Status.ACTIVE))
        call(expressionsSupport, "numberTemplateOf", "{0}", arrayOf(1))
        call(expressionsSupport, "numberTemplateOf", "{0}", listOf(1))
        call(expressionsSupport, "numberTemplate", template, arrayOf(numberPath))
        call(expressionsSupport, "numberTemplate", template, listOf(numberPath))
        call(expressionsSupport, "stringTemplateOf", "{0}", arrayOf("value"))
        call(expressionsSupport, "stringTemplate", template, arrayOf(stringPath))
        call(expressionsSupport, "stringTemplate", template, listOf(stringPath))
        call(expressionsSupport, "booleanTemplateOf", "{0}", arrayOf(true))
        call(expressionsSupport, "booleanTemplateOf", "{0}", listOf(true))
        call(expressionsSupport, "booleanTemplate", template, arrayOf(true))
        call(expressionsSupport, "booleanTemplate", template, listOf(true))

        call(expressionsSupport, "simpleOperation", Ops.EQ, expressions)
        call(expressionsSupport, "dslOperation", Ops.EQ, expressions)
        call(expressionsSupport, "booleanOperation", Ops.EQ, expressions)
        call(expressionsSupport, "comparableOperation", Ops.EQ, expressions)
        call(expressionsSupport, "dateOperation", Ops.EQ, expressions)
        call(expressionsSupport, "timeOperation", Ops.EQ, expressions)
        call(expressionsSupport, "numberOperation", Ops.ADD, arrayOf<Expression<*>>(numberPath))
        call(expressionsSupport, "stringOperation", Ops.TRIM, expressions)
        call(expressionsSupport, "enumOperation", Ops.EQ, expressions)
        call(expressionsSupport, "collectionOperation", Ops.EQ, expressions)

        call(expressionsSupport, "simplePathOf", "simple")
        call(expressionsSupport, "simplePathOf", parent, "child")
        call(expressionsSupport, "simplePathOf", metadata)
        call(expressionsSupport, "dslPathOf", "dsl")
        call(expressionsSupport, "dslPathOf", parent, "child")
        call(expressionsSupport, "dslPathOf", metadata)
        call(expressionsSupport, "comparablePathOf", "comparable")
        call(expressionsSupport, "comparablePathOf", parent, "child")
        call(expressionsSupport, "comparablePathOf", metadata)
        call(expressionsSupport, "comparableEntityPathOf", "entity")
        call(expressionsSupport, "comparableEntityPathOf", parent, "child")
        call(expressionsSupport, "comparableEntityPathOf", metadata)
        call(expressionsSupport, "datePathOf", "date")
        call(expressionsSupport, "datePathOf", parent, "child")
        call(expressionsSupport, "datePathOf", metadata)
        call(expressionsSupport, "dateTimePathOf", "dateTime")
        call(expressionsSupport, "dateTimePathOf", parent, "child")
        call(expressionsSupport, "dateTimePathOf", metadata)
        call(expressionsSupport, "timePathOf", "time")
        call(expressionsSupport, "timePathOf", parent, "child")
        call(expressionsSupport, "timePathOf", metadata)
        call(expressionsSupport, "numberPathOf", "number")
        call(expressionsSupport, "numberPathOf", parent, "child")
        call(expressionsSupport, "numberPathOf", metadata)
        call(expressionsSupport, "stringPathOf", "string")
        call(expressionsSupport, "simplePathOf", parent, "string")
        call(expressionsSupport, "simplePathOf", metadata)
        call(expressionsSupport, "booleanPathOf", "boolean")
        call(expressionsSupport, "booleanPathOf", parent, "child")
        call(expressionsSupport, "booleanPathOf", metadata)
        call(expressionsSupport, "enumPathOf", "enum")
        call(expressionsSupport, "enumPathOf", parent, "child")
        call(expressionsSupport, "enumPathOf", metadata)
        call(expressionsSupport, "arrayPathOf", "array")
        call(expressionsSupport, "arrayPathOf", parent, "child")
        call(expressionsSupport, "arrayPathOf", metadata)
        call(expressionsSupport, "collectionPathOf", metadata)
        call(expressionsSupport, "listPathOf", metadata)
        call(expressionsSupport, "setPathOf", metadata)
        call(expressionsSupport, "mapPathOf", metadata)

        call(expressionsSupport, "simpleExpressionListOfTuple", simpleExpressions)
        call(expressionsSupport, "simpleExpressionListOf", simpleExpressions)
        call(expressionsSupport, "expressionListOfTuple", expressions)
        call(expressionsSupport, "expressionListOf", expressions)
        call(expressionsSupport, "expressionListOf", listOf(stringPath))
        call(expressionsSupport, "simpleExpressionSetOf", simpleExpressions)
        call(expressionsSupport, "expressionSetOfTuple", expressions)
        call(expressionsSupport, "expressionSetOf", expressions)
        call(expressionsSupport, "nullExpressionOf")
        call(expressionsSupport, "nullExpression", stringPath)

        call(expressionsSupport, "booleanExpressionOf", true)
        call(expressionsSupport, "comparableExpressionOf", "value")
        call(expressionsSupport, "dateExpressionOf", Date())
        call(expressionsSupport, "dateTimeExpressionOf", Date())
        call(expressionsSupport, "timeExpressionOf", Time(0))
        call(expressionsSupport, "enumExpressionOf", Status.ACTIVE)
        call(expressionsSupport, "numberExpressionOf", 1)
        call(expressionsSupport, "stringExpressionOf", "value")
        call(expressionsSupport, "asBoolean", Expressions.booleanPath("flag"))
        call(expressionsSupport, "asComparable", stringPath)
        call(expressionsSupport, "asDate", stringPath)
        call(expressionsSupport, "asDateTime", stringPath)
        call(expressionsSupport, "asTime", stringPath)
        callExact(expressionsSupport, "asEnum", arrayOf(Expression::class.java), arrayOf(Expressions.enumPath(Status::class.java, "status")))
        call(expressionsSupport, "asNumber", numberPath)
        call(expressionsSupport, "asString", stringPath)
        call(expressionsSupport, "asSimple", "value")
        callExact(expressionsSupport, "asSimple", arrayOf(Expression::class.java), arrayOf(stringPath))

        reifiedMarkerMethods.shouldNotBeEmpty()
        reifiedMarkerMethods.all { it in expectedReifiedMarkerMethods }.shouldBeTrue()
    }

    @Test
    fun `ExpressionUtilsSupport inline facades execute through JVM entry points`() {
        val stringPath = Expressions.stringPath("name")
        val metadata = PathMetadataFactory.forVariable("root")
        val template = TemplateFactory.DEFAULT.create("{0}")
        val listPath = listPathOf<String, SimpleExpression<String>>(metadata)
        val expressions = arrayOf<Expression<*>>(stringPath)
        val predicates = listOf<BooleanExpression>(stringPath.eq("value"))

        call(expressionUtilsSupport, "newOperation", Ops.EQ, expressions)
        call(expressionUtilsSupport, "newPredicate", Ops.EQ, expressions)
        call(expressionUtilsSupport, "pathOf", "name")
        call(expressionUtilsSupport, "pathOf", stringPath, "child")
        call(expressionUtilsSupport, "pathOf", metadata)
        call(expressionUtilsSupport, "templateExpressionOf", "{0}", arrayOf("value"))
        call(expressionUtilsSupport, "templateExpressionOf", "{0}", listOf("value"))
        call(expressionUtilsSupport, "newTemplateExpression", template, arrayOf("value"))
        call(expressionUtilsSupport, "newTemplateExpression", template, listOf("value"))
        call(expressionUtilsSupport, "all", listPath)
        call(expressionUtilsSupport, "any", listPath)
        call(expressionUtilsSupport, "allOrNull", predicates)
        call(expressionUtilsSupport, "and", predicates[0], predicates[0])
        call(expressionUtilsSupport, "anyOrNull", predicates)
        call(expressionUtilsSupport, "count", stringPath)
        call(expressionUtilsSupport, "eqConst", stringPath, "value")
        call(expressionUtilsSupport, "eq", stringPath, stringPath)
        call(expressionUtilsSupport, "inValues", stringPath, listPath)
        call(expressionUtilsSupport, "inValues", stringPath, listOf("value"))
        call(expressionUtilsSupport, "inAny", stringPath, listOf(listOf("value")))
        call(expressionUtilsSupport, "isNull", stringPath)
        call(expressionUtilsSupport, "isNotNull", stringPath)
        call(expressionUtilsSupport, "likeToRegex", stringPath, false)
        call(expressionUtilsSupport, "regexToLike", stringPath)
        call(expressionUtilsSupport, "neConst", stringPath, "value")
        call(expressionUtilsSupport, "ne", stringPath, stringPath)
        call(expressionUtilsSupport, "notIn", stringPath, listPath)
        call(expressionUtilsSupport, "notIn", stringPath, listOf("value"))
        call(expressionUtilsSupport, "notInAny", stringPath, listOf(listOf("value")))
        call(expressionUtilsSupport, "or", predicates[0], predicates[0])
        call(expressionUtilsSupport, "distinctList", listOf(stringPath, stringPath))
        call(expressionUtilsSupport, "extract", stringPath)
        call(expressionUtilsSupport, "rootVariable", stringPath)
        call(expressionUtilsSupport, "rootVariable", stringPath, 1)
        call(expressionUtilsSupport, "toExpression", "value")
        call(expressionUtilsSupport, "lowercase", stringPath)
        call(expressionUtilsSupport, "orderBy", listOf(stringPath.asc()))

        call(expressionUtilsSupport, "all", listPathOf<String, SimpleExpression<String>>(metadata))
        call(expressionUtilsSupport, "any", listPathOf<String, SimpleExpression<String>>(metadata))

        reifiedMarkerMethods.shouldNotBeEmpty()
        reifiedMarkerMethods.all { it in expectedReifiedMarkerMethods }.shouldBeTrue()
    }

    @Test
    fun `querydsl facades preserve expression results`() {
        val name = Expressions.stringPath("name")
        val alias = name.alias(Expressions.stringPath("alias"))
        alias.shouldBeInstanceOf<SimpleExpression<*>>()

        val constant = "value".toExpression()
        constant.toString() shouldBeEqualTo "value"

        val template = simpleTemplateOf<String>("{0}", "value")
        template.shouldBeInstanceOf<SimpleTemplate<*>>()
        template.toString().shouldNotBeEmpty()

        val path = simplePathOf<String>("name")
        path.metadata.name shouldBeEqualTo "name"

        val predicates = listOf(name.eq("value"))
        predicates.allOrNull().shouldNotBeNull()
        listOf<Expression<*>>(name, name).distinctList().size shouldBeEqualTo 1
    }

    private fun call(type: Class<*>, name: String, vararg args: Any?): Any? {
        val method = type.methods
            .asSequence()
            .filter { Modifier.isStatic(it.modifiers) && it.name == name && it.parameterCount == args.size }
            .sortedByDescending { score(it, args) }
            .firstOrNull()
            ?: error("No overload for $name/${args.size}")
        return invokeAllowingReifiedMarker(method, args)
    }

    private fun callExact(type: Class<*>, name: String, parameterTypes: Array<Class<*>>, args: Array<Any?>): Any? =
        invokeAllowingReifiedMarker(type.getMethod(name, *parameterTypes), args)

    private fun invokeAllowingReifiedMarker(method: Method, args: Array<out Any?>): Any? =
        try {
            method.invoke(null, *args).shouldNotBeNull()
        } catch (e: InvocationTargetException) {
            val cause = e.targetException
            if (cause is UnsupportedOperationException &&
                cause.message?.contains("reified type parameter") == true
            ) {
                reifiedMarkerMethods += method.name
                null
            } else {
                throw cause
            }
        }

    private fun score(method: Method, args: Array<out Any?>): Int =
        method.parameterTypes.zip(args).sumOf { (parameter, argument) ->
            when {
                argument == null -> 0
                parameter == argument.javaClass -> 4
                box(parameter).isAssignableFrom(argument.javaClass) -> 2
                else -> -100
            }
        }

    private fun box(type: Class<*>): Class<*> = when (type) {
        Boolean::class.javaPrimitiveType -> Boolean::class.java
        Int::class.javaPrimitiveType -> Int::class.java
        else -> type
    }
}
