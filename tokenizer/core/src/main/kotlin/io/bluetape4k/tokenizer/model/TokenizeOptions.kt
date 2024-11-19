package io.bluetape4k.tokenizer.model

import java.io.Serializable
import java.util.*

/**
 * 형태소 분석 시 옵션
 */
data class TokenizeOptions(
    val locale: Locale = Locale.KOREAN,
    // 추가로 필터링할 품사 정보를 표현해도 좋겠다
): Serializable {
    companion object {
        val DEFAULT = TokenizeOptions()
    }
}
