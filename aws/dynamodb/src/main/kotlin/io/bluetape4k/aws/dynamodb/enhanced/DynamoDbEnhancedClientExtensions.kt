package io.bluetape4k.aws.dynamodb.enhanced

import io.bluetape4k.aws.dynamodb.DynamoDb
import io.bluetape4k.aws.dynamodb.DynamoDb.MAX_BATCH_ITEM_SIZE
import io.bluetape4k.aws.dynamodb.model.BatchWriteItemEnhancedRequest
import io.bluetape4k.aws.dynamodb.model.writeBatchOf
import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.MappedTableResource
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteResult

/**
 * Create DynamoDb Table with specific name ([tableName])
 *
 * @param T entity type
 * @param tableName table name
 * @return [DynamoDbTable] instance
 */
inline fun <reified T: Any> DynamoDbEnhancedClient.table(tableName: String): DynamoDbTable<T> {
    tableName.requireNotBlank("tableName")
    return table(tableName, TableSchema.fromBean(T::class.java))
}

/**
 * 대량의 Item 을 저장할 때, [DynamoDb.MAX_BATCH_ITEM_SIZE] 만큼의 크기로 나누어 저장한다.
 *
 * @param T  entity type
 * @param itemClass entity class
 * @param table [MappedTableResource] instance
 * @param items 저장할 item 컬렉션
 * @param chunkSize [DynamoDb.MAX_BATCH_ITEM_SIZE] 보다 작은 값을 사용해야 한다 (1~25)
 * @return [BatchWriteResult] 컬렉션
 */
fun <T: Any> DynamoDbEnhancedClient.batchWriteItems(
    itemClass: Class<T>,
    table: MappedTableResource<T>,
    items: Collection<T>,
    chunkSize: Int = MAX_BATCH_ITEM_SIZE,
): List<BatchWriteResult> {
    val chunk = chunkSize.coerceIn(1, MAX_BATCH_ITEM_SIZE)
    return items
        .chunked(chunk)
        .map { chunkedItems ->
            val request =
                BatchWriteItemEnhancedRequest {
                    addWriteBatch(writeBatchOf(table, chunkedItems, itemClass))
                }
            batchWriteItem(request)
        }
}

/**
 * 대량의 Item 을 저장할 때, [DynamoDb.MAX_BATCH_ITEM_SIZE] 만큼의 크기로 나누어 저장한다.
 *
 * @param T entity type
 * @param table [MappedTableResource] instance
 * @param items 저장할 item 컬렉션
 * @param chunkSize [DynamoDb.MAX_BATCH_ITEM_SIZE] 보다 작은 값을 사용해야 한다 (1~25)
 * @return [BatchWriteResult] 컬렉션
 */
inline fun <reified T: Any> DynamoDbEnhancedClient.batchWriteItems(
    table: MappedTableResource<T>,
    items: Collection<T>,
    chunkSize: Int = MAX_BATCH_ITEM_SIZE,
): List<BatchWriteResult> = batchWriteItems(T::class.java, table, items, chunkSize)

/**
 * 테이블이 존재하는지 확인합니다.
 *
 * @param tableName 확인할 테이블 이름
 * @return 존재 여부
 */
fun DynamoDbEnhancedClient.existsTable(tableName: String): Boolean =
    runCatching {
        table<Any>(tableName).describeTable()
        true
    }.getOrDefault(false)
