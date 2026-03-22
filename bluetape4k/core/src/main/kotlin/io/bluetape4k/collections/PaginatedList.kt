package io.bluetape4k.collections

import java.io.Serializable

/**
 * 페이징 처리된 데이터 목록 정보를 나타내는 인터페이스.
 *
 * @param T 요소 타입
 */
interface PaginatedList<out T> {

    /** 현재 페이지의 데이터 목록 */
    val contents: List<T>

    /** 현재 페이지 번호 (0부터 시작) */
    val pageNo: Int

    /** 페이지당 항목 수 */
    val pageSize: Int

    /** 전체 항목 수 */
    val totalItemCount: Long

    /** 전체 페이지 수 */
    val totalPageCount: Long
}

/**
 * [PaginatedList]의 기본 구현체.
 *
 * @param T 요소 타입
 * @param contents 현재 페이지의 데이터 목록
 * @param pageNo 현재 페이지 번호 (기본값: 0)
 * @param pageSize 페이지당 항목 수 (기본값: 10)
 * @param totalItemCount 전체 항목 수
 */
data class SimplePaginatedList<out T>(
    override val contents: List<T>,
    override val pageNo: Int = 0,
    override val pageSize: Int = 10,
    override val totalItemCount: Long,
) : PaginatedList<T>, Serializable {

    /**
     * 전체 페이지 수를 계산합니다.
     */
    override val totalPageCount: Long
        get() = if (totalItemCount == 0L) 0L
        else (totalItemCount / pageSize) + (if (totalItemCount % pageSize > 0) 1 else 0)
}
