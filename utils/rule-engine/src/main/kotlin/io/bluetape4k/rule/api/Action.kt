package io.bluetape4k.rule.api

/**
 * Rule의 실행부분을 표현하는 함수형 인터페이스입니다.
 *
 * ```kotlin
 * val action = Action { facts -> facts["discount"] = true }
 * ```
 */
fun interface Action {

    /**
     * Rule의 [Condition]을 만족했을 때 실행되는 작업
     *
     * @param facts Rule 실행에 필요한 데이터
     */
    fun execute(facts: Facts)
}
