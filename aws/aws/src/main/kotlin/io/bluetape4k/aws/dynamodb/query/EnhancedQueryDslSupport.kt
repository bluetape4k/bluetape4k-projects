package io.bluetape4k.aws.dynamodb.query

import io.bluetape4k.aws.dynamodb.model.DynamoDbEntity
import io.bluetape4k.aws.dynamodb.model.QueryEnhancedRequest
import io.bluetape4k.aws.dynamodb.model.keyOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireNotNull
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

/**
 * Enhanced DynamoDB Query ([QueryEnhancedRequest]) 를 빌드합니다.
 *
 * ```kotlin
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
 * ```kotlin
 * val fromKey = dynamoDbKeyOf(partitionKey, updatedAtFrom.toString())
 * val toKey = dynamoDbKeyOf(partitionKey, updatedAtTo.toString())
 *
 * val queryRequest = queryEnhancedRequest<Food> {
 *      queryConditional(QueryConditional.sortBetween(fromKey, toKey))
 * }
 * ```
 *
 * @param T DynamoDB Entity Type
 * @param builder Enhanced Query Builder DSL
 * @return [QueryEnhancedRequest] 인스턴스
 */
inline fun <T : DynamoDbEntity> queryEnhancedRequest(
    builder: EnhancedQueryBuilderKt<T>.() -> Unit,
): QueryEnhancedRequest = EnhancedQueryBuilderKt<T>().apply(builder).build()

@DynamoDslMarker
class EnhancedQueryBuilderKt<T : Any> {
    companion object : KLogging()

    var primaryKey: PrimaryKey? = null
    var sortKey: SortKey? = null
    var filtering: RootFilter? = null
    var scanIndexForward: Boolean = true
    var lastEvaluatedKey: Map<String, AttributeValue>? = null

    /**
     * 설정된 DSL 상태를 [QueryEnhancedRequest]로 변환합니다.
     *
     * - `primaryKey`는 필수입니다.
     * - `sortKey`가 없으면 `keyEqualTo` 조건을 사용합니다.
     * - `filtering`이 있으면 Enhanced Expression으로 변환해 `filterExpression`에 적용합니다.
     */
    fun build(): QueryEnhancedRequest {
        log.debug { "Start query ...  primaryKey=$primaryKey, sortKey=$sortKey" }
        primaryKey.requireNotNull("primaryKey")

        val pk = primaryKey!!

        return QueryEnhancedRequest {
            val conditional =
                sortKey?.let { sk: SortKey ->

                    when (sk.comparisonOperator) {
                        is BeginsWith -> {
                            QueryConditional.sortBeginsWith(
                                keyOf(pk.equals.right, sk.comparisonOperator.right)
                            )
                        }
                        is GreaterThan -> {
                            QueryConditional.sortGreaterThan(
                                keyOf(pk.equals.right, sk.comparisonOperator.right)
                            )
                        }
                        is GreaterThanOrEquals -> {
                            QueryConditional.sortGreaterThanOrEqualTo(
                                keyOf(pk.equals.right, sk.comparisonOperator.right)
                            )
                        }
                        is LessThan -> {
                            QueryConditional.sortLessThan(
                                keyOf(pk.equals.right, sk.comparisonOperator.right)
                            )
                        }
                        is LessThanOrEquals -> {
                            QueryConditional.sortLessThanOrEqualTo(
                                keyOf(pk.equals.right, sk.comparisonOperator.right)
                            )
                        }
                        is Between -> {
                            QueryConditional.sortBetween(
                                keyOf(sk.sortKeyName, sk.comparisonOperator.left.toString()),
                                keyOf(sk.sortKeyName, sk.comparisonOperator.right.toString())
                            )
                        }
                        else -> {
                            throw UnsupportedOperationException("Unknown comparison operator: ${sk.comparisonOperator}")
                        }
                    }
                } ?: QueryConditional.keyEqualTo(keyOf(pk.equals.right))

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

/** 파티션 키 DSL을 설정합니다. */
inline fun <T : DynamoDbEntity> EnhancedQueryBuilderKt<T>.primaryKey(
    keyName: String = "primaryKey",
    builder: PrimaryKeyBuilder.() -> Unit,
) {
    primaryKey = PrimaryKeyBuilder(keyName).apply(builder).build()
}

/** 정렬 키 DSL을 설정합니다. */
inline fun <T : DynamoDbEntity> EnhancedQueryBuilderKt<T>.sortKey(
    keyName: String = "sortKey",
    builder: SortKeyBuilder.() -> Unit,
) {
    sortKey = SortKeyBuilder(keyName).apply(builder).build()
}

/** 필터 조건 DSL을 설정합니다. */
inline fun <T : DynamoDbEntity> EnhancedQueryBuilderKt<T>.filtering(
    builder: RootFilterBuilder.() -> Unit,
) {
    filtering = RootFilterBuilder().apply(builder).build()
}
