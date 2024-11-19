package io.bluetape4k.tokenizer.model

/**
 * 금칙어 처리 결과
 *
 * @property request    금칙어 처리 요청 정보
 * @property maskedText 금칙어 처리 적용 결과 문자열
 * @property blockWords 적발된 금직어 목록
 */
data class BlockwordResponse(
    val request: BlockwordRequest,
    val maskedText: String,
    val blockWords: List<String> = emptyList(),
): AbstractMessage() {
    /**
     * 금칙어가 적발되었는지 여부
     */
    val blockwordExists: Boolean
        get() = blockWords.isNotEmpty()
}

/**
 * 금칙어 처리 결과를 생성한다.
 *
 * @param request    금칙어 처리 요청 정보
 * @param maskedText 금칙어 처리 적용 결과 문자열
 * @param blockWords 적발된 금칙어 목록
 */
fun blockwordResponseOf(
    request: BlockwordRequest,
    maskedText: String,
    blockWords: List<String> = emptyList(),
): BlockwordResponse = BlockwordResponse(request, maskedText, blockWords)
