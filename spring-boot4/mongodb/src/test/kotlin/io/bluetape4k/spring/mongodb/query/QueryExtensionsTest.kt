package io.bluetape4k.spring.mongodb.query

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

/**
 * [QueryExtensions]의 단위 테스트입니다.
 *
 * MongoDB 연결 없이 [Query] 객체의 구조를 비교하여 확장 함수의 정확성을 검증합니다.
 */
class QueryExtensionsTest {
    companion object: KLoggingChannel()

    // ====================================================
    // queryOf
    // ====================================================

    @Test
    fun `queryOf - 조건 없으면 빈 Query를 생성한다`() {
        val query = queryOf()
        query.shouldNotBeNull()
        query.queryObject.isEmpty() shouldBeEqualTo true
    }

    @Test
    fun `queryOf - 단일 조건으로 Query를 생성한다`() {
        val criteria = Criteria.where("name").`is`("Alice")
        val query = queryOf(criteria)

        query.queryObject shouldBeEqualTo Query(criteria).queryObject
    }

    @Test
    fun `queryOf - 복수 조건을 AND로 결합한 Query를 생성한다`() {
        val c1 = Criteria.where("age").gt(20)
        val c2 = Criteria.where("city").`is`("Seoul")

        val query = queryOf(c1, c2)
        query.queryObject.shouldNotBeNull()
        query.queryObject.containsKey("\$and") shouldBeEqualTo true
    }

    // ====================================================
    // toQuery
    // ====================================================

    @Test
    fun `toQuery - Criteria에서 Query를 생성한다`() {
        val criteria = Criteria.where("name").`is`("Alice")
        val query = criteria.toQuery()

        query.queryObject shouldBeEqualTo Query(criteria).queryObject
    }

    // ====================================================
    // sortBy
    // ====================================================

    @Test
    fun `sortBy(Sort) - 정렬을 설정한다`() {
        val query = queryOf().sortBy(Sort.by(Sort.Order.asc("name")))

        query.sortObject.shouldNotBeNull()
        query.sortObject["name"] shouldBeEqualTo 1
    }

    @Test
    fun `sortBy(Order vararg) - 여러 정렬을 설정한다`() {
        val query = queryOf().sortBy(Sort.Order.asc("name"), Sort.Order.desc("age"))

        query.sortObject.shouldNotBeNull()
        query.sortObject["name"] shouldBeEqualTo 1
        query.sortObject["age"] shouldBeEqualTo -1
    }

    @Test
    fun `sortAscBy - 오름차순 정렬을 설정한다`() {
        val query = queryOf().sortAscBy("name", "age")

        query.sortObject["name"] shouldBeEqualTo 1
        query.sortObject["age"] shouldBeEqualTo 1
    }

    @Test
    fun `sortDescBy - 내림차순 정렬을 설정한다`() {
        val query = queryOf().sortDescBy("createdAt")

        query.sortObject["createdAt"] shouldBeEqualTo -1
    }

    // ====================================================
    // limitTo / skipTo / paginate
    // ====================================================

    @Test
    fun `limitTo - 조회 결과 수를 제한한다`() {
        val query = queryOf().limitTo(10)
        query.limit shouldBeEqualTo 10
    }

    @Test
    fun `skipTo - 건너뛸 문서 수를 설정한다`() {
        val query = queryOf().skipTo(20)
        query.skip shouldBeEqualTo 20L
    }

    @Test
    fun `paginate - 페이지네이션을 올바르게 설정한다`() {
        val query = queryOf().paginate(page = 2, size = 10)

        query.skip shouldBeEqualTo 20L
        query.limit shouldBeEqualTo 10
    }

    @Test
    fun `paginate - 첫 페이지는 skip 0이다`() {
        val query = queryOf().paginate(page = 0, size = 25)

        query.skip shouldBeEqualTo 0L
        query.limit shouldBeEqualTo 25
    }
}
