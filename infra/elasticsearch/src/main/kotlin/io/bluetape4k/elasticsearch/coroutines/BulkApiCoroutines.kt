package io.bluetape4k.elasticsearch.coroutines

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient
import co.elastic.clients.elasticsearch.core.BulkRequest
import co.elastic.clients.elasticsearch.core.BulkResponse
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem
import io.bluetape4k.coroutines.flow.extensions.chunked
import io.bluetape4k.elasticsearch.ElasticsearchDefaults
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.future.await

/**
 * [Flow]<[BulkOperation]> 을 [chunkSize] 단위로 묶어 Elasticsearch 에 일괄 인덱싱하고
 * 각 chunk 의 [BulkResponse] 를 emit 하는 Flow 를 반환합니다.
 *
 * Kafka `sendAsFlow` 패턴과 동형 — Flow back-pressure 가 자연스럽게 적용됩니다.
 *
 * ## Partial failure 처리 정책
 *
 * - 기본 정책: chunk 단위 [BulkResponse] 그대로 emit — **partial failure 는 throw 하지 않음**.
 * - 사용자가 `BulkResponse.errors()` 로 에러 존재 여부를 확인하거나
 *   [onItemError] 콜백으로 실패 item 만 골라 받을 수 있습니다 (기본 no-op).
 * - 사용자가 retry / dead-letter / 로깅 / metric 등 처리 전략을 자유롭게 선택할 수 있도록
 *   item-level error 는 [BulkResponse] 에 그대로 포함됩니다.
 *
 * ## 사용 예시
 * ```kotlin
 * flowOf(op1, op2, op3, op4, op5)
 *     .bulkAsFlow(client, indexName = "my-index", chunkSize = 2) { failedItem ->
 *         logger.warn { "Bulk item failed: ${failedItem.error()?.reason()}" }
 *     }
 *     .collect { response ->
 *         println("hasErrors=${response.errors()}, items=${response.items().size}")
 *     }
 * ```
 *
 * @param client     [ElasticsearchAsyncClient] 인스턴스
 * @param indexName  대상 인덱스 이름 (개별 [BulkOperation] 에 index 가 명시되어 있으면 그 값이 우선)
 * @param chunkSize  한 BulkRequest 당 최대 operation 개수 (기본: [ElasticsearchDefaults.DEFAULT_BULK_CHUNK_SIZE])
 * @param onItemError 실패한 [BulkResponseItem] 에 대해 호출되는 item 단위 에러 처리 콜백 (기본: no-op)
 * @return 각 BulkRequest 의 [BulkResponse] 를 emit 하는 [Flow]
 */
fun Flow<BulkOperation>.bulkAsFlow(
    client: ElasticsearchAsyncClient,
    indexName: String,
    chunkSize: Int = ElasticsearchDefaults.DEFAULT_BULK_CHUNK_SIZE,
    onItemError: (BulkResponseItem) -> Unit = {},
): Flow<BulkResponse> {
    indexName.requireNotBlank("indexName")
    chunkSize.requirePositiveNumber("chunkSize")

    return chunked(chunkSize).map { chunk ->
        val response = client.suspendBulk {
            index(indexName)
            operations(chunk)
        }
        if (response.errors()) {
            response.items().forEach { item ->
                if (item.error() != null) {
                    onItemError(item)
                }
            }
        }
        response
    }
}

/**
 * [BulkRequest.Builder] 빌더 람다로 BulkRequest 를 구성하고 suspend 방식으로 발행합니다.
 *
 * `ElasticsearchAsyncClient.bulk()` 의 suspend 래퍼입니다.
 *
 * ## 주의 사항
 * 취소(cancel)는 클라이언트 측 대기만 종료합니다. 서버 측 요청은 이미 전송된 상태이므로
 * 코루틴 취소가 Elasticsearch 작업을 실제로 중단하지는 않습니다.
 *
 * ## 사용 예시
 * ```kotlin
 * val response = client.suspendBulk {
 *     index("my-index")
 *     operations(listOf(op1, op2, op3))
 * }
 * if (response.errors()) {
 *     response.items().filter { it.error() != null }.forEach { item ->
 *         println("Failed: ${item.id()} → ${item.error()?.reason()}")
 *     }
 * }
 * ```
 *
 * @param block [BulkRequest.Builder] 설정 람다
 * @return [BulkResponse]
 */
suspend fun ElasticsearchAsyncClient.suspendBulk(
    block: BulkRequest.Builder.() -> Unit,
): BulkResponse {
    val request = BulkRequest.Builder().apply(block).build()
    return this.bulk(request).await()
}
