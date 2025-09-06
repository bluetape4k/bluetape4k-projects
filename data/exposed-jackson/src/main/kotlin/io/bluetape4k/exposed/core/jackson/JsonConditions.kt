package io.bluetape4k.exposed.core.jackson

import org.jetbrains.exposed.v1.core.ComplexExpression
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.asLiteral
import org.jetbrains.exposed.v1.core.stringLiteral
import org.jetbrains.exposed.v1.core.vendors.currentDialect


// Operator Classes

class Contains(
    val target: Expression<*>,
    val candidate: Expression<*>,
    val path: String?,
    val jsonType: IColumnType<*>,
): Op<Boolean>(), ComplexExpression {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) =
        currentDialect.functionProvider.jsonContains(target, candidate, path, jsonType, queryBuilder)
}

class Exists(
    val expression: Expression<*>,
    vararg val path: String,
    val optional: String?,
    val jsonType: IColumnType<*>,
): Op<Boolean>(), ComplexExpression {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) =
        currentDialect.functionProvider.jsonExists(expression, path = path, optional, jsonType, queryBuilder)
}

// Extension Functions

fun ExpressionWithColumnType<*>.contains(
    candidate: Expression<*>,
    path: String? = null,
): Contains =
    Contains(this, candidate, path, columnType)

fun <T> ExpressionWithColumnType<*>.contains(
    candidate: T,
    path: String? = null,
): Contains = when (candidate) {
//    is Iterable<*>, is Array<*> -> Contains(this, stringLiteral(asLiteral(candidate).toString()), path, columnType)
    is String -> Contains(this, stringLiteral(candidate), path, columnType)
    else -> Contains(this, asLiteral(candidate), path, columnType)
}

fun ExpressionWithColumnType<*>.exists(vararg path: String, optional: String? = null): Exists =
    Exists(this, path = path, optional, columnType)
