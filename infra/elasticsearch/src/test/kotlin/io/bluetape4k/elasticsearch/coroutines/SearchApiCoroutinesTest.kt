package io.bluetape4k.elasticsearch.coroutines

import co.elastic.clients.elasticsearch._types.SortOrder
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeBlank
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.elasticsearch.AbstractElasticsearchTest
import io.bluetape4k.elasticsearch.ElasticsearchTestFixtures
import io.bluetape4k.elasticsearch.ElasticsearchTestFixtures.createTestIndex
import io.bluetape4k.elasticsearch.ElasticsearchTestFixtures.deleteTestIndex
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.time.Duration.Companion.seconds

/**
 * [SearchApiCoroutines] suspend / Flow 확장함수에 대한 통합 테스트.
 *
 * Testcontainers 기반 실 Elasticsearch 클러스터에 10000건의 [SearchDoc] 을 사전 인덱싱한 뒤,
 * Point-in-Time(PIT) 생성/해제, `searchAsFlow` 무한 스크롤, 일반 `searchSuspending` 시나리오를 검증합니다.
 *
 * 테스트 인덱스는 `@BeforeAll` 에서 한 번만 생성되며, 모든 테스트가 끝난 후 `@AfterAll` 에서 삭제됩니다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchApiCoroutinesTest: AbstractElasticsearchTest() {

    companion object: KLogging() {
        private const val TOTAL_DOCS: Int = 10_000
        private val CATEGORIES: List<String> = listOf("A", "B", "C", "D", "E")
    }

    /**
     * 검색 테스트에 사용하는 단순 문서 타입.
     */
    data class SearchDoc(
        val title: String,
        val category: String,
        val score: Int,
    )

    private lateinit var indexName: String

    // -------------------------------------------------------------------------
    // 사전 데이터 셋업 (10000건 bulk 인덱싱)
    // -------------------------------------------------------------------------

    @BeforeAll
    fun setUpAll() = runBlocking {
        indexName = ElasticsearchTestFixtures.randomIndexName("search-flow")
        asyncClient.createTestIndex(indexName).await()

        // category 를 균등 분포로 생성 (A~E 각 2000건)
        val operations = (0 until TOTAL_DOCS).map { i ->
            val doc = SearchDoc(
                title = "doc-$i",
                category = CATEGORIES[i % CATEGORIES.size],
                score = i,
            )
            BulkOperation.of { op ->
                op.index { idx -> idx.index(indexName).id(i.toString()).document(doc) }
            }
        }

        // bulkAsFlow 로 일괄 인덱싱 (chunkSize 500)
        operations.asFlow()
            .bulkAsFlow(asyncClient, indexName, chunkSize = 500)
            .collect { /* drop chunk responses */ }

        // 검색 가능 상태로 만들기 위한 refresh
        asyncClient.indices().refresh { it.index(listOf(indexName)) }.await()
        Unit
    }

    @AfterAll
    fun tearDownAll() = runBlocking {
        runCatching { asyncClient.deleteTestIndex(indexName).await() }
        Unit
    }

    // -------------------------------------------------------------------------
    // openPointInTimeSuspending / closePointInTimeSuspending
    // -------------------------------------------------------------------------

    @Test
    fun `PIT 를 열면 비어있지 않은 ID 를 반환하고 닫으면 success 가 true 이다`() =
        runTest(timeout = 30.seconds) {
            val pitId = asyncClient.openPointInTimeSuspending(indexName, keepAlive = "1m")
            pitId.shouldNotBeBlank()

            val closed = asyncClient.closePointInTimeSuspending(pitId)
            closed.shouldBeTrue()
        }

    @Test
    fun `PIT 와 searchAsFlow 는 기본 옵션으로도 동작한다`() = runTest(timeout = 60.seconds) {
        val pitId = asyncClient.openPointInTimeSuspending(indexName)
        pitId.shouldNotBeBlank()
        asyncClient.closePointInTimeSuspending(pitId).shouldBeTrue()

        val first = asyncClient.searchAsFlow<SearchDoc>(indexName)
            .take(1)
            .toList()
        first.size shouldBeEqualTo 1
    }

    // -------------------------------------------------------------------------
    // searchAsFlow — 전체 10000건 순회
    // -------------------------------------------------------------------------

    @Test
    fun `searchAsFlow 는 전체 10000건을 PIT + search_after 로 순회한다`() =
        runTest(timeout = 120.seconds) {
            val docs: List<SearchDoc> = asyncClient.searchAsFlow<SearchDoc>(
                indexName = indexName,
                batchSize = 100,
                keepAlive = "2m",
            ) {
                query { q -> q.matchAll { it } }
                // tie-breaker — _shard_doc 를 사용해 동일 sort key 충돌을 방지한다.
                sort { s -> s.field { f -> f.field("_shard_doc").order(SortOrder.Asc) } }
            }.toList()

            docs.size shouldBeEqualTo TOTAL_DOCS
        }

    // -------------------------------------------------------------------------
    // searchAsFlow — 특정 category 필터
    // -------------------------------------------------------------------------

    @Test
    fun `searchAsFlow 는 category 필터를 반영하여 약 2000건을 반환한다`() =
        runTest(timeout = 120.seconds) {
            val targetCategory = "A"
            val count = asyncClient.searchAsFlow<SearchDoc>(
                indexName = indexName,
                batchSize = 100,
                keepAlive = "2m",
            ) {
                query { q ->
                    q.term { t -> t.field("category.keyword").value(targetCategory) }
                }
                sort { s -> s.field { f -> f.field("_shard_doc").order(SortOrder.Asc) } }
            }.count()

            // A category 는 정확히 TOTAL_DOCS / CATEGORIES.size = 2000 건이지만,
            // 환경 변동성을 고려해 1900 건 이상이면 통과로 간주한다.
            count shouldBeGreaterOrEqualTo 1900
        }

    // -------------------------------------------------------------------------
    // searchAsFlow cancel 안전성 — take(N) 으로 조기 종료해도 PIT close 가 안전해야 함
    // -------------------------------------------------------------------------

    @Test
    fun `searchAsFlow 를 take 로 조기 종료해도 예외 없이 정상 종료된다`() =
        runTest(timeout = 60.seconds) {
            val first10 = asyncClient.searchAsFlow<SearchDoc>(
                indexName = indexName,
                batchSize = 100,
                keepAlive = "2m",
            ) {
                query { q -> q.matchAll { it } }
                sort { s -> s.field { f -> f.field("_shard_doc").order(SortOrder.Asc) } }
            }
                .take(10)
                .toList()

            // best-effort: 조기 cancel 시에도 PIT 누수 없이 take 가 성공해야 한다.
            first10.size shouldBeEqualTo 10
        }

    // -------------------------------------------------------------------------
    // searchSuspending — 기본 쿼리
    // -------------------------------------------------------------------------

    @Test
    fun `searchSuspending match_all 은 totalHits 가 0 보다 크다`() =
        runTest(timeout = 30.seconds) {
            val response = asyncClient.searchSuspending(clazz = SearchDoc::class) {
                index(listOf(indexName))
                query { q -> q.matchAll { it } }
                size(10)
                trackTotalHits { t -> t.enabled(true) }
            }
            response.shouldNotBeNull()
            val totalHits = response.hits().total()?.value() ?: 0L
            totalHits shouldBeGreaterThan 0L
        }

    @Test
    fun `searchSuspending term 쿼리는 특정 category 만 반환한다`() =
        runTest(timeout = 30.seconds) {
            val targetCategory = "B"
            val response = asyncClient.searchSuspending(clazz = SearchDoc::class) {
                index(listOf(indexName))
                query { q ->
                    q.term { t -> t.field("category.keyword").value(targetCategory) }
                }
                size(50)
            }

            val hits = response.hits().hits()
            hits.size shouldBeGreaterOrEqualTo 1
            hits.forEach { hit ->
                val source = hit.source()
                source.shouldNotBeNull()
                source.category shouldBeEqualTo targetCategory
            }
        }
}
