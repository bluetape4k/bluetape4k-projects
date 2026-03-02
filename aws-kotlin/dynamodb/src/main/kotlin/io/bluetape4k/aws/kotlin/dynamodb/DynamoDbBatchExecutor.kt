package io.bluetape4k.aws.kotlin.dynamodb

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.batchWriteItem
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.WriteRequest
import io.bluetape4k.aws.kotlin.dynamodb.Defaults.MAX_BATCH_ITEM_SIZE
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.github.resilience4j.kotlin.retry.executeSuspendFunction
import io.github.resilience4j.retry.Retry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer

/**
 * AWS Kotlin SDK를 사용해 DynamoDB에 배치 작업(Put/Delete)을 수행하는 Executor입니다.
 *
 * ## 동작/계약
 * - 내부적으로 25개([MAX_BATCH_ITEM_SIZE]) 단위로 청크를 나눠 BatchWriteItem을 호출한다.
 * - 미처리 항목은 [maxUnprocessedRetry] 한도 내에서 재귀적으로 재시도한다.
 * - [Retry]를 통해 일시적 오류(ThrottlingException 등)에 대한 재시도 정책을 적용한다.
 *
 * ```kotlin
 * val executor = DynamoDbBatchExecutor<Order>(client)
 * executor.putAll("orders", orders, OrderMapper())
 * ```
 *
 * @param T 배치 작업 대상 엔티티 타입
 * @param client AWS Kotlin SDK [DynamoDbClient] 인스턴스
 * @param retry Resilience4j [Retry] 정책 (기본: "dynamo-batch" 기본 설정)
 * @param maxUnprocessedRetry 미처리 항목 재시도 최대 횟수 (기본: 10)
 */
class DynamoDbBatchExecutor<T: Any>(
    private val client: DynamoDbClient,
    private val retry: Retry = Retry.ofDefaults("dynamo-batch"),
    private val maxUnprocessedRetry: Int = 10,
): CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()) {

    companion object: KLoggingChannel()

    /**
     * 테이블 이름과 [WriteRequest]를 묶는 배치 작업 단위입니다.
     *
     * @property tableName DynamoDB 테이블 이름
     * @property writeRequest 실행할 쓰기 요청
     */
    data class TableItemTuple(val tableName: String, val writeRequest: WriteRequest)

    /**
     * 미처리 항목과 현재 시도 횟수를 보관하는 데이터 클래스입니다.
     *
     * @property attempt 현재 시도 횟수 (1부터 시작)
     * @property items 재시도할 [TableItemTuple] 목록
     */
    data class RetryablePut(val attempt: Int, val items: List<TableItemTuple>)

    /**
     * 속성 맵 목록을 [tableName] 테이블에 Batch Put으로 저장합니다.
     *
     * ## 동작/계약
     * - 이미 `Map<String, AttributeValue>`로 변환된 아이템을 직접 Put한다.
     * - 25개 단위로 청크를 나눠 BatchWriteItem을 실행한다.
     *
     * ```kotlin
     * val items = listOf(mapOf("id" to AttributeValue.S("1")))
     * executor.putAll("users", items)
     * ```
     *
     * @param tableName 저장할 DynamoDB 테이블 이름
     * @param items 저장할 DynamoDB 속성 맵 목록
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
     * [mapper]로 변환한 [items]를 [tableName] 테이블에 Batch Put으로 저장합니다.
     *
     * ## 동작/계약
     * - [mapper]가 각 엔티티를 `Map<String, AttributeValue>`로 변환한다.
     * - 25개 단위 청크로 분할해 BatchWriteItem을 실행한다.
     *
     * ```kotlin
     * executor.putAll("orders", listOf(order1, order2), OrderMapper())
     * ```
     *
     * @param tableName 저장할 DynamoDB 테이블 이름
     * @param items 저장할 엔티티 목록
     * @param mapper 엔티티를 DynamoDB 속성 맵으로 변환하는 [DynamoItemMapper]
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
     * [primaryKeySelector]로 추출한 키를 기준으로 [items]를 [tableName] 테이블에서 Batch 삭제합니다.
     *
     * ## 동작/계약
     * - [primaryKeySelector] 람다로 각 엔티티의 파티션 키를 추출한다.
     * - 25개 단위 청크로 분할해 BatchWriteItem Delete를 실행한다.
     *
     * ```kotlin
     * executor.deleteAll("orders", orders) { mapOf("id" to AttributeValue.S(it.id)) }
     * ```
     *
     * @param tableName 삭제할 DynamoDB 테이블 이름
     * @param items 삭제할 엔티티 목록
     * @param primaryKeySelector 엔티티에서 DynamoDB 키 맵을 추출하는 함수
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
     * [primaryKeyMapper]로 추출한 키를 기준으로 [items]를 [tableName] 테이블에서 Batch 삭제합니다.
     *
     * ## 동작/계약
     * - [primaryKeyMapper]가 각 엔티티의 키 속성 맵을 반환한다.
     * - 25개 단위 청크로 분할해 BatchWriteItem Delete를 실행한다.
     *
     * ```kotlin
     * executor.deleteAll("orders", orders, OrderKeyMapper())
     * ```
     *
     * @param tableName 삭제할 DynamoDB 테이블 이름
     * @param items 삭제할 엔티티 목록
     * @param primaryKeyMapper 엔티티에서 DynamoDB 키 속성 맵을 추출하는 [DynamoItemMapper]
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

    private tailrec suspend fun persistAll(items: List<TableItemTuple>, attempt: Int = 1) {
        val requestItems = items.groupBy({ it.tableName }, { it.writeRequest })

        val result = client.batchWriteItem {
            this.requestItems = requestItems
        }

        if (result.unprocessedItems?.isNotEmpty() == true) {
            check(attempt < maxUnprocessedRetry) {
                "Failed to process batch write after $attempt attempts; unprocessed items remained."
            }

            val unprocessedItems = result.unprocessedItems!!.entries.flatMap { entry ->
                entry.value.map { TableItemTuple(entry.key, it) }
            }
            persistAll(unprocessedItems, attempt + 1)
        }
    }
}
