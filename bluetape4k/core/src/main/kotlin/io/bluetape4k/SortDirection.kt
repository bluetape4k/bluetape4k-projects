package io.bluetape4k

/**
 * 정렬 방향을 나타내는 열거형입니다.
 *
 * ```kotlin
 * // 오름차순 정렬 방향 사용
 * val dir = SortDirection.ASC
 * println(dir.direction) // 1
 *
 * // 정수 값으로 방향 결정
 * val asc = SortDirection.of(1)   // SortDirection.ASC
 * val desc = SortDirection.of(-1) // SortDirection.DESC
 * val alsoDesc = SortDirection.of(0) // SortDirection.DESC (0 이하는 내림차순)
 * ```
 *
 * @property direction 방향 값 (1: 오름차순, -1: 내림차순)
 */
enum class SortDirection(val direction: Int) {

    /**
     * 오름차순
     */
    ASC(1),

    /**
     * 내림차순
     */
    DESC(-1);

    companion object {
        /**
         * 정렬 방향을 반환합니다.
         *
         * @param dir 방향 값 (dir > 0: 오름차순, dir <= 0 내림차순)
         * @return [SortDirection]
         */
        fun of(dir: Int): SortDirection = if (dir > 0) ASC else DESC
    }
}
