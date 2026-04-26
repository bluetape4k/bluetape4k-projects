package io.bluetape4k.elasticsearch.coroutines

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient
import co.elastic.clients.elasticsearch.core.CountRequest
import co.elastic.clients.elasticsearch.core.CountResponse
import co.elastic.clients.elasticsearch.core.DeleteRequest
import co.elastic.clients.elasticsearch.core.DeleteResponse
import co.elastic.clients.elasticsearch.core.GetRequest
import co.elastic.clients.elasticsearch.core.GetResponse
import co.elastic.clients.elasticsearch.core.IndexRequest
import co.elastic.clients.elasticsearch.core.IndexResponse
import co.elastic.clients.elasticsearch.core.SearchRequest
import co.elastic.clients.elasticsearch.core.SearchResponse
import co.elastic.clients.elasticsearch.core.UpdateRequest
import co.elastic.clients.elasticsearch.core.UpdateResponse
import co.elastic.clients.elasticsearch.core.ExistsRequest as CoreExistsRequest
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse
import co.elastic.clients.elasticsearch.indices.ExistsRequest as IndicesExistsRequest
import io.bluetape4k.support.requireNotBlank
import kotlinx.coroutines.future.await
import kotlin.reflect.KClass

// ---------------------------------------------------------------------------
// Document API — suspend 확장함수
// ---------------------------------------------------------------------------

/**
 * 문서를 인덱스에 저장하거나 갱신합니다 (suspend 버전).
 *
 * `ElasticsearchAsyncClient.index()` 의 suspend 래퍼입니다.
 * 내부적으로 `IndexRequest.Builder<TDocument>` 를 `block` 으로 구성한 뒤 `CompletableFuture.await()` 로 대기합니다.
 *
 * ## 주의 사항
 * 취소(cancel)는 클라이언트 측 대기만 종료합니다. 서버 측 요청은 이미 전송된 상태이므로
 * 코루틴 취소가 Elasticsearch 작업을 실제로 중단하지는 않습니다.
 *
 * ## 사용 예시
 * ```kotlin
 * val response = asyncClient.indexSuspending<MyDoc> {
 *     index("my-index")
 *     id("1")
 *     document(myDoc)
 * }
 * println(response.result())
 * ```
 *
 * @param TDocument 저장할 문서 타입
 * @param block `IndexRequest.Builder<TDocument>` 설정 람다
 * @return [IndexResponse]
 */
suspend inline fun <reified TDocument : Any> ElasticsearchAsyncClient.indexSuspending(
    block: IndexRequest.Builder<TDocument>.() -> Unit,
): IndexResponse {
    val request = IndexRequest.Builder<TDocument>().apply(block).build()
    return this.index(request).await()
}

/**
 * ID 로 문서를 조회합니다 (suspend 버전).
 *
 * `ElasticsearchAsyncClient.get(request, Class)` 의 suspend 래퍼입니다.
 *
 * ## 사용 예시
 * ```kotlin
 * val response = asyncClient.getSuspending(
 *     index = "my-index",
 *     id = "1",
 *     clazz = MyDoc::class,
 * )
 * val doc: MyDoc? = response.source()
 * ```
 *
 * @param T 문서 타입
 * @param index 인덱스 이름
 * @param id 문서 ID
 * @param clazz 문서 타입의 [KClass]
 * @return [GetResponse] of [T]
 */
suspend fun <T : Any> ElasticsearchAsyncClient.getSuspending(
    index: String,
    id: String,
    clazz: KClass<T>,
): GetResponse<T> {
    index.requireNotBlank("index")
    id.requireNotBlank("id")
    val request = GetRequest.of { it.index(index).id(id) }
    return this.get(request, clazz.java).await()
}

/**
 * 문서를 삭제합니다 (suspend 버전).
 *
 * `ElasticsearchAsyncClient.delete()` 의 suspend 래퍼입니다.
 *
 * ## 사용 예시
 * ```kotlin
 * val response = asyncClient.deleteSuspending {
 *     index("my-index")
 *     id("1")
 * }
 * println(response.result())
 * ```
 *
 * @param block `DeleteRequest.Builder` 설정 람다
 * @return [DeleteResponse]
 */
suspend fun ElasticsearchAsyncClient.deleteSuspending(
    block: DeleteRequest.Builder.() -> Unit,
): DeleteResponse {
    val request = DeleteRequest.Builder().apply(block).build()
    return this.delete(request).await()
}

/**
 * 문서를 부분 갱신합니다 (suspend 버전).
 *
 * `ElasticsearchAsyncClient.update(request, Class)` 의 suspend 래퍼입니다.
 * 반환 타입은 `UpdateResponse<Any>` 이므로, 필요 시 타입 캐스팅하세요.
 *
 * ## 주의 사항
 * 취소(cancel)는 클라이언트 측 대기만 종료합니다.
 *
 * ## 사용 예시
 * ```kotlin
 * val response = asyncClient.updateSuspending<MyDoc, MyPartialDoc> {
 *     index("my-index")
 *     id("1")
 *     doc(partialDoc)
 * }
 * println(response.result())
 * ```
 *
 * @param TDocument 문서 전체 타입
 * @param TPartialDocument 부분 업데이트 문서 타입
 * @param block `UpdateRequest.Builder<TDocument, TPartialDocument>` 설정 람다
 * @return `UpdateResponse<TDocument>`
 */
@Suppress("UNCHECKED_CAST")
suspend inline fun <reified TDocument : Any, reified TPartialDocument : Any>
        ElasticsearchAsyncClient.updateSuspending(
    block: UpdateRequest.Builder<TDocument, TPartialDocument>.() -> Unit,
): UpdateResponse<TDocument> {
    val request = UpdateRequest.Builder<TDocument, TPartialDocument>().apply(block).build()
    return this.update(request, TDocument::class.java).await()
}

/**
 * 문서 존재 여부를 확인합니다 (suspend 버전).
 *
 * `ElasticsearchAsyncClient.exists()` 의 suspend 래퍼입니다.
 *
 * ## 사용 예시
 * ```kotlin
 * val exists = asyncClient.existsSuspending {
 *     index("my-index")
 *     id("1")
 * }
 * if (exists) println("문서가 존재합니다.")
 * ```
 *
 * @param block `ExistsRequest.Builder` 설정 람다 (core)
 * @return `true` 이면 문서 존재, `false` 이면 없음
 */
suspend fun ElasticsearchAsyncClient.existsSuspending(
    block: CoreExistsRequest.Builder.() -> Unit,
): Boolean {
    val request = CoreExistsRequest.Builder().apply(block).build()
    return this.exists(request).await().value()
}

// ---------------------------------------------------------------------------
// Search API — suspend 확장함수
// ---------------------------------------------------------------------------

/**
 * 검색 요청을 실행하고 결과를 반환합니다 (suspend 버전).
 *
 * `ElasticsearchAsyncClient.search(request, Class)` 의 suspend 래퍼입니다.
 *
 * ## 사용 예시
 * ```kotlin
 * val response = asyncClient.searchSuspending(
 *     clazz = MyDoc::class,
 * ) {
 *     index("my-index")
 *     query { q -> q.matchAll { it } }
 *     size(10)
 * }
 * response.hits().hits().forEach { hit -> println(hit.source()) }
 * ```
 *
 * @param T 히트 문서 타입
 * @param block `SearchRequest.Builder` 설정 람다
 * @param clazz 문서 타입의 [KClass]
 * @return [SearchResponse] of [T]
 */
suspend fun <T : Any> ElasticsearchAsyncClient.searchSuspending(
    clazz: KClass<T>,
    block: SearchRequest.Builder.() -> Unit,
): SearchResponse<T> {
    val request = SearchRequest.Builder().apply(block).build()
    return this.search(request, clazz.java).await()
}

/**
 * 조건에 맞는 문서 수를 반환합니다 (suspend 버전).
 *
 * `ElasticsearchAsyncClient.count()` 의 suspend 래퍼입니다.
 *
 * ## 사용 예시
 * ```kotlin
 * val response = asyncClient.countSuspending {
 *     index("my-index")
 *     query { q -> q.term { t -> t.field("status").value("active") } }
 * }
 * println("count = ${response.count()}")
 * ```
 *
 * @param block `CountRequest.Builder` 설정 람다
 * @return [CountResponse]
 */
suspend fun ElasticsearchAsyncClient.countSuspending(
    block: CountRequest.Builder.() -> Unit,
): CountResponse {
    val request = CountRequest.Builder().apply(block).build()
    return this.count(request).await()
}

// ---------------------------------------------------------------------------
// Indices API — suspend 확장함수
// ---------------------------------------------------------------------------

/**
 * 새 인덱스를 생성합니다 (suspend 버전).
 *
 * `ElasticsearchIndicesAsyncClient.create()` 의 suspend 래퍼입니다.
 *
 * ## 사용 예시
 * ```kotlin
 * val response = asyncClient.createIndexSuspending {
 *     index("my-index")
 *     settings { s -> s.numberOfShards("1").numberOfReplicas("0") }
 * }
 * println("acknowledged = ${response.acknowledged()}")
 * ```
 *
 * @param block `CreateIndexRequest.Builder` 설정 람다
 * @return [CreateIndexResponse]
 */
suspend fun ElasticsearchAsyncClient.createIndexSuspending(
    block: CreateIndexRequest.Builder.() -> Unit,
): CreateIndexResponse {
    val request = CreateIndexRequest.Builder().apply(block).build()
    return this.indices().create(request).await()
}

/**
 * 인덱스를 삭제합니다 (suspend 버전).
 *
 * `ElasticsearchIndicesAsyncClient.delete()` 의 suspend 래퍼입니다.
 *
 * ## 사용 예시
 * ```kotlin
 * val response = asyncClient.deleteIndexSuspending {
 *     index(listOf("my-index"))
 * }
 * println("acknowledged = ${response.acknowledged()}")
 * ```
 *
 * @param block `DeleteIndexRequest.Builder` 설정 람다
 * @return [DeleteIndexResponse]
 */
suspend fun ElasticsearchAsyncClient.deleteIndexSuspending(
    block: DeleteIndexRequest.Builder.() -> Unit,
): DeleteIndexResponse {
    val request = DeleteIndexRequest.Builder().apply(block).build()
    return this.indices().delete(request).await()
}

/**
 * 인덱스 존재 여부를 확인합니다 (suspend 버전).
 *
 * `ElasticsearchIndicesAsyncClient.exists()` 의 suspend 래퍼입니다.
 *
 * ## 사용 예시
 * ```kotlin
 * val exists = asyncClient.indexExistsSuspending {
 *     index(listOf("my-index"))
 * }
 * if (exists) println("인덱스가 존재합니다.")
 * ```
 *
 * @param block `IndicesExistsRequest.Builder` 설정 람다
 * @return `true` 이면 인덱스 존재, `false` 이면 없음
 */
suspend fun ElasticsearchAsyncClient.indexExistsSuspending(
    block: IndicesExistsRequest.Builder.() -> Unit,
): Boolean {
    val request = IndicesExistsRequest.Builder().apply(block).build()
    return this.indices().exists(request).await().value()
}
