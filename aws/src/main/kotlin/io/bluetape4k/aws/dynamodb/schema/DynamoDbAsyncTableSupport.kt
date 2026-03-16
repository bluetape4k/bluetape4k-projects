package io.bluetape4k.aws.dynamodb.schema

import io.bluetape4k.aws.dynamodb.model.provisionedThroughputOf
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable
import java.util.concurrent.CompletableFuture

/**
 * [DynamoDbAsyncTable]을 이용하여 Table을 생성합니다.
 *
 * ```
 * val client: DynamoDbEnhancedAsyncClient
 *
 * override val table: DynamoDbAsyncTable<FoodDocument> by lazy {
 *     client.table("$tablePrefix${Schema.TABLE_NAME}")
 * }
 *
 * table.createTable(100, 100).await()
 * ```
 *
 * @param readCapacityUnits 읽기 용량 단위
 * @param writeCapacityUnits 쓰기 용량 단위
 *
 * @return [CompletableFuture] 인스턴스
 */
fun <T: Any> DynamoDbAsyncTable<T>.createTable(
    readCapacityUnits: Long? = null,
    writeCapacityUnits: Long? = null,
): CompletableFuture<Void> {
    val request = CreateTableEnhancedRequest {
        provisionedThroughput(provisionedThroughputOf(readCapacityUnits, writeCapacityUnits))
    }
    return createTable(request)
}

/**
 * [DynamoDbAsyncTable]에 여러 개의 Item을 저장합니다.
 *
 * ```
 * val client: DynamoDbEnhancedAsyncClient
 *
 * override val table: DynamoDbAsyncTable<FoodDocument> by lazy {
 *     client.table("$tablePrefix${Schema.TABLE_NAME}")
 * }
 *
 * table.createTable(100, 100).await()
 * table.putItems(food1, food2, food3).await()
 * ```
 *
 * @param items 저장할 Item 목록
 * @return [CompletableFuture] 인스턴스
 *
 */
fun <T: Any> DynamoDbAsyncTable<T>.putItems(vararg items: T): CompletableFuture<Void> {
    return putItems(items.asList())
}

/**
 * [DynamoDbAsyncTable]에 여러 개의 Item을 저장합니다.
 *
 * ```
 * val client: DynamoDbEnhancedAsyncClient
 *
 * override val table: DynamoDbAsyncTable<FoodDocument> by lazy {
 *     client.table("$tablePrefix${Schema.TABLE_NAME}")
 * }
 *
 * table.createTable(100, 100).await()
 * table.putItems(listOf(food1, food2, food3)).await()
 * ```
 *
 * @param items 저장할 Item 목록
 * @return [CompletableFuture] 인스턴스
 *
 */
fun <T: Any> DynamoDbAsyncTable<T>.putItems(items: Collection<T>): CompletableFuture<Void> {
    val futures = items.map { putItem(it) }
    return CompletableFuture.allOf(*futures.toTypedArray())
}
