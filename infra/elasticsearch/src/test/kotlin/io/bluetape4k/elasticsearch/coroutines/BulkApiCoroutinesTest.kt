package io.bluetape4k.elasticsearch.coroutines

import co.elastic.clients.elasticsearch._types.mapping.Property
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.elasticsearch.AbstractElasticsearchTest
import io.bluetape4k.elasticsearch.ElasticsearchTestFixtures
import io.bluetape4k.elasticsearch.ElasticsearchTestFixtures.createTestIndex
import io.bluetape4k.elasticsearch.ElasticsearchTestFixtures.deleteTestIndex
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.await
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

/**
 * [bulkAsFlow] 와 [suspendBulk] 에 대한 Bulk API 통합 테스트.
 *
 * Testcontainers 를 통해 실제 Elasticsearch 클러스터와 통신하며 다음 시나리오를 검증합니다.
 * - 5,000건 정상 bulk 인덱싱 → chunk 단위 [BulkResponse] emit 개수 / 색인 결과 수 검증
 * - mapping 오류로 인한 partial error 처리 → `onItemError` 콜백 누적 검증
 * - [suspendBulk] 직접 호출에 의한 단일 요청 검증
 */
class BulkApiCoroutinesTest: AbstractElasticsearchTest() {

    companion object: KLogging() {
        private const val TOTAL_DOCS = 5_000
        private const val CHUNK_SIZE = 500
        private const val EXPECTED_CHUNK_COUNT = TOTAL_DOCS / CHUNK_SIZE
        private const val PARTIAL_VALID_DOCS = 950
        private const val PARTIAL_INVALID_DOCS = 50
    }

    /**
     * Bulk 인덱싱 대상 테스트 문서.
     */
    data class TestDocument(
        val title: String,
        val value: Int,
    )

    private lateinit var indexName: String

    @BeforeEach
    fun setUp() = runTest(timeout = 60.seconds) {
        indexName = ElasticsearchTestFixtures.randomIndexName("bulk-test")
        asyncClient.createTestIndex(indexName).await()
    }

    @AfterEach
    fun tearDown() = runTest(timeout = 60.seconds) {
        runCatching { asyncClient.deleteTestIndex(indexName).await() }
    }

    // -------------------------------------------------------------------------
    // 1) 5,000건 정상 bulk 인덱싱
    // -------------------------------------------------------------------------

    @Test
    fun `5천건 bulk 인덱싱 시 chunk 단위로 BulkResponse 가 emit 된다`() =
        runTest(timeout = 120.seconds) {
            val operations: Flow<BulkOperation> = flow {
                repeat(TOTAL_DOCS) { i ->
                    emit(buildIndexOperation(indexName, i))
                }
            }

            val responses = operations
                .bulkAsFlow(client = asyncClient, indexName = indexName, chunkSize = CHUNK_SIZE)
                .toList()

            // chunk 개수 검증
            responses.size shouldBeEqualTo EXPECTED_CHUNK_COUNT

            // 각 chunk 의 응답이 정상이어야 함
            responses.forEach { response ->
                response.shouldNotBeNull()
                response.errors().shouldBeFalse()
                response.items().size shouldBeEqualTo CHUNK_SIZE
            }

            // refresh 후 색인된 문서 수 확인
            asyncClient.indices().refresh { it.index(listOf(indexName)) }.await()

            val countResponse = asyncClient.countSuspending {
                index(listOf(indexName))
            }
            countResponse.count() shouldBeGreaterOrEqualTo TOTAL_DOCS.toLong()
        }

    // -------------------------------------------------------------------------
    // 2) Partial error 처리 — mapping 오류 시 onItemError 콜백 호출
    // -------------------------------------------------------------------------

    @Test
    fun `mapping 오류가 포함된 bulk 요청은 onItemError 콜백으로 실패 item 을 전달한다`() =
        runTest(timeout = 60.seconds) {
            // value 필드를 integer 로 강제하는 strict mapping 인덱스 생성
            val strictIndex = ElasticsearchTestFixtures.randomIndexName("bulk-partial")
            asyncClient.createIndexSuspending {
                index(strictIndex)
                mappings(
                    TypeMapping.of { tm ->
                        tm.properties("title", Property.of { p -> p.text { it } })
                            .properties("value", Property.of { p -> p.integer { it } })
                    }
                )
            }

            try {
                val errorItems = mutableListOf<BulkResponseItem>()
                val errorCount = AtomicInteger(0)

                val operations: Flow<BulkOperation> = flow {
                    // 정상 문서 — value 가 Int
                    repeat(PARTIAL_VALID_DOCS) { i ->
                        emit(buildIndexOperation(strictIndex, i))
                    }
                    // 비정상 문서 — value 가 Int 로 변환 불가능한 문자열
                    repeat(PARTIAL_INVALID_DOCS) { i ->
                        emit(buildInvalidIndexOperation(strictIndex, i))
                    }
                }

                val responses = operations.bulkAsFlow(
                    client = asyncClient,
                    indexName = strictIndex,
                    chunkSize = CHUNK_SIZE,
                ) { failedItem ->
                    errorItems.add(failedItem)
                    errorCount.incrementAndGet()
                }.toList()

                // 응답이 일부 chunk 단위로 emit 되었는지 확인
                responses.shouldNotBeNull()
                responses.isNotEmpty().shouldBeTrue()

                // 적어도 한 chunk 는 errors=true
                responses.any { it.errors() }.shouldBeTrue()

                // onItemError 콜백 누적치 검증 — 50건 모두 실패해야 함
                errorCount.get() shouldBeEqualTo PARTIAL_INVALID_DOCS
                errorItems.size shouldBeEqualTo PARTIAL_INVALID_DOCS

                // 각 실패 item 은 error() 가 null 이 아니어야 함
                errorItems.forEach { item ->
                    item.error().shouldNotBeNull()
                }
            } finally {
                runCatching { asyncClient.deleteTestIndex(strictIndex).await() }
            }
        }

    // -------------------------------------------------------------------------
    // 3) suspendBulk 직접 사용
    // -------------------------------------------------------------------------

    @Test
    fun `suspendBulk 로 BulkRequest 를 직접 발행하면 BulkResponse 를 반환한다`() =
        runTest(timeout = 30.seconds) {
            val operationCount = 10
            val operations = (0 until operationCount).map { i ->
                buildIndexOperation(indexName, i)
            }

            val response = asyncClient.suspendBulk {
                index(indexName)
                operations(operations)
            }

            response.shouldNotBeNull()
            response.errors().shouldBeFalse()
            response.items().size shouldBeEqualTo operationCount
            response.took() shouldBeGreaterOrEqualTo 0L

            // refresh 후 색인 결과 확인
            asyncClient.indices().refresh { it.index(listOf(indexName)) }.await()

            val countResponse = asyncClient.countSuspending {
                index(listOf(indexName))
            }
            countResponse.count() shouldBeGreaterOrEqualTo operationCount.toLong()
        }

    // -------------------------------------------------------------------------
    // 헬퍼 — BulkOperation 빌더
    // -------------------------------------------------------------------------

    /**
     * [TestDocument] 한 건을 색인하는 [BulkOperation] 을 생성합니다.
     */
    private fun buildIndexOperation(indexName: String, i: Int): BulkOperation =
        BulkOperation.of { op ->
            op.index<TestDocument> { idx ->
                idx.index(indexName)
                    .id(UUID.randomUUID().toString())
                    .document(TestDocument(title = "doc $i", value = i))
            }
        }

    /**
     * `value` 필드가 `Int` 로 변환 불가능한 문자열인 비정상 [BulkOperation] 을 생성합니다.
     *
     * strict mapping 인덱스에 전송하면 mapping 오류로 색인이 실패합니다.
     */
    private fun buildInvalidIndexOperation(indexName: String, i: Int): BulkOperation =
        BulkOperation.of { op ->
            op.index<Map<String, Any?>> { idx ->
                idx.index(indexName)
                    .id(UUID.randomUUID().toString())
                    .document(mapOf("title" to "bad doc $i", "value" to "not-a-number-$i"))
            }
        }
}
