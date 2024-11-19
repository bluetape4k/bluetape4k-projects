package io.bluetape4k.tokenizer.model

/**
 * 금칙어의 심각도
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
        val DEFAULT = Severity.LOW
    }
}
