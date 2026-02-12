package io.bluetape4k.aws.dynamodb.enhanced

import io.bluetape4k.aws.dynamodb.DynamoDb
import io.bluetape4k.aws.dynamodb.DynamoDb.MAX_BATCH_ITEM_SIZE
import io.bluetape4k.aws.dynamodb.model.BatchWriteItemEnhancedRequest
import io.bluetape4k.aws.dynamodb.model.writeBatchOf
import io.bluetape4k.coroutines.flow.extensions.chunked
import io.bluetape4k.support.requireNotBlank
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.future.await
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient
import software.amazon.awssdk.enhanced.dynamodb.MappedTableResource
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteResult

/**
 * ([tableName]) 이름의 DynamoDb Table 을 생성합니다.
 *
 * @param T DynamoDB Table 의 Entity Type
 * @param tableName 테이블 이름
 * @return [DynamoDbAsyncTable] instance
 */
inline fun <reified T: Any> DynamoDbEnhancedAsyncClient.table(tableName: String): DynamoDbAsyncTable<T> {
    tableName.requireNotBlank("tableName")
    return table(tableName, TableSchema.fromBean(T::class.java))
}

/**
 * 대량의 Item 을 저장할 때, [DynamoDb.MAX_BATCH_ITEM_SIZE] 만큼의 크기로 나누어 저장한다.
 *
 * @param T
 * @param itemClass entity class
 * @param table [DynamoDbAsyncTable] instance
 * @param items 저장할 item 컬렉션
 * @param chunkSize [DynamoDb.MAX_BATCH_ITEM_SIZE] 보다 작은 값을 사용해야 한다 (1~25)
 * @return [BatchWriteResult] 컬렉션
 */
fun <T: Any> DynamoDbEnhancedAsyncClient.batchWriteItems(
    itemClass: Class<T>,
    table: MappedTableResource<T>,
    items: Collection<T>,
    chunkSize: Int = MAX_BATCH_ITEM_SIZE,
): Flow<BatchWriteResult> {
    val chunk = chunkSize.coerceIn(1, MAX_BATCH_ITEM_SIZE)

    return items
        .asFlow()
        .buffer(chunk)
        .chunked(chunk)
        .map { chunkedItems ->
            val request =
                BatchWriteItemEnhancedRequest {
                    addWriteBatch(writeBatchOf(table, chunkedItems, itemClass))
                }
            batchWriteItem(request).await()
        }
}

/**
 * 대량의 Item 을 저장할 때, [DynamoDb.MAX_BATCH_ITEM_SIZE] 만큼의 크기로 나누어 저장한다.
 *
 * @param T
 * @param table [DynamoDbAsyncTable] instance
 * @param items 저장할 item 컬렉션
 * @param chunkSize [DynamoDb.MAX_BATCH_ITEM_SIZE] 보다 작은 값을 사용해야 한다 (1~25)
 * @return [BatchWriteResult] 컬렉션
 */
inline fun <reified T: Any> DynamoDbEnhancedAsyncClient.batchWriteItems(
    table: MappedTableResource<T>,
    items: Collection<T>,
    chunkSize: Int = MAX_BATCH_ITEM_SIZE,
): Flow<BatchWriteResult> =
    batchWriteItems(T::class.java, table, items, chunkSize)
