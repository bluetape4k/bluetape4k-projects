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

    /**
     * 테이블 이름과 [WriteRequest]를 묶는 작업 단위입니다.
     *
     * ## 동작/계약
     * - [tableName]과 [writeRequest]를 하나의 배치 작업 항목으로 묶는다.
     * - [DynamoDbBatchExecutor.persist]에 전달할 때 사용한다.
     *
     * ```kotlin
     * val tuple = DynamoDbBatchExecutor.TableItemTuple("orders", writeRequest)
     * // tuple.tableName == "orders"
     * ```
     *
     * @property tableName DynamoDB 테이블 이름
     * @property writeRequest 실행할 쓰기 요청
     */
    data class TableItemTuple(
        val tableName: String,
        val writeRequest: WriteRequest,
    ): Serializable

    /**
     * 재시도 가능한 Batch 쓰기 작업의 상태를 보관하는 데이터 클래스입니다.
     *
     * ## 동작/계약
     * - [attempt]는 현재까지 시도한 횟수를 나타낸다.
     * - [items]는 아직 처리되지 않은 [TableItemTuple] 목록이다.
     *
     * @property attempt 현재 시도 횟수 (1부터 시작)
     * @property items 재시도할 [TableItemTuple] 목록
     */
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

    /**
     * [mapper]를 사용해 [items]를 [tableName] 테이블에 Batch로 저장합니다.
     *
     * ## 동작/계약
     * - [mapper]로 [T] 엔티티를 `Map<String, AttributeValue>`로 변환한 뒤 Batch 저장한다.
     * - 25개 단위로 청크를 나눠 여러 번의 BatchWrite를 수행한다.
     * - 미처리 항목이 남으면 [Retry] 설정 내에서 재시도한다.
     *
     * ```kotlin
     * executor.persist("orders", listOf(order1, order2), OrderMapper())
     * ```
     *
     * @param tableName 저장할 DynamoDB 테이블 이름
     * @param items 저장할 엔티티 목록
     * @param mapper 엔티티를 DynamoDB Item으로 변환하는 [DynamoItemMapper]
     */
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

    /**
     * `Map<String, AttributeValue>` 형태의 [items]를 [tableName] 테이블에 Batch로 저장합니다.
     *
     * ## 동작/계약
     * - 이미 DynamoDB 속성 맵으로 변환된 아이템을 직접 저장할 때 사용한다.
     * - 25개 단위로 청크를 나눠 BatchWrite를 수행한다.
     *
     * ```kotlin
     * val items = listOf(mapOf("id" to stringOf("1"), "name" to stringOf("Alice")))
     * executor.persist("users", items)
     * ```
     *
     * @param tableName 저장할 DynamoDB 테이블 이름
     * @param items 저장할 DynamoDB Item 속성 맵 목록
     */
    suspend inline fun persist(
        tableName: String,
        items: List<Map<String, AttributeValue>>,
    ) {
        persist(items.map { TableItemTuple(tableName, writeRequestOf(it)) })
    }

    /**
     * [TableItemTuple] 목록을 25개 단위로 분할하여 Batch로 저장합니다.
     *
     * ## 동작/계약
     * - [writeItems]를 [MAX_BATCH_ITEM_SIZE](25)개씩 청크로 나눠 순차 실행한다.
     * - 미처리 항목이 남으면 [Retry] 설정 내에서 재시도하며, 한계 초과 시 예외를 던진다.
     *
     * ```kotlin
     * val tuples = listOf(TableItemTuple("orders", writeRequest1))
     * executor.persist(tuples)
     * ```
     *
     * @param writeItems 저장할 [TableItemTuple] 목록
     */
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
