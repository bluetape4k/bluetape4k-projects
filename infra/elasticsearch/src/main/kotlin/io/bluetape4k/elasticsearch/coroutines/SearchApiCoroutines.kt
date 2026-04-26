package io.bluetape4k.elasticsearch.coroutines

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient
import co.elastic.clients.elasticsearch._types.FieldValue
import co.elastic.clients.elasticsearch.core.ClosePointInTimeRequest
import co.elastic.clients.elasticsearch.core.OpenPointInTimeRequest
import co.elastic.clients.elasticsearch.core.SearchRequest
import io.bluetape4k.elasticsearch.ElasticsearchDefaults
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.future.await

// ---------------------------------------------------------------------------
// Point-In-Time(PIT) suspend 확장함수
// ---------------------------------------------------------------------------

/**
 * 지정한 인덱스에 대해 새 Point-in-Time(PIT) 을 열고 PIT ID 를 반환합니다 (suspend 버전).
 *
 * 반환된 PIT ID 는 search_after 페이징의 일관된 스냅샷으로 사용되며,
 * 사용 후에는 반드시 [closePointInTimeSuspending] 으로 해제해야 합니다.
 *
 * ## 사용 예시
 * ```kotlin
 * val pitId = asyncClient.openPointInTimeSuspending("my-index", keepAlive = "1m")
 * try {
 *     // search 호출 ...
 * } finally {
 *     asyncClient.closePointInTimeSuspending(pitId)
 * }
 * ```
 *
 * @param indexName 검색 대상 인덱스 (blank 불가)
 * @param keepAlive PIT 유효 시간 (예: `"1m"`, `"30s"`)
 * @return 새로 발급된 PIT ID
 */
suspend fun ElasticsearchAsyncClient.openPointInTimeSuspending(
    indexName: String,
    keepAlive: String = "1m",
): String {
    indexName.requireNotBlank("indexName")
    keepAlive.requireNotBlank("keepAlive")

    val request = OpenPointInTimeRequest.Builder()
        .index(listOf(indexName))
        .keepAlive { it.time(keepAlive) }
        .build()
    return this.openPointInTime(request).await().id()
}

/**
 * 열려 있는 Point-in-Time(PIT) 을 해제합니다 (suspend 버전).
 *
 * PIT 는 `keep_alive` 만료 시 자동 해제되지만, 디스크/heap 점유를 줄이려면
 * 사용 직후 명시적으로 닫는 것이 권장됩니다.
 *
 * ## 사용 예시
 * ```kotlin
 * val ok = asyncClient.closePointInTimeSuspending(pitId)
 * if (!ok) log.warn { "PIT close 실패: $pitId" }
 * ```
 *
 * @param pitId 해제할 PIT ID (blank 불가)
 * @return 서버 측 close 성공 여부
 */
suspend fun ElasticsearchAsyncClient.closePointInTimeSuspending(
    pitId: String,
): Boolean {
    pitId.requireNotBlank("pitId")

    val request = ClosePointInTimeRequest.Builder()
        .id(pitId)
        .build()
    return this.closePointInTime(request).await().succeeded()
}

// ---------------------------------------------------------------------------
// search_after + PIT 기반 무한 스크롤 Flow
// ---------------------------------------------------------------------------

/**
 * `search_after` + PIT(point-in-time) 페이징을 [Flow] 로 노출하는 무한 스크롤 검색입니다.
 *
 * Elasticsearch 의 scroll API 는 deprecated 되었으므로 본 함수는 PIT + `search_after` 조합을 사용합니다.
 *
 * ## 동작
 * 1. 함수 시작 시 [openPointInTimeSuspending] 으로 PIT 를 열어 일관된 스냅샷을 확보합니다.
 * 2. `batchSize` 만큼 search 를 반복 호출하며 `searchAfter` 커서를 다음 페이지로 갱신합니다.
 * 3. 빈 페이지가 반환되면 종료되며, 정상 종료 / 예외 / `CancellationException` 어느 경로에서도
 *    `finally` 블록에서 PIT 를 닫아 자원 누수를 막습니다.
 *
 * ## 사용 시 주의
 * - `queryBlock` 안에서 **반드시 tie-breaker 를 포함한 `sort` 를 지정**해야 합니다 (예: 정렬 기준 + `_shard_doc`).
 *   sort 가 없으면 `searchAfter` 가 동작하지 않습니다.
 * - `index`, `pit`, `size`, `searchAfter` 는 본 함수가 자동으로 채우므로 [queryBlock] 에서 다시 지정하지 마세요.
 * - PIT close 가 실패해도 leak 만 막으면 충분하므로 `runCatching` 으로 swallow 합니다.
 *
 * ## 사용 예시
 * ```kotlin
 * asyncClient.searchAsFlow<MyDoc>(
 *     indexName = "my-index",
 *     batchSize = 500,
 *     keepAlive = "2m",
 * ) {
 *     query { q -> q.matchAll { it } }
 *     sort { s -> s.field { f -> f.field("_shard_doc").order(SortOrder.Asc) } }
 * }.collect { doc ->
 *     println(doc)
 * }
 * ```
 *
 * @param T 결과 문서 타입
 * @param indexName 검색 대상 인덱스 (blank 불가)
 * @param batchSize 한 번의 search 호출당 page size (양수, 기본값 [ElasticsearchDefaults.DEFAULT_SEARCH_BATCH_SIZE])
 * @param keepAlive PIT 유효 시간 (예: `"1m"`)
 * @param queryBlock `SearchRequest.Builder` 설정 람다 (query / sort 등)
 * @return 매 hit 의 source 문서를 lazy 하게 발행하는 [Flow]
 */
inline fun <reified T : Any> ElasticsearchAsyncClient.searchAsFlow(
    indexName: String,
    batchSize: Int = ElasticsearchDefaults.DEFAULT_SEARCH_BATCH_SIZE,
    keepAlive: String = "1m",
    noinline queryBlock: SearchRequest.Builder.() -> Unit = {},
): Flow<T> {
    indexName.requireNotBlank("indexName")
    keepAlive.requireNotBlank("keepAlive")
    batchSize.requirePositiveNumber("batchSize")

    val client = this
    val docClass = T::class.java

    return flow {
        val pitId = client.openPointInTimeSuspending(indexName, keepAlive)
        try {
            var searchAfter: List<FieldValue> = emptyList()
            while (true) {
                val request = SearchRequest.Builder()
                    .apply(queryBlock)
                    .pit { p -> p.id(pitId).keepAlive { t -> t.time(keepAlive) } }
                    .searchAfter(searchAfter)
                    .size(batchSize)
                    .build()

                val response = client.search(request, docClass).await()
                val hits = response.hits().hits()
                if (hits.isEmpty()) break

                hits.forEach { hit -> hit.source()?.let { emit(it) } }

                val lastSort = hits.last().sort()
                if (lastSort.isNullOrEmpty()) break
                searchAfter = lastSort
            }
        } finally {
            // CancellationException 포함 모든 종료 경로에서 PIT close 보장.
            // close 자체가 실패해도 swallow — leak 만 막으면 충분.
            runCatching { client.closePointInTimeSuspending(pitId) }
        }
    }
}
