package io.bluetape4k.aws.dynamodb.query

import io.bluetape4k.aws.dynamodb.model.Expression
import io.bluetape4k.aws.dynamodb.model.toAttributeValue
import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.collections.eclipse.unifiedMapOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import software.amazon.awssdk.enhanced.dynamodb.Expression
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import kotlin.random.Random

data class FilterRequestProperties(
    val expressionAttributeValues: MutableMap<String, AttributeValue>,
    val filterExpression: String,
    val expressionAttributeNames: MutableMap<String, String>,
)

fun FilterRequestProperties.toExpression(): Expression = Expression {
    expression(filterExpression)
    expressionAttributeNames.takeIf { it.isNotEmpty() }?.let { expressionNames(it) }
    expressionAttributeValues.takeIf { it.isNotEmpty() }?.let { expressionValues(it) }
}

@DynamoDslMarker
interface FilterQuery

@DynamoDslMarker
class RootFilter(val filterConnections: List<FilterConnection>): FilterQuery {

    fun getFilterRequestProperties(): FilterRequestProperties {
        val expressionAttributeValues = unifiedMapOf<String, AttributeValue>()
        val expressionAttributeNames = unifiedMapOf<String, String>()
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

    fun getFilterRequestProperties(): FilterRequestProperties {
        val expressionAttributeValues = unifiedMapOf<String, AttributeValue>()
        val expressionAttributeNames = unifiedMapOf<String, String>()
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
                    is Equals -> singleValueComparator("=", comparator)
                    is NotEquals -> singleValueComparator("<>", comparator)
                    is GreaterThan -> singleValueComparator(">", comparator)
                    is GreaterThanOrEquals -> singleValueComparator(">=", comparator)
                    is LessThan -> singleValueComparator("<", comparator)
                    is LessThanOrEquals -> singleValueComparator("<=", comparator)
                    is Between -> {
                        val leftExprAttrValue = toExprAttrValue(dynamoFunction.attributeName + "left")
                        val rightExprAttrValue = toExprAttrValue(dynamoFunction.attributeName + "right")

                        filterExpression += " BETWEEN $leftExprAttrValue AND $rightExprAttrValue"
                        expressionAttributeValues[leftExprAttrValue] = comparator.left.toAttributeValue()
                        expressionAttributeValues[rightExprAttrValue] = comparator.right.toAttributeValue()
                    }

                    is InList -> {
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

            else -> {
                log.warn { "Not supported DynamoFunction: $dynamoFunction" }
            }
        }

        return FilterRequestProperties(expressionAttributeValues, filterExpression, expressionAttributeNames)
    }
}

// Represents a connector and an individual condition 'AND X', 'OR (Y AND Z)', ...
data class FilterConnection(
    val value: FilterQuery,
    val connectionToLeft: FilterBooleanConnection? = null,
)

enum class FilterBooleanConnection {
    AND, OR
}

interface DynamoFunction

data class Attribute(val attributeName: String): DynamoFunction
data class AttributeExists(val attributeName: String): DynamoFunction

@DynamoDslMarker
interface FilterQueryBuilder {
    fun build(): FilterQuery
}

@DynamoDslMarker
class ConcreteFilterBuilder: FilterQueryBuilder {
    var dynamoFunction: DynamoFunction? = null
    var comparator: DynamoComparator? = null

    override fun build(): FilterQuery {
        return ConcreteFilter(dynamoFunction!!, comparator)
    }
}

infix fun ConcreteFilterBuilder.eq(value: Any) {
    comparator = Equals(value)
}

infix fun ConcreteFilterBuilder.ne(value: Any) {
    comparator = NotEquals(value)
}

infix fun ConcreteFilterBuilder.gt(value: Any) {
    comparator = GreaterThan(value)
}

infix fun ConcreteFilterBuilder.ge(value: Any) {
    comparator = GreaterThanOrEquals(value)
}

infix fun ConcreteFilterBuilder.lt(value: Any) {
    comparator = LessThan(value)
}

infix fun ConcreteFilterBuilder.le(value: Any) {
    comparator = LessThanOrEquals(value)
}

infix fun ConcreteFilterBuilder.inList(values: List<Any>) {
    comparator = InList(values)
}

fun ConcreteFilterBuilder.inList(vararg values: Any) {
    comparator = InList(values.toFastList())
}

@DynamoDslMarker
class RootFilterBuilder: FilterQueryBuilder {

    var currentFilter: FilterQuery? = null
    var filterQueries = fastListOf<FilterConnection>()

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


inline fun RootFilterBuilder.attribute(
    value: String,
    @BuilderInference builder: ConcreteFilterBuilder.() -> Unit = {},
): RootFilterBuilder = apply {
    val concreteFilter = ConcreteFilterBuilder().apply(builder)
    concreteFilter.dynamoFunction = Attribute(value)

    if (filterQueries.isEmpty()) {
        filterQueries.add(FilterConnection(concreteFilter.build(), null))
    } else {
        currentFilter = concreteFilter.build()
    }
}

infix fun RootFilterBuilder.attributeExists(value: String): RootFilterBuilder = apply {
    this.currentFilter = ConcreteFilter(AttributeExists(value))
}
