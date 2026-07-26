package io.bluetape4k.elasticsearch.examples

import co.elastic.clients.elasticsearch._types.SortOrder
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.elasticsearch.AbstractElasticsearchTest
import io.bluetape4k.elasticsearch.ElasticsearchTestFixtures
import io.bluetape4k.elasticsearch.ElasticsearchTestFixtures.createTestIndex
import io.bluetape4k.elasticsearch.ElasticsearchTestFixtures.deleteTestIndex
import io.bluetape4k.elasticsearch.coroutines.bulkAsFlow
import io.bluetape4k.elasticsearch.coroutines.getSuspending
import io.bluetape4k.elasticsearch.coroutines.indexSuspending
import io.bluetape4k.elasticsearch.coroutines.searchAsFlow
import io.bluetape4k.elasticsearch.coroutines.searchSuspending
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.future.await
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.time.Duration.Companion.seconds

/**
 * 상품(Product) 인덱스를 활용한 실전 시나리오 예시 테스트.
 *
 * 이 테스트 클래스는 README Examples 섹션과 1:1 대응하며,
 * 다음 시나리오를 실행 가능한 JUnit5 테스트로 보여줍니다.
 *
 * 1. 상품 색인 및 조회
 * 2. 상품 bulk 색인 및 검색
 * 3. 카테고리별 상품 검색
 * 4. 가격 범위 검색
 * 5. searchAsFlow 로 전체 상품 스트리밍
 */
class ProductIndexExample: AbstractElasticsearchTest() {

    companion object: KLogging() {
        private val CATEGORIES = listOf("Electronics", "Books", "Clothing", "Food", "Sports")
    }

    /**
     * 실전 예시에서 사용하는 상품 데이터 클래스.
     *
     * @property id 상품 고유 ID
     * @property name 상품명
     * @property category 상품 카테고리
     * @property price 상품 가격
     * @property inStock 재고 여부
     * @property tags 상품 태그 목록
     */
    data class Product(
        val id: String,
        val name: String,
        val category: String,
        val price: Double,
        val inStock: Boolean,
        val tags: List<String> = emptyList(),
    )

    private lateinit var indexName: String

    @BeforeEach
    fun setUp() = runTest(timeout = 60.seconds) {
        indexName = ElasticsearchTestFixtures.randomIndexName("product-example")
        asyncClient.createTestIndex(indexName).await()
    }

    @AfterEach
    fun tearDown() = runTest(timeout = 60.seconds) {
        runCatching { asyncClient.deleteTestIndex(indexName).await() }
    }

    // -------------------------------------------------------------------------
    // 1) 상품 색인 및 조회 예시
    // -------------------------------------------------------------------------

    /**
     * 단일 상품을 Elasticsearch 에 색인하고 ID 로 다시 조회하는 기본 예시입니다.
     *
     * `indexSuspending` 으로 문서를 저장하고 `getSuspending` 으로 조회한 뒤,
     * 원본 데이터와 동일한지 검증합니다.
     */
    @Test
    fun `상품 색인 및 조회 예시`() = runTest(timeout = 60.seconds) {
        // 준비 — 예시 상품 생성
        val product = Product(
            id = UUID.randomUUID().toString(),
            name = "Kotlin 프로그래밍 가이드",
            category = "Books",
            price = 29.99,
            inStock = true,
            tags = listOf("kotlin", "programming", "backend"),
        )

        // 색인
        val indexResponse = asyncClient.indexSuspending<Product> {
            index(indexName)
            id(product.id)
            document(product)
        }
        indexResponse.shouldNotBeNull()

        // 검색 가능 상태로 refresh
        asyncClient.indices().refresh { it.index(listOf(indexName)) }.await()

        // 조회
        val getResponse = asyncClient.getSuspending(
            index = indexName,
            id = product.id,
            clazz = Product::class,
        )

        // 검증
        getResponse.found().shouldBeTrue()
        val retrieved = getResponse.source()
        retrieved.shouldNotBeNull()
        retrieved.name shouldBeEqualTo product.name
        retrieved.category shouldBeEqualTo product.category
        retrieved.price shouldBeEqualTo product.price
        retrieved.inStock shouldBeEqualTo product.inStock
        retrieved.tags shouldBeEqualTo product.tags
    }

    // -------------------------------------------------------------------------
    // 2) 상품 bulk 색인 및 검색 예시
    // -------------------------------------------------------------------------

    /**
     * 100개 상품을 `bulkAsFlow` 로 일괄 색인하고 match_all 검색으로 수량을 검증하는 예시입니다.
     *
     * Flow 기반 back-pressure 가 자연스럽게 적용되어 대량 색인에 적합합니다.
     */
    @Test
    fun `상품 bulk 색인 및 검색 예시`() = runTest(timeout = 60.seconds) {
        val totalProducts = 100

        // bulk 색인
        val operations = (0 until totalProducts).map { i ->
            val product = Product(
                id = "product-$i",
                name = "상품 $i",
                category = CATEGORIES[i % CATEGORIES.size],
                price = 10.0 + (i % 90),
                inStock = i % 3 != 0,
                tags = listOf("tag-${i % 10}"),
            )
            BulkOperation.of { op ->
                op.index<Product> { idx ->
                    idx.index(indexName)
                        .id(product.id)
                        .document(product)
                }
            }
        }

        operations.asFlow()
            .bulkAsFlow(client = asyncClient, indexName = indexName, chunkSize = 20)
            .collect()

        // 검색 가능 상태로 refresh
        asyncClient.indices().refresh { it.index(listOf(indexName)) }.await()

        // match_all 검색으로 전체 수량 확인
        val searchResponse = asyncClient.searchSuspending(clazz = Product::class) {
            index(listOf(indexName))
            query { q -> q.matchAll { it } }
            size(1)
            trackTotalHits { t -> t.enabled(true) }
        }

        val totalHits = searchResponse.hits().total()?.value() ?: 0L
        totalHits shouldBeEqualTo totalProducts.toLong()
    }

    // -------------------------------------------------------------------------
    // 3) 카테고리별 상품 검색 예시
    // -------------------------------------------------------------------------

    /**
     * term 쿼리를 사용해 특정 카테고리 상품만 조회하는 예시입니다.
     *
     * `category.keyword` 필드로 정확히 일치하는 문서만 반환되는지 검증합니다.
     */
    @Test
    fun `카테고리별 상품 검색 예시`() = runTest(timeout = 60.seconds) {
        val targetCategory = "Electronics"

        // 각 카테고리별 상품 색인
        val products = listOf(
            Product("e1", "스마트폰", "Electronics", 299.99, true, listOf("mobile")),
            Product("e2", "노트북", "Electronics", 999.99, true, listOf("laptop")),
            Product("b1", "소설책", "Books", 15.0, true, listOf("fiction")),
            Product("c1", "티셔츠", "Clothing", 25.0, false, listOf("fashion")),
        )

        val operations = products.map { product ->
            BulkOperation.of { op ->
                op.index<Product> { idx ->
                    idx.index(indexName)
                        .id(product.id)
                        .document(product)
                }
            }
        }

        operations.asFlow()
            .bulkAsFlow(client = asyncClient, indexName = indexName)
            .collect()

        asyncClient.indices().refresh { it.index(listOf(indexName)) }.await()

        // category.keyword 로 정확히 일치하는 상품만 조회
        val searchResponse = asyncClient.searchSuspending(clazz = Product::class) {
            index(listOf(indexName))
            query { q ->
                q.term { t -> t.field("category.keyword").value(targetCategory) }
            }
            size(10)
        }

        val hits = searchResponse.hits().hits()
        hits.size shouldBeEqualTo 2  // Electronics 상품은 2건

        hits.forEach { hit ->
            val source = hit.source()
            source.shouldNotBeNull()
            source.category shouldBeEqualTo targetCategory
        }
    }

    // -------------------------------------------------------------------------
    // 4) 가격 범위 검색 예시
    // -------------------------------------------------------------------------

    /**
     * range 쿼리를 사용해 특정 가격 범위의 상품만 조회하는 예시입니다.
     *
     * price 필드에 gte(이상)/lte(이하) 조건을 적용하여 범위 내 상품을 찾습니다.
     */
    @Test
    fun `가격 범위 검색 예시`() = runTest(timeout = 60.seconds) {
        val minPrice = 10.0
        val maxPrice = 50.0

        // 다양한 가격대 상품 색인
        val products = listOf(
            Product("p1", "저렴한 상품", "Books", 5.0, true),
            Product("p2", "적정 가격 상품 A", "Books", 15.0, true),
            Product("p3", "적정 가격 상품 B", "Clothing", 30.0, true),
            Product("p4", "적정 가격 상품 C", "Food", 45.0, true),
            Product("p5", "고가 상품", "Electronics", 200.0, true),
        )

        val operations = products.map { product ->
            BulkOperation.of { op ->
                op.index<Product> { idx ->
                    idx.index(indexName)
                        .id(product.id)
                        .document(product)
                }
            }
        }

        operations.asFlow()
            .bulkAsFlow(client = asyncClient, indexName = indexName)
            .collect()

        asyncClient.indices().refresh { it.index(listOf(indexName)) }.await()

        // 가격 범위 검색: 10.0 이상 50.0 이하
        val searchResponse = asyncClient.searchSuspending(clazz = Product::class) {
            index(listOf(indexName))
            query { q ->
                q.range { r ->
                    r.number { n ->
                        n.field("price").gte(minPrice).lte(maxPrice)
                    }
                }
            }
            size(10)
        }

        val hits = searchResponse.hits().hits()
        hits.size shouldBeEqualTo 3  // 15.0, 30.0, 45.0 — 범위 내 3건

        hits.forEach { hit ->
            val source = hit.source()
            source.shouldNotBeNull()
            source.price shouldBeGreaterOrEqualTo minPrice
            source.price.shouldBeLessOrEqualTo(maxPrice)
        }
    }

    // -------------------------------------------------------------------------
    // 5) searchAsFlow 로 전체 상품 스트리밍 예시
    // -------------------------------------------------------------------------

    /**
     * `searchAsFlow` 를 사용해 대량 상품 데이터를 PIT + search_after 방식으로 스트리밍하는 예시입니다.
     *
     * 200개 상품을 색인한 뒤 Flow 로 전체 순회하여 누락 없이 모두 수집되는지 검증합니다.
     * scroll API 대신 PIT 기반이므로 Elasticsearch 8.x 에서 권장되는 방식입니다.
     */
    @Test
    fun `searchAsFlow 로 전체 상품 스트리밍 예시`() = runTest(timeout = 60.seconds) {
        val totalProducts = 200

        // 대량 상품 색인
        val operations = (0 until totalProducts).map { i ->
            val product = Product(
                id = "stream-product-$i",
                name = "스트리밍 상품 $i",
                category = CATEGORIES[i % CATEGORIES.size],
                price = 1.0 + i,
                inStock = true,
            )
            BulkOperation.of { op ->
                op.index<Product> { idx ->
                    idx.index(indexName)
                        .id(product.id)
                        .document(product)
                }
            }
        }

        operations.asFlow()
            .bulkAsFlow(client = asyncClient, indexName = indexName, chunkSize = 50)
            .collect()

        asyncClient.indices().refresh { it.index(listOf(indexName)) }.await()

        // searchAsFlow 로 전체 상품 스트리밍
        val streamedCount = asyncClient.searchAsFlow<Product>(
            indexName = indexName,
            batchSize = 50,
            keepAlive = "2m",
        ) {
            query { q -> q.matchAll { it } }
            // tie-breaker — _shard_doc 를 포함해야 search_after 가 정확히 동작한다
            sort { s -> s.field { f -> f.field("_shard_doc").order(SortOrder.Asc) } }
        }.count()

        streamedCount shouldBeEqualTo totalProducts
    }

    // -------------------------------------------------------------------------
    // 헬퍼 함수
    // -------------------------------------------------------------------------

    /**
     * Double 값이 [max] 이하임을 검증하는 헬퍼 확장함수입니다.
     */
    private fun Double.shouldBeLessOrEqualTo(max: Double) {
        (this <= max).shouldBeTrue()
    }
}
