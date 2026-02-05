package io.bluetape4k.aws.dynamodb.query

import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotNull
import software.amazon.awssdk.services.dynamodb.model.QueryRequest

/**
 * [QueryRequest] 를 DSL 형태로 생성하기 위한 함수
 */
inline fun queryRequest(builder: QueryRequestBuilderDSL.() -> Unit): QueryRequest =
    QueryRequestBuilderDSL().apply(builder).build()

@DynamoDslMarker
class QueryRequestBuilderDSL {
    var tableName: String? = null
    var primaryKey: PrimaryKey? = null
    var sortKey: SortKey? = null
    var filtering: RootFilter? = null

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

inline fun QueryRequestBuilderDSL.primaryKey(
    keyName: String,
    @BuilderInference builder: PrimaryKeyBuilder.() -> Unit,
) {
    primaryKey = PrimaryKeyBuilder(keyName).apply(builder).build()
}

inline fun QueryRequestBuilderDSL.sortKey(
    keyName: String,
    @BuilderInference builder: SortKeyBuilder.() -> Unit,
) {
    sortKey = SortKeyBuilder(keyName).apply(builder).build()
}

inline fun QueryRequestBuilderDSL.filtering(
    @BuilderInference builder: RootFilterBuilder.() -> Unit,
) {
    filtering = RootFilterBuilder().apply(builder).build()
}
