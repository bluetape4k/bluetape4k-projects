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
    initializer: BatchGetItemEnhancedRequest.Builder.() -> Unit,
): BatchGetItemEnhancedRequest {
    return BatchGetItemEnhancedRequest.builder().apply(initializer).build()
}

inline fun BatchWriteItemEnhancedRequest(
    initializer: BatchWriteItemEnhancedRequest.Builder.() -> Unit,
): BatchWriteItemEnhancedRequest {
    return BatchWriteItemEnhancedRequest.builder().apply(initializer).build()
}

inline fun CreateTableEnhancedRequest(
    initializer: CreateTableEnhancedRequest.Builder.() -> Unit,
): CreateTableEnhancedRequest {
    return CreateTableEnhancedRequest.builder().apply(initializer).build()
}

inline fun DeleteItemEnhancedRequest(
    initializer: DeleteItemEnhancedRequest.Builder.() -> Unit,
): DeleteItemEnhancedRequest {
    return DeleteItemEnhancedRequest.builder().apply(initializer).build()
}

inline fun Expression(
    initializer: Expression.Builder.() -> Unit,
): Expression {
    return Expression.builder().apply(initializer).build()
}

inline fun GetItemEnhancedRequest(
    initializer: GetItemEnhancedRequest.Builder.() -> Unit,
): GetItemEnhancedRequest {
    return GetItemEnhancedRequest.builder().apply(initializer).build()
}

inline fun <reified T: Any> ReadBatch(
    table: MappedTableResource<T>,
    @BuilderInference initializer: ReadBatch.Builder<T>.() -> Unit,
): ReadBatch {
    return ReadBatch.builder(T::class.java)
        .apply { mappedTableResource(table) }
        .apply(initializer)
        .build()
}

inline fun <reified T: Any> PutItemEnhancedRequest(
    @BuilderInference initializer: PutItemEnhancedRequest.Builder<T>.() -> Unit,
): PutItemEnhancedRequest<T> {
    return PutItemEnhancedRequest.builder(T::class.java).apply(initializer).build()
}

inline fun QueryEnhancedRequest(
    initializer: QueryEnhancedRequest.Builder.() -> Unit,
): QueryEnhancedRequest {
    return QueryEnhancedRequest.builder().apply(initializer).build()
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
    append("consistentRead: ").appendLine(consistentRead() ?: "")
}

inline fun ScanEnhancedRequest(
    initializer: ScanEnhancedRequest.Builder.() -> Unit,
): ScanEnhancedRequest {
    return ScanEnhancedRequest.builder().apply(initializer).build()
}

inline fun TransactGetItemsEnhancedRequest(
    initializer: TransactGetItemsEnhancedRequest.Builder.() -> Unit,
): TransactGetItemsEnhancedRequest {
    return TransactGetItemsEnhancedRequest.builder().apply(initializer).build()
}

inline fun TransactWriteItemsEnhancedRequest(
    initializer: TransactWriteItemsEnhancedRequest.Builder.() -> Unit,
): TransactWriteItemsEnhancedRequest {
    return TransactWriteItemsEnhancedRequest.builder().apply(initializer).build()
}

inline fun <reified T: Any> UpdateItemEnhancedRequest(
    initializer: UpdateItemEnhancedRequest.Builder<T>.() -> Unit,
): UpdateItemEnhancedRequest<T> {
    return UpdateItemEnhancedRequest.builder(T::class.java).apply(initializer).build()
}
