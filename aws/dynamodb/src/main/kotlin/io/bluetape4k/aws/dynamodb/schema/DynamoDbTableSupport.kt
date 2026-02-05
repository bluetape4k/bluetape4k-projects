package io.bluetape4k.aws.dynamodb.schema

import io.bluetape4k.aws.dynamodb.model.provisionedThroughputOf
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedLocalSecondaryIndex
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput

/**
 * [DynamoDbAsyncTable]을 이용하여 Table을 생성합니다.
 *
 * ```
 * val client: DynamoDbClient
 *
 * override val table: DynamoDbTable<FoodDocument> by lazy {
 *     client.table("$tablePrefix${Schema.TABLE_NAME}")
 * }
 *
 * table.createTable(100, 100)
 * ```
 *
 * @param readCapacityUnits 읽기 용량 단위
 * @param writeCapacityUnits 쓰기 용량 단위

 */
fun <T: Any> DynamoDbTable<T>.createTable(
    readCapacityUnits: Long? = null,
    writeCapacityUnits: Long? = null,
) {
    val request = CreateTableEnhancedRequest {
        provisionedThroughput(provisionedThroughputOf(readCapacityUnits, writeCapacityUnits))
    }
    createTable(request)
}

/**
 * [DynamoDbTable]에 여러 개의 Item을 저장합니다.
 */
fun <T: Any> DynamoDbTable<T>.putItems(vararg items: T) {
    putItems(items.asList())
}

/**
 * [DynamoDbTable]에 여러 개의 Item을 저장합니다.
 */
fun <T: Any> DynamoDbTable<T>.putItems(items: Collection<T>) {
    items.forEach { putItem(it) }
}

/**
 * [CreateTableEnhancedRequest]를 생성합니다.
 *
 * ```
 * val request = CreateTableEnhancedRequest {
 *    provisionedThroughput(provisionedThroughputOf(100, 100))
 *    localSecondaryIndices(localSecondaryIndices)
 *    globalSecondaryIndices(globalSecondaryIndices)
 *    // ...
 * }
 *
 * table.createTable(request)
 * ```
 *
 * @return [CreateTableEnhancedRequest] 인스턴스
 */
inline fun CreateTableEnhancedRequest(
    @BuilderInference builder: CreateTableEnhancedRequest.Builder.() -> Unit,
): CreateTableEnhancedRequest {
    return CreateTableEnhancedRequest.builder().apply(builder).build()
}

/**
 * [CreateTableEnhancedRequest]를 생성합니다.
 *
 * ```
 * val request = createTableEnhancedRequestOf(
 *    provisionedThroughput = provisionedThroughputOf(100, 100),
 *    localSecondaryIndices = localSecondaryIndices,
 *    globalSecondaryIndices = globalSecondaryIndices,
 * )
 *
 * table.createTable(request)
 * ```
 *
 * @return [CreateTableEnhancedRequest] 인스턴스
 */
fun createTableEnhancedRequestOf(
    provisionedThroughput: ProvisionedThroughput? = null,
    localSecondaryIndices: Collection<EnhancedLocalSecondaryIndex>? = null,
    globalSecondaryIndices: Collection<EnhancedGlobalSecondaryIndex>? = null,
): CreateTableEnhancedRequest = CreateTableEnhancedRequest {
    provisionedThroughput(provisionedThroughput)
    localSecondaryIndices(localSecondaryIndices)
    globalSecondaryIndices(globalSecondaryIndices)
}
