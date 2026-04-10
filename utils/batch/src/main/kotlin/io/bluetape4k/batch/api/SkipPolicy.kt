package io.bluetape4k.batch.api

/**
 * 아이템/청크 처리 실패 시 skip 허용 여부를 결정하는 정책.
 *
 * ```kotlin
 * // 최대 10회 skip 허용
 * val policy = SkipPolicy.maxSkips(10)
 *
 * // 모든 예외 skip 허용
 * val lenient = SkipPolicy.ALL
 *
 * // 커스텀 정책
 * val custom = SkipPolicy { exception, skipCount ->
 *     exception is DataIntegrityException && skipCount < 5
 * }
 * ```
 *
 * @see SkipPolicy.NONE 스킵 불허 (모든 예외 전파)
 * @see SkipPolicy.ALL 모든 예외 스킵
 * @see SkipPolicy.maxSkips 최대 skip 횟수 제한
 */
fun interface SkipPolicy {
    /**
     * skip 허용 여부를 판단한다.
     *
     * @param exception 발생한 예외
     * @param skipCount 현재까지 누적 skip 수
     * @return true면 skip 허용, false면 Step FAILED
     */
    fun shouldSkip(exception: Throwable, skipCount: Long): Boolean

    companion object {
        /** 모든 예외를 skip 허용 */
        val ALL: SkipPolicy = SkipPolicy { _, _ -> true }

        /** skip 없음 (모든 예외에서 Step FAILED) */
        val NONE: SkipPolicy = SkipPolicy { _, _ -> false }

        /**
         * 최대 skip 횟수 제한.
         *
         * @param maxSkips 허용 최대 skip 수
         * @return [maxSkips]를 초과하지 않는 동안 skip을 허용하는 정책
         */
        fun maxSkips(maxSkips: Long): SkipPolicy = SkipPolicy { _, skipCount -> skipCount < maxSkips }
    }
}
