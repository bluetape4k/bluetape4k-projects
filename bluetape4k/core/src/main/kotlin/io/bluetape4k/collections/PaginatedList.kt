package io.bluetape4k.collections

import io.bluetape4k.support.requirePositiveNumber
import io.bluetape4k.support.requireZeroOrPositiveNumber
import java.io.Serializable

/**
 * 페이징 처리된 데이터 목록 정보를 나타내는 인터페이스.
 *
 * 예제:
 * ```kotlin
 * val page = SimplePaginatedList(
 *     contents = listOf("a", "b", "c"),
 *     pageNo = 0,
 *     pageSize = 10,
 *     totalItemCount = 25,
 * )
 * page.totalPageCount // 3
 * page.contents       // ["a", "b", "c"]
 * ```
 *
 * @param T 요소 타입
 */
interface PaginatedList<out T> {

    /**
     * 현재 페이지의 데이터 목록.
     *
     * 예제:
     * ```kotlin
     * val page = SimplePaginatedList(
     *     contents = listOf(1, 2, 3),
     *     pageNo = 2,
     *     pageSize = 5,
     *     totalItemCount = 50,
     * )
     * page.contents // [1, 2, 3]
     * ```
     */
    val contents: List<T>

    /**
     * 현재 페이지 번호 (0부터 시작).
     *
     * 예제:
     * ```kotlin
     * val page = SimplePaginatedList(
     *     contents = listOf("x"),
     *     pageNo = 2,
     *     totalItemCount = 30,
     * )
     * page.pageNo // 2
     * ```
     */
    val pageNo: Int

    /**
     * 페이지당 항목 수.
     *
     * 예제:
     * ```kotlin
     * val page = SimplePaginatedList(
     *     contents = listOf("x"),
     *     pageSize = 20,
     *     totalItemCount = 100,
     * )
     * page.pageSize // 20
     * ```
     */
    val pageSize: Int

    /**
     * 전체 항목 수.
     *
     * 예제:
     * ```kotlin
     * val page = SimplePaginatedList(
     *     contents = listOf("x"),
     *     totalItemCount = 25,
     * )
     * page.totalItemCount // 25
     * ```
     */
    val totalItemCount: Long

    /**
     * 전체 페이지 수.
     *
     * 예제:
     * ```kotlin
     * // 나머지 있는 경우: ceil(25 / 10) = 3
     * val page1 = SimplePaginatedList(contents = listOf("x"), totalItemCount = 25, pageSize = 10)
     * page1.totalPageCount // 3
     *
     * // 나누어 떨어지는 경우: 20 / 10 = 2
     * val page2 = SimplePaginatedList(contents = listOf("x"), totalItemCount = 20, pageSize = 10)
     * page2.totalPageCount // 2
     *
     * // 항목이 없는 경우
     * val page3 = SimplePaginatedList(contents = emptyList<String>(), totalItemCount = 0)
     * page3.totalPageCount // 0
     * ```
     */
    val totalPageCount: Long
}

/**
 * [PaginatedList]의 기본 구현체.
 *
 * 예제:
 * ```kotlin
 * // 기본값 사용: pageNo=0, pageSize=10
 * val page = SimplePaginatedList(
 *     contents = listOf(1, 2, 3),
 *     totalItemCount = 100,
 * )
 * page.pageNo        // 0
 * page.pageSize      // 10
 * page.totalPageCount // 10
 *
 * // 명시적 파라미터 지정
 * val page2 = SimplePaginatedList(
 *     contents = listOf("a", "b", "c"),
 *     pageNo = 1,
 *     pageSize = 5,
 *     totalItemCount = 13,
 * )
 * page2.totalPageCount // 3
 * ```
 *
 * @param T 요소 타입
 * @param contents 현재 페이지의 데이터 목록
 * @param pageNo 현재 페이지 번호 (기본값: 0)
 * @param pageSize 페이지당 항목 수 (기본값: 10)
 * @param totalItemCount 전체 항목 수
 * @throws IllegalArgumentException [pageNo] 또는 [totalItemCount]가 음수이거나 [pageSize]가 0 이하인 경우
 */
data class SimplePaginatedList<out T>(
    override val contents: List<T>,
    override val pageNo: Int = 0,
    override val pageSize: Int = 10,
    override val totalItemCount: Long,
): PaginatedList<T>, Serializable {

    init {
        pageNo.requireZeroOrPositiveNumber("pageNo")
        pageSize.requirePositiveNumber("pageSize")
        totalItemCount.requireZeroOrPositiveNumber("totalItemCount")
    }

    /**
     * 전체 페이지 수를 계산합니다.
     *
     * - `totalItemCount`가 0이면 0을 반환합니다.
     * - 나머지가 있는 경우 페이지 수를 1 증가시켜 반올림합니다.
     *
     * 예제:
     * ```kotlin
     * // 나머지 있는 경우
     * SimplePaginatedList(contents = listOf("x"), totalItemCount = 25, pageSize = 10)
     *     .totalPageCount // 3
     *
     * // 나누어 떨어지는 경우
     * SimplePaginatedList(contents = listOf("x"), totalItemCount = 20, pageSize = 10)
     *     .totalPageCount // 2
     *
     * // 항목이 없는 경우
     * SimplePaginatedList(contents = emptyList<String>(), totalItemCount = 0)
     *     .totalPageCount // 0
     * ```
     */
    override val totalPageCount: Long
        get() = if (totalItemCount == 0L) 0L
        else (totalItemCount / pageSize) + (if (totalItemCount % pageSize > 0) 1 else 0)
}
