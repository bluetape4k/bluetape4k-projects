package io.bluetape4k.aws.dynamodb.enhanced

import io.bluetape4k.aws.dynamodb.DynamoDb
import io.bluetape4k.aws.dynamodb.DynamoDb.MAX_BATCH_ITEM_SIZE
import io.bluetape4k.aws.dynamodb.model.BatchWriteItemEnhancedRequest
import io.bluetape4k.aws.dynamodb.model.writeBatchOf
import io.bluetape4k.coroutines.flow.extensions.chunked
import io.bluetape4k.support.coerce
import io.bluetape4k.support.requireNotBlank
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.future.await
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension
import software.amazon.awssdk.enhanced.dynamodb.MappedTableResource
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.internal.client.ExtensionResolver
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteResult
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient

/**
 * [DynamoDbEnhancedAsyncClient] 를 생성합니다.
 *
 * ```
 * val client = dynamoDbEnhancedAsyncClient {
 *    dynamoDbClient(DynamoDbAsyncClient.create())
 * }
 * ```
 *
 * @param builder [DynamoDbEnhancedAsyncClient.Builder] 를 초기화하는 람다 함수
 * @return [DynamoDbEnhancedAsyncClient] instance
 */
inline fun dynamoDbEnhancedAsyncClient(
    @BuilderInference builder: DynamoDbEnhancedAsyncClient.Builder.() -> Unit,
): DynamoDbEnhancedAsyncClient {
    return DynamoDbEnhancedAsyncClient.builder().apply(builder).build()
}

/**
 * [DynamoDbEnhancedAsyncClient] 를 생성합니다.
 *
 * ```
 * val client = dynamoDbEnhancedAsyncClientOf(DynamoDbAsyncClient.create()) {
 *   extensions(ExtensionResolver.defaultExtensions())
 * }
 * ```
 *
 * @param client [DynamoDbAsyncClient] instance
 * @param builder [DynamoDbEnhancedAsyncClient.Builder] 를 초기화하는 람다 함수
 * @return [DynamoDbEnhancedAsyncClient] instance
 */
inline fun dynamoDbEnhancedAsyncClientOf(
    client: DynamoDbAsyncClient,
    @BuilderInference builder: DynamoDbEnhancedAsyncClient.Builder.() -> Unit = { extensions(ExtensionResolver.defaultExtensions()) },
): DynamoDbEnhancedAsyncClient =
    dynamoDbEnhancedAsyncClient {
        dynamoDbClient(client)
        builder()
    }

/**
 * [DynamoDbEnhancedAsyncClient] 를 생성합니다.
 *
 * ```
 * val client = dynamoDbEnhancedAsyncClientOf(
 *      DynamoDbAsyncClient.create(),
 *      *ExtensionResolver.defaultExtensions().toTypedArray()
 * )
 * ```
 *
 * @param client [DynamoDbAsyncClient] instance
 * @param extensions [DynamoDbEnhancedClientExtension] extensions
 * @return [DynamoDbEnhancedAsyncClient] instance
 */
fun dynamoDbEnhancedAsyncClientOf(
    client: DynamoDbAsyncClient,
    vararg extensions: DynamoDbEnhancedClientExtension = ExtensionResolver.defaultExtensions().toTypedArray(),
): DynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient {
    dynamoDbClient(client)
    extensions(*extensions)
}


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
    val chunk = chunkSize.coerce(1, MAX_BATCH_ITEM_SIZE)
    return items.asFlow()
        .buffer(chunk)
        .chunked(chunk)
        .map { chunkedItems ->
            val request = BatchWriteItemEnhancedRequest {
                val writeBatch = writeBatchOf(table, chunkedItems, itemClass)
                addWriteBatch(writeBatch)
            }
            batchWriteItem(request).await()
        }
}
