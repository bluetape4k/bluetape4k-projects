package io.bluetape4k.exposed.core

/**
 * 페이징 조회 결과를 나타내는 데이터 클래스입니다.
 *
 * @param T 엔티티 타입
 * @property content 현재 페이지의 엔티티 목록
 * @property totalCount 전체 엔티티 수
 * @property pageNumber 현재 페이지 번호 (0부터 시작)
 * @property pageSize 페이지 크기
 */
data class ExposedPage<T>(
    val content: List<T>,
    val totalCount: Long,
    val pageNumber: Int,
    val pageSize: Int,
) {
    /**
     * 전체 페이지 수
     */
    val totalPages: Int
        get() = if (pageSize == 0) 0 else ((totalCount + pageSize - 1) / pageSize).toInt()

    /**
     * 현재 페이지가 첫 번째 페이지인지 여부
     */
    val isFirst: Boolean
        get() = pageNumber == 0

    /**
     * 현재 페이지가 마지막 페이지인지 여부
     */
    val isLast: Boolean
        get() = pageNumber >= totalPages - 1

    /**
     * 다음 페이지가 있는지 여부
     */
    val hasNext: Boolean
        get() = pageNumber < totalPages - 1

    /**
     * 이전 페이지가 있는지 여부
     */
    val hasPrevious: Boolean
        get() = pageNumber > 0
}
