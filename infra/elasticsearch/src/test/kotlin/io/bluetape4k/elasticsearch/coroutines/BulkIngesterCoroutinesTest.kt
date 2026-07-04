package io.bluetape4k.elasticsearch.coroutines

import co.elastic.clients.elasticsearch.core.BulkRequest
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.elasticsearch.AbstractElasticsearchTest
import io.bluetape4k.elasticsearch.ElasticsearchTestFixtures.createTestIndex
import io.bluetape4k.elasticsearch.ElasticsearchTestFixtures.deleteTestIndex
import io.bluetape4k.elasticsearch.ElasticsearchTestFixtures.randomIndexName
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * [BulkIngester] Coroutines 확장함수 테스트.
 *
 * - [bulkIngesterOf] 팩토리 생성 검증
 * - [BulkIngester.addSuspend] 를 사용한 1000건 인덱싱 검증
 * - [bulkProgressListener] Flow 이벤트 수신 검증
 */
class BulkIngesterCoroutinesTest : AbstractElasticsearchTest() {

    companion object : KLoggingChannel()

    /** 각 테스트에서 사용할 임시 인덱스 이름 */
    private lateinit var indexName: String

    @BeforeEach
    fun setup() {
        indexName = randomIndexName("bulk-ingester-test")
        asyncClient.createTestIndex(indexName).get()
    }

    @AfterEach
    fun cleanup() {
        asyncClient.deleteTestIndex(indexName).get()
    }

    @Test
    fun `bulkIngesterOf 로 BulkIngester 인스턴스를 생성한다`() {
        val ingester = bulkIngesterOf<Void>(client)
        try {
            ingester.shouldNotBeNull()
        } finally {
            ingester.close()
        }
    }

    @Test
    fun `addSuspend 로 1000건 문서를 인덱싱한다`() = runTest(timeout = 60.seconds) {
        val docCount = 1000

        val ingester = bulkIngesterOf<Void>(
            client = asyncClient,
            maxOperations = 200,
        )

        ingester.use {
            repeat(docCount) { i ->
                val operation = BulkOperation.of { op ->
                    op.index { idx ->
                        idx.index(indexName)
                            .id("doc-$i")
                            .document(mapOf("title" to "Document $i", "seq" to i))
                    }
                }
                it.addSuspend(operation)
            }
        }

        // BulkIngester.close() 이후 refresh 하여 검색 가능 상태로 전환
        asyncClient.indices().refresh { it.index(indexName) }.await()

        val countResponse = asyncClient.countSuspending {
            index(indexName)
        }
        countResponse.count() shouldBeGreaterOrEqualTo docCount.toLong()
    }

    @Test
    fun `bulkProgressListener 를 통해 After 이벤트를 수신한다`() = runTest(timeout = 60.seconds) {
        val handle = bulkProgressListener<Void>()
        val (listener, events) = handle

        val ingester = bulkIngesterOf<Void>(
            client = asyncClient,
            maxOperations = 100,
            listener = listener,
        )

        // After 이벤트를 수집하는 Job — ingester 닫힌 후 first()로 수신
        val afterEventDeferred = launch {
            val afterEvent = events.filterIsInstance<BulkProgressEvent.After<Void>>().first()
            afterEvent.shouldNotBeNull()
            afterEvent.response.errors().shouldBeFalse()
        }

        ingester.use {
            repeat(100) { i ->
                val operation = BulkOperation.of { op ->
                    op.index { idx ->
                        idx.index(indexName)
                            .id("event-doc-$i")
                            .document(mapOf("title" to "Event Document $i"))
                    }
                }
                it.addSuspend(operation)
            }
        }

        // ingester.close() 가 호출된 후 이벤트 Job이 완료될 때까지 대기
        afterEventDeferred.join()
        // 채널 정리 — 메모리 누수 방지
        handle.close()
    }

    @Test
    fun `bulkProgressListener drops overflowed events from bounded buffer`() = runTest {
        val handle = bulkProgressListener<Void>(bufferCapacity = 1)
        val (listener, events) = handle

        try {
            val request = bulkRequestOf("overflow")

            listener.beforeBulk(1L, request, emptyList<Void>())
            listener.beforeBulk(2L, request, emptyList<Void>())

            val event = events.first()
            event shouldBeInstanceOf BulkProgressEvent.Before::class
            (event as BulkProgressEvent.Before<Void>).executionId shouldBeEqualTo 1L

            withTimeoutOrNull(100.milliseconds) {
                events.first()
            }.shouldBeNull()
        } finally {
            handle.close()
        }
    }

    private fun bulkRequestOf(id: String): BulkRequest =
        BulkRequest.of { request ->
            request.operations(
                listOf(
                    BulkOperation.of { operation ->
                        operation.index<Map<String, String>> { index ->
                            index.index("bulk-progress-test")
                                .id(id)
                                .document(mapOf("id" to id))
                        }
                    }
                )
            )
        }
}
