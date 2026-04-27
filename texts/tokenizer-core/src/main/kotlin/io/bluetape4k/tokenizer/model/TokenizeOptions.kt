package io.bluetape4k.tokenizer.model

import java.io.Serializable
import java.util.*

/**
 * 형태소 분석 실행 시 로캘 기반 동작을 지정하는 옵션이다.
 *
 * ## 동작/계약
 * - 기본 로캘은 `Locale.KOREAN`이다.
 * - 현재 공개 필드는 `locale` 하나이며 분석기 확장 시 옵션이 추가될 수 있다.
 * - 직렬화 가능한 요청 모델에서 재사용하도록 `Serializable`을 구현한다.
 *
 * ```kotlin
 * val options = TokenizeOptions.DEFAULT
 * // options.locale == Locale.KOREAN
 * ```
 */
data class TokenizeOptions(
    val locale: Locale = Locale.KOREAN,
    // 추가로 필터링할 품사 정보를 표현해도 좋겠다
): Serializable {
    companion object {
        /**
         * 형태소 분석 기본 옵션 인스턴스다.
         *
         * ## 동작/계약
         * - `locale=Locale.KOREAN`으로 초기화된 기본값이다.
         * - 재사용 가능한 정적 옵션으로 제공된다.
         *
         * ```kotlin
         * val defaults = TokenizeOptions.DEFAULT
         * // defaults == TokenizeOptions()
         * ```
         */
        val DEFAULT = TokenizeOptions()
    }
}
