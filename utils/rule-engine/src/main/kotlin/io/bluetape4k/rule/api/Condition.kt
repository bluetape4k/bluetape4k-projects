package io.bluetape4k.rule.api

/**
 * Rule 실행 조건을 나타내는 함수형 인터페이스입니다.
 * 조건이 만족해야만 [Action]을 수행합니다.
 *
 * ```kotlin
 * val condition = Condition { facts -> facts.get<Int>("age")!! >= 18 }
 * ```
 */
fun interface Condition {

    /**
     * Rule 을 적용할 수 있는 조건인지 판단합니다.
     *
     * @param facts Rule 실행에 필요한 데이터
     * @return 적용 가능 여부
     */
    fun evaluate(facts: Facts): Boolean

    companion object {
        /**
         * 항상 false를 반환하는 [Condition]
         */
        @JvmField
        val FALSE: Condition = Condition { false }

        /**
         * 항상 true를 반환하는 [Condition]
         */
        @JvmField
        val TRUE: Condition = Condition { true }
    }
}
