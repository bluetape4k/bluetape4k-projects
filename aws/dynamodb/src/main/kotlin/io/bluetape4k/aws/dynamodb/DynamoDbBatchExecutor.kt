package io.bluetape4k.aws.dynamodb

import io.bluetape4k.aws.dynamodb.DynamoDb.MAX_BATCH_ITEM_SIZE
import io.bluetape4k.aws.dynamodb.model.BatchWriteItemRequest
import io.bluetape4k.aws.dynamodb.model.writeRequestOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.info
import io.github.resilience4j.kotlin.retry.executeSuspendFunction
import io.github.resilience4j.retry.Retry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.withContext
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemResponse
import software.amazon.awssdk.services.dynamodb.model.WriteRequest
import java.io.Serializable

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
            log.info { "Create DynamoDbBatchExecutor instance." }
            return DynamoDbBatchExecutor(dynamoDB, retry)
        }
    }

    data class TableItemTuple(
        val tableName: String,
        val writeRequest: WriteRequest,
    ): Serializable

    data class RetryablePut(
        val attempt: Int,
        val items: List<TableItemTuple>,
    ): Serializable

    /**
     * [tableName] table에서 [items] 를 삭제하는 작업을 Batch로 수행합니다.
     *
     * @param tableName  Table name
     * @param items      삭제할 Items
     * @param primaryKeySelector primary key selector
     */
    suspend fun delete(
        tableName: String,
        items: List<T>,
        primaryKeySelector: (T) -> Map<String, AttributeValue>,
    ) {
        val writeRequests =
            items
                .map { item ->
                    WriteRequest
                        .builder()
                        .deleteRequest { it.key(primaryKeySelector(item)) }
                        .build()
                }.map { TableItemTuple(tableName, it) }

        writeRequests
            .chunked(MAX_BATCH_ITEM_SIZE)
            .forEach {
                executeBatchPersist(it)
            }
    }

    suspend inline fun persist(
        tableName: String,
        items: List<T>,
        mapper: DynamoItemMapper<T>,
    ) {
        persist(
            items
                .buildWriteRequest(mapper)
                .map { TableItemTuple(tableName, it) }
        )
    }

    suspend inline fun persist(
        tableName: String,
        items: List<Map<String, AttributeValue>>,
    ) {
        persist(items.map { TableItemTuple(tableName, writeRequestOf(it)) })
    }

    suspend fun persist(writeItems: List<TableItemTuple>) {
        writeItems
            .chunked(MAX_BATCH_ITEM_SIZE)
            .asFlow()
            .buffer()
            .collect {
                executeBatchPersist(it)
            }
    }

    private suspend fun executeBatchPersist(writeList: List<TableItemTuple>) {
        var pending = writeList
        var attempt = 0
        val maxAttempts = retry.retryConfig.maxAttempts

        while (pending.isNotEmpty()) {
            attempt++
            val result =
                retry.executeSuspendFunction {
                    batchPersistOnce(pending)
                }

            val unprocessed = result.unprocessedItems()
            if (unprocessed.isEmpty()) return

            if (attempt >= maxAttempts) {
                val remaining = unprocessed.values.sumOf { it.size }
                error("BatchWriteItem has $remaining unprocessed items after $attempt attempts.")
            }

            pending = buildWriteLists(unprocessed)
        }
    }

    private suspend fun batchPersistOnce(writeList: List<TableItemTuple>): BatchWriteItemResponse {
        val requestItems = writeList.groupBy({ it.tableName }, { it.writeRequest })
        val batchRequest = BatchWriteItemRequest { requestItems(requestItems) }

        // Non-Blocking 으로 저장 작업을 수행하기 위해서
        return withContext(coroutineContext) {
            // DynamoDB의 Batch 쓰기 작업
            dynamoDB.batchWriteItem(batchRequest)
        }
    }

    private fun buildWriteLists(
        items: Map<String, List<WriteRequest>>,
    ): List<TableItemTuple> =
        items.entries
            .flatMap { entry ->
                entry.value.map { TableItemTuple(entry.key, it) }
            }
}
