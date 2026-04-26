package io.bluetape4k.elasticsearch.coroutines

import io.bluetape4k.elasticsearch.AbstractElasticsearchTest
import io.bluetape4k.elasticsearch.ElasticsearchTestFixtures
import io.bluetape4k.elasticsearch.ElasticsearchTestFixtures.createTestIndex
import io.bluetape4k.elasticsearch.ElasticsearchTestFixtures.deleteTestIndex
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.future.await
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

/**
 * [ElasticsearchCoroutines] suspend 확장함수에 대한 CRUD 통합 테스트.
 *
 * Testcontainers 를 통해 실제 Elasticsearch 클러스터와 통신하며
 * 인덱스 생성/존재 확인/삭제, 문서 CRUD, 문서 존재 확인, 검색 시나리오를 검증합니다.
 */
class ElasticsearchCoroutinesTest : AbstractElasticsearchTest() {

    companion object : KLogging()

    /**
     * 테스트에서 사용하는 문서 타입.
     */
    data class TestDocument(
        val title: String,
        val content: String,
        val tags: List<String> = emptyList(),
        val score: Double = 0.0,
    )

    /**
     * 업데이트 시 부분 변경에 사용하는 타입.
     */
    data class TestDocumentPartial(
        val title: String? = null,
        val score: Double? = null,
    )

    private lateinit var indexName: String

    @BeforeEach
    fun setUp() = runTest(timeout = 30.seconds) {
        indexName = ElasticsearchTestFixtures.randomIndexName("crud-test")
        asyncClient.createTestIndex(indexName).await()
    }

    @AfterEach
    fun tearDown() = runTest(timeout = 30.seconds) {
        runCatching { asyncClient.deleteTestIndex(indexName).await() }
    }

    // -------------------------------------------------------------------------
    // 인덱스 생성 / 존재 확인 / 삭제
    // -------------------------------------------------------------------------

    @Test
    fun `인덱스 생성 후 존재 확인 그리고 삭제가 순서대로 동작한다`() = runTest(timeout = 30.seconds) {
        val tmpIndex = ElasticsearchTestFixtures.randomIndexName("lifecycle-test")

        // 생성
        val createResponse = asyncClient.createIndexSuspending { index(tmpIndex) }
        createResponse.acknowledged().shouldBeTrue()

        // 존재 확인 — 생성 후 존재해야 함
        val existsAfterCreate = asyncClient.indexExistsSuspending { index(listOf(tmpIndex)) }
        existsAfterCreate.shouldBeTrue()

        // 삭제
        val deleteResponse = asyncClient.deleteIndexSuspending { index(listOf(tmpIndex)) }
        deleteResponse.acknowledged().shouldBeTrue()

        // 존재 확인 — 삭제 후 없어야 함
        val existsAfterDelete = asyncClient.indexExistsSuspending { index(listOf(tmpIndex)) }
        existsAfterDelete.shouldBeFalse()
    }

    // -------------------------------------------------------------------------
    // 문서 CRUD roundtrip
    // -------------------------------------------------------------------------

    @Test
    fun `문서 색인 후 조회하면 동일한 내용을 반환한다`() = runTest(timeout = 30.seconds) {
        val docId = "doc-1"
        val doc = TestDocument(
            title = "Elasticsearch 소개",
            content = "분산 검색 엔진입니다.",
            tags = listOf("search", "nosql"),
            score = 9.5,
        )

        // 색인
        val indexResponse = asyncClient.indexSuspending<TestDocument> {
            index(indexName)
            id(docId)
            document(doc)
        }
        indexResponse.shouldNotBeNull()

        // 인덱스 refresh — 검색 가능 상태로
        asyncClient.indices().refresh { it.index(listOf(indexName)) }.await()

        // 조회
        val getResponse = asyncClient.getSuspending(
            index = indexName,
            id = docId,
            clazz = TestDocument::class,
        )
        getResponse.found().shouldBeTrue()
        val retrieved = getResponse.source()
        retrieved.shouldNotBeNull()
        retrieved.title shouldBeEqualTo doc.title
        retrieved.content shouldBeEqualTo doc.content
        retrieved.tags shouldBeEqualTo doc.tags
        retrieved.score shouldBe doc.score
    }

    @Test
    fun `문서 색인 후 업데이트하면 변경된 내용이 반영된다`() = runTest(timeout = 30.seconds) {
        val docId = "doc-update"
        val original = TestDocument(title = "원본 제목", content = "원본 내용", score = 1.0)

        // 색인
        asyncClient.indexSuspending<TestDocument> {
            index(indexName)
            id(docId)
            document(original)
        }

        // 업데이트
        val updatedScore = 9.9
        val updateResponse = asyncClient.updateSuspending<TestDocument, TestDocumentPartial> {
            index(indexName)
            id(docId)
            doc(TestDocumentPartial(score = updatedScore))
        }
        updateResponse.shouldNotBeNull()

        // 변경 내용 검증
        asyncClient.indices().refresh { it.index(listOf(indexName)) }.await()

        val getResponse = asyncClient.getSuspending(
            index = indexName,
            id = docId,
            clazz = TestDocument::class,
        )
        getResponse.found().shouldBeTrue()
        val updated = getResponse.source()
        updated.shouldNotBeNull()
        updated.title shouldBeEqualTo original.title      // 변경하지 않은 필드
        updated.score shouldBe updatedScore               // 변경한 필드
    }

    @Test
    fun `문서 색인 후 삭제하면 조회되지 않는다`() = runTest(timeout = 30.seconds) {
        val docId = "doc-delete"
        val doc = TestDocument(title = "삭제 대상", content = "삭제될 내용")

        // 색인
        asyncClient.indexSuspending<TestDocument> {
            index(indexName)
            id(docId)
            document(doc)
        }

        // 삭제
        val deleteResponse = asyncClient.deleteSuspending {
            index(indexName)
            id(docId)
        }
        deleteResponse.shouldNotBeNull()

        // 삭제 후 조회
        asyncClient.indices().refresh { it.index(listOf(indexName)) }.await()

        val getResponse = asyncClient.getSuspending(
            index = indexName,
            id = docId,
            clazz = TestDocument::class,
        )
        getResponse.found().shouldBeFalse()
    }

    // -------------------------------------------------------------------------
    // 문서 존재 확인 (existsSuspending)
    // -------------------------------------------------------------------------

    @Test
    fun `색인된 문서는 exists 가 true 를 반환한다`() = runTest(timeout = 30.seconds) {
        val docId = "exists-doc"
        asyncClient.indexSuspending<TestDocument> {
            index(indexName)
            id(docId)
            document(TestDocument(title = "존재 확인", content = "테스트"))
        }
        asyncClient.indices().refresh { it.index(listOf(indexName)) }.await()

        val exists = asyncClient.existsSuspending {
            index(indexName)
            id(docId)
        }
        exists.shouldBeTrue()
    }

    @Test
    fun `없는 문서는 exists 가 false 를 반환한다`() = runTest(timeout = 30.seconds) {
        val exists = asyncClient.existsSuspending {
            index(indexName)
            id("non-existent-id")
        }
        exists.shouldBeFalse()
    }

    // -------------------------------------------------------------------------
    // 검색 (searchSuspending)
    // -------------------------------------------------------------------------

    @Test
    fun `match_all 쿼리로 색인된 전체 문서를 조회한다`() = runTest(timeout = 30.seconds) {
        val docs = listOf(
            TestDocument(title = "문서 A", content = "내용 A", tags = listOf("alpha")),
            TestDocument(title = "문서 B", content = "내용 B", tags = listOf("beta")),
            TestDocument(title = "문서 C", content = "내용 C", tags = listOf("gamma")),
        )

        docs.forEachIndexed { idx, doc ->
            asyncClient.indexSuspending<TestDocument> {
                index(indexName)
                id("doc-$idx")
                document(doc)
            }
        }
        asyncClient.indices().refresh { it.index(listOf(indexName)) }.await()

        val searchResponse = asyncClient.searchSuspending(clazz = TestDocument::class) {
            index(listOf(indexName))
            query { q -> q.matchAll { it } }
            size(10)
        }

        val totalHits = searchResponse.hits().total()?.value() ?: 0L
        totalHits shouldBeGreaterOrEqualTo docs.size.toLong()
    }

    @Test
    fun `term 쿼리로 특정 태그를 가진 문서를 조회한다`() = runTest(timeout = 30.seconds) {
        val targetTag = "kotlin"
        asyncClient.indexSuspending<TestDocument> {
            index(indexName)
            id("kotlin-doc")
            document(TestDocument(title = "Kotlin 문서", content = "코틀린 관련", tags = listOf(targetTag)))
        }
        asyncClient.indexSuspending<TestDocument> {
            index(indexName)
            id("java-doc")
            document(TestDocument(title = "Java 문서", content = "자바 관련", tags = listOf("java")))
        }
        asyncClient.indices().refresh { it.index(listOf(indexName)) }.await()

        val searchResponse = asyncClient.searchSuspending(clazz = TestDocument::class) {
            index(listOf(indexName))
            query { q ->
                q.term { t ->
                    t.field("tags").value(targetTag)
                }
            }
            size(10)
        }

        val hits = searchResponse.hits().hits()
        hits.size shouldBeGreaterOrEqualTo 1

        val foundTitles = hits.mapNotNull { it.source()?.title }
        foundTitles.any { it.contains("Kotlin") }.shouldBeTrue()
    }
}
