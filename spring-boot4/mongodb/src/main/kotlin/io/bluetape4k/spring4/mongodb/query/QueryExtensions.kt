package io.bluetape4k.spring4.mongodb.query

import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

// ====================================================
// Query 팩토리 함수
// ====================================================

/**
 * 여러 [Criteria]를 AND로 결합하여 [Query]를 생성합니다.
 *
 * ## 동작/계약
 * - 인자가 없으면 빈 조건의 [Query]가 생성됩니다.
 * - 인자가 하나면 단일 조건, 둘 이상이면 AND로 결합합니다.
 *
 * ```kotlin
 * val query = queryOf(
 *     Criteria.where("age").gt(20),
 *     Criteria.where("city").`is`("Seoul")
 * )
 * ```
 */
fun queryOf(vararg criteria: Criteria): Query =
    when (criteria.size) {
        0 -> Query()
        1 -> Query(criteria[0])
        else -> Query(Criteria().andOperator(*criteria))
    }

/**
 * [Criteria]로부터 [Query]를 생성합니다.
 *
 * ```kotlin
 * val query = Criteria.where("name").`is`("Alice").toQuery()
 * ```
 */
fun Criteria.toQuery(): Query = Query(this)

// ====================================================
// Query 빌더 확장
// ====================================================

/**
 * [Sort]를 적용한 [Query]를 반환합니다.
 *
 * ## 동작/계약
 * - `this.with(sort)`를 호출하며 원본 [Query]를 수정합니다.
 *
 * ```kotlin
 * val query = queryOf(Criteria.where("age").gt(20))
 *     .sortBy(Sort.by(Sort.Order.asc("name")))
 * ```
 */
fun Query.sortBy(sort: Sort): Query = with(sort)

/**
 * 단일 [Sort.Order]로 정렬을 설정합니다.
 *
 * ```kotlin
 * val query = queryOf().sortBy(Sort.Order.asc("name"))
 * ```
 */
fun Query.sortBy(vararg orders: Sort.Order): Query = with(Sort.by(*orders))

/**
 * 오름차순 정렬을 설정합니다.
 *
 * ```kotlin
 * val query = queryOf().sortAscBy("name", "age")
 * ```
 */
fun Query.sortAscBy(vararg fields: String): Query = with(Sort.by(Sort.Direction.ASC, *fields))

/**
 * 내림차순 정렬을 설정합니다.
 *
 * ```kotlin
 * val query = queryOf().sortDescBy("createdAt")
 * ```
 */
fun Query.sortDescBy(vararg fields: String): Query = with(Sort.by(Sort.Direction.DESC, *fields))

/**
 * 조회 결과 수를 제한합니다.
 *
 * ```kotlin
 * val query = queryOf().limitTo(10)
 * ```
 */
fun Query.limitTo(limit: Int): Query = limit(limit)

/**
 * 건너뛸 문서 수를 설정합니다.
 *
 * ```kotlin
 * val query = queryOf().skipTo(20)
 * ```
 */
fun Query.skipTo(skip: Long): Query = skip(skip)

/**
 * 페이지네이션을 설정합니다.
 *
 * ```kotlin
 * // 3번째 페이지 (0-based), 페이지당 10개
 * val query = queryOf().paginate(page = 2, size = 10)
 * ```
 *
 * @param page 0-based 페이지 번호
 * @param size 페이지당 문서 수
 */
fun Query.paginate(
    page: Int,
    size: Int,
): Query = skip(page.toLong() * size).limit(size)
