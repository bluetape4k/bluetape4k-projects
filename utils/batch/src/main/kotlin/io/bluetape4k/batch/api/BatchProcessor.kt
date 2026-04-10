package io.bluetape4k.batch.api

/**
 * 배치 아이템 변환 프로세서.
 *
 * 입력 아이템([I])을 출력 아이템([O])으로 변환한다.
 *
 * ## 반환값 의미
 * - null 반환: **필터** — 해당 아이템은 writer에 전달되지 않는다 (skipCount 증가 없음).
 * - 예외: SkipPolicy 평가 → 통과하면 skipCount 증가, 실패하면 Step FAILED.
 *
 * ```kotlin
 * val processor = BatchProcessor<String, Int> { item ->
 *     item.toIntOrNull()   // null이면 해당 아이템 필터링
 * }
 * ```
 *
 * @param I 입력 아이템 타입
 * @param O 출력 아이템 타입
 */
fun interface BatchProcessor<in I : Any, out O : Any> {
    /**
     * 아이템을 처리하여 변환된 결과를 반환한다.
     *
     * @param item 처리할 입력 아이템
     * @return 변환된 출력 아이템, 또는 필터링 시 null
     */
    suspend fun process(item: I): O?
}
