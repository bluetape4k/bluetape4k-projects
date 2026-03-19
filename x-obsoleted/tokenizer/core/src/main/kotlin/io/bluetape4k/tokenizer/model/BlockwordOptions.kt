package io.bluetape4k.tokenizer.model

import java.io.Serializable
import java.util.*

/**
 * 금칙어 마스킹과 판정 강도를 제어하는 요청 옵션이다.
 *
 * ## 동작/계약
 * - 기본 마스크 문자열은 `*`이며 호출 측에서 다른 문자열로 교체할 수 있다.
 * - 기본 로캘은 `Locale.KOREAN`으로 설정된다.
 * - 기본 심각도는 `Severity.DEFAULT`(`LOW`)를 사용한다.
 *
 * ```kotlin
 * val options = blockwordOptionsOf(mask = "#", severity = Severity.HIGH)
 * // options.mask == "#"
 * // options.locale == Locale.KOREAN
 * // options.severity == Severity.HIGH
 * ```
 */
data class BlockwordOptions(
    val mask: String = "*",
    val locale: Locale = Locale.KOREAN,
    val severity: Severity = Severity.DEFAULT,
): Serializable {
    companion object {
        /**
         * 금칙어 처리 기본 옵션 인스턴스다.
         *
         * ## 동작/계약
         * - `mask="*"`, `locale=Locale.KOREAN`, `severity=Severity.DEFAULT`를 사용한다.
         * - 재사용 가능한 정적 기본값으로 제공된다.
         *
         * ```kotlin
         * val defaults = BlockwordOptions.DEFAULT
         * // defaults.mask == "*"
         * // defaults.severity == Severity.LOW
         * ```
         */
        val DEFAULT = BlockwordOptions()
    }
}

/**
 * 금칙어 처리 옵션 인스턴스를 간단히 생성한다.
 *
 * ## 동작/계약
 * - 전달한 `mask`, `locale`, `severity`를 그대로 `BlockwordOptions`에 담아 반환한다.
 * - 인자를 생략하면 `BlockwordOptions.DEFAULT`와 동일한 기본값 조합이 적용된다.
 *
 * ```kotlin
 * val options = blockwordOptionsOf()
 * // options == BlockwordOptions.DEFAULT
 * ```
 */
fun blockwordOptionsOf(
    mask: String = "*",
    locale: Locale = Locale.KOREAN,
    severity: Severity = Severity.DEFAULT,
): BlockwordOptions = BlockwordOptions(mask, locale, severity)
