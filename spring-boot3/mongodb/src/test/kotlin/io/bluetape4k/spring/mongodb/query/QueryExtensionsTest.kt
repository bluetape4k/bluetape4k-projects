package io.bluetape4k.spring.mongodb.query

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

/**
 * [QueryExtensions]의 단위 테스트입니다.
 *
 * MongoDB 연결 없이 [Query] 객체의 구성을 검증합니다.
 */
class QueryExtensionsTest {

    companion object: KLogging()

    // ====================================================
    // queryOf 팩토리
    // ====================================================

    @Test
    fun `queryOf - 조건 없이 빈 Query를 생성한다`() {
        val query = queryOf()
        query.shouldNotBeNull()
        query.queryObject.isEmpty() shouldBeEqualTo true
    }

    @Test
    fun `queryOf - 단일 조건으로 Query를 생성한다`() {
        val query = queryOf(Criteria.where("name").`is`("Alice"))
        query.shouldNotBeNull()
        query.queryObject["name"] shouldBeEqualTo "Alice"
    }

    @Test
    fun `queryOf - 여러 조건을 AND로 결합한 Query를 생성한다`() {
        val query = queryOf(
            Criteria.where("age").gt(20),
            Criteria.where("city").`is`("Seoul")
        )
        query.shouldNotBeNull()
        query.queryObject.containsKey("\$and") shouldBeEqualTo true
    }

    // ====================================================
    // Criteria.toQuery()
    // ====================================================

    @Test
    fun `toQuery - Criteria에서 Query로 변환한다`() {
        val criteria = Criteria.where("name").`is`("Alice")
        val query = criteria.toQuery()

        query.shouldNotBeNull()
        query.queryObject["name"] shouldBeEqualTo "Alice"
    }

    // ====================================================
    // 정렬
    // ====================================================

    @Test
    fun `sortBy - Sort 객체로 정렬을 설정한다`() {
        val query = queryOf().sortBy(Sort.by(Sort.Order.asc("name")))
        query.shouldNotBeNull()
        query.sortObject["name"] shouldBeEqualTo 1
    }

    @Test
    fun `sortBy - vararg Order로 정렬을 설정한다`() {
        val query = queryOf().sortBy(Sort.Order.asc("name"), Sort.Order.desc("age"))
        query.shouldNotBeNull()
        query.sortObject["name"] shouldBeEqualTo 1
        query.sortObject["age"] shouldBeEqualTo -1
    }

    @Test
    fun `sortAscBy - 오름차순 정렬을 설정한다`() {
        val query = queryOf().sortAscBy("name", "age")
        query.shouldNotBeNull()
        query.sortObject["name"] shouldBeEqualTo 1
        query.sortObject["age"] shouldBeEqualTo 1
    }

    @Test
    fun `sortDescBy - 내림차순 정렬을 설정한다`() {
        val query = queryOf().sortDescBy("createdAt")
        query.shouldNotBeNull()
        query.sortObject["createdAt"] shouldBeEqualTo -1
    }

    // ====================================================
    // 페이지네이션
    // ====================================================

    @Test
    fun `limitTo - 조회 결과 수를 제한한다`() {
        val query = queryOf().limitTo(10)
        query.shouldNotBeNull()
        query.limit shouldBeEqualTo 10
    }

    @Test
    fun `skipTo - 건너뛸 문서 수를 설정한다`() {
        val query = queryOf().skipTo(20)
        query.shouldNotBeNull()
        query.skip shouldBeEqualTo 20
    }

    @Test
    fun `paginate - 페이지네이션을 올바르게 설정한다`() {
        val query = queryOf().paginate(page = 2, size = 10)
        query.shouldNotBeNull()
        query.skip shouldBeEqualTo 20
        query.limit shouldBeEqualTo 10
    }

    @Test
    fun `paginate - 0번째 페이지는 skip 0이다`() {
        val query = queryOf().paginate(page = 0, size = 25)
        query.shouldNotBeNull()
        query.skip shouldBeEqualTo 0
        query.limit shouldBeEqualTo 25
    }
}
