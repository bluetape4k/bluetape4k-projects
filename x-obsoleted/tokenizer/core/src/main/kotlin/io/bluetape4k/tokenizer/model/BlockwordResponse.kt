package io.bluetape4k.tokenizer.model

/**
 * 금칙어 처리 결과와 적발 단어 목록을 담는 응답 모델이다.
 *
 * ## 동작/계약
 * - `maskedText`는 호출 측에서 전달한 최종 마스킹 문자열을 그대로 보관한다.
 * - `blockWords` 기본값은 빈 목록이며 적발 단어가 없음을 의미한다.
 * - `blockwordExists`는 `blockWords.isNotEmpty()`를 계산해 노출한다.
 *
 * ```kotlin
 * val request = blockwordRequestOf("문장")
 * val response = blockwordResponseOf(request, "문장", listOf("금칙어"))
 * // response.blockwordExists == true
 * // response.blockWords.size == 1
 * ```
 */
data class BlockwordResponse(
    val request: BlockwordRequest,
    val maskedText: String,
    val blockWords: List<String> = emptyList(),
): AbstractMessage() {
    /**
     * 금칙어가 1개 이상 적발되었는지 나타낸다.
     *
     * ## 동작/계약
     * - `blockWords`가 비어 있지 않으면 `true`를 반환한다.
     * - 계산 프로퍼티이므로 별도 상태를 저장하지 않는다.
     *
     * ```kotlin
     * val empty = blockwordResponseOf(blockwordRequestOf("문장"), "문장")
     * // empty.blockwordExists == false
     * ```
     */
    val blockwordExists: Boolean
        get() = blockWords.isNotEmpty()
}

/**
 * 금칙어 처리 응답 인스턴스를 생성한다.
 *
 * ## 동작/계약
 * - 전달한 `request`, `maskedText`, `blockWords`를 그대로 `BlockwordResponse`에 매핑한다.
 * - `blockWords`를 생략하면 빈 목록으로 초기화된다.
 *
 * ```kotlin
 * val request = blockwordRequestOf("문장")
 * val response = blockwordResponseOf(request, "***")
 * // response.maskedText == "***"
 * // response.blockwordExists == false
 * ```
 */
fun blockwordResponseOf(
    request: BlockwordRequest,
    maskedText: String,
    blockWords: List<String> = emptyList(),
): BlockwordResponse = BlockwordResponse(request, maskedText, blockWords)
