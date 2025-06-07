package io.bluetape4k.aws.dynamodb

import io.bluetape4k.aws.dynamodb.DynamoDb.MAX_BATCH_ITEM_SIZE
import io.bluetape4k.aws.dynamodb.model.BatchWriteItemRequest
import io.bluetape4k.aws.dynamodb.model.WriteRequest
import io.bluetape4k.aws.dynamodb.model.writeRequestOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.github.resilience4j.kotlin.retry.executeSuspendFunction
import io.github.resilience4j.retry.Retry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.WriteRequest

/**
 * Coroutine 환경에서 DynamoDB 에 배치 작업을 수행하는 Executor
 *
 * @param T  작업할 DynamoDB Item Type
 * @property dynamoDB [DynamoDbClient] 인스턴스
 * @property retry  Resilience4j [Retry] 인스턴스
 */
class DynamoDbBatchExecutor<T: Any>(
    private val dynamoDB: DynamoDbClient,
    private val retry: Retry,
): CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()) {

    companion object: KLoggingChannel() {
        @JvmStatic
        operator fun <T: Any> invoke(
            dynamoDB: DynamoDbClient = DynamoDbClient.create(),
            retry: Retry = Retry.ofDefaults("dynamo-batch"),
        ): DynamoDbBatchExecutor<T> {
            return DynamoDbBatchExecutor(dynamoDB, retry)
        }
    }

    data class TableItemTuple(val tableName: String, val writeRequest: WriteRequest)
    data class RetryablePut(val attempt: Int, val items: List<TableItemTuple>)

    /**
     * [tableName] table에서 [items] 를 삭제하는 작업을 Batch로 수행합니다.
     *
     * @param tableName  Table name
     * @param items      삭제할 Items
     * @param primaryKeySelector primary key selector
     */
    suspend fun delete(tableName: String, items: List<T>, primaryKeySelector: (T) -> Map<String, AttributeValue>) {
        val writeRequests = items
            .map { item ->
                WriteRequest {
                    io.bluetape4k.aws.dynamodb.model.DeleteRequest { key(primaryKeySelector(item)) }
                }
            }
            .map { TableItemTuple(tableName, it) }

        writeRequests
            .chunked(MAX_BATCH_ITEM_SIZE)
            .forEach {
                executeBatchPersist(it)
            }
    }

    suspend fun persist(tableName: String, items: List<T>, mapper: DynamoItemMapper<T>) {
        val writeItems = items.buildWriteRequest(mapper)
        persist(writeItems.map { TableItemTuple(tableName, it) })
    }

    suspend fun persist(tableName: String, items: List<Map<String, AttributeValue>>) {
        val writeItems = items.map { writeRequestOf(it) }
        persist(writeItems.map { TableItemTuple(tableName, it) })
    }

    suspend fun persist(writeItems: List<TableItemTuple>) {
        writeItems
            .chunked(MAX_BATCH_ITEM_SIZE)
            .asFlow()
            .buffer()
            .onEach { executeBatchPersist(it) }
            .collect()
    }

    private suspend fun executeBatchPersist(writeList: List<TableItemTuple>) {
        retry.executeSuspendFunction {
            batchPersist(writeList)
        }
    }

    private suspend fun batchPersist(writeList: List<TableItemTuple>) {
        val requestItems = writeList.groupBy({ it.tableName }, { it.writeRequest })
        val batchRequest = BatchWriteItemRequest { requestItems(requestItems) }

        // Non-Blocking 으로 저장 작업을 수행하기 위해서
        val result = withContext(coroutineContext) {
            // DynamoDB의 Batch 쓰기 작업
            dynamoDB.batchWriteItem(batchRequest)
        }

        // Partial failure
        if (result.unprocessedItems().isNotEmpty()) {
            val unprocessedWriteList = buildWriteLists(result.unprocessedItems())
            batchPersist(unprocessedWriteList)
        }
    }

    private fun buildWriteLists(items: Map<String, List<WriteRequest>>): List<TableItemTuple> {
        return items.entries
            .flatMap { entry ->
                entry.value.map { TableItemTuple(entry.key, it) }
            }
    }
}
