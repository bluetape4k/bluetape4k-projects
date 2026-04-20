package io.bluetape4k.aws.dynamodb.query

import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotNull
import software.amazon.awssdk.services.dynamodb.model.QueryRequest

/**
 * [QueryRequest] 를 DSL 형태로 생성하기 위한 함수
 *
 * ```kotlin
 * val request = queryRequest {
 *     tableName = "orders"
 *     primaryKey("pk") { eq("order#1") }
 * }
 *
 * check(request.keyConditions().containsKey("pk"))
 * ```
 */
inline fun queryRequest(builder: QueryRequestBuilderDSL.() -> Unit): QueryRequest =
    QueryRequestBuilderDSL().apply(builder).build()

/** [QueryRequest] 생성용 DSL 상태를 보관하는 빌더입니다. */
@DynamoDslMarker
class QueryRequestBuilderDSL {
    var tableName: String? = null
    var primaryKey: PrimaryKey? = null
    var sortKey: SortKey? = null
    var filtering: RootFilter? = null

    /**
     * 현재 DSL 상태를 [QueryRequest]로 변환합니다.
     *
     * `tableName`, `primaryKey`가 누락되면 예외가 발생합니다.
     */
    fun build(): QueryRequest {
        tableName.requireNotBlank("tableName")
        primaryKey.requireNotNull("primaryKey")

        val request = QueryRequest.builder().tableName(tableName)
        // WHY: requireNotNull 위에서 이미 null이면 예외를 던지므로 !! 대신 안전한 캐스트 사용
        val pk = checkNotNull(primaryKey) { "primaryKey must not be null" }

        if (sortKey == null) {
            request.keyConditions(mapOf(pk.keyName to pk.equals.toCondition()))
        } else {
            val sk = checkNotNull(sortKey) { "sortKey must not be null" }
            request.keyConditions(
                mapOf(
                    pk.keyName to pk.equals.toCondition(),
                    sk.sortKeyName to sk.comparisonOperator.toCondition()
                )
            )
        }

        filtering?.let { filter ->
            val props = filter.getFilterRequestProperties()

            request.filterExpression(props.filterExpression)
            if (props.expressionAttributeNames.isNotEmpty()) {
                request.expressionAttributeNames(props.expressionAttributeNames)
            }
            if (props.expressionAttributeValues.isNotEmpty()) {
                request.expressionAttributeValues(props.expressionAttributeValues)
            }
        }

        return request.build()
    }
}

/** 파티션 키 조건을 설정합니다. */
inline fun QueryRequestBuilderDSL.primaryKey(
    keyName: String,
    builder: PrimaryKeyBuilder.() -> Unit,
) {
    primaryKey = PrimaryKeyBuilder(keyName).apply(builder).build()
}

/** 정렬 키 조건을 설정합니다. */
inline fun QueryRequestBuilderDSL.sortKey(
    keyName: String,
    builder: SortKeyBuilder.() -> Unit,
) {
    sortKey = SortKeyBuilder(keyName).apply(builder).build()
}

/** 필터 조건을 설정합니다. */
inline fun QueryRequestBuilderDSL.filtering(
    builder: RootFilterBuilder.() -> Unit,
) {
    filtering = RootFilterBuilder().apply(builder).build()
}
