package io.bluetape4k.rule.api

/**
 * 코루틴 기반의 Rule 실행 조건을 나타내는 함수형 인터페이스입니다.
 *
 * ```kotlin
 * val condition = SuspendCondition { facts -> facts.get<Int>("age")!! >= 18 }
 * ```
 */
fun interface SuspendCondition {

    /**
     * Rule 을 적용할 수 있는 조건인지 비동기로 판단합니다.
     *
     * @param facts Rule 실행에 필요한 데이터
     * @return 적용 가능 여부
     */
    suspend fun evaluate(facts: Facts): Boolean

    companion object {
        /**
         * 항상 false를 반환하는 [SuspendCondition]
         */
        @JvmField
        val FALSE: SuspendCondition = SuspendCondition { false }

        /**
         * 항상 true를 반환하는 [SuspendCondition]
         */
        @JvmField
        val TRUE: SuspendCondition = SuspendCondition { true }
    }
}
