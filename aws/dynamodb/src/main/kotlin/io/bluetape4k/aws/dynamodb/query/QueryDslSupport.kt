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

        if (sortKey == null) {
            request.keyConditions(mapOf(primaryKey!!.keyName to primaryKey!!.equals.toCondition()))
        } else {
            request.keyConditions(
                mapOf(
                    primaryKey!!.keyName to primaryKey!!.equals.toCondition(),
                    sortKey!!.sortKeyName to sortKey!!.comparisonOperator.toCondition()
                )
            )
        }

        if (filtering != null) {
            val props = filtering!!.getFilterRequestProperties()

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
    @BuilderInference builder: PrimaryKeyBuilder.() -> Unit,
) {
    primaryKey = PrimaryKeyBuilder(keyName).apply(builder).build()
}

/** 정렬 키 조건을 설정합니다. */
inline fun QueryRequestBuilderDSL.sortKey(
    keyName: String,
    @BuilderInference builder: SortKeyBuilder.() -> Unit,
) {
    sortKey = SortKeyBuilder(keyName).apply(builder).build()
}

/** 필터 조건을 설정합니다. */
inline fun QueryRequestBuilderDSL.filtering(
    @BuilderInference builder: RootFilterBuilder.() -> Unit,
) {
    filtering = RootFilterBuilder().apply(builder).build()
}
