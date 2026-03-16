package io.bluetape4k.aws.dynamodb.query

import io.bluetape4k.aws.dynamodb.model.Expression
import io.bluetape4k.aws.dynamodb.model.toAttributeValue
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import software.amazon.awssdk.enhanced.dynamodb.Expression
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.io.Serializable
import kotlin.random.Random

/**
 * 필터 DSL을 AWS Enhanced Expression으로 변환하기 위한 중간 결과 객체입니다.
 *
 * @property expressionAttributeValues expression value 바인딩 맵
 * @property filterExpression DynamoDB filter expression 문자열
 * @property expressionAttributeNames expression name 바인딩 맵
 */
data class FilterRequestProperties(
    val expressionAttributeValues: MutableMap<String, AttributeValue>,
    val filterExpression: String,
    val expressionAttributeNames: MutableMap<String, String>,
)

/**
 * [FilterRequestProperties]를 AWS Enhanced Client의 [Expression]으로 변환합니다.
 *
 * ```kotlin
 * val expression = requestProperties.toExpression()
 * check(expression.expression().isNotBlank())
 * ```
 */
fun FilterRequestProperties.toExpression(): Expression = Expression {
    expression(filterExpression)
    expressionAttributeNames.takeIf { it.isNotEmpty() }?.let { expressionNames(it) }
    expressionAttributeValues.takeIf { it.isNotEmpty() }?.let { expressionValues(it) }
}

/** 필터 조건 트리 루트/노드 마커 인터페이스입니다. */
@DynamoDslMarker
interface FilterQuery

/**
 * AND/OR로 연결된 필터 트리 루트입니다.
 *
 * [getFilterRequestProperties]는 연결 순서대로 filter expression 문자열과 바인딩 맵을 생성합니다.
 */
@DynamoDslMarker
class RootFilter(val filterConnections: List<FilterConnection>): FilterQuery {

    /**
     * 필터 트리를 순회해 [FilterRequestProperties]를 생성합니다.
     *
     * `filterConnections` 첫 항목은 연결자 없이 시작해야 하며, 이후 항목은 `AND` 또는 `OR` 연결자를 가져야 합니다.
     */
    fun getFilterRequestProperties(): FilterRequestProperties {
        val expressionAttributeValues = mutableMapOf<String, AttributeValue>()
        val expressionAttributeNames = mutableMapOf<String, String>()
        var filterExpression = ""

        fun filter(condition: FilterQuery) {
            when (condition) {
                is RootFilter -> {
                    val nestedProps = condition.getFilterRequestProperties()
                    filterExpression += "(${nestedProps.filterExpression})"
                    expressionAttributeValues.putAll(nestedProps.expressionAttributeValues)
                    expressionAttributeNames.putAll(nestedProps.expressionAttributeNames)
                }

                is ConcreteFilter -> {
                    val nestedProps = condition.getFilterRequestProperties()
                    filterExpression += nestedProps.filterExpression
                    expressionAttributeValues.putAll(nestedProps.expressionAttributeValues)
                    expressionAttributeNames.putAll(nestedProps.expressionAttributeNames)
                }
            }
        }

        val condition = filterConnections.first().value
        filter(condition)

        filterConnections.drop(1)
            .forEach {
                it.connectionToLeft?.let { booleanConnection: FilterBooleanConnection ->
                    filterExpression += " ${booleanConnection.name} "
                    filter(it.value)
                } ?: error("Non head filters without connection to left")
            }

        return FilterRequestProperties(expressionAttributeValues, filterExpression, expressionAttributeNames)
    }
}

/**
 * 단일 속성 기반 필터 조건입니다.
 *
 * [dynamoFunction]과 [comparator] 조합을 expression 문자열로 변환합니다.
 */
@DynamoDslMarker
class ConcreteFilter(
    val dynamoFunction: DynamoFunction,
    val comparator: DynamoComparator? = null,
): FilterQuery {

    companion object: KLogging() {
        private const val source = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
        private val alphabets = ('a' until 'z') + ('A' until 'Z')

        private fun toExprAttrName(attributeName: String): String =
            "#" + generateExprAttrName(attributeName)

        private fun toExprAttrValue(attributeName: String): String =
            ":" + generateExprAttrName(attributeName)

        private fun generateExprAttrName(attributeName: String): String =
            attributeName.filter { it in alphabets } + nonce()

        private fun nonce(length: Int = 5): String = buildString {
            append("__")
            repeat(length) {
                append(source[Random.nextInt(0, source.length)])
            }
        }
    }

    /**
     * 단일 필터를 [FilterRequestProperties]로 변환합니다.
     *
     * 테스트(`DynamoDbQueryDslTest`) 기준으로 변환 결과는
     * expression 문자열, 이름 바인딩, 값 바인딩을 함께 구성합니다.
     */
    fun getFilterRequestProperties(): FilterRequestProperties {
        val expressionAttributeValues = mutableMapOf<String, AttributeValue>()
        val expressionAttributeNames = mutableMapOf<String, String>()
        var filterExpression = ""

        when (dynamoFunction) {
            is Attribute -> {
                val exprAttrName = toExprAttrName(dynamoFunction.attributeName)
                filterExpression += exprAttrName
                expressionAttributeNames[exprAttrName] = dynamoFunction.attributeName

                fun singleValueComparator(operator: String, comparator: SingleValueDynamoComparator) {
                    val exprAttrValue = toExprAttrValue(dynamoFunction.attributeName)
                    filterExpression += " $operator $exprAttrValue"
                    expressionAttributeValues[exprAttrValue] = comparator.right.toAttributeValue()
                }

                when (comparator) {
                    is Equals           -> singleValueComparator("=", comparator)
                    is NotEquals        -> singleValueComparator("<>", comparator)
                    is GreaterThan      -> singleValueComparator(">", comparator)
                    is GreaterThanOrEquals -> singleValueComparator(">=", comparator)
                    is LessThan         -> singleValueComparator("<", comparator)
                    is LessThanOrEquals -> singleValueComparator("<=", comparator)
                    is Between          -> {
                        val leftExprAttrValue = toExprAttrValue(dynamoFunction.attributeName + "left")
                        val rightExprAttrValue = toExprAttrValue(dynamoFunction.attributeName + "right")

                        filterExpression += " BETWEEN $leftExprAttrValue AND $rightExprAttrValue"
                        expressionAttributeValues[leftExprAttrValue] = comparator.left.toAttributeValue()
                        expressionAttributeValues[rightExprAttrValue] = comparator.right.toAttributeValue()
                    }

                    is InList           -> {
                        val attrValues = comparator.right.joinToString {
                            toExprAttrValue(dynamoFunction.attributeName).apply {
                                expressionAttributeValues[this] = it.toAttributeValue()
                            }
                        }

                        filterExpression += " IN ($attrValues)"
                    }
                }
            }

            is AttributeExists -> {
                val exprAttrName = toExprAttrName(dynamoFunction.attributeName)
                filterExpression += " attribute_exists($exprAttrName)"
                expressionAttributeNames[exprAttrName] = dynamoFunction.attributeName
            }

            else         -> {
                log.warn { "Not supported DynamoFunction: $dynamoFunction" }
            }
        }

        return FilterRequestProperties(expressionAttributeValues, filterExpression, expressionAttributeNames)
    }
}

/** `AND X`, `OR (Y AND Z)` 같은 연결 단위를 표현합니다. */
data class FilterConnection(
    val value: FilterQuery,
    val connectionToLeft: FilterBooleanConnection? = null,
): Serializable

/** 조건 연결 연산자입니다. */
enum class FilterBooleanConnection {
    AND,
    OR
}

/** 필터 함수(속성/함수 호출) 마커 인터페이스입니다. */
interface DynamoFunction: Serializable

/** 속성 기반 필터 대상을 지정합니다. */
data class Attribute(val attributeName: String): DynamoFunction

/** `attribute_exists(name)` 필터 함수를 지정합니다. */
data class AttributeExists(val attributeName: String): DynamoFunction

/** 필터 DSL builder 공통 계약입니다. */
@DynamoDslMarker
interface FilterQueryBuilder {
    fun build(): FilterQuery
}

/** 단일 [ConcreteFilter] 빌더입니다. */
@DynamoDslMarker
class ConcreteFilterBuilder: FilterQueryBuilder {
    var dynamoFunction: DynamoFunction? = null
    var comparator: DynamoComparator? = null

    override fun build(): FilterQuery {
        return ConcreteFilter(dynamoFunction!!, comparator)
    }
}

/** `=` 비교 연산자를 설정합니다. */
infix fun ConcreteFilterBuilder.eq(value: Any) {
    comparator = Equals(value)
}

/** `<>` 비교 연산자를 설정합니다. */
infix fun ConcreteFilterBuilder.ne(value: Any) {
    comparator = NotEquals(value)
}

/** `>` 비교 연산자를 설정합니다. */
infix fun ConcreteFilterBuilder.gt(value: Any) {
    comparator = GreaterThan(value)
}

/** `>=` 비교 연산자를 설정합니다. */
infix fun ConcreteFilterBuilder.ge(value: Any) {
    comparator = GreaterThanOrEquals(value)
}

/** `<` 비교 연산자를 설정합니다. */
infix fun ConcreteFilterBuilder.lt(value: Any) {
    comparator = LessThan(value)
}

/** `<=` 비교 연산자를 설정합니다. */
infix fun ConcreteFilterBuilder.le(value: Any) {
    comparator = LessThanOrEquals(value)
}

/** `IN` 비교 연산자를 설정합니다. */
infix fun ConcreteFilterBuilder.inList(values: List<Any>) {
    comparator = InList(values)
}

/** `IN` 비교 연산자를 vararg로 설정합니다. */
fun ConcreteFilterBuilder.inList(vararg values: Any) {
    comparator = InList(values.toList())
}

/**
 * 복합 조건(AND/OR) 루트 빌더입니다.
 *
 * ```kotlin
 * val root = RootFilterBuilder().apply {
 *     attribute("status") { eq("OPEN") } and attributeExists("updatedAt")
 * }.build()
 *
 * check(root.getFilterRequestProperties().filterExpression.isNotBlank())
 * ```
 */
@DynamoDslMarker
class RootFilterBuilder: FilterQueryBuilder {

    var currentFilter: FilterQuery? = null
    var filterQueries = mutableListOf<FilterConnection>()

    override fun build(): RootFilter = RootFilter(filterQueries)

    infix fun and(setup: RootFilterBuilder.() -> Unit): RootFilterBuilder = apply {
        val value = RootFilterBuilder().apply(setup)
        filterQueries.add(FilterConnection(value.build(), FilterBooleanConnection.AND))
    }

    infix fun or(block: RootFilterBuilder.() -> Unit): RootFilterBuilder = apply {
        val value = RootFilterBuilder().apply(block)
        filterQueries.add(FilterConnection(value.build(), FilterBooleanConnection.OR))
    }

    @Suppress("UNUSED_PARAMETER")
    infix fun and(value: RootFilterBuilder): RootFilterBuilder = apply {
        filterQueries.add(FilterConnection(this.currentFilter!!, FilterBooleanConnection.AND))
    }

    @Suppress("UNUSED_PARAMETER")
    infix fun or(value: RootFilterBuilder): RootFilterBuilder = apply {
        filterQueries.add(FilterConnection(this.currentFilter!!, FilterBooleanConnection.OR))
    }
}

/**
 * 속성 필터를 추가합니다.
 *
 * 첫 호출은 루트 조건으로 등록되고, 이후 호출은 `and`/`or` 연결을 위해 [currentFilter]에 저장됩니다.
 */
inline fun RootFilterBuilder.attribute(
    value: String,
    builder: ConcreteFilterBuilder.() -> Unit = {},
): RootFilterBuilder = apply {
    val concreteFilter = ConcreteFilterBuilder().apply(builder)
    concreteFilter.dynamoFunction = Attribute(value)

    if (filterQueries.isEmpty()) {
        filterQueries.add(FilterConnection(concreteFilter.build(), null))
    } else {
        currentFilter = concreteFilter.build()
    }
}

/**
 * `attribute_exists(name)` 필터를 현재 조건으로 등록합니다.
 */
infix fun RootFilterBuilder.attributeExists(value: String): RootFilterBuilder = apply {
    this.currentFilter = ConcreteFilter(AttributeExists(value))
}
