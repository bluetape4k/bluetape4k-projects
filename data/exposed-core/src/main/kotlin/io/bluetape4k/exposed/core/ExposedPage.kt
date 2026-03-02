package io.bluetape4k.exposed.core

/**
 * Exposed 조회 결과를 페이지 메타데이터와 함께 보관하는 불변 DTO입니다.
 *
 * ## 동작/계약
 * - `totalPages`는 `pageSize == 0`이면 `0`, 아니면 올림 나눗셈으로 계산됩니다.
 * - `isFirst/isLast/hasNext/hasPrevious`는 `pageNumber`와 `totalPages`만으로 계산됩니다.
 * - 내부 리스트를 변경하지 않으며 계산 프로퍼티 호출 시 추가 상태를 저장하지 않습니다.
 *
 * ```kotlin
 * val page = ExposedPage(listOf(1, 2), totalCount = 10, pageNumber = 0, pageSize = 2)
 * // page.totalPages == 5
 * ```
 */
data class ExposedPage<T>(
    val content: List<T>,
    val totalCount: Long,
    val pageNumber: Int,
    val pageSize: Int,
) {
    /** 전체 페이지 수를 반환합니다. */
    val totalPages: Int
        get() = if (pageSize == 0) 0 else ((totalCount + pageSize - 1) / pageSize).toInt()

    /** 현재 페이지가 첫 페이지인지 반환합니다. */
    val isFirst: Boolean
        get() = pageNumber == 0

    /** 현재 페이지가 마지막 페이지인지 반환합니다. */
    val isLast: Boolean
        get() = pageNumber >= totalPages - 1

    /** 다음 페이지 존재 여부를 반환합니다. */
    val hasNext: Boolean
        get() = pageNumber < totalPages - 1

    /** 이전 페이지 존재 여부를 반환합니다. */
    val hasPrevious: Boolean
        get() = pageNumber > 0
}
