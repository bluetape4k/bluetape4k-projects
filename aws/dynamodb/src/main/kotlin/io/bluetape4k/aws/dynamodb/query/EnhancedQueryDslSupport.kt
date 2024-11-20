package io.bluetape4k.aws.dynamodb.query

import io.bluetape4k.aws.dynamodb.model.DynamoDbEntity
import io.bluetape4k.aws.dynamodb.model.QueryEnhancedRequest
import io.bluetape4k.aws.dynamodb.model.dynamoDbKeyOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireNotNull
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

/**
 * Enhanced DynamoDB Query ([QueryEnhancedRequest]) 를 빌드합니다.
 *
 * ```
 * val queryRequest = queryEnhancedRequest<DynamoDbEntity> {
 *    primaryKey("primaryKey") {
 *          equals("value")
 *    }
 *    sortKey("sortKey") {
 *      greaterThan("value")
 *      // or
 *      between("value1", "value2")
 *      // or
 *      beginsWith("value")
 *    }
 *    filtering {
 *      and {
 *          eq("key1", "value1")
 *          eq("key2", "value2")
 *          lt("key3", "value3")
 *          gt("key4", "value4")
 *          // ...
 *      }
 *      // or
 *    }
 *    scanIndexForward = true
 * }
 * ```
 *
 * ```
 * val fromKey = dynamoDbKeyOf(partitionKey, updatedAtFrom.toString())
 * val toKey = dynamoDbKeyOf(partitionKey, updatedAtTo.toString())
 *
 * val queryRequest = queryEnhancedRequest<Food> {
 *      queryConditional(QueryConditional.sortBetween(fromKey, toKey))
 * }
 * ```
 *
 * @param T DynamoDB Entity Type
 * @param initializer Enhanced Query Builder DSL
 * @return [QueryEnhancedRequest] 인스턴스
 */
fun <T: DynamoDbEntity> queryEnhancedRequest(
    initializer: EnhancedQueryBuilderKt<T>.() -> Unit,
): QueryEnhancedRequest {
    return EnhancedQueryBuilderKt<T>().apply(initializer).build()
}

@DynamoDslMarker
class EnhancedQueryBuilderKt<T: Any> {

    companion object: KLogging()

    var primaryKey: PrimaryKey? = null
    var sortKey: SortKey? = null
    var filtering: RootFilter? = null
    var scanIndexForward: Boolean = true
    var lastEvaluatedKey: Map<String, AttributeValue>? = null

    fun build(): QueryEnhancedRequest {
        log.debug { "Start query ...  primaryKey=$primaryKey, sortKey=$sortKey" }
        primaryKey.requireNotNull("primaryKey")

        return QueryEnhancedRequest {
            val conditional = sortKey?.let { sk: SortKey ->

                when (sk.comparisonOperator) {
                    is BeginsWith          -> QueryConditional.sortBeginsWith(
                        dynamoDbKeyOf(primaryKey!!.equals.right, sk.comparisonOperator.right)
                    )

                    is GreaterThan         -> QueryConditional.sortGreaterThan(
                        dynamoDbKeyOf(primaryKey!!.equals.right, sk.comparisonOperator.right)
                    )

                    is GreaterThanOrEquals -> QueryConditional.sortGreaterThanOrEqualTo(
                        dynamoDbKeyOf(primaryKey!!.equals.right, sk.comparisonOperator.right)
                    )

                    is LessThan            -> QueryConditional.sortLessThan(
                        dynamoDbKeyOf(primaryKey!!.equals.right, sk.comparisonOperator.right)
                    )

                    is LessThanOrEquals    -> QueryConditional.sortLessThanOrEqualTo(
                        dynamoDbKeyOf(primaryKey!!.equals.right, sk.comparisonOperator.right)
                    )

                    is Between             -> QueryConditional.sortBetween(
                        dynamoDbKeyOf(sk.sortKeyName, sk.comparisonOperator.left.toString()),
                        dynamoDbKeyOf(sk.sortKeyName, sk.comparisonOperator.right.toString())
                    )

                    else                   ->
                        throw UnsupportedOperationException("Unknown comparison operator: ${sk.comparisonOperator}")
                }
            } ?: QueryConditional.keyEqualTo(dynamoDbKeyOf(primaryKey!!.equals.right))

            queryConditional(conditional)

            filtering?.let { filter ->
                val props = filter.getFilterRequestProperties()
                filterExpression(props.toExpression())
            }

            scanIndexForward(scanIndexForward)
            lastEvaluatedKey?.let { exclusiveStartKey(it) }
        }
    }
}

inline fun <T: DynamoDbEntity> EnhancedQueryBuilderKt<T>.primaryKey(
    keyName: String = "primaryKey",
    initializer: PrimaryKeyBuilder.() -> Unit,
) {
    primaryKey = PrimaryKeyBuilder(keyName).apply(initializer).build()
}

inline fun <T: DynamoDbEntity> EnhancedQueryBuilderKt<T>.sortKey(
    keyName: String = "sortKey",
    initializer: SortKeyBuilder.() -> Unit,
) {
    sortKey = SortKeyBuilder(keyName).apply(initializer).build()
}

inline fun <T: DynamoDbEntity> EnhancedQueryBuilderKt<T>.filtering(
    initializer: RootFilterBuilder.() -> Unit,
) {
    filtering = RootFilterBuilder().apply(initializer).build()
}
