package io.bluetape4k.tokenizer.model

/**
 * 금칙어의 노출 위험도를 단계별로 표현하는 열거형이다.
 *
 * ## 동작/계약
 * - `LOW`, `MIDDLE`, `HIGH` 순으로 위험 수준이 높아진다.
 * - 기본값은 `DEFAULT`를 통해 `LOW`로 제공된다.
 * - 금칙어 필터 옵션에서 차단 기준 강도를 지정할 때 사용한다.
 *
 * ```kotlin
 * val severity = Severity.DEFAULT
 * // severity == Severity.LOW
 * ```
 */
enum class Severity {

    /**
     * 심각도 낮음 (은어, 속어 종류)
     * 14세 이하 저학년에게는 보여주면 안되는 금칙어
     */
    LOW,

    /**
     * 심각도 중간 (욕설 종류)
     * 19세 이하 미성년자에게는 보여주지 말아야 할 금칙어
     */
    MIDDLE,

    /**
     * 심각도 높음 (욕설, 증오, 지역비하 등)
     * 모든 연령층에서 보여주면 안되는 금칙어
     */
    HIGH;

    companion object {
        /**
         * 금칙어 심각도 기본값(`LOW`)이다.
         *
         * ## 동작/계약
         * - `BlockwordOptions` 기본 생성 시 이 값을 사용한다.
         * - 상수 참조이므로 런타임 상태에 따라 값이 변하지 않는다.
         *
         * ```kotlin
         * val defaultSeverity = Severity.DEFAULT
         * // defaultSeverity == Severity.LOW
         * ```
         */
        val DEFAULT = Severity.LOW
    }
}
