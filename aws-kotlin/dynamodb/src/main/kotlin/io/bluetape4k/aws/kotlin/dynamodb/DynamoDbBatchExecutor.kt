package io.bluetape4k.aws.kotlin.dynamodb

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.batchWriteItem
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.WriteRequest
import io.bluetape4k.aws.kotlin.dynamodb.Defaults.MAX_BATCH_ITEM_SIZE
import io.bluetape4k.logging.KLogging
import io.github.resilience4j.kotlin.retry.executeSuspendFunction
import io.github.resilience4j.retry.Retry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer

class DynamoDbBatchExecutor<T: Any>(
    private val client: DynamoDbClient,
    private val retry: Retry = Retry.ofDefaults("dynamo-batch"),
): CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()) {

    companion object: KLogging()

    data class TableItemTuple(val tableName: String, val writeRequest: WriteRequest)
    data class RetryablePut(val attempt: Int, val items: List<TableItemTuple>)

    /**
     * [tableName] table에 [items] 를 Put 작업을 Batch로 수행합니다.
     */
    suspend fun putAll(tableName: String, items: List<Map<String, AttributeValue>>) {
        val writeRequests = items.map {
            val request = WriteRequest {
                putRequest {
                    this.item = it
                }
            }
            TableItemTuple(tableName, request)
        }
        persist(writeRequests)
    }

    /**
     * [tableName] table에 [items] 를 Put 작업을 Batch로 수행합니다.
     */
    suspend fun putAll(tableName: String, items: List<T>, mapper: DynamoItemMapper<T>) {
        val writeRequests = items.map {
            val request = WriteRequest {
                putRequest {
                    this.item = mapper.mapToDynamoItem(it)
                }
            }
            TableItemTuple(tableName, request)
        }
        persist(writeRequests)
    }

    /**
     * [tableName] table에서 [items] 를 삭제하는 작업을 Batch로 수행합니다.
     */
    suspend fun deleteAll(
        tableName: String,
        items: List<T>,
        primaryKeySelector: (T) -> Map<String, AttributeValue>,
    ) {
        val writeRequests = items.map { item ->
            val request = WriteRequest {
                deleteRequest {
                    key = primaryKeySelector(item)
                }
            }
            TableItemTuple(tableName, request)
        }
        persist(writeRequests)
    }

    /**
     * [tableName] table에서 [items] 를 삭제하는 작업을 Batch로 수행합니다.
     */
    suspend fun deleteAll(
        tableName: String,
        items: List<T>,
        primaryKeyMapper: DynamoItemMapper<T>,
    ) {
        val writeRequests = items.map {
            val request = WriteRequest {
                deleteRequest {
                    key = primaryKeyMapper.mapToDynamoItem(it)
                }
            }
            TableItemTuple(tableName, request)
        }
        persist(writeRequests)
    }


    private suspend fun persist(items: List<TableItemTuple>) {
        items.chunked(MAX_BATCH_ITEM_SIZE)
            .asFlow()
            .buffer()
            .collect { chunked ->
                retry.executeSuspendFunction { persistAll(chunked) }
            }
    }

    private tailrec suspend fun persistAll(items: List<TableItemTuple>) {
        val requestItems = items.groupBy({ it.tableName }, { it.writeRequest })

        val result = client.batchWriteItem {
            this.requestItems = requestItems
        }

        // 부분적으로 Write 작업이 실패한 경우, 다시 시도합니다.
        // TODO: 무한 반복이 될 수도 있습니다 - 시도를 제한하거나, 타임아웃을 넣거나 해야 할 것 같습니다.
        if (result.unprocessedItems?.isNotEmpty() == true) {
            val unprocessedItems = result.unprocessedItems!!.entries.flatMap { entry ->
                entry.value.map { TableItemTuple(entry.key, it) }
            }
            persistAll(unprocessedItems)
        }
    }
}
