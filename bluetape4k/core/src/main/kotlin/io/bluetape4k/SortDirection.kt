package io.bluetape4k

/**
 * 정렬 방향
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
        val VALS = entries.toTypedArray()

        /**
         * 정렬 방향을 반환합니다.
         *
         * @param dir 방향 값 (dir > 0: 오름차순, dir <= 0 내림차순)
         * @return [SortDirection]
         */
        fun of(dir: Int): SortDirection = if (dir > 0) ASC else DESC
    }
}
