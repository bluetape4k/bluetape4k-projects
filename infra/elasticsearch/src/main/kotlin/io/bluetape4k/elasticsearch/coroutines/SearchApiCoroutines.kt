package io.bluetape4k.elasticsearch.coroutines

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient
import co.elastic.clients.elasticsearch._types.FieldValue
import co.elastic.clients.elasticsearch.core.ClosePointInTimeRequest
import co.elastic.clients.elasticsearch.core.OpenPointInTimeRequest
import co.elastic.clients.elasticsearch.core.SearchRequest
import io.bluetape4k.elasticsearch.ElasticsearchDefaults
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext

@PublishedApi
internal val log = KotlinLogging.logger {}

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

@PublishedApi
internal suspend fun closePointInTimeBestEffort(
    pitId: String,
    close: suspend (String) -> Boolean,
) {
    pitId.requireNotBlank("pitId")

    withContext(NonCancellable) {
        try {
            close(pitId)
        } catch (e: CancellationException) {
            log.warn(e) { "Cancelled while closing ES PIT: pitId=$pitId" }
        } catch (e: Exception) {
            log.warn(e) { "Failed to close ES PIT: pitId=$pitId" }
        }
    }
}

// ---------------------------------------------------------------------------
// search_after + PIT 기반 무한 스크롤 Flow
// ---------------------------------------------------------------------------

/**
 * Exposes `search_after` + PIT(point-in-time) pagination as a lazy [Flow].
 *
 * Elasticsearch deprecated the scroll API, so this helper uses PIT with `search_after`
 * for consistent cursor-based paging.
 *
 * ## Behavior
 * 1. Opens a PIT through [openPointInTimeSuspending] before the first page.
 * 2. Repeats search requests in `batchSize` chunks and advances the `searchAfter` cursor.
 * 3. Stops when Elasticsearch returns an empty page and closes the PIT from `finally`
 *    for normal completion, failures, and collector cancellation.
 *
 * ## Usage notes
 * - [queryBlock] must set a stable sort that includes a tie-breaker such as `_shard_doc`.
 *   Without sorting, `searchAfter` cannot advance safely.
 * - Do not set `index`, `pit`, `size`, or `searchAfter` inside [queryBlock]; this helper
 *   owns those fields.
 * - PIT close runs from a non-cancellable cleanup boundary. Close failures are logged
 *   and swallowed so the original collector cancellation or upstream failure can continue
 *   to propagate.
 *
 * ## Example
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
                    .apply { if (searchAfter.isNotEmpty()) searchAfter(searchAfter) }
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
            // Close PIT from a cleanup boundary that survives collector cancellation.
            closePointInTimeBestEffort(pitId) { id ->
                client.closePointInTimeSuspending(id)
            }
        }
    }
}
