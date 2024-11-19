package io.bluetape4k.tokenizer.model

import io.bluetape4k.support.requireNotBlank

/**
 * 금칙어 처리를 위한 요청
 *
 * @property text 금칙어 처리 대상 문자열
 * @property options 금칙어 처리를 위한 옵션 정보
 * @property requestTimestamp 요청 생성 시각
 */
data class BlockwordRequest(
    val text: String,
    val options: BlockwordOptions = BlockwordOptions.DEFAULT,
): AbstractMessage() {
    init {
        text.requireNotBlank("text")
    }
}

/**
 * 금칙어 처리 요청 정보를 생성한다.
 *
 * @param text    금칙어 처리 대상 문자열
 * @param options 금칙어 처리를 위한 옵션 정보
 */
fun blockwordRequestOf(
    text: String,
    options: BlockwordOptions = BlockwordOptions.DEFAULT,
): BlockwordRequest {
    text.requireNotBlank("text")
    return BlockwordRequest(text, options)
}
