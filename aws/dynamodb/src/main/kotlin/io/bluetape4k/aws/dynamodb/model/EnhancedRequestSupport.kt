package io.bluetape4k.aws.dynamodb.model

import software.amazon.awssdk.enhanced.dynamodb.Expression
import software.amazon.awssdk.enhanced.dynamodb.MappedTableResource
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetItemEnhancedRequest
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest
import software.amazon.awssdk.enhanced.dynamodb.model.ReadBatch
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest
import software.amazon.awssdk.enhanced.dynamodb.model.TransactGetItemsEnhancedRequest
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

inline fun BatchGetItemEnhancedRequest(
    @BuilderInference builder: BatchGetItemEnhancedRequest.Builder.() -> Unit,
): BatchGetItemEnhancedRequest {
    return BatchGetItemEnhancedRequest.builder().apply(builder).build()
}

inline fun BatchWriteItemEnhancedRequest(
    @BuilderInference builder: BatchWriteItemEnhancedRequest.Builder.() -> Unit,
): BatchWriteItemEnhancedRequest {
    return BatchWriteItemEnhancedRequest.builder().apply(builder).build()
}

inline fun CreateTableEnhancedRequest(
    @BuilderInference builder: CreateTableEnhancedRequest.Builder.() -> Unit,
): CreateTableEnhancedRequest {
    return CreateTableEnhancedRequest.builder().apply(builder).build()
}

inline fun DeleteItemEnhancedRequest(
    @BuilderInference builder: DeleteItemEnhancedRequest.Builder.() -> Unit,
): DeleteItemEnhancedRequest {
    return DeleteItemEnhancedRequest.builder().apply(builder).build()
}

inline fun Expression(
    @BuilderInference builder: Expression.Builder.() -> Unit,
): Expression {
    return Expression.builder().apply(builder).build()
}

inline fun GetItemEnhancedRequest(
    @BuilderInference builder: GetItemEnhancedRequest.Builder.() -> Unit,
): GetItemEnhancedRequest {
    return GetItemEnhancedRequest.builder().apply(builder).build()
}

inline fun <reified T: Any> ReadBatch(
    table: MappedTableResource<T>,
    @BuilderInference builder: ReadBatch.Builder<T>.() -> Unit,
): ReadBatch {
    return ReadBatch.builder(T::class.java)
        .apply { mappedTableResource(table) }
        .apply(builder)
        .build()
}

inline fun <reified T: Any> PutItemEnhancedRequest(
    @BuilderInference builder: PutItemEnhancedRequest.Builder<T>.() -> Unit,
): PutItemEnhancedRequest<T> {
    return PutItemEnhancedRequest.builder(T::class.java).apply(builder).build()
}

inline fun QueryEnhancedRequest(
    @BuilderInference builder: QueryEnhancedRequest.Builder.() -> Unit,
): QueryEnhancedRequest {
    return QueryEnhancedRequest.builder().apply(builder).build()
}

/**
 * Create [QueryEnhancedRequest] instance with default values.
 */
fun queryEhnahcedRequestOf(
    queryConditional: QueryConditional? = null,
    exclusiveStartKey: Map<String, AttributeValue>? = null,
    scanIndexForward: Boolean? = null,
    limit: Int? = null,
    consistentRead: Boolean? = null,
    filterExpression: Expression? = null,
    attributesToProject: Collection<String>? = null,
): QueryEnhancedRequest = QueryEnhancedRequest {
    queryConditional(queryConditional)
    exclusiveStartKey(exclusiveStartKey)
    scanIndexForward(scanIndexForward)
    limit(limit)
    consistentRead(consistentRead)
    filterExpression(filterExpression)
    attributesToProject(attributesToProject)
}

/**
 * [QueryEnhancedRequest]의 정보를 문자열로 표현합니다.
 */
fun QueryEnhancedRequest.describe(): String = buildString {
    appendLine()
    append("queryConditional: ").appendLine(queryConditional())
    append("filters expression: ").appendLine(filterExpression().expression())
    append("filters expression names: ").appendLine(filterExpression().expressionNames())
    append("filters expression values: ").appendLine(filterExpression().expressionValues())
    append("limit: ").appendLine(limit() ?: -1)
    append("scanIndexForward: ").appendLine(scanIndexForward())
    append("attributesToProject: ").appendLine(attributesToProject())
    append("consistentRead: ").appendLine(consistentRead())
}

inline fun ScanEnhancedRequest(
    @BuilderInference builder: ScanEnhancedRequest.Builder.() -> Unit,
): ScanEnhancedRequest {
    return ScanEnhancedRequest.builder().apply(builder).build()
}

inline fun TransactGetItemsEnhancedRequest(
    @BuilderInference builder: TransactGetItemsEnhancedRequest.Builder.() -> Unit,
): TransactGetItemsEnhancedRequest {
    return TransactGetItemsEnhancedRequest.builder().apply(builder).build()
}

inline fun TransactWriteItemsEnhancedRequest(
    @BuilderInference builder: TransactWriteItemsEnhancedRequest.Builder.() -> Unit,
): TransactWriteItemsEnhancedRequest {
    return TransactWriteItemsEnhancedRequest.builder().apply(builder).build()
}

inline fun <reified T: Any> UpdateItemEnhancedRequest(
    @BuilderInference builder: UpdateItemEnhancedRequest.Builder<T>.() -> Unit,
): UpdateItemEnhancedRequest<T> {
    return UpdateItemEnhancedRequest
        .builder(T::class.java)
        .apply(builder)
        .build()
}
